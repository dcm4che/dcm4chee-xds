package org.dcm4chee.xds2.infoset.util;

import java.util.List;
import java.util.Map;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.soap.SOAPBinding;

public class BasePortTypeFactory {

    protected final static String URN_IHE_ITI = "urn:ihe:iti:xds-b:2007";
    protected static final String URN_IHE_RAD_XDSI_B_2009 = "urn:ihe:rad:xdsi-b:2009";
    protected static final String URN_IHE_RAD_2009 = "urn:ihe:rad:2009";

    /**
     * Configures the port with the default handlers, endpoint and mtom.<br/>
     * <br/>
     * <b>ATTENTION:</b> If <i>mtom</i> is enabled, you need to provide a handler that forces MTOM
     * to be done manually. There is a bug in the JDK classes which prevents MTOM and handlers to
     * work at the same time. For more information about this issue, see <a
     * href="https://java.net/jira/browse/WSIT-1320">this link</a>.
     * 
     * @param bindingProvider the current binding provider
     * @param endpointAddress the target address
     * @param mtom enables mtom
     * @param mustUnderstand sets the must understand flag in certain headers, see {@link EnsureMustUnderstandHandler}
     * @param addLogHandler add the log handler
     */
    public static void configurePort(BindingProvider bindingProvider, String endpointAddress, boolean mtom, boolean mustUnderstand, boolean addLogHandler) {
        SOAPBinding binding = (SOAPBinding) bindingProvider.getBinding(); 
        binding.setMTOMEnabled(mtom);
        if (mustUnderstand)
            addHandler(bindingProvider, new EnsureMustUnderstandHandler());
        if (addLogHandler)
            addHandler(bindingProvider, new SentSOAPLogHandler());
        Map<String, Object> reqCtx = bindingProvider.getRequestContext();
        reqCtx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
    }
    
    /**
     * Adds a new handler to the given {@link BindingProvider}. This method also keeps all the
     * handlers which are already there.
     * 
     * @param bindingProvider
     *        the {@link BindingProvider} to add the handler to
     * @param handler
     *        the handler to add
     */
    public static void addHandler(BindingProvider bindingProvider, SOAPHandler<?> handler) {
        SOAPBinding binding = (SOAPBinding) bindingProvider.getBinding();
        @SuppressWarnings("rawtypes")
        List<Handler> currentHandlers = binding.getHandlerChain();
        currentHandlers.add(handler);
        binding.setHandlerChain(currentHandlers);
    }
}
