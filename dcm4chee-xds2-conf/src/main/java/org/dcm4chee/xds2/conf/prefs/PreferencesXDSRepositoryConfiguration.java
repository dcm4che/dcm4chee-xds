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
import org.dcm4chee.xds2.conf.XdsRepository;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Franz Willer <franz.willer@gmail.com>
 *
 */
public class PreferencesXDSRepositoryConfiguration
        extends PreferencesDicomConfigurationExtension {

    private static final String NODE_NAME = "xdsRepository";

    @Override
    protected void storeChilds(Device device, Preferences deviceNode) {
        XdsRepository repository =
                device.getDeviceExtension(XdsRepository.class);
        if (repository != null)
            storeTo(repository, deviceNode.node(NODE_NAME));
    }

    private void storeTo(XdsRepository repository, Preferences prefs) {
        PreferencesUtils.storeNotNull(prefs, "xdsApplicationName", repository.getApplicationName());
        PreferencesUtils.storeNotNull(prefs, "xdsRepositoryUID", repository.getRepositoryUID());
        PreferencesUtils.storeNotEmpty(prefs, "xdsRegistryURL", repository.getRegistryURLs());
        PreferencesUtils.storeNotEmpty(prefs, "xdsAcceptedMimeTypes", repository.getAcceptedMimeTypes());
        PreferencesUtils.storeNotNull(prefs, "xdsSoapMsgLogDir", repository.getSoapLogDir());
        PreferencesUtils.storeNotDef(prefs, "xdsCheckMimetype", repository.isCheckMimetype(), false);
        PreferencesUtils.storeNotEmpty(prefs, "xdsLogFullMessageHosts", repository.getLogFullMessageHosts());
        PreferencesUtils.storeNotNull(prefs, "xdsAllowedCipherHostname", repository.getAllowedCipherHostname());
        PreferencesUtils.storeNotDef(prefs, "xdsForceMTOM", repository.isForceMTOM(), false);
    }

    @Override
    protected void loadChilds(Device device, Preferences deviceNode)
            throws BackingStoreException, ConfigurationException {
        if (!deviceNode.nodeExists(NODE_NAME))
            return;
        
        Preferences loggerNode = deviceNode.node(NODE_NAME);
        XdsRepository repository = new XdsRepository();
        loadFrom(repository, loggerNode);
        device.addDeviceExtension(repository);
    }

    private void loadFrom(XdsRepository repository, Preferences prefs) {
        repository.setApplicationName(prefs.get("xdsApplicationName", null));
        repository.setRepositoryUID(prefs.get("xdsRepositoryUID", null));
        repository.setRegistryURLs(PreferencesUtils.stringArray(prefs, "xdsRegistryURL"));
        repository.setAcceptedMimeTypes(PreferencesUtils.stringArray(prefs, "xdsAcceptedMimeTypes"));
        repository.setSoapLogDir(prefs.get("xdsSoapMsgLogDir", null));
        repository.setCheckMimetype(PreferencesUtils.booleanValue(prefs.get("xdsCheckMimetype", "false")));
        repository.setLogFullMessageHosts(PreferencesUtils.stringArray(prefs, "xdsLogFullMessageHosts"));
        repository.setAllowedCipherHostname(prefs.get("xdsAllowedCipherHostname", "*"));
        repository.setForceMTOM(PreferencesUtils.booleanValue(prefs.get("xdsForceMTOM", "false")));
    }

    @Override
    protected void mergeChilds(Device prev, Device device, Preferences deviceNode)
            throws BackingStoreException {
        XdsRepository prevRepository =
                prev.getDeviceExtension(XdsRepository.class);
        XdsRepository repository =
                device.getDeviceExtension(XdsRepository.class);
        if (repository == null && prevRepository == null)
            return;
        
        Preferences arrNode = deviceNode.node(NODE_NAME);
        if (repository == null)
            arrNode.removeNode();
        else if (prevRepository == null)
            storeTo(repository, arrNode);
        else
            storeDiffs(arrNode, prevRepository, repository);
    }

    private void storeDiffs(Preferences prefs, XdsRepository prevRepository, XdsRepository repository) {
        PreferencesUtils.storeDiff(prefs, "xdsApplicationName",
                prevRepository.getApplicationName(),
                repository.getApplicationName());
        PreferencesUtils.storeDiff(prefs, "xdsRepositoryUID",
                prevRepository.getRepositoryUID(),
                repository.getRepositoryUID());
        PreferencesUtils.storeDiff(prefs, "xdsRegistryURL",
                prevRepository.getRegistryURLs(),
                repository.getRegistryURLs());
        PreferencesUtils.storeDiff(prefs, "xdsAcceptedMimeTypes",
                prevRepository.getAcceptedMimeTypes(),
                repository.getAcceptedMimeTypes());
        PreferencesUtils.storeDiff(prefs, "xdsCheckMimetype",
                prevRepository.isCheckMimetype(),
                repository.isCheckMimetype());
        PreferencesUtils.storeDiff(prefs, "xdsSoapMsgLogDir",
                prevRepository.getSoapLogDir(),
                repository.getSoapLogDir());
        PreferencesUtils.storeDiff(prefs, "xdsLogFullMessageHosts",
                prevRepository.getLogFullMessageHosts(),
                repository.getLogFullMessageHosts());
        PreferencesUtils.storeDiff(prefs, "xdsAllowedCipherHostname",
                prevRepository.getAllowedCipherHostname(),
                repository.getAllowedCipherHostname());
        PreferencesUtils.storeDiff(prefs, "xdsForceMTOM",
                prevRepository.isForceMTOM(),
                repository.isForceMTOM());
    }
}
