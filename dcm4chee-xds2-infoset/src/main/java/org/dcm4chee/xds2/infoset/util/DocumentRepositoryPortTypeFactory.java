package org.dcm4chee.xds2.infoset.util;

import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.SOAPBinding;

import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryPortType;
import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryService;
import org.dcm4chee.xds2.infoset.ws.repository.DocumentRepositoryPortType;
import org.dcm4chee.xds2.infoset.ws.repository.DocumentRepositoryService;

public class DocumentRepositoryPortTypeFactory {

    protected static DocumentRepositoryService service = null;
    private final static URL WSDL_LOCATION = DocumentRepositoryService.class.getResource("/wsdl/XDS.b_DocumentRepository.wsdl");

    static {
        service = new DocumentRepositoryService(WSDL_LOCATION, new QName("urn:ihe:iti:xds-b:2007", "DocumentRepository_Service"));
        service.getDocumentRepositoryPortSoap12();
    }

    public static DocumentRepositoryPortType getDocumentRepositoryPortSoap12() {
        return service.getPort(new QName("urn:ihe:iti:xds-b:2007", "DocumentRepository_Port_Soap12"), 
                DocumentRepositoryPortType.class, new AddressingFeature(true, true));
    }

    public static DocumentRepositoryPortType getDocumentRepositoryPortSoap12(String endpointAddress) {
        DocumentRepositoryPortType port = getDocumentRepositoryPortSoap12();
        configurePort(port, endpointAddress);
        return port;
    }

    public static void configurePort(DocumentRepositoryPortType port, String endpointAddress) {
        BindingProvider bindingProvider = (BindingProvider)port;
        SOAPBinding binding = (SOAPBinding) bindingProvider.getBinding(); 
        binding.setMTOMEnabled(true);
        Map<String, Object> reqCtx = bindingProvider.getRequestContext();
        reqCtx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
    }	
}
