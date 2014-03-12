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
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
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

package org.dcm4chee.xds2.ws.handler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.dcm4chee.xds2.conf.XdsDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForceMTOMHandler implements SOAPHandler<SOAPMessageContext> {
    private static Logger log = LoggerFactory.getLogger(ForceMTOMHandler.class);
    
    @Override
    public boolean handleMessage(SOAPMessageContext ctx) {
        boolean outbound = (Boolean)ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        log.debug("##########handleMessage ForceMTOMHandler:"+outbound);
        SOAPMessage msg = ctx.getMessage();
        if (outbound) {
        	try {
	            if (msg.countAttachments() == 0 && XdsDevice.getXdsRepository().isForceMTOM()) {
	                try {
	                    @SuppressWarnings("unchecked")
	                    Map<String, List<String>> headers = (Map<String, List<String>>) ctx.get(SOAPMessageContext.HTTP_REQUEST_HEADERS);
	                    if (headers.get("content-type").get(0).indexOf("application/xop+xml") != -1) {
	                        log.info("FORCE MTOM/XOP Response Message!");
	                        ByteArrayDataSource DUMMY_PLAIN_DATA_SOURCE = 
	                           new ByteArrayDataSource("Force MTOM/XOP to make 'important vendor' happy.\nThanks for forcing me to add this completly needless stuff to a simple SOAP message!", "text/plain");
	                        msg.addAttachmentPart(msg.createAttachmentPart(new DataHandler(DUMMY_PLAIN_DATA_SOURCE)));
	                    }
	                } catch (IOException x) {
	                    log.error("FORCE MTOM/XOP failed!", x);
	                }
	            }
        	} catch (Exception x) {
        		log.warn("Error in forcing MTOM response! Reply anyway!");
        	}
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext ctx) {
        log.warn("################ handleFault");
        return true;
    }

    @Override
    public void close(MessageContext context) {
        log.debug("################ close");
    }


    @Override
    public Set<QName> getHeaders() {
        return null;
    }

}
