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

package org.dcm4chee.xds2.repository.persistence;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@NamedQueries({
    @NamedQuery(
        name="XdsDocument.findByUID",
        query="SELECT d FROM XdsDocument d WHERE d.uid = ?1"),
    @NamedQuery(
            name="XdsDocument.findByUIDs",
            query="SELECT d FROM XdsDocument d WHERE d.uid IN (:docUIDs)")
    })

@Entity
@Table(name = "xds_document")
public class XdsDocument implements Serializable {

	private static final long serialVersionUID = 1070787830873350147L;

    public static final String FIND_BY_UID = "XdsDocument.findByUID";
    public static final String FIND_BY_UIDS = "XdsDocument.findByUIDs";

	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "pk")
    private long pk;

    @Basic(optional = false)
    @Column(name = "uid", updatable = false)
    private String uid;

    @Basic(optional = false)
    @Column(name = "created_time", updatable = false)
    private Date createdTime;

    @Basic(optional = false)
    @Column(name = "updated_time", updatable = true)
    private Date updatedTime;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "object_fk")
    private List<XdsFileRef> fileRefs;

    @Basic(optional = false)
    @Column(name = "mimetype", updatable = false)
    private String mimetype;

    @Basic(optional = false)
    @Column(name = "size", updatable = false)
    private long size;

    @Basic(optional = false)
    @Column(name = "digest", updatable = false)
    private String digest;

    public XdsDocument() {};

    public XdsDocument(String uid, String mimetype, long docSize, String digest) {
        this.uid = uid;
    	this.mimetype = mimetype;
        this.size = docSize;
        this.digest = digest;
    }

    @PrePersist
    public void onPrePersist() {
        Date now = new Date();
        createdTime = now;
        updatedTime = now;
    }
    
    @PreUpdate
    public void onPreUpdate() {
        updatedTime = new Date();
    }

    public long getPk() {
        return pk;
    }

    public String getUid() {
		return uid;
	}

	public Date getCreatedTime() {
        return createdTime;
    }

    public Date getUpdatedTime() {
		return updatedTime;
	}

	public String getMimetype() {
        return mimetype;
    }

    public long getSize() {
        return size;
    }


    public String getDigest() {
		return digest;
	}

	public List<XdsFileRef> getFileRefs() {
		return fileRefs;
	}

	public void setFileRefs(List<XdsFileRef> fileRefs) {
		this.fileRefs = fileRefs;
	}

	@Override
    public String toString() {
        return "XdsDocument[pk=" + pk
                + ", uid=" + uid
                + ", mimetype=" + mimetype
                + ", size=" + size
                + "]";
    }
}
