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
import org.dcm4chee.xds2.conf.XCAiRespondingGWCfg;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Franz Willer <franz.willer@gmail.com>
 *
 */
public class PreferencesXCAiRespondingGWConfiguration
        extends PreferencesDicomConfigurationExtension {

    private static final String NODE_NAME = "xcaiRespondingGW";

    @Override
    protected void storeChilds(Device device, Preferences deviceNode) {
        XCAiRespondingGWCfg rspGW =
                device.getDeviceExtension(XCAiRespondingGWCfg.class);
        if (rspGW != null)
            storeTo(rspGW, deviceNode.node(NODE_NAME));
    }

    private void storeTo(XCAiRespondingGWCfg rspGW, Preferences prefs) {
        PreferencesUtils.storeNotNull(prefs, "xdsApplicationName", rspGW.getApplicationName());
        PreferencesUtils.storeNotEmpty(prefs, "xdsiSourceURL", rspGW.getXDSiSourceURLs());
        PreferencesUtils.storeNotNull(prefs, "xdsHomeCommunityID", rspGW.getHomeCommunityID());
        PreferencesUtils.storeNotNull(prefs, "xdsSoapMsgLogDir", rspGW.getSoapLogDir());
    }

    @Override
    protected void loadChilds(Device device, Preferences deviceNode)
            throws BackingStoreException, ConfigurationException {
        if (!deviceNode.nodeExists(NODE_NAME))
            return;
        
        Preferences loggerNode = deviceNode.node(NODE_NAME);
        XCAiRespondingGWCfg rspGW = new XCAiRespondingGWCfg();
        loadFrom(rspGW, loggerNode);
        device.addDeviceExtension(rspGW);
    }

    private void loadFrom(XCAiRespondingGWCfg rspGW, Preferences prefs) {
        rspGW.setApplicationName(prefs.get("xdsApplicationName", null));
        rspGW.setHomeCommunityID(prefs.get("xdsHomeCommunityID", null));
        rspGW.setXDSiSourceURLs(PreferencesUtils.stringArray(prefs, "xdsiSourceURL"));
        rspGW.setSoapLogDir(prefs.get("xdsSoapMsgLogDir", null));
    }

    @Override
    protected void mergeChilds(Device prev, Device device, Preferences deviceNode)
            throws BackingStoreException {
        XCAiRespondingGWCfg prevRspGW =
                prev.getDeviceExtension(XCAiRespondingGWCfg.class);
        XCAiRespondingGWCfg rspGW =
                device.getDeviceExtension(XCAiRespondingGWCfg.class);
        if (rspGW == null && prevRspGW == null)
            return;
        
        Preferences arrNode = deviceNode.node(NODE_NAME);
        if (rspGW == null)
            arrNode.removeNode();
        else if (prevRspGW == null)
            storeTo(rspGW, arrNode);
        else
            storeDiffs(arrNode, prevRspGW, rspGW);
    }

    private void storeDiffs(Preferences prefs, XCAiRespondingGWCfg prevRspGW, XCAiRespondingGWCfg rspGW) {
        PreferencesUtils.storeDiff(prefs, "xdsApplicationName",
                prevRspGW.getApplicationName(),
                rspGW.getApplicationName());
        PreferencesUtils.storeDiff(prefs, "xdsiSourceURL",
                prevRspGW.getXDSiSourceURLs(),
                rspGW.getXDSiSourceURLs());
        PreferencesUtils.storeDiff(prefs, "xdsHomeCommunityID",
                prevRspGW.getHomeCommunityID(),
                rspGW.getHomeCommunityID());
        PreferencesUtils.storeDiff(prefs, "xdsSoapMsgLogDir",
                prevRspGW.getSoapLogDir(),
                rspGW.getSoapLogDir());
    }
}
