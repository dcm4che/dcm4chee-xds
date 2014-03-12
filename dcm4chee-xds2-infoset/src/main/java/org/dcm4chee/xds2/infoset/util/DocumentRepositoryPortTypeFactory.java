package org.dcm4chee.xds2.infoset.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.SOAPBinding;

import org.dcm4chee.xds2.infoset.ws.repository.DocumentRepositoryPortType;
import org.dcm4chee.xds2.infoset.ws.repository.DocumentRepositoryService;

public class DocumentRepositoryPortTypeFactory extends BasePortTypeFactory {

    protected static DocumentRepositoryService service = null;
    private final static URL WSDL_LOCATION = DocumentRepositoryService.class.getResource("/wsdl/XDS.b_DocumentRepository.wsdl");

    static {
        service = new DocumentRepositoryService(WSDL_LOCATION, new QName(URN_IHE_ITI, "DocumentRepository_Service"));
        service.getDocumentRepositoryPortSoap12();
    }

    public static DocumentRepositoryPortType getDocumentRepositoryPortSoap12() {
        return service.getPort(new QName(URN_IHE_ITI, "DocumentRepository_Port_Soap12"), 
                DocumentRepositoryPortType.class, new AddressingFeature(true, true));
    }

    public static DocumentRepositoryPortType getDocumentRepositoryPortSoap12(String endpointAddress) {
        DocumentRepositoryPortType port = getDocumentRepositoryPortSoap12();
        configurePort((BindingProvider)port, endpointAddress, true, true, true);
        return port;
    }
}
