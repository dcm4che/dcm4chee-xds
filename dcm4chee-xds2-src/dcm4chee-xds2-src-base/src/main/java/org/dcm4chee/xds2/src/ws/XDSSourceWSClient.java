/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), available at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * TIANI Medgraph AG.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunter.zeilinger@tiani.com>
 * Franz Willer <franz.willer@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package org.dcm4chee.xds2.src.ws;

import java.net.URL;
import java.util.Map;

import javax.xml.ws.BindingProvider;

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

    //only effective if webservice stack supports javax.xml.ws.client.connectionTimeout property in BindingProvider (jbossws-3.4.0, JBoss7)
    private int connectionTimeout = -1;
    private int receiveTimeout = -1;
    
    private static Logger log = LoggerFactory.getLogger(XDSSourceWSClient.class);
    
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

    public RegistryResponseType sendProvideAndRegister(PnRRequest req, URL xdsRegistryURI) throws MetadataConfigurationException {
        DocumentRepositoryPortType port = DocumentRepositoryPortTypeFactory.getDocumentRepositoryPortSoap12(xdsRegistryURI.toString());
        if (connectionTimeout >= 0) {
            ((BindingProvider)port).getRequestContext().put("javax.xml.ws.client.connectionTimeout", String.valueOf(connectionTimeout));
            log.debug("Set connectionTimeout to {}", connectionTimeout);
        }
        if (receiveTimeout >= 0) {
            ((BindingProvider)port).getRequestContext().put("javax.xml.ws.client.receiveTimeout", String.valueOf(receiveTimeout));
            log.debug("Set receiveTimeout to {}", receiveTimeout);
        }
        log.debug("Send {}", req);
        RegistryResponseType rsp = port.documentRepositoryProvideAndRegisterDocumentSetB(req.createInfoset());
        log.debug("Send PnR request finished! response:{}", rsp.getStatus());
        return rsp;
    }
    
}