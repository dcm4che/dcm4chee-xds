package org.dcm4chee.xds2.conf;

import org.dcm4che3.conf.core.api.ConfigurableClass;
import org.dcm4che3.conf.core.api.ConfigurableProperty;
import org.dcm4che3.conf.core.api.LDAP;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DeviceExtension;

@LDAP(objectClasses = "xdsSource")
@ConfigurableClass
public class XdsSource extends DeviceExtension {

    private static final long serialVersionUID = 1288348626210008707L;

    @ConfigurableProperty(name = "xdsSourceUid")
    private String uid;

    /**
     * Which registry to route Register requests/queries
     */
    @ConfigurableProperty(name = "xdsRegistry", isReference = true)
    private Device registry;

    @ConfigurableProperty(name = "xdsRepository", isReference = true)
    private Device repository;

    @ConfigurableProperty(name = "xdsURL")
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
