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

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.ldap.LdapDicomConfigurationExtension;
import org.dcm4che3.conf.ldap.LdapUtils;
import org.dcm4che3.net.Device;
import org.dcm4chee.xds2.conf.XCARespondingGWCfg;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Franz Willer <franz.willer@gmail.com>
 */
public class LdapXCARespondingGWConfiguration extends LdapDicomConfigurationExtension {
    private static final String CN_XCA_RESPONDING_GW = "cn=XCARespondingGW,";

    @Override
    protected void storeChilds(String deviceDN, Device device) throws NamingException {
        XCARespondingGWCfg rspGW = device.getDeviceExtension(XCARespondingGWCfg.class);
        if (rspGW != null)
            store(deviceDN, rspGW);
    }

    private void store(String deviceDN, XCARespondingGWCfg rspGW)
            throws NamingException {
        config.createSubcontext(CN_XCA_RESPONDING_GW + deviceDN,
                storeTo(rspGW, deviceDN, new BasicAttributes(true)));
    }

    private Attributes storeTo(XCARespondingGWCfg rspGW, String deviceDN, Attributes attrs) {
        attrs.put(new BasicAttribute("objectclass", "xcaRespondingGW"));
        LdapUtils.storeNotNull(attrs, "xdsApplicationName", rspGW.getApplicationName());
        LdapUtils.storeNotNull(attrs, "xdsHomeCommunityID", rspGW.getHomeCommunityID());
        LdapUtils.storeNotEmpty(attrs, "xdsRepositoryURL", rspGW.getRepositoryURLs());
        LdapUtils.storeNotNull(attrs, "xdsSoapMsgLogDir", rspGW.getSoapLogDir());
        LdapUtils.storeNotNull(attrs, "xdsRegistryURL", rspGW.getRegistryURL());
        return attrs;
    }

    @Override
    protected void loadChilds(Device device, String deviceDN)
            throws NamingException, ConfigurationException {
        Attributes attrs;
        try {
            attrs = config.getAttributes(CN_XCA_RESPONDING_GW + deviceDN);
        } catch (NameNotFoundException e) {
            return;
        }
        XCARespondingGWCfg rspGW = new XCARespondingGWCfg();
        loadFrom(rspGW, attrs);
        device.addDeviceExtension(rspGW);
    }

    private void loadFrom(XCARespondingGWCfg rspGW, Attributes attrs) throws NamingException {
        rspGW.setApplicationName(LdapUtils.stringValue(attrs.get("xdsApplicationName"), null));
        rspGW.setHomeCommunityID(LdapUtils.stringValue(attrs.get("xdsHomeCommunityID"), null));
        rspGW.setRegistryURL(LdapUtils.stringValue(attrs.get("xdsRegistryURL"), null));
        rspGW.setRepositoryURLs(LdapUtils.stringArray(attrs.get("xdsRepositoryURL")));
        rspGW.setSoapLogDir(LdapUtils.stringValue(attrs.get("xdsSoapMsgLogDir"), null));
    }

    @Override
    protected void mergeChilds(Device prev, Device device, String deviceDN)
            throws NamingException {
        XCARespondingGWCfg prevRspGW = prev.getDeviceExtension(XCARespondingGWCfg.class);
        XCARespondingGWCfg rspGW = device.getDeviceExtension(XCARespondingGWCfg.class);
        if (rspGW == null) {
            if (prevRspGW != null)
                config.destroySubcontextWithChilds(CN_XCA_RESPONDING_GW + deviceDN);
            return;
        }
        if (prevRspGW == null) {
            store(deviceDN, rspGW);
            return;
        }
        config.modifyAttributes(CN_XCA_RESPONDING_GW + deviceDN,
                storeDiffs(prevRspGW, rspGW, deviceDN,
                        new ArrayList<ModificationItem>()));
    }

    private List<ModificationItem> storeDiffs(XCARespondingGWCfg prevRspGW, XCARespondingGWCfg rspGW,
            String deviceDN, ArrayList<ModificationItem> mods) {
        LdapUtils.storeDiff(mods, "xdsApplicationName",
                prevRspGW.getApplicationName(),
                rspGW.getApplicationName());
        LdapUtils.storeDiff(mods, "xdsRepositoryURL",
                prevRspGW.getRepositoryURLs(),
                rspGW.getRepositoryURLs());
        LdapUtils.storeDiff(mods, "xdsRegistryURL",
                prevRspGW.getRegistryURL(),
                rspGW.getRegistryURL());
        LdapUtils.storeDiff(mods, "xdsHomeCommunityID",
                prevRspGW.getHomeCommunityID(),
                rspGW.getHomeCommunityID());
        LdapUtils.storeDiff(mods, "xdsSoapMsgLogDir",
                prevRspGW.getSoapLogDir(),
                rspGW.getSoapLogDir());
        return mods;
    }
}
