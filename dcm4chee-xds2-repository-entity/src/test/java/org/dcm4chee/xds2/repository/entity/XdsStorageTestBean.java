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

package org.dcm4chee.xds2.repository.entity;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.dcm4chee.storage.entity.Availability;
import org.dcm4chee.storage.entity.FileSystem;
import org.dcm4chee.storage.entity.FileSystemStatus;
import org.dcm4chee.xds2.repository.persistence.XdsFileRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
@Local (XdsStorageTestBeanLocal.class)
public class XdsStorageTestBean implements XdsStorageTestBeanLocal {

    @PersistenceContext(unitName = "dcm4chee-xds2-storage")
    private EntityManager em;

    private static Logger log = LoggerFactory.getLogger(XdsStorageTestBean.class);
    
    public XdsStorageTestBean() {
    }

	@Override
	public FileSystem createFileSystem(String grpName, String fileURL,
			Availability availability, FileSystemStatus status) {
		log.info("create Filesystem!");
        FileSystem fs = new FileSystem();
        fs.setGroupID(grpName);
        fs.setURI(fileURL);
        fs.setAvailability(availability);
        fs.setStatus(status);
        em.persist(fs);
        return fs;
	}

	@Override
	public XdsFileRef createFile(String filePath, String mimetype, long size,
			String digest, FileSystem fs) {
		log.info("create File!");
		XdsFileRef f = new XdsFileRef(fs, filePath, mimetype, size, digest, null);
		em.persist(f);
		return f;
	}

	@Override
	public void deleteFsGroup(String grpName) {
		FileSystem fs = (FileSystem) em.createQuery("SELECT OBJECT(f) from FileSystem f WHERE f.groupID = :grp").setParameter("grp", grpName).getSingleResult();
		em.createQuery("DELETE from XdsFileRef f WHERE f.fileSystem = :fs").setParameter("fs", fs).executeUpdate();
		em.remove(fs);
		//em.createQuery("UPDATE XdsFileRef f SET f.fileSystem = null WHERE f.fileSystem = :fs").setParameter("fs", fs).executeUpdate();
		//em.createQuery("DELETE from FileSystem f WHERE f.groupID = :grp").setParameter("grp", grpName).executeUpdate();
	}

	@Override
	public void linkFileSystems(FileSystem... chain) {
		if (chain.length == 1) {
			chain[0].setNextFileSystem(null);
			em.merge(chain[0]);
		} else {
			FileSystem fs;
			for (int i = 0, len = chain.length - 1 ; i < len ; ) {
				fs = chain[i];
				fs.setNextFileSystem(chain[++i]);
				em.merge(fs);
			}
		}
	}
    
    
    
}
