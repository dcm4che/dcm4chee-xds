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

package org.dcm4chee.xds2.registry.hl7;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.dcm4che.audit.AuditMessages;
import org.dcm4che.audit.AuditMessages.EventActionCode;
import org.dcm4che.conf.api.ConfigurationException;
import org.dcm4che.conf.ldap.audit.LdapAuditLoggerConfiguration;
import org.dcm4che.conf.ldap.audit.LdapAuditRecordRepositoryConfiguration;
import org.dcm4che.conf.prefs.audit.PreferencesAuditLoggerConfiguration;
import org.dcm4che.conf.prefs.audit.PreferencesAuditRecordRepositoryConfiguration;
import org.dcm4che.hl7.HL7Exception;
import org.dcm4che.hl7.HL7Message;
import org.dcm4che.hl7.HL7Segment;
import org.dcm4che.net.Connection;
import org.dcm4che.net.Device;
import org.dcm4che.net.audit.AuditLogger;
import org.dcm4che.net.hl7.HL7Application;
import org.dcm4che.net.hl7.HL7MessageListener;
import org.dcm4che.util.SafeClose;
import org.dcm4che.util.StringUtils;
import org.dcm4chee.xds2.common.audit.XDSAudit;
import org.dcm4chee.xds2.conf.XdsApplication;
import org.dcm4chee.xds2.conf.XdsConfiguration;
import org.dcm4chee.xds2.conf.XdsDevice;
import org.dcm4chee.xds2.conf.ldap.LdapXdsConfiguration;
import org.dcm4chee.xds2.conf.prefs.PreferencesXdsConfiguration;
import org.dcm4chee.xds2.registry.ws.XDSRegistryBeanLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@Path("/xds-rs")
@Produces("text/html")
public class XdsHL7Servlet extends HttpServlet {

    private static String ldapPropertiesURL;
    private static String xdsDeviceName;
    private static String hl7AppName;
    private static XdsConfiguration xdsConfig;
    private HL7Application hl7App;
    
    @EJB
    private XDSRegistryBeanLocal xdsRegistryBean;
    
    public static final Logger log = LoggerFactory.getLogger(XdsHL7Servlet.class);
    
    private static final String BR = "<br />";
    
    private final HL7MessageListener handler = new HL7MessageListener() {

        @Override
        public byte[] onMessage(HL7Application hl7App, Connection conn,
                Socket s, HL7Segment msh, byte[] msg, int off, int len,
                int mshlen) throws HL7Exception {
            String pid = "";
            String srcUserID = msh.getField(3, "") + '|' + msh.getField(2, "");
            String destUserID = msh.getField(5, "") + '|' + msh.getField(4, "");
            String msgType = msh.getField(9, "ADT-A01");
            String eventActionCode = msgType.endsWith("08") ? 
                    EventActionCode.Update : EventActionCode.Create;
            byte[] msh10 = msh.getField(10, "").getBytes();
            String remoteHost;
            try {
                remoteHost = conn.getEndPoint().getHostName();
            } catch (UnknownHostException e1) {
                log.warn("Failed to get remoteHostName!");
                remoteHost ="UNKNOWN";
            }
            boolean success = false;
            try {
                log.info("HL7 message received from "+s.getInetAddress()+":\n"+ new String(msg));
                log.info("  received from:"+s.getInetAddress());
                HL7Message hl7msg = HL7Message.parse(msg, null);
                HL7Segment pidSeg = hl7msg.getSegment("PID");
                if (pidSeg == null)
                    throw new HL7Exception(HL7Exception.AR, "PID segment missing!");
                pid = pidSeg.getField(3, "").trim();
                if (pid.length() < 1)
                    throw new HL7Exception(HL7Exception.AR, "Patient ID (PID[3]) is empty!");
                log.info("#######patient ID:"+pid);
                if (xdsRegistryBean.newPatientID(pid)) {
                    log.info("New Patient ID created:"+pid);
                    success = true;
                } else {
                    log.info("Patient ID already exists:"+pid);
                }
                return HL7Message.makeACK(msh, HL7Exception.AA, null).getBytes(null);
            } catch (Exception e) {
                if (e instanceof HL7Exception) {
                    throw (HL7Exception) e;
                } else {
                    throw new HL7Exception(HL7Exception.AE, e);
                }
            } finally {
                XDSAudit.logPatientFeed(pid, eventActionCode, msh10, srcUserID, remoteHost, destUserID, success);
            }
        }
    };
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        log.info("############ Servlet INIT called! config:"+config);
        super.init(config);
        ldapPropertiesURL = StringUtils.replaceSystemProperties(
                System.getProperty(
                    "org.dcm4chee.xds.ldapPropertiesURL",
                    config.getInitParameter("ldapPropertiesURL")));
        xdsDeviceName = System.getProperty(
                XdsConfiguration.SYSTEM_PROPERTY_XDS_DEVICENAME,
                config.getInitParameter("xdsDeviceName"));
        hl7AppName = System.getProperty(
                XdsConfiguration.SYSTEM_PROPERTY_HL7_APPNAME,
                config.getInitParameter("hl7AppName"));
        try {
            initXdsDevice();
        } catch (Exception e) {
            destroyCfg();
            log.error("XDS device initialization failed!", e);
        }
        
    }

    private void initXdsDevice() throws IOException, MalformedURLException,
            ConfigurationException, GeneralSecurityException {
        InputStream ldapConf = null;
        try {
            ldapConf = new URL(ldapPropertiesURL)
                .openStream();
            Properties p = new Properties();
            p.load(ldapConf);
            LdapXdsConfiguration ldapConfig = new LdapXdsConfiguration(p);
            ldapConfig.addDicomConfigurationExtension(
                    new LdapAuditLoggerConfiguration());
            ldapConfig.addDicomConfigurationExtension(
                    new LdapAuditRecordRepositoryConfiguration());
            xdsConfig = ldapConfig;
        } catch(FileNotFoundException e) {
            log.info("Could not find " + ldapPropertiesURL
                    + " - use Java Preferences as Configuration Backend");
            PreferencesXdsConfiguration prefConfig = new PreferencesXdsConfiguration();
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
        } else if (device instanceof XdsDevice) {
            XdsDevice.setLocalXdsDevice((XdsDevice) device);
            hl7App = XdsDevice.getLocalXdsDevice().getHL7Application(hl7AppName);
        } else {
            hl7App = xdsConfig.findHL7Application(hl7AppName);
        }
        if (hl7App != null) {
            log.info("###### HL7 device:"+device);
            log.info("###### HL7 Application Name:"+hl7App.getApplicationName());
            log.info("###### HL7 Accepted Message Types:"+Arrays.toString(hl7App.getAcceptedMessageTypes()));
            hl7App.setHL7MessageListener(handler);
            ExecutorService executorService = Executors.newCachedThreadPool();
            device.setExecutor(executorService);
            device.bindConnections();
        } else if (hl7AppName != null) {
            log.error("HL7 Application '"+hl7AppName+"' not found!");
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
        XDSAudit.logApplicationActivity(AuditMessages.EventTypeCode.ApplicationStop, true);
    }
    private void destroyCfg() {
        if (xdsConfig != null)
            xdsConfig.close();
        if (hl7App != null)
            hl7App.getDevice().unbindConnections();
    }
    
    @GET
    @Path("/reconfigure")
    public Response reconfigureXdsDevice() {
        log.info("################ Reconfigure XDS device!");
        //log(AuditMessages.EventTypeCode.NetworkConfiguration);
        XdsDevice d = XdsDevice.getLocalXdsDevice();
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
        XdsDevice d = XdsDevice.getLocalXdsDevice();
        if (d == null) {
            sb.append(" NOT configured").toString();
        } else {
            sb.append(d.getDeviceName());
            append(sb, d.getDescription(), " (", ")");
            sb.append("<h4>XDS Applications:</h4><pre>");
            for (XdsApplication xdsApp : d.getXdsApplications()) {
                appendXdsApp(sb, xdsApp, "\n", null);
            }
            sb.append("</pre><h4>HL7 Applications:</h4><pre>");
            for (HL7Application hl7App : d.getHL7Applications()) {
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
    
    private void appendXdsApp(StringBuilder sb, XdsApplication xdsApp, String prefix, String postfix) {
        append(sb, prefix);
        sb.append("Application Name:").append(xdsApp.getApplicationName());
        append(sb, xdsApp.getAffinityDomain(), "\n  Affinity domain:", null);
        if (xdsApp.getAcceptedMimeTypes() != null)
            sb.append("\n  MIME types:").append(StringUtils.concat(xdsApp.getAcceptedMimeTypes(), ','));
        append(sb, xdsApp.getSoapLogDir(), "\n  SOAP logging dir:", null);
        sb.append("\n  CreateMissingCodes:").append(xdsApp.isCreateMissingCodes());
        sb.append("\n  CreateMissingPIDs:").append(xdsApp.isCreateMissingPIDs());
    }
    private void appendHL7App(StringBuilder sb, HL7Application hl7App, String prefix, String postfix) {
        append(sb, prefix);
        sb.append("Application Name:").append(hl7App.getApplicationName());
        append(sb, hl7App.getHL7DefaultCharacterSet(), "\n  DefaultCharacterSet:", null);
        if (hl7App.getAcceptedMessageTypes() != null)
            sb.append("\n  Accepted Message types:").append(StringUtils.concat(hl7App.getAcceptedMessageTypes(), ','));
        if (hl7App.getAcceptedSendingApplications() != null)
            sb.append("\n  Accepted Sending Applications:").append(StringUtils.concat(hl7App.getAcceptedSendingApplications(), ','));
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
