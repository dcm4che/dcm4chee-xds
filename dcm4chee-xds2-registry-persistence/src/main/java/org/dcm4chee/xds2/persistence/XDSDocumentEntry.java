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
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa-Gevaert AG.
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See listed authors below.
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

package org.dcm4chee.xds2.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A specific ExtrinsicObject implementation for XDS DocumentEntry.
 * ExtrinsicObject with objectType='urn:uuid:7edca82f-054d-47f2-a032-9b2a5b5186c1'
 * 
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Nov 02, 2011
 */

@NamedQueries({
    @NamedQuery(
        name="XDSDocumentEntry.findByUniqueID",
        query="SELECT s FROM XDSDocumentEntry s WHERE s.uniqueId = ?1"),
    @NamedQuery(
        name="XDSDocumentEntry.findByUUID",
        query="SELECT s FROM XDSDocumentEntry s LEFT JOIN FETCH s.slots WHERE s.id = ?1"),
    @NamedQuery(
        name="XDSDocumentEntry.findByUniqueIDs",
        query="SELECT s FROM XDSDocumentEntry s WHERE s.uniqueId IN (:uniqueIds)"),
    @NamedQuery(
        name="XDSDocumentEntry.findByUUIDs",
        query="SELECT s FROM XDSDocumentEntry s WHERE s.id IN (:uuids)")
    })

@Entity
@DiscriminatorValue("XDSDocumentEntry")
public class XDSDocumentEntry extends ExtrinsicObject implements XDSObject {
    private static final long serialVersionUID = 513457139488147710L;

    public static final String FIND_BY_UNIQUE_ID = "XDSDocumentEntry.findByUniqueID";
    public static final String FIND_BY_UNIQUE_IDS = "XDSDocumentEntry.findByUniqueIDs";
    public static final String FIND_BY_UUID = "XDSDocumentEntry.findByUUID";
    public static final String FIND_BY_UUIDS = "XDSDocumentEntry.findByUUIDs";
    public static final String FIND_BY_UNIQUE_IDS_NAME = "uniqueIds";
    public static final String FIND_BY_UUIDS_NAME = "uuids";
    
    private static Logger log = LoggerFactory.getLogger(XDSDocumentEntry.class);

    private static final XDSSearchIndexKey[] THIS_INDEXES = { XDSSearchIndexKey.DOCUMENT_ENTRY_UNIQUE_ID,
        XDSSearchIndexKey.DOCUMENT_ENTRY_AUTHOR };
    
    @Override
    XDSSearchIndexKey[] getIndexes() {
        return THIS_INDEXES;
    }
    
    @Basic(optional = false)
    @Column(name = "unique_id")
    @Index(name="xds_uniqueId_idx")
    private String uniqueId;

    @Basic(optional = false)
    @JoinColumn(name = "patient_fk")
    @ManyToOne
    private XADPatient patient;

    @Basic(optional = false)
    @JoinColumn(name = "src_pat_fk")
    @ManyToOne
    private XADPatient sourcePatient;

    @ManyToMany
    @JoinTable(name = "xds_rel_document_code", 
        joinColumns = @JoinColumn(name = "document_fk", referencedColumnName = "pk"),
        inverseJoinColumns = @JoinColumn(name = "code_fk", referencedColumnName = "pk"))
    private Collection<XDSCode> xdsCodes;
    
    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public XADPatient getPatient() {
        return patient;
    }

    public void setPatient(XADPatient patient) {
        this.patient = patient;
    }
    
    public XADPatient getSourcePatient() {
        return sourcePatient;
    }

    public void setSourcePatient(XADPatient sourcePatient) {
        this.sourcePatient = sourcePatient;
    }

    public Collection<XDSCode> getXDSCodes() {
        return xdsCodes;
    }

    public void setXDSCodes(Collection<XDSCode> xdsCodes) {
        this.xdsCodes = xdsCodes;
    }

    public String getHash() {
        return getSlotValue("hash");
    }
    
    private String getSlotValue(String name) {
        List<String> values = getSlotValues(name);
        return values.isEmpty() ? null : values.get(0);
    }
    private List<String> getSlotValues(String name) {
        log.debug("#### get Slot values for '{}'", name);
        List<String> values = new ArrayList<String>();
        List<Slot> slots = getSlots();
        if (slots != null) {
            for (int i=0, len=slots.size() ; i < len ; i++) {
                if (name.equals(slots.get(i).getName())) {
                    values.add(slots.get(i).getValue());
                }
            }
        }
        log.debug("#### Values :{}", values);
        return values;
    }

}
