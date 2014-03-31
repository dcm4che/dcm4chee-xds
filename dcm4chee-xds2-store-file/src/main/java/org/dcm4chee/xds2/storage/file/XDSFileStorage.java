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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.enterprise.context.ApplicationScoped;

import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.storage.XDSDocument;
import org.dcm4chee.xds2.storage.XDSStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class XDSFileStorage implements XDSStorage {
    private static final String DOCUMENT_CONTENT_FILENAME = "content";
    private static final String MIME_TYPE_FILENAME = "mimetype";
    //private static final String METADATA_FILE_NAME = "metadata.xml";
    private static final String HASH_FILENAME = "hash";
    private static final String UNKNOWN_MIME = "application/octet-stream";

    private static final int[] directoryTree = new int[]{347, 331, 317};

    private File baseDir;
    private boolean saveHash = true;
    
    private static Logger log = LoggerFactory.getLogger(XDSFileStorage.class);

    public XDSFileStorage() {
        setBaseDir("/xds/repository/store");
    }
    
    @Override
    public XDSDocument storeDocument(String docUID, byte[] content, String mime)
            throws XDSException, IOException {
        File docPath = getDocumentPath(docUID);
        if (docPath.exists()) {
            //TODO throw exception if hash is different!
            return null;
        }
        docPath.mkdirs();
        File docFile = new File(docPath, DOCUMENT_CONTENT_FILENAME);
        byte[] hash = writeFile(docFile, content, true);
        if (saveHash) {
            writeFileIgnoreError(new File(docPath, HASH_FILENAME), hash);
        }
        if (mime != null) {
            writeFileIgnoreError(new File(docPath, MIME_TYPE_FILENAME), mime.getBytes());
        }
        return new XDSDocument(docUID, mime, new DataHandler(new FileDataSource(docFile)), docFile.length(), hash);
    }

    @Override
    public XDSDocument retrieveDocument(String docUID) throws XDSException, IOException {
        File docPath = getDocumentPath(docUID);
        if (!docPath.exists()) {
            return null;
        }
        File docFile = new File(docPath, DOCUMENT_CONTENT_FILENAME);
        byte[] mime = this.readFileIgnoreError(new File(docPath, MIME_TYPE_FILENAME));
        byte[] hash = this.readFileIgnoreError(new File(docPath, HASH_FILENAME));
        String mimeType = mime == null ? UNKNOWN_MIME : new String(mime);
        return new XDSDocument(docUID, mimeType, new DataHandler(new FileDataSource(docFile)), docFile.length(), hash);
    }
    
    @Override
    public void commit(String[] docUIDs, boolean success) {
        for (int i = 0 ; i < docUIDs.length ; i++) {
            File docPath = getDocumentPath(docUIDs[i]);
            if (!success) {
                deleteFilesAndEmptyParents(docPath);
            }
        }
    }
    
    private void deleteFilesAndEmptyParents(File path) {
        try {
        	for (File f : path.listFiles()) {
        		if (!f.delete()) {
        			log.warn("Could not delete file:"+f);
        		}
        	}
        	while (path.list().length == 0) {
        		if (!path.delete()) {
        			log.warn("Could not delete empty directory");
        		}
        		path = path.getParentFile();
        	}
        } catch (Exception x) {
            log.debug("Failed to delete path "+path, x);
        }
    }
    

   public void setBaseDir(String dirName) {
        File dir = new File(dirName);
        if ( dir.isAbsolute() ) {
            baseDir = dir;
        } else {
            String serverHomeDir = System.getProperty("jboss.server.base.dir","/xds_store");
            baseDir = new File(serverHomeDir, dir.getPath());
        }
        if ( !baseDir.exists() ) {
            log.info("M-CREATE XDS Store base directory "+baseDir.getAbsolutePath());
            baseDir.mkdirs();
        }
    }
    
    public File getBaseDir() {
        return baseDir;
    }

    public boolean isSaveHash() {
        return saveHash;
    }

    public void setSaveHash(boolean saveHash) {
        this.saveHash = saveHash;
    }

    private byte[] writeFile(File f, byte[] content, boolean calcHash) throws IOException, XDSException {
        MessageDigest md = null;
        OutputStream out = null;
        if ( !f.exists() ) {
            log.debug("#### Write File:"+f);
            try {
                f.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(f);
                if (calcHash) {
                    md = newMessageDigest();
                    out = new DigestOutputStream(fos, md);
                } else {
                    out = fos;
                }
                out.write(content);
                log.debug("#### Document content written to file "+f);
            } finally {
                if ( out != null )
                    try {
                        out.close();
                    } catch (IOException ignore) {
                        log.error("Ignored error during close!",ignore);
                    }
            }
        }
        return md == null ? null : md.digest();
    }

    private void writeFileIgnoreError(File f, byte[] b) {
        try {
            writeFile(f, b, false);
        } catch (Exception ignore) {
            log.warn("Failed to write file! Ignored! file:"+f);
            log.debug("StackTrace:", ignore);
        }
    }
    
    private byte[] readFile(File f, MessageDigest md) throws IOException {
        InputStream is = null;
        try {
            FileInputStream fis = new FileInputStream(f);
            if (md == null) {
                is = fis;
            } else {
                is = new DigestInputStream(fis, md);
            }
            byte[] content = new byte[(int)f.length()];
            is.read(content);
            return content;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignore) {}
            }
        }

    }
    
    private byte[] readFileIgnoreError(File f) {
        try {
            return readFile(f, null);
        } catch (Exception ignore) {
            log.warn("Failed to read file! Ignored! file:"+f);
            log.debug("StackTrace:", ignore);
            return null;
        }
    }
    
    private File getDocumentPath(String docUid) {
        log.debug("getDocumentPath for "+docUid+" (baseDir:"+baseDir+")");
        return new File( baseDir, getFilepathForUID(docUid) );
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

    private MessageDigest newMessageDigest() throws XDSException {
        try {
            return MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException x) {
            log.warn("Failed to calc SHA1 hash! SHA1 Algorithm Unknown");
            throw new XDSException(XDSException.XDS_ERR_REPOSITORY_ERROR, 
                    "Unexpected error in XDS service !: Failed to calc SHA1 hash! SHA1 Algorithm unknown.", x);
        }
    }
}
