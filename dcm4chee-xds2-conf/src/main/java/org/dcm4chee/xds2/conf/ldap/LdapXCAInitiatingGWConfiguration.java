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
import org.dcm4chee.xds2.conf.XCAInitiatingGWCfg;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Franz Willer <franz.willer@gmail.com>
 */
public class LdapXCAInitiatingGWConfiguration extends LdapDicomConfigurationExtension {
    private static final String CN_XCA_INITIATING_GW = "cn=XCAInitiatingGW,";

    @Override
    protected void storeChilds(String deviceDN, Device device) throws NamingException {
        XCAInitiatingGWCfg gw = device.getDeviceExtension(XCAInitiatingGWCfg.class);
        if (gw != null)
            store(deviceDN, gw);
    }

    private void store(String deviceDN, XCAInitiatingGWCfg rspGW)
            throws NamingException {
        config.createSubcontext(CN_XCA_INITIATING_GW + deviceDN,
                storeTo(rspGW, deviceDN, new BasicAttributes(true)));
    }

    private Attributes storeTo(XCAInitiatingGWCfg rspGW, String deviceDN, Attributes attrs) {
        attrs.put(new BasicAttribute("objectclass", "xcaRespondingGW"));
        LdapUtils.storeNotNull(attrs, "xdsApplicationName", rspGW.getApplicationName());
        LdapUtils.storeNotNull(attrs, "xdsHomeCommunityID", rspGW.getHomeCommunityID());
        LdapUtils.storeNotEmpty(attrs, "xdsRepositoryURL", rspGW.getRepositoryURLs());
        LdapUtils.storeNotEmpty(attrs, "xdsRespondingGatewayURL", rspGW.getRespondingGWURLs());
        LdapUtils.storeNotEmpty(attrs, "xdsRespondingGatewayRetrieveURL", rspGW.getRespondingGWRetrieveURLs());
        LdapUtils.storeNotNull(attrs, "xdsSoapMsgLogDir", rspGW.getSoapLogDir());
        LdapUtils.storeNotNull(attrs, "xdsRegistryURL", rspGW.getRegistryURL());
        LdapUtils.storeNotNull(attrs, "xdsAsync", rspGW.isAsync());
        LdapUtils.storeNotNull(attrs, "xdsAsyncHandler", rspGW.isAsyncHandler());
        return attrs;
    }

    @Override
    protected void loadChilds(Device device, String deviceDN)
            throws NamingException, ConfigurationException {
        Attributes attrs;
        try {
            attrs = config.getAttributes(CN_XCA_INITIATING_GW + deviceDN);
        } catch (NameNotFoundException e) {
            return;
        }
        XCAInitiatingGWCfg gw = new XCAInitiatingGWCfg();
        loadFrom(gw, attrs);
        device.addDeviceExtension(gw);
    }

    private void loadFrom(XCAInitiatingGWCfg rsp, Attributes attrs) throws NamingException {
        rsp.setApplicationName(LdapUtils.stringValue(attrs.get("xdsApplicationName"), null));
        rsp.setHomeCommunityID(LdapUtils.stringValue(attrs.get("xdsHomeCommunityID"), null));
        rsp.setRegistryURL(LdapUtils.stringValue(attrs.get("xdsRegistryURL"), null));
        rsp.setRepositoryURLs(LdapUtils.stringArray(attrs.get("xdsRepositoryURL")));
        rsp.setRespondingGWURLs(LdapUtils.stringArray(attrs.get("xdsRespondingGatewayURL")));
        rsp.setRespondingGWRetrieveURLs(LdapUtils.stringArray(attrs.get("xdsRespondingGatewayRetrieveURL")));
        rsp.setSoapLogDir(LdapUtils.stringValue(attrs.get("xdsSoapMsgLogDir"), null));
        rsp.setAsync(LdapUtils.booleanValue(attrs.get("xdsAsync"), false));
        rsp.setAsyncHandler(LdapUtils.booleanValue(attrs.get("xdsAsyncHandler"), false));
    }

    @Override
    protected void mergeChilds(Device prev, Device device, String deviceDN)
            throws NamingException {
        XCAInitiatingGWCfg prevGW = prev.getDeviceExtension(XCAInitiatingGWCfg.class);
        XCAInitiatingGWCfg gw = device.getDeviceExtension(XCAInitiatingGWCfg.class);
        if (gw == null) {
            if (prevGW != null)
                config.destroySubcontextWithChilds(CN_XCA_INITIATING_GW + deviceDN);
            return;
        }
        if (prevGW == null) {
            store(deviceDN, gw);
            return;
        }
        config.modifyAttributes(CN_XCA_INITIATING_GW + deviceDN,
                storeDiffs(prevGW, gw, deviceDN,
                        new ArrayList<ModificationItem>()));
    }

    private List<ModificationItem> storeDiffs(XCAInitiatingGWCfg prevGW, XCAInitiatingGWCfg gw,
            String deviceDN, ArrayList<ModificationItem> mods) {
        LdapUtils.storeDiff(mods, "xdsApplicationName",
                prevGW.getApplicationName(),
                gw.getApplicationName());
        LdapUtils.storeDiff(mods, "xdsRepositoryURL",
                prevGW.getRepositoryURLs(),
                gw.getRepositoryURLs());
        LdapUtils.storeDiff(mods, "xdsRespondingGatewayURL",
                prevGW.getRespondingGWURLs(),
                gw.getRespondingGWURLs());
        LdapUtils.storeDiff(mods, "xdsRespondingGatewayRetrieveURL",
                prevGW.getRespondingGWRetrieveURLs(),
                gw.getRespondingGWRetrieveURLs());
        LdapUtils.storeDiff(mods, "xdsRegistryURL",
                prevGW.getRegistryURL(),
                gw.getRegistryURL());
        LdapUtils.storeDiff(mods, "xdsHomeCommunityID",
                prevGW.getHomeCommunityID(),
                gw.getHomeCommunityID());
        LdapUtils.storeDiff(mods, "xdsSoapMsgLogDir",
                prevGW.getSoapLogDir(),
                gw.getSoapLogDir());
        LdapUtils.storeDiff(mods, "xdsAsync",
                prevGW.isAsync(),
                gw.isAsync());
        LdapUtils.storeDiff(mods, "xdsAsyncHandler",
                prevGW.isAsyncHandler(),
                gw.isAsyncHandler());
        return mods;
    }
}
