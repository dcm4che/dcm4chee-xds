package org.dcm4chee.xds2.infoset.util;

import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.AddressingFeature;

import org.dcm4chee.xds2.infoset.ws.src.ImagingDocumentSource;
import org.dcm4chee.xds2.infoset.ws.src.ImagingDocumentSourcePortType;
import org.dcm4chee.xds2.infoset.ws.xca.RespondingGatewayService;

public class ImagingDocumentSourcePortTypeFactory extends BasePortTypeFactory {

    protected static ImagingDocumentSource service = null;
    private final static URL WSDL_LOCATION = RespondingGatewayService.class.getResource("/wsdl/XDS-I.b_ImagingDocumentSource.wsdl");

    static {
        service = new ImagingDocumentSource(WSDL_LOCATION, new QName(URN_IHE_RAD_XDSI_B_2009, "ImagingDocumentSource"));
        service.getImagingDocumentSourcePortSoap12();
    }

    public static ImagingDocumentSourcePortType getImagingDocumentSourcePort() {
        return service.getPort(new QName(URN_IHE_RAD_XDSI_B_2009, "ImagingDocumentSource_Port_Soap12"), 
                ImagingDocumentSourcePortType.class, new AddressingFeature(true, true));
    }

    public static ImagingDocumentSourcePortType getImagingDocumentSourcePort(String endpointAddress) {
        ImagingDocumentSourcePortType port = getImagingDocumentSourcePort();
        configurePort((BindingProvider)port, endpointAddress, true, true, true);
        return port;
    }
}
