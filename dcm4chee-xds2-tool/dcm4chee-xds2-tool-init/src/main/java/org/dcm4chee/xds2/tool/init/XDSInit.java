package org.dcm4chee.xds2.tool.init;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.soap.AddressingFeature;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
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
                filenames = Arrays.asList(XDSInitCommon.defaultFiles);
            }
        }
        System.out.print("Looking up endpoint URL "+url+" ...");
        QName name = new QName("urn:ihe:iti:xds-b:2007", "XDSbRegistry");
        Service service = Service.create(url, name);
        DocumentRegistryPortType docRegistry = (DocumentRegistryPortType) 
        service.getPort(DocumentRegistryPortType.class, new AddressingFeature());
        System.out.print("Adding definitions...");
        try {
            XDSInitCommon.initializeRegistry(filenames, defaultInit, docRegistry);
        } catch (Exception e) {
            System.out.println(getExceptionMessage(e, " - FAILURE!\nError(s):"));
            return;
        }
        System.out.println("SUCCESS!");
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
