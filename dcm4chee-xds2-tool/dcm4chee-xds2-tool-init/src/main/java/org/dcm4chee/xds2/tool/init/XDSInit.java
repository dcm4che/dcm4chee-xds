package org.dcm4chee.xds2.tool.init;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.soap.AddressingFeature;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.dcm4chee.xds2.infoset.rim.RegistryError;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryPortType;

public class XDSInit {
    private static final String DEFAULT_WSDL_URL = "http://localhost:8080/dcm4chee-xds/XDSbRegistry/b?wsdl";
    
    public static void main(String[] args) throws Exception {
        CommandLine cl = parseComandLine(args);
        URL url = new URL(cl.getOptionValue("wsdl", DEFAULT_WSDL_URL));
        @SuppressWarnings("unchecked")
        List<String> filenames = cl.getArgList();
        boolean defaultInit = filenames.isEmpty();
        if (defaultInit) {
            if (cl.hasOption("additional")) {
                filenames = Arrays.asList("additional.xml");
            } else {
                filenames = Arrays.asList("initialize.xml", "ebXmlAssociationTypes.xml");
            }
        }
        QName name = new QName("urn:ihe:iti:xds-b:2007", "XDSbRegistry");
        Service service = Service.create(url, name);
        DocumentRegistryPortType docRegistry = (DocumentRegistryPortType) 
        service.getPort(DocumentRegistryPortType.class, new AddressingFeature());
        for (String fn : filenames) {
            System.out.print("Send '"+fn+"' to "+url);
            try {
                SubmitObjectsRequest req = getSubmitObjectsRequest(fn, defaultInit);
                RegistryResponseType rsp = docRegistry.documentRegistryRegisterDocumentSetB(req);
                if (!"urn:oasis:names:tc:ebxml-regrep:ResponseStatusType:Success".equals(rsp.getStatus())){
                    System.out.println(" - FAILURE!\nErrors:");
                    int i = 1;
                    for (RegistryError err : rsp.getRegistryErrorList().getRegistryError()) {
                        System.out.println((i++)+") "+err.getErrorCode()+" : "+err.getCodeContext());
                    }
                } else {
                    System.out.println(" - SUCCESS!");
                }
            } catch (Exception x) {
                System.out.println(getExceptionMessage(x, " - Send failed! Reason:"));
            }
        }
    }


    private static String getExceptionMessage(Exception x, String msg) {
        StringBuilder sb = new StringBuilder(msg);
        sb.append(x.getClass().getSimpleName()).append(":").append(x.getMessage());
        Throwable e = x.getCause();
        while (e != null) {
            sb.append("\n caused by:").append(e.getClass().getSimpleName())
            .append(':').append(e.getMessage());
            e = e.getCause();
        }
        return sb.toString();
    }


    public static SubmitObjectsRequest getSubmitObjectsRequest(String metadataFilename, boolean defaultInit) throws JAXBException, FileNotFoundException {
        JAXBContext jaxbContext = JAXBContext.newInstance(SubmitObjectsRequest.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        SubmitObjectsRequest req = (SubmitObjectsRequest) unmarshaller.unmarshal(
                defaultInit ? XDSInit.class.getResourceAsStream(metadataFilename) :
                    new FileInputStream(metadataFilename));
        return req;
    }
    
    private static CommandLine parseComandLine(String[] args) throws ParseException {
        Options opts = new Options();
        opts.addOption("h", "help", false, "Initialize XDS Registry by sending ebXML SubmitObjectRequests");//rb.getString("help"));
        opts.addOption("v", "version", false, "Version:");
        opts.addOption("wsdl", true, "WSDL URL of XDS Registry Service");
        opts.addOption("additional", false, "Add additional ClassificationSchemes/ClassificationNode");
        CommandLineParser parser = new PosixParser();
        CommandLine cl = parser.parse(opts, args);
        if (cl.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "Usage: xdsinit [-wsdl wsdl-url] [<SubmitObjectRequest filename1> [filename2 [..] ]",
                    "XDSInit.\n"+
                    "Send SubmitObjectRequest ebXML Object to server", 
                    opts,
                    "Example1: xdsinit\n" +
                    "          Initialize XDS Registry with default XDS Classifications and Association type schemas:\n"+
                    "          WSDL URL:"+DEFAULT_WSDL_URL+"\n"+
                    "Example2: xdsinit -wsdl http://xdsserver:8080/XDSbRegistry?wsdl\n"+
                    "          initialize XDS Registry on host 'xdsserver' with defaults.\n"
                    );
            System.exit(0);
        }
        if (cl.hasOption("V")) {
            Package p = XDSInit.class.getPackage();
            String s = XDSInit.class.getSimpleName();
            System.out.println(s.substring(s.lastIndexOf('.')+1) + ": " +
                   p.getImplementationVersion());
            System.exit(0);
        }
        return cl;
    }

}
