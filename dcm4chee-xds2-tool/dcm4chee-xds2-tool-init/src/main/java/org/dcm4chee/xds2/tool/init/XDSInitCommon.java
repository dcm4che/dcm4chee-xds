package org.dcm4chee.xds2.tool.init;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.dcm4chee.xds2.infoset.rim.RegistryError;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryPortType;

public class XDSInitCommon {

    static final String[] defaultFiles = {"initialize.xml", "ebXmlAssociationTypes.xml"};

    public static void autoInitializeRegistry(DocumentRegistryPortType docRegistry) {
        initializeRegistry(Arrays.asList(defaultFiles), true, docRegistry);
    }

    public static void initializeRegistry(List<String> filenames, boolean defaultInit, DocumentRegistryPortType docRegistry)  {
        for (String fn : filenames) {
                SubmitObjectsRequest req;
                try {
                    req = getSubmitObjectsRequest(fn, defaultInit);
                } catch (FileNotFoundException | JAXBException e) {
                    throw new RuntimeException(e);
                }
                RegistryResponseType rsp = docRegistry.documentRegistryRegisterDocumentSetB(req);
                if (!"urn:oasis:names:tc:ebxml-regrep:ResponseStatusType:Success".equals(rsp.getStatus())){
                    String errorMsg = "";
                    int i = 1;
                    for (RegistryError err : rsp.getRegistryErrorList().getRegistryError()) {
                        errorMsg += (i++)+") "+err.getErrorCode()+" : "+err.getCodeContext()+"\n";
                    }
                    throw new RuntimeException(errorMsg);
                } 
        }
    }

    public static SubmitObjectsRequest getSubmitObjectsRequest(String metadataFilename, boolean defaultInit) throws JAXBException, FileNotFoundException {
        JAXBContext jaxbContext = JAXBContext.newInstance(SubmitObjectsRequest.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        SubmitObjectsRequest req = (SubmitObjectsRequest) unmarshaller.unmarshal(
                defaultInit ? XDSInitCommon.class.getResourceAsStream(metadataFilename) :
                    new FileInputStream(metadataFilename));
        return req;
    }

}
