package org.dcm4chee.xds2.infoset.util;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.AddressingFeature;

import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryPortType;
import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryService;

public class DocumentRegistryPortTypeFactory extends BasePortTypeFactory{

    protected static DocumentRegistryService service = null;
    private final static URL WSDL_LOCATION = DocumentRegistryService.class.getResource("/wsdl/XDS.b_DocumentRegistry.wsdl");

    static {
        service = new DocumentRegistryService(WSDL_LOCATION, new QName(URN_IHE_ITI, "DocumentRegistry_Service"));
        service.getDocumentRegistryPortSoap12();
    }

    public static DocumentRegistryPortType getDocumentRegistryPortSoap12() {
        return service.getPort(new QName(URN_IHE_ITI, "DocumentRegistry_Port_Soap12"), 
                DocumentRegistryPortType.class, new AddressingFeature(true, true));
    }

    public static DocumentRegistryPortType getDocumentRegistryPortSoap12(String endpointAddress) {
        DocumentRegistryPortType port = getDocumentRegistryPortSoap12();
        configurePort((BindingProvider)port, endpointAddress, false, true, true);
        return port;
    }
}
