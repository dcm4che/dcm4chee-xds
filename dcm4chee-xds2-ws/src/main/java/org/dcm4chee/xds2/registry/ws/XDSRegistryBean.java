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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.ws.Action;
import javax.xml.ws.BindingType;
import javax.xml.ws.soap.Addressing;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.AssociationType1;
import org.dcm4chee.xds2.infoset.rim.ClassificationNodeType;
import org.dcm4chee.xds2.infoset.rim.ClassificationSchemeType;
import org.dcm4chee.xds2.infoset.rim.ClassificationType;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.ObjectFactory;
import org.dcm4chee.xds2.infoset.rim.ObjectRefType;
import org.dcm4chee.xds2.infoset.rim.RegistryError;
import org.dcm4chee.xds2.infoset.rim.RegistryErrorList;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectListType;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.infoset.ws.DocumentRegistryPortType;
import org.dcm4chee.xds2.persistence.Association;
import org.dcm4chee.xds2.persistence.Identifiable;
import org.dcm4chee.xds2.persistence.QRegistryObject;
import org.dcm4chee.xds2.persistence.RegistryObject;
import org.dcm4chee.xds2.persistence.Slot;
import org.dcm4chee.xds2.persistence.XADIssuer;
import org.dcm4chee.xds2.persistence.XADPatient;
import org.dcm4chee.xds2.persistence.XDSCode;
import org.dcm4chee.xds2.persistence.XDSDocumentEntry;
import org.dcm4chee.xds2.persistence.XDSFolder;
import org.dcm4chee.xds2.persistence.XDSSubmissionSet;
import org.dcm4chee.xds2.registry.ws.query.StoredQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.query.jpa.impl.JPAQuery;

@Stateless
@WebService(endpointInterface="org.dcm4chee.xds2.infoset.ws.DocumentRegistryPortType", 
        name="xds",
        serviceName="XDSbRegistry",
        portName="DocumentRegistry_Port_Soap12",
        targetNamespace="urn:ihe:iti:xds-b:2007",
        wsdlLocation = "/META-INF/wsdl/XDS.b_DocumentRegistry.wsdl"
)
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@Addressing(enabled=true, required=true)
@HandlerChain(file="handlers.xml")
public class XDSRegistryBean implements DocumentRegistryPortType {

    private ObjectFactory factory = new ObjectFactory();

    @Resource 
    private SessionContext context;
    @Resource
    private EJBContext ejbContext;
    
    @PersistenceContext(unitName = "dcm4chee-xds")
    private EntityManager em;

    private static Logger log = LoggerFactory.getLogger(XDSRegistryBean.class);
    
    public XDSRegistryBean() {
    }
    
    @Override
    @WebMethod(operationName = "DocumentRegistry_RegisterDocumentSet-b", action = "urn:ihe:iti:21111:RegisterDocumentSet-b")
    @WebResult(name = "RegistryResponse", targetNamespace = "urn:oasis:names:tc:ebxml-regrep:xsd:rs:3.0", partName = "body")
    @Action(input="urn:ihe:iti:2007:RegisterDocumentSet-b", 
            output="urn:ihe:iti:2007:RegisterDocumentSet-bResponse")
    public RegistryResponseType documentRegistryRegisterDocumentSetB(
            SubmitObjectsRequest req) {
        log.info("################ documentRegistryRegisterDocumentSetB called! CallerPrincipal:"+
                context.getCallerPrincipal());
        RegistryResponseType rsp = factory.createRegistryResponseType();
        try {
            store(req);
            rsp.setStatus(XDSConstants.XDS_B_STATUS_SUCCESS);
        } catch (Exception x) {
            XDSException e;
            if (x instanceof XDSException) {
                e = (XDSException) x;
                log.error("XDSException:"+e);
                log.debug("XDSException stacktrace:", x);
            } else {
                log.error("Unexpected error in XDS service (register document)!: "+x.getMessage());
                log.error("Stacktrace: ",x);
                e = new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                        "Unexpected error in XDS service !: "+x.getMessage(),x);
            }
            this.addError(rsp, e);
            ejbContext.setRollbackOnly();
        }
        log.info("################# documentRegistryRegisterDocumentSetB finished! #############");
        return rsp;
    }

    @Override
    @Action(input="urn:ihe:iti:2007:RegistryStoredQuery", 
            output="urn:ihe:iti:2007:RegistryStoredQueryResponse")
    public AdhocQueryResponse documentRegistryRegistryStoredQuery(
            AdhocQueryRequest req) {
        log.info("################ documentRegistryRegistryStoredQuery called! CallerPrincipal:"
                +context.getCallerPrincipal());
        log.debug("ReturnType:"+req.getResponseOption().getReturnType());
        try {
            return StoredQuery.getStoredQuery(req, this).query();
        } catch (Exception x) {
            log.error("Unexpected error in XDS service (query)!: "+x.getMessage(),x);
            AdhocQueryResponse rsp = factory.createAdhocQueryResponse();
            XDSException e = (x instanceof XDSException) ? (XDSException) x :
                new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                        "Unexpected error in XDS service !: "+x.getMessage(),x);
            this.addError(rsp, e);
            rsp.setRegistryObjectList(factory.createRegistryObjectListType());
            return rsp;
        }
    }

    private List<Identifiable> store(SubmitObjectsRequest req) throws XDSException {
        XDSPersistenceWrapper wrapper = new XDSPersistenceWrapper(this);
        RegistryObjectListType rol = req.getRegistryObjectList();
        List<JAXBElement<? extends IdentifiableType>> objs = rol.getIdentifiable();
        IdentifiableType obj;
        List<Identifiable> objects = new ArrayList<Identifiable>();
        for (int i=0,len=objs.size() ; i < len ; i++) {
            obj = objs.get(i).getValue();
            if (obj instanceof ExtrinsicObjectType) {
                objects.add(wrapper.toExtrinsicObject((ExtrinsicObjectType)obj));
            } else if (obj instanceof RegistryPackageType) {
                objects.add(wrapper.toRegistryPackage((RegistryPackageType)obj));
            } else if (obj instanceof ClassificationType) {
                objects.add(wrapper.toClassification((ClassificationType)obj));
            } else if (obj instanceof AssociationType1) {
                objects.add(wrapper.toAssociation((AssociationType1)obj));
            } else if (obj instanceof ClassificationSchemeType) {//for initialization
                wrapper.toClassificationScheme((ClassificationSchemeType)obj, objects);
            } else if (obj instanceof ClassificationNodeType) {//for initialization
                wrapper.toClassificationNode((ClassificationNodeType)obj, null, objects);
            } else if (obj instanceof ObjectRefType) {//for initialization
                objects.add(wrapper.toObjectRef((ObjectRefType)obj));
            } else {
                log.warn("### unknown RegistryObject:"+obj);
                
            }
        }
        wrapper.logUIDMapping();
        storeRegistryObjects(objects);
        handleLifecycle(objects);
        return objects;
    }

    public RegistryObject getRegistryObjectByUUID(String id) {
        JPAQuery query = new JPAQuery(em);
        QRegistryObject registryObject = QRegistryObject.registryObject;
        RegistryObject obj = query.from(registryObject).where(registryObject.id.eq(id)).uniqueResult(registryObject);
        return obj;
    }
    /**
     * Standard query (ExtrinsicObject):
     *    SELECT i FROM ExtrinsicObject i LEFT JOIN FETCH i.slots as s LEFT JOIN FETCH i.classifications as c LEFT JOIN FETCH i.externalIdentifiers as e WHERE e.value = ?1 and e.identificationScheme.id='"
     * 
     * @param ids
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<XDSDocumentEntry> getDocumentEntriesByUUID(boolean fetch, String... ids) {
        //return getDocumentEntriesById(fetch, "id", ids);
        return (List<XDSDocumentEntry>) getObjectsByNamedQuery(XDSDocumentEntry.FIND_BY_UUIDS, XDSDocumentEntry.FIND_BY_UUIDS_NAME, ids);
    }
    @SuppressWarnings("unchecked")
    public List<XDSDocumentEntry> getDocumentEntriesByUniqueId(boolean fetch, String... ids) {
        return (List<XDSDocumentEntry>) getObjectsByNamedQuery(XDSDocumentEntry.FIND_BY_UNIQUE_IDS, XDSDocumentEntry.FIND_BY_UNIQUE_IDS_NAME, ids);
    }

    public XDSDocumentEntry getDocumentEntryByUniqueId(String uniqueId) {
        return (XDSDocumentEntry) getObjectByNamedQuery(XDSDocumentEntry.FIND_BY_UNIQUE_ID, uniqueId);
    }
    public XDSDocumentEntry getDocumentEntryByUUID(String uuid) {
        return (XDSDocumentEntry) getObjectByNamedQuery(XDSDocumentEntry.FIND_BY_UUID, uuid);
    }

    public XDSDocumentEntry getDocumentEntryByUUIDandFetch(String uuid) {
        XDSDocumentEntry doc = (XDSDocumentEntry) getObjectByNamedQuery(XDSDocumentEntry.FIND_BY_UUID, uuid);
        log.info("found classifications:"+doc.getClassifications().size());
        //doc.getExternalIdentifiers();
        //doc.getDescription();
        //doc.getName();
        //doc.getXDSCodes();
        return doc;
    }

    public XDSSubmissionSet getSubmissionSetByUniqueId(String uniqueId) {
        return (XDSSubmissionSet) getObjectByNamedQuery(XDSSubmissionSet.FIND_BY_UNIQUE_ID, uniqueId);
    }
    public XDSSubmissionSet getSubmissionSetByUUID(String uuid) {
        return (XDSSubmissionSet) getObjectByNamedQuery(XDSSubmissionSet.FIND_BY_UUID, uuid);
    }

    
    public XDSFolder getFolderByUniqueId(String uniqueId) {
        return (XDSFolder) getObjectByNamedQuery(XDSFolder.FIND_BY_UNIQUE_ID, uniqueId);
    }
    public XDSFolder getFolderByUUID(String uuid) {
        return (XDSFolder) getObjectByNamedQuery(XDSFolder.FIND_BY_UUID, uuid);
    }

    private Object getObjectByNamedQuery(String queryName, String param) {
        try {
            return em.createNamedQuery(queryName).setParameter(1, param).getSingleResult();
        } catch (NoResultException x) {
            log.warn("############ Object not found! "+queryName+":"+param);
            return null;
        }
    }
    @SuppressWarnings({ "rawtypes" })
    private List getObjectsByNamedQuery(String queryName, String paramName, String... params) {
        try {
            Query q = em.createNamedQuery(queryName);
            q.setParameter(paramName, Arrays.asList(params));
            return q.getResultList();
        } catch (NoResultException x) {
            log.warn("############ Object not found! "+queryName);
            return null;
        }
    }

    public void storeRegistryObjects(List<Identifiable> objects) throws XDSException {
        log.debug("##########Store list of objects:{}", objects);
        ArrayList<String> ids = new ArrayList<String>(objects.size());
        for (Identifiable i : objects) {ids.add(i.getId());}
        @SuppressWarnings("unchecked")
        List<String> uuids = (List<String>) em.createQuery("SELECT i.id FROM Identifiable i WHERE i.id IN :ids")
            .setParameter("ids", ids).getResultList();
        if (uuids.size() > 0) {
            throw new XDSException(XDSException.XDS_ERR_REGISTRY_METADATA_ERROR, 
                    "Objects with following UUIDs already exists! :"+uuids, null);
        }
        Identifiable obj;
        for (int i = 0, len = objects.size() ; i < len ; i++) {
            obj = objects.get(i);
            log.debug("##### store {}   class:{}", obj.getId(), obj.getClass().getSimpleName());
            em.persist(obj);
        }
    }

    public XADPatient getPatient(String pid, boolean createMissing) throws XDSException {
        XADPatient qryPat = new XADPatient(pid);
        if (!"ISO".equals(qryPat.getIssuerOfPatientID().getUniversalIdType())) {
            throw new XDSException(XDSException.XDS_ERR_UNKNOWN_PATID, "PatientID with wrong UniversalId type (must be ISO)! pid:"+pid, null);
        }
        try {
            return (XADPatient) em.createNamedQuery(XADPatient.FIND_PATIENT_BY_PID_AND_ISSUER)
                .setParameter(1, qryPat.getPatientID())
                .setParameter(2, qryPat.getIssuerOfPatientID().getUniversalID())
                .getSingleResult();
        } catch (NoResultException x) {
            if (createMissing) {
                log.warn("Unknown XAD Patient! Create missing Patient with pid:"+pid);
                qryPat.setIssuerOfPatientID(getOrCreateIssuerOfPID(qryPat.getIssuerOfPatientID()));
                em.persist(qryPat);
                return qryPat;
            } else {
                throw new XDSException(XDSException.XDS_ERR_UNKNOWN_PATID, "PatientID:"+pid, null);
            }
        }
    }
    
    private XADIssuer getOrCreateIssuerOfPID(XADIssuer issuer) {
        try {
            return (XADIssuer) em.createNamedQuery(XADIssuer.FIND_ISSUER_BY_UID)
            .setParameter(1, issuer.getUniversalID()).getSingleResult();
        } catch (NoResultException x) {
            log.info("Create new Issuer of PatientID:"+issuer);
            em.persist(issuer);
            return issuer;
        }            
    }

    public XDSCode getXDSCode(ClassificationType clType, boolean createMissing) throws XDSException {
        XDSCode code = new XDSCode(clType.getNodeRepresentation(),
                clType.getSlot().get(0).getValueList().getValue().get(0),
                clType.getName().getLocalizedString().get(0).getValue(),
                clType.getClassificationScheme());

        try {
            return (XDSCode) em.createNamedQuery(XDSCode.FIND_BY_CODE_VALUE)
                .setParameter(1, code.getCodeValue())
                .setParameter(2, code.getCodingSchemeDesignator())
                .setParameter(3, code.getCodeClassification())
                .getSingleResult();
        } catch (NoResultException x) {
            if (createMissing) {
                log.warn("Unknown XDS Code! Create missing XDSCode:"+code);
                em.persist(code);
                return code;
            } else {
                throw new XDSException(XDSException.XDS_ERR_REGISTRY_METADATA_ERROR, "Unknown code:"+code, null);
            }
        }
    }
    
    public void handleLifecycle(List<Identifiable> objs) throws XDSException {
        log.debug("##########Handle lifecycle");
        Identifiable obj;
        Association assoc;
        for (int i=0,len=objs.size() ; i < len ; i++) {
            obj = objs.get(i);
            if (obj instanceof Association) {
                assoc = (Association) obj;
                if (assoc.getAssocType() == null) {
                    throw new XDSException(XDSException.XDS_ERR_REGISTRY_METADATA_ERROR, "Missing AssociationType! Association:"+assoc.getId(), null);
                }
                log.info("### handle Association:"+assoc.getId()+" assocType:"+assoc.getAssocType().getId());
                if (XDSConstants.RPLC.equals(assoc.getAssocType().getId())) {
                    handleReplace(assoc);
                } else if (XDSConstants.XFRM_RPLC.equals(assoc.getAssocType().getId())) {
                    handleReplace(assoc);
                } else if (XDSConstants.APND.equals(assoc.getAssocType().getId())) {
                    getTargetAndcheckDeprecated(assoc);
                } else if (XDSConstants.XFRM.equals(assoc.getAssocType().getId())) {
                    getTargetAndcheckDeprecated(assoc);
                } else if (XDSConstants.HAS_MEMBER.equals(assoc.getAssocType().getId())) {
                    handleHasMember(assoc);
                }

            }
        }
        log.info("########## handle lifecycle finished! ");
    }

    private void handleReplace(Association assoc) throws XDSException {
        log.debug("##### Handle replace of document: source: {}  target:{}(status:{})",
                new Object[]{assoc.getSourceObject().getId(), assoc.getTargetObject().getId(),
                    assoc.getTargetObject().getStatus()} );
        RegistryObject ro = getTargetAndcheckDeprecated(assoc);
        ro.setStatus(XDSConstants.STATUS_DEPRECATED);
        em.merge(ro);
        deprecateAPNDandXFRM(ro);
       
        @SuppressWarnings("unchecked")
        List<Association> folderAssocs = (List<Association>) em.createQuery(
                "SELECT a FROM Association a WHERE ( a.assocType.id = '"+XDSConstants.HAS_MEMBER+
                "') AND a.targetObject=?1")
                .setParameter(1, ro).getResultList();
        for(Association fAssoc : folderAssocs) {
            if (fAssoc.getSourceObject() instanceof XDSFolder) {
                updateLastUpdateTime(fAssoc.getSourceObject());
                Association newAssoc = new Association();
                newAssoc.setId("urn:uuid:"+UUID.randomUUID().toString());
                newAssoc.setSourceObject(fAssoc.getSourceObject());//folder
                newAssoc.setTargetObject(assoc.getSourceObject());//new doc
                newAssoc.setAssocType(fAssoc.getAssocType());
                em.persist(newAssoc);
            }
        }
    }
    public void deprecateAPNDandXFRM(RegistryObject ro) {
        deprecateAPNDandXFRM(ro, em.createQuery(
                "SELECT a FROM Association a WHERE ( a.assocType.id = '"+XDSConstants.APND+
                "' OR a.assocType.id = '"+XDSConstants.XFRM+"') AND a.targetObject=?1"));
    }
    private void deprecateAPNDandXFRM(RegistryObject ro, Query query) {
        @SuppressWarnings("unchecked")
        List<Association> assocs = (List<Association>) query
            .setParameter(1, ro).getResultList();
        RegistryObject ro1;
        for (int i = 0, len=assocs.size(); i < len ; i++) {
            ro1 = assocs.get(i).getSourceObject();
            ro1.setStatus(XDSConstants.STATUS_DEPRECATED);
            log.info("Deprecate object "+ro1.getId()+" which is a '"+assocs.get(i).getAssocType().getId()+"' of "+ro.getId());
            em.merge(ro1);
            deprecateAPNDandXFRM(ro1, query);
        }
    }

    private RegistryObject getTargetAndcheckDeprecated(Association assoc) throws XDSException {
        RegistryObject ro = assoc.getTargetObject();
        if (XDSConstants.STATUS_DEPRECATED.equals(ro.getStatus())) {
            throw new XDSException(XDSException.XDS_ERR_REGISTRY_DEPRECATED_DOC_ERROR, 
                    "Target RegistryObject is deprecated! Association:"+assoc, null);
        }
        return ro;
    }

    private void handleHasMember(Association assoc) {
        log.info("########## HANDLE HasMember!");
        log.info("########## HANDLE HasMember! source:"+assoc.getSourceObject());
        if(assoc.getSourceObject() instanceof XDSFolder) {
            log.info("########## HANDLE HasMember! updateLastUpdateTime");
            updateLastUpdateTime(assoc.getSourceObject());
        }
        log.info("########## HANDLE HasMember finished!");
    }

    public void updateLastUpdateTime(RegistryObject ro) {
        log.info("########## updateLastUpdateTime!");
        List<Slot> slots = ro.getSlots();
        log.info("########## updateLastUpdateTime! slots:"+slots);
        for (Slot slot : slots) {
            log.info("########## updateLastUpdateTime! slot Name:"+slot.getName());
            if (XDSConstants.SLOT_NAME_LAST_UPDATE_TIME.equals(slot.getName())) {
                log.info("########## updateLastUpdateTime! slot setValue");
                slot.setValue(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
                log.info("########## updateLastUpdateTime! persist:");
                em.merge(slot);
                log.info("########## updateLastUpdateTime! persist done:"+slot.getName());
            }
        }
        log.info("########## updateLastUpdateTime! done");
    }

    public void addError(RegistryResponseType rsp, XDSException x) {
        rsp.setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        try {
            RegistryErrorList errList = rsp.getRegistryErrorList();
            if (errList == null) {
                errList = factory.createRegistryErrorList();
                rsp.setRegistryErrorList( errList );
            }
            List<RegistryError> errors = errList.getRegistryError();
            RegistryError error = getRegistryError(x);
            errors.add(error);
        } catch (JAXBException e) {
            log.error("Failed to set ErrorList in response!", e);
        }
    }
    public void addError(AdhocQueryResponse rsp, XDSException x) {
        rsp.setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        try {
            RegistryErrorList errList = rsp.getRegistryErrorList();
            if (errList == null) {
                errList = factory.createRegistryErrorList();
                rsp.setRegistryErrorList( errList );
            }
            List<RegistryError> errors = errList.getRegistryError();
            RegistryError error = getRegistryError(x);
            errors.add(error);
        } catch (JAXBException e) {
            log.error("Failed to set ErrorList in response!", e);
        }
    }
    
    private RegistryError getRegistryError(XDSException xdsException) throws JAXBException {
        RegistryError error = factory.createRegistryError();
        error.setErrorCode(xdsException.getErrorCode());
        error.setCodeContext(xdsException.getMessage());
        error.setSeverity(xdsException.getSeverity());
        error.setLocation(xdsException.getLocation());
        return error;
    }

    
    public EntityManager getEntityManager() {
        return em;
    }
}
