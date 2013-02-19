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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPHeader;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.conf.XdsDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

public class LogHandler implements SOAPHandler<SOAPMessageContext> {
    private Set<QName> headers = new HashSet<QName>();
    private static final char sepChar = File.separatorChar;
    
    private static ThreadLocal<SOAPHeader> soapHeader = new ThreadLocal<SOAPHeader>();

    private static Logger log = LoggerFactory.getLogger(LogHandler.class);
    
    @Override
    public boolean handleMessage(SOAPMessageContext ctx) {
        log.info("##########handleMessage LogHandler:"+ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY));
        storeInboundSOAPHeader(ctx);
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
        log.info("################ close");
    }

    @Override
    public Set<QName> getHeaders() {
        log.debug("################ getHeaders");
        return headers;
    }
    
    public static SOAPHeader getInboundSOAPHeader() {
        return soapHeader.get();
    }

    private void storeInboundSOAPHeader(SOAPMessageContext ctx) {
        if (!(Boolean)ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)) {
            try {
                if (soapHeader.get() != null) {
                    log.warn("Inbound SOAPHeader already set! SOAPHeader:"+soapHeader.get());
                }
                soapHeader.set(ctx.getMessage().getSOAPHeader());
                log.debug("Saved inbound SOAPHeader:"+soapHeader.get());
            } catch (Exception x) {
                log.warn("Failed to save SOAP Header to ThreadLocal!", x);
            }
        }
    }

    private void logMessage(SOAPMessageContext ctx) {
        String action = getAction(ctx);
        String host = getHost(ctx);
        String logDir = XdsDevice.getXdsRegistry().getSoapLogDir();
        if (logDir != null) {
            FileOutputStream out = null;
            try {
                File f = getLogFile(host, action, ".xml", logDir);
                f.getParentFile().mkdirs();
                log.info("SOAP message saved to file "+f);
                out = new FileOutputStream(f);
                ctx.getMessage().writeTo(out);//On registry we don't need take care for attachments!
            } catch (Exception x) {
                log.error("Error logging SOAP message to file!", x);
            } finally {
                if (out != null)
                    try {
                        out.close();
                    } catch (IOException ignore) {}
            }
        }
    }

    private String getAction(SOAPMessageContext ctx) {
        try {
            SOAPHeader hdr =ctx.getMessage().getSOAPHeader();
            NodeList nodeList = hdr.getElementsByTagNameNS(XDSConstants.WS_ADDRESSING_NS, "Action");
            if (nodeList.getLength() == 0) {
                return "noAction";
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

    private String getHost(SOAPMessageContext ctx) {
        HttpServletRequest rq =(HttpServletRequest)ctx.get(SOAPMessageContext.SERVLET_REQUEST);
        String host;
        String xForward = (String) rq.getHeader("x-forwarded-for");
        if (xForward != null) {
            int pos = xForward.indexOf(',');
            host = (pos > 0 ? xForward.substring(0,pos) : xForward).trim();
        } else {
            host = rq.getRemoteAddr();
        }
        return host;
    }

    private File getLogFile(String host, String action, String extension, String logDir) {
        Calendar cal = Calendar.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append(logDir).append(sepChar).append(cal.get(Calendar.YEAR))
        .append(sepChar).append(cal.get(Calendar.MONTH)+1).append(sepChar)
        .append(cal.get(Calendar.DAY_OF_MONTH)).append(sepChar).append(host)
        .append(sepChar).append(action).append('_')
        .append(Integer.toHexString((int)cal.getTimeInMillis()))
        .append(extension);
        
        return new File(sb.toString());
    }

}
