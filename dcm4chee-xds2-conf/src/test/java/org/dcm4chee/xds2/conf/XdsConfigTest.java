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

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.prefs.Preferences;

import org.dcm4che.conf.api.ConfigurationNotFoundException;
import org.dcm4che.data.Code;
import org.dcm4che.data.Issuer;
import org.dcm4che.net.Connection;
import org.dcm4che.net.Device;
import org.dcm4che.net.hl7.HL7Application;
import org.dcm4che.net.hl7.HL7Device;
import org.dcm4che.util.SafeClose;
import org.dcm4chee.xds2.conf.XdsApplication;
import org.dcm4chee.xds2.conf.XdsConfiguration;
import org.dcm4chee.xds2.conf.ldap.LdapXdsConfiguration;
import org.dcm4chee.xds2.conf.prefs.PreferencesXdsConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotNull;

public class XdsConfigTest {

    private static final String XDS_REGISTRY1 = "XDS_REGISTRY1";
    private static final String XDS_REGISTRY2 = "XDS_REGISTRY2";
    private static final String XDS_REGISTRY3 = "XDS_REGISTRY3";
    private static final String XDS_REGISTRY4 = "XDS_REGISTRY4";
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
    private String AFFINITY_DOMAIN = "1.2.3.4.5&ISO";

    private static int testCount = 0;
    private XdsConfiguration config;

    @Before
    public void setUp() throws Exception {
        testCount++;
        config = System.getProperty("ldap") == null
                ? new PreferencesXdsConfiguration(Preferences.userRoot())
                : new LdapXdsConfiguration();
        cleanUp();
    }

    @After
    public void tearDown() throws Exception {
        if (System.getProperty("keep") == null)
            cleanUp();
        config.close();
    }

    @Test
    public void testSimple() throws Exception {
        config.persist(createXdsDevice("xds"+testCount, SITE_A, INST_A, XDS_REGISTRY1, AFFINITY_DOMAIN, MIME_TYPES1, null, false, false));
        if (config instanceof PreferencesXdsConfiguration)
            export(System.getProperty("export"));
        checkXdsDevice("xds"+testCount, SITE_A, INST_A, XDS_REGISTRY1, AFFINITY_DOMAIN, MIME_TYPES1, null, false, false, null);
        
    }
    @Test
    public void testWithOptional() throws Exception {
        config.persist(createXdsDevice("xds"+testCount, SITE_A, INST_A, XDS_REGISTRY2, AFFINITY_DOMAIN, MIME_TYPES2, "/log/xdslog", true, true));
        if (config instanceof PreferencesXdsConfiguration)
            export(System.getProperty("export"));
        checkXdsDevice("xds"+testCount, SITE_A, INST_A, XDS_REGISTRY2, AFFINITY_DOMAIN, MIME_TYPES2, "/log/xdslog", true, true, null);
    }
    @Test
    public void testWithHL7() throws Exception {
        XdsDevice xdsDevice = createXdsDevice("xds"+testCount, SITE_A, INST_A, XDS_REGISTRY3, AFFINITY_DOMAIN, MIME_TYPES1, "/log/xdslog", true, true);
        ArrayList<HL7Application> hl7Apps = new ArrayList<HL7Application>();
        HL7Application hl7App = createHL7Application(HL7_APP_NAME1, "hl7-conn", "localhost", 2575, 2576, xdsDevice);
        hl7Apps.add(hl7App);
        xdsDevice.addHL7Application(hl7App);
        config.persist(xdsDevice);
        if (config instanceof PreferencesXdsConfiguration)
            export(System.getProperty("export"));
        checkXdsDevice("xds"+testCount, SITE_A, INST_A, XDS_REGISTRY3, AFFINITY_DOMAIN, MIME_TYPES1, "/log/xdslog", true, true, hl7Apps);
        HL7Application hl7AppFound = config.findHL7Application(HL7_APP_NAME1);
        assertNotNull("findHL7Application", hl7AppFound);
    }

    @Test
    public void testModify() throws Exception {
        XdsDevice xdsDevice = createXdsDevice("xds"+testCount, SITE_A, INST_A, XDS_REGISTRY4, AFFINITY_DOMAIN, MIME_TYPES1, "/log/xdslog/m", true, true);
        ArrayList<HL7Application> hl7Apps = new ArrayList<HL7Application>();
        HL7Application hl7App = createHL7Application(HL7_APP_NAME2, "hl7-conn", "localhost", 2575, 2576, xdsDevice);
        hl7Apps.add(hl7App);
        xdsDevice.addHL7Application(hl7App);
        config.persist(xdsDevice);
        if (config instanceof PreferencesXdsConfiguration)
            export(System.getProperty("export"));
        checkXdsDevice("xds"+testCount, SITE_A, INST_A, XDS_REGISTRY4, AFFINITY_DOMAIN, MIME_TYPES1, "/log/xdslog/m", true, true, hl7Apps);
        XdsApplication xdsApp = xdsDevice.getXdsApplication(XDS_REGISTRY4);
        xdsApp.setAffinityDomain("5.4.3.2.1&ISO");
        xdsApp.setSoapLogDir("/log/xdslog/mod");
        xdsApp.setCreateMissingPIDs(false);
        for (Connection c : hl7App.getConnections()) {
            c.setPort(c.getPort()+10000);
            c.setHostname("changed");
        }
        HL7Application hl7App2 = createHL7Application(HL7_APP_NAME3, "hl7-conn", "localhost", 3575, 3576, xdsDevice);
        hl7Apps.add(hl7App2);
        xdsDevice.addHL7Application(hl7App2);
        config.merge(xdsDevice);
        checkXdsDevice("xds"+testCount, SITE_A, INST_A, XDS_REGISTRY4, "5.4.3.2.1&ISO", MIME_TYPES1, "/log/xdslog/mod", false, true, hl7Apps);
        HL7Application hl7AppFound = config.findHL7Application(HL7_APP_NAME2);
        assertNotNull("findHL7Application1", hl7AppFound);
        HL7Application hl7AppFound1 = config.findHL7Application(HL7_APP_NAME3);
        assertNotNull("findHL7Application2", hl7AppFound1);
    }

    private void cleanUp() throws Exception {
        try {
            config.removeDevice("xds"+testCount);
        } catch (ConfigurationNotFoundException e) {}
    }

    private void export(String name) throws Exception {
        if (name == null)
            return;

        OutputStream os = new FileOutputStream(name);
        try {
            ((PreferencesXdsConfiguration) config)
                    .getDicomConfigurationRoot().exportSubtree(os);
        } finally {
            SafeClose.close(os);
        }
    }

    private Device init(Device device, Issuer issuer, Code institutionCode)
            throws Exception {
        String name = device.getDeviceName();
        device.setIssuerOfPatientID(issuer);
        device.setIssuerOfAccessionNumber(issuer);
        if (institutionCode != null) {
            device.setInstitutionNames(institutionCode.getCodeMeaning());
            device.setInstitutionCodes(institutionCode);
        }
        return device;
    }


    private XdsDevice createXdsDevice(String name, Issuer issuer, Code institutionCode, String appName, 
            String affinityDomain, String[] mime, String logDir, boolean createPID, boolean createCode) throws Exception {
         XdsDevice device = new XdsDevice(name);
         init(device, issuer, institutionCode);
         XdsApplication xdsApp = new XdsApplication(appName);
         device.addXdsApplication(xdsApp);
         xdsApp.setAffinityDomain(affinityDomain);
         xdsApp.setAcceptedMimeTypes(mime);
         xdsApp.setSoapLogDir(logDir);
         xdsApp.setCreateMissingPIDs(createPID);
         xdsApp.setCreateMissingCodes(createCode);
         return device;
     }

    private HL7Application createHL7Application(String appName, String connName,
            String host, int port, int tlsPort, XdsDevice xdsDevice) throws Exception {
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
            int port, XdsDevice xdsDevice) {
         for (Connection c : xdsDevice.listConnections()) {
             if (c.getCommonName().equals(connName))
                 return c;
         }
         Connection conn = new Connection(connName, host, port);
         xdsDevice.addConnection(conn);
        return conn;
    }
    
    private void checkXdsDevice(String name, Issuer issuer, Code institutionCode, String appName, 
            String affinityDomain, String[] mime, String logDir, boolean createPID, boolean createCode, Collection<HL7Application> hl7Apps) throws Exception {
        XdsApplication app = config.findXdsApplication(appName);
        assertEquals(name+"-"+appName+"-AffinityDomain", affinityDomain, app.getAffinityDomain());
        assertEquals(name+"-"+appName+"-ApplicationName", appName, app.getApplicationName());
        assertEquals(name+"-"+appName+"-SoapLogDir", logDir, app.getSoapLogDir());
        assertArrayEquals(name+"-"+appName+"-MimeTypes", mime, app.getAcceptedMimeTypes());
        assertEquals(name+"-"+appName+"-createCode", createCode, app.isCreateMissingCodes());
        assertEquals(name+"-"+appName+"-createPIDs", createPID, app.isCreateMissingPIDs());
        XdsDevice xdsDevice = app.getDevice();
        assertEquals(name+"-"+appName+"-NumberOfHL7Apps", 
                hl7Apps == null ? 0 : hl7Apps.size(), xdsDevice.getHL7Applications().size());
        if (hl7Apps != null) {
            HL7Application hl7app1;
            for (HL7Application hl7app : hl7Apps) {
                hl7app1 = xdsDevice.getHL7Application(hl7app.getApplicationName());
                assertNotNull(name+"-"+appName+"-HL7Applicationname "+hl7app.getApplicationName()+" not found!", hl7app1);
                assertArrayEquals(name+"-"+appName+"-AcceptedMessageTypes", hl7app.getAcceptedMessageTypes(),hl7app1.getAcceptedMessageTypes());
                assertArrayEquals(name+"-"+appName+"-AcceptedSendingApplications", hl7app.getAcceptedSendingApplications(),hl7app1.getAcceptedSendingApplications());
                assertEquals(name+"-"+appName+"-HL7DefaultCharacterSet", hl7app.getHL7DefaultCharacterSet(),hl7app1.getHL7DefaultCharacterSet());
                List<Connection> conns = hl7app.getConnections();
                assertNotNull(name+"-"+appName+"-Missing connections (original)", conns);
                List<Connection> conns1 = hl7app1.getConnections();
                assertNotNull(name+"-"+appName+"-Missing connections (stored)", conns1);
                assertEquals(name+"-"+appName+"-Number of connections", conns.size(), conns1.size());
                loop: for (Connection con : conns) {
                    for (Connection con1 : conns1) {
                      if (con.getPort() == con1.getPort() &&
                          isEqual(con.getHostname(), con1.getHostname()) &&
                          isEqual(con.getHttpProxy(), con1.getHttpProxy()))
                          continue loop;
                    }
                    fail(name+"-"+appName+"-No Identical Connection found:"+con+"\nstored connections:"+conns1);
                }
            }
        }
    }
    
    private boolean isEqual(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }
        return o1.equals(o2);
    }
}
