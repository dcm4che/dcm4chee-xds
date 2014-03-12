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
 * Portions created by the Initial Developer are Copyright (C) 2011
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

package org.dcm4chee.xds2.conf.ldap;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.ModificationItem;

import org.dcm4che.conf.api.ConfigurationException;
import org.dcm4che.conf.ldap.LdapDicomConfigurationExtension;
import org.dcm4che.conf.ldap.LdapUtils;
import org.dcm4che.net.Device;
import org.dcm4chee.xds2.conf.XdsRegistry;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Franz Willer <franz.willer@gmail.com>
 */
public class LdapXDSRegistryConfiguration extends LdapDicomConfigurationExtension {
    private static final String CN_XDS_REGISTRY = "cn=XDSRegistry,";

    @Override
    protected void storeChilds(String deviceDN, Device device) throws NamingException {
        XdsRegistry registry = device.getDeviceExtension(XdsRegistry.class);
        if (registry != null)
            store(deviceDN, registry);
    }

    private void store(String deviceDN, XdsRegistry registry)
            throws NamingException {
        config.createSubcontext(CN_XDS_REGISTRY + deviceDN,
                storeTo(registry, deviceDN, new BasicAttributes(true)));
    }

    private Attributes storeTo(XdsRegistry registry, String deviceDN, Attributes attrs) {
        attrs.put(new BasicAttribute("objectclass", "xdsRegistry"));
        attrs.put(new BasicAttribute("cn", "XDSRegistry"));
        LdapUtils.storeNotNull(attrs, "xdsApplicationName", registry.getApplicationName());
        LdapUtils.storeNotEmpty(attrs, "xdsAffinityDomain", registry.getAffinityDomain());
        LdapUtils.storeNotNull(attrs, "xdsAffinityDomainConfigDir", registry.getAffinityDomainConfigDir());
        LdapUtils.storeNotEmpty(attrs, "xdsAcceptedMimeTypes", registry.getAcceptedMimeTypes());
        LdapUtils.storeNotNull(attrs, "xdsSoapMsgLogDir", registry.getSoapLogDir());
        LdapUtils.storeNotDef(attrs, "xdsCreateMissingPIDs", registry.isCreateMissingPIDs(), false);
        LdapUtils.storeNotDef(attrs, "xdsCreateMissingCodes", registry.isCreateMissingPIDs(), false);
        LdapUtils.storeNotDef(attrs, "xdsCheckAffinityDomain", registry.isCheckAffinityDomain(), true);
        LdapUtils.storeNotDef(attrs, "xdsCheckMimetype", registry.isCheckMimetype(), true);
        LdapUtils.storeNotDef(attrs, "xdsDontSaveCodeClassifications", registry.isDontSaveCodeClassifications(), false);
        LdapUtils.storeNotDef(attrs, "xdsPreMetadataCheck", registry.isPreMetadataCheck(), false);
        return attrs;
    }

    @Override
    protected void loadChilds(Device device, String deviceDN)
            throws NamingException, ConfigurationException {
        Attributes attrs;
        try {
            attrs = config.getAttributes(CN_XDS_REGISTRY + deviceDN);
        } catch (NameNotFoundException e) {
            return;
        }
        XdsRegistry registry = new XdsRegistry();
        loadFrom(registry, attrs);
        device.addDeviceExtension(registry);
    }

    private void loadFrom(XdsRegistry registry, Attributes attrs) throws NamingException {
        registry.setApplicationName(LdapUtils.stringValue(attrs.get("xdsApplicationName"), null));
        registry.setAffinityDomain(LdapUtils.stringArray(attrs.get("xdsAffinityDomain")));
        registry.setAffinityDomainConfigDir(LdapUtils.stringValue(attrs.get("xdsAffinityDomainConfigDir"), null));
        registry.setAcceptedMimeTypes(LdapUtils.stringArray(attrs.get("xdsAcceptedMimeTypes")));
        registry.setSoapLogDir(LdapUtils.stringValue(attrs.get("xdsSoapMsgLogDir"), null));
        registry.setCreateMissingPIDs(LdapUtils.booleanValue(attrs.get("xdsCreateMissingPIDs"), false));
        registry.setCreateMissingCodes(LdapUtils.booleanValue(attrs.get("xdsCreateMissingCodes"), false));
        registry.setDontSaveCodeClassifications(LdapUtils.booleanValue(attrs.get("xdsDontSaveCodeClassifications"), false));
        registry.setCheckAffinityDomain(LdapUtils.booleanValue(attrs.get("xdsCheckAffinityDomain"), true));
        registry.setCheckMimetype(LdapUtils.booleanValue(attrs.get("xdsCheckMimetype"), true));
        registry.setPreMetadataCheck(LdapUtils.booleanValue(attrs.get("xdsPreMetadataCheck"), false));
    }

    @Override
    protected void mergeChilds(Device prev, Device device, String deviceDN)
            throws NamingException {
        XdsRegistry prevRegistry = prev.getDeviceExtension(XdsRegistry.class);
        XdsRegistry registry = device.getDeviceExtension(XdsRegistry.class);
        if (registry == null) {
            if (prevRegistry != null)
                config.destroySubcontextWithChilds(CN_XDS_REGISTRY + deviceDN);
            return;
        }
        if (prevRegistry == null) {
            store(deviceDN, registry);
            return;
        }
        config.modifyAttributes(CN_XDS_REGISTRY + deviceDN,
                storeDiffs(prevRegistry, registry, deviceDN,
                        new ArrayList<ModificationItem>()));
    }

    private List<ModificationItem> storeDiffs(XdsRegistry prevRegistry, XdsRegistry registry,
            String deviceDN, ArrayList<ModificationItem> mods) {
        LdapUtils.storeDiff(mods, "xdsApplicationName",
                prevRegistry.getApplicationName(),
                registry.getApplicationName());
        LdapUtils.storeDiff(mods, "xdsAffinityDomain",
                prevRegistry.getAffinityDomain(),
                registry.getAffinityDomain());
        LdapUtils.storeDiff(mods, "xdsAffinityDomainConfigDir",
                prevRegistry.getAffinityDomainConfigDir(),
                registry.getAffinityDomainConfigDir());
        LdapUtils.storeDiff(mods, "xdsAcceptedMimeTypes",
                prevRegistry.getAcceptedMimeTypes(),
                registry.getAcceptedMimeTypes());
        LdapUtils.storeDiff(mods, "xdsSoapMsgLogDir",
                prevRegistry.getSoapLogDir(),
                registry.getSoapLogDir());
        LdapUtils.storeDiff(mods, "xdsCreateMissingPIDs",
                prevRegistry.isCreateMissingPIDs(),
                registry.isCreateMissingPIDs());
        LdapUtils.storeDiff(mods, "xdsCreateMissingCodes",
                prevRegistry.isCreateMissingCodes(),
                registry.isCreateMissingCodes());
        LdapUtils.storeDiff(mods, "xdsDontSaveCodeClassifications",
                prevRegistry.isCreateMissingCodes(),
                registry.isCreateMissingCodes());
        LdapUtils.storeDiff(mods, "xdsCheckAffinityDomain",
                prevRegistry.isCheckAffinityDomain(),
                registry.isCheckAffinityDomain());
        LdapUtils.storeDiff(mods, "xdsCheckMimetype",
                prevRegistry.isCheckMimetype(),
                registry.isCheckMimetype());
        LdapUtils.storeDiff(mods, "xdsPreMetadataCheck",
                prevRegistry.isPreMetadataCheck(),
                registry.isPreMetadataCheck());
        return mods;
    }
}
