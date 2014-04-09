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

import java.util.Collection;
import java.util.Map;

import org.dcm4che3.net.DeviceExtension;
import org.dcm4che3.conf.api.generic.ConfigClass;
import org.dcm4che3.conf.api.generic.ConfigField;
import org.dcm4che3.conf.api.generic.ReflectiveConfig;
import org.dcm4che3.net.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */
@ConfigClass(commonName = "XCAiInitiatingGW", objectClass = "xcaiInitiatingGW", nodeName = "xcaiInitiatingGW")
public class XCAiInitiatingGWCfg extends DeviceExtension {

    public static final Logger log = LoggerFactory.getLogger(XCAiInitiatingGWCfg.class);

    private static final long serialVersionUID = -8258532093950989486L;

    private static final Object DEFAULTID = "*";

    @ConfigField(name = "xdsApplicationName")
    private String applicationName;

    @ConfigField(name = "xdsHomeCommunityID")
    private String homeCommunityID;

    @ConfigField(name = "xdsSoapMsgLogDir")
    private String soapLogDir;

    @ConfigField(name = "xdsAsync")
    private boolean async;

    @ConfigField(name = "xdsAsyncHandler")
    private boolean asyncHandler;

    @ConfigField(mapName = "xdsRespondingGateways", mapKey = "xdsHomeCommunityId", name = "xdsRespondingGateway", mapElementObjectClass = "xdsRespondingGatewayByHomeCommunityId")
    private Map<String, Device> respondingGWDevicebyHomeCommunityId;

    @ConfigField(mapName = "xdsImagingSources", mapKey = "xdsSourceUid", name = "xdsImagingSource", mapElementObjectClass = "xdsImagingSourceByUid")
    private Map<String, Device> srcDevicebySrcIdMap;

    public String getRespondingGWURL(String homeCommunityID) {
        try {
            return respondingGWDevicebyHomeCommunityId.get(homeCommunityID).getDeviceExtensionNotNull(XCAiRespondingGWCfg.class)
                    .getRetrieveUrl();
        } catch (Exception e) {

            try {
                String url = respondingGWDevicebyHomeCommunityId.get(DEFAULTID).getDeviceExtensionNotNull(XCAiRespondingGWCfg.class)
                        .getRetrieveUrl();
                log.warn("Using default XCAi responding GW for home community id {}!", homeCommunityID);
                return url;
            } catch (Exception ee) {
                throw new RuntimeException("Cannot retrieve URL of responding GW for homeCommunityId " + homeCommunityID, e);
            }

        }
    }

    public Collection<String> getCommunityIDs() {
        return respondingGWDevicebyHomeCommunityId.keySet();
    }

    public String getXDSiSourceURL(String sourceID) {
        try {
            return srcDevicebySrcIdMap.get(sourceID).getDeviceExtensionNotNull(XdsSource.class).getUrl();
        } catch (Exception e) {
            try {
                String srcurl =  srcDevicebySrcIdMap.get(DEFAULTID).getDeviceExtensionNotNull(XdsSource.class).getUrl();
                log.warn("Using default URL of XDSiSource for source id {}!", homeCommunityID);
                return srcurl;
            } catch (Exception ee) {
                throw new RuntimeException("Cannot retrieve URL of XDSiSource for source id " + sourceID, e);
            }

        }        
    }

    public Map<String, Device> getRespondingGWDevicebyHomeCommunityId() {
        return respondingGWDevicebyHomeCommunityId;
    }

    public void setRespondingGWDevicebyHomeCommunityId(Map<String, Device> respondingGWDevicebyHomeCommunityId) {
        this.respondingGWDevicebyHomeCommunityId = respondingGWDevicebyHomeCommunityId;
    }

    public Map<String, Device> getSrcDevicebySrcIdMap() {
        return srcDevicebySrcIdMap;
    }

    public void setSrcDevicebySrcIdMap(Map<String, Device> srcDevicebySrcIdMap) {
        this.srcDevicebySrcIdMap = srcDevicebySrcIdMap;
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

    @Override
    public void reconfigure(DeviceExtension from) {
        XCAiInitiatingGWCfg src = (XCAiInitiatingGWCfg) from;
        ReflectiveConfig.reconfigure(src, this);
    }
}
