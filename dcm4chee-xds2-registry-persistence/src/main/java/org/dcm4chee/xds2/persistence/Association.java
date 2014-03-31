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

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;


/**
 * Association instances are used to define many-to-many associations among RegistryObjects in the
 * information model.
 * An instance of the Association class represents an association between two RegistryObjects.
 * 
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Nov 02, 2011
 */
@NamedQueries({
    @NamedQuery(
        name="Association.findBySourceObject",
        query="SELECT a FROM Association a WHERE a.sourceObject = ?1"),
    @NamedQuery(
        name="Association.findByTargetObject",
        query="SELECT a FROM Association a WHERE a.targetObject = ?1"),
    @NamedQuery(
        name="Association.findBySourceOrTargetObject",
        query="SELECT a FROM Association a WHERE a.sourceObject = ?1 OR a.targetObject = ?1")
    })

@Entity
@DiscriminatorValue("Association")
public class Association extends RegistryObject implements Serializable {
    private static final long serialVersionUID = 513457139488147710L;

    public static final String FIND_BY_SOURCE_OBJECT = "Association.findBySourceObject";
    public static final String FIND_BY_TARGET_OBJECT = "Association.findByTargetObject";
    public static final String FIND_BY_SOURCE_OR_TARGET = "Association.findBySourceOrTargetObject";
    
    @OneToOne
    @JoinColumn(name = "assocType_classNode_fk")
    private ClassificationNode assocType;
    
    @OneToOne
    @JoinColumn(name = "assoc_source_fk")
    private RegistryObject sourceObject;
    @OneToOne
    @JoinColumn(name = "assoc_target_fk")
    private RegistryObject targetObject;
    
    /**
     * Each Association MUST have an associationType attribute that identifies the type of that association.
     * The value of the associationType attribute MUST be a reference to a ClassificationNode within the
     * canonical AssociationType ClassificationScheme. While the AssociationType scheme MAY easily be
     * extended, a Registry MUST support the canonical association types as defined by the canonical
     * AssociationType ClassificationScheme.
     * 
     * @return
     */
    public ClassificationNode getAssocType() {
        return assocType;
    }
    public void setAssocType(ClassificationNode assocType) {
        this.assocType = assocType;
    }
    
    /**
     * Each Association MUST have a sourceObject attribute that references the RegistryObject instance that
     * is the source of that Association.
     * 
     * @return
     */
    public RegistryObject getSourceObject() {
        return sourceObject;
    }
    public void setSourceObject(RegistryObject sourceObject) {
        this.sourceObject = sourceObject;
    }
    
    /**
     * Each Association MUST have a targetObject attribute that references the RegistryObject instance that
     * is the target of that Association.
     * 
     * @return
     */
    public RegistryObject getTargetObject() {
        return targetObject;
    }
    public void setTargetObject(RegistryObject targetObject) {
        this.targetObject = targetObject;
    }
    
    @Override
    public String toString() {
        return "Association:"+getId()+" type:"+getAssocType().getId()+
            " src:"+getSourceObject().getId()+" target:"+getTargetObject().getId();
    }
}
