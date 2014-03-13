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

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * A Patient ID of XDS Affinity Domain. 
 * 
 * This is the XAD PID (PID within the affinity domain) and the 
 * 
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Nov 02, 2011
 */
@NamedQueries({
    @NamedQuery(
        name="XADPatient.findByPIDandUniversalID",
        query="SELECT p FROM XADPatient p WHERE p.patientID = ?1 and p.issuerOfPatientID.universalID = ?2"),
    @NamedQuery(
        name="XADPatient.findByPID",
        query="SELECT p FROM XADPatient p WHERE p.patientID = ?1"),
    @NamedQuery(
        name="XADPatient.findByAffinityDomain",
        query="SELECT p FROM XADPatient p WHERE p.issuerOfPatientID.universalID LIKE ?1 ORDER BY p.issuerOfPatientID")
    })

@Entity
@Table(name = "xad_patient", uniqueConstraints=
    {@UniqueConstraint(columnNames = {"pat_id", "pat_id_issuer_fk"})})
public class XADPatient implements Serializable {
    private static final long serialVersionUID = 513457139488147710L;

    public static final String FIND_PATIENT_BY_PID = "XADPatient.findByPID";
    public static final String FIND_PATIENT_BY_PID_AND_UNIVERSAL_ID = "XADPatient.findByPIDandUniversalID";
    public static final String FIND_PATIENT_BY_AFFINITYDOMAIN = "XADPatient.findByAffinityDomain";

    @Id
    @GeneratedValue
    @Column(name = "pk")
    private int pk;
    
    @Basic(optional = false)
    @Column(name = "created_time", updatable = false)
    private Date createdTime;

    @Basic(optional = false)
    @Column(name = "updated_time")
    private Date updatedTime;

    @Basic(optional = false)
    @Column(name = "pat_id")
    //@Index(name="pat_id_idx")
    private String patientID;
    
    @Basic(optional = false)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pat_id_issuer_fk")
    private XADIssuer issuerOfPatientID;

    @Basic(optional = false)
    @JoinColumn(name = "linked_pat_fk")
    @ManyToOne
    private XADPatient linkedPatient;

    public XADPatient() {
    }

    public XADPatient(String pid) {
        setPatientIDWithAuthority(pid);
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

    public Date getCreatedTime() {
        return createdTime;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    /**
     * Return the Patient ID in XAD format <pat_id>^^^&<universal authority ID>&ISO
     * @return
     */
    public String getXADPatientID() {
        return patientID+"^^^"+issuerOfPatientID.getXADIssuer();
    }
    
    /**
     * Return the Patient ID in HL7 CX format <pat_id>^^^<local authority ID>&<universal authority ID>&ISO
     * @return
     */
    public String getCXPatientID() {
        return patientID+"^^^"+issuerOfPatientID.getCXIssuer();
    }
    
    private void setPatientIDWithAuthority(String pid) {
        int pos, pos1;
        if ( (pos = pid.indexOf('^')) == -1 || 
             (pos1 = pid.indexOf('^', pos+1)) == -1 || 
             (pos1 = pid.indexOf('^', pos1+1)) == -1) {
            throw new IllegalArgumentException("Missing Authority in patID! pid:"+pid);
        }            
        this.patientID = pid.substring(0, pos);
        String authority = pid.substring(++pos1);
        issuerOfPatientID = new XADIssuer(authority);
    }
    
    public String getPatientID() {
        return patientID;
    }

    public void setPatientID(String patientID) {
        if (patientID.indexOf('^') != -1 || patientID.indexOf('^') != -1) {
            throw new IllegalArgumentException("patientID must not contain '^' or '&'!");
        }
        this.patientID = patientID;
    }

    public XADIssuer getIssuerOfPatientID() {
        return issuerOfPatientID;
    }

    public void setIssuerOfPatientID(XADIssuer issuerOfPatientID) {
        this.issuerOfPatientID = issuerOfPatientID;
    }

    public XADPatient getLinkedPatient() {
        return linkedPatient;
    }

    public void setLinkedPatient(XADPatient linkedPatient) {
        this.linkedPatient = linkedPatient;
    }

    @Override
    public String toString() {
        return getXADPatientID();
    }
    
}
