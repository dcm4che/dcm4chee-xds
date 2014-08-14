package org.dcm4chee.xds2.conf;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.ConfigurationNotFoundException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.data.Code;
import org.dcm4che3.data.Issuer;
import org.dcm4che3.net.Device;
import org.dcm4chee.storage.conf.Availability;
import org.dcm4chee.storage.conf.Filesystem;
import org.dcm4chee.storage.conf.FilesystemGroup;
import org.dcm4chee.storage.conf.StorageConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultConfigurator {

    private static final Logger log = LoggerFactory.getLogger(DefaultConfigurator.class);

    private static final String[] affinityDomain = { "1.2.3.4.5" };
    private static final String[] MIME_TYPES2 = new String[] { "application/xml", "application/dicom", "application/pdf", 
        "application/msword", "application/msexcel", "text/plain", "text/xml", "image/jpeg", "image/png", "image/tiff" };
    
    public static void applyDefaultConfig(DicomConfiguration config, String deviceName) {

        log.info("Initializing the default configuration for device {}", deviceName);

        try {

            // should work with agility
            String ip = System.getProperty("jboss.bind.address", "localhost");
            

            Device device = new Device(deviceName);
            Issuer issuer = new Issuer("XDS_A", "1.2.40.0.13.1.1.999.111.1111", "ISO");
            Code institutionCode = new Code("111.1111", "99DCM4CHEE", null, "Site A");
            device.setIssuerOfPatientID(issuer);
            device.setIssuerOfAccessionNumber(issuer);
            if (institutionCode != null) {
                device.setInstitutionNames(institutionCode.getCodeMeaning());
                device.setInstitutionCodes(institutionCode);
            }

            // registry
            XdsRegistry registry = new XdsRegistry();
            device.addDeviceExtension(registry);
            registry.setApplicationName("XDSREGISTRY");
            registry.setAffinityDomain(affinityDomain);
            registry.setAffinityDomainConfigDir("${jboss.server.config.dir}/xds/affinitydomain");
            registry.setAcceptedMimeTypes(MIME_TYPES2);
            registry.setSoapLogDir("../standalone/log/xds");
            registry.setCreateMissingPIDs(true);
            registry.setCreateMissingCodes(true);
            registry.setCheckAffinityDomain(true);
            registry.setCheckMimetype(true);
            registry.setPreMetadataCheck(false);
            registry.setQueryUrl("http://"+ip+":8080/xds/registry");
            registry.setRegisterUrl("http://"+ip+":8080/xds/registry");
            registry.setCheckAffinityDomain(false);
            registry.setCheckMimetype(false);

            // generic source

            XdsSource source = new XdsSource();
            device.addDeviceExtension(source);
            source.setUid("0");
            source.setRegistry(device);
            source.setRepository(device);

            // repository
            XdsRepository rep = new XdsRepository();
            device.addDeviceExtension(rep);
            rep.setApplicationName("XDS-REPOSITORY");
            rep.setRepositoryUID("9.9.9.9");
            rep.setRetrieveUrl("http://"+ip+":8080/xds/repository");
            rep.setProvideUrl("http://"+ip+":8080/xds/repository");
            rep.setAcceptedMimeTypes(MIME_TYPES2);
            rep.setSoapLogDir("../standalone/log/xds");
            rep.setCheckMimetype(false);
            rep.setAllowedCipherHostname("*");
            rep.setLogFullMessageHosts(new String[] {});
            HashMap<String,String> fsGrps = new HashMap<String,String>(1);
            fsGrps.put("*", "XDS_ONLINE");
            rep.setFsGroupIDbyAffinity(fsGrps);

            // storage
            StorageConfiguration store = new StorageConfiguration();
            device.addDeviceExtension(store);
            store.setApplicationName("XDS-REPOSITORY-STORAGE");
            FilesystemGroup grp = new FilesystemGroup("XDS_ONLINE", "1GiB");
            
            String serverHomeDir = System.getProperty("jboss.server.base.dir");
            File fsDir = new File(serverHomeDir, "xds-repository-storage");
            Filesystem fs1 = new Filesystem("xds_fs_1", fsDir.toURI().toString(), 10, Availability.ONLINE);
            grp.addFilesystem(fs1);
            
            // used elsewhere as well
            Map<String, Device> deviceBySrcUid = new HashMap<String, Device>();
            deviceBySrcUid.put("*", device);
            rep.setSrcDevicebySrcIdMap(deviceBySrcUid);

            try {
                if (config.findDevice(deviceName) != null)
                    config.merge(device);
            } catch (ConfigurationNotFoundException nfe) {
                config.persist(device);
            } catch (ConfigurationException ce) {
                throw ce;
            }

            config.close();

        } catch (ConfigurationException e) {
            log.error("Could not auto-initialize default XDS configuration", e);
        }

    }
}


