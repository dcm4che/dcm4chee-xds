/*
 *  ***** BEGIN LICENSE BLOCK ***** Version: MPL 1.1/GPL 2.0/LGPL 2.1
 * 
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 * 
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in Java(TM), available at
 * http://sourceforge.net/projects/dcm4che.
 * 
 * The Initial Developer of the Original Code is TIANI Medgraph AG. Portions created by the Initial
 * Developer are Copyright (C) 2003-2005 the Initial Developer. All Rights Reserved.
 * 
 * Contributor(s): Gunter Zeilinger <gunter.zeilinger@tiani.com> Franz Willer
 * <franz.willer@gmail.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of either the GNU General
 * Public License Version 2 or later (the "GPL"), or the GNU Lesser General Public License Version
 * 2.1 or later (the "LGPL"), in which case the provisions of the GPL or the LGPL are applicable
 * instead of those above. If you wish to allow use of your version of this file only under the
 * terms of either the GPL or the LGPL, and not to allow others to use your version of this file
 * under the terms of the MPL, indicate your decision by deleting the provisions above and replace
 * them with the notice and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under the terms of any one of
 * the MPL, the GPL or the LGPL.
 * 
 * ***** END LICENSE BLOCK *****
 */
package org.dcm4chee.xds2.src.ws;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;

import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.infoset.util.DocumentRepositoryPortTypeFactory;
import org.dcm4chee.xds2.infoset.ws.repository.DocumentRepositoryPortType;
import org.dcm4chee.xds2.src.metadata.PnRRequest;
import org.dcm4chee.xds2.src.metadata.exception.MetadataConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Code (without coding scheme version, equality only by value and designator).
 * 
 * @author franz.willer@agfa.com
 * 
 */
public class XDSSourceWSClient {

    //only effective if webservice stack supports javax.xml.ws.client.connectionTimeout or BindingProviderProperties.CONNECT_TIMEOUT property in BindingProvider (jbossws-3.4.0, JBoss7)
    private int           connectionTimeout = -1;
    private int           receiveTimeout    = -1;
    private String logDir;
    
    private static Logger log               = LoggerFactory.getLogger(XDSSourceWSClient.class);

    public XDSSourceWSClient() {
    }

    public XDSSourceWSClient(String logDir) {
        this.logDir = logDir;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setReceiveTimeout(int receiveTimeout) {
        this.receiveTimeout = receiveTimeout;
    }

    public int getReceiveTimeout() {
        return receiveTimeout;
    }

    public String getLogDir() {
        return logDir;
    }

    public void setLogDir(String logDir) {
        this.logDir = logDir;
    }

    /**
     * Sends a webservice request to the given URL and allows the addition of handlers for e.g. logging.
     * 
     * @param req the request to be sent
     * @param xdsRegistryURI the destination URL
     * @param handlers a list of handlers to be process while executing the request
     * @return a response object containing the answer from the given webservice
     * @throws MetadataConfigurationException
     */
    public RegistryResponseType
        sendProvideAndRegister(PnRRequest req, URL xdsRegistryURI, @SuppressWarnings("rawtypes") List<Handler> handlers) throws MetadataConfigurationException {
        DocumentRepositoryPortType port = DocumentRepositoryPortTypeFactory.getDocumentRepositoryPortSoap12(xdsRegistryURI.toString(), logDir);
        Map<String, Object> ctx = ((BindingProvider) port).getRequestContext();
        if (handlers != null && handlers.size() > 0) {
            addHandlers(port, handlers);
        }
        if (connectionTimeout >= 0) {
            ctx.put("javax.xml.ws.client.connectionTimeout", String.valueOf(connectionTimeout));
            ctx.put("com.sun.xml.ws.connect.timeout", connectionTimeout);
            ctx.put("com.sun.xml.internal.ws.connect.timeout", connectionTimeout);
            log.debug("Set connectionTimeout to {}", connectionTimeout);
        }
        if (receiveTimeout >= 0) {
            ctx.put("javax.xml.ws.client.receiveTimeout", String.valueOf(receiveTimeout));
            ctx.put("com.sun.xml.internal.ws.request.timeout", receiveTimeout);
            ctx.put("com.sun.xml.ws.request.timeout", receiveTimeout);
            log.debug("Set receiveTimeout to {}", receiveTimeout);
        }
        log.debug("Send {}", req);
        RegistryResponseType rsp = port.documentRepositoryProvideAndRegisterDocumentSetB(req.createInfoset());
        log.debug("Send PnR request finished! response:{}", rsp.getStatus());
        return rsp;
    }
    
    @SuppressWarnings("rawtypes")
    public RegistryResponseType sendProvideAndRegister(PnRRequest req, URL xdsRegistryURI) throws MetadataConfigurationException {
        return sendProvideAndRegister(req, xdsRegistryURI, Collections.<Handler>emptyList());
    }

    @SuppressWarnings("rawtypes")
    private void addHandlers(DocumentRepositoryPortType port, List<Handler> handlers) {
        Binding bind = ((BindingProvider) port).getBinding();
        List<Handler> currentHandlers = bind.getHandlerChain();
        if (currentHandlers == null) {
            currentHandlers = new ArrayList<Handler>();
        }
        for (Handler<?> handler : handlers) {
            if(handler!=null) {
                currentHandlers.add(handler);
            }
        }
        bind.setHandlerChain(currentHandlers);
    }
}
