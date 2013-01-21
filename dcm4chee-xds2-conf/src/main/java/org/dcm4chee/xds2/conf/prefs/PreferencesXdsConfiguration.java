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

package org.dcm4chee.xds2.conf.prefs;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.dcm4che.conf.api.ConfigurationException;
import org.dcm4che.conf.prefs.hl7.PreferencesHL7Configuration;
import org.dcm4che.net.Device;
import org.dcm4chee.xds2.conf.XdsApplication;
import org.dcm4chee.xds2.conf.XdsConfiguration;
import org.dcm4chee.xds2.conf.XdsDevice;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Franz Willer <franz.willer@gmail.com>
 */
public class PreferencesXdsConfiguration extends PreferencesHL7Configuration implements XdsConfiguration {

    public PreferencesXdsConfiguration() {
    }

    public PreferencesXdsConfiguration(Preferences rootPrefs) {
        super(rootPrefs);
    }
    
    @Override
    public synchronized XdsApplication findXdsApplication(String name) throws ConfigurationException {
        return ((XdsDevice) findDevice("xdsApplication", name))
            .getXdsApplication(name);
    }

    @Override
    protected Device newDevice(Preferences deviceNode) {
         return new XdsDevice(deviceNode.name());
    }

    protected XdsApplication newXdsApplication(Preferences appNode) {
        return new XdsApplication(appNode.name());
    }

    @Override
    protected void storeChilds(Device device, Preferences deviceNode) {
        super.storeChilds(device, deviceNode);
        if (!(device instanceof XdsDevice))
            return;
        XdsDevice xdsDev = (XdsDevice) device;
        Preferences parent = deviceNode.node("xdsApplication");
        for (XdsApplication xdsApp : xdsDev.getXdsApplications()) {
            Preferences appNode = parent.node(xdsApp.getApplicationName());
            storeTo(xdsApp, appNode);
            storeChilds(xdsApp, appNode);
        }
    }

    protected void storeTo(XdsApplication xdsApp, Preferences prefs) {
        storeNotNull(prefs, "xdsApplicationName", xdsApp.getApplicationName());
        storeNotNull(prefs, "xdsAffinityDomain", xdsApp.getAffinityDomain());
        storeNotEmpty(prefs, "xdsAcceptedMimeTypes", xdsApp.getAcceptedMimeTypes());
        storeNotNull(prefs, "xdsSoapMsgLogDir", xdsApp.getSoapLogDir());
        storeNotDef(prefs, "xdsCreateMissingPIDs", xdsApp.isCreateMissingPIDs(), false);
        storeNotDef(prefs, "xdsCreateMissingCodes", xdsApp.isCreateMissingPIDs(), false);
        
    }

    protected void storeChilds(XdsApplication xdsApp, Preferences appNode) {
    }

    @Override
    protected void loadChilds(Device device, Preferences deviceNode)
            throws BackingStoreException, ConfigurationException {
        super.loadChilds(device, deviceNode);
        if (!(device instanceof XdsDevice))
            return;

        loadXdsApplications((XdsDevice) device, deviceNode);
    }

    private void loadXdsApplications(XdsDevice xdsDev, Preferences deviceNode)
            throws BackingStoreException {
        Preferences appsNode = deviceNode.node("xdsApplication");
        for (String appName : appsNode.childrenNames()) {
            Preferences appNode = appsNode.node(appName);
            XdsApplication xdsApp = newXdsApplication(appNode);
            loadFrom(xdsApp, appNode);
            loadChilds(xdsApp, appNode);
            xdsDev.addXdsApplication(xdsApp);
        }
    }

    protected void loadFrom(XdsApplication xdsApp, Preferences prefs) {
        xdsApp.setAffinityDomain(prefs.get("xdsAffinityDomain",null));
        xdsApp.setAcceptedMimeTypes(stringArray(prefs, "xdsAcceptedMimeTypes"));
        xdsApp.setSoapLogDir(prefs.get("xdsSoapMsgLogDir", null));
        xdsApp.setCreateMissingPIDs(booleanValue(prefs.get("xdsCreateMissingPIDs", "FALSE")));
        xdsApp.setCreateMissingCodes(booleanValue(prefs.get("xdsCreateMissingCodes", "FALSE")));
        xdsApp.setDontSaveCodeClassifications(booleanValue(prefs.get("xdsDontSaveCodeClassifications", "FALSE")));
    }

    protected void loadChilds(XdsApplication hl7app, Preferences appNode) {
    }

    @Override
    protected void mergeChilds(Device prev, Device device,
            Preferences devicePrefs) throws BackingStoreException {
        super.mergeChilds(prev, device, devicePrefs);
        if (!(prev instanceof XdsDevice && device instanceof XdsDevice))
            return;

        mergeXdsApps((XdsDevice) prev, (XdsDevice) device, devicePrefs);
    }

    private void mergeXdsApps(XdsDevice prevDev, XdsDevice dev, Preferences deviceNode)
            throws BackingStoreException {
        Preferences appsNode = deviceNode.node("xdsApplication");
        for (XdsApplication app : prevDev.getXdsApplications()) {
            String appName = app.getApplicationName();
            boolean b = dev.getXdsApplication(appName) == null;
            if (b) {
                appsNode.node(appName).removeNode();
            }
        }
        for (XdsApplication app : dev.getXdsApplications()) {
            String appName = app.getApplicationName();
            XdsApplication prevApp = prevDev.getXdsApplication(appName);
            Preferences appNode = appsNode.node(appName);
            if (prevApp == null) {
                storeTo(app, appNode);
                storeChilds(app, appNode);
            } else {
                storeDiffs(appNode, prevApp, app);
                mergeChilds(prevApp, app, appNode);
            }
        }
    }

    protected void storeDiffs(Preferences prefs, XdsApplication a, XdsApplication b) {
        storeDiff(prefs, "xdsAffinityDomain",
                a.getAffinityDomain(),
                b.getAffinityDomain());
        storeDiff(prefs, "xdsAcceptedMimeTypes",
                a.getAcceptedMimeTypes(),
                b.getAcceptedMimeTypes());
        storeDiff(prefs, "xdsSoapMsgLogDir",
                a.getSoapLogDir(),
                b.getSoapLogDir());
        storeDiff(prefs, "xdsCreateMissingPIDs",
                a.isCreateMissingPIDs(),
                b.isCreateMissingPIDs());
        storeDiff(prefs, "xdsCreateMissingCodes",
                a.isCreateMissingCodes(),
                b.isCreateMissingCodes());
    }

    protected void mergeChilds(XdsApplication prev, XdsApplication app,
            Preferences appNode) {
    }


}
