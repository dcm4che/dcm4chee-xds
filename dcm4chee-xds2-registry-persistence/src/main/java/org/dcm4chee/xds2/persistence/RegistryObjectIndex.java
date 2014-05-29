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
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.dcm4chee.xds2.persistence.RegistryObject.XDSSearchIndexKey;
import org.hibernate.annotations.Index;


/**
 * An entity that holds data extracted from RegistryObject's blobs into key/value pairs that are used to perform queries.
 * 
 * @Entity
 * @author Roman K
 *
 */
@Entity
@Table(name="xds_searchTable")
public class RegistryObjectIndex implements Serializable {


    private static final long serialVersionUID = -2887627435710299659L;

    @Id
    @GeneratedValue
    @Column(name = "pk")
    private long pk;
    
    @ManyToOne
    @JoinColumn(name = "registry_object_fk")    
    private RegistryObject subject;

    @Basic
    @Column(name = "search_key")
    @Enumerated(EnumType.STRING)
    private XDSSearchIndexKey key;
    
    @Basic
    @Column(name = "value")
    private String value;

    public long getPk() {
        return pk;
    }

    public void setPk(long pk) {
        this.pk = pk;
    }

    public RegistryObject getSubject() {
        return subject;
    }

    public void setSubject(RegistryObject subject) {
        this.subject = subject;
    }

    public XDSSearchIndexKey getKey() {
        return key;
    }

    public void setKey(XDSSearchIndexKey key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    @Override
    public boolean equals(Object x) {
        
        if (x == null) return false;
        if (!x.getClass().equals(RegistryObjectIndex.class)) return false;

        RegistryObjectIndex arg0 = (RegistryObjectIndex) x;
        
        // exclude pk from comparison
        return (getKey().equals(arg0.getKey()) && 
                getValue().equals(arg0.getValue()) && 
                getSubject().equals(arg0.getSubject()));
    }
    
    @Override
    public String toString() {
        return String.format("RegObjIndex - subj:%s | pk: %s | key:%s | value:%s", getSubject().getId(),getPk(), getKey(), getValue());
    }
    
    @Override
    public int hashCode() {
        return getSubject().hashCode()+getValue().hashCode()*10+getKey().ordinal();
    }
    
}
