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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Index;


/**
 * Base class to identify objects by an id attribute and also provides attribute
 * extensibility by allowing dynamic, instance-specific attributes called Slots
 * 
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Nov 02, 2011
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="objtype", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("Identifiable")
public abstract class Identifiable implements Serializable {
    private static final long serialVersionUID = -354612904987140207L;

    @Id
    @GeneratedValue
    @Column(name = "pk")
    private long pk;
    
    @Column(name = "id", unique = true)
    @Index(name="id_idx")
    private String id;
    
    @Basic(optional = true)
    @Column(name = "home")
    private String home;
    
    @Basic(optional = true)
    @OneToMany(mappedBy="parent", cascade=CascadeType.ALL)
    private List<Slot> slots;
    

    public long getPk() {
        return pk;
    }
    
    /**
     * The id MUST be a valid URN and MUST be unique across all other RegistryObjects
     * @return
     */
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }

    /**
     * If present, MUST contain the base URL to the home registry.
     * @return
     */
    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    /**
     * serve as extensible attributes that MAY be defined for the Identifiable instance.
     * @return
     */
    public List<Slot> getSlots() {
        return slots;
    }

    public void setSlots(List<Slot> slots) {
        for (Slot slot : slots) {
            if (slot.getParent() == null) {
                slot.setParent(this);
            } else if (!slot.getParent().equals(this)) {
                throw new IllegalArgumentException("Slot with different parent! slot:"+slot);
            }
        }
        this.slots = slots;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (id == null || o == null || o.getClass() != getClass()) {
            return false;
        }
        return id.equals(((Identifiable) o).getId());
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
    
    @Override
    public String toString() {
        return this.getClass().getSimpleName()+":"+id;
    }
}
