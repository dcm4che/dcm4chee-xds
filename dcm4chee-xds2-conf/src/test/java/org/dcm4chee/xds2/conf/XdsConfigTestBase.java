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

import org.dcm4che3.audit.AuditMessages;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.ConfigurationNotFoundException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.data.Code;
import org.dcm4che3.data.Issuer;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.audit.AuditLogger;
import org.dcm4che3.net.audit.AuditRecordRepository;
import org.dcm4che3.net.hl7.HL7Application;
import org.dcm4che3.net.hl7.HL7DeviceExtension;
import org.junit.After;
import org.junit.Test;

public class XdsConfigTestBase {

    private static final String PIX_MANAGER_APP2 = "PIX_MANAGER_APP2";
    private static final String PIX_MANAGER_APP = "PIX_MANAGER_APP";
    private static final String PIX_CONSUMER_APP2 = "PIX_CONSUMER_APP2";
    private static final String PIX_CONSUMER_APP = "PIX_CONSUMER_APP";
    private static final String LOG_XDSLOG = "log/xdslog";
    private static final String VAR_LOG_XDSLOG = "var/log/xdslog";
    private static final String HOME_COMMUNITY_ID = "1.3.5.7.9";
    private static final String[] AUTHORITY_MAP = new String[]{"1.2.3.4.9.999|&1.2.3.4.9.99.2&ISO"};
    private static final String[] AUTHORITY_MAP2 = new String[]{"1.2.3.4.9.999|&1.2.3.4.9.99.2&ISO", "1.2.3.4.9.990|&1.2.3.4.9.99.5&ISO"};
    private static final String HOME_COMMUNITY2_ID = "1.3.5.7.9.111";
    private static final String XCA_RESPONDING_GW = "XCARespondingGW";
    private static final String XCA_INITIATING_GW = "XCAInitiatingGW";
    private static final String XCAI_RESPONDING_GW = "XCAiRespondingGW";
    private static final String XCAI_INITIATING_GW = "XCAiInitiatingGW";
    private static final String DEFAULT_XDSLOG = "/var/log/xdslog";
    protected static final String DEFAULT_XDS_DEVICE = "dcm4chee-xds2-default";
    private static final String XDS_REGISTRY1 = "XDS_REGISTRY1";
    private static final String XDS_REGISTRY2 = "XDS_REGISTRY2";
    private static final String XDS_REGISTRY3 = "XDS_REGISTRY3";
    private static final String XDS_REGISTRY4 = "XDS_REGISTRY4";
    
    private static final String XDS_REPOSITORY1 = "XDS_REPOSITORY1";
    private static final String XDS_REPO1_UID = "1.2.3.4";
    private static final String XDS_REPO2_UID = "4.3.2.1";
    private static final String XDS_REGISTRY_URI = "http://xds/registry";
    private static final String XDS_REGISTRY2_URI = "http://xds2/registry";
    private static final String[] XDS_REGISTRY_URIS = {"0|http://xds/registry"};
    private static final String[] XDS_REGISTRY2_URIS = {"0|http://xds1/registry","1.2.3.4|http://xds2/registry"};
    private static final String[] DEFAULT_XDS_REGISTRY_URI = {"0|http://localhost:8080/dcm4chee-xds/XDSbRegistry/b"};
    private static final String[] XDS_REPOSITORY_URIS = {"0|http://xds/repository"};
    private static final String[] XDS_REPOSITORY2_URIS = {"0|http://xds1/repository", "1.2.3.4|http://xds2/repository"};
    private static final String[] XDS_RESPGW_URIS = {"0|http://xds/respGW"};
    private static final String[] XDS_RESPGW2_URIS = {"0|http://xds1/respGW", "1.2.3.4|http://xds2/respGW"};
    private static final String[] XDSI_SRC_URI = {"0|http://xds/imgsrc"};
    private static final String[] XDSI_SRC2_URI = {"0|http://xds1/imgsrc","1.2.3.4|http://xds2/imgsrc"};
    
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
    private static final String[] MIME_TYPES2 = new String[]{"application/xml","application/dicom","application/pdf", "text/plain", "text/xml"};
    private String[] AFFINITY_DOMAIN = {"1.2.3.4.5"};
    private static final String[] AFFINITY_DOMAIN2 = {"5.4.3.2.1"};

    protected static int testCount = 0;
    protected static String testDeviceName;
    protected static String arrDeviceName;
    protected DicomConfiguration config;

    @After
    public void tearDown() throws Exception {
        if (System.getProperty("keep") == null)
            cleanUp();
        if (config != null)
            config.close();
    }
    @Test
    public void testRegistry() throws Exception {
        Device d = createDevice("registry", SITE_A, INST_A);
        addRegistry(d, XDS_REGISTRY1, AFFINITY_DOMAIN, MIME_TYPES1, null, false, false);
        config.persist(d);
        afterPersist();
        checkXdsRegistry(SITE_A, INST_A, XDS_REGISTRY1, AFFINITY_DOMAIN, MIME_TYPES1, null, false, false, true, true, false, null);
    }
    @Test
    public void testRegistryWithOptional() throws Exception {
        Device d = createDevice("registry", SITE_A, INST_A);
        addRegistry(d, XDS_REGISTRY2, AFFINITY_DOMAIN, MIME_TYPES2, "/log/xdslog", true, true);
        config.persist(d);
        afterPersist();
        checkXdsRegistry(SITE_A, INST_A, XDS_REGISTRY2, AFFINITY_DOMAIN, MIME_TYPES2, "/log/xdslog", true, true, true, true, false, null);
    }
    @Test
    public void testRegistryWithHL7() throws Exception {
        Device device = createDevice("registry_hl7", SITE_A, INST_A);
        addRegistry(device, XDS_REGISTRY3, AFFINITY_DOMAIN, MIME_TYPES1, "/log/xdslog", true, true);
        ArrayList<HL7Application> hl7Apps = addHL7(device);
        config.persist(device);
        afterPersist();
        checkXdsRegistry(SITE_A, INST_A, XDS_REGISTRY3, AFFINITY_DOMAIN, MIME_TYPES1, "/log/xdslog", true, true, true, true, false, hl7Apps);
    }

    @Test
    public void testModify() throws Exception {
        Device d = createDevice("registry_modify", SITE_A, INST_A);
        addRegistry(d, XDS_REGISTRY4, AFFINITY_DOMAIN, MIME_TYPES1, "/log/xdslog/m", true, true);
        HL7DeviceExtension hl7Ext = new HL7DeviceExtension();
        d.addDeviceExtension(hl7Ext);
        ArrayList<HL7Application> hl7Apps = new ArrayList<HL7Application>();
        HL7Application hl7App = createHL7Application(HL7_APP_NAME2, "hl7-conn", "localhost", 2575, 2576, d);
        hl7Apps.add(hl7App);
        hl7Ext.addHL7Application(hl7App);
        config.persist(d);
        checkXdsRegistry(SITE_A, INST_A, XDS_REGISTRY4, AFFINITY_DOMAIN, MIME_TYPES1, "/log/xdslog/m", true, true, true, true, false, hl7Apps);
        XdsRegistry xdsApp = d.getDeviceExtension(XdsRegistry.class);
        xdsApp.setAffinityDomain(AFFINITY_DOMAIN2);
        xdsApp.setSoapLogDir("/log/xdslog/mod");
        xdsApp.setCreateMissingPIDs(false);
        xdsApp.setAcceptedMimeTypes(MIME_TYPES2);
        xdsApp.setCheckAffinityDomain(false);
        xdsApp.setCheckMimetype(false);
        xdsApp.setPreMetadataCheck(true);

        for (Connection c : hl7App.getConnections()) {
            c.setPort(c.getPort()+10000);
            c.setHostname("changed");
        }
        HL7Application hl7App2 = createHL7Application(HL7_APP_NAME3, "hl7-conn", "localhost", 3575, 3576, d);
        hl7Apps.add(hl7App2);
        hl7Ext.addHL7Application(hl7App2);
        config.merge(d);
        afterPersist();
        checkXdsRegistry(SITE_A, INST_A, XDS_REGISTRY4, AFFINITY_DOMAIN2, MIME_TYPES2, "/log/xdslog/mod", false, true, false, false, true, hl7Apps);
    }

    @Test
    public void testRepository() throws Exception {
        Device d = createDevice("repository", SITE_A, INST_A);
        addRepository(d, XDS_REPOSITORY1, XDS_REPO1_UID, XDS_REGISTRY_URIS, MIME_TYPES1, null);
        config.persist(d);
        afterPersist();
        checkXdsRepository(XDS_REPOSITORY1, XDS_REPO1_UID, XDS_REGISTRY_URIS, MIME_TYPES1, null, true);
        XdsRepository rep = d.getDeviceExtension(XdsRepository.class);
        rep.setAcceptedMimeTypes(MIME_TYPES2);
        rep.setCheckMimetype(false);
        rep.setRepositoryUID(XDS_REPO2_UID);
        rep.setRegistryURLs(XDS_REGISTRY2_URIS);
        config.merge(d);
        checkXdsRepository(XDS_REPOSITORY1, XDS_REPO2_UID, XDS_REGISTRY2_URIS, MIME_TYPES2, null, false);
    }
    
    @Test
    public void testXCARespondingGW() throws Exception {
        Device d = createDevice("xca_resp_gw", SITE_A, INST_A);
        XCARespondingGWCfg respGW = addXCARespondingGW(d);
        config.persist(d);
        afterPersist();
        checkXCARespondingGW(XCA_RESPONDING_GW, HOME_COMMUNITY_ID, XDS_REGISTRY_URI, XDS_REPOSITORY_URIS, LOG_XDSLOG);
        respGW.setHomeCommunityID(HOME_COMMUNITY2_ID);
        respGW.setRegistryURL(XDS_REGISTRY2_URI);
        respGW.setRepositoryURLs(XDS_REPOSITORY2_URIS);
        respGW.setSoapLogDir(VAR_LOG_XDSLOG);
        config.merge(d);
        checkXCARespondingGW(XCA_RESPONDING_GW, HOME_COMMUNITY2_ID, XDS_REGISTRY2_URI, XDS_REPOSITORY2_URIS, VAR_LOG_XDSLOG);
    }
    @Test
    public void testXCAInitiatingGW() throws Exception {
        Device d = createDevice("xca_init_gw", SITE_A, INST_A);
        XCAInitiatingGWCfg initGW = addXCAInitiatingGW(d);
        config.persist(d);
        afterPersist();
        checkXCAInitiatingGW(XCA_INITIATING_GW, HOME_COMMUNITY_ID, XDS_REGISTRY_URI, XDS_REPOSITORY_URIS, LOG_XDSLOG,
                PIX_CONSUMER_APP, PIX_MANAGER_APP, AUTHORITY_MAP, false, true, XDS_RESPGW_URIS, XDS_RESPGW_URIS);
        initGW.setHomeCommunityID(HOME_COMMUNITY2_ID);
        initGW.setRegistryURL(XDS_REGISTRY2_URI);
        initGW.setRepositoryURLs(XDS_REPOSITORY2_URIS);
        initGW.setSoapLogDir(VAR_LOG_XDSLOG);
        initGW.setLocalPIXConsumerApplication(PIX_CONSUMER_APP2);
        initGW.setRemotePIXManagerApplication(PIX_MANAGER_APP2);
        initGW.setAssigningAuthoritiesMap(AUTHORITY_MAP2);
        initGW.setAsync(true);
        initGW.setAsyncHandler(false);
        initGW.setRespondingGWURLs(XDS_RESPGW2_URIS);
        initGW.setRespondingGWRetrieveURLs(XDS_RESPGW2_URIS);
        config.merge(d);
        checkXCAInitiatingGW(XCA_INITIATING_GW, HOME_COMMUNITY2_ID, XDS_REGISTRY2_URI, XDS_REPOSITORY2_URIS, VAR_LOG_XDSLOG,
            PIX_CONSUMER_APP2, PIX_MANAGER_APP2, AUTHORITY_MAP2, true, false, XDS_RESPGW2_URIS, XDS_RESPGW2_URIS);
    }
    
    @Test
    public void testXCAiRespondingGW() throws Exception {
        Device d = createDevice("xcai_resp_gw", SITE_A, INST_A);
        XCAiRespondingGWCfg respGW = addXCAiRespondingGW(d);
        config.persist(d);
        afterPersist();
        checkXCAiRespondingGW(XCAI_RESPONDING_GW, HOME_COMMUNITY_ID, XDSI_SRC_URI, LOG_XDSLOG);
        respGW.setHomeCommunityID(HOME_COMMUNITY2_ID);
        respGW.setXDSiSourceURLs(XDSI_SRC2_URI);
        respGW.setSoapLogDir(VAR_LOG_XDSLOG);
        config.merge(d);
        checkXCAiRespondingGW(XCAI_RESPONDING_GW, HOME_COMMUNITY2_ID, XDSI_SRC2_URI, VAR_LOG_XDSLOG);
    }

    @Test
    public void testXCAiInitiatingGW() throws Exception {
        Device d = createDevice("xcai_init_gw", SITE_A, INST_A);
        XCAiInitiatingGWCfg initGW = addXCAiInitiatingGW(d);
        config.persist(d);
        afterPersist();
        checkXCAiInitiatingGW(XCAI_INITIATING_GW, HOME_COMMUNITY_ID, XDSI_SRC_URI, LOG_XDSLOG,
                false, true, XDS_RESPGW_URIS);
        initGW.setHomeCommunityID(HOME_COMMUNITY2_ID);
        initGW.setXDSiSourceURLs(XDSI_SRC2_URI);
        initGW.setSoapLogDir(VAR_LOG_XDSLOG);
        initGW.setAsync(true);
        initGW.setAsyncHandler(false);
        initGW.setRespondingGWURLs(XDS_RESPGW2_URIS);
        config.merge(d);
        checkXCAiInitiatingGW(XCAI_INITIATING_GW, HOME_COMMUNITY2_ID, XDSI_SRC2_URI, VAR_LOG_XDSLOG,
            true, false, XDS_RESPGW2_URIS);
    }
    
    @Test
    public void testDefaultConfig() throws Exception {
        testDeviceName = DEFAULT_XDS_DEVICE;
        Device d = createDevice(DEFAULT_XDS_DEVICE, SITE_A, INST_A);
        XdsRegistry reg = addRegistry(d, XDS_REGISTRY1, AFFINITY_DOMAIN, MIME_TYPES2, DEFAULT_XDSLOG, true, true);
        reg.setCheckAffinityDomain(false);
        reg.setCheckMimetype(false);
        XdsRepository rep = new XdsRepository();
        d.addDeviceExtension(rep);
        rep.setApplicationName(XDS_REPOSITORY1);
        rep.setRepositoryUID(XDS_REPO1_UID);
        rep.setRegistryURLs(DEFAULT_XDS_REGISTRY_URI);
        rep.setAcceptedMimeTypes(MIME_TYPES2);
        rep.setSoapLogDir(DEFAULT_XDSLOG);
        rep.setCheckMimetype(false);
        rep.setAllowedCipherHostname("*");
        rep.setLogFullMessageHosts(new String[]{});
        this.addXCARespondingGW(d);
        this.addXCAInitiatingGW(d);
        this.addXCAiRespondingGW(d);
        this.addXCAiInitiatingGW(d);
        ArrayList<HL7Application> hl7Apps = this.addHL7(d);
        this.addAuditLogger(d);
        config.persist(d);
        afterPersist();
        checkXdsRegistry(SITE_A, INST_A, XDS_REGISTRY1, AFFINITY_DOMAIN, MIME_TYPES2, DEFAULT_XDSLOG, true, true, false, false, false, hl7Apps);
        checkXdsRepository(XDS_REPOSITORY1, XDS_REPO1_UID, DEFAULT_XDS_REGISTRY_URI, MIME_TYPES2, DEFAULT_XDSLOG, false);
        checkXCARespondingGW(XCA_RESPONDING_GW, HOME_COMMUNITY_ID, XDS_REGISTRY_URI, XDS_REPOSITORY_URIS, LOG_XDSLOG);
        checkXCAInitiatingGW(XCA_INITIATING_GW, HOME_COMMUNITY_ID, XDS_REGISTRY_URI, XDS_REPOSITORY_URIS, LOG_XDSLOG,
                PIX_CONSUMER_APP, PIX_MANAGER_APP, AUTHORITY_MAP, false, true, XDS_RESPGW_URIS, XDS_RESPGW_URIS);
        checkXCAiRespondingGW(XCAI_RESPONDING_GW, HOME_COMMUNITY_ID, XDSI_SRC_URI, LOG_XDSLOG);
        checkXCAiInitiatingGW(XCAI_INITIATING_GW, HOME_COMMUNITY_ID, XDSI_SRC_URI, LOG_XDSLOG,
                false, true, XDS_RESPGW_URIS);
    }

    protected void cleanUp() throws Exception {
        if (config == null || testCount==0)
            return;
        try {
            config.removeDevice(testDeviceName);
        } catch (ConfigurationNotFoundException e) {}
        if ( arrDeviceName != null) {
            try {
                config.removeDevice(arrDeviceName);
            } catch (ConfigurationNotFoundException e) {}
            arrDeviceName = null;
        }
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


    private Device createDevice(String name, Issuer issuer, Code institutionCode) throws Exception {
        testDeviceName = name;
        Device device = new Device(name);
        init(device, issuer, institutionCode);
        return device;
    }
    
    private XdsRegistry addRegistry(Device device, String appName, String[] affinityDomain,
            String[] mime, String logDir, boolean createPID,
            boolean createCode) {
        XdsRegistry registry = new XdsRegistry();
         device.addDeviceExtension(registry);
         registry.setApplicationName(appName);
         registry.setAffinityDomain(affinityDomain);
         registry.setAffinityDomainConfigDir("${jboss.server.config.dir}/affinitydomain");
         registry.setAcceptedMimeTypes(mime);
         registry.setSoapLogDir(logDir);
         registry.setCreateMissingPIDs(createPID);
         registry.setCreateMissingCodes(createCode);
         registry.setCheckAffinityDomain(true);
         registry.setCheckMimetype(true);
         registry.setPreMetadataCheck(false);
         return registry;
    }

    private XdsRepository addRepository(Device device, String appName, String repositoryUID,
            String[] registryURLs, String[] mime, String logDir) {
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
         return rep;
    }
    private XCARespondingGWCfg addXCARespondingGW(Device d) {
        XCARespondingGWCfg respGW = new XCARespondingGWCfg();
        d.addDeviceExtension(respGW);
        respGW.setApplicationName(XCA_RESPONDING_GW);
        respGW.setHomeCommunityID(HOME_COMMUNITY_ID);
        respGW.setRegistryURL(XDS_REGISTRY_URI);
        respGW.setRepositoryURLs(XDS_REPOSITORY_URIS);
        respGW.setSoapLogDir(LOG_XDSLOG);
        return respGW;
    }
    private XCAInitiatingGWCfg addXCAInitiatingGW(Device d) {
        XCAInitiatingGWCfg initGW = new XCAInitiatingGWCfg();
        d.addDeviceExtension(initGW);
        initGW.setApplicationName(XCA_INITIATING_GW);
        initGW.setHomeCommunityID(HOME_COMMUNITY_ID);
        initGW.setRegistryURL(XDS_REGISTRY_URI);
        initGW.setRepositoryURLs(XDS_REPOSITORY_URIS);
        initGW.setSoapLogDir(LOG_XDSLOG);
        initGW.setLocalPIXConsumerApplication(PIX_CONSUMER_APP);
        initGW.setRemotePIXManagerApplication(PIX_MANAGER_APP);
        initGW.setAssigningAuthoritiesMap(AUTHORITY_MAP);
        initGW.setAsync(false);
        initGW.setAsyncHandler(true);
        initGW.setRespondingGWURLs(XDS_RESPGW_URIS);
        initGW.setRespondingGWRetrieveURLs(XDS_RESPGW_URIS);
        return initGW;
    }
    private XCAiRespondingGWCfg addXCAiRespondingGW(Device d) {
        XCAiRespondingGWCfg respGW = new XCAiRespondingGWCfg();
        d.addDeviceExtension(respGW);
        respGW.setApplicationName(XCAI_RESPONDING_GW);
        respGW.setHomeCommunityID(HOME_COMMUNITY_ID);
        respGW.setXDSiSourceURLs(XDSI_SRC_URI);
        respGW.setSoapLogDir(LOG_XDSLOG);
        return respGW;
    }
    private XCAiInitiatingGWCfg addXCAiInitiatingGW(Device d) {
        XCAiInitiatingGWCfg initGW = new XCAiInitiatingGWCfg();
        d.addDeviceExtension(initGW);
        initGW.setApplicationName(XCAI_INITIATING_GW);
        initGW.setHomeCommunityID(HOME_COMMUNITY_ID);
        initGW.setXDSiSourceURLs(XDSI_SRC_URI);
        initGW.setSoapLogDir(LOG_XDSLOG);
        initGW.setAsync(false);
        initGW.setAsyncHandler(true);
        initGW.setRespondingGWURLs(XDS_RESPGW_URIS);
        return initGW;
    }
    private ArrayList<HL7Application> addHL7(Device device) throws Exception {
        HL7DeviceExtension hl7Ext = new HL7DeviceExtension();
        device.addDeviceExtension(hl7Ext);
        ArrayList<HL7Application> hl7Apps = new ArrayList<HL7Application>();
        HL7Application hl7App = createHL7Application(HL7_APP_NAME1, "hl7-conn", "localhost", 2575, 2576, device);
        hl7Apps.add(hl7App);
        hl7Ext.addHL7Application(hl7App);
        return hl7Apps;
    }
    private AuditLogger addAuditLogger(Device device) throws Exception {
        Connection udp = new Connection("audit-udp", "localhost");
        udp.setProtocol(Connection.Protocol.SYSLOG_UDP);
        udp.setPort(514);
        device.addConnection(udp);
        arrDeviceName = "dcm4cheARR";
        Device arrDevice = new Device(arrDeviceName);
        AuditRecordRepository arr = new AuditRecordRepository();
        arrDevice.addDeviceExtension(arr);
        Connection arrUDP = new Connection("audit-udp", "arr.dcm4che.org");
        arrUDP.setProtocol(Connection.Protocol.SYSLOG_UDP);
        arrUDP.setPort(514);
        arrDevice.addConnection(arrUDP);
        arr.addConnection(arrUDP);
        config.persist(arrDevice);

        AuditLogger logger = new AuditLogger();
        device.addDeviceExtension(logger);
        logger.addConnection(udp);
        logger.setAuditRecordRepositoryDevice(arrDevice);
        logger.setSchemaURI(AuditMessages.SCHEMA_URI);
        return logger;
    }

    private HL7Application createHL7Application(String appName, String connName,
            String host, int port, int tlsPort, Device xdsDevice) throws Exception {
         HL7Application hl7App = new HL7Application(appName);
         Connection conn = getOrCreateConnection(connName, host, port, xdsDevice);
         hl7App.addConnection(conn);
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
    private void checkXdsRegistry(Issuer issuer, Code institutionCode, String appName, 
            String[] affinityDomain, String[] mime, String logDir, boolean createPID, boolean createCode,
            boolean checkAD, boolean checkMime, boolean preMetaCheck, Collection<HL7Application> hl7Apps) throws Exception {
        Device device = config.findDevice(testDeviceName);
        XdsRegistry app = device.getDeviceExtension(XdsRegistry.class);
        String prefix = testDeviceName+"-"+appName;
        assertArrayEquals(prefix + "-AffinityDomain: count:", affinityDomain, app.getAffinityDomain());
        assertEquals(prefix + "-ApplicationName", appName, app.getApplicationName());
        assertEquals(prefix + "-SoapLogDir", logDir, app.getSoapLogDir());
        assertArrayEquals(prefix + "-MimeTypes", mime, app.getAcceptedMimeTypes());
        assertEquals(prefix + "-createCode", createCode, app.isCreateMissingCodes());
        assertEquals(prefix + "-createPIDs", createPID, app.isCreateMissingPIDs());
        assertEquals(prefix + "-checkAffinityDomain", checkAD, app.isCheckAffinityDomain());
        assertEquals(prefix + "-checkMimetype", checkMime, app.isCheckMimetype());
        assertEquals(prefix + "-preMetadataCheck", preMetaCheck, app.isPreMetadataCheck());
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

    private void checkXdsRepository(String appName, 
            String repUID, String[] urls, String[] mime, String logDir, boolean check) throws Exception {
        Device device = config.findDevice(testDeviceName);
        XdsRepository app = device.getDeviceExtension(XdsRepository.class);
        String prefix = testDeviceName+"-"+appName;
        assertEquals(prefix + "-ApplicationName", appName, app.getApplicationName());
        assertEquals(prefix + "-repositoryUID", repUID, app.getRepositoryUID());
        assertArrayEqualContent(prefix + "-RegistryURLs", urls, app.getRegistryURLs());
        assertEquals(prefix + "-SoapLogDir", logDir, app.getSoapLogDir());
        assertArrayEquals(prefix + "-MimeTypes", mime, app.getAcceptedMimeTypes());
        assertEquals(prefix + "-checkMimetype", check, app.isCheckMimetype());
    }
    
    private void checkXCARespondingGW(String appName, 
            String homeID, String regUrl, String[] repUrls, String logDir) throws Exception {
        Device device = config.findDevice(testDeviceName);
        XCARespondingGWCfg app = device.getDeviceExtension(XCARespondingGWCfg.class);
        String prefix = testDeviceName+"-"+appName;
        assertEquals(prefix + "-ApplicationName", appName, app.getApplicationName());
        assertEquals(prefix + "-HomeCommunityID", homeID, app.getHomeCommunityID());
        assertEquals(prefix + "-RegistryURL", regUrl, app.getRegistryURL());
        assertArrayEqualContent(prefix + "-RepositoryURLs", repUrls, app.getRepositoryURLs());
        assertEquals(prefix + "-SoapLogDir", logDir, app.getSoapLogDir());
    }
    private void checkXCAInitiatingGW(String appName, String homeID, String regUrl, 
            String[] repUrls, String logDir, String pixConsumer, String pixMgr, String[] authMap,
            boolean async, boolean asyncHandler, String[] rspGWurls, String[] rspGWRetrUrls) throws Exception {
        Device device = config.findDevice(testDeviceName);
        XCAInitiatingGWCfg app = device.getDeviceExtension(XCAInitiatingGWCfg.class);
        String prefix = testDeviceName+"-"+appName;
        assertEquals(prefix + "-ApplicationName", appName, app.getApplicationName());
        assertEquals(prefix + "-HomeCommunityID", homeID, app.getHomeCommunityID());
        assertEquals(prefix + "-RegistryURL", regUrl, app.getRegistryURL());
        assertArrayEqualContent(prefix + "-RepositoryURLs", repUrls, app.getRepositoryURLs());
        assertEquals(prefix + "-SoapLogDir", logDir, app.getSoapLogDir());

        assertEquals(prefix + "-PIXConsumer", pixConsumer, app.getLocalPIXConsumerApplication());
        assertEquals(prefix + "-PIXManager", pixMgr, app.getRemotePIXManagerApplication());
        assertArrayEqualContent(prefix + "-AssigningAuthMap", authMap, app.getAssigningAuthoritiesMap());
        assertEquals(prefix + "-Async", async, app.isAsync());
        assertEquals(prefix + "-AsyncHandler", asyncHandler, app.isAsyncHandler());
        assertArrayEqualContent(prefix + "-RespondingGWURLs", rspGWurls, app.getRespondingGWURLs());
        assertArrayEqualContent(prefix + "-RespondingGWRetrieveURLs", rspGWRetrUrls, app.getRespondingGWRetrieveURLs());
    }
    private void checkXCAiRespondingGW(String appName, 
            String homeID, String[] imgsrcUrls, String logDir) throws Exception {
        Device device = config.findDevice(testDeviceName);
        XCAiRespondingGWCfg app = device.getDeviceExtension(XCAiRespondingGWCfg.class);
        String prefix = testDeviceName+"-"+appName;
        assertEquals(prefix + "-ApplicationName", appName, app.getApplicationName());
        assertEquals(prefix + "-HomeCommunityID", homeID, app.getHomeCommunityID());
        assertArrayEqualContent(prefix + "-ImagingSourceURL", imgsrcUrls, app.getXDSiSourceURLs());
        assertEquals(prefix + "-SoapLogDir", logDir, app.getSoapLogDir());
    }
    private void checkXCAiInitiatingGW(String appName, String homeID, 
            String[] imgsrcUrls, String logDir, boolean async, boolean asyncHandler, String[] rspGWurls) throws Exception {
        Device device = config.findDevice(testDeviceName);
        XCAiInitiatingGWCfg app = device.getDeviceExtension(XCAiInitiatingGWCfg.class);
        String prefix = testDeviceName+"-"+appName;
        assertEquals(prefix + "-ApplicationName", appName, app.getApplicationName());
        assertEquals(prefix + "-HomeCommunityID", homeID, app.getHomeCommunityID());
        assertArrayEqualContent(prefix + "-ImagingSourceURL", imgsrcUrls, app.getXDSiSourceURLs());
        assertEquals(prefix + "-SoapLogDir", logDir, app.getSoapLogDir());

        assertEquals(prefix + "-Async", async, app.isAsync());
        assertEquals(prefix + "-AsyncHandler", asyncHandler, app.isAsyncHandler());
        assertArrayEqualContent(prefix + "-RespondingGWURLs", rspGWurls, app.getRespondingGWURLs());
    }
   
    protected HL7Application findHL7Application(String hl7AppName1) throws ConfigurationException {
        return null;
    }
    
    private boolean isEqual(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }
        return o1.equals(o2);
    }
    
    private void assertArrayEqualContent(String msg, String[] expected,
            String[] actual) {
        if (expected == null) {
            if (actual != null) 
                fail(msg+" expected null! String[].length:"+actual.length);
            return;
        } else if (actual == null) {
            fail(msg+" expected not null!");
        }
        if (expected.length != actual.length)
            fail(msg+" array length is different! expected:"+expected.length+" but was "+actual.length);
        String a;
        loop: for (String e : expected) {
            for (int i = 0 ; i < actual.length ; i++) {
                a = actual[i];
                if ((e == null && a == null) || (e != null && e.equals(a))) {
                    actual[i]="@"+a+"@DELETED";
                    continue loop;
                }
            }
            fail(msg+" Expected element not found:"+e);
        }
    }

}
