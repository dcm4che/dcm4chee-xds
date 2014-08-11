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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.dcm4che3.conf.api.generic.ConfigClass;
import org.dcm4che3.conf.api.generic.ConfigField;
import org.dcm4che3.conf.api.generic.ReflectiveConfig;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DeviceExtension;
import org.dcm4che3.net.hl7.HL7Application;
import org.dcm4chee.xds2.common.XDSUtil;
import org.dcm4chee.xds2.common.deactivatable.Deactivateable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */
@ConfigClass(commonName = "XCAInitiatingGW", objectClass = "xcaInitiatingGW", nodeName = "xcaInitiatingGW")
public class XCAInitiatingGWCfg extends DeviceExtension implements Deactivateable {

    @ConfigClass(objectClass = "xdsGatewayRef")
    public static class GatewayReference implements Serializable
    {

        private static final long serialVersionUID = 9174301221517954931L;

        @ConfigField(name = "xdsAffinityDomain")
        private String affinityDomain;

        @ConfigField(name = "xdsRespondingGateway")
        private Device respondingGWdevice;

        public String getAffinityDomain() {
            return affinityDomain;
        }

        public void setAffinityDomain(String affinityDomain) {
            this.affinityDomain = affinityDomain;
        }

        public Device getRespondingGWdevice() {
            return respondingGWdevice;
        }

        public void setRespondingGWdevice(Device respondingGWdevice) {
            this.respondingGWdevice = respondingGWdevice;
        }

        public GatewayReference() {
            super();
        }

    }

    public static final Logger log = LoggerFactory.getLogger(XCAInitiatingGWCfg.class);

    private static final long serialVersionUID = -8258532093950989486L;

    private static final String DEFAULTID = "*";

    @ConfigField(name = "xdsIsDeactivated",
            label = "Deactivated",
            description = "Controls whether the service is deactivated")
    private boolean deactivated = false;

    @ConfigField(name = "xdsApplicationName")
    private String applicationName;

    @ConfigField(name = "xdsHomeCommunityID")
    private String homeCommunityID;

    @ConfigField(name = "xdsRespondingGateways", mapKey = "xdsHomeCommunityID")
    private Map<String, GatewayReference> respondingGWByHomeCommunityIdMap;

    @ConfigField(mapName = "xdsRepositories", mapKey = "xdsRepositoryUid", name = "xdsRepository", mapElementObjectClass = "xdsRepositoryByUid")
    private Map<String, Device> repositoryDeviceByUidMap;

    // AffinityDomain Option
    @ConfigField(name = "xdsRegistry")
    private Device registry;

    @ConfigField(name = "xdsSoapMsgLogDir")
    private String soapLogDir;

    @ConfigField(name = "xdsAsync")
    private boolean async;

    @ConfigField(name = "xdsAsyncHandler")
    private boolean asyncHandler;

    @ConfigField(name = "xdsPIXManagerApplication")
    private String remotePIXManagerApplication;

    @ConfigField(name = "xdsPIXConsumerApplication")
    private String localPIXConsumerApplication;

    public Collection<String> getHomeCommunityIDs() {
        return respondingGWByHomeCommunityIdMap.keySet();
    }

    public String getRespondingGWQueryURL(String homeCommunityID) {
        try {
            return respondingGWByHomeCommunityIdMap.get(homeCommunityID).getRespondingGWdevice()
                    .getDeviceExtensionNotNull(XCARespondingGWCfg.class).getQueryUrl();
        } catch (Exception e) {

            try {
                String url = respondingGWByHomeCommunityIdMap.get(DEFAULTID).getRespondingGWdevice()
                        .getDeviceExtensionNotNull(XCARespondingGWCfg.class).getQueryUrl();
                log.warn("Using default XCA responding GW for home community id {}!", homeCommunityID);
                return url;
            } catch (Exception ee) {
                throw new RuntimeException("Cannot retrieve QueryURL of responding GW for homeCommunityId " + homeCommunityID, e);
            }

        }
    }

    public String getRespondingGWRetrieveURL(String homeCommunityID) {
        try {
            return respondingGWByHomeCommunityIdMap.get(homeCommunityID).getRespondingGWdevice()
                    .getDeviceExtensionNotNull(XCARespondingGWCfg.class).getRetrieveUrl();
        } catch (Exception e) {
            try {
                String url = respondingGWByHomeCommunityIdMap.get(DEFAULTID).getRespondingGWdevice()
                        .getDeviceExtensionNotNull(XCARespondingGWCfg.class).getRetrieveUrl();
                log.warn("Using default XCA responding GW for home community id {}!", homeCommunityID);
                return url;
            } catch (Exception ee) {
                throw new RuntimeException("Cannot retrieve retrieveURL of responding GW for homeCommunityId " + homeCommunityID, e);
            }
        }
    }

    public String getAssigningAuthority(String homeCommunityID) {
        try {
            return respondingGWByHomeCommunityIdMap.get(homeCommunityID).getAffinityDomain();
        } catch (Exception e) {
            try {
                String aa = respondingGWByHomeCommunityIdMap.get(DEFAULTID).getAffinityDomain();
                log.warn("Using default Assigning Authority for home community id {}!", homeCommunityID);
                return aa;
            } catch (Exception ee) {
                throw new RuntimeException("Cannot retrieve AffinityDomain for homeCommunityId " + homeCommunityID, e);
            }
        }
    }

    public String[] getAssigningAuthorities() {

        String[] authorities = new String[respondingGWByHomeCommunityIdMap.size()];

        int i = 0;
        for (Entry<String, GatewayReference> e : respondingGWByHomeCommunityIdMap.entrySet()) {
            authorities[i++] = e.getValue().getAffinityDomain();
        }

        return authorities;
    }

    public String getRegistryURL() {
        try {
            return registry.getDeviceExtensionNotNull(XdsRegistry.class).getQueryUrl();
        } catch (Exception e) {
            throw new RuntimeException("Cannot retrieve registry URL", e);
        }
    }

    public String getRepositoryURL(String repositoryID) {
        try {
            return repositoryDeviceByUidMap.get(repositoryID).getDeviceExtensionNotNull(XdsRepository.class).getRetrieveUrl();
        } catch (Exception e) {
            try {
                String repo = repositoryDeviceByUidMap.get(DEFAULTID).getDeviceExtensionNotNull(XdsRepository.class).getRetrieveUrl();
                log.warn("Using default Repository URL for repository UID {}!", repositoryID);
                return repo;
            } catch (Exception ee) {
                throw new RuntimeException("Cannot retrieve repository URL for repository UID " + repositoryID, e);
            }
        }


    }

    public Map<String, GatewayReference> getRespondingGWByHomeCommunityIdMap() {
        return respondingGWByHomeCommunityIdMap;
    }

    public void setRespondingGWByHomeCommunityIdMap(Map<String, GatewayReference> respondingGWByHomeCommunityIdMap) {
        this.respondingGWByHomeCommunityIdMap = respondingGWByHomeCommunityIdMap;
    }

    public Map<String, Device> getRepositoryDeviceByUidMap() {
        return repositoryDeviceByUidMap;
    }

    public void setRepositoryDeviceByUidMap(Map<String, Device> repositoryDeviceByUidMap) {
        this.repositoryDeviceByUidMap = repositoryDeviceByUidMap;
    }

    public Device getRegistry() {
        return registry;
    }

    public void setRegistry(Device registry) {
        this.registry = registry;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public final void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getHomeCommunityID() {
        return homeCommunityID;
    }

    public void setHomeCommunityID(String homeCommunityID) {
        this.homeCommunityID = homeCommunityID;
    }

    public String getSoapLogDir() {
        return soapLogDir;
    }

    public void setSoapLogDir(String soapLogDir) {
        this.soapLogDir = soapLogDir;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public boolean isAsyncHandler() {
        return asyncHandler;
    }

    public void setAsyncHandler(boolean asyncHandler) {
        this.asyncHandler = asyncHandler;
    }

    public String getRemotePIXManagerApplication() {
        return remotePIXManagerApplication;
    }

    public void setRemotePIXManagerApplication(String appName) {
        this.remotePIXManagerApplication = appName;
    }

    public String getLocalPIXConsumerApplication() {
        return localPIXConsumerApplication;
    }

    public void setLocalPIXConsumerApplication(String appName) {
        this.localPIXConsumerApplication = appName;
    }

    @Override
    public boolean isDeactivated() {
        return deactivated;
    }

    public void setDeactivated(boolean deactivated) {
        this.deactivated = deactivated;
    }

    @Override
    public void reconfigure(DeviceExtension from) {
        XCAInitiatingGWCfg src = (XCAInitiatingGWCfg) from;
        ReflectiveConfig.reconfigure(src, this);
    }

}
