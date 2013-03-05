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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ejb.EJB;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.dcm4che.audit.AuditMessages;
import org.dcm4che.conf.api.ConfigurationException;
import org.dcm4che.conf.api.DicomConfiguration;
import org.dcm4che.conf.ldap.LdapDicomConfiguration;
import org.dcm4che.conf.ldap.audit.LdapAuditLoggerConfiguration;
import org.dcm4che.conf.ldap.audit.LdapAuditRecordRepositoryConfiguration;
import org.dcm4che.conf.ldap.hl7.LdapHL7Configuration;
import org.dcm4che.conf.prefs.PreferencesDicomConfiguration;
import org.dcm4che.conf.prefs.audit.PreferencesAuditLoggerConfiguration;
import org.dcm4che.conf.prefs.audit.PreferencesAuditRecordRepositoryConfiguration;
import org.dcm4che.conf.prefs.hl7.PreferencesHL7Configuration;
import org.dcm4che.net.Connection;
import org.dcm4che.net.Device;
import org.dcm4che.net.audit.AuditLogger;
import org.dcm4che.net.audit.AuditRecordRepository;
import org.dcm4che.net.hl7.HL7Application;
import org.dcm4che.net.hl7.HL7DeviceExtension;
import org.dcm4che.util.SafeClose;
import org.dcm4che.util.StringUtils;
import org.dcm4chee.xds2.common.audit.XDSAudit;
import org.dcm4chee.xds2.common.code.AffinityDomainCodes;
import org.dcm4chee.xds2.common.code.Code;
import org.dcm4chee.xds2.common.code.XADCfgRepository;
import org.dcm4chee.xds2.conf.XdsDevice;
import org.dcm4chee.xds2.conf.XdsRegistry;
import org.dcm4chee.xds2.conf.XdsRepository;
import org.dcm4chee.xds2.conf.ldap.LdapXDSRegistryConfiguration;
import org.dcm4chee.xds2.conf.ldap.LdapXDSRepositoryConfiguration;
import org.dcm4chee.xds2.conf.prefs.PreferencesXDSRegistryConfiguration;
import org.dcm4chee.xds2.conf.prefs.PreferencesXDSRepositoryConfiguration;
import org.dcm4chee.xds2.registry.hl7.XdsHL7Service;
import org.dcm4chee.xds2.storage.file.XDSFileStorage;
import org.dcm4chee.xds2.storage.file.XDSFileStorageMBean;
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
    private static DicomConfiguration xdsConfig;
    private HL7Application hl7App;
    
    private ObjectInstance mbean;
    
    @EJB
    private XDSRegistryBeanLocal xdsRegistryBean;
    
    public static final Logger log = LoggerFactory.getLogger(XdsServiceServlet.class);
    
    private static final String BR = "<br />";
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        log.info("############ Servlet INIT called! config:"+config);
        super.init(config);
        ldapPropertiesURL = StringUtils.replaceSystemProperties(
                System.getProperty(
                    "org.dcm4chee.xds.ldapPropertiesURL",
                    config.getInitParameter("ldapPropertiesURL")));
        xdsDeviceName = System.getProperty("org.dcm4chee.xds.deviceName",
                config.getInitParameter("xdsDeviceName"));
        hl7AppName = System.getProperty("org.dcm4chee.xds.hl7AppName",
                config.getInitParameter("hl7AppName"));
        XDSFileStorageMBean xdsStore = new XDSFileStorage("xds2-docs");
        try {
            mbean = ManagementFactory.getPlatformMBeanServer().registerMBean(xdsStore, new ObjectName("dcm4chee.xds2:service=Store"));
        } catch (Exception x) {
            log.error("######### Failed to start XDSStorage MBean!", x);
        }
        try {
            initXdsDevice();
        } catch (Exception e) {
            destroyCfg();
            log.error("XDS device initialization failed!", e);
        }
        
    }

    public void initXdsDevice() throws IOException, MalformedURLException,
            ConfigurationException, GeneralSecurityException {
        InputStream ldapConf = null;
        try {
            ldapConf = new URL(ldapPropertiesURL).openStream();
            Properties p = new Properties();
            p.load(ldapConf);
            LdapDicomConfiguration ldapConfig = new LdapDicomConfiguration(p);
            ldapConfig.addDicomConfigurationExtension(new LdapXDSRegistryConfiguration());
            ldapConfig.addDicomConfigurationExtension(new LdapXDSRepositoryConfiguration());
            ldapConfig.addDicomConfigurationExtension(new LdapHL7Configuration());
            ldapConfig.addDicomConfigurationExtension(new LdapAuditLoggerConfiguration());
            ldapConfig.addDicomConfigurationExtension(new LdapAuditRecordRepositoryConfiguration());
            xdsConfig = ldapConfig;
        } catch(FileNotFoundException e) {
            log.info("Could not find " + ldapPropertiesURL
                    + " - use Java Preferences as Configuration Backend");
            PreferencesDicomConfiguration prefConfig = new PreferencesDicomConfiguration();
            prefConfig.addDicomConfigurationExtension(new PreferencesXDSRegistryConfiguration());
            prefConfig.addDicomConfigurationExtension(new PreferencesXDSRepositoryConfiguration());
            prefConfig.addDicomConfigurationExtension(new PreferencesHL7Configuration());
            prefConfig.addDicomConfigurationExtension(new PreferencesAuditLoggerConfiguration());
            prefConfig.addDicomConfigurationExtension(new PreferencesAuditRecordRepositoryConfiguration());
            xdsConfig = prefConfig;
        } finally {
            SafeClose.close(ldapConf);
        }
        
        log.info("###### xdsConfig:"+xdsConfig);
        Device device = xdsConfig.findDevice(xdsDeviceName);
        if (device == null) {
            String msg = "XDS Device '"+xdsDeviceName+"' not found!";
            log.error(msg);
            throw new ConfigurationException(msg);
        }
        XdsDevice.setLocalXdsDevice(device);
        
        if (hl7AppName != null) {
            HL7DeviceExtension hl7 = device.getDeviceExtension(HL7DeviceExtension.class);
            if (hl7 != null) {
                hl7App = hl7.getHL7Application(hl7AppName);
                if (hl7App != null) {
                    log.info("###### HL7 device:"+device);
                    log.info("###### HL7 Application Name:"+hl7App.getApplicationName());
                    log.info("###### HL7 Accepted Message Types:"+Arrays.toString(hl7App.getAcceptedMessageTypes()));
                    hl7App.setHL7MessageListener(new XdsHL7Service(xdsRegistryBean));
                    ExecutorService executorService = Executors.newCachedThreadPool();
                    device.setExecutor(executorService);
                    device.bindConnections();
                } else if (hl7AppName != null) {
                    log.error("HL7 Application '"+hl7AppName+"' not found!");
                }
            }
        } else {
            log.info("No HL7 Application defined for this service!");
        }
        XDSAudit.setAuditLogger(device.getDeviceExtension(AuditLogger.class));
        log.info("###### Audit Logger:"+XDSAudit.info());
        XDSAudit.logApplicationActivity(AuditMessages.EventTypeCode.ApplicationStart, true);
    }

    @Override
    public void destroy() {
        destroyCfg();
        if (mbean != null) {
            try {
                ManagementFactory.getPlatformMBeanServer()
                    .unregisterMBean(mbean.getObjectName());
            } catch (Exception x) {
                log.warn("###### Failed to unregister XDSStorage MBean!", x);
            }
        }
        XDSAudit.logApplicationActivity(AuditMessages.EventTypeCode.ApplicationStop, true);
    }
    private void destroyCfg() {
        if (xdsConfig != null)
            xdsConfig.close();
        if (hl7App != null)
            hl7App.getDevice().unbindConnections();
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
        if (d != null && xdsConfig != null) {
            try {
                d.reconfigure(xdsConfig.findDevice(d.getDeviceName()));
                log.info("Device "+d.getDeviceName()+" reconfigured!");
                d.rebindConnections();
                log.info("rebindConnections done!");
                return Response.ok().entity(getConfigurationString("XDS device '"+d.getDeviceName()+"' reconfigured at "+
                        new Date())).build();
            } catch (Exception x) {
                log.error("Reconfiguration of XDS Device failed!", x);
                return Response.serverError().entity("Reload configuration of XDS device '"+
                        d.getDeviceName()+"' failed! "+x).build();
            }
        } else {
            log.info("No local XDS device set! Try to initialize XDS device "+xdsDeviceName);
            try {
                initXdsDevice();
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
            append(sb, d.getDescription(), " (", ")");
            sb.append("<h4>XDS Applications:</h4><pre>");
            appendXdsRegistry(sb, d.getDeviceExtension(XdsRegistry.class), "\n", null);
            appendXdsRepo(sb, d.getDeviceExtension(XdsRepository.class), "\n\n", null);
            sb.append("<h4>Audit Logger:</h4><pre>");
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
            append(sb, xdsApp.isCheckAffinityDomain(), "\n  checkAffinityDomain:", null);
            append(sb, xdsApp.isCheckMimetype(), "\n  checkMimetype:", null);
            append(sb, xdsApp.getSoapLogDir(), "\n  SOAP logging dir:", null);
            sb.append("\n  CreateMissingCodes:").append(xdsApp.isCreateMissingCodes());
            sb.append("\n  CreateMissingPIDs:").append(xdsApp.isCreateMissingPIDs());
        }
    }
    
    private void appendXdsRepo(StringBuilder sb, XdsRepository xdsApp, String prefix, String postfix) {
        append(sb, prefix);
        sb.append(APPLICATION_NAME).append(xdsApp.getApplicationName());
        append(sb, xdsApp.getRepositoryUID(), "\n  repositoryUID:", null);
        sb.append("\n  Registry URLs:");
        String[] urls = xdsApp.getRegistryURLs();
        for (int i=0 ; i < urls.length ; i++)
            sb.append("\n  ").append(urls[i]);
        if (xdsApp.getAcceptedMimeTypes() != null)
            sb.append("\n  MIME types:").append(StringUtils.concat(xdsApp.getAcceptedMimeTypes(), ','));
        append(sb, xdsApp.isCheckMimetype(), "\n  checkMimetype:", null);
        append(sb, xdsApp.getSoapLogDir(), "\n  SOAP logging dir:", null);
        sb.append("\n  Hostnames/IPs for full SOAP message logging:");
        if (xdsApp.getLogFullMessageHosts() != null)
            sb.append(StringUtils.concat(xdsApp.getLogFullMessageHosts(), ','));
    }

    private void appendAuditLogger(StringBuilder sb, AuditLogger logger, String prefix, String postfix) {
        append(sb, prefix);
        if (logger == null) {
            sb.append(NOT_CONFIGURED);
        } else {
            sb.append(APPLICATION_NAME).append(logger.getApplicationName());
            append(sb, logger.getAuditSourceID(), "\n  Audit SourceID:", null);
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
            append(sb, c.getCommonName(), "\n      Name:", null);
            append(sb, c.getHostname(),   "\n       Hostname:", null);
            append(sb, c.getPort(),       "\n           Port:", null);
            if (c.isTls()) {
                if (c.getTlsProtocols() != null)
                    append(sb, StringUtils.concat(c.getTlsProtocols(),','),
                            "\n   TlsProtocols:", null);
                if (c.getTlsCipherSuites() != null)
                    append(sb, StringUtils.concat(c.getTlsCipherSuites(),','),
                            "\n   CipherSuites:", null);
            }
            sb.append("\n");
        }
    }
    
    private void appendHL7App(StringBuilder sb, HL7Application hl7App, String prefix, String postfix) {
        append(sb, prefix);
        sb.append(APPLICATION_NAME).append(hl7App.getApplicationName());
        append(sb, hl7App.getHL7DefaultCharacterSet(), "\n  DefaultCharacterSet:", null);
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
    private void append(StringBuilder sb, Object o, String prefix, String postfix) {
        if (o != null) {
            append(sb, prefix);
            sb.append(o);
            append(sb, postfix);
        }
    }
}
