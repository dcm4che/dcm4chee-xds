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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPHeader;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.conf.XdsDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.w3c.dom.NodeList;

public class LogHandler implements SOAPHandler<SOAPMessageContext> {
    private Set<QName> headers = new HashSet<QName>();
    private static final char sepChar = File.separatorChar;
    
    private static ThreadLocal<SOAPHeader> soapHeader = new ThreadLocal<SOAPHeader>();

    private static Logger log = LoggerFactory.getLogger(LogHandler.class);
    
    @Override
    public boolean handleMessage(SOAPMessageContext ctx) {
        log.debug("##########handleMessage LogHandler:"+ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY));
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
        log.debug("################ close");
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
        String action = getWsaHeader(ctx, "Action", "noAction");
        String msgID = getWsaHeader(ctx, "MessageID", null);
        String logDir = null;
        boolean logFullMessage = false;
        try {
            if ((action.endsWith(":RegisterDocumentSet-b") ||
                 action.endsWith(":RegisterDocumentSet-bResponse")) && XdsDevice.getXdsRegistry() != null) {
                logDir = XdsDevice.getXdsRegistry().getSoapLogDir();
            } else if ((action.endsWith(":CrossGatewayQuery") ||
                        action.endsWith(":CrossGatewayQueryResponse")) && XdsDevice.getXCARespondingGW() != null) {
                logDir = XdsDevice.getXCARespondingGW().getSoapLogDir();
            } else if (XdsDevice.getXdsRepository() != null) {
                logDir = XdsDevice.getXdsRepository().getSoapLogDir();
            }
/*            String [] hosts = XdsDevice.getXdsRepository().getLogFullMessageHosts(); 
            if (hosts != null && hosts.length > 0) {
                logFullMessage = true;
            }*/
        } catch (Exception ignore) {
            log.warn("Failed to get logDir from XDS configuration!", ignore);
        }
        if (logDir != null) {
            FileOutputStream out = null;
            try {
                File f;
                if (((Boolean)ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY))) {
                    logDir = MDC.get("initiatorLogDir");
                    f = new File(logDir, action + ".xml");
                } else {
                    f = getLogFile(getHost(ctx), action, msgID, ".xml", logDir);
                    f.getParentFile().mkdirs();
                }
                out = new FileOutputStream(f);
                if (logFullMessage) {
                    ctx.getMessage().writeTo(out);
                } else {
                    Source s = ctx.getMessage().getSOAPPart().getContent();
                    Transformer t = TransformerFactory.newInstance().newTransformer();
                    t.setOutputProperty("indent", "yes");
                    t.transform(s, new StreamResult(out));
                }
                log.info("SOAP message saved to file "+f);
            } catch (Exception x) {
                log.error("Error logging SOAP message to file!", x);
            } finally {
                if (out != null)
                    try {
                        out.close();
                    } catch (IOException ignore) {}
            }
        }
        if (((Boolean)ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY))) {
            MDC.put("initiatorFinished", "true");
            log.info("SOAP message "+getWsaHeader(ctx, "RelatesTo", null)+" finished!");
            MDC.remove("initiatorLogDir");
            MDC.remove("initiatorMsgID");
            MDC.remove("initiatorFinished");
        } else {
            log.info("Start processing SOAP message "+msgID);
        }
    }

    private String getWsaHeader(SOAPMessageContext ctx, String name, String def) {
        try {
            SOAPHeader hdr =ctx.getMessage().getSOAPHeader();
            NodeList nodeList = hdr.getElementsByTagNameNS(XDSConstants.WS_ADDRESSING_NS, name);
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

    private String getHost(SOAPMessageContext ctx) {
        HttpServletRequest rq =(HttpServletRequest)ctx.get(SOAPMessageContext.SERVLET_REQUEST);
        String ip;
        String xForward = (String) rq.getHeader("x-forwarded-for");
        if (xForward != null) {
            int pos = xForward.indexOf(',');
            ip = (pos > 0 ? xForward.substring(0,pos) : xForward).trim();
        } else {
            ip = rq.getRemoteHost();
        }
        if (Boolean.valueOf(System.getProperty("org.dcm4chee.disableDNSLookups", "false")))
            return ip;
        try {
            return InetAddress.getByName(ip).getHostName();
        } catch (UnknownHostException ignore) {
            return ip;
        }
    }

    private File getLogFile(String host, String action, String msgID, String extension, String logDir) {
        Calendar cal = Calendar.getInstance();
        msgID = msgID == null ? "xxxx" : Integer.toHexString(msgID.hashCode());
        StringBuilder sb = new StringBuilder();
        sb.append(logDir).append(sepChar).append(cal.get(Calendar.YEAR))
        .append(sepChar).append(cal.get(Calendar.MONTH)+1).append(sepChar)
        .append(cal.get(Calendar.DAY_OF_MONTH)).append(sepChar).append(host)
        .append(sepChar).append(action).append('_').append(msgID);
        File dir = new File(sb.toString());
        MDC.put("initiatorLogDir", dir.getAbsolutePath());
        MDC.put("initiatorMsgID", msgID);
        MDC.put("remoteHost", host);
        log.info("set MDC remoteHost:"+MDC.get("remoteHost"));
        return new File(dir, action+".xml");
    }

}
