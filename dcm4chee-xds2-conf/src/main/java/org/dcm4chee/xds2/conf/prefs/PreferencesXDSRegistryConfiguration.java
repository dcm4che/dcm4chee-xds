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
 * Portions created by the Initial Developer are Copyright (C) 2013
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

package org.dcm4chee.xds2.conf.prefs;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.dcm4che.conf.api.ConfigurationException;
import org.dcm4che.conf.prefs.PreferencesDicomConfigurationExtension;
import org.dcm4che.conf.prefs.PreferencesUtils;
import org.dcm4che.net.Device;
import org.dcm4chee.xds2.conf.XdsRegistry;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Franz Willer <franz.willer@gmail.com>
 *
 */
public class PreferencesXDSRegistryConfiguration
        extends PreferencesDicomConfigurationExtension {

    private static final String FALSE = "FALSE";
    private static final String NODE_NAME = "xdsRegistry";

    @Override
    protected void storeChilds(Device device, Preferences deviceNode) {
        XdsRegistry registry =
                device.getDeviceExtension(XdsRegistry.class);
        if (registry != null)
            storeTo(registry, deviceNode.node(NODE_NAME));
    }

    private void storeTo(XdsRegistry registry, Preferences prefs) {
        PreferencesUtils.storeNotNull(prefs, "xdsApplicationName", registry.getApplicationName());
        PreferencesUtils.storeNotEmpty(prefs, "xdsAffinityDomain", registry.getAffinityDomain());
        PreferencesUtils.storeNotEmpty(prefs, "xdsAcceptedMimeTypes", registry.getAcceptedMimeTypes());
        PreferencesUtils.storeNotNull(prefs, "xdsSoapMsgLogDir", registry.getSoapLogDir());
        PreferencesUtils.storeNotDef(prefs, "xdsCreateMissingPIDs",registry.isCreateMissingPIDs(), false);
        PreferencesUtils.storeNotDef(prefs, "xdsCreateMissingCodes", registry.isCreateMissingPIDs(), false);
        PreferencesUtils.storeNotDef(prefs, "xdsCheckAffinityDomain", registry.isCheckAffinityDomain(), false);
        PreferencesUtils.storeNotDef(prefs, "xdsCheckMimetype", registry.isCheckMimetype(), false);
        PreferencesUtils.storeNotDef(prefs, "xdsDontSaveCodeClassifications", registry.isDontSaveCodeClassifications(), false);
    }

    @Override
    protected void loadChilds(Device device, Preferences deviceNode)
            throws BackingStoreException, ConfigurationException {
        if (!deviceNode.nodeExists(NODE_NAME))
            return;
        
        Preferences loggerNode = deviceNode.node(NODE_NAME);
        XdsRegistry registry = new XdsRegistry();
        loadFrom(registry, loggerNode);
        device.addDeviceExtension(registry);
    }

    private void loadFrom(XdsRegistry registry, Preferences prefs) {
        registry.setApplicationName(prefs.get("xdsApplicationName",null));
        registry.setAffinityDomain(PreferencesUtils.stringArray(prefs, "xdsAffinityDomain"));
        registry.setAcceptedMimeTypes(PreferencesUtils.stringArray(prefs, "xdsAcceptedMimeTypes"));
        registry.setSoapLogDir(prefs.get("xdsSoapMsgLogDir", null));
        registry.setCreateMissingPIDs(PreferencesUtils.booleanValue(prefs.get("xdsCreateMissingPIDs", FALSE)));
        registry.setCreateMissingCodes(PreferencesUtils.booleanValue(prefs.get("xdsCreateMissingCodes", FALSE)));
        registry.setDontSaveCodeClassifications(PreferencesUtils.booleanValue(prefs.get("xdsDontSaveCodeClassifications", FALSE)));
        registry.setCheckAffinityDomain(PreferencesUtils.booleanValue(prefs.get("xdsCheckAffinityDomain", "true")));
        registry.setCheckMimetype(PreferencesUtils.booleanValue(prefs.get("xdsCheckMimetype", "true")));
        registry.setPreMetadataCheck(PreferencesUtils.booleanValue(prefs.get("xdsPreMetadataCheck", FALSE)));
    }

    @Override
    protected void mergeChilds(Device prev, Device device, Preferences deviceNode)
            throws BackingStoreException {
        XdsRegistry prevRegistry =
                prev.getDeviceExtension(XdsRegistry.class);
        XdsRegistry registry =
                device.getDeviceExtension(XdsRegistry.class);
        if (registry == null && prevRegistry == null)
            return;
        
        Preferences xdsNode = deviceNode.node(NODE_NAME);
        if (registry == null)
            xdsNode.removeNode();
        else if (prevRegistry == null)
            storeTo(registry, xdsNode);
        else
            storeDiffs(xdsNode, prevRegistry, registry);
    }

    private void storeDiffs(Preferences prefs, XdsRegistry prevRegistry, XdsRegistry registry) {
        PreferencesUtils.storeDiff(prefs, "xdsApplicationName",
                prevRegistry.getApplicationName(),
                registry.getApplicationName());
        PreferencesUtils.storeDiff(prefs, "xdsAffinityDomain",
                prevRegistry.getAffinityDomain(),
                registry.getAffinityDomain());
        PreferencesUtils.storeDiff(prefs, "xdsAcceptedMimeTypes",
                prevRegistry.getAcceptedMimeTypes(),
                registry.getAcceptedMimeTypes());
        PreferencesUtils.storeDiff(prefs, "xdsSoapMsgLogDir",
                prevRegistry.getSoapLogDir(),
                registry.getSoapLogDir());
        PreferencesUtils.storeDiff(prefs, "xdsCreateMissingPIDs",
                prevRegistry.isCreateMissingPIDs(),
                registry.isCreateMissingPIDs());
        PreferencesUtils.storeDiff(prefs, "xdsCreateMissingCodes",
                prevRegistry.isCreateMissingCodes(),
                registry.isCreateMissingCodes());
        PreferencesUtils.storeDiff(prefs, "xdsDontSaveCodeClassifications",
                prevRegistry.isCreateMissingCodes(),
                registry.isCreateMissingCodes());
        PreferencesUtils.storeDiff(prefs, "xdsCheckAffinityDomain",
                prevRegistry.isCheckAffinityDomain(),
                registry.isCheckAffinityDomain());
        PreferencesUtils.storeDiff(prefs, "xdsCheckMimetype",
                prevRegistry.isCheckMimetype(),
                registry.isCheckMimetype());
        PreferencesUtils.storeDiff(prefs, "xdsPreMetadataCheck",
                prevRegistry.isPreMetadataCheck(),
                registry.isPreMetadataCheck());
    }
}
