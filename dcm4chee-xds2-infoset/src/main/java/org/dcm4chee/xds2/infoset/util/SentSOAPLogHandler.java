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

package org.dcm4chee.xds2.infoset.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPHeader;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.w3c.dom.NodeList;

public class SentSOAPLogHandler implements SOAPHandler<SOAPMessageContext> {
    private Set<QName> headers = new HashSet<QName>();
    private static final char sepChar = File.separatorChar;
    
    private static Logger log = LoggerFactory.getLogger(SentSOAPLogHandler.class);
    
    @Override
    public boolean handleMessage(SOAPMessageContext ctx) {
        log.debug("##########handleMessage LogHandler:"+ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY));
        logMessage(ctx);
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext ctx) {
        log.warn("################ handleFault");
        logMessage(ctx);
        return true;
    }

    @Override
    public void close(MessageContext context) {
        log.debug("################ close");
    }

    @Override
    public Set<QName> getHeaders() {
        log.debug("################ getHeaders");
        return headers;
    }
    
    private void logMessage(SOAPMessageContext ctx) {
        String action = getWsaHeader(ctx, "Action", "noAction");
        FileOutputStream out = null;
        try {
            String msgID = ((Boolean)ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)) ? 
                    getWsaHeader(ctx, "MessageID", null) : getWsaHeader(ctx, "RelatesTo", null);
            File f = getLogFile(action, msgID);
            f.getParentFile().mkdirs();
            log.info("sent SOAP message saved to file "+f);
            out = new FileOutputStream(f);
            Source s = ctx.getMessage().getSOAPPart().getContent();
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty("indent", "yes");
            t.transform(s, new StreamResult(out));
        } catch (Exception x) {
            log.error("Error logging sent SOAP message to file!", x);
        } finally {
            if (out != null)
                try {
                    out.close();
                } catch (IOException ignore) {}
        }
    }

    private File getLogFile(String action, String msgID) {
        String dir = MDC.get("initiatorLogDir"); 
        if (dir == null) {
            Calendar cal = Calendar.getInstance();
            StringBuilder sb = new StringBuilder();
            sb.append("/var/log/xdslog/sentMessages").append(sepChar).append(cal.get(Calendar.YEAR))
            .append(sepChar).append(cal.get(Calendar.MONTH)+1).append(sepChar)
            .append(cal.get(Calendar.DAY_OF_MONTH));
            dir = sb.toString();
            String initiator = MDC.get("initiatorMsgID");
            return new File(dir, "sent_"+action+"_"+initiator+"#"+msgID+".xml");
        }        
        return new File(dir, "sent_"+action+"_"+msgID+".xml");
    }
    private String getWsaHeader(SOAPMessageContext ctx, String name, String def) {
        try {
            SOAPHeader hdr =ctx.getMessage().getSOAPHeader();
            NodeList nodeList = hdr.getElementsByTagNameNS("http://www.w3.org/2005/08/addressing", name);
            if (nodeList.getLength() == 0) {
                return def;
            }
            String action = nodeList.item(0).getTextContent();
            //remove 'urn:ihe:iti:2007:' to avoid ':' in filename!
            int pos = action.lastIndexOf(':');
            if (pos != -1)
                action = action.substring(++pos);
            return action;
        } catch (Exception x) {
            return "errorGetSOAPHeader";
        }
    }

}
