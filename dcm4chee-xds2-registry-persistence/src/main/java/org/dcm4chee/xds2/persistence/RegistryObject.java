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
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Index;

/**
 * The RegistryObject class extends the Identifiable class and serves as a common super class for most
 * classes in the information model.
 * 
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Nov 02, 2011
 */
@Entity
@DiscriminatorValue("RegistryObject")
public abstract class RegistryObject extends Identifiable implements Serializable {
    private static final long serialVersionUID = 513457139488147710L;

    @Basic(optional = false)
    @Column(name = "lid")
    private String lid;
    @Basic(optional = true)
    @Column(name = "objectType")
    @Index(name="objectType_idx")
    private String objectType;
    @Basic(optional = false)
    @Column(name = "status")
    @Index(name="status_idx")
    private String status;

    //Versioninfo (flattened: versionName, comment)
    @Basic(optional = true)
    @Column(name = "version_name", length=16)
    private String versionName;
    @Basic(optional = true)
    @Column(name = "comment1")
    private String comment;
    
    @Basic(optional = true)
    @OneToMany(mappedBy="classifiedObject", cascade=CascadeType.ALL)
    private Set<Classification> classifications;
    
    @Basic(optional = true)
    @OneToMany(mappedBy="parent", cascade=CascadeType.ALL)
    private Set<Description> description;

    @Basic(optional = true)
    @OneToMany(mappedBy="parent", cascade=CascadeType.ALL)
    private Set<Name> name;

    @Basic(optional = true)
    @OneToMany(mappedBy="registryObject", cascade=CascadeType.ALL)
    private Set<ExternalIdentifier> externalIdentifiers;
    
    /**
     * Each RegistryObject instance MUST have a lid (Logical Id) attribute . The lid is used to refer to a
     * logical RegistryObject in a version independent manner. All versions of a RegistryObject MUST have
     * the same value for the lid attribute. Note that this is in contrast with the id attribute that MUST be
     * unique for each version of the same logical RegistryObject. The lid attribute MAY be specified by the
     * submitter when creating the original version of a RegistryObject. If the submitter assigns the lid
     * attribute, she must guarantee that it is a globally unique URN. A registry MUST honor a valid submittersupplied
     * LID. If the submitter does not specify a LID then the registry MUST assign a LID and the value
     * of the LID attribute MUST be identical to the value of the id attribute of the first (originally created)
     * version of the logical RegistryObject.
     * 
     * @return
     */
    public String getLid() {
        return lid;
    }
    public void setLid(String lid) {
        this.lid = lid;
    }
    
    /**
     * Each RegistryObject instance has an objectType attribute. The value of the objectType attribute MUST
     * be a reference to a ClassificationNode in the canonical ObjectType ClassificationScheme. A Registry
     * MUST support the object types as defined by the ObjectType ClassificationScheme. The canonical
     * ObjectType ClassificationScheme may easily be extended by adding additional ClassificationNodes to
     * the canonical ObjectType ClassificationScheme.
     * The objectType for almost all objects in the information model matches the ClassificationNode that
     * corresponds to the name of their class. For example the objectType for a Classification is a reference to
     * the ClassificationNode with code 'Classification' in the canonical ObjectType ClassificationScheme.
     * The only exception to this rule is that the objectType for an ExtrinsicObject or an ExternalLink instance
     * MAY be defined by the submitter and indicates the type of content associated with that object.
     * A registry MUST set the correct objectType on a RegistryObject when returning it as a response to a
     * client request. A client MAY set the objectType on a RegistryObject when submitting the object. A
     * client SHOULD set the objectType when the object is an ExternalLink or an ExtrinsicObject since
     * content pointed to or described by these types may be of arbitrary objectType.
     * 
     * @return
     */
    public String getObjectType() {
        return objectType;
    }
    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }
    
    /**
     * Each RegistryObject instance has an objectType attribute. The value of the objectType attribute MUST
     * be a reference to a ClassificationNode in the canonical ObjectType ClassificationScheme. A Registry
     * MUST support the object types as defined by the ObjectType ClassificationScheme. The canonical
     * ObjectType ClassificationScheme may easily be extended by adding additional ClassificationNodes to
     * the canonical ObjectType ClassificationScheme.
     * The objectType for almost all objects in the information model matches the ClassificationNode that
     * corresponds to the name of their class. For example the objectType for a Classification is a reference to
     * the ClassificationNode with code 'Classification' in the canonical ObjectType ClassificationScheme.
     * The only exception to this rule is that the objectType for an ExtrinsicObject or an ExternalLink instance
     * MAY be defined by the submitter and indicates the type of content associated with that object.
     * A registry MUST set the correct objectType on a RegistryObject when returning it as a response to a
     * client request. A client MAY set the objectType on a RegistryObject when submitting the object. A
     * client SHOULD set the objectType when the object is an ExternalLink or an ExtrinsicObject since
     * content pointed to or described by these types may be of arbitrary objectType.
     * 
     * @return
     */
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * VerionInfo (flattened: VersionName, Comment)
     * Each RegistryObject instance MAY have a versionInfo attribute. The value of the versionInfo attribute
     * MUST be of type VersionInfo. The versionInfo attribute provides information about the specific version
     * of a RegistryObject. The versionInfo attribute is set by the registry.
     * 
     * @return
     */
    public String getVersionName() {
        return versionName;
    }
    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }
    /**
     * VerionInfo (flattened: VersionName, Comment)
     * @return
     */
    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    /**
     * Each RegistryObject instance MAY have a human readable name. The name does not need to be
     * unique with respect to other RegistryObject instances. This attribute is I18N capable and therefore of
     * type InternationalString
     * 
     * @return
     */
    public Set<Name> getName() {
        return name;
    }
    public void setName(Set<Name> name) {
        this.name = name;
    }
    
    /**
     * Each RegistryObject instance MAY have a Set of zero or more Classification instances that are
     * composed within the RegistryObject. These Classification instances classify the RegistryObject.
     * 
     * @return
     */
    public Set<Classification> getClassifications() {
        return classifications;
    }
    public void setClassifications(Set<Classification> classifications) {
        this.classifications = classifications;
    }
    
    /**
     * Each RegistryObject instance MAY have textual description in a human readable and user-friendly
     * form. This attribute is I18N capable and therefore of type InternationalString.
     * 
     * @param description
     */
    public void setDescription(Set<Description> description) {
        this.description = description;
    }
    public Set<Description> getDescription() {
        return description;
    }
    
    /**
     * Each RegistryObject instance MAY have a Set of zero or more ExternalIdentifier instances that are
     * composed within the RegistryObject. These ExternalIdentifier instances serve as alternate identifiers
     * for the RegistryObject.
     * 
     * @param externalIdentifiers
     */
    public void setExternalIdentifiers(Set<ExternalIdentifier> externalIdentifiers) {
        this.externalIdentifiers = externalIdentifiers;
    }
    public Set<ExternalIdentifier> getExternalIdentifiers() {
        return externalIdentifiers;
    }
}
