package org.dcm4chee.xds2.ctrl;

import java.util.Map;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.generic.ConfigField;
import org.dcm4che3.conf.api.generic.ReflectiveConfig;
import org.dcm4che3.conf.api.generic.adapters.ReflectiveAdapter;
import org.dcm4che3.net.DeviceExtension;

/**
 * A representation of a configuration object easily de/serializable from/to JSON  
 * @author Roman K
 * 
 * Will be moved to the library, this package is temporary
 *
 */
public class ConfigObjectJSON {

        /**
         * Object here is either a primitive, an array, a list, or Map<String, Object>
         */
        public Map<String,Object> rootConfigNode;

        public Map<String, Object> metadata;
        
    	public static ConfigObjectJSON serializeDeviceExtension(DeviceExtension de) throws ConfigurationException {
    		ConfigObjectJSON edata = new ConfigObjectJSON();


    		// serialize the configuration
    		
    		ReflectiveConfig rconfig = new ReflectiveConfig(null, null);
    		ReflectiveAdapter ad = new ReflectiveAdapter(de.getClass());
    		
    		try {
    		    edata.rootConfigNode = ad.serialize(de, rconfig, null);
    		    edata.metadata = ad.getMetadata(rconfig, null);
    		} catch (ConfigurationException e) {
    		    throw new ConfigurationException("Unable to serialize configuration for presentation in the browser for " + de.getClass().getSimpleName() + ", device "
    		            + de.getDevice().getDeviceName(),e);
    		}
    		return edata;
    	}

}
