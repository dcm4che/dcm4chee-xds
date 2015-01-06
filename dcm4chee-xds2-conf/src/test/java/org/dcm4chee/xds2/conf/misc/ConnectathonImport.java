package org.dcm4chee.xds2.conf.misc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dcm4che3.conf.api.ConfigurationNotFoundException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.net.Device;
import org.dcm4chee.xds2.conf.XCARespondingGWCfg;
import org.dcm4chee.xds2.conf.XCAiRespondingGWCfg;
import org.dcm4chee.xds2.conf.XdsConfigTest;
import org.dcm4chee.xds2.conf.XdsRegistry;
import org.dcm4chee.xds2.conf.XdsRepository;
import org.dcm4chee.xds2.conf.XdsSource;

public class ConnectathonImport {

    static Map<String, Map<String, String>> oids = new HashMap<String, Map<String, String>>();

    public static void readOIDs() {

        // Input file which needs to be parsed
        String fileToParse = "src/test/resources/ListOID.csv";
        BufferedReader fileReader = null;

        // Delimiter used in CSV file
        final String DELIMITER = ",";
        try {
            String line = "";

            // Create the file reader
            fileReader = new BufferedReader(new FileReader(fileToParse));

            // read header
            List<String> propNames = Arrays.asList(fileReader.readLine().split(DELIMITER));

            // Read the file line by line
            while ((line = fileReader.readLine()) != null) {
                Map<String, String> props = new HashMap<String, String>();

                // Get all tokens available in line
                String[] tokens = line.split(DELIMITER);

                for (int i = 0; i < propNames.size(); i++) {
                    props.put(propNames.get(i), tokens[i]);
                }

                Map<String, String> sysoids = oids.get(props.get("system"));
                if (sysoids == null)
                    sysoids = new HashMap<String, String>();

                sysoids.put(props.get("oid requirement"), props.get("OID"));

                oids.put(props.get("system"), sysoids);
            }

            // System.out.print(oids);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fileReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {

        XdsConfigTest ldaptst = new XdsConfigTest();

        ldaptst.setUp();
        // Device d = ldaptst.config.findDevice("dcm4chee-xds");

        DicomConfiguration config = ldaptst.config;

        // System.out.println(d.listDeviceExtensions());

        readOIDs();

        // Input file which needs to be parsed
        String fileToParse = "src/test/resources/WebServiceConfiguration.csv";
        BufferedReader fileReader = null;

        // Delimiter used in CSV file
        final String DELIMITER = ",";
        String line = "";
        // Create the file reader
        fileReader = new BufferedReader(new FileReader(fileToParse));

        try {
            // read header
            List<String> propNames = Arrays.asList(fileReader.readLine().split(DELIMITER));

            // Read the file line by line
            int linenum = 0;
            while ((line = fileReader.readLine()) != null) {
                linenum++;
                try {
                    // Get all tokens available in line
                    String[] tokens = line.split(DELIMITER);
                    Map<String, String> props = new HashMap<String, String>();
                    for (int i = 0; i < tokens.length; i++) {
                        props.put(propNames.get(i), tokens[i]);
                    }

                    // ok, now the funny part

                    boolean isSecure = props.get("is secured").toLowerCase().equals("true");
                    
                    // get device name - secure/nonsecure
                    String deviceName = "partner - "+props.get("Company") + " - " + props.get("System") + (isSecure ? "(secure)" : "(unsecure)");

                    boolean merge = false;
                    Device device;

                    try {
                        device = config.findDevice(deviceName);
                        merge = true;
                    } catch (ConfigurationNotFoundException e) {
                        device = new Device(deviceName);
                    }

                    // which url to use
                    String url;

                    if (isSecure) {
                        url = props.get("url");
                    } else {
                        // proxy url
                        URL origurl = new URL(props.get("url"));
                        try {
                            url = new URL("http", "192.168.0.15", Integer.parseInt(props.get("port proxy")), origurl.getFile()).toString();
                        } catch (NumberFormatException e) {
                            url = props.get("url");
                        }
                    }

                    switch (props.get("Actor")) {
                    case "DOC_REGISTRY":

                        XdsRegistry reg = device.getDeviceExtension(XdsRegistry.class);
                        if (reg == null) {
                            reg = new XdsRegistry();
                            device.addDeviceExtension(reg);

                            // satisfy schema
                            String[] ad = { "none" };
                            reg.setAffinityDomain(ad);
                            reg.setAffinityDomainConfigDir("dir");

                            reg.setRegisterUrl("none");
                            reg.setQueryUrl("none");

                        }

                        // app name
                        reg.setApplicationName(props.get("System"));

                        // url
                        if (props.get("ws-type").startsWith("ITI-18"))
                            reg.setQueryUrl(url);
                        else if (props.get("ws-type").startsWith("ITI-42"))
                            reg.setRegisterUrl(url);

                        break;
                    case ("EMBED_REPOS"):
                    case ("DOC_REPOSITORY"):

                        XdsRepository repo = device.getDeviceExtension(XdsRepository.class);
                        if (repo == null) {
                            repo = new XdsRepository();
                            device.addDeviceExtension(repo);
                            
                            repo.setProvideUrl("none");
                            repo.setRetrieveUrl("none");
                        }

                        // app name
                        repo.setApplicationName(props.get("System"));

                        repo.setRepositoryUID(oids.get(props.get("System")).get("repositoryUniqueID OID"));
                        
                        // url
                        if (props.get("ws-type").startsWith("ITI-41"))
                            repo.setProvideUrl(url);
                        else if (props.get("ws-type").startsWith("ITI-43"))
                            repo.setRetrieveUrl(url);

                        break;
                    case "IMG_DOC_SOURCE":

                        XdsSource src = device.getDeviceExtension(XdsSource.class);
                        if (src == null) {
                            src = new XdsSource();
                            device.addDeviceExtension(src);

                        }

                        src.setUid(oids.get(props.get("System")).get("sourceID OID"));
                        
                        // url
                        if (props.get("ws-type").startsWith("RAD-69"))
                            src.setUrl(url);

                        break;                        
                    case "RESP_GATEWAY":

                        XCARespondingGWCfg rgw = device.getDeviceExtension(XCARespondingGWCfg.class);
                        if (rgw == null) {
                            rgw = new XCARespondingGWCfg();
                            device.addDeviceExtension(rgw);
                            
                            rgw.setRegistry(device);
                            rgw.setQueryUrl("none"); 
                            rgw.setRetrieveUrl("none");                            
                        }

                        // app name
                        rgw.setApplicationName(props.get("System"));
                        
                        
                        rgw.setHomeCommunityID(oids.get(props.get("System")).get("homeCommunityID OID"));
                        
                        // url
                        if (props.get("ws-type").startsWith("ITI-38"))
                            rgw.setQueryUrl(url); else
                        if (props.get("ws-type").startsWith("ITI-39"))
                            rgw.setRetrieveUrl(url);

                        break;               
                    case "RESP_IMG_GATEWAY":

                        XCAiRespondingGWCfg rgwi = device.getDeviceExtension(XCAiRespondingGWCfg.class);
                        if (rgwi == null) {
                            rgwi = new XCAiRespondingGWCfg();
                            device.addDeviceExtension(rgwi);
                            
                            rgwi.setRetrieveUrl("none");                            
                        }

                        // app name
                        rgwi.setApplicationName(props.get("System"));
                        
                        rgwi.setHomeCommunityID(oids.get(props.get("System")).get("homeCommunityID OID"));
                        
                        // url
                        if (props.get("ws-type").startsWith("RAD-75"))
                            rgwi.setRetrieveUrl(url);

                        break;                           
                    default:
                        continue;
                    }

                    if (merge)
                        config.merge(device);
                    else
                        config.persist(device);

                }

                catch (Exception e) {
                    throw new Exception("Error on line " + linenum, e);
                }

            }
        } finally {
            fileReader.close();
        }
    }
}
