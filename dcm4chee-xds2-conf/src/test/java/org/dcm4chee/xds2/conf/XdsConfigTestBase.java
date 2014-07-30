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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dcm4che3.audit.AuditMessages;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.ConfigurationNotFoundException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.data.Code;
import org.dcm4che3.data.Issuer;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DeviceExtension;
import org.dcm4che3.net.audit.AuditLogger;
import org.dcm4che3.net.audit.AuditRecordRepository;
import org.dcm4che3.net.hl7.HL7Application;
import org.dcm4che3.net.hl7.HL7DeviceExtension;
import org.dcm4chee.xds2.conf.XCAInitiatingGWCfg.GatewayReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class XdsConfigTestBase {

    private static final String PIX_MANAGER_APP2 = "PIX_MANAGER_APP2";
    private static final String PIX_MANAGER_APP = "PIX_MANAGER_APP";
    private static final String PIX_CONSUMER_APP2 = "PIX_CONSUMER_APP2";
    private static final String PIX_CONSUMER_APP = "PIX_CONSUMER_APP";
    private static final String LOG_XDSLOG = "log/xdslog";
    private static final String VAR_LOG_XDSLOG = "var/log/xdslog";
    private static final String HOME_COMMUNITY_ID = "1.3.5.7.9";
    private static final String HOME_COMMUNITY2_ID = "1.3.5.7.9.111";
    private static final String XCA_RESPONDING_GW = "XCARespondingGW";
    private static final String XCA_INITIATING_GW = "XCAInitiatingGW";
    private static final String XCAI_RESPONDING_GW = "XCAiRespondingGW";
    private static final String XCAI_INITIATING_GW = "XCAiInitiatingGW";
    private static final String DEFAULT_XDSLOG = "/var/log/xdslog";
    protected static final String DEFAULT_XDS_DEVICE = "dcm4chee-xds";
    private static final String XDS_REGISTRY1 = "XDS_REGISTRY1";
    private static final String XDS_REGISTRY2 = "XDS_REGISTRY2";
    private static final String XDS_REGISTRY3 = "XDS_REGISTRY3";
    private static final String XDS_REGISTRY4 = "XDS_REGISTRY4";

    private static final String XDS_REPOSITORY1 = "XDS_REPOSITORY1";
    private static final String XDS_REPO1_UID = "1.2.3.4";
    private static final String XDS_REPO2_UID = "4.3.2.1";
    private static final String HL7_APP_NAME1 = "XDS^DCM4CHEE";
    private static final String HL7_APP_NAME2 = "XDS2^DCM4CHEE";
    private static final String HL7_APP_NAME3 = "XDS3^DCM4CHEE";
    private static final String[] HL7_MESSAGE_TYPES = { "ADT^A02", "ADT^A03", "ADT^A06", "ADT^A07", "ADT^A08", "ADT^A40", "ORM^O01" };
    private static final Issuer SITE_A = new Issuer("XDS_A", "1.2.40.0.13.1.1.999.111.1111", "ISO");
    private static final Code INST_A = new Code("111.1111", "99DCM4CHEE", null, "Site A");
    private static final String[] MIME_TYPES1 = new String[] { "application/dicom" };
    private static final String[] MIME_TYPES2 = new String[] { "application/xml", "application/dicom", "application/pdf", "text/plain",
            "text/xml" };
    private String[] AFFINITY_DOMAIN = { "1.2.3.4.5" };
    private static final String[] AFFINITY_DOMAIN2 = { "5.4.3.2.1" };
    private static final String DEFAULTID = "*";

    protected static int testCount = 0;
    protected static String testDeviceName;
    protected static String arrDeviceName;
    public DicomConfiguration config;

    static Set<String> createdDevices = new HashSet<String>();

    @After
    public void tearDown() throws Exception {
        if (System.getProperty("keep") == null)
        	cleanUp();
        if (config != null)
            config.close();
    }

    protected void cleanUp() throws Exception {
        if (config == null || testCount == 0)
            return;
    
    
        // clean up created devices
        for (String dName : createdDevices) {
            try {
                config.removeDevice(dName);
            } catch (ConfigurationNotFoundException e) {
            }
        }
        createdDevices.clear();
    
    }

    public Device addDeviceWithExtensionAndPersist(DeviceExtension extension, String deviceName) throws Exception {
    
        String dName = "testDevice_" + extension.getClass().getSimpleName();
        int i = 0;
        while (createdDevices.contains(dName + i))
            i++;
    
        createdDevices.add(dName + i);
    
        Device srcd = createDevice(dName + i, SITE_A, INST_A);
    
        srcd.addDeviceExtension(extension);
        config.persist(srcd);
        return srcd;
    
    }


    public <T extends DeviceExtension> T loadConfigAndAssertEquals(String devicename, Class<T> configClass, T configToCompare)
            throws ConfigurationException {
    
        Device loadedDevice = config.findDevice(devicename);
        T loaded = loadedDevice.getDeviceExtension(configClass);
    
        boolean eq = DeepEquals.deepEquals(configToCompare, loaded);
        Assert.assertTrue("Root class: " + configClass.getSimpleName() + "\n" + DeepEquals.getLastPair(), eq);
    
        return loaded;
    }

    private XdsRegistry createRegistry() {
        XdsRegistry registry = new XdsRegistry();
        registry.setApplicationName(XDS_REGISTRY1);
        registry.setAffinityDomain(AFFINITY_DOMAIN);
        registry.setAffinityDomainConfigDir("${jboss.server.config.dir}/affinitydomain");
        registry.setAcceptedMimeTypes(MIME_TYPES1);
        registry.setSoapLogDir(null);
        registry.setCreateMissingPIDs(false);
        registry.setCreateMissingCodes(false);
        registry.setCheckAffinityDomain(true);
        registry.setCheckMimetype(true);
        registry.setPreMetadataCheck(false);
        registry.setRegisterUrl("http://localhost/registryregister");
        registry.setQueryUrl("http://localhost/registryquery");
        return registry;
    }

    private XCARespondingGWCfg createRespondingGW() throws Exception {
        XCARespondingGWCfg respGW = new XCARespondingGWCfg();

        respGW.setApplicationName(XCA_RESPONDING_GW);
        respGW.setHomeCommunityID(HOME_COMMUNITY_ID);
        respGW.setSoapLogDir(LOG_XDSLOG);

        respGW.setQueryUrl("https://myurl");
        respGW.setRetrieveUrl("http:/my-retrieveurl");

        XdsRegistry registry = createRegistry();
        Device regd = addDeviceWithExtensionAndPersist(registry, "registry_device_x");
        respGW.setRegistry(regd);

        XdsRepository repo1 = createRepo();
        repo1.setRepositoryUID("123");
        XdsRepository repo2 = createRepo();
        repo2.setRepositoryUID("456");

        Device repod1 = addDeviceWithExtensionAndPersist(repo1, "repo_device_1");
        Device repod2 = addDeviceWithExtensionAndPersist(repo2, "repo_device_2");

        Map<String, Device> repDevices = new HashMap<String, Device>();
        repDevices.put("123", repod1);
        repDevices.put("456", repod2);

        respGW.setRepositoryDeviceByUidMap(repDevices);
        return respGW;
    }

    private XdsRepository createRepo() throws Exception {
        // create registry which will be referenced
        XdsRegistry registry = createRegistry();
        Device regd = addDeviceWithExtensionAndPersist(registry, "registry_device");

        // ceate source that will be referenced
        XdsSource srcext = new XdsSource();
        srcext.setRegistry(regd);
        srcext.setUid("1233231");
        Device srcd = addDeviceWithExtensionAndPersist(srcext, "source_device");

        XdsRepository rep = new XdsRepository();

        rep.setApplicationName(XDS_REPOSITORY1);
        rep.setRepositoryUID(XDS_REPO1_UID);
        rep.setAcceptedMimeTypes(MIME_TYPES1);
        rep.setSoapLogDir(null);
        rep.setCheckMimetype(true);
        rep.setAllowedCipherHostname("*");
        rep.setLogFullMessageHosts(new String[] {});
        rep.setRetrieveUrl("http://retrieve");
        rep.setProvideUrl("http://provide");
        rep.setForceMTOM(true);

        // reference registry
        Map<String, Device> deviceBySrcUid = new HashMap<String, Device>();

        deviceBySrcUid.put(srcext.getUid(), srcd);
        rep.setSrcDevicebySrcIdMap(deviceBySrcUid);

        Map<String, String> fsGroupIDbyAffinity = new HashMap<String, String>();
        fsGroupIDbyAffinity.put("*", "XDS_ONLINE_TEST_GROUP");
        fsGroupIDbyAffinity.put("1.2.3.4", "XDS_ONLINE_1234_GROUP");
        rep.setFsGroupIDbyAffinity(fsGroupIDbyAffinity);

        XdsRepository repo = rep;
        return repo;
    }

    private XCAiRespondingGWCfg createXCAiRespondingGW() throws Exception {
        XCAiRespondingGWCfg respGW = new XCAiRespondingGWCfg();
        respGW.setApplicationName(XCAI_RESPONDING_GW);
        respGW.setHomeCommunityID(HOME_COMMUNITY_ID);
        respGW.setSoapLogDir(LOG_XDSLOG);
        respGW.setRetrieveUrl("http://retrieveurl");
    
        // src by uid
        XdsSource src1 = new XdsSource();
        src1.setUid("123");
        src1.setUrl("theSrcUrl");
    
        Device srcd1 = addDeviceWithExtensionAndPersist(src1, "src_device_1");
        Map<String, Device> uid2src = new HashMap<String, Device>();
        uid2src.put("1.1.1.1", srcd1);
    
        respGW.setSrcDevicebySrcIdMap(uid2src);
        return respGW;
    }

    @Test
    public void testRegistry() throws Exception {
        Device d = createDevice("registry", SITE_A, INST_A);
        createdDevices.add("registry");    
        XdsRegistry registry = createRegistry();
    
        d.addDeviceExtension(registry);
        config.persist(d);
        afterPersist();
    
        // assert
    
        Device device = config.findDevice("registry");
        XdsRegistry loadedRegistry = device.getDeviceExtension(XdsRegistry.class);
    
        boolean eq = DeepEquals.deepEquals(registry, loadedRegistry);
    
        Assert.assertTrue("Root: XdsRegistry \n" + DeepEquals.getLastPair(), eq);
    
    }

    @Test
    public void testRegistryWithOptional() throws Exception {
        Device d = createDevice("registry", SITE_A, INST_A);
        createdDevices.add("registry");        
        XdsRegistry registry = new XdsRegistry();
        d.addDeviceExtension(registry);
        registry.setApplicationName(XDS_REGISTRY2);
        registry.setAffinityDomain(AFFINITY_DOMAIN);
        registry.setAffinityDomainConfigDir("${jboss.server.config.dir}/affinitydomain");
        registry.setAcceptedMimeTypes(MIME_TYPES2);
        registry.setSoapLogDir("/log/xdslog");
        registry.setCreateMissingPIDs(true);
        registry.setCreateMissingCodes(true);
        registry.setCheckAffinityDomain(true);
        registry.setCheckMimetype(true);
        registry.setPreMetadataCheck(false);
        registry.setRegisterUrl("http://localhost/registry");
        registry.setQueryUrl("http://localhost/registry");
        config.persist(d);
        afterPersist();

        // assert
        loadConfigAndAssertEquals("registry", XdsRegistry.class, registry);
    }

    @Test
    public void testRegistryWithHL7() throws Exception {

        // create/store

        Device device = createDevice("registry_hl7", SITE_A, INST_A);
        createdDevices.add("registry_hl7");

        XdsRegistry registry = new XdsRegistry();
        device.addDeviceExtension(registry);
        registry.setApplicationName(XDS_REGISTRY3);
        registry.setAffinityDomain(AFFINITY_DOMAIN);
        registry.setAffinityDomainConfigDir("${jboss.server.config.dir}/affinitydomain");
        registry.setAcceptedMimeTypes(MIME_TYPES1);
        registry.setSoapLogDir("/log/xdslog");
        registry.setCreateMissingPIDs(true);
        registry.setCreateMissingCodes(true);
        registry.setCheckAffinityDomain(true);
        registry.setCheckMimetype(true);
        registry.setPreMetadataCheck(false);
        registry.setRegisterUrl("http://localhost/registry");
        registry.setQueryUrl("http://localhost/registry");
        ArrayList<HL7Application> hl7Apps = addHL7(device);

        config.persist(device);
        afterPersist();

        // assert

        loadConfigAndAssertEquals("registry_hl7", XdsRegistry.class, registry);

        checkHL7Apps("registry_hl7", hl7Apps);
    }

    @Test
    public void testModify() throws Exception {
        Device d = createDevice("registry_modify", SITE_A, INST_A);
        createdDevices.add("registry_modify");

        XdsRegistry registry = new XdsRegistry();
        d.addDeviceExtension(registry);

        registry.setApplicationName(XDS_REGISTRY4);
        registry.setAffinityDomain(AFFINITY_DOMAIN);
        registry.setAffinityDomainConfigDir("${jboss.server.config.dir}/affinitydomain");
        registry.setAcceptedMimeTypes(MIME_TYPES1);
        registry.setSoapLogDir("/log/xdslog/m");
        registry.setCreateMissingPIDs(true);
        registry.setCreateMissingCodes(true);
        registry.setCheckAffinityDomain(true);
        registry.setCheckMimetype(true);
        registry.setPreMetadataCheck(false);
        registry.setRegisterUrl("http://localhost/registry");
        registry.setQueryUrl("http://localhost/registry");

        HL7DeviceExtension hl7Ext = new HL7DeviceExtension();
        d.addDeviceExtension(hl7Ext);
        ArrayList<HL7Application> hl7Apps = new ArrayList<HL7Application>();
        HL7Application hl7App = createHL7Application(HL7_APP_NAME2, "hl7-conn", "localhost", 2575, 2576, d);
        hl7Apps.add(hl7App);
        hl7Ext.addHL7Application(hl7App);
        config.persist(d);

        // assert loaded

        loadConfigAndAssertEquals("registry_modify", XdsRegistry.class, registry);

        checkHL7Apps("registry_modify", hl7Apps);

        // modify and merge

        XdsRegistry xdsApp = d.getDeviceExtension(XdsRegistry.class);
        xdsApp.setAffinityDomain(AFFINITY_DOMAIN2);
        xdsApp.setSoapLogDir("/log/xdslog/mod");
        xdsApp.setCreateMissingPIDs(false);
        xdsApp.setAcceptedMimeTypes(MIME_TYPES2);
        xdsApp.setCheckAffinityDomain(false);
        xdsApp.setCheckMimetype(false);
        xdsApp.setPreMetadataCheck(true);
        
        for (Connection c : hl7App.getConnections()) {
            c.setPort(c.getPort() + 10000);
            c.setHostname("changed");
        }
        HL7Application hl7App2 = createHL7Application(HL7_APP_NAME3, "hl7-conn", "localhost", 3575, 3576, d);
        hl7Apps.add(hl7App2);
        hl7Ext.addHL7Application(hl7App2);
        config.merge(d);
        afterPersist();

        // assert merged correctly

        loadConfigAndAssertEquals("registry_modify", XdsRegistry.class, registry);

        checkHL7Apps("registry_modify", hl7Apps);

    }

    @Test
    public void testRepository() throws Exception {

        XdsRepository repo = createRepo();

        Device d = createDevice("repository", SITE_A, INST_A);
        createdDevices.add("repository");
        d.addDeviceExtension(repo);

        config.persist(d);
        afterPersist();

        // assert loaded

        XdsRepository loadedRepo = loadConfigAndAssertEquals("repository", XdsRepository.class, repo);

        // check methods

        Assert.assertEquals("getRegistryURL ", "http://localhost/registryregister", loadedRepo.getRegistryURL("1233231"));
        Assert.assertEquals("'getFilesystemGroupID for 1.2.3.4'", "XDS_ONLINE_1234_GROUP", loadedRepo.getFilesystemGroupID("1.2.3.4"));
        Assert.assertEquals("'getFilesystemGroupID for x'", "XDS_ONLINE_TEST_GROUP", loadedRepo.getFilesystemGroupID("x"));
        Assert.assertEquals("'getFilesystemGroupID for null'", "XDS_ONLINE_TEST_GROUP", loadedRepo.getFilesystemGroupID(null));

        // modify/merge

        XdsRegistry registry2 = createRegistry();
        Device regd2 = addDeviceWithExtensionAndPersist(registry2, "registry_device2");

        XdsSource srcext2 = new XdsSource();
        srcext2.setRegistry(regd2);
        srcext2.setUid("9999");
        Device srcd2 = addDeviceWithExtensionAndPersist(srcext2, "source_device2");

        repo = d.getDeviceExtension(XdsRepository.class);
        repo.setAcceptedMimeTypes(MIME_TYPES2);
        repo.setCheckMimetype(false);
        repo.setRepositoryUID(XDS_REPO2_UID);
        repo.getSrcDevicebySrcIdMap().put("9999", srcd2);

        repo.getSrcDevicebySrcIdMap().get("1233231").getDeviceExtension(XdsSource.class).setRegistry(d);
        config.merge(repo.getSrcDevicebySrcIdMap().get("1233231"));

        Device old = config.findDevice("repository");

        config.merge(d);

        // assert merged

        loadConfigAndAssertEquals("repository", XdsRepository.class, repo);

        
        // assert reconfigure
        
        Device dr = config.findDevice("repository");
        old.reconfigure(dr);
        
        loadConfigAndAssertEquals("repository", XdsRepository.class, old.getDeviceExtension(XdsRepository.class));

    }

    @Test
    public void testXCARespondingGW() throws Exception {

        XCARespondingGWCfg respGW = createRespondingGW();

        Device d = createDevice("xca_resp_gw", SITE_A, INST_A);
        d.addDeviceExtension(respGW);

        createdDevices.add("xca_resp_gw");

        config.persist(d);
        afterPersist();

        // assert loaded

        loadConfigAndAssertEquals("xca_resp_gw", XCARespondingGWCfg.class, respGW);

        // modify /merge

        respGW.setHomeCommunityID(HOME_COMMUNITY2_ID);
        respGW.setSoapLogDir(VAR_LOG_XDSLOG);
        respGW.getRepositoryDeviceByUidMap().put("456", respGW.getRepositoryDeviceByUidMap().get("123"));
        respGW.getRepositoryDeviceByUidMap().remove("123");

        config.merge(d);

        // assert merged

        XCARespondingGWCfg loaded = loadConfigAndAssertEquals("xca_resp_gw", XCARespondingGWCfg.class, respGW);

        // check methods

        Assert.assertEquals("getRepositoryURL ", "http://retrieve", loaded.getRepositoryURL("456"));
        Assert.assertEquals("getRegistryURL ", "http://localhost/registryquery", loaded.getRegistryURL());

    }

    @Test
    public void testXCAInitiatingGW() throws Exception {
        Device d = createDevice("xca_init_gw", SITE_A, INST_A);
        createdDevices.add("xca_init_gw");

        XCAInitiatingGWCfg initGW = new XCAInitiatingGWCfg();
        d.addDeviceExtension(initGW);

        initGW.setApplicationName(XCA_INITIATING_GW);
        initGW.setHomeCommunityID(HOME_COMMUNITY_ID);
        initGW.setSoapLogDir(LOG_XDSLOG);
        initGW.setLocalPIXConsumerApplication(PIX_CONSUMER_APP);
        initGW.setRemotePIXManagerApplication(PIX_MANAGER_APP);
        initGW.setAsync(false);
        initGW.setAsyncHandler(true);

        // registry
        XdsRegistry registry = createRegistry();
        registry.setQueryUrl("MyNewQueryURL");
        Device regd = addDeviceWithExtensionAndPersist(registry, "registry_device_rgw");
        initGW.setRegistry(regd);

        // repos
        XdsRepository repo1 = createRepo();
        repo1.setRepositoryUID("123");
        XdsRepository repo2 = createRepo();
        repo2.setRepositoryUID("456");
        repo2.setRetrieveUrl("MyNewRetrUrl");

        Device repod1 = addDeviceWithExtensionAndPersist(repo1, "repo_device_1");
        Device repod2 = addDeviceWithExtensionAndPersist(repo2, "repo_device_2");

        Map<String, Device> repDevices = new HashMap<String, Device>();
        repDevices.put("123", repod1);
        repDevices.put("456", repod2);

        initGW.setRepositoryDeviceByUidMap(repDevices);

        // resp gws
        XCARespondingGWCfg respGW1 = createRespondingGW();
        respGW1.setQueryUrl("theQueryURL");
        respGW1.setRetrieveUrl("theRetrURL");
        XCARespondingGWCfg respGW2 = createRespondingGW();

        // Device rgwd1 = addDeviceWithExtensionAndPersist(respGW1,
        // "repo_device_1");
        repod1.addDeviceExtension(respGW1);
        config.merge(repod1);

        Device rgwd2 = addDeviceWithExtensionAndPersist(respGW2, "rgw_device_2");

        GatewayReference gwr1 = new GatewayReference();
        gwr1.setAffinityDomain("1.2.3.4.5");
        gwr1.setRespondingGWdevice(repod1);

        GatewayReference gwr2 = new GatewayReference();
        gwr2.setAffinityDomain("10.20.30.40.50");
        gwr2.setRespondingGWdevice(rgwd2);

        Map<String, GatewayReference> gws = new HashMap<String, GatewayReference>();
        gws.put("1001", gwr1);
        gws.put("2002", gwr2);

        initGW.setRespondingGWByHomeCommunityIdMap(gws);

        config.persist(d);
        afterPersist();

        // assert loaded

        XCAInitiatingGWCfg loaded = loadConfigAndAssertEquals("xca_init_gw", XCAInitiatingGWCfg.class, initGW);

        // check methods

        String[] ads = { "1.2.3.4.5", "10.20.30.40.50" };
        Assert.assertArrayEquals("getAssigningAuthorities ", ads, loaded.getAssigningAuthorities());
        Assert.assertEquals("getCommunityIDs ", new HashSet<String>(Arrays.asList("1001", "2002")), loaded.getHomeCommunityIDs());
        Assert.assertEquals("getRespondingGWQueryURL ", "theQueryURL", loaded.getRespondingGWQueryURL("1001"));
        Assert.assertEquals("getRespondingGWRetrieveURL ", "theRetrURL", loaded.getRespondingGWRetrieveURL("1001"));
        Assert.assertEquals("getAssigningAuthority ", "1.2.3.4.5", loaded.getAssigningAuthority("1001"));
        Assert.assertEquals("getRegistryURL ", "MyNewQueryURL", loaded.getRegistryURL());
        Assert.assertEquals("getRepositoryURL ", "MyNewRetrUrl", loaded.getRepositoryURL("456"));

        // merge

        initGW.setHomeCommunityID(HOME_COMMUNITY2_ID);
        initGW.setSoapLogDir(VAR_LOG_XDSLOG);
        initGW.setLocalPIXConsumerApplication(PIX_CONSUMER_APP2);
        initGW.setRemotePIXManagerApplication(PIX_MANAGER_APP2);
        initGW.setAsync(true);
        initGW.setAsyncHandler(false);

        initGW.getRespondingGWByHomeCommunityIdMap().remove("1001");
        gwr2.setAffinityDomain("1000.2000.3000"); // hm..

        config.merge(d);

        // check

        loadConfigAndAssertEquals("xca_init_gw", XCAInitiatingGWCfg.class, initGW);

    }

    @Test
    public void testXCAiRespondingGW() throws Exception {

        XCAiRespondingGWCfg respGW = createXCAiRespondingGW();

        Device d = createDevice("xcai_resp_gw", SITE_A, INST_A);
        d.addDeviceExtension(respGW);

        createdDevices.add("xcai_resp_gw");

        config.persist(d);
        afterPersist();

        // check
        loadConfigAndAssertEquals("xcai_resp_gw", XCAiRespondingGWCfg.class, respGW);

        // modify merge

        respGW.setHomeCommunityID(HOME_COMMUNITY2_ID);
        respGW.setSoapLogDir(VAR_LOG_XDSLOG);

        config.merge(d);

        // check
        XCAiRespondingGWCfg loaded = loadConfigAndAssertEquals("xcai_resp_gw", XCAiRespondingGWCfg.class, respGW);

        // check methods
        Assert.assertEquals("getXDSiSourceURL ", "theSrcUrl", loaded.getXDSiSourceURL("1.1.1.1"));

    }

    @Test
    public void testXCAiInitiatingGW() throws Exception {
        Device d = createDevice("xcai_init_gw", SITE_A, INST_A);
        createdDevices.add("xcai_init_gw");

        XCAiInitiatingGWCfg initGW = new XCAiInitiatingGWCfg();
        d.addDeviceExtension(initGW);

        initGW.setApplicationName(XCAI_INITIATING_GW);
        initGW.setHomeCommunityID(HOME_COMMUNITY_ID);
        initGW.setSoapLogDir(LOG_XDSLOG);
        initGW.setAsync(false);
        initGW.setAsyncHandler(true);

        // resp gws
        XCAiRespondingGWCfg respGW1 = createXCAiRespondingGW();
        respGW1.setRetrieveUrl("newUrl");
        XCAiRespondingGWCfg respGW2 = createXCAiRespondingGW();

        Device rgwd1 = addDeviceWithExtensionAndPersist(respGW1, "irgw_device_1");
        Device rgwd2 = addDeviceWithExtensionAndPersist(respGW2, "irgw_device_2");

        Map<String, Device> repDevices = new HashMap<String, Device>();
        repDevices.put("123", rgwd1);
        repDevices.put("456", rgwd2);

        initGW.setRespondingGWDevicebyHomeCommunityId(repDevices);

        // src by uid
        XdsSource src1 = new XdsSource();
        src1.setUid("1.1.1.1");

        Device srcd1 = addDeviceWithExtensionAndPersist(src1, "src_device_1");
        Map<String, Device> uid2src = new HashMap<String, Device>();
        uid2src.put("1.1.1.1", srcd1);

        initGW.setSrcDevicebySrcIdMap(uid2src);

        config.persist(d);
        afterPersist();

        // check
        loadConfigAndAssertEquals("xcai_init_gw", XCAiInitiatingGWCfg.class, initGW);

        // modify merge
        initGW.setHomeCommunityID(HOME_COMMUNITY2_ID);
        initGW.setSoapLogDir(VAR_LOG_XDSLOG);
        initGW.setAsync(true);
        initGW.setAsyncHandler(false);

        XdsSource src2 = new XdsSource();
        src2.setUid("1.2.1.2");
        src2.setUrl("srcUrl");

        Device srcd2 = addDeviceWithExtensionAndPersist(src2, "src_device_2");
        uid2src.put("1.2.1.2", srcd2);

        config.merge(d);

        // check
        XCAiInitiatingGWCfg loaded = loadConfigAndAssertEquals("xcai_init_gw", XCAiInitiatingGWCfg.class, initGW);

        // check methods

        Assert.assertEquals("getCommunityIDs ", new HashSet<String>(Arrays.asList("123", "456")), loaded.getCommunityIDs());

        Assert.assertEquals("getRespondingGWURL ", "newUrl", loaded.getRespondingGWURL("123"));

        Assert.assertEquals("getXDSiSourceURL ", "srcUrl", loaded.getXDSiSourceURL("1.2.1.2"));

    }

    @Test
    public void testDefaultConfig() throws Exception {

        // if default config device is already in the config - leave it
        // untouched, cancel test
        try {
            if (config.findDevice(DEFAULT_XDS_DEVICE) != null)
                return;
        } catch (Exception e) {
        }

        Device d = createDevice(DEFAULT_XDS_DEVICE, SITE_A, INST_A);

        // registry
        XdsRegistry registry = new XdsRegistry();
        d.addDeviceExtension(registry);
        registry.setApplicationName(XDS_REGISTRY1);
        registry.setAffinityDomain(AFFINITY_DOMAIN);
        registry.setAffinityDomainConfigDir("${jboss.server.config.dir}/affinitydomain");
        registry.setAcceptedMimeTypes(MIME_TYPES2);
        registry.setSoapLogDir(DEFAULT_XDSLOG);
        registry.setCreateMissingPIDs(true);
        registry.setCreateMissingCodes(true);
        registry.setCheckAffinityDomain(true);
        registry.setCheckMimetype(true);
        registry.setPreMetadataCheck(false);
        registry.setQueryUrl("http://localhost:8080/xds/registry");
        registry.setRegisterUrl("http://localhost:8080/xds/registry");
        registry.setCheckAffinityDomain(false);
        registry.setCheckMimetype(false);
        
        // generic source

        XdsSource source = new XdsSource();
        d.addDeviceExtension(source);        
        source.setUid("0");
        source.setRegistry(d);
        source.setRepository(d);

        // repository
        XdsRepository rep = new XdsRepository();
        d.addDeviceExtension(rep);
        rep.setApplicationName(XDS_REPOSITORY1);
        rep.setRepositoryUID(XDS_REPO1_UID);
        rep.setRetrieveUrl("http://localhost:8080/xds/repository");
        rep.setProvideUrl("http://localhost:8080/xds/repository");
        rep.setAcceptedMimeTypes(MIME_TYPES2);
        rep.setSoapLogDir(DEFAULT_XDSLOG);
        rep.setCheckMimetype(false);
        rep.setAllowedCipherHostname("*");
        rep.setLogFullMessageHosts(new String[] {});


        // used elsewhere as well
        Map<String, Device> deviceBySrcUid = new HashMap<String, Device>();
        deviceBySrcUid.put(DEFAULTID, d);
        rep.setSrcDevicebySrcIdMap(deviceBySrcUid);
        Map<String, String> fsGroupIDbyAffinity = new HashMap<String, String>();
        fsGroupIDbyAffinity.put("*", "XDS_ONLINE");
        rep.setFsGroupIDbyAffinity(fsGroupIDbyAffinity);


        // XCAResponding GW

        XCARespondingGWCfg respGW = new XCARespondingGWCfg();
        d.addDeviceExtension(respGW);
        respGW.setApplicationName(XCA_RESPONDING_GW);
        respGW.setHomeCommunityID(HOME_COMMUNITY_ID);
        respGW.setSoapLogDir(LOG_XDSLOG);
        respGW.setRetrieveUrl("http://localhost:8080/xca/RespondingGW");
        respGW.setQueryUrl("http://localhost:8080/xca/RespondingGW");
        respGW.setRegistry(d);

        // also used below
        Map<String, Device> repoByUid = new HashMap<String, Device>();
        repoByUid.put(DEFAULTID, d);
        respGW.setRepositoryDeviceByUidMap(repoByUid);

        // XCA Initiating GW

        XCAInitiatingGWCfg initGW = new XCAInitiatingGWCfg();
        d.addDeviceExtension(initGW);
        initGW.setApplicationName(XCA_INITIATING_GW);
        initGW.setHomeCommunityID(HOME_COMMUNITY_ID);
        initGW.setSoapLogDir(LOG_XDSLOG);
        initGW.setLocalPIXConsumerApplication(PIX_CONSUMER_APP);
        initGW.setRemotePIXManagerApplication(PIX_MANAGER_APP);
        initGW.setAsync(false);
        initGW.setAsyncHandler(true);

        initGW.setRegistry(d);
        initGW.setRepositoryDeviceByUidMap(repoByUid);

        // gw ref
        GatewayReference gwr1 = new GatewayReference();
        gwr1.setAffinityDomain("&1.2.3.4.9.99.2&ISO");
        gwr1.setRespondingGWdevice(d);

        Map<String, GatewayReference> gws = new HashMap<String, GatewayReference>();
        gws.put(DEFAULTID, gwr1);

        initGW.setRespondingGWByHomeCommunityIdMap(gws);

        // XCAiResponding GW

        XCAiRespondingGWCfg irespGW = new XCAiRespondingGWCfg();
        d.addDeviceExtension(irespGW);
        irespGW.setApplicationName(XCAI_RESPONDING_GW);
        irespGW.setHomeCommunityID(HOME_COMMUNITY_ID);
        irespGW.setSoapLogDir(LOG_XDSLOG);
        irespGW.setRetrieveUrl("http://localhost:8080/xcai/RespondingGW");

        irespGW.setSrcDevicebySrcIdMap(deviceBySrcUid);

        // XCAiInitiating GW

        XCAiInitiatingGWCfg iinitGW = new XCAiInitiatingGWCfg();
        d.addDeviceExtension(iinitGW);
        iinitGW.setApplicationName(XCAI_INITIATING_GW);
        iinitGW.setHomeCommunityID(HOME_COMMUNITY_ID);
        iinitGW.setSoapLogDir(LOG_XDSLOG);
        iinitGW.setAsync(false);
        iinitGW.setAsyncHandler(true);
        iinitGW.setSrcDevicebySrcIdMap(deviceBySrcUid);

        Map<String, Device> rgwbycid = new HashMap<String, Device>();
        rgwbycid.put(DEFAULTID, d);
        iinitGW.setRespondingGWDevicebyHomeCommunityId(rgwbycid);

        ArrayList<HL7Application> hl7Apps = this.addHL7(d);
        this.addAuditLogger(d);

        config.persist(d);
        afterPersist();
        checkHL7Apps(DEFAULT_XDS_DEVICE, hl7Apps);


    }
/*_*/
    public void afterPersist() throws Exception {
    }

    private Device init(Device device, Issuer issuer, Code institutionCode) throws Exception {
        device.setIssuerOfPatientID(issuer);
        device.setIssuerOfAccessionNumber(issuer);
        if (institutionCode != null) {
            device.setInstitutionNames(institutionCode.getCodeMeaning());
            device.setInstitutionCodes(institutionCode);
        }
        return device;
    }

    public Device createDevice(String name, Issuer issuer, Code institutionCode) throws Exception {
        testDeviceName = name;
        Device device = new Device(name);
        init(device, issuer, institutionCode);
        return device;
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
        arrDeviceName = "dcm4chee-AuditRecordRepository";
        Device arrDevice = new Device(arrDeviceName);
        createdDevices.add(arrDeviceName);
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

    private HL7Application createHL7Application(String appName, String connName, String host, int port, int tlsPort, Device xdsDevice)
            throws Exception {
        HL7Application hl7App = new HL7Application(appName);
        Connection conn = getOrCreateConnection(connName, host, port, xdsDevice);
        hl7App.addConnection(conn);
        hl7App.setAcceptedMessageTypes(HL7_MESSAGE_TYPES);
        return hl7App;
    }

    private Connection getOrCreateConnection(String connName, String host, int port, Device xdsDevice) {
        for (Connection c : xdsDevice.listConnections()) {
            if (c.getCommonName().equals(connName))
                return c;
        }
        Connection conn = new Connection(connName, host, port);
        conn.setProtocol(Connection.Protocol.HL7);
        xdsDevice.addConnection(conn);
        return conn;
    }

    /* _ */
    private void checkHL7Apps(String deviceName, Collection<HL7Application> hl7Apps) throws Exception {
        Device device = config.findDevice(deviceName);
        String prefix = deviceName;

        HL7DeviceExtension foundHL7ext = device.getDeviceExtension(HL7DeviceExtension.class);
        assertEquals(prefix + "-NumberOfHL7Apps", hl7Apps == null ? 0 : hl7Apps.size(), foundHL7ext == null ? 0 : foundHL7ext
                .getHL7Applications().size());
        if (hl7Apps != null) {
            HL7Application hl7app1;
            for (HL7Application hl7app : hl7Apps) {
                hl7app1 = foundHL7ext.getHL7Application(hl7app.getApplicationName());
                assertNotNull(prefix + "-HL7Applicationname " + hl7app.getApplicationName() + " not found!", hl7app1);
                assertArrayEquals(prefix + "-AcceptedMessageTypes", hl7app.getAcceptedMessageTypes(), hl7app1.getAcceptedMessageTypes());
                assertArrayEquals(prefix + "-AcceptedSendingApplications", hl7app.getAcceptedSendingApplications(),
                        hl7app1.getAcceptedSendingApplications());
                assertEquals(prefix + "-HL7DefaultCharacterSet", hl7app.getHL7DefaultCharacterSet(), hl7app1.getHL7DefaultCharacterSet());
                List<Connection> conns = hl7app.getConnections();
                assertNotNull(prefix + "-Missing connections (original)", conns);
                List<Connection> conns1 = hl7app1.getConnections();
                assertNotNull(prefix + "-Missing connections (stored)", conns1);
                assertEquals(prefix + "-Number of connections", conns.size(), conns1.size());
                loop: for (Connection con : conns) {
                    for (Connection con1 : conns1) {
                        if (con.getPort() == con1.getPort() && isEqual(con.getHostname(), con1.getHostname())
                                && isEqual(con.getHttpProxy(), con1.getHttpProxy()))
                            continue loop;
                    }
                    fail(prefix + "-No Identical Connection found:" + con + "\nstored connections:" + conns1);
                }
            }
        }
        /* _ */
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

}
