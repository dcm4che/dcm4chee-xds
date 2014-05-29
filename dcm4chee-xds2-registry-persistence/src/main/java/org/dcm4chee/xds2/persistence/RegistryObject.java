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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.jxpath.JXPathContext;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.rim.ClassificationType;
import org.dcm4chee.xds2.infoset.rim.ExternalIdentifierType;
import org.dcm4chee.xds2.infoset.rim.InternationalStringType;
import org.dcm4chee.xds2.infoset.rim.ObjectFactory;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectListType;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectType;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.infoset.rim.VersionInfoType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The RegistryObject class extends the Identifiable class and serves as a
 * common super class for most classes in the information model.
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
    public static final Map<XDSSearchIndexKey,String> INDEX_XPATHS;
    private static final boolean FORCE_REINDEX = true;
    
    /**
     * Searchtable keys for which index can be enabled.<br/>
     * Defined in a single location to prevent name clashes, and to use enum binding for a field in the searchtable
     * @author Roman K
     *
     */
    public static enum XDSSearchIndexKey {
        
        DOCUMENT_ENTRY_AUTHOR,

        SUBMISSION_SET_AUTHOR,
        SUBMISSION_SET_SOURCE_ID,
        
    }
    static {
        INDEX_XPATHS = new HashMap<RegistryObject.XDSSearchIndexKey, String>();

        // Document Entry's Author
        INDEX_XPATHS.put(XDSSearchIndexKey.DOCUMENT_ENTRY_AUTHOR,
                String.format("classification[classificationScheme='%s']/slot[name='%s']/valueList/value" ,XDSConstants.UUID_XDSDocumentEntry_author, XDSConstants.SLOT_NAME_AUTHOR_PERSON));
        
        // Submission Set's Author
        INDEX_XPATHS.put(XDSSearchIndexKey.SUBMISSION_SET_AUTHOR,
                String.format("classification[classificationScheme='%s']/slot[name='%s']/valueList/value" ,XDSConstants.UUID_XDSSubmissionSet_autor, XDSConstants.SLOT_NAME_AUTHOR_PERSON));

        // Submission Set's Source ID
        INDEX_XPATHS.put(XDSSearchIndexKey.SUBMISSION_SET_SOURCE_ID,
                String.format("externalIdentifier[identificationScheme='%s']/value", XDSConstants.UUID_XDSSubmissionSet_sourceId));
    }
    
    /**
     * This method should be overridden by subclasses to specify which indexes are used for them
     */
    XDSSearchIndexKey[] getIndexes() {
        return null;
    }
    
    
    // Un/marshallers are not thread-safe, but are very expensive to create (~0.1 sec per un/marshaller), so
    // threadlocal it is

    public static ThreadLocal<Marshaller> marshallerThreadLocal = new ThreadLocal<Marshaller>() {
        @Override
        protected Marshaller initialValue() {
            try {
                return JAXBContext.newInstance(RegistryObjectListType.class).createMarshaller();
            } catch (JAXBException e) {
                throw new RuntimeException("Unable to create Marshaller for RegistryObjectType", e);
            }
        }
    };

    public static ThreadLocal<Unmarshaller> unmarshallerThreadLocal = new ThreadLocal<Unmarshaller>() {
        @Override
        protected Unmarshaller initialValue() {
            try {
                return JAXBContext.newInstance(RegistryObjectListType.class).createUnmarshaller();
            } catch (JAXBException e) {
                throw new RuntimeException("Unable to create Unmarshaller for RegistryObjectType", e);
            }
        }
    };

    @Transient
    private byte[] blobXml;

    @Basic(optional = false)
    @Column(name = "lid")
    private String lid;

    @Basic(optional = true)
    @Column(name = "objectType")
    @Index(name = "xds_objectType_idx")
    private String objectType;

    @Basic(optional = false)
    @Column(name = "status")
    @Index(name = "xds_status_idx")
    private String status;

    // Versioninfo (flattened: versionName, comment)
    @Basic(optional = true)
    @Column(name = "version_name", length = 16)
    private String versionName;
    @Basic(optional = true)
    @Column(name = "comment1")
    private String comment;
    
    @Transient
    private Set<RegistryObjectIndex> currIndexedValues = new HashSet<RegistryObjectIndex>();

    /**
     * The deserialized blob singleton  
     */
    @Transient
    private RegistryObjectType fullObject;

    /**
     * The blob 
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "xmlBlob")
    @Access(AccessType.PROPERTY)
    public byte[] getXml() throws JAXBException {
        log.debug("getXml called (id {})", getId());

        // if fullObject was not used - no need to serialize it
        if (fullObject == null)
            return blobXml;
    
        // if fullObject was initialized, we have to serialize it to persist any
        // changes that could have been made
        log.debug("Marshalling fullObject in getXml (id {})", getId());
        ByteArrayOutputStream xmlStream = new ByteArrayOutputStream();
        marshallerThreadLocal.get().marshal((new ObjectFactory()).createRegistryObject(fullObject), xmlStream);
        byte[] xml = xmlStream.toByteArray();
    
        return xml;
    }

    public void setXml(byte[] xml) throws JAXBException {
        log.debug("setXml called (id {})", getId());
        blobXml = xml;
    }

    
    /**
     * Indexed properties that are used in queries. 
     * Do not use the setter - the update is fully seamless - when object is merged/persisted - indexes are re/-created and updated if needed  
     * @return
     */
    @OneToMany(mappedBy="subject", cascade=CascadeType.ALL, orphanRemoval=true)
    @Access(AccessType.PROPERTY)
    @SuppressWarnings("unchecked")
    public Set<RegistryObjectIndex> getIndexedValues() {
        log.debug("getIndexedValues called for object with id {}", getId());

        // TODO: OPTIMIZATION - if marshalling is fast - can check whether the object has already changed first
        
        // if fullObject was not initialized - nothing has changed and we could just return old value 
        // (except if reindexing is forced)
        if (fullObject == null && !FORCE_REINDEX) return currIndexedValues;
        
        if (getIndexes() == null) return currIndexedValues;
        
        // validate/update searchIndex table
        // iterate over all enabled indexes
        Set<RegistryObjectIndex> newIndexValues = new HashSet<RegistryObjectIndex>();
        for (XDSSearchIndexKey key : getIndexes()) {
            // run xpath expr on fullobject
            JXPathContext context = JXPathContext.newContext(getFullObject());
            Iterator<String> valueIterator = (Iterator<String>) context.iterate(INDEX_XPATHS.get(key));
            
            // add to newIndexValues
            while (valueIterator.hasNext()) {
                RegistryObjectIndex ind = new RegistryObjectIndex();
                ind.setSubject(this);
                ind.setKey(key);
                ind.setValue((String) valueIterator.next());
                newIndexValues.add(ind);
            }
        }
        
        // Retain what we have there already, and add new ones.
        // Note thats retain makes use of a custom equals for RegistryObjectIndex that does not consider the pk.
        currIndexedValues.retainAll(newIndexValues); 
        currIndexedValues.addAll(newIndexValues);
        
        return currIndexedValues;
    }

    public void setIndexedValues(Set<RegistryObjectIndex> indexedValues) {
        
        this.currIndexedValues = indexedValues;
    }
    
    // TODO:DB_RESTRUCT override sub setters to save into blob!

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

    
    @Override
    public void setHome(String home) {
        super.setHome(home);
        getFullObject().setHome(home);
    }
    
    @Override
    public void setId(String id) {
        super.setId(id);
        getFullObject().setId(id);
    }
    
    @Override
    public void setSlotTypes(List<SlotType1> slotTs) {
        super.setSlotTypes(slotTs);
        getFullObject().getSlot().clear();
        getFullObject().getSlot().addAll(slotTs);
    }
    
    
    /**
     * Each RegistryObject instance MUST have a lid (Logical Id) attribute . The
     * lid is used to refer to a logical RegistryObject in a version independent
     * manner. All versions of a RegistryObject MUST have the same value for the
     * lid attribute. Note that this is in contrast with the id attribute that
     * MUST be unique for each version of the same logical RegistryObject. The
     * lid attribute MAY be specified by the submitter when creating the
     * original version of a RegistryObject. If the submitter assigns the lid
     * attribute, she must guarantee that it is a globally unique URN. A
     * registry MUST honor a valid submittersupplied LID. If the submitter does
     * not specify a LID then the registry MUST assign a LID and the value of
     * the LID attribute MUST be identical to the value of the id attribute of
     * the first (originally created) version of the logical RegistryObject.
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
     * Each RegistryObject instance has an objectType attribute. The value of
     * the objectType attribute MUST be a reference to a ClassificationNode in
     * the canonical ObjectType ClassificationScheme. A Registry MUST support
     * the object types as defined by the ObjectType ClassificationScheme. The
     * canonical ObjectType ClassificationScheme may easily be extended by
     * adding additional ClassificationNodes to the canonical ObjectType
     * ClassificationScheme. The objectType for almost all objects in the
     * information model matches the ClassificationNode that corresponds to the
     * name of their class. For example the objectType for a Classification is a
     * reference to the ClassificationNode with code 'Classification' in the
     * canonical ObjectType ClassificationScheme. The only exception to this
     * rule is that the objectType for an ExtrinsicObject or an ExternalLink
     * instance MAY be defined by the submitter and indicates the type of
     * content associated with that object. A registry MUST set the correct
     * objectType on a RegistryObject when returning it as a response to a
     * client request. A client MAY set the objectType on a RegistryObject when
     * submitting the object. A client SHOULD set the objectType when the object
     * is an ExternalLink or an ExtrinsicObject since content pointed to or
     * described by these types may be of arbitrary objectType.
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
     * Each RegistryObject instance has an objectType attribute. The value of
     * the objectType attribute MUST be a reference to a ClassificationNode in
     * the canonical ObjectType ClassificationScheme. A Registry MUST support
     * the object types as defined by the ObjectType ClassificationScheme. The
     * canonical ObjectType ClassificationScheme may easily be extended by
     * adding additional ClassificationNodes to the canonical ObjectType
     * ClassificationScheme. The objectType for almost all objects in the
     * information model matches the ClassificationNode that corresponds to the
     * name of their class. For example the objectType for a Classification is a
     * reference to the ClassificationNode with code 'Classification' in the
     * canonical ObjectType ClassificationScheme. The only exception to this
     * rule is that the objectType for an ExtrinsicObject or an ExternalLink
     * instance MAY be defined by the submitter and indicates the type of
     * content associated with that object. A registry MUST set the correct
     * objectType on a RegistryObject when returning it as a response to a
     * client request. A client MAY set the objectType on a RegistryObject when
     * submitting the object. A client SHOULD set the objectType when the object
     * is an ExternalLink or an ExtrinsicObject since content pointed to or
     * described by these types may be of arbitrary objectType.
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
     * VerionInfo (flattened: VersionName, Comment) Each RegistryObject instance
     * MAY have a versionInfo attribute. The value of the versionInfo attribute
     * MUST be of type VersionInfo. The versionInfo attribute provides
     * information about the specific version of a RegistryObject. The
     * versionInfo attribute is set by the registry.
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
     * 
     * @return
     */
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
        getVersionInfo().setComment(comment);
    }

    
    /**
     * Will lazily fetch the blob from the DB, in not done already. If the
     * returned object is changed, the changes will be persisted in case the
     * entity is persisted!
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public synchronized RegistryObjectType getFullObject() {
        log.debug("requested singleton  (id {})", getId());

        // singleton way
        if (fullObject != null)
            return fullObject;

        log.debug("no singleton RegistryObjectType, trying to fetch db (id {})", getId());

        // if not created yet, try to fetch from db
        try {
            byte[] xml = getXml();
            if (xml != null) {
                log.debug("Unmarshalling RegistryObjectType ... (id {})", getId());
                ByteArrayInputStream is = new ByteArrayInputStream(xml);
                fullObject = ((JAXBElement<RegistryObjectType>) unmarshallerThreadLocal.get().unmarshal(is)).getValue();
                return fullObject;
            }
        } catch (JAXBException e) {
            // Exception is used since if there is an issue with unmarshalling,
            // but the blob exists, then an empty fullobject is created and it
            // will replace the blob - so data is lost
            throw new RuntimeException("Error while unmarshalling a RegistryObjectType with id " + getId(), e);
        }

        log.debug("no blob from db, creating new RegistryObjectType (id {})", getId());

        // create new
        fullObject = (new ObjectFactory()).createRegistryObjectType();
        return fullObject;

    }

}
