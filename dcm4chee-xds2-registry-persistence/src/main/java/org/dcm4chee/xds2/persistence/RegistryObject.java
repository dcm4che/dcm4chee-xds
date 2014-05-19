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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.Transient;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.dcm4chee.xds2.infoset.rim.ClassificationType;
import org.dcm4chee.xds2.infoset.rim.ExternalIdentifierType;
import org.dcm4chee.xds2.infoset.rim.InternationalStringType;
import org.dcm4chee.xds2.infoset.rim.ObjectFactory;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectType;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.infoset.rim.VersionInfoType;
import org.hibernate.annotations.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static Logger log = LoggerFactory.getLogger(RegistryObject.class);
    
    
    /**
     * The blob singleton
     */
    @Transient
    private RegistryObjectType fullObject;

   
    
    @Basic(optional = false)
    @Column(name = "lid")
    private String lid;
    
    
    @Basic(optional = true)
    @Column(name = "objectType")
    @Index(name="xds_objectType_idx")
    private String objectType;
    
    
    @Basic(optional = false)
    @Column(name = "status")
    @Index(name="xds_status_idx")
    private String status;

    //Versioninfo (flattened: versionName, comment)
    @Basic(optional = true)
    @Column(name = "version_name", length=16)
    private String versionName;
    @Basic(optional = true)
    @Column(name = "comment1")
    private String comment;
    
    // TODO:DB_RESTRUCT override other setters to save into blob! 

    /* These getters/setter pull the data from the blob singleton */
    
    public Collection<ClassificationType> getClassifications() {
        return getFullObject().getClassification();
    }

    public InternationalStringType getDescription() {
        return getFullObject().getDescription();
    }
    public void setDescription(InternationalStringType description) {
        getFullObject().setDescription(description);
    }
    
    public InternationalStringType getName() {
        return getFullObject().getName();
    }
    
    public void setName(InternationalStringType name) {
        getFullObject().setName(name);
    }
    
    public Collection<ExternalIdentifierType> getExternalIdentifiers() {
        return getFullObject().getExternalIdentifier();
    }
    
    
    
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
        getFullObject().setLid(lid);
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
        getFullObject().setObjectType(objectType);
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
        getFullObject().setStatus(status);
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
        getVersionInfo().setVersionName(versionName);
    }
    
    
    private synchronized VersionInfoType getVersionInfo() {
        if (getFullObject().getVersionInfo() == null)
            getFullObject().setVersionInfo((new ObjectFactory()).createVersionInfoType());
        return getFullObject().getVersionInfo();
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
        getVersionInfo().setComment(comment);
    }
    
   
    @Basic(fetch=FetchType.LAZY)
    @Column(name = "xmlBlob")    
    @Lob
    @Access(AccessType.PROPERTY)
    public byte[] getXml() throws JAXBException {
        log.debug("getXml called");
        if (fullObject == null) return null;
        
        Marshaller m = JAXBContext.newInstance(RegistryObjectType.class).createMarshaller();
        ByteArrayOutputStream xmlStream = new ByteArrayOutputStream();
        m.marshal((new ObjectFactory()).createRegistryObject(fullObject), xmlStream);
        byte[] xml = xmlStream.toByteArray();
        return xml;
         
    }
    @SuppressWarnings("unchecked")
    public void setXml(byte[] xml) throws JAXBException {
        log.debug("setXml called");
        Unmarshaller um = JAXBContext.newInstance(SubmitObjectsRequest.class).createUnmarshaller();
        ByteArrayInputStream is = new ByteArrayInputStream(xml);
        fullObject = ((JAXBElement<RegistryObjectType>) um.unmarshal(is)).getValue();
    }
    
    /**
     * Will lazily fetch the blob from the DB, in not done already. If the returned object is changed, 
     * the changes will be persisted in case the entity is persisted!
     * @return
     */
    public RegistryObjectType getFullObject() {

        // singleton way 
        if (fullObject != null) return fullObject;

        log.debug("no singleton RegistryObjectType, trying to fetch db");

        // if not created yet, try to fetch from db
        try {
            getXml();
        } catch (JAXBException e) {
            log.warn("Error while marshalling a RegistryObjectType with id "+getId(),e);
        }
        
        if (fullObject != null) return fullObject;

        log.debug("no blob from db, creating new RegistryObjectType");
        
        // create new
        fullObject = (new ObjectFactory()).createRegistryObjectType();
        return fullObject;
        
    }
    
    
    
}
