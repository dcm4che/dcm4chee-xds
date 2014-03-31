package org.dcm4chee.xds2.conf;

import org.dcm4che3.conf.api.generic.ConfigClass;
import org.dcm4che3.conf.api.generic.ConfigField;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DeviceExtension;

@ConfigClass(commonName = "XDSSource", objectClass = "xdsSource", nodeName = "xdsSource")
public class XdsSource extends DeviceExtension {

    /**
     * 
     */
    private static final long serialVersionUID = 1288348626210008707L;

    @ConfigField(name = "xdsSourceUid")
    private String uid;

    /**
     * Which registry to route Register requests/queries
     */
    @ConfigField(name = "xdsRegistry")
    private Device registry;

    @ConfigField(name = "xdsRepository")
    private Device repository;

    @ConfigField(name = "xdsURL")
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Device getRegistry() {
        return registry;
    }

    public void setRegistry(Device registry) {
        this.registry = registry;
    }

    public Device getRepository() {
        return repository;
    }

    public void setRepository(Device repository) {
        this.repository = repository;
    }

}
