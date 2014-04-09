package org.dcm4chee.xds2.ctrl;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.generic.ReflectiveConfig;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigNode;
import org.dcm4che3.conf.api.generic.adapters.ReflectiveAdapter;
import org.dcm4che3.net.DeviceExtension;

public class GenericDeviceExtensionJSON {

        public String devicename;
        public String extensiontype;
        public ConfigNode config;
        
        
    	public static GenericDeviceExtensionJSON serializeDeviceExtension(DeviceExtension de) throws ConfigurationException {
    		GenericDeviceExtensionJSON edata = new GenericDeviceExtensionJSON();

    		// fill in stuff
    		edata.devicename = de.getDevice().getDeviceName();
    		edata.extensiontype = de.getClass().getSimpleName();

    		// serialize the configuration
    		
    		ReflectiveConfig rconfig = new ReflectiveConfig(null, null);
    		ReflectiveAdapter ad = new ReflectiveAdapter(de.getClass());
    		
    		try {
    		    edata.config = ad.serialize(de, rconfig, null);
    		} catch (ConfigurationException e) {
    		    throw new ConfigurationException("Unable to serialize configuration for presentation in the browser for " + de.getClass().getSimpleName() + ", device "
    		            + de.getDevice().getDeviceName(),e);
    		}
    		return edata;
    	}

}
