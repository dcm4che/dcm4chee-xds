package org.dcm4chee.conf.browser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.api.generic.ConfigClass;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DeviceExtension;
import org.dcm4chee.xds2.common.cdi.Xds;
import org.dcm4chee.xds2.ctrl.ConfigObjectJSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigRESTServicesServlet {

    public static final Logger log = LoggerFactory.getLogger(ConfigRESTServicesServlet.class);

    public enum OnlineStatus {
        ONLINE,
        OFFLINE,
        UNSUPPORTED
    }

    
    public class DeviceJSON {
        
        public String deviceName;
        public Collection<String> appEntities;
        public Collection<String> deviceExtensions;
        public boolean managable;
        
    }    
    
    public class ExtensionJSON {
        
        public String deviceName;
        public String extensionName;
        public OnlineStatus isOnline;
        public boolean restartable;
        public boolean reconfigurable;
        public ConfigObjectJSON configuration;
        
    }
    
    @Inject
    @Xds
    DicomConfiguration config;

    /*@Inject
    Instance<DeviceExtension> deviceExtensionClasses;*/


    
    @GET
    @Path("/devices")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DeviceJSON> listDevices() throws ConfigurationException {
        
        List<DeviceJSON> list = new ArrayList<DeviceJSON>();
        for (String deviceName : config.listDeviceNames()) {
            Device d = config.findDevice(deviceName);
            
            DeviceJSON jd = new DeviceJSON();
            jd.deviceName = deviceName;
            jd.managable = false;
            jd.appEntities = d.getApplicationAETitles();
            
            jd.deviceExtensions = new ArrayList<String>();
            for (DeviceExtension de : d.listDeviceExtensions()) {
                jd.deviceExtensions.add(de.getClass().getSimpleName());
            }
            
            list.add(jd);
        }
        
        return list;
    }
    
    @GET
    @Path("/extensions/{deviceName}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ExtensionJSON> getExtensions(@PathParam(value = "deviceName") String deviceName) throws ConfigurationException {

        
        Device d = config.findDevice(deviceName);

        
        List<ExtensionJSON> extList = new ArrayList<ExtensionJSON>();
        
        for (DeviceExtension de : d.listDeviceExtensions()) {
            if (de.getClass().getAnnotation(ConfigClass.class) == null) continue;
            
            ExtensionJSON extJson = new ExtensionJSON();

            extJson.deviceName = deviceName;
            extJson.reconfigurable = false;
            extJson.restartable = false;
            extJson.extensionName = de.getClass().getSimpleName();
            extJson.configuration = ConfigObjectJSON.serializeDeviceExtension(de);
            
            extList.add(extJson);
        }

        return extList;

    }

}
