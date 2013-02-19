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
import org.dcm4chee.xds2.conf.XdsRepository;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Franz Willer <franz.willer@gmail.com>
 */
public class LdapXDSRepositoryConfiguration extends LdapDicomConfigurationExtension {
    private static final String CN_XDS_REPOSITORY = "cn=XDSRepository,";

    @Override
    protected void storeChilds(String deviceDN, Device device) throws NamingException {
        XdsRepository repository = device.getDeviceExtension(XdsRepository.class);
        if (repository != null)
            store(deviceDN, repository);
    }

    private void store(String deviceDN, XdsRepository repository)
            throws NamingException {
        config.createSubcontext(CN_XDS_REPOSITORY + deviceDN,
                storeTo(repository, deviceDN, new BasicAttributes(true)));
    }

    private Attributes storeTo(XdsRepository repository, String deviceDN, Attributes attrs) {
        attrs.put(new BasicAttribute("objectclass", "xdsRepository"));
        LdapUtils.storeNotNull(attrs, "xdsApplicationName", repository.getApplicationName());
        LdapUtils.storeNotNull(attrs, "xdsRepositoryUID", repository.getRepositoryUID());
        LdapUtils.storeNotEmpty(attrs, "xdsRegistryURL", repository.getRegistryURLs());
        LdapUtils.storeNotEmpty(attrs, "xdsAcceptedMimeTypes", repository.getAcceptedMimeTypes());
        LdapUtils.storeNotNull(attrs, "xdsSoapMsgLogDir", repository.getSoapLogDir());
        System.out.println("#####################attrs:"+attrs);
        return attrs;
    }

    @Override
    protected void loadChilds(Device device, String deviceDN)
            throws NamingException, ConfigurationException {
        Attributes attrs;
        try {
            attrs = config.getAttributes(CN_XDS_REPOSITORY + deviceDN);
        } catch (NameNotFoundException e) {
            return;
        }
        XdsRepository repository = new XdsRepository();
        loadFrom(repository, attrs);
        device.addDeviceExtension(repository);
    }

    private void loadFrom(XdsRepository repository, Attributes attrs) throws NamingException {
        repository.setApplicationName(LdapUtils.stringValue(attrs.get("xdsApplicationName"), null));
        repository.setRepositoryUID(LdapUtils.stringValue(attrs.get("xdsRepositoryUID"), null));
        repository.setRegistryURLs(LdapUtils.stringArray(attrs.get("xdsRegistryURL")));
        repository.setAcceptedMimeTypes(LdapUtils.stringArray(attrs.get("xdsAcceptedMimeTypes")));
        repository.setSoapLogDir(LdapUtils.stringValue(attrs.get("xdsSoapMsgLogDir"), null));
    }

    @Override
    protected void mergeChilds(Device prev, Device device, String deviceDN)
            throws NamingException {
        XdsRepository prevRepository = prev.getDeviceExtension(XdsRepository.class);
        XdsRepository repository = device.getDeviceExtension(XdsRepository.class);
        if (repository == null) {
            if (prevRepository != null)
                config.destroySubcontextWithChilds(CN_XDS_REPOSITORY + deviceDN);
            return;
        }
        if (prevRepository == null) {
            store(deviceDN, repository);
            return;
        }
        config.modifyAttributes(CN_XDS_REPOSITORY + deviceDN,
                storeDiffs(prevRepository, repository, deviceDN,
                        new ArrayList<ModificationItem>()));
    }

    private List<ModificationItem> storeDiffs(XdsRepository prevRepository, XdsRepository repository,
            String deviceDN, ArrayList<ModificationItem> mods) {
        LdapUtils.storeDiff(mods, "xdsApplicationName",
                prevRepository.getApplicationName(),
                repository.getApplicationName());
        LdapUtils.storeDiff(mods, "xdsRepositoryUID",
                prevRepository.getRepositoryUID(),
                repository.getRepositoryUID());
        LdapUtils.storeDiff(mods, "xdsRegistryURL",
                prevRepository.getRegistryURLs(),
                repository.getRegistryURLs());
        LdapUtils.storeDiff(mods, "xdsAcceptedMimeTypes",
                prevRepository.getAcceptedMimeTypes(),
                repository.getAcceptedMimeTypes());
        LdapUtils.storeDiff(mods, "xdsSoapMsgLogDir",
                prevRepository.getSoapLogDir(),
                repository.getSoapLogDir());
        return mods;
    }
}
