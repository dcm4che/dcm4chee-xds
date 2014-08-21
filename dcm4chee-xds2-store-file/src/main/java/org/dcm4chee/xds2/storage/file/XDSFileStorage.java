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

package org.dcm4chee.xds2.storage.file;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.net.Device;
import org.dcm4chee.storage.conf.Storage;
import org.dcm4chee.storage.service.StorageResult;
import org.dcm4chee.storage.service.StorageService;
import org.dcm4chee.storage.service.TimeAndUidPathFormat;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.repository.persistence.XdsDocument;
import org.dcm4chee.xds2.repository.persistence.XdsFileRef;
import org.dcm4chee.xds2.storage.XDSDocument;
import org.dcm4chee.xds2.storage.XDSStorage;
import org.dcm4chee.xds2.storage.ejb.XdsStorageBeanLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class XDSFileStorage implements XDSStorage {
    private static final String DEF_STORAGE_DEVICE_NAME ="dcm4chee-storage";
    private static final String STORAGE_DEVICE_NAME_PROPERTY = "org.dcm4chee.xds.storage.deviceName";
    private static final String DEFAULT_ONLINE_GROUP = "XDS_ONLINE";

    private static final int[] directoryTree = new int[]{347, 331, 317};

    private Device device;

    private static Logger log = LoggerFactory.getLogger(XDSFileStorage.class);

    public XDSFileStorage() {
    }

    @Inject
    private StorageService storage;

    @Inject @Named("deviceNameProperty")
    private String xdsDeviceNameProperty;

    @Inject
    @Storage    
    private DicomConfiguration conf;

    @EJB
    private XdsStorageBeanLocal ejb;

    @Produces
    @Storage
    public Device getDevice() {
        if (device==null) {
            try {
                device = this.findDevice();
            } catch (ConfigurationException x) {
                log.error("Provide Storage Device failed!", x);
                return null;
            }
        }
        System.out.println("##############\n###############\n###############\nDevice:"+device);
        return device;
    }

    @Produces
    @Storage
    public Format getPathFormat() {
        return new TimeAndUidPathFormat(true);
    }
    @Override
    public XDSDocument storeDocument(String groupID, String docUID, byte[] content, String mime)
            throws XDSException, IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            XdsDocument doc = ejb.findDocument(docUID);
            ByteArrayInputStream is = new ByteArrayInputStream(content);
            if (doc == null) {
                StorageResult r = storage.store(new ByteArrayInputStream(content), 
                        groupID == null ? DEFAULT_ONLINE_GROUP : groupID, docUID, md);
                if (r.errorMsg != null) {
                    throw new XDSException(XDSException.XDS_ERR_REPOSITORY_OUT_OF_RESOURCES, r.errorMsg, null);
                }
                XdsFileRef f = ejb.createFile(groupID, r.filesystem.getId(), docUID, r.filePath.toString(), mime, r.size, r.hash);
                return new XDSDocument(docUID, mime, new DataHandler(new FileDataSource(storage.toPath(f).toFile())), 
                        f.getFileSize(), f.getDigest());
            } else {
                log.info("Document {} is already stored on this repository!", doc);
                if (!storage.checkDigest(is, doc.getDigest(), md)) {
                    throw new XDSException(XDSException.XDS_ERR_NON_IDENTICAL_HASH, 
                            "DocumentEntry uniqueId:"+docUID+" already exists but has different hash value! registerDocument skipped!", null);
                }
                return new XDSDocument(docUID, mime, null, doc.getSize(), doc.getDigest()).commit();
            }
        } catch (NoSuchAlgorithmException | ConfigurationException x) {
            throw new XDSException(XDSException.XDS_ERR_REPOSITORY_ERROR, "Unexpected error in XDS service !: "+x.getMessage(), x);
        }
    }

    @Override
    public XDSDocument retrieveDocument(String docUID) throws XDSException, IOException {
        List<XdsFileRef> XdsFileRefs = ejb.findFileRefs(docUID);
        if (XdsFileRefs.size() == 0) {
            return null;
        }
        XdsFileRef f = XdsFileRefs.get(0);
        return new XDSDocument(docUID, f.getMimetype(), new DataHandler(new FileDataSource(storage.toPath(f).toFile())), 
                f.getFileSize(), f.getDigest());
    }

    @Override
    public void commit(XDSDocument[] docs, boolean success) {
        if (!success) {
            ArrayList<String> docUIDs = new ArrayList<String>();
            for (XDSDocument doc : docs) {
                if (doc != null && !doc.isCommitted()) {
                    docUIDs.add(doc.getUID());
                }
            }
            if (docUIDs.size() > 0) {
                List<XdsFileRef> fileRefs = ejb.findFileRefs(docUIDs);
                for (XdsFileRef f : fileRefs) {
                    storage.deleteFileAndParentDirectories(storage.toPath(f));
                }
            }
        }
    }


    public static String getFilepathForUID(String uid) {
        if (directoryTree == null) 
            return uid;
        StringBuffer sb = new StringBuffer();
        int hash = uid.hashCode();
        int modulo;
        for (int i = 0; i < directoryTree.length; i++) {
            if (directoryTree[i] == 0) {
                sb.append(Integer.toHexString(hash)).append(File.separatorChar);
            } else {
                modulo = hash % directoryTree[i];
                if (modulo < 0) {
                    modulo *= -1;
                }
                sb.append(modulo).append(File.separatorChar);
            }
        }
        sb.append(uid);
        return sb.toString();
    }

    private Device findDevice() throws ConfigurationException {
        String deviceName = System.getProperty(STORAGE_DEVICE_NAME_PROPERTY, 
                System.getProperty(xdsDeviceNameProperty, DEF_STORAGE_DEVICE_NAME));
        return conf.findDevice(deviceName);
    }

}
