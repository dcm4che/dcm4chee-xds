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
 * Portions created by the Initial Developer are Copyright (C) 2012
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

package org.dcm4chee.xds2.ctrl;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.audit.AuditLogger;
import org.dcm4che3.net.audit.AuditRecordRepository;
import org.dcm4che3.net.hl7.HL7Application;
import org.dcm4che3.net.hl7.HL7DeviceExtension;
import org.dcm4che3.util.StringUtils;
import org.dcm4chee.xds2.conf.XCAInitiatingGWCfg;
import org.dcm4chee.xds2.conf.XCARespondingGWCfg;
import org.dcm4chee.xds2.conf.XCAiInitiatingGWCfg;
import org.dcm4chee.xds2.conf.XCAiRespondingGWCfg;
import org.dcm4chee.xds2.conf.XdsRegistry;
import org.dcm4chee.xds2.conf.XdsRepository;
import org.dcm4chee.xds2.service.XdsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/ctrl")
@RequestScoped
public class XdsDeviceCtrl {

    private static final String APPLICATION_NAME = "Application Name:";
    private static final String NOT_CONFIGURED = "Not configured!";
    private static String ldapPropertiesURL;
    private static String xdsDeviceName;
    private static String hl7AppName;
    
    @Inject
    private XdsService service;
    
    public static final Logger log = LoggerFactory.getLogger(XdsDeviceCtrl.class);
    
    private static final String BR = "<br />";
    
    @Context
    private HttpServletRequest request;
    
    @GET
    @Path("running")
    public String isRunning() {
        return String.valueOf(service.isRunning());
    }

    @GET
    @Path("start")
    public Response start() throws Exception {
        service.start();
        return Response.status(Status.OK).build();
    }

    @GET
    @Path("stop")
    public Response stop() {
        service.stop();
        return Response.status(Status.OK).build();
    }

    @GET
    @Path("reload")
    public Response reload() throws Exception {
        service.reload();
        return Response.status(Status.OK).build();
    }

    @GET
    @Path("/config")
    @Produces("text/html")
    public Response showConfiguration() {
        return Response.ok().entity(getConfigurationString(null)).build();
    }
    
    protected String getConfigurationString(String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><body>");
        if (msg != null)
            sb.append(msg).append(BR);
        sb.append("<h3>XDS device:</h3>");
        Device d = service.getDevice();
        if (d == null) {
            sb.append(" NOT configured").toString();
        } else {
            sb.append(d.getDeviceName());
            append(sb, d.getDescription(), " (", ")", true);
            sb.append("<h4>XDS Applications:</h4><pre>");
            appendXdsRegistry(sb, d.getDeviceExtension(XdsRegistry.class), "\n</pre><h5>XDS Registry:</h5><pre>\n", null);
            appendXdsRepo(sb, d.getDeviceExtension(XdsRepository.class), "\n</pre><h5>XDS Repository:</h5><pre>\n", null);
            appendXCARespGW(sb, d.getDeviceExtension(XCARespondingGWCfg.class), "\n</pre><h5>XCA Responding Gateway:</h5><pre>\n", null);
            appendXCAInitiatingGW(sb, d.getDeviceExtension(XCAInitiatingGWCfg.class), "\n</pre><h5>XCA Initiating Gateway:</h5><pre>\n", null);
            appendXCAiRespGW(sb, d.getDeviceExtension(XCAiRespondingGWCfg.class), "\n</pre><h5>XCA-I Responding Gateway:</h5><pre>\n", null);
            appendXCAiInitiatingGW(sb, d.getDeviceExtension(XCAiInitiatingGWCfg.class), "\n</pre><h5>XCA-I Initiating Gateway:</h5><pre>\n", null);
            sb.append("</pre><h4>Audit Logger:</h4><pre>");
            appendAuditLogger(sb, d.getDeviceExtension(AuditLogger.class), "\n\n", null);
            sb.append("</pre><h4>HL7 Applications:</h4><pre>");
            HL7DeviceExtension hl7 = d.getDeviceExtension(HL7DeviceExtension.class);
            for (HL7Application hl7App : hl7.getHL7Applications()) {
                appendHL7App(sb, hl7App, "\n", null);
            }
            sb.append("</pre><h4>Connections:</h4><pre>");
            for (Connection c : d.listConnections()) {
                c.promptTo(sb, "  ");
            }
            sb.append("</pre>");
        }
        sb.append(BR).append("<h3>Config properties:</h3>").append(BR)
        .append("ldapPropertiesURL:").append(ldapPropertiesURL).append(BR)
        .append("xdsDeviceName:").append(xdsDeviceName).append(BR)
        .append("hl7AppName:").append(hl7AppName).append(BR);
        sb.append("</body></html>");
        return sb.toString();
    }
    
    private void appendXdsRegistry(StringBuilder sb, XdsRegistry xdsApp, String prefix, String postfix) {
        append(sb, prefix);
        if (xdsApp == null) {
            sb.append("not configured");
        } else {
            sb.append(APPLICATION_NAME).append(xdsApp.getApplicationName());
            sb.append("\n  Affinity domain(s):").append(StringUtils.concat(xdsApp.getAffinityDomain(), ','));
            sb.append("\n  Affinity domain config dir:").append(xdsApp.getAffinityDomainConfigDir());
            if (xdsApp.getAcceptedMimeTypes() != null)
                sb.append("\n  MIME types:").append(StringUtils.concat(xdsApp.getAcceptedMimeTypes(), ','));
            append(sb, xdsApp.isCheckAffinityDomain(), "\n  checkAffinityDomain:", null, true);
            append(sb, xdsApp.isCheckMimetype(), "\n  checkMimetype:", null, true);
            append(sb, xdsApp.getSoapLogDir(), "\n  SOAP logging dir:", null, true);
            sb.append("\n  CreateMissingCodes:").append(xdsApp.isCreateMissingCodes());
            sb.append("\n  CreateMissingPIDs:").append(xdsApp.isCreateMissingPIDs());
            sb.append("\n  PreMetadataCheck:").append(xdsApp.isPreMetadataCheck());
            sb.append("\n  DontSaveCodeClassifications:").append(xdsApp.isDontSaveCodeClassifications());
        }
    }
    
    private void appendXdsRepo(StringBuilder sb, XdsRepository xdsApp, String prefix, String postfix) {
        append(sb, prefix);
        if (xdsApp == null) {
            sb.append("not configured");
        } else {
            sb.append(APPLICATION_NAME).append(xdsApp.getApplicationName());
            append(sb, xdsApp.getRepositoryUID(), "\n  repositoryUID:", null, false);
            appendArray(sb, xdsApp.getRegistryURLs(), "\n  Registry URLs:");
            if (xdsApp.getAcceptedMimeTypes() != null)
                sb.append("\n  MIME types:").append(StringUtils.concat(xdsApp.getAcceptedMimeTypes(), ','));
            append(sb, xdsApp.isCheckMimetype(), "\n  checkMimetype:", null, true);
            append(sb, xdsApp.isForceMTOM(), "\n  forceMTOM:", null, true);
            append(sb, xdsApp.getSoapLogDir(), "\n  SOAP logging dir:", null, true);
            sb.append("\n  Hostnames/IPs for full SOAP message logging:");
            if (xdsApp.getLogFullMessageHosts() != null)
                sb.append(StringUtils.concat(xdsApp.getLogFullMessageHosts(), ','));
            append(sb, xdsApp.getAllowedCipherHostname(), "\n  AllowedCipherHostname:", " (not used in JBoss7 / appache cxf! Set system property: 'org.jboss.security.ignoreHttpsHost=true'", true);
            append(sb, System.getProperty("org.jboss.security.ignoreHttpsHost"), "\n  org.jboss.security.ignoreHttpsHost:", null, true);
        }
    }

    private void appendXCARespGW(StringBuilder sb, XCARespondingGWCfg rspGW, String prefix, String postfix) {
        append(sb, prefix);
        if (rspGW == null) {
            sb.append("not configured");
        } else {
            sb.append(APPLICATION_NAME).append(rspGW.getApplicationName());
            append(sb, rspGW.getHomeCommunityID(), "\n  HomeCommunityID:", null, false);
            append(sb, rspGW.getRegistryURL(), "\n  Registry URL:", null, false);
            appendArray(sb, rspGW.getRepositoryURLs(), "\n  Repository URLs:");
            append(sb, rspGW.getSoapLogDir(), "\n  SOAP logging dir:", null, true);
        }
    }

    private void appendXCAInitiatingGW(StringBuilder sb, XCAInitiatingGWCfg gw, String prefix, String postfix) {
        append(sb, prefix);
        if (gw == null) {
            sb.append("not configured");
        } else {
            sb.append(APPLICATION_NAME).append(gw.getApplicationName());
            append(sb, gw.getHomeCommunityID(), "\n  HomeCommunityID:", null, false);
            append(sb, gw.getRegistryURL(), "\n  Registry URL:", null, false);
            appendArray(sb, gw.getRespondingGWURLs(), "\n  Responding Gateway URLs: (Query and Retrieve if no RespondingGWRetrieveURL configured)");
            appendArray(sb, gw.getRespondingGWRetrieveURLs(), "\n  Responding Gateway Retrieve URLs:");
            appendArray(sb, gw.getRepositoryURLs(), "\n  Repository URLs:");
            append(sb, gw.getLocalPIXConsumerApplication(), "\n  HL7v2 PIX Consumer (application^facility):", null, false);
            append(sb, gw.getRemotePIXManagerApplication(), "\n  HL7v2 PIX Manager  (application^facility):", null, false);
            appendArray(sb, gw.getAssigningAuthoritiesMap(), "\n  Assigning Authorities:");
            append(sb, gw.isAsync(), "\n  ASYNC:", null, false);
            append(sb, gw.isAsyncHandler(), "\n  AsyncHandler:", null, false);
            append(sb, gw.getSoapLogDir(), "\n  SOAP logging dir:", null, true);
        }
    }
    private void appendXCAiRespGW(StringBuilder sb, XCAiRespondingGWCfg rspGW, String prefix, String postfix) {
        append(sb, prefix);
        if (rspGW == null) {
            sb.append("not configured");
        } else {
            sb.append(APPLICATION_NAME).append(rspGW.getApplicationName());
            append(sb, rspGW.getHomeCommunityID(), "\n  HomeCommunityID:", null, false);
            appendArray(sb, rspGW.getXDSiSourceURLs(), "\n  XDS-I Source URLs:");
            append(sb, rspGW.getSoapLogDir(), "\n  SOAP logging dir:", null, true);
        }
    }

    private void appendXCAiInitiatingGW(StringBuilder sb, XCAiInitiatingGWCfg gw, String prefix, String postfix) {
        append(sb, prefix);
        if (gw == null) {
            sb.append("not configured");
        } else {
            sb.append(APPLICATION_NAME).append(gw.getApplicationName());
            append(sb, gw.getHomeCommunityID(), "\n  HomeCommunityID:", null, false);
            appendArray(sb, gw.getRespondingGWURLs(), "\n  Responding Gateway URLs: (Query and Retrieve if no RespondingGWRetrieveURL configured)");
            appendArray(sb, gw.getXDSiSourceURLs(), "\n  XDS-I Source URLs:");
            append(sb, gw.isAsync(), "\n  ASYNC:", null, false);
            append(sb, gw.isAsyncHandler(), "\n  AsyncHandler:", null, false);
            append(sb, gw.getSoapLogDir(), "\n  SOAP logging dir:", null, true);
        }
    }
 
    private void appendArray(StringBuilder sb, String[] sa, String prefix) {
        if (sa != null) {
            sb.append(prefix);
            for (int i=0 ; i < sa.length ; i++)
               sb.append("\n  ").append(sa[i]);
        }
    }

    private void appendAuditLogger(StringBuilder sb, AuditLogger logger, String prefix, String postfix) {
        append(sb, prefix);
        if (logger == null) {
            sb.append(NOT_CONFIGURED);
        } else {
            sb.append(APPLICATION_NAME).append(logger.getApplicationName());
            append(sb, logger.getAuditSourceID(), "\n  Audit SourceID:", null, false);
            Device arrDevice = logger.getAuditRecordRepositoryDevice();
            sb.append("\n  Audit Record Repository:");
            if (arrDevice == null) {
                sb.append(NOT_CONFIGURED);
            } else {
                sb.append(arrDevice.getDeviceName());
                AuditRecordRepository arr = arrDevice.getDeviceExtension(AuditRecordRepository.class);
                sb.append("\n     Connections:");
                if (arr == null) {
                    sb.append(NOT_CONFIGURED);
                } else {
                    appendAuditConnections(sb, arr.getConnections());
                }
            }
            sb.append("\n  Audit Logger connections:");
                appendAuditConnections(sb, logger.getConnections());
        }
    }

    private void appendAuditConnections(StringBuilder sb, List<Connection> cons) {
        for (Connection c : cons) {
            append(sb, c.getCommonName(), "\n      Name:", null, false);
            append(sb, c.getHostname(),   "\n       Hostname:", null, false);
            append(sb, c.getPort(),       "\n           Port:", null, false);
            if (c.isTls()) {
                if (c.getTlsProtocols() != null)
                    append(sb, StringUtils.concat(c.getTlsProtocols(),','),
                            "\n   TlsProtocols:", null, true);
                if (c.getTlsCipherSuites() != null)
                    append(sb, StringUtils.concat(c.getTlsCipherSuites(),','),
                            "\n   CipherSuites:", null, true);
            }
            sb.append("\n");
        }
    }
    
    private void appendHL7App(StringBuilder sb, HL7Application hl7App, String prefix, String postfix) {
        append(sb, prefix);
        sb.append(APPLICATION_NAME).append(hl7App.getApplicationName());
        append(sb, hl7App.getHL7DefaultCharacterSet(), "\n  DefaultCharacterSet:", null, true);
        if (hl7App.getAcceptedMessageTypes() != null)
            sb.append("\n  Accepted Message types:").append(StringUtils.concat(hl7App.getAcceptedMessageTypes(), ','));
        if (hl7App.getAcceptedSendingApplications() != null)
            sb.append("\n  Accepted Sending Applications:").append(StringUtils.concat(hl7App.getAcceptedSendingApplications(), ','));
    }

    protected void append(StringBuilder sb, Object o) {
        if (o != null) {
            sb.append(o);
        }
    }
    protected void append(StringBuilder sb, Object o, String prefix, String postfix, boolean hideNull) {
        if (o != null) {
            append(sb, prefix);
            sb.append(o);
            append(sb, postfix);
        } else if (!hideNull) {
            append(sb, prefix);
            sb.append("-NOT CONFIGURED-");
            append(sb, postfix);
        }
    }
}
