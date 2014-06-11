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

package org.dcm4chee.xds2.registry.ws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.JAXBElement;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.Pointer;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.conf.XdsRegistry;
import org.dcm4chee.xds2.infoset.rim.AssociationType1;
import org.dcm4chee.xds2.infoset.rim.ClassificationNodeType;
import org.dcm4chee.xds2.infoset.rim.ClassificationSchemeType;
import org.dcm4chee.xds2.infoset.rim.ClassificationType;
import org.dcm4chee.xds2.infoset.rim.ExternalIdentifierType;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.ObjectFactory;
import org.dcm4chee.xds2.infoset.rim.ObjectRefType;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectListType;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectType;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.infoset.rim.VersionInfoType;
import org.dcm4chee.xds2.persistence.Association;
import org.dcm4chee.xds2.persistence.ClassificationNode;
import org.dcm4chee.xds2.persistence.ClassificationScheme;
import org.dcm4chee.xds2.persistence.ExtrinsicObject;
import org.dcm4chee.xds2.persistence.Identifiable;
import org.dcm4chee.xds2.persistence.ObjectRef;
import org.dcm4chee.xds2.persistence.RegistryObject;
import org.dcm4chee.xds2.persistence.RegistryPackage;
import org.dcm4chee.xds2.persistence.Slot;
import org.dcm4chee.xds2.persistence.XADPatient;
import org.dcm4chee.xds2.persistence.XDSCode;
import org.dcm4chee.xds2.persistence.XDSDocumentEntry;
import org.dcm4chee.xds2.persistence.XDSFolder;
import org.dcm4chee.xds2.persistence.XDSObject;
import org.dcm4chee.xds2.persistence.XDSSubmissionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAXB <-> persistence (entity) layer
 * 
 * Maps local ID's to UUID's
 * 
 * @author franz.willer@gmail.com
 *
 */
public class XDSPersistenceWrapper {

    private XDSRegistryBean session;
    private ObjectFactory factory = new ObjectFactory();
    
    XDSSubmissionSet submissionSet;

    private HashMap<String, Identifiable> uuidMapping = new HashMap<String, Identifiable>();
    private HashMap<String, String> newUUIDs = new HashMap<String, String>();
    
    //private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

    private static Logger log = LoggerFactory.getLogger(XDSPersistenceWrapper.class);

    XdsRegistry cfg;
    public XDSPersistenceWrapper(XDSRegistryBean session) {
        this.session = session;
        this.cfg = session.getXdsRegistryConfig();
    }
    
    public ExtrinsicObject toExtrinsicObject(ExtrinsicObjectType eoType) throws XDSException {
        ExtrinsicObject eo;
        XDSDocumentEntry doc = null;
        if (XDSConstants.UUID_XDSDocumentEntry.equals(eoType.getObjectType())) {
            XDSValidator.checkDocumentEntryMetadata(eoType);
            doc = new XDSDocumentEntry();
            eo = doc;
            List<ExternalIdentifierType> list = eoType.getExternalIdentifier();
            if (list != null) {
                for (ExternalIdentifierType eiType : list) {
                    if (XDSConstants.UUID_XDSDocumentEntry_patientId.equals(eiType.getIdentificationScheme())) {
                        doc.setPatient(getPatient(eiType));
                    } else if (XDSConstants.UUID_XDSDocumentEntry_uniqueId.equals(eiType.getIdentificationScheme())) {
                        XDSValidator.checkAlreadyExists(eiType.getValue(), eoType, session);
                        doc.setUniqueId(eiType.getValue());
                    }
                }
            }
            doc.setSourcePatient(getPatient(getSlotValue(eoType, XDSConstants.SLOT_NAME_SOURCE_PATIENT_ID), true));
        } else {
            eo = new ExtrinsicObject();
        }
        toPersistenceObj(eoType, eo);
        eo.setMimeType(eoType.getMimeType());
        return eo;
    }

    public RegistryPackage toRegistryPackage(RegistryPackageType rpType) throws XDSException {
        RegistryPackage rp = null;
        List<ExternalIdentifierType> list = rpType.getExternalIdentifier();
        if (list != null) {
            for (ExternalIdentifierType eiType : list) {
                if (XDSConstants.UUID_XDSSubmissionSet_patientId.equals(eiType.getIdentificationScheme())) {
                    if (rp == null) {
                        XDSValidator.checkSubmissionSetMetadata(rpType);
                        rp = new XDSSubmissionSet();
                    }
                    ((XDSSubmissionSet)rp).setPatient(getPatient(eiType));
                } else if (XDSConstants.UUID_XDSSubmissionSet_uniqueId.equals(eiType.getIdentificationScheme())) {
                    if (rp == null) {
                        XDSValidator.checkSubmissionSetMetadata(rpType);
                        rp = new XDSSubmissionSet();
                    }
                    ((XDSSubmissionSet)rp).setUniqueId(eiType.getValue());
                } else if (XDSConstants.UUID_XDSFolder_patientId.equals(eiType.getIdentificationScheme())) {
                    if (rp == null) {
                        XDSValidator.checkFolderMetadata(rpType);
                        rp = new XDSFolder();
                    }
                    ((XDSFolder)rp).setPatient(getPatient(eiType));
                } else if (XDSConstants.UUID_XDSFolder_uniqueId.equals(eiType.getIdentificationScheme())) {
                    if (rp == null){
                        XDSValidator.checkFolderMetadata(rpType);
                        rp = new XDSFolder();
                    }
                    ((XDSFolder)rp).setUniqueId(eiType.getValue());
                }
            }
        }
        if (rp == null) {
            rp = new RegistryPackage();
        }
        toPersistenceObj(rpType, rp);
        if (rp instanceof XDSSubmissionSet)
            submissionSet = (XDSSubmissionSet)rp;
        return rp;
    }

    public Association toAssociation(AssociationType1 assocType) throws XDSException {
        Association assoc = new Association();
        toPersistenceObj(assocType, assoc);
        log.debug("######assocType.getAssociationType():{}",assocType.getAssociationType());
        assoc.setAssocType((ClassificationNode)
                getRegistryObject(assocType.getAssociationType()));
        return assoc;
    }
    
    /**
     * Sets the association fields for a corresponding Association entity 
     * @param assocType
     * @throws XDSException
     */
    public void setAssociationSrcAndTarget(AssociationType1 assocType) throws XDSException {
        
        Association assoc = (Association) getRegistryObject(assocType.getId());
        assoc.setSourceObject(getCheckedRegistryObject(assocType.getSourceObject(), "Associaton! targetObject not found!", assocType));
        assoc.setTargetObject(getCheckedRegistryObject(assocType.getTargetObject(), "Associaton! sourceObject not found!", assocType));
        XDSValidator.checkSamePatient(assoc);
        
    }

    public ClassificationScheme toClassificationScheme(ClassificationSchemeType schemeType, List<Identifiable> objects) throws XDSException {
        ClassificationScheme scheme = new ClassificationScheme();
        toPersistenceObj(schemeType, scheme);
        scheme.setNodeType(schemeType.getNodeType());
        scheme.setInternal(schemeType.isIsInternal());
        objects.add(scheme);
        copyClassificationNodes(schemeType.getClassificationNode(), scheme, objects);
        return scheme;
    }

    public ClassificationNode toClassificationNode(ClassificationNodeType nodeType, Identifiable parent, List<Identifiable> objects) throws XDSException {
        ClassificationNode node = new ClassificationNode();
        toPersistenceObj(nodeType, node);
        node.setParent(parent == null ? getIdentifiable(nodeType.getParent()) : parent);
        node.setCode(nodeType.getCode());
        node.setPath(nodeType.getPath());
        objects.add(node);
        for (ClassificationNodeType nType : nodeType.getClassificationNode()) {
            toClassificationNode(nType, node, objects);
        }
        return node;
    }

    public ObjectRef toObjectRef(ObjectRefType objRefType) {
        ObjectRef objRef = new ObjectRef();
        toPersistenceIdentifiable(objRefType, objRef);
        return objRef;
    }

    public void toPersistenceObj(RegistryObjectType roType, RegistryObject ro) throws XDSException {
        toPersistenceIdentifiable(roType, ro);
        
        ro.setName(roType.getName());
        ro.setDescription(roType.getDescription());
        
        ro.getClassifications().addAll(roType.getClassification());
        ro.getExternalIdentifiers().addAll(roType.getExternalIdentifier());
        
        ro.setLid(roType.getLid());
        ro.setObjectType(roType.getObjectType());
        ro.setStatus(roType.getStatus());
        indexCodes(roType.getClassification(), ro);
    
        if (roType.getVersionInfo() != null) {
            ro.setComment(roType.getVersionInfo().getComment());
            ro.setVersionName(roType.getVersionInfo().getVersionName());
        }
    }

    public void toPersistenceIdentifiable(IdentifiableType roType, Identifiable ro) {
        ro.setId(roType.getId());
        ro.setHome(roType.getHome());
        copySlots(roType.getSlot(), ro);
        // ids should be already updated by checkAndCorrectSubmitObjectsRequest for the whole request
        uuidMapping.put(roType.getId(), ro);
    }

    public RegistryObjectListType toRegistryObjectListType(List<? extends Identifiable> objects, boolean isLeafClass) throws XDSException {
        return toRegistryObjectListType(objects, isLeafClass, false);
    }

    public RegistryObjectListType toRegistryObjectListType(List<? extends Identifiable> objects, 
            boolean isLeafClass, boolean allowMultiPatientResponse) throws XDSException {
        RegistryObjectListType objListType = factory.createRegistryObjectListType();
        List<JAXBElement<? extends IdentifiableType>> objList = objListType.getIdentifiable();
        if (objects != null) {
            if (isLeafClass && !allowMultiPatientResponse) {
                log.info("#### call checkSamePatient");
                XDSValidator.checkSamePatient(objects);
                log.info("#### finished checkSamePatient");
            }
            Identifiable obj;
            for (int i=0,len=objects.size() ; i < len ; i++) {
                obj = objects.get(i);
                if (!isLeafClass) {
                    objList.add(toJAXBObjectRef(obj));
                } else if (obj instanceof ExtrinsicObject) {
                    objList.add(toJAXBExtrinsicObject((ExtrinsicObject)obj));
                } else if (obj instanceof RegistryPackage) {
                    objList.add(toJAXBRegistryPackage((RegistryPackage)obj));
                } else if (obj instanceof Association) {
                    objList.add(toJAXBAssociation((Association)obj));
                } else {
                    log.error("Unknown RegistryObject! id:"+obj.getId());
                }
            }
        }
        return objListType;
    }

    private JAXBElement<? extends IdentifiableType> toJAXBExtrinsicObject(
            ExtrinsicObject eo) {
        ExtrinsicObjectType eoType = factory.createExtrinsicObjectType();
        toEbXmlObj(eo, eoType);
        eoType.setMimeType(eo.getMimeType());
        eoType.setIsOpaque(eo.isOpaque());
        return factory.createExtrinsicObject(eoType);
    }

    private JAXBElement<? extends IdentifiableType> toJAXBRegistryPackage(
            RegistryPackage rp) {
        RegistryPackageType rpType = factory.createRegistryPackageType();
        toEbXmlObj(rp, rpType);
        return factory.createRegistryPackage(rpType);
    }

    private JAXBElement<? extends IdentifiableType> toJAXBAssociation(
            Association assoc) {
        AssociationType1 assocType = factory.createAssociationType1();
        toEbXmlObj(assoc, assocType);
        if (assoc.getAssocType() != null) {
            assocType.setAssociationType(assoc.getAssocType().getId());
        } else {
            log.error("AssocType is null! Association id:"+assoc.getId());
            assocType.setAssociationType("urn:willi:123-abc-123");
        }
        if (assoc.getSourceObject() != null) {
            assocType.setSourceObject(assoc.getSourceObject().getId());
        } else {
            log.error("SourceObject is null! Association id:"+assoc.getId());
            assocType.setSourceObject("urn:willi:123-abc-456");
        }
        if (assoc.getTargetObject() != null) {
            assocType.setTargetObject(assoc.getTargetObject().getId());
        } else {
            log.error("TargetObject is null! Association id:"+assoc.getId());
            assocType.setTargetObject("urn:willi:123-abc-789");
        }
        return factory.createAssociation(assocType);
    }

    private JAXBElement<? extends IdentifiableType> toJAXBObjectRef(Identifiable i) {
        ObjectRefType objRef = factory.createObjectRefType();
        objRef.setId(i.getId());
        return factory.createObjectRef(objRef);
    }

    public void toEbXmlObj(RegistryObject ro, RegistryObjectType roType) {
        log.debug("\n#### called toEbXmlObj");
        roType.setId(ro.getId());
        roType.setLid(ro.getLid());
        roType.setHome(ro.getHome());
        roType.setObjectType(ro.getObjectType());
        roType.setStatus(ro.getStatus());
    
    
        roType.setName(ro.getName());
        roType.setDescription(ro.getDescription());
        roType.getClassification().addAll(ro.getClassifications());
        roType.getExternalIdentifier().addAll(ro.getExternalIdentifiers());
        
        log.debug("\n#### copySlotType");
        copySlotType(ro.getSlots(), roType);
        VersionInfoType version = factory.createVersionInfoType();
        version.setVersionName(ro.getVersionName());
        version.setComment(ro.getComment());
        roType.setVersionInfo(version);
        log.debug("\n#### finnished toEbXmlObj");
    }

    private void indexCodes(List<ClassificationType> list, RegistryObject ro) throws XDSException {
        if (list != null) {
            if (ro instanceof XDSObject) {
                Collection<XDSCode> xdsCodes = ((XDSObject) ro).getXDSCodes();
                if (xdsCodes == null) {
                    xdsCodes = new ArrayList<XDSCode>();
                    ((XDSObject) ro).setXDSCodes(xdsCodes);
                }
                for (ClassificationType clType : list) {
                    if (isXDSCode(clType)) {
                        xdsCodes.add(session.getXDSCode(clType, cfg.isCreateMissingCodes()));
                    }
                }
            }
        }
    }

    /**
     * Correct metadata parameters to conform to XDS specification.
     * Replace non-conformant ids to uuids, update references, check classification scemes, nodes, etc
     */
    void checkAndCorrectSubmitObjectsRequest(SubmitObjectsRequest req) throws XDSException {
    
        // TODO: DB_RESTRUCT - CODE INSPECTION - is it correct to replace ids with uuids for ALL identifiables, not only ROs?
    
        
        // First run - replace non-uuid IDs with UUIDs for all identifiables, included nested ones
        
        JXPathContext requestContext = JXPathContext.newContext(req);
        // Use //id xpath to find all id fields of identifiables in the request
        Iterator ids = (Iterator) requestContext.iteratePointers("//id"); 
        
        while (ids.hasNext()) {
            
            Pointer p = (Pointer) ids.next();
            String oldId = (String) p.getValue(); 
            String newIdUUID = oldId;

            if (oldId == null) continue;
            
            // Replace non-UUID id with a generated UUID
            if (!oldId.startsWith("urn:")) {
                newIdUUID = "urn:uuid:" + UUID.randomUUID().toString();
                p.setValue(newIdUUID);
                log.debug("Replacing id {} with uuid {}", oldId, newIdUUID);
            }
            
            newUUIDs.put(oldId,newIdUUID);
        }
    
        // Second run - perform check and correction recursively
        for (JAXBElement<? extends IdentifiableType> elem : req.getRegistryObjectList().getIdentifiable()) {
    
            // filter RegistryObjects only
            if (!RegistryObjectType.class.isAssignableFrom(elem.getValue().getClass()))
                continue;
            RegistryObjectType ro = (RegistryObjectType) elem.getValue();
    
            checkAndCorrectMetadata(ro);
        }
    }

    public void checkAndCorrectMetadata(RegistryObjectType ro) throws XDSException {
        // Set Lid to id if not specified
        if (ro.getLid() == null)
            ro.setLid(ro.getId());
        
        // Set status if not specified 
        if (ro.getStatus() == null )
            ro.setStatus("urn:oasis:names:tc:ebxml-regrep:StatusType:Approved");
        
        //TODO VersionInfo
        // Set version if not specified 
        if (ro.getVersionInfo() == null)
        {
            VersionInfoType ver = new ObjectFactory().createVersionInfoType();
            ver.setComment("Initial Version");
            ver.setVersionName("1.0");
            ro.setVersionInfo(ver);
        }
        
        // Perform correction for ext ids
        for (ExternalIdentifierType extId : ro.getExternalIdentifier()) {
            // set reference to parent registry object if not set already
            if (extId.getRegistryObject() == null)
                extId.setRegistryObject(ro.getId());

            // correct recursively
            checkAndCorrectMetadata(extId);
        }
        
        // Perform correction for classifications
        for (ClassificationType classif : ro.getClassification()) {
            
            // set reference to parent registry object if not set already
            if (classif.getClassifiedObject() == null)
                classif.setClassifiedObject(ro.getId()); //TODO: DB_RESTRUCT : is it correct??

            // correct recursively
            checkAndCorrectMetadata(classif);
            
            // check classification
            checkClassification(classif);
            
        }

        // if this RegistryObject is an ext id - update registryObject in case its Id has changed @ 1st run
        if (ro.getClass().equals(ExternalIdentifierType.class)) {
            ExternalIdentifierType extId = (ExternalIdentifierType) ro;
            String uuid = newUUIDs.get(extId.getRegistryObject());
            // if uuid is null - do nothing, maybe the referenced object is stored in the registry
            if (uuid != null) extId.setRegistryObject(uuid);
        }
        
        // if this RegistryObject is a classification - update classifiedObj in case its Id has changed @ 1st run
        if (ro.getClass().equals(ClassificationType.class)) {
            ClassificationType cl = (ClassificationType) ro;
            String uuid = newUUIDs.get(cl.getClassifiedObject());
            // if uuid is null - do nothing, maybe the referenced object is stored in the registry
            if (uuid != null) cl.setClassifiedObject(uuid);
        }
        
        // if this RegistryObject is an Association - update source and target uuids in case there were changed @ 1st run
        if (ro.getClass().equals(AssociationType1.class)) {
            AssociationType1 assoc = (AssociationType1) ro;
            String srcUuid = newUUIDs.get(assoc.getSourceObject());
            String targetUuid = newUUIDs.get(assoc.getTargetObject());
            
            // if uuids are null - do nothing, maybe referenced objects are stored in the registry
            if (srcUuid != null) assoc.setSourceObject(srcUuid);
            if (targetUuid != null) assoc.setTargetObject(targetUuid);
        }
        
        
    }

    private void checkClassification(ClassificationType classif) throws XDSException {
        if (classif.getClassificationScheme() != null) {
            RegistryObject obj = getRegistryObject(classif.getClassificationScheme());

            // if referenced scheme is missing - throw exception
            if (obj == null) 
                throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                        String.format("Classification Scheme (%s) not found! Classification id: %s",
                                classif.getClassificationScheme(),
                                classif.getId()
                                ),
                        null);

            
            // if referenced object is not a scheme - throw exception
            if (!(obj instanceof ClassificationScheme))
            throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR,
                    String.format("Referenced ClassificationScheme (%s) is not a ClassificationScheme! (objType:%s, class:%s) Classification id: %s",
                            classif.getClassificationScheme(),
                            obj.getObjectType(),
                            obj.getClass().getSimpleName(),
                            classif.getId()),
                    null);
        } else
        if (classif.getClassificationNode() != null) {
            RegistryObject obj = getRegistryObject(classif.getClassificationNode());
            
            // if referenced classification node is missing - throw exception
            if (obj == null) {
                throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                        String.format("Classification Node (%s) missing! Classification id: %s", 
                                classif.getClassificationNode(),
                                classif.getId()), 
                        null);
            } 

            // if referenced object is not a node - throw exception
            if (!(obj instanceof ClassificationNode)) {
                throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                    String.format("Referenced ClassificationNode (%s) is not a ClassificationNode! (objType:%s, class:%s ) Classification id: %s", 
                        classif.getClassificationScheme(),
                        obj.getObjectType(), 
                        obj.getClass().getSimpleName(),
                        classif.getId()), 
                    null);
            } 
        } else {
            
            throw new XDSException(XDSException.XDS_ERR_REGISTRY_METADATA_ERROR, 
                    "Classification has neither Node nor Scheme specified! Classification id:"+classif.getId(), null);
        }
    }
        
        
    private void copyClassificationNodes(List<ClassificationNodeType> list, Identifiable parent, List<Identifiable> objects) throws XDSException {
        if (list != null) {
            for (ClassificationNodeType nodeType : list) {
                toClassificationNode(nodeType, parent, objects);
            }
        }
    }


    private void copySlots(List<SlotType1> list, Identifiable ro) {
        ro.setSlotTypes(list);
    }

    private void copySlotType(List<Slot> slots, RegistryObjectType roType) {
        if (slots != null) {
            SlotType1 slotType;
            HashMap<String, SlotType1> slotTypeMap = new HashMap<String, SlotType1>();
            for (Slot slot : slots) {
                slotType = slotTypeMap.get(slot.getName());
                if (log.isDebugEnabled())
                    log.debug("########add slot name:"+slot.getName()+" value:"+slot.getValue()+" parent.pk:"+slot.getParent().getPk()+" pk:"+slot.getPk());
                if (slotType == null) {
                    slotType = factory.createSlotType1();
                    slotType.setName(slot.getName());
                    slotType.setSlotType(slot.getType());
                    slotType.setValueList(factory.createValueListType());
                    slotTypeMap.put(slot.getName(), slotType);
                }
                slotType.getValueList().getValue().add(slot.getValue());
            }
            roType.getSlot().addAll(slotTypeMap.values());
        }
    }

    public String getSlotValue(IdentifiableType idType, String slotName) {
        List<SlotType1> list = idType.getSlot();
        if (list != null) {
            for (SlotType1 slotType : list) {
                if (slotName.equals(slotType.getName()))
                    return slotType.getValueList().getValue().get(0);
            }
        }
        return null;
    }
    
    private RegistryObject getCheckedRegistryObject(String id, String errMsg, IdentifiableType parent) throws XDSException {
        RegistryObject ro = null;
        try {
            ro = getRegistryObject(id);
        } catch (Exception ignore) {}
        if (ro == null)
            throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, errMsg+" id="+id+" parent="+parent.getId(), null);
        return ro;
    }
    private RegistryObject getRegistryObject(String id) {
        if (id == null)
            return null;
        Identifiable ro = uuidMapping.get(id);
        if (ro == null) {
            ro = session.getRegistryObjectByUUID(id);
        }
        if (ro != null && !(ro instanceof RegistryObject)) {
            log.warn("####### Identifiable is not a RegistryObject! id:"+ro.getId());
        }
        return (RegistryObject) ro;
    }
    private Identifiable getIdentifiable(String id) {
        if (id == null)
            return null;
        Identifiable obj = uuidMapping.get(id);
        return (obj == null) ? session.getIdentifiableByUUID(id) : obj;
    }
    
    private XADPatient getPatient(ExternalIdentifierType eiType) throws XDSException {
        XADPatient pat = getPatient(eiType.getValue(), cfg.isCreateMissingPIDs());
        if (pat.getIssuerOfPatientID().getNamespaceID() != null) {
            log.debug("XAD PatientID ("+eiType.getValue()+") contains Namespace ID! Corrected in metadata!");
            eiType.setValue(pat.getXADPatientID());
        }
        return pat;
    }
    private XADPatient getPatient(String pid, boolean createMissing) throws XDSException {
        return session.getPatient(pid, createMissing);
    }
    
    private boolean isXDSCode(ClassificationType cl) {
        List<SlotType1> slots = cl.getSlot();
        return slots != null && slots.size() == 1 && "codingScheme".equals(slots.get(0).getName());
    }

    protected void logUIDMapping() {
        //if (log.isDebugEnabled()) {
            log.info("UUID_MAPPING:-------------------------------------------------");
            for (Map.Entry<String, Identifiable>e :this.uuidMapping.entrySet()){
                log.debug(e.getKey()+":"+e.getValue().getId());
            }
            log.info("--------------------------------------------------------------");
        //}
    }

    public XDSSubmissionSet getSubmissionSet() {
        return submissionSet;
    }
    
}
