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
import java.util.HashMap;
import java.util.Map;

import org.dcm4che.conf.api.ConfigurationException;
import org.dcm4che.conf.api.hl7.HL7ApplicationCache;
import org.dcm4che.conf.api.hl7.HL7Configuration;
import org.dcm4che.net.DeviceExtension;
import org.dcm4che.net.hl7.HL7Application;
import org.dcm4chee.xds2.common.XDSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */
public class XCAInitiatingGWCfg extends DeviceExtension {

    private static final long serialVersionUID = -8258532093950989486L;

    private String applicationName;
    private String homeCommunityID;
    private Map<String, String> respondingGWUrlMapping = new HashMap<String,String>();
    private Map<String, String> respondingGWRetrieveUrlMapping = new HashMap<String,String>();
    private Map<String, String> homeIdToAssigningAuthotityMapping = new HashMap<String,String>();
    //AffinityDomain Option
    private String registryURL;
    private Map<String, String> repositoryUrlMapping = new HashMap<String,String>();
    private String soapLogDir;
    private boolean async;
    private boolean asyncHandler;
    private String pixManagerApplication;
    private String pixConsumerApplication;
    private HL7ApplicationCache hl7AppCache;
    
    private static Logger log = LoggerFactory.getLogger(XCAInitiatingGWCfg.class);

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
    public String getRegistryURL() {
        return registryURL;
    }
    public void setRegistryURL(String registryURL) {
        this.registryURL = registryURL;
    }
    public String getSoapLogDir() {
        return soapLogDir;
    }

    public void setSoapLogDir(String soapLogDir) {
        this.soapLogDir = soapLogDir;
    }
    
    public String[] getRepositoryURLs() {
        return XDSUtil.map2keyValueStrings(repositoryUrlMapping, '|');
    }
    public void setRepositoryURLs(String[] repositoryURLs) {
        XDSUtil.storeKeyValueStrings2map(repositoryURLs, '|', "*", repositoryUrlMapping);
    }
    public String getRepositoryURL(String repositoryID) {
        return XDSUtil.getValue(repositoryID, "*", repositoryUrlMapping);
    }

    public String[] getRespondingGWURLs() {
        return XDSUtil.map2keyValueStrings(respondingGWUrlMapping, '|');
    }
    public void setRespondingGWURLs(String[] urls) {
        XDSUtil.storeKeyValueStrings2map(urls, '|', "*", respondingGWUrlMapping);
    }
    public String[] getRespondingGWRetrieveURLs() {
        return XDSUtil.map2keyValueStrings(respondingGWRetrieveUrlMapping, '|');
    }
    public void setRespondingGWRetrieveURLs(String[] urls) {
        XDSUtil.storeKeyValueStrings2map(urls, '|', "*", respondingGWRetrieveUrlMapping);
    }
    
    public Collection<String> getCommunityIDs() {
        return respondingGWUrlMapping.keySet();
    }
    public String getRespondingGWQueryURL(String homeCommunityID) {
        return XDSUtil.getValue(homeCommunityID, "*", respondingGWUrlMapping);
    }
    public String getRespondingGWRetrieveURL(String homeCommunityID) {
        String url = XDSUtil.getValue(homeCommunityID, "*", respondingGWRetrieveUrlMapping);
        return url == null || "query".equalsIgnoreCase(url) ? 
                XDSUtil.getValue(homeCommunityID, "*", respondingGWUrlMapping) : url;
    }

    public String[] getAssigningAuthoritiesMap() {
        return XDSUtil.map2keyValueStrings(homeIdToAssigningAuthotityMapping, '|');
    }
    public void setAssigningAuthoritiesMap(String[] sa) {
        XDSUtil.storeKeyValueStrings2map(sa, '|', "*", homeIdToAssigningAuthotityMapping);
    }
    public String[] getAssigningAuthorities() {
        int size = homeIdToAssigningAuthotityMapping.size();
        return size == 0 ? null : homeIdToAssigningAuthotityMapping.values().toArray(new String[size]);
    }
    public String getAssigningAuthority(String homeCommunityID) {
        return XDSUtil.getValue(homeCommunityID, "*", homeIdToAssigningAuthotityMapping);
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
        return pixManagerApplication;
    }

    public void setRemotePIXManagerApplication(String appName) {
        this.pixManagerApplication = appName;
    }

    public String getLocalPIXConsumerApplication() {
        return pixConsumerApplication;
    }

    public void setLocalPIXConsumerApplication(String appName) {
        this.pixConsumerApplication = appName;
    }

    public void init(HL7Configuration hl7Configuration) {
        this.hl7AppCache = new HL7ApplicationCache(hl7Configuration);
    }
    
    public HL7Application getPixConsumerApplication() {
        return findHL7Application(pixConsumerApplication);
    }
    public HL7Application getPixManagerApplication() {
        return findHL7Application(pixManagerApplication);
    }
    public HL7Application findHL7Application(String name) {
        try {
            return name == null ? null : hl7AppCache.findHL7Application(name);
        } catch (ConfigurationException e) {
            log.warn("HL7Application not found! name:"+name);
            return null;
        }
    }
    
    @Override
    public void reconfigure(DeviceExtension from) {
        XCAInitiatingGWCfg src = (XCAInitiatingGWCfg) from;
        setApplicationName(src.getApplicationName());
        setHomeCommunityID(src.getHomeCommunityID());
        setRegistryURL(src.getRegistryURL());
        setRepositoryURLs(src.getRepositoryURLs());
        setRespondingGWURLs(src.getRespondingGWURLs());
        setRespondingGWRetrieveURLs(src.getRespondingGWRetrieveURLs());
        setSoapLogDir(src.getSoapLogDir());
        setAsync(src.isAsync());
        setAsyncHandler(src.isAsyncHandler());
        setRemotePIXManagerApplication(src.pixManagerApplication);
        setLocalPIXConsumerApplication(src.pixConsumerApplication);
        setAssigningAuthoritiesMap(src.getAssigningAuthoritiesMap());
    }
}
