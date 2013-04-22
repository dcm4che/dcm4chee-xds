package org.dcm4chee.xds2.infoset.util;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.AddressingFeature;

import org.dcm4chee.xds2.infoset.ws.xca.RespondingGatewayService;
import org.dcm4chee.xds2.infoset.ws.xca.XCAIRespondingGateway;
import org.dcm4chee.xds2.infoset.ws.xca.XCAIRespondingGatewayPortType;

public class XCAiRespondingGatewayPortTypeFactory extends BasePortTypeFactory {

    protected static XCAIRespondingGateway service = null;
    private final static URL WSDL_LOCATION = RespondingGatewayService.class.getResource("/wsdl/XCA-I-RespondingGateway.wsdl");

    static {
        service = new XCAIRespondingGateway(WSDL_LOCATION, new QName("urn:ihe:rad:xdsi-b:2009", "XCAIRespondingGateway"));
        service.getXCAIRespondingGatewayPortSoap12();
    }

    public static XCAIRespondingGatewayPortType getXCAIRespondingGatewayPortSoap12() {
        return service.getPort(new QName("urn:ihe:rad:xdsi-b:2009", "XCAIRespondingGateway_Port_Soap12"), 
                XCAIRespondingGatewayPortType.class, new AddressingFeature(true, true));
    }

    public static XCAIRespondingGatewayPortType getXCAIRespondingGatewayPortSoap12(String endpointAddress) {
        XCAIRespondingGatewayPortType port = getXCAIRespondingGatewayPortSoap12();
        configurePort((BindingProvider)port, endpointAddress, false, true, true);
        return port;
    }
}
