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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.JAXBElement;

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
import org.dcm4chee.xds2.persistence.XDSDocumentEntry;
import org.dcm4chee.xds2.persistence.XDSFolder;
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
    
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

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
        assoc.setSourceObject(getCheckedRegistryObject(assocType.getSourceObject(), "Associaton! targetObject not found!", assocType));
        assoc.setTargetObject(getCheckedRegistryObject(assocType.getTargetObject(), "Associaton! sourceObject not found!", assocType));
        XDSValidator.checkSamePatient(assoc);
        return assoc;
    }
    
    /*public Classification toClassification(ClassificationType clType) throws XDSException {
        Classification cl = new Classification();
        toPersistenceObj(clType, cl);
        cl.setNodeRepresentation(clType.getNodeRepresentation());
        log.debug("######clType.getClassificationScheme:{}",clType.getClassificationScheme());
        if (clType.getClassificationScheme() != null) {
            RegistryObject obj = getRegistryObject(clType.getClassificationScheme());
            if (obj == null) {
                throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                        "Classification Scheme ("+clType.getClassificationScheme()+
                        ") missing! Classification id:"+clType.getId(), null);
            } else if (obj instanceof ClassificationScheme) {
                cl.setClassificationScheme((ClassificationScheme) obj);
            } else {
                throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                        "Referenced ClassificationScheme ("+clType.getClassificationScheme()+
                        ") is not a ClassificationScheme! (objType:"+obj.getObjectType()+", class:"+obj.getClass().getSimpleName()+
                        ") Classification id:"+clType.getId(), null);
            }
        } else if (clType.getClassificationNode() != null) {
            RegistryObject obj = getRegistryObject(clType.getClassificationNode());
            if (obj == null) {
                throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                        "Classification Node ("+clType.getClassificationNode()+
                        ") missing! Classification id:"+clType.getId(), null);
            } else if (obj instanceof ClassificationNode) {
                cl.setClassificationNode((ClassificationNode) obj);
            } else {
                throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                        "Referenced ClassificationNode ("+clType.getClassificationScheme()+
                        ") is not a ClassificationNode! (objType:"+obj.getObjectType()+", class:"+obj.getClass().getSimpleName()+
                        ") Classification id:"+clType.getId(), null);
            } 
        }else {
            log.error("#######Classification Node AND Scheme missing!!! classification id:"+clType.getId());
            throw new XDSException(XDSException.XDS_ERR_REGISTRY_METADATA_ERROR, 
                    "Classification has neither Node nor Scheme specified! Classification id:"+cl.getId(), null);
        }
        log.debug("######clType.getClassifiedObject:{}", clType.getClassifiedObject());
        if (clType.getClassifiedObject() != null) {
            cl.setClassifiedObject(getRegistryObject(clType.getClassifiedObject()));
        }

        return cl;
    }*/

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
        
        ro.setLid(roType.getLid() == null ? ro.getId() : roType.getLid());//TODO if no LID, check if older RegistryObject exists and use this Lid!
        ro.setObjectType(roType.getObjectType());
        ro.setStatus("urn:oasis:names:tc:ebxml-regrep:StatusType:Approved");
        /*copyName(roType.getName(), ro);
        copyDescriptions(roType.getDescription(), ro);
        copyClassifications(roType.getClassification(), ro);
        copyExternalIdentifier(roType.getExternalIdentifier(), ro);*/
        //TODO VersionInfo
        ro.setVersionName("1.0");
        ro.setComment("Initial Version");
    }
    public void toPersistenceIdentifiable(IdentifiableType roType, Identifiable ro) {
        String id = roType.getId();
        ro.setId(id.startsWith("urn:") ? id : "urn:uuid:"+UUID.randomUUID().toString());
        uuidMapping.put(id, ro);
        ro.setHome(roType.getHome());
        copySlots(roType.getSlot(), ro);
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
               /* } else if (obj instanceof Classification) {
                    objList.add(toJAXBClassification((Classification)obj));*/
                } else {
                    log.error("Unknown RegistryObject! id:"+obj.getId());
                }
            }
        }
        return objListType;
    }
    
    /*private void copyName(InternationalStringType isType, RegistryObject ro) {
        Set<Name> names = new HashSet<Name>();
        if (isType != null) {
            Name name;
            for (LocalizedStringType localString : isType.getLocalizedString()) {
                name = new Name();
                name.setLang(localString.getLang());
                name.setValue(localString.getValue());
                name.setCharset(localString.getCharset());
                name.setParent(ro);
                names.add(name);
            }
        }
        ro.setName(names);
    }-*/

    /*private void copyDescriptions(InternationalStringType isType, RegistryObject ro) {
        Set<Description> descriptions = new HashSet<Description>();
        if (isType != null) {
            Description desc;
            for (LocalizedStringType localString : isType.getLocalizedString()) {
                desc = new Description();
                desc.setLang(localString.getLang());
                log.info("######## description.setValue:"+localString.getValue()+" parent:"+ro);
                desc.setValue(localString.getValue());
                desc.setCharset(localString.getCharset());
                desc.setParent(ro);
                descriptions.add(desc);
            }
        }
        ro.setDescription(descriptions);
    }*/
    
    /*private void copyClassifications(List<ClassificationType> list, RegistryObject ro) throws XDSException {
        if (list != null) {
            Set<Classification> clList = ro.getClassifications();
            Collection<XDSCode> xdsCodes = null;
            if (ro instanceof XDSObject) {
               xdsCodes = ((XDSObject) ro).getXDSCodes();
               if (xdsCodes == null) {
                   xdsCodes = new ArrayList<XDSCode>();
                   ((XDSObject) ro).setXDSCodes(xdsCodes);
               }
            }
            for (ClassificationType clType : list) {
                if (xdsCodes != null && isXDSCode(clType)) {
                    xdsCodes.add(session.getXDSCode(clType, cfg.isCreateMissingCodes()));
                    if (!cfg.isDontSaveCodeClassifications()) {
                        clList.add(toClassification(clType));
                    }
                } else {
                    clList.add(toClassification(clType));
                }
            }
        }
    }*/

    private void copyClassificationNodes(List<ClassificationNodeType> list, Identifiable parent, List<Identifiable> objects) throws XDSException {
        if (list != null) {
            for (ClassificationNodeType nodeType : list) {
                toClassificationNode(nodeType, parent, objects);
            }
        }
    }

    /*private void copyExternalIdentifier(List<ExternalIdentifierType> list, RegistryObject ro) throws XDSException {
        if (list != null) {
            ExternalIdentifier ei;
            for (ExternalIdentifierType eiType : list) {
                ei = new ExternalIdentifier();
                toPersistenceObj(eiType, ei);
                ei.setIdentificationScheme((ClassificationScheme)
                        getRegistryObject(eiType.getIdentificationScheme()));
                ei.setValue(eiType.getValue());
                ei.setRegistryObject(eiType.getRegistryObject() != null ? getRegistryObject(eiType.getRegistryObject()) : ro);
                ro.getExternalIdentifiers().add(ei);
            }
        }
    }*/

    private void copySlots(List<SlotType1> list, Identifiable ro) {
        List<Slot> slots = new ArrayList<Slot>();
        if (list != null) {
            List<String> values;
            for (SlotType1 slotType : list) {
                if (!XDSConstants.SLOT_NAME_LAST_UPDATE_TIME.equals(slotType.getName())) {
                    values = slotType.getValueList().getValue();
                    for (int i = 0, len = values.size() ; i < len ; i++) {
                        slots.add(newSlot(ro, slotType.getName(), slotType.getSlotType(), values.get(i)));
                    }
                }
            }
            if (ro instanceof XDSFolder) {
                slots.add( newSlot(ro, XDSConstants.SLOT_NAME_LAST_UPDATE_TIME, null, sdf.format(new Date())));
            }
        }
        ro.setSlots(slots);
    }

    private Slot newSlot(Identifiable ro, String slotName, String slotType, String v) {
        Slot slot;
        slot = new Slot();
        slot.setName(slotName);
        slot.setType(slotType);
        slot.setValue(v);
        slot.setParent(ro);
        return slot;
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
 /*   private JAXBElement<? extends IdentifiableType> toJAXBClassification(
            Classification obj) {
        return factory.createClassification(toClassificationType(obj));
    }*/
    
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

        
        /*log.debug("\n#### copyNameType");
        //copyNameType(ro.getName(), roType);
        log.debug("\n#### copyDescriptionType");
        //copyDescriptionType(ro.getDescription(), roType);
        log.debug("\n#### copyClassificationType");
        //copyClassificationType(ro.getClassifications(), roType);
        log.debug("\n#### copyExternalIdentifierType");
        //copyExternalIdentifierType(ro.getExternalIdentifiers(), roType);
        */
        
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

    /*private void copyNameType(Set<Name> name, RegistryObjectType roType) {
        InternationalStringType is = factory.createInternationalStringType();
        List<LocalizedStringType> values = is.getLocalizedString();
        LocalizedStringType value;
        for (Name n : name) {
            value = factory.createLocalizedStringType();
            value.setCharset(n.getCharset());
            value.setLang(n.getLang());
            value.setValue(n.getValue());
            values.add(value);
        }
        roType.setName(is);
    }

    private void copyDescriptionType(Set<Description> descriptions, RegistryObjectType roType) {
        InternationalStringType is = factory.createInternationalStringType();
        List<LocalizedStringType> values = is.getLocalizedString();
        LocalizedStringType value;
        for (Description n : descriptions) {
            value = factory.createLocalizedStringType();
            value.setCharset(n.getCharset());
            value.setLang(n.getLang());
            value.setValue(n.getValue());
            values.add(value);
        }
        roType.setDescription(is);
    }
    
    private void copyClassificationType(Set<Classification> classifications, RegistryObjectType roType) {
        if (classifications != null) {
            List<ClassificationType> classificationTypes = roType.getClassification();
            ClassificationType clType;
            for (Classification cl: classifications) {
                clType = toClassificationType(cl);
                classificationTypes.add(clType);
            }
        }
    }

    private ClassificationType toClassificationType(Classification cl) {
        ClassificationType clType = factory.createClassificationType();
        toEbXmlObj(cl, clType);
        clType.setNodeRepresentation(cl.getNodeRepresentation());
        if (cl.getClassificationScheme() != null) {
            clType.setClassificationScheme(cl.getClassificationScheme().getId());
        } else if (cl.getClassificationNode() != null) {
            clType.setClassificationNode(cl.getClassificationNode().getId());
        } else {
            log.error("Missing ClassificationNode and ClassificationScheme! Classification id:"+cl.getId());
            clType.setClassificationScheme("urn:willi:987-abc-456");
        }
        if (cl.getClassifiedObject() != null) {
            clType.setClassifiedObject(cl.getClassifiedObject().getId());
        } else {
            log.error("Missing ClassifiedObject! Classification id:"+cl.getId());
            //TODO: move constant to configuration?
            clType.setClassifiedObject("urn:willi:987-abc-789");
        }
        return clType;
    }

    private void copyExternalIdentifierType(Set<ExternalIdentifier> externalIdentifiers, RegistryObjectType roType) {
        if (externalIdentifiers != null) {
            List<ExternalIdentifierType> eiTypes = roType.getExternalIdentifier();
            ExternalIdentifierType eiType;
            for (ExternalIdentifier ei : externalIdentifiers) {
                eiType = factory.createExternalIdentifierType();
                toEbXmlObj(ei, eiType);
                if (ei.getIdentificationScheme() != null) {
                    eiType.setIdentificationScheme(ei.getIdentificationScheme().getId());
                } else {
                    log.error("IdentificationScheme is null! ExternalIdentifier id:"+ei.getId());
                    eiType.setIdentificationScheme("urn:willi:12345-abcd-123");
                }
                eiType.setValue(ei.getValue());
                if (ei.getRegistryObject() != null) {
                    eiType.setRegistryObject(ei.getRegistryObject().getId());
                } else {
                    log.error("RegistryObject is null! ExternalIdentifier id:"+ei.getId());
                    eiType.setRegistryObject("urn:willi:12345-abcd-456");
                }
                eiTypes.add(eiType);
            }
        }
    } */

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
    
    protected void logUIDMapping() {
        //if (log.isDebugEnabled()) {
            log.info("UUID_MAPPING:-------------------------------------------------");
            for (Map.Entry<String, Identifiable>e :this.uuidMapping.entrySet()){
                log.debug(e.getKey()+":"+e.getValue().getId());
            }
            log.info("--------------------------------------------------------------");
        //}
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

    public XDSSubmissionSet getSubmissionSet() {
        return submissionSet;
    }
    
}
