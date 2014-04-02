package org.dcm4chee.xds2.conf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.api.generic.ConfigClass;
import org.dcm4che3.conf.api.generic.ConfigField;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DeviceExtension;

@ConfigClass(commonName="XDSBrowser", objectClass="xdsBrowser")

public class XdsBrowser {

    
    @Inject
    DicomConfiguration config;
    
    
    /**
     * Extensions of which devices the browser can start/stop/reconfigure/monitor.
     * Map extensionName-device
     */
    @ConfigField(name="xdsControlledDevices")
    private Set<Device> controlledDevices;
    
    
    
    public Set<Device> getControlledDevices() {
        return controlledDevices;
    }



    public void setControlledDevices(Set<Device> controlledDevices) {
        this.controlledDevices = controlledDevices;
    }



    public List<DeviceExtension> getControlledDeviceExtensions() {
        
        //List<Class<? extends DeviceExtension>> supported = Arrays.asList(XdsRegistry.class,XdsRepository.class,XCAiInitiatingGWCfg.class, XCAInitiatingGWCfg.class, XCARespondingGWCfg.class, XCAiRespondingGWCfg.class);
        
        List<DeviceExtension> exts = new ArrayList<DeviceExtension>();
        
        for (Device d : controlledDevices) {
            for (DeviceExtension de : d.listDeviceExtensions()) {
                if (de.getClass().getAnnotation(ConfigClass.class) != null)
                    exts.add(de);
            }
        }
        
        return exts;
    }

}
