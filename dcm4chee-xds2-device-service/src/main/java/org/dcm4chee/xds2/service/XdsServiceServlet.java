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

package org.dcm4chee.xds2.service;

import java.net.BindException;
import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.dcm4che.audit.AuditMessages;
import org.dcm4che.net.Connection;
import org.dcm4che.net.Device;
import org.dcm4che.net.audit.AuditLogger;
import org.dcm4che.net.audit.AuditRecordRepository;
import org.dcm4che.net.hl7.HL7Application;
import org.dcm4che.net.hl7.HL7DeviceExtension;
import org.dcm4che.util.StringUtils;
import org.dcm4chee.xds2.common.audit.XDSAudit;
import org.dcm4chee.xds2.common.code.AffinityDomainCodes;
import org.dcm4chee.xds2.common.code.Code;
import org.dcm4chee.xds2.common.code.XADCfgRepository;
import org.dcm4chee.xds2.conf.XCAInitiatingGWCfg;
import org.dcm4chee.xds2.conf.XCARespondingGWCfg;
import org.dcm4chee.xds2.conf.XCAiInitiatingGWCfg;
import org.dcm4chee.xds2.conf.XCAiRespondingGWCfg;
import org.dcm4chee.xds2.conf.XdsDevice;
import org.dcm4chee.xds2.conf.XdsRegistry;
import org.dcm4chee.xds2.conf.XdsRepository;
import org.dcm4chee.xds2.registry.hl7.XdsHL7Service;
import org.dcm4chee.xds2.ws.registry.XDSRegistryBeanLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@Path("/xds-rs")
@Produces("text/html")
public class XdsServiceServlet extends HttpServlet {

    private static final String APPLICATION_NAME = "Application Name:";
    private static final String NOT_CONFIGURED = "Not configured!";
    private static String ldapPropertiesURL;
    private static String xdsDeviceName;
    private static String hl7AppName;
    
    @EJB
    private XDSRegistryBeanLocal xdsRegistryBean;
    
    public static final Logger log = LoggerFactory.getLogger(XdsServiceServlet.class);
    
    private static final String BR = "<br />";
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        log.info("############ Servlet INIT called! config:"+config);
        super.init(config);
        ldapPropertiesURL = System.getProperty(XdsDevice.PROP_LDAP_PROPERTIES_URL);
        if (ldapPropertiesURL == null) {
        	ldapPropertiesURL = config.getInitParameter("ldapPropertiesURL");
        	if (ldapPropertiesURL != null)
        		System.setProperty(XdsDevice.PROP_LDAP_PROPERTIES_URL, ldapPropertiesURL);
        }
        ldapPropertiesURL = StringUtils.replaceSystemProperties(ldapPropertiesURL);
        xdsDeviceName = System.getProperty(XdsDevice.PROP_XDS_DEVICE_NAME);
        if (xdsDeviceName == null) {
        	xdsDeviceName = config.getInitParameter("xdsDeviceName");
        	if (xdsDeviceName != null)
        		System.setProperty(XdsDevice.PROP_XDS_DEVICE_NAME, xdsDeviceName);
        }
        hl7AppName = System.getProperty(XdsDevice.PROP_XDS_HL7_APP_NAME);
        if (hl7AppName == null) {
        	hl7AppName = config.getInitParameter("hl7AppName");
        	if (hl7AppName != null)
        		System.setProperty(XdsDevice.PROP_XDS_HL7_APP_NAME, hl7AppName);
        }
        initXdsDevice();
    }

    public void initXdsDevice() {
        log.info("###### add HL7 message listener!");
        XdsDevice.setHL7MessageListener(new XdsHL7Service(xdsRegistryBean));
        XDSAudit.logApplicationActivity(AuditMessages.EventTypeCode.ApplicationStart, true);
    }

    @Override
    public void destroy() {
    	XdsDevice.destroyCfg();
        XDSAudit.logApplicationActivity(AuditMessages.EventTypeCode.ApplicationStop, true);
    }
/*
 * RESTFUL Services    
 */
    @GET
    @Path("/reconfigure")
    public Response reconfigureXdsDevice() {
        log.info("################ Reconfigure XDS device!");
        //log(AuditMessages.EventTypeCode.NetworkConfiguration);
        Device d = XdsDevice.getLocalXdsDevice();
        if (d != null) {
          	if (XdsDevice.reconfigure()) {
                return Response.ok().entity(getConfigurationString("XDS device '"+d.getDeviceName()+"' reconfigured at "+
                        new Date())).build();
          	} else {
                log.error("Reconfiguration of XDS Device failed!");
                return Response.serverError().entity("Reload configuration of XDS device '"+
                        d.getDeviceName()+"' failed! ").build();
            }
        } else {
            log.info("No local XDS device set! Try to initialize XDS device "+xdsDeviceName);
            try {
                XdsDevice.checkMissingXdsDevice();
                log.info("XDS device "+xdsDeviceName+" initialized!");
                return Response.ok().entity(getConfigurationString("XDS device '"+xdsDeviceName+"' initialized at "+
                        new Date())).build();
            } catch (Exception x) {
                String msg = "Initialization of XDS device '"+xdsDeviceName+"' failed! Reason:"+x;
                log.info(msg);
                return Response.serverError().entity(msg).build();
            }
        }
    }

    @GET
    @Path("/running")
    public Response isRunning() {
        Device device = XdsDevice.getLocalXdsDevice();
        return device.getExecutor() != null 
                ? Response.ok().entity(Boolean.TRUE.toString()).build() 
                        : Response.serverError().entity(Boolean.FALSE.toString()).build();
    }

    @GET
    @Path("/restart")
    public Response restart() throws InterruptedException {
        Device device = XdsDevice.getLocalXdsDevice();
        try {
            XdsDevice.reconfigure();
            int count = getRestartTimeout();
            while (device.getExecutor() == null) {
                try {
                    device.rebindConnections();
                } catch (BindException e) {
                    if (count < 0) {
                        log.error("Error restarting {}: {}", xdsDeviceName, e.getMessage());
                        return Response.serverError().entity(null).build();
                    }
                    count--;
                    Thread.sleep(1000);
                }
            }
            log.info("Device " + device.getDeviceName() + " restarted");
            return Response.noContent().entity(null).build();
        } catch (Exception e) {
            log.error("Error restarting {}: {}", xdsDeviceName, e.getMessage());
            return Response.serverError().entity(null).build();
        }
    }

    private static int getRestartTimeout() {
        String timeoutString = System.getProperty("org.dcm4chee.xds.restart.timeout","10");
        try {
            return Integer.parseInt(timeoutString);
        } catch (NumberFormatException e) {
            log.error("{} ({})", new Object[] { e, "org.dcm4chee.xds.restart.timeout" });
            return 10;
        }
    }

    @GET
    @Path("/show")
    public Response showConfiguration() {
        return Response.ok().entity(getConfigurationString(null)).build();
    }
    private String getConfigurationString(String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><body>");
        if (msg != null)
            sb.append(msg).append(BR);
        sb.append("<h3>XDS device:</h3>");
        Device d = XdsDevice.getLocalXdsDevice();
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
    
    @GET
    @Path("/initCodes/{affinity}")
    public Response initCodes(@PathParam("affinity") String affinityDomain) {
        log.info("################ Init XDS Affinity domain codes!");
        XdsRegistry d = XdsDevice.getXdsRegistry();
        if (d != null && d.getCodeRepository() != null) {
            try {
                d.getCodeRepository().initCodes(affinityDomain);
                log.info("Load codes of affinity domain "+affinityDomain+" finished!");
                return Response.ok().entity(getAffinityDomainResultMsg(affinityDomain)).build();
            } catch (Exception x) {
                log.error("Load codes of affinity domain "+affinityDomain+" failed!");
                return Response.serverError().entity("Load codes of affinity domain "+affinityDomain+" failed!"+x).build();
            }
        } else {
            return Response.serverError().entity(getConfigurationString("Code Repository not configured!")).build();
        }
    }

    @GET
    @Path("/showPatIDs/{affinity}")
    public Response showPatIDs(@PathParam("affinity") String affinityDomain) {
        log.info("################ Show Patient ID's!");
        try {
            List<String> patIDs = xdsRegistryBean.listPatientIDs(affinityDomain);
            StringBuilder sb = new StringBuilder(patIDs.size()<<4);
            sb.append("<h4>List of Patient IDs for affinity domain ").append(affinityDomain).append("</h4><pre>");
            for (int i = 0, len = patIDs.size() ; i < len ; i++) {
                sb.append("\n    ").append(patIDs.get(i));
            }
            sb.append("</pre>");
            return Response.ok().entity(sb.toString()).build();
        } catch (Exception x) {
            log.error("List patient IDs of affinity domain failed!", x);
            return Response.serverError().entity("List patient IDs of affinity domain "+affinityDomain+" failed!"+x).build();
        }
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

    private String getAffinityDomainResultMsg(String affinityDomain) {
        StringBuilder sb = new StringBuilder();
        sb.append("Load codes of affinity domain <b>").append(affinityDomain).
        append(" </b>finished at ").append(new Date());
        XADCfgRepository codeRepository = XdsDevice.getXdsRegistry().getCodeRepository();
        String[] ads = "*".equals(affinityDomain) ?
            codeRepository.getAffinityDomains().toArray(new String[0]) : new String[]{affinityDomain};
        for (int i = 0 ; i < ads.length ; i++) {
            AffinityDomainCodes adCodes = codeRepository.getAffinityDomainCodes(ads[i]);
            sb.append("<h4>Affinity domain:").append(adCodes.getAffinityDomain()).append("</h4>");
            List<Code> codes;
            for (String codeType : adCodes.getCodeTypes()) {
                sb.append("<h5>Code Type:").append(codeType).append("</h5><pre>");
                codes = adCodes.getCodes(codeType);
                for (int j = 0, len = codes.size() ; j < len ; j++) {
                    sb.append("\n   ").append(codes.get(j));
                }
                sb.append("</pre>");
            }
        }
        return sb.toString();
    }

    private void append(StringBuilder sb, Object o) {
        if (o != null) {
            sb.append(o);
        }
    }
    private void append(StringBuilder sb, Object o, String prefix, String postfix, boolean hideNull) {
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
