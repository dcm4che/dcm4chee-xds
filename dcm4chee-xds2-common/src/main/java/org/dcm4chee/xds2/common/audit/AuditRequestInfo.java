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
package org.dcm4chee.xds2.common.audit;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.SOAPHeader;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.dcm4che.audit.AuditMessages;
import org.dcm4chee.xds2.common.XDSConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;


/**
 * Request Information for XDS audit logging.
 * 
 * @author franz.willer@gmail.com
 *
 */
public class AuditRequestInfo {

    private static final String UNKNOWN = "UNKNOWN";
    private SOAPHeader soapHeader;
    private HttpServletRequest servletRequest;
    String host;
    private boolean enableDNSLookups = Boolean.valueOf(
            System.getProperty("org.dcm4chee.xds.audit.enableDNSLookups", "true"));
    
    public static final Logger log = LoggerFactory.getLogger(AuditRequestInfo.class);
    
    public AuditRequestInfo(SOAPHeader hdr, WebServiceContext wsContext) {
        soapHeader = hdr;
        try {
            servletRequest = (HttpServletRequest)wsContext.getMessageContext().get(SOAPMessageContext.SERVLET_REQUEST);
        } catch (Exception x) {
            log.warn("Failed to get ServletRequest from WebServiceContext!");
            log.debug("Stacktrace:", x);
        }
    }

    public SOAPHeader getSoapHeader() {
        return soapHeader;
    }

    public HttpServletRequest getServletRequest() {
        return servletRequest;
    }

    public String getRemoteHost() {
        if (host != null)
            return host;
        if (servletRequest != null) {
            String xForward = (String) servletRequest.getHeader("x-forwarded-for");
            if (xForward != null) {
                int pos = xForward.indexOf(',');
                host = (pos > 0 ? xForward.substring(0,pos) : xForward).trim();
            } else {
                host = servletRequest.getRemoteAddr();
            }
            if ( enableDNSLookups ) {
                try {
                    host =  InetAddress.getByName(host).getHostName();
                } catch (UnknownHostException ignore) {
                }
            }
            return host;
        }
        return UNKNOWN;
    }

    public String getLocalHost() {
        if (servletRequest != null) {
            String host = servletRequest.getLocalName();
            if (host == null)
                host = servletRequest.getLocalAddr();
            if (this.enableDNSLookups && AuditMessages.isIP(host)) {
                try {
                    host = InetAddress.getByName(host).getHostName();
                } catch (UnknownHostException e) {
                }
            }
            return host;
        }
        return UNKNOWN;
    }

    public String getReplyTo() {
        if (soapHeader != null) {
            NodeList replyToNodes = soapHeader.getElementsByTagNameNS(XDSConstants.WS_ADDRESSING_NS, "ReplyTo");
            if (replyToNodes.getLength() > 0) {
                return replyToNodes.item(0).getTextContent();
            }
        }
        return XDSConstants.WS_ADDRESSING_ANONYMOUS;
    }

    public String getRequestURI() {
        if (soapHeader != null) {
            NodeList toNodes = soapHeader.getElementsByTagNameNS(XDSConstants.WS_ADDRESSING_NS, "To");
            if (toNodes.getLength() > 0) {
                return toNodes.item(0).getTextContent();
            }
        }
        if (servletRequest != null) {
            return "http://"+getLocalHost()+":"+servletRequest.getLocalPort()+
                servletRequest.getRequestURI();
        }
        return UNKNOWN;
    }
    
    public String getRemoteUser() {
        return servletRequest == null ? null : servletRequest.getRemoteUser(); 
    }
}
