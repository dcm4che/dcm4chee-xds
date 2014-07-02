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

import org.dcm4che3.net.DeviceExtension;
import org.dcm4che3.util.StringUtils;
import org.dcm4che3.conf.api.generic.ConfigClass;
import org.dcm4che3.conf.api.generic.ConfigField;
import org.dcm4che3.conf.api.generic.ReflectiveConfig;
import org.dcm4chee.xds2.common.code.XADCfgRepository;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */

@ConfigClass(commonName = "XDSRegistry", objectClass = "xdsRegistry", nodeName = "xdsRegistry")
public class XdsRegistry extends DeviceExtension {

    private static final String DEFAULT_AFFINITYDOMAIN_CFG_DIR = "${jboss.server.config.dir}/affinitydomain";

    private static final long serialVersionUID = -8258532093950989486L;

    @ConfigField(name = "xdsApplicationName",
                label = "Application Name",
                description = "XDS Application name")
    private String applicationName;

    @ConfigField(name = "xdsAffinityDomain",
                label =  "Affinity Domain",
                description = "Affinity Domain given as Universal Entity ID and Universal Entity ID Type ISO (e.g.: 1.2.3.4.5&ISO)")
    private String[] affinityDomain = new String[] {};

    @ConfigField(name = "xdsAffinityDomainConfigDir",
            label = "Affinity Domain Config Directory",
            description = "Path to affinity domain configuration directory")
    private String affinityDomainConfigDir;

    @ConfigField(name = "xdsAcceptedMimeTypes",
            label = "Accept MIME Types",
            description = "MIME types accepted by the webservice",
            optional = true)
    private String[] acceptedMimeTypes = new String[] {};

    @ConfigField(name = "xdsSoapMsgLogDir",
            optional = true)
    private String soapLogDir;

    @ConfigField(name = "xdsCreateMissingPIDs", 
            label= "Create Missing Patient IDs",
            description = "Specifies to create Patient IDs that are not yet known. (not conform to XDS specification!)",
            def = "false",
            optional = true)
    private boolean createMissingPIDs;

    @ConfigField(name = "xdsCreateMissingCodes",
            label = "Create Missing Codes",
            description= "Specifies to create Codes that are not known in the Affinity Domain. (not conform to XDS specification!)",
            def = "false",
            optional = true)
    private boolean createMissingCodes;

    @ConfigField(name = "xdsDontSaveCodeClassifications", 
            label = "Dont Save Code Classifications",
            description = "Specifies to save codes only as XDSCode entities and not as Classifications in ebRIM format",
            def = "false",
            optional = true)
    private boolean dontSaveCodeClassifications;

    @ConfigField(name = "xdsCheckAffinityDomain", 
            label = "Check Affinity Domain",
            description = "Check affinityDomain in received PatientIDs (Patient feed and preMetadataCheck)",
            def = "true",
            optional = true)
    private boolean checkAffinityDomain;

    // TODO: confirm correct meaning
    @ConfigField(name = "xdsCheckMimetype", 
            label = "Check MIME Type",
            description = "Indicates whether MIME types of incoming SOAP messages should be checked against accepted mime types",
            def = "true",
            optional = true)
    private boolean checkMimetype;

    @ConfigField(name = "xdsPreMetadataCheck", 
            label = "Pre-Metadata Check",
            description = "Check metadta before processing the PnR request. (to get correct error (XDSPatientIdDoesNotMatch instead of XDSUnknownPatientId) in pre-connectathon tests)",
            def = "false",
            optional = true)
    private boolean preMetadataCheck;

    @ConfigField(name = "xdsRegisterUrl",
            label = "Register URL",
            description = "Register URL that should be used to register documents with this registry (Does NOT actually configure the registry's endpoint!)"
            )
    private String registerUrl;

    @ConfigField(name = "xdsQueryUrl",
            label = "Query URL",
            description = "Query URL that should be used to query this registry (Does NOT actually configure the registry endpoint!)")
    private String queryUrl;
    
    @ConfigField(name = "xdsBrowser", def = "null") 
    private XdsBrowser xdsBrowser;
    
    public XdsBrowser getXdsBrowser() {
        return xdsBrowser;
    }

    public void setXdsBrowser(XdsBrowser xdsBrowser) {
        this.xdsBrowser = xdsBrowser;
    }

    private XADCfgRepository xadCfgRepository;

    public String getApplicationName() {
        return applicationName;
    }

    public String getRegisterUrl() {
        return registerUrl;
    }

    public void setRegisterUrl(String registerUrl) {
        this.registerUrl = registerUrl;
    }

    public String getQueryUrl() {
        return queryUrl;
    }

    public void setQueryUrl(String queryUrl) {
        this.queryUrl = queryUrl;
    }

    public final void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String[] getAffinityDomain() {
        return affinityDomain;
    }

    public void setAffinityDomain(String[] ads) {
        this.affinityDomain = ads;
        if (ads != null) {
            String ad;
            for (int i = 0; i < ads.length; i++) {
                ad = ads[i];
                if (ad.endsWith("&ISO")) {
                    affinityDomain[i] = ad.substring(0, ad.length() - 4);
                }
            }
        }
    }

    public String getAffinityDomainConfigDir() {
        return affinityDomainConfigDir;
    }

    public void setAffinityDomainConfigDir(String dir) {
        this.affinityDomainConfigDir = dir;
        dir = StringUtils.replaceSystemProperties(dir == null ? DEFAULT_AFFINITYDOMAIN_CFG_DIR : dir);
        if (xadCfgRepository == null || xadCfgRepository.configChanged(null, dir)) {
            xadCfgRepository = new XADCfgRepository(System.getProperty("org.dcm4chee.xds.codeprovider.name", "IHECodeProvider"), dir);
        }
    }

    public XADCfgRepository getCodeRepository() {
        return xadCfgRepository;
    }

    public String[] getAcceptedMimeTypes() {
        return acceptedMimeTypes;
    }

    public void setAcceptedMimeTypes(String[] mimeTypes) {
        this.acceptedMimeTypes = mimeTypes;
    }

    public String getSoapLogDir() {
        return soapLogDir;
    }

    public void setSoapLogDir(String soapLogDir) {
        this.soapLogDir = soapLogDir;
    }

    public boolean isCreateMissingPIDs() {
        return createMissingPIDs;
    }

    public void setCreateMissingPIDs(boolean createMissingPIDs) {
        this.createMissingPIDs = createMissingPIDs;
    }

    public boolean isCreateMissingCodes() {
        return createMissingCodes;
    }

    public void setCreateMissingCodes(boolean createMissingCodes) {
        this.createMissingCodes = createMissingCodes;
    }

    public boolean isDontSaveCodeClassifications() {
        return dontSaveCodeClassifications;
    }

    public void setDontSaveCodeClassifications(boolean dontSaveCodeClassifications) {
        this.dontSaveCodeClassifications = dontSaveCodeClassifications;
    }

    public boolean isCheckAffinityDomain() {
        return checkAffinityDomain;
    }

    public void setCheckAffinityDomain(boolean checkAffinityDomain) {
        this.checkAffinityDomain = checkAffinityDomain;
    }

    public boolean isCheckMimetype() {
        return checkMimetype;
    }

    public void setCheckMimetype(boolean checkMimetype) {
        this.checkMimetype = checkMimetype;
    }

    public boolean isPreMetadataCheck() {
        return preMetadataCheck;
    }

    public void setPreMetadataCheck(boolean b) {
        preMetadataCheck = b;
    }

    @Override
    public void reconfigure(DeviceExtension from) {
        XdsRegistry src = (XdsRegistry) from;
        ReflectiveConfig.reconfigure(src, this);
    }
}
