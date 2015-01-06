package org.dcm4chee.xds2.conf;

import org.dcm4che3.conf.core.api.ConfigurableProperty;

public class XCAExtension extends XdsExtension {

    @ConfigurableProperty(name = "xdsHomeCommunityID")
    private String homeCommunityID;

    public String getHomeCommunityID() {
        return homeCommunityID;
    }

    public void setHomeCommunityID(String homeCommunityID) {
        this.homeCommunityID = homeCommunityID;
    }
}
