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

package org.dcm4chee.xds2.conf;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.ConfigurationNotFoundException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.api.hl7.HL7ApplicationCache;
import org.dcm4che3.conf.api.hl7.HL7Configuration;
import org.dcm4che3.conf.ldap.LdapDicomConfiguration;
import org.dcm4che3.conf.ldap.audit.LdapAuditLoggerConfiguration;
import org.dcm4che3.conf.ldap.audit.LdapAuditRecordRepositoryConfiguration;
import org.dcm4che3.conf.ldap.hl7.LdapHL7Configuration;
import org.dcm4che3.conf.prefs.PreferencesDicomConfiguration;
import org.dcm4che3.conf.prefs.audit.PreferencesAuditLoggerConfiguration;
import org.dcm4che3.conf.prefs.audit.PreferencesAuditRecordRepositoryConfiguration;
import org.dcm4che3.conf.prefs.hl7.PreferencesHL7Configuration;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.audit.AuditLogger;
import org.dcm4che3.net.hl7.HL7Application;
import org.dcm4che3.net.hl7.HL7DeviceExtension;
import org.dcm4che3.net.hl7.HL7MessageListener;
import org.dcm4che3.util.SafeClose;
import org.dcm4che3.util.StringUtils;
import org.dcm4chee.xds2.common.audit.XDSAudit;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.conf.ldap.LdapXCAInitiatingGWConfiguration;
import org.dcm4chee.xds2.conf.ldap.LdapXCARespondingGWConfiguration;
import org.dcm4chee.xds2.conf.ldap.LdapXCAiInitiatingGWConfiguration;
import org.dcm4chee.xds2.conf.ldap.LdapXCAiRespondingGWConfiguration;
import org.dcm4chee.xds2.conf.ldap.LdapXDSRegistryConfiguration;
import org.dcm4chee.xds2.conf.ldap.LdapXDSRepositoryConfiguration;
import org.dcm4chee.xds2.conf.prefs.PreferencesXCAInitiatingGWConfiguration;
import org.dcm4chee.xds2.conf.prefs.PreferencesXCARespondingGWConfiguration;
import org.dcm4chee.xds2.conf.prefs.PreferencesXCAiInitiatingGWConfiguration;
import org.dcm4chee.xds2.conf.prefs.PreferencesXCAiRespondingGWConfiguration;
import org.dcm4chee.xds2.conf.prefs.PreferencesXDSRegistryConfiguration;
import org.dcm4chee.xds2.conf.prefs.PreferencesXDSRepositoryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 *
 */
public class XdsDevice {

    public static final String PROP_XDS_DEVICE_NAME = "org.dcm4chee.xds.deviceName";
    public static final String PROP_LDAP_PROPERTIES_URL = "org.dcm4chee.xds.ldapPropertiesURL";
    public static final String PROP_XDS_HL7_APP_NAME = "org.dcm4chee.xds.hl7AppName";

	private static DicomConfiguration xdsConfig;
	private static String xdsDeviceName;
	private static Device localXdsDevice;
	private static HL7Application hl7App;
    private static HL7ApplicationCache hl7AppCache;

    public static final Logger log = LoggerFactory.getLogger(XdsDevice.class);

    static {
    	init();
    }
    
    public static void init() {
        log.info("############XdsDevice INIT called!");
        String ldapPropertiesURL = System.getProperty(PROP_LDAP_PROPERTIES_URL);
        String deviceName = System.getProperty(PROP_XDS_DEVICE_NAME,"xds-device");
        String hl7AppName = System.getProperty(PROP_XDS_HL7_APP_NAME);
        try {
            initXdsDevice(ldapPropertiesURL, deviceName, hl7AppName);
        } catch (Exception e) {
            destroyCfg();
            log.error("XDS device initialization failed!", e);
        }
    }

    private static void initXdsDevice(String ldapPropertiesURL, String deviceName, String hl7AppName) throws IOException, MalformedURLException,
            ConfigurationException, GeneralSecurityException {
        HL7Configuration hl7config = null;
        if (ldapPropertiesURL != null) {
            InputStream ldapConf = null;
		    try {
		    	ldapPropertiesURL = StringUtils.replaceSystemProperties(ldapPropertiesURL);
		        ldapConf = new URL(ldapPropertiesURL).openStream();
		        Properties p = new Properties();
		        p.load(ldapConf);
		        LdapDicomConfiguration ldapConfig = new LdapDicomConfiguration(p);
		        ldapConfig.addDicomConfigurationExtension(new LdapXDSRegistryConfiguration());
		        ldapConfig.addDicomConfigurationExtension(new LdapXDSRepositoryConfiguration());
		        ldapConfig.addDicomConfigurationExtension(new LdapXCARespondingGWConfiguration());
		        ldapConfig.addDicomConfigurationExtension(new LdapXCAInitiatingGWConfiguration());
		        ldapConfig.addDicomConfigurationExtension(new LdapXCAiRespondingGWConfiguration());
		        ldapConfig.addDicomConfigurationExtension(new LdapXCAiInitiatingGWConfiguration());
		        LdapHL7Configuration hl7cfg = new LdapHL7Configuration();
		        ldapConfig.addDicomConfigurationExtension(hl7cfg);
		        ldapConfig.addDicomConfigurationExtension(new LdapAuditLoggerConfiguration());
		        ldapConfig.addDicomConfigurationExtension(new LdapAuditRecordRepositoryConfiguration());
		        xdsConfig = ldapConfig;
		        hl7config = hl7cfg;
		        log.info("Use LDAP as Configuration Backend.");
		    } catch(FileNotFoundException e) {
		        log.info("Could not find ldap.properties at " + ldapPropertiesURL);
	        } finally {
	            SafeClose.close(ldapConf);
	        }
        } else {
        	log.info("System property '"+PROP_LDAP_PROPERTIES_URL+"' not set.");
        }
        if (xdsConfig == null) {
            log.info("Use Java Preferences as Configuration Backend.");
            PreferencesDicomConfiguration prefConfig = new PreferencesDicomConfiguration();
            prefConfig.addDicomConfigurationExtension(new PreferencesXDSRegistryConfiguration());
            prefConfig.addDicomConfigurationExtension(new PreferencesXDSRepositoryConfiguration());
            prefConfig.addDicomConfigurationExtension(new PreferencesXCARespondingGWConfiguration());
            prefConfig.addDicomConfigurationExtension(new PreferencesXCAInitiatingGWConfiguration());
            prefConfig.addDicomConfigurationExtension(new PreferencesXCAiRespondingGWConfiguration());
            prefConfig.addDicomConfigurationExtension(new PreferencesXCAiInitiatingGWConfiguration());
            PreferencesHL7Configuration hl7cfg = new PreferencesHL7Configuration();
            prefConfig.addDicomConfigurationExtension(hl7cfg);
            prefConfig.addDicomConfigurationExtension(new PreferencesAuditLoggerConfiguration());
            prefConfig.addDicomConfigurationExtension(new PreferencesAuditRecordRepositoryConfiguration());
            xdsConfig = prefConfig;
            hl7config = hl7cfg;
        }
        
        log.info("###### xdsConfig:"+xdsConfig);
        xdsDeviceName = deviceName;
        try {
	        Device device = xdsConfig.findDevice(xdsDeviceName);
	        if (device == null) {
	            String msg = "XDS Device '"+xdsDeviceName+"' not found!";
	            log.error(msg);
	            throw new ConfigurationException(msg);
	        }
	        XdsDevice.setLocalXdsDevice(device);
	        hl7AppCache = new HL7ApplicationCache(hl7config);
	        if (device.getDeviceExtension(XdsRegistry.class) != null) {
	            HL7DeviceExtension hl7 = device.getDeviceExtension(HL7DeviceExtension.class);
	            if (hl7 != null) {
	                if (hl7AppName != null) {
	                    hl7App = hl7.getHL7Application(hl7AppName);
	                } else if (hl7.getHL7Applications().size() > 0) {
	                    hl7App = hl7.getHL7Applications().iterator().next();
	                }
	                if (hl7App != null) {
	                    log.info("###### HL7 Application device:"+device);
	                    log.info("###### HL7 Application Name:"+hl7App.getApplicationName());
	                    log.info("###### HL7 Accepted Message Types:"+Arrays.toString(hl7App.getAcceptedMessageTypes()));
	                    log.info("HL7 application will be started if a HL7 message listener is set!");
	                } else if (hl7AppName != null) {
	                    log.error("HL7 Application '"+hl7AppName+"' not found!");
	                } else {
	                    log.warn("No HL7 Application defined in XDS device! "+device);
	                }
	            }
	        } else {
	            log.info("No HL7 Application defined for this device! device:"+device);
	        }
	        XDSAudit.setAuditLogger(device.getDeviceExtension(AuditLogger.class));
	        if (AuditLogger.getDefaultLogger() == null) {
	            AuditLogger.setDefaultLogger(XDSAudit.getAuditLogger());
	        }
        } catch (ConfigurationNotFoundException x) {
        	log.warn("XDS device '"+xdsDeviceName+"' not found!");
        } catch (Exception x) {
        	log.warn("XDS Device configuration failed!", x);
        }
    }
    
    public static void destroyCfg() {
        if (xdsConfig != null)
            xdsConfig.close();
        if (hl7App != null)
            hl7App.getDevice().unbindConnections();
    }
    
    public static boolean setHL7MessageListener(HL7MessageListener listener) {
    	if (hl7App != null) {
            try {
	            log.info("Start HL7 application by adding a HL7 message listener!");
	    		hl7App.setHL7MessageListener(listener);
	            ExecutorService executorService = Executors.newCachedThreadPool();
	            localXdsDevice.setExecutor(executorService);
				localXdsDevice.bindConnections();
	    		return true;
			} catch (Exception x) {
				log.error("Failed to start HL7 application!", x);
			}
    	}
    	return false;
    }
    
    public static Device getLocalXdsDevice() {
        return localXdsDevice;
    }

    public static void setLocalXdsDevice(Device d) {
        if (XdsDevice.localXdsDevice != null) {
            log.warn("Local XDS Device already set! current:"+XdsDevice.localXdsDevice);
            XdsDevice.localXdsDevice.unbindConnections();
            log.info("Unbind connections of current device done! switch to new device:"+d);
        }
        XdsDevice.localXdsDevice = d;
    }
    
    public static XdsRegistry getXdsRegistry() {
        return localXdsDevice.getDeviceExtension(XdsRegistry.class);
    }

    public static XdsRepository getXdsRepository() {
        return localXdsDevice.getDeviceExtension(XdsRepository.class);
    }

    public static XCARespondingGWCfg getXCARespondingGW() {
        return localXdsDevice.getDeviceExtension(XCARespondingGWCfg.class);
    }

    public static XCAInitiatingGWCfg getXCAInitiatingGW() {
        return localXdsDevice.getDeviceExtension(XCAInitiatingGWCfg.class);
    }

    public static XCAiRespondingGWCfg getXCAiRespondingGW() {
        return localXdsDevice.getDeviceExtension(XCAiRespondingGWCfg.class);
    }

    public static XCAiInitiatingGWCfg getXCAiInitiatingGW() {
        return localXdsDevice.getDeviceExtension(XCAiInitiatingGWCfg.class);
    }

    public static HL7Application findHL7Application(String name) {
        try {
            return name == null ? null : hl7AppCache.findHL7Application(name);
        } catch (ConfigurationException e) {
            log.warn("HL7Application not found! name:"+name);
            return null;
        }
    }

	public static void checkMissingXdsDevice() throws XDSException {
		if (localXdsDevice == null) {
			log.info("XDS Device not initialized!");
			Device d = null;
			try {
				if (xdsConfig != null) {
					log.info("XDS Configuration already initialized! Try to find XDS Device in configuration.");
					try {
						d = xdsConfig.findDevice(xdsDeviceName);
						setLocalXdsDevice(d);
						return;
					} catch (Exception x) {
						log.info("XDS device '"+xdsDeviceName+"' not found! Try to re-initialize XDS configuration!");
					}
					log.info("XDS device '"+xdsDeviceName+"' not found! Try to re-initialize XDS configuration!");
					destroyCfg();
				} else {
					log.info("XDS Configuration is not initialized.");
				}
				log.info("initialize XDS Configuration and XDS Device!");
				init();
				if (localXdsDevice != null)
					return;
			} catch (Exception x) {
				log.error("(Re-)Initialize XDS Config and XDS Device failed!", x);
			}
			throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
    				"Missing XDS Device configuration!", null);
		} else {
    		log.info("XDS Device already set!");
		}
	}

    public static boolean reconfigure() {
        if (localXdsDevice != null) {
            try {
	        	localXdsDevice.reconfigure(xdsConfig.findDevice(localXdsDevice.getDeviceName()));
	            log.info("Device "+localXdsDevice.getDeviceName()+" reconfigured!");
	            localXdsDevice.rebindConnections();
	            log.info("rebindConnections done!");
	            return true;
            } catch (Exception x) {
                log.error("Reconfiguration of device "+localXdsDevice.getDeviceName()+" failed!", x);
            }
        } else {
            log.info("Local XDS Device not set! Reconfigure request ignored.");
        }
        return false;
    }

}
