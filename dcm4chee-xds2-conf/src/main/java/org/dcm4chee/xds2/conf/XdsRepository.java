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

import java.util.HashMap;
import java.util.Map;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.generic.ConfigClass;
import org.dcm4che3.conf.api.generic.ConfigField;
import org.dcm4che3.conf.api.generic.ReflectiveConfig;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DeviceExtension;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */

@ConfigClass(commonName = "XDSRepository", objectClass = "xdsRepository", nodeName = "xdsRepository")
public class XdsRepository extends DeviceExtension {

    private static final long serialVersionUID = -8258532093950989486L;

    @ConfigField(name = "xdsApplicationName")
    private String applicationName;

    @ConfigField(name = "xdsRepositoryUID")
    private String repositoryUID;

    @ConfigField(name = "xdsSoapMsgLogDir")
    private String soapLogDir;

    @ConfigField(name = "xdsAcceptedMimeTypes")
    private String[] acceptedMimeTypes = new String[] {};

    @ConfigField(name = "xdsLogFullMessageHosts")
    private String[] logFullMessageHosts = new String[] {};

    @ConfigField(name = "xdsCheckMimetype")
    private boolean checkMimetype;

    @ConfigField(name = "xdsAllowedCipherHostname")
    private String allowedCipherHostname;

    @ConfigField(name = "xdsForceMTOM")
    private boolean forceMTOM;

    @ConfigField(name = "xdsProvideUrl")
    private String provideUrl;

    @ConfigField(name = "xdsRetrieveUrl")
    private String retrieveUrl;

    @ConfigField(mapName = "xdsSources", mapKey = "xdsSourceUid", name = "xdsSource", mapElementObjectClass = "xdsSourceByUid")
    private Map<String, Device> srcDevicebySrcIdMap;

    public String getRegistryURL(String sourceID) {
        try {

            XdsSource src = srcDevicebySrcIdMap.get(sourceID).getDeviceExtensionNotNull(XdsSource.class);
            XdsRegistry reg = src.getRegistry().getDeviceExtensionNotNull(XdsRegistry.class);

            return reg.getRegisterUrl();
        } catch (Exception e) {
            throw new RuntimeException("Cannot retrieve the registry URL for source id " + sourceID, e);
        }
    }

    // Getters&setters

    public String getApplicationName() {
        return applicationName;
    }

    public Map<String, Device> getSrcDevicebySrcIdMap() {
        return srcDevicebySrcIdMap;
    }

    public void setSrcDevicebySrcIdMap(Map<String, Device> srcDevicebySrcIdMap) {
        this.srcDevicebySrcIdMap = srcDevicebySrcIdMap;
    }

    public final void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getProvideUrl() {
        return provideUrl;
    }

    public void setProvideUrl(String provideUrl) {
        this.provideUrl = provideUrl;
    }

    public String getRetrieveUrl() {
        return retrieveUrl;
    }

    public void setRetrieveUrl(String retrieveUrl) {
        this.retrieveUrl = retrieveUrl;
    }

    public String getSoapLogDir() {
        return soapLogDir;
    }

    public void setSoapLogDir(String soapLogDir) {
        this.soapLogDir = soapLogDir;
    }

    public String getRepositoryUID() {
        return repositoryUID;
    }

    public void setRepositoryUID(String repsitoryUID) {
        this.repositoryUID = repsitoryUID;
    }

    public String[] getAcceptedMimeTypes() {
        return acceptedMimeTypes;
    }

    public void setAcceptedMimeTypes(String[] mimeTypes) {
        this.acceptedMimeTypes = mimeTypes;
    }

    public boolean isCheckMimetype() {
        return checkMimetype;
    }

    public void setCheckMimetype(boolean checkMimetype) {
        this.checkMimetype = checkMimetype;
    }

    public String[] getLogFullMessageHosts() {
        return logFullMessageHosts;
    }

    public void setLogFullMessageHosts(String[] logFullMessageHosts) {
        this.logFullMessageHosts = logFullMessageHosts;
    }

    public String getAllowedCipherHostname() {
        return allowedCipherHostname;
    }

    public void setAllowedCipherHostname(String allowedCipherHostnames) {
        this.allowedCipherHostname = allowedCipherHostnames;
    }

    public boolean isForceMTOM() {
        return forceMTOM;
    }

    public void setForceMTOM(boolean forceMTOM) {
        this.forceMTOM = forceMTOM;
    }

    @Override
    public void reconfigure(DeviceExtension from) {
        XdsRepository src = (XdsRepository) from;
        ReflectiveConfig.reconfigure(src, this);
    }
}
