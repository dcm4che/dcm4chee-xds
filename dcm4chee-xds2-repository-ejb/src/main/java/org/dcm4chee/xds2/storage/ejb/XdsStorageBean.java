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

package org.dcm4chee.xds2.storage.ejb;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.dcm4chee.xds2.repository.persistence.XdsDocument;
import org.dcm4chee.xds2.repository.persistence.XdsFileRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
@Local (XdsStorageBeanLocal.class)
public class XdsStorageBean implements XdsStorageBeanLocal {

    @PersistenceContext(unitName = "dcm4chee-xds-storage")
    private EntityManager em;

    private static Logger log = LoggerFactory.getLogger(XdsStorageBean.class);

    public XdsStorageBean() {
    }

    @Override
    public XdsFileRef createFile(String groupID, String filesystemID, String docUID, String filePath, String mimetype, long size,
            String digest) {
        log.info("create File!");
        XdsDocument doc = findDocument(docUID);
        if (doc == null) {
            doc = createDocument(docUID, mimetype, size, digest);
        }
        XdsFileRef f = new XdsFileRef(groupID, filesystemID, filePath.replace(File.separatorChar, '/'), mimetype, size, digest, doc);
        em.persist(f);
        return f;
    }

    @Override
    public XdsDocument createDocument(String docUID, String mimetype, long size, String digest) {
        XdsDocument doc = new XdsDocument(docUID, mimetype, size, digest);
        em.persist(doc);
        return doc;
    }

    @Override
    public XdsDocument findDocument(String docUID) {
        Query qry = em.createNamedQuery(XdsDocument.FIND_BY_UID);
        qry.setParameter(1, docUID);
        try {
            return (XdsDocument) qry.getSingleResult();
        } catch (NoResultException x) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<XdsFileRef> findFileRefs(String docUID) {
        Query qry = em.createNamedQuery(XdsFileRef.FIND_BY_DOCUMENT_UID);
        qry.setParameter(1, docUID);
        return (List<XdsFileRef>) qry.getResultList();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<XdsFileRef> findFileRefs(List<String> docUIDs) {
        Query qry = em.createNamedQuery(XdsFileRef.FIND_BY_DOCUMENT_UIDS);
        qry.setParameter("docUIDs", docUIDs);
        return (List<XdsFileRef>) qry.getResultList();
    }

    @Override
    public List<XdsFileRef> deleteDocument(String... docUID) {
        return deleteDocument(Arrays.asList(docUID));
    }

    @Override
    public List<XdsFileRef> deleteDocument(List<String> docUIDs) {
        List<XdsFileRef> fileRefs = new ArrayList<XdsFileRef>();
        if (docUIDs.isEmpty())
            return fileRefs;
        Query qry = em.createNamedQuery(XdsDocument.FIND_BY_UIDS);
        qry.setParameter("docUIDs", docUIDs);
        @SuppressWarnings("unchecked")
        List<XdsDocument> docs = (List<XdsDocument>) qry.getResultList();
        List<XdsFileRef> tmp;
        for (XdsDocument doc : docs) {
            tmp = doc.getFileRefs();
            if (tmp != null)
                fileRefs.addAll(tmp);
            em.remove(doc);
        }
        return fileRefs;
    }

}
