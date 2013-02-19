package org.dcm4chee.xds2.infoset.util;

import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.AddressingFeature;

import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryPortType;
import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryService;

public class DocumentRegistryPortTypeFactory {

    protected static DocumentRegistryService service = null;
    private final static URL WSDL_LOCATION = DocumentRegistryService.class.getResource("/wsdl/XDS.b_DocumentRegistry.wsdl");

    static {
        service = new DocumentRegistryService(WSDL_LOCATION, new QName("urn:ihe:iti:xds-b:2007", "DocumentRegistry_Service"));
        service.getDocumentRegistryPortSoap12();
    }

    public static DocumentRegistryPortType getDocumentRegistryPortSoap12() {
        return service.getPort(new QName("urn:ihe:iti:xds-b:2007", "DocumentRegistry_Port_Soap12"), DocumentRegistryPortType.class, new AddressingFeature());
    }

    public static DocumentRegistryPortType getDocumentRegistryPortSoap12(String endpointAddress) {
        DocumentRegistryPortType port = getDocumentRegistryPortSoap12();
        configurePort(port, endpointAddress);
        return port;
    }

    public static void configurePort(DocumentRegistryPortType port, String endpointAddress) {
        BindingProvider bindingProvider = (BindingProvider)port;
        Map<String, Object> reqCtx = bindingProvider.getRequestContext();
        reqCtx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
    }	
}
