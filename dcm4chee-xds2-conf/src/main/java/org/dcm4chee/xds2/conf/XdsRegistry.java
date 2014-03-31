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

	@ConfigField(name = "xdsApplicationName")
	private String applicationName;

	@ConfigField(name = "xdsAffinityDomain")
	private String[] affinityDomain = new String[] {};

	@ConfigField(name = "xdsAffinityDomainConfigDir")
	private String affinityDomainConfigDir;

	@ConfigField(name = "xdsAcceptedMimeTypes")
	private String[] acceptedMimeTypes = new String[] {};

	@ConfigField(name = "xdsSoapMsgLogDir")
	private String soapLogDir;

	@ConfigField(name = "xdsCreateMissingPIDs", def = "false")
	private boolean createMissingPIDs;

	@ConfigField(name = "xdsCreateMissingCodes", def = "false")
	private boolean createMissingCodes;

	@ConfigField(name = "xdsDontSaveCodeClassifications", def = "false")
	private boolean dontSaveCodeClassifications;

	@ConfigField(name = "xdsCheckAffinityDomain", def = "true")
	private boolean checkAffinityDomain;

	@ConfigField(name = "xdsCheckMimetype", def = "true")
	private boolean checkMimetype;

	@ConfigField(name = "xdsPreMetadataCheck", def = "false")
	private boolean preMetadataCheck;

	private XADCfgRepository xadCfgRepository;

	public String getApplicationName() {
		return applicationName;
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
