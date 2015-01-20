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

import org.dcm4che3.conf.core.api.ConfigurableClass;
import org.dcm4che3.conf.core.api.ConfigurableProperty;
import org.dcm4che3.conf.core.api.LDAP;
import org.dcm4che3.conf.core.util.ConfigIterators;
import org.dcm4che3.net.DeviceExtension;
import org.dcm4che3.util.StringUtils;
import org.dcm4chee.xds2.common.code.XADCfgRepository;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */

@LDAP(objectClasses = "xdsRegistry")
@ConfigurableClass
public class XdsRegistry extends XdsExtension {

    private static final String DEFAULT_AFFINITYDOMAIN_CFG_DIR = "${jboss.server.config.dir}/affinitydomain";

    private static final long serialVersionUID = -8258532093950989486L;

    @ConfigurableProperty(name = "xdsAffinityDomain",
            label = "Affinity Domain",
            description = "Affinity Domain given as Universal Entity ID and Universal Entity ID Type ISO (e.g.: 1.2.3.4.5&ISO)",
            group = "Affinity domain"
    )
    private String[] affinityDomain = new String[]{};

    @ConfigurableProperty(name = "xdsAffinityDomainConfigDir",
            label = "Affinity Domain Config Directory",
            description = "Path to affinity domain configuration directory",
            group = "Affinity domain")
    private String affinityDomainConfigDir;

    @ConfigurableProperty(name = "xdsCheckMimetype",
            label = "Check MIME Type",
            description = "Perform MIME type check for registered documents",
            defaultValue = "true",
            group = "XDS profile strictness")
    private boolean checkMimetype;

    @ConfigurableProperty(name = "xdsAcceptedMimeTypes",
            label = "Accept MIME Types",
            description = "Which MIME types documents that are registered with this registry allowed to have.",
            group = "XDS profile strictness")
    private String[] acceptedMimeTypes = new String[]{};

    @ConfigurableProperty(name = "xdsCreateMissingPIDs",
            label = "Create Missing Patient IDs",
            description = "Specifies to create Patient IDs that are not known by the registry. (not conform to XDS specification!)",
            defaultValue = "false",
            group = "XDS profile strictness")
    private boolean createMissingPIDs;

    @ConfigurableProperty(name = "xdsCreateMissingCodes",
            label = "Create Missing Codes",
            description = "Specifies to create Codes that are not known in the Affinity Domain. (not conform to XDS specification!)",
            defaultValue = "false",
            group = "XDS profile strictness")
    private boolean createMissingCodes;

    @ConfigurableProperty(name = "xdsCheckAffinityDomain",
            label = "Check Affinity Domain",
            description = "Checks for matching affinity domain in received patient feeds and when registering documents. Only deactivate for testing.",
            defaultValue = "true",
            group = "Affinity domain")
    private boolean checkAffinityDomain;

    @ConfigurableProperty(name = "xdsRegisterUrl",
            label = "Register URL",
            description = "Register URL that should be used to register documents with this registry (Does NOT actually configure the registry's endpoint!)",
            group = "Endpoints"
    )
    private String registerUrl;

    @ConfigurableProperty(name = "xdsQueryUrl",
            label = "Query URL",
            description = "Query URL that should be used to query this registry (Does NOT actually configure the registry endpoint!)",
            group = "Endpoints"
    )
    private String queryUrl;

    private XADCfgRepository xadCfgRepository;


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

    @Override
    public void reconfigure(DeviceExtension from) {
        XdsRegistry src = (XdsRegistry) from;
        ConfigIterators.reconfigure(src, this, XdsRegistry.class);
    }

}
