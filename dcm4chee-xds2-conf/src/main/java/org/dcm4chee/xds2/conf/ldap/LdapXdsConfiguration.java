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
import java.util.Properties;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;

import org.dcm4che.conf.api.ConfigurationException;
import org.dcm4che.conf.ldap.hl7.LdapHL7Configuration;
import org.dcm4che.net.Device;
import org.dcm4chee.xds2.conf.XdsApplication;
import org.dcm4chee.xds2.conf.XdsConfiguration;
import org.dcm4chee.xds2.conf.XdsDevice;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Franz Willer <franz.willer@gmail.com>
 */
public class LdapXdsConfiguration extends LdapHL7Configuration implements XdsConfiguration {

    private static final String XDS_CONFIGURATION = "XDS Configuration";
    public LdapXdsConfiguration() throws ConfigurationException {
        super();
        setConfigurationCN(XDS_CONFIGURATION);
    }

    public LdapXdsConfiguration(Properties p) throws ConfigurationException {
        super(p);
        setConfigurationCN(XDS_CONFIGURATION);
    }

    @Override
    public XdsApplication findXdsApplication(String name)
            throws ConfigurationException {
        XdsApplication app = (XdsApplication) ((XdsDevice) findDevice(
                "(&(objectclass=xdsApplication)(xdsApplicationName=" + name + "))", name))
            .getXdsApplication(name);
        if (app instanceof XdsApplication) {
            return (XdsApplication) app;
        } else {
            return null;
        }
    }

    @Override
    protected void loadChilds(Device device, String deviceDN) throws NamingException, ConfigurationException {
        super.loadChilds(device, deviceDN);
        if (!(device instanceof XdsDevice))
            return;
        loadXdsApplications((XdsDevice) device, deviceDN);
    }

    private void loadXdsApplications(XdsDevice device, String deviceDN) throws NamingException {
        NamingEnumeration<SearchResult> ne = search(deviceDN, "(objectclass=xdsApplication)");
        try {
            while (ne.hasMore()) {
                device.addXdsApplication(
                        loadXdsApplication(ne.next(), deviceDN, device));
            }
        } finally {
           safeClose(ne);
        }
    }
    
    protected XdsApplication loadXdsApplication(SearchResult sr, String deviceDN,
            XdsDevice device) throws NamingException {
        Attributes attrs = sr.getAttributes();
        XdsApplication xdsApp = newXdsApplication(attrs);
        loadFrom(xdsApp, attrs);
        loadChilds(xdsApp, sr.getNameInNamespace());
        return xdsApp;
    }

    protected void loadChilds(XdsApplication xdsApp, String xdsAppDN) throws NamingException {
    }
    
    @Override
    protected Device newDevice(Attributes attrs) throws NamingException {
        return new XdsDevice(stringValue(attrs.get("dicomDeviceName"), null));
    }

    protected XdsApplication newXdsApplication(Attributes attrs) throws NamingException {
        return new XdsApplication(stringValue(attrs.get("xdsApplicationName"), null));
    }
    
    protected void loadFrom(XdsApplication xdsApp, Attributes attrs) throws NamingException {
        xdsApp.setAffinityDomain(stringValue(attrs.get("xdsAffinityDomain"), null));
        xdsApp.setAcceptedMimeTypes(stringArray(attrs.get("xdsAcceptedMimeTypes")));
        xdsApp.setSoapLogDir(stringValue(attrs.get("xdsSoapMsgLogDir"), null));
        xdsApp.setCreateMissingPIDs(booleanValue(attrs.get("xdsCreateMissingPIDs"), false));
        xdsApp.setCreateMissingCodes(booleanValue(attrs.get("xdsCreateMissingCodes"), false));
        xdsApp.setDontSaveCodeClassifications(booleanValue(attrs.get("xdsDontSaveCodeClassifications"), false));
    }

    protected Attribute objectClassesOf(XdsApplication xdsApp, Attribute attr) {
        attr.add("xdsApplication");
        return attr;
    }
    
    @Override
    protected void storeChilds(String deviceDN, Device device) throws NamingException {
        super.storeChilds(deviceDN, device);
        if (!(device instanceof XdsDevice))
            return;
        XdsDevice xdsDev = (XdsDevice) device;
        for (XdsApplication xdsApp : xdsDev.getXdsApplications()) {
            String appDN = xdsAppDN(xdsApp.getApplicationName(), deviceDN);
            createSubcontext(appDN, storeTo(xdsApp, deviceDN, new BasicAttributes(true)));
            storeChilds(appDN, xdsApp);
        }
    }

    protected void storeChilds(String appDN, XdsApplication hl7App) {
    }

    private String xdsAppDN(String name, String deviceDN) {
        return dnOf("xdsApplicationName" , name, deviceDN);
    }

    protected Attributes storeTo(XdsApplication xdsApp, String deviceDN, Attributes attrs) {
        attrs.put(objectClassesOf(xdsApp, new BasicAttribute("objectclass")));
        storeNotNull(attrs, "xdsApplicationName", xdsApp.getApplicationName());
        storeNotNull(attrs, "xdsAffinityDomain", xdsApp.getAffinityDomain());
        storeNotEmpty(attrs, "xdsAcceptedMimeTypes", xdsApp.getAcceptedMimeTypes());
        storeNotNull(attrs, "xdsSoapMsgLogDir", xdsApp.getSoapLogDir());
        storeNotDef(attrs, "xdsCreateMissingPIDs", xdsApp.isCreateMissingPIDs(), false);
        storeNotDef(attrs, "xdsCreateMissingCodes", xdsApp.isCreateMissingPIDs(), false);
        return attrs;
    }

    @Override
    protected void mergeChilds(Device prev, Device device, String deviceDN)
            throws NamingException {
        super.mergeChilds(prev, device, deviceDN);
        if (!(prev instanceof XdsDevice && device instanceof XdsDevice))
            return;

        mergeXDsApps((XdsDevice) prev, (XdsDevice) device, deviceDN);
    }

    private void mergeXDsApps(XdsDevice prevDev, XdsDevice dev, String deviceDN)
            throws NamingException {
        for (XdsApplication ae : prevDev.getXdsApplications()) {
            String aet = ae.getApplicationName();
            if (dev.getXdsApplication(aet) == null)
                destroySubcontextWithChilds(xdsAppDN(aet, deviceDN));
        }
        for (XdsApplication ae : dev.getXdsApplications()) {
            String aet = ae.getApplicationName();
            XdsApplication prevAE = prevDev.getXdsApplication(aet);
            if (prevAE == null) {
                String aeDN = xdsAppDN(ae.getApplicationName(), deviceDN);
                createSubcontext(aeDN,
                        storeTo(ae, deviceDN, new BasicAttributes(true)));
                storeChilds(aeDN, ae);
            } else
                merge(prevAE, ae, deviceDN);
        }
    }

    private void merge(XdsApplication prev, XdsApplication app, String deviceDN)
            throws NamingException {
        String appDN = xdsAppDN(app.getApplicationName(), deviceDN);
        modifyAttributes(appDN, storeDiffs(prev, app, deviceDN, 
                new ArrayList<ModificationItem>()));
        mergeChilds(prev, app, appDN);
    }

    protected void mergeChilds(XdsApplication prev, XdsApplication app, String appDN) {
    }

    protected List<ModificationItem> storeDiffs(XdsApplication a, XdsApplication b,
            String deviceDN, List<ModificationItem> mods) {
        storeDiff(mods, "xdsAffinityDomain",
                a.getAffinityDomain(),
                b.getAffinityDomain());
        storeDiff(mods, "xdsAcceptedMimeTypes",
                a.getAcceptedMimeTypes(),
                b.getAcceptedMimeTypes());
        storeDiff(mods, "xdsSoapMsgLogDir",
                a.getSoapLogDir(),
                b.getSoapLogDir());
        storeDiff(mods, "xdsCreateMissingPIDs",
                a.isCreateMissingPIDs(),
                b.isCreateMissingPIDs());
        storeDiff(mods, "xdsCreateMissingCodes",
                a.isCreateMissingCodes(),
                b.isCreateMissingCodes());
        return mods;
    }
}
