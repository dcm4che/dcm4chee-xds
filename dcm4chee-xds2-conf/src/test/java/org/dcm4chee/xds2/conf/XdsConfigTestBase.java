/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contentsOfthis file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copyOfthe License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is partOfdcm4che, an implementationOfDICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial DeveloperOfthe Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contentsOfthis file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisionsOfthe GPL or the LGPL are applicable instead
 * of those above. If you wish to allow useOfyour versionOfthis file only
 * under the termsOfeither the GPL or the LGPL, and not to allow others to
 * use your versionOfthis file under the termsOfthe MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your versionOfthis file under
 * the termsOfany oneOfthe MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chee.xds2.conf;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.dcm4che.conf.api.ConfigurationException;
import org.dcm4che.conf.api.ConfigurationNotFoundException;
import org.dcm4che.conf.api.DicomConfiguration;
import org.dcm4che.data.Code;
import org.dcm4che.data.Issuer;
import org.dcm4che.net.Connection;
import org.dcm4che.net.Device;
import org.dcm4che.net.hl7.HL7Application;
import org.dcm4che.net.hl7.HL7DeviceExtension;
import org.junit.After;
import org.junit.Test;

public class XdsConfigTestBase {

    private static final String XDS_REGISTRY1 = "XDS_REGISTRY1";
    private static final String XDS_REGISTRY2 = "XDS_REGISTRY2";
    private static final String XDS_REGISTRY3 = "XDS_REGISTRY3";
    private static final String XDS_REGISTRY4 = "XDS_REGISTRY4";
    
    private static final String XDS_REPOSITORY1 = "XDS_REPOSITORY1";
    private static final String XDS_REPO1_UID = "1.2.3.4";
    private static final String XDS_REPO2_UID = "4.3.2.1";
    private static final String[] XDS_REPO1_URI = {"0|http:/xds/repo1"};
    private static final String[] XDS_REPO2_URI = {"0|http:/xds/repo2"};
    
    private static final String HL7_APP_NAME1 = "XDS^DCM4CHEE";
    private static final String HL7_APP_NAME2 = "XDS2^DCM4CHEE";
    private static final String HL7_APP_NAME3 = "XDS3^DCM4CHEE";
    private static final String[] HL7_MESSAGE_TYPES = {
        "ADT^A02",
        "ADT^A03",
        "ADT^A06",
        "ADT^A07",
        "ADT^A08",
        "ADT^A40",
        "ORM^O01"
    };
    private static final Issuer SITE_A =
        new Issuer("XDS_A", "1.2.40.0.13.1.1.999.111.1111", "ISO");
    private static final Code INST_A =
        new Code("111.1111", "99DCM4CHEE", null, "Site A");
    private static final String[] MIME_TYPES1 = new String[]{"application/dicom"};
    private static final String[] MIME_TYPES2 = new String[]{"text/xml","application/dicom"};
    private String[] AFFINITY_DOMAIN = {"1.2.3.4.5"};
    private static final String[] AFFINITY_DOMAIN2 = {"5.4.3.2.1"};

    protected static int testCount = 0;
    protected DicomConfiguration config;

    @After
    public void tearDown() throws Exception {
        if (System.getProperty("keep") == null)
            cleanUp();
        if (config != null)
            config.close();
    }
    @Test
    public void testSimple() throws Exception {
        config.persist(createXdsRegistryDevice("xds"+testCount, SITE_A, INST_A, XDS_REGISTRY1, AFFINITY_DOMAIN, MIME_TYPES1, null, false, false));
        afterPersist();
        checkXdsRegistryDevice("xds"+testCount, SITE_A, INST_A, XDS_REGISTRY1, AFFINITY_DOMAIN, MIME_TYPES1, null, false, false, true, null);
        
    }
    @Test
    public void testWithOptional() throws Exception {
        config.persist(createXdsRegistryDevice("xds"+testCount, SITE_A, INST_A, XDS_REGISTRY2, AFFINITY_DOMAIN, MIME_TYPES2, "/log/xdslog", true, true));
        afterPersist();
        checkXdsRegistryDevice("xds"+testCount, SITE_A, INST_A, XDS_REGISTRY2, AFFINITY_DOMAIN, MIME_TYPES2, "/log/xdslog", true, true, true, null);
    }
    @Test
    public void testWithHL7() throws Exception {
        Device device = createXdsRegistryDevice("xds"+testCount, SITE_A, INST_A, XDS_REGISTRY3, AFFINITY_DOMAIN, MIME_TYPES1, "/log/xdslog", true, true);
        HL7DeviceExtension hl7Ext = new HL7DeviceExtension();
        device.addDeviceExtension(hl7Ext);
        ArrayList<HL7Application> hl7Apps = new ArrayList<HL7Application>();
        HL7Application hl7App = createHL7Application(HL7_APP_NAME1, "hl7-conn", "localhost", 2575, 2576, device);
        hl7Apps.add(hl7App);
        hl7Ext.addHL7Application(hl7App);
        config.persist(device);
        afterPersist();
        checkXdsRegistryDevice("xds"+testCount, SITE_A, INST_A, XDS_REGISTRY3, AFFINITY_DOMAIN, MIME_TYPES1, "/log/xdslog", true, true, true, hl7Apps);
    }
    
    protected HL7Application findHL7Application(String hl7AppName1) throws ConfigurationException {
        return null;
    }

    @Test
    public void testModify() throws Exception {
        Device device = createXdsRegistryDevice("xds"+testCount, SITE_A, INST_A, XDS_REGISTRY4, AFFINITY_DOMAIN, MIME_TYPES1, "/log/xdslog/m", true, true);
        HL7DeviceExtension hl7Ext = new HL7DeviceExtension();
        device.addDeviceExtension(hl7Ext);
        ArrayList<HL7Application> hl7Apps = new ArrayList<HL7Application>();
        HL7Application hl7App = createHL7Application(HL7_APP_NAME2, "hl7-conn", "localhost", 2575, 2576, device);
        hl7Apps.add(hl7App);
        hl7Ext.addHL7Application(hl7App);
        config.persist(device);
        afterPersist();
        checkXdsRegistryDevice("xds"+testCount, SITE_A, INST_A, XDS_REGISTRY4, AFFINITY_DOMAIN, MIME_TYPES1, "/log/xdslog/m", true, true, true, hl7Apps);
        XdsRegistry xdsApp = device.getDeviceExtension(XdsRegistry.class);
        xdsApp.setAffinityDomain(AFFINITY_DOMAIN2);
        xdsApp.setSoapLogDir("/log/xdslog/mod");
        xdsApp.setCreateMissingPIDs(false);
        xdsApp.setAcceptedMimeTypes(MIME_TYPES2);
        xdsApp.setCheckAffinityDomain(false);
        xdsApp.setCheckMimetype(false);
        for (Connection c : hl7App.getConnections()) {
            c.setPort(c.getPort()+10000);
            c.setHostname("changed");
        }
        HL7Application hl7App2 = createHL7Application(HL7_APP_NAME3, "hl7-conn", "localhost", 3575, 3576, device);
        hl7Apps.add(hl7App2);
        hl7Ext.addHL7Application(hl7App2);
        config.merge(device);
        checkXdsRegistryDevice("xds"+testCount, SITE_A, INST_A, XDS_REGISTRY4, AFFINITY_DOMAIN2, MIME_TYPES2, "/log/xdslog/mod", false, true, false, hl7Apps);
    }

    @Test
    public void testRepository() throws Exception {
        Device app = createXdsRepositoryDevice("xds"+testCount, SITE_A, INST_A, XDS_REPOSITORY1, XDS_REPO1_UID, XDS_REPO1_URI, MIME_TYPES1, null);
        config.persist(app);
        afterPersist();
        checkXdsRepositoryDevice("xds"+testCount, XDS_REPOSITORY1, XDS_REPO1_UID, XDS_REPO1_URI, MIME_TYPES1, null, true);
        XdsRepository rep = app.getDeviceExtension(XdsRepository.class);
        rep.setAcceptedMimeTypes(MIME_TYPES2);
        rep.setCheckMimetype(false);
        rep.setRepositoryUID(XDS_REPO2_UID);
        rep.setRegistryURLs(XDS_REPO2_URI);
        config.merge(app);
        checkXdsRepositoryDevice("xds"+testCount, XDS_REPOSITORY1, XDS_REPO2_UID, XDS_REPO2_URI, MIME_TYPES2, null, false);
    }
    
    protected void cleanUp() throws Exception {
        if (config == null || testCount==0)
            return;
        try {
            config.removeDevice("xds"+testCount);
        } catch (ConfigurationNotFoundException e) {}
    }

    public void afterPersist() throws Exception {
    }

    private Device init(Device device, Issuer issuer, Code institutionCode)
            throws Exception {
        device.setIssuerOfPatientID(issuer);
        device.setIssuerOfAccessionNumber(issuer);
        if (institutionCode != null) {
            device.setInstitutionNames(institutionCode.getCodeMeaning());
            device.setInstitutionCodes(institutionCode);
        }
        return device;
    }


    private Device createXdsRegistryDevice(String name, Issuer issuer, Code institutionCode, String appName, 
            String[] affinityDomain, String[] mime, String logDir, boolean createPID, boolean createCode) throws Exception {
         Device device = new Device(name);
         init(device, issuer, institutionCode);
         XdsRegistry registry = new XdsRegistry();
         device.addDeviceExtension(registry);
         registry.setApplicationName(appName);
         registry.setAffinityDomain(affinityDomain);
         registry.setAffinityDomainConfigDir("/domainconfig");
         registry.setAcceptedMimeTypes(mime);
         registry.setSoapLogDir(logDir);
         registry.setCreateMissingPIDs(createPID);
         registry.setCreateMissingCodes(createCode);
         registry.setCheckAffinityDomain(true);
         registry.setCheckMimetype(true);
         return device;
     }

    private Device createXdsRepositoryDevice(String name, Issuer issuer, Code institutionCode, String appName, 
            String repositoryUID, String[] registryURLs, String[] mime, String logDir) throws Exception {
         Device device = new Device(name);
         init(device, issuer, institutionCode);
         XdsRepository rep = new XdsRepository();
         device.addDeviceExtension(rep);
         rep.setApplicationName(appName);
         rep.setRepositoryUID(repositoryUID);
         rep.setRegistryURLs(registryURLs);
         rep.setAcceptedMimeTypes(mime);
         rep.setSoapLogDir(logDir);
         rep.setCheckMimetype(true);
         rep.setAllowedCipherHostname("*");
         rep.setLogFullMessageHosts(new String[]{});
         return device;
     }
    
    private HL7Application createHL7Application(String appName, String connName,
            String host, int port, int tlsPort, Device xdsDevice) throws Exception {
         HL7Application hl7App = new HL7Application(appName);
         Connection conn = getOrCreateConnection(connName, host, port, xdsDevice);
         hl7App.addConnection(conn);
         Connection connTLS = getOrCreateConnection(connName+"-tls", host, tlsPort, xdsDevice);
         connTLS.setTlsCipherSuites(
                 Connection.TLS_RSA_WITH_AES_128_CBC_SHA, 
                 Connection.TLS_RSA_WITH_3DES_EDE_CBC_SHA);
         hl7App.addConnection(connTLS);
         hl7App.setAcceptedMessageTypes(HL7_MESSAGE_TYPES);
         return hl7App;
     }

    private Connection getOrCreateConnection(String connName, String host,
            int port, Device xdsDevice) {
         for (Connection c : xdsDevice.listConnections()) {
             if (c.getCommonName().equals(connName))
                 return c;
         }
         Connection conn = new Connection(connName, host, port);
         conn.setProtocol(Connection.Protocol.HL7);
         xdsDevice.addConnection(conn);
        return conn;
    }
/*_*/    
    private void checkXdsRegistryDevice(String name, Issuer issuer, Code institutionCode, String appName, 
            String[] affinityDomain, String[] mime, String logDir, boolean createPID, boolean createCode, boolean check, Collection<HL7Application> hl7Apps) throws Exception {
        Device device = config.findDevice(name);
        XdsRegistry app = device.getDeviceExtension(XdsRegistry.class);
        String prefix = name+"-"+appName;
        assertArrayEquals(prefix + "-AffinityDomain: count:", affinityDomain, app.getAffinityDomain());
        assertEquals(prefix + "-ApplicationName", appName, app.getApplicationName());
        assertEquals(prefix + "-SoapLogDir", logDir, app.getSoapLogDir());
        assertArrayEquals(prefix + "-MimeTypes", mime, app.getAcceptedMimeTypes());
        assertEquals(prefix + "-createCode", createCode, app.isCreateMissingCodes());
        assertEquals(prefix + "-createPIDs", createPID, app.isCreateMissingPIDs());
        assertEquals(prefix + "-checkAffinityDomain", check, app.isCheckAffinityDomain());
        assertEquals(prefix + "-checkMimetype", check, app.isCheckMimetype());
        HL7DeviceExtension foundHL7ext = device.getDeviceExtension(HL7DeviceExtension.class);
        assertEquals(prefix + "-NumberOfHL7Apps", 
                hl7Apps == null ? 0 : hl7Apps.size(), 
                foundHL7ext == null ? 0 : foundHL7ext.getHL7Applications().size());
        if (hl7Apps != null) {
            HL7Application hl7app1;
            for (HL7Application hl7app : hl7Apps) {
                hl7app1 = foundHL7ext.getHL7Application(hl7app.getApplicationName());
                assertNotNull(prefix + "-HL7Applicationname " + hl7app.getApplicationName() + " not found!", hl7app1);
                assertArrayEquals(prefix + "-AcceptedMessageTypes", hl7app.getAcceptedMessageTypes(),hl7app1.getAcceptedMessageTypes());
                assertArrayEquals(prefix + "-AcceptedSendingApplications", hl7app.getAcceptedSendingApplications(),hl7app1.getAcceptedSendingApplications());
                assertEquals(prefix + "-HL7DefaultCharacterSet", hl7app.getHL7DefaultCharacterSet(),hl7app1.getHL7DefaultCharacterSet());
                List<Connection> conns = hl7app.getConnections();
                assertNotNull(prefix + "-Missing connections (original)", conns);
                List<Connection> conns1 = hl7app1.getConnections();
                assertNotNull(prefix + "-Missing connections (stored)", conns1);
                assertEquals(prefix + "-Number of connections", conns.size(), conns1.size());
                loop: for (Connection con : conns) {
                    for (Connection con1 : conns1) {
                      if (con.getPort() == con1.getPort() &&
                          isEqual(con.getHostname(), con1.getHostname()) &&
                          isEqual(con.getHttpProxy(), con1.getHttpProxy()))
                          continue loop;
                    }
                    fail(prefix + "-No Identical Connection found:" + con + "\nstored connections:" + conns1);
                }
            }
        }
/*_*/
    }

    private void checkXdsRepositoryDevice(String name, String appName, 
            String repUID, String[] urls, String[] mime, String logDir, boolean check) throws Exception {
        Device device = config.findDevice(name);
        XdsRepository app = device.getDeviceExtension(XdsRepository.class);
        String prefix = name+"-"+appName;
        assertEquals(prefix + "-ApplicationName", appName, app.getApplicationName());
        assertEquals(prefix + "-repositoryUID", repUID, app.getRepositoryUID());
        assertArrayEquals(prefix + "-RegistryURLs", urls, app.getRegistryURLs());
        assertEquals(prefix + "-SoapLogDir", logDir, app.getSoapLogDir());
        assertArrayEquals(prefix + "-MimeTypes", mime, app.getAcceptedMimeTypes());
        assertEquals(prefix + "-checkMimetype", check, app.isCheckMimetype());
    }
    
    private boolean isEqual(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }
        return o1.equals(o2);
    }
}
