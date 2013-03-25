package org.dcm4chee.xds2.infoset.util;

import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.AddressingFeature;

import org.dcm4chee.xds2.infoset.ws.xca.RespondingGatewayPortType;
import org.dcm4chee.xds2.infoset.ws.xca.RespondingGatewayService;

public class RespondingGatewayPortTypeFactory {

    protected static RespondingGatewayService service = null;
    private final static URL WSDL_LOCATION = RespondingGatewayService.class.getResource("/wsdl/XCARespondingGateway.wsdl");

    static {
        service = new RespondingGatewayService(WSDL_LOCATION, new QName("urn:ihe:iti:xds-b:2007", "RespondingGateway_Service"));
        service.getRespondingGatewayPortSoap12();
    }

    public static RespondingGatewayPortType getRespondingGatewayPortSoap12() {
        return service.getPort(new QName("urn:ihe:iti:xds-b:2007", "RespondingGateway_Port_Soap12"), 
                RespondingGatewayPortType.class, new AddressingFeature(true, true));
    }

    public static RespondingGatewayPortType getRespondingGatewayPortSoap12(String endpointAddress) {
        RespondingGatewayPortType port = getRespondingGatewayPortSoap12();
        configurePort(port, endpointAddress);
        return port;
    }

    public static void configurePort(RespondingGatewayPortType port, String endpointAddress) {
        BindingProvider bindingProvider = (BindingProvider)port;
        Map<String, Object> reqCtx = bindingProvider.getRequestContext();
        reqCtx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
    }	
}
