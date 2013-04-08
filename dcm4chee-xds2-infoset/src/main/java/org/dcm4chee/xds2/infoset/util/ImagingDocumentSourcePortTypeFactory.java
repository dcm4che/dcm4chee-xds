package org.dcm4chee.xds2.infoset.util;

import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.AddressingFeature;

import org.dcm4chee.xds2.infoset.ws.src.ImagingDocumentSource;
import org.dcm4chee.xds2.infoset.ws.src.ImagingDocumentSourcePortType;
import org.dcm4chee.xds2.infoset.ws.xca.RespondingGatewayService;

public class ImagingDocumentSourcePortTypeFactory {

    protected static ImagingDocumentSource service = null;
    private final static URL WSDL_LOCATION = RespondingGatewayService.class.getResource("/wsdl/XDS-I.b_ImagingDocumentSource.wsdl");

    static {
        service = new ImagingDocumentSource(WSDL_LOCATION, new QName("urn:ihe:rad:xdsi-b:2009", "ImagingDocumentSource"));
        service.getImagingDocumentSourcePortSoap12();
    }

    public static ImagingDocumentSourcePortType getImagingDocumentSourcePort() {
        return service.getPort(new QName("urn:ihe:rad:xdsi-b:2009", "ImagingDocumentSource_Port_Soap12"), 
                ImagingDocumentSourcePortType.class, new AddressingFeature(true, true));
    }

    public static ImagingDocumentSourcePortType getImagingDocumentSourcePort(String endpointAddress) {
        ImagingDocumentSourcePortType port = getImagingDocumentSourcePort();
        configurePort(port, endpointAddress);
        return port;
    }

    public static void configurePort(ImagingDocumentSourcePortType port, String endpointAddress) {
        BindingProvider bindingProvider = (BindingProvider)port;
        Map<String, Object> reqCtx = bindingProvider.getRequestContext();
        reqCtx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
    }	
}
