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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.infoset.rim.AssociationType1;
import org.dcm4chee.xds2.infoset.rim.ClassificationType;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.InternationalStringType;
import org.dcm4chee.xds2.infoset.rim.LocalizedStringType;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryPortType;
import org.dcm4chee.xds2.persistence.Association;
import org.dcm4chee.xds2.persistence.Identifiable;
import org.dcm4chee.xds2.persistence.InternationalString;
import org.dcm4chee.xds2.persistence.QRegistryObject;
import org.dcm4chee.xds2.persistence.RegistryObject;
import org.dcm4chee.xds2.persistence.RegistryPackage;
import org.dcm4chee.xds2.persistence.Slot;
import org.dcm4chee.xds2.persistence.XDSDocumentEntry;
import org.dcm4chee.xds2.persistence.XDSFolder;
import org.dcm4chee.xds2.persistence.XDSSubmissionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.query.jpa.impl.JPAQuery;

@Stateless
public class XDSRegistryTestBean implements XDSRegistryTestBeanI {

    @EJB
    private DocumentRegistryPortType registryBean;

    
    @PersistenceContext(unitName = "dcm4chee-xds")
    private EntityManager em;

    private static Logger log = LoggerFactory.getLogger(XDSRegistryTestBean.class);
    
    public XDSRegistryTestBean() {
    }
    
    /* (non-Javadoc)
     * @see org.dcm4chee.xds2.registry.ws.XDSRegistryTestBeanI#getRegistryObjectByUUID(java.lang.String)
     */
    @Override
    public RegistryObject getRegistryObjectByUUID(String id) {
        JPAQuery query = new JPAQuery(em);
        QRegistryObject registryObject = QRegistryObject.registryObject;
        RegistryObject obj = query.from(registryObject).where(registryObject.id.eq(id)).uniqueResult(registryObject);
        return obj;
    }

    /* (non-Javadoc)
     * @see org.dcm4chee.xds2.registry.ws.XDSRegistryTestBeanI#getEm()
     */
    @Override
    public EntityManager getEm() {
        return em;
    }

    /* (non-Javadoc)
     * @see org.dcm4chee.xds2.registry.ws.XDSRegistryTestBeanI#getDocumentEntryByUUID(java.lang.String)
     */
    @Override
    public XDSDocumentEntry getDocumentEntryByUUID(String uuid) {
        return (XDSDocumentEntry) getObjectByNamedQuery(XDSDocumentEntry.FIND_BY_UUID, uuid);
    }

    /* (non-Javadoc)
     * @see org.dcm4chee.xds2.registry.ws.XDSRegistryTestBeanI#getSubmissionSetByUUID(java.lang.String)
     */
    @Override
    public XDSSubmissionSet getSubmissionSetByUUID(String uuid) {
        return (XDSSubmissionSet) getObjectByNamedQuery(XDSSubmissionSet.FIND_BY_UUID, uuid);
    }

    /* (non-Javadoc)
     * @see org.dcm4chee.xds2.registry.ws.XDSRegistryTestBeanI#getFolderByUUID(java.lang.String)
     */
    @Override
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
    
    /* (non-Javadoc)
     * @see org.dcm4chee.xds2.registry.ws.XDSRegistryTestBeanI#checkExtrinsicObjectType(org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType)
     */
    @Override
    public void checkExtrinsicObjectType(ExtrinsicObjectType obj) throws XDSRegistryTestBeanException {
        try {
            String msgPrefix = "ExtrinsicObject "+obj.getId();
            XDSDocumentEntry doc = getDocumentEntryByUUID(obj.getId());
            assertNotNull(msgPrefix+" not found by UUID!", doc);
            checkSlots(obj.getSlot(), doc.getSlots(), msgPrefix);
            /*checkClassificationList(msgPrefix, obj.getClassification(), doc.getClassifications());
            checkInternationalString(obj.getName(), doc.getName(), msgPrefix+": Name");
            checkInternationalString(obj.getDescription(), doc.getDescription(), msgPrefix+": Description");
            checkExternalIdentifier(obj.getExternalIdentifier(), doc.getExternalIdentifiers(), msgPrefix);*/
            
            //TODO: deepEquals
            
            
        } catch (AssertionError error) {
            throw new XDSRegistryTestBeanException(error);
        }
    }

    /* (non-Javadoc)
     * @see org.dcm4chee.xds2.registry.ws.XDSRegistryTestBeanI#checkRegistryPackage(org.dcm4chee.xds2.infoset.rim.RegistryPackageType, boolean)
     */
    @Override
    public void checkRegistryPackage(RegistryPackageType obj, boolean isSubmissionSet) throws XDSRegistryTestBeanException {
        try {
            if (isSubmissionSet) {
                RegistryPackage rp = getSubmissionSetByUUID(obj.getId());
                assertNotNull("SubmissionSet not found by UUID!", rp);
            } else {
                RegistryPackage rp = getFolderByUUID(obj.getId());
                assertNotNull("Folder not found by UUID!", rp);
            }
        } catch (AssertionError error) {
            throw new XDSRegistryTestBeanException(error);
        }
    }

    /* (non-Javadoc)
     * @see org.dcm4chee.xds2.registry.ws.XDSRegistryTestBeanI#checkClassification(org.dcm4chee.xds2.infoset.rim.ClassificationType)
     */
    @Override
    public void checkClassification(ClassificationType obj) throws XDSRegistryTestBeanException {
        try {
            /* TODO: DB_RESTRUCT Classification cl = (Classification) getRegistryObjectByUUID(obj.getId());
            assertNotNull("Classification not found! :"+obj.getId(), cl);
            checkClassification(obj, cl); */
        } catch (AssertionError error) {
            throw new XDSRegistryTestBeanException(error);
        }
    }

    /* (non-Javadoc)
     * @see org.dcm4chee.xds2.registry.ws.XDSRegistryTestBeanI#checkAssociation(org.dcm4chee.xds2.infoset.rim.AssociationType1)
     */
    @Override
    public void checkAssociation(AssociationType1 obj) throws XDSRegistryTestBeanException {
        try {
            String msgPrefix = "Association "+obj.getId();
            Association assoc = (Association) getRegistryObjectByUUID(obj.getId());
            assertNotNull(msgPrefix+" Association not found! :"+obj.getId(), assoc);
            assertEquals(msgPrefix+" AssociationType:", 
                    obj.getAssociationType(), assoc.getAssocType().getId());
            assertEquals(msgPrefix+" SourceObject", 
                    obj.getSourceObject(), assoc.getSourceObject() == null ? null : assoc.getSourceObject().getId());
            assertEquals(msgPrefix+" targetObject:", 
                    obj.getTargetObject(), assoc.getTargetObject() == null ? null : assoc.getTargetObject().getId());
            assertEquals(msgPrefix+" ObjectType:", obj.getObjectType(), assoc.getObjectType());
        } catch (AssertionError error) {
            throw new XDSRegistryTestBeanException(error);
        }
    }
    
// Clearing Database from test data
    
    /* (non-Javadoc)
     * @see org.dcm4chee.xds2.registry.ws.XDSRegistryTestBeanI#removeTestPatients(java.lang.String)
     */
    @Override
    public void removeTestPatients(String... patIds) {
        Query q = em.createQuery("DELETE FROM XADPatient pat WHERE pat.patientID IN (:patIds)");
        q.setParameter("patIds", Arrays.asList(patIds));
        q.executeUpdate();
    }
    /* (non-Javadoc)
     * @see org.dcm4chee.xds2.registry.ws.XDSRegistryTestBeanI#removeTestIssuerByNamespaceId(java.lang.String)
     */
    @Override
    public void removeTestIssuerByNamespaceId(String namespaceId) {
        Query q = em.createQuery("DELETE FROM XADIssuer i WHERE i.namespaceID = ?1");
        q.setParameter(1, namespaceId);
        q.executeUpdate();
    }
    /* (non-Javadoc)
     * @see org.dcm4chee.xds2.registry.ws.XDSRegistryTestBeanI#removeAllIdentifiables(java.lang.String)
     */
    @Override
    public void removeAllIdentifiables(String baseID) {
        @SuppressWarnings("unchecked")
        List<Identifiable> l = (List<Identifiable>) em.createQuery("SELECT i FROM Identifiable i WHERE i.id LIKE ?1")
        .setParameter(1, baseID+"%").getResultList();
        for (int i = 0, len = l.size() ; i < len ; i++)
            em.remove(l.get(i));
    }
    /* (non-Javadoc)
     * @see org.dcm4chee.xds2.registry.ws.XDSRegistryTestBeanI#removeAllIdentifiables(java.util.Set)
     */
    @Override
    public void removeAllIdentifiables(Set<String> ids) {
        Query q = em.createQuery("UPDATE ClassificationNode n SET n.parent = null WHERE n.id IN (:ids)");
        q.setParameter("ids", ids);
        q.executeUpdate();
        em.createQuery("DELETE FROM Name n WHERE n.parent.id IN (:ids)").setParameter("ids", ids);
        em.createQuery("DELETE FROM Description d WHERE d.parent.id IN (:ids)").setParameter("ids", ids);
        @SuppressWarnings("unchecked")
        List<Identifiable> l = (List<Identifiable>) em.createQuery("SELECT i FROM Identifiable i WHERE i.id IN (:ids) ORDER BY i.pk DESC")
        .setParameter("ids", ids).getResultList();
        for (int i = 0, len = l.size() ; i < len ; i++)
            em.remove(l.get(i));
            
    }
    
    /* (non-Javadoc)
     * @see org.dcm4chee.xds2.registry.ws.XDSRegistryTestBeanI#removeXDSCodes()
     */
    @Override
    public void removeXDSCodes() {
        Query q = em.createQuery("DELETE FROM XDSCode c WHERE c.codingSchemeDesignator LIKE ?1");
        q.setParameter(1, "dcm4che %");
        q.executeUpdate();
    }
    
    /* (non-Javadoc)
     * @see org.dcm4chee.xds2.registry.ws.XDSRegistryTestBeanI#getTotalIdentifiablesCount()
     */
    @Override
    public Long getTotalIdentifiablesCount() {
        return (Long) em.createQuery("SELECT count(i) FROM Identifiable i").getResultList().get(0);
    }

    
    
    /* (non-Javadoc)
     * @see org.dcm4chee.xds2.registry.ws.XDSRegistryTestBeanI#getConcurrentPatient(java.util.concurrent.Semaphore, java.util.concurrent.Semaphore, org.dcm4chee.xds2.registry.ws.XDSRegistryBean)
     */
    @Override
    public void concurrentRegister(Semaphore masterSemaphore, Semaphore childrenSemaphore, SubmitObjectsRequest sor) throws InterruptedException, XDSException {
        
       try {
        // do register
        RegistryResponseType rsp = registryBean.documentRegistryRegisterDocumentSetB(sor);
        
        assertEquals("Status should be success", XDSConstants.XDS_B_STATUS_SUCCESS, rsp.getStatus());
        
        // tell the master we're done
        masterSemaphore.release();
        log.info("One permit for master released");
        
        // wait for others before committing
        childrenSemaphore.tryAcquire(1, TimeUnit.SECONDS);
        log.info("Got permit for committing");

       } catch (InterruptedException e) {
           throw new RuntimeException("Was interrupted!");
       } 
    }
    
    
    /* (non-Javadoc)
     * @see org.dcm4chee.xds2.registry.ws.XDSRegistryTestBeanI#getConcurrentPatientRecordsNum()
     */
    @Override
    public long getConcurrentPatientRecordsNum() {
       return  (long) em.createQuery("SELECT count(p) FROM XADPatient p WHERE p.patientID = (:pids)").setParameter("pids", XDSTestUtil.CONCURRENT_PATID).getSingleResult();
    }
    
// Helper for compare RegistryObjects of jaxb and persistence objects    

    /* TODO: DB_RESTRUCT private void checkClassification(ClassificationType obj, Classification cl) throws XDSRegistryTestBeanException {
        try {
            String msgPrefix = "Classification "+obj.getId();
            if (cl.getClassificationNode() == null)
                msgPrefix += " ClassificationScheme"+cl.getClassificationScheme();
            else
                msgPrefix += " ClassificationNode:"+cl.getClassificationNode();
            assertEquals(msgPrefix+" ClassificationNode:", 
                    obj.getClassificationNode(), cl.getClassificationNode() == null ? null : cl.getClassificationNode().getId());
            assertEquals(msgPrefix+" getClassificationScheme:", 
                    obj.getClassificationScheme(), cl.getClassificationScheme() == null ? null : cl.getClassificationScheme().getId());
            assertEquals(msgPrefix+" ClassifiedObject:", obj.getClassifiedObject(), cl.getClassifiedObject().getId());
            assertEquals(msgPrefix+" ObjectType:", obj.getObjectType(), cl.getObjectType());
            checkSlots(obj.getSlot(), cl.getSlots(), msgPrefix);
            //TODO: checkInternationalString(obj.getName(), cl.getName(), msgPrefix);
        } catch (AssertionError error) {
            throw new XDSRegistryTestBeanException(error);
        }
    }*/

    /* TODO: DB_RESTRUCT private void checkClassificationList(String msgPrefix,
            List<ClassificationType> cltList, Set<Classification> clList)
            throws XDSRegistryTestBeanException {
        assertEquals(msgPrefix+" Number of Classifications:", cltList.size(), clList.size());
        ClassificationType clt;
        String cltNode, cltScheme;
        cltList: for (int i = 0, len = cltList.size() ; i < len ; i++) {
            clt = cltList.get(i);
            cltNode = clt.getClassificationNode();
            cltScheme = clt.getClassificationScheme();
            StringBuilder sb = new StringBuilder();
            for (Classification cl : clList) {
                if ((cltNode != null && cltNode.equals(cl.getClassificationNode().getId())) ||
                    (cltScheme != null && cltScheme.equals(cl.getClassificationScheme().getId()))) {
                    try {
                        assertEquals("ClassifiedObject:", clt.getClassifiedObject(), cl.getClassifiedObject().getId());
                        assertEquals("ObjectType:", clt.getObjectType(), cl.getObjectType());
                        checkSlots(clt.getSlot(), cl.getSlots(), "");
                        checkInternationalString(clt.getName(), cl.getName(), ": Name");
                        checkInternationalString(clt.getDescription(), cl.getDescription(), ": Description");
                        continue cltList;
                    } catch (AssertionError e) {
                        sb.append(" ").append(e.getMessage());
                    }
                }
            }
            String postfix = cltNode == null ? "ClassificationScheme:"+cltScheme : "ClassificationNode:"+cltNode;
            if (sb.length() > 0) {
                fail(msgPrefix+" No matching Classification ("+postfix+") found:"+sb);
            } 
            fail(msgPrefix+" Missing Classification with "+postfix);
        }
    } 

    private void checkExternalIdentifier(List<ExternalIdentifierType> externalIdentifier,
            Set<ExternalIdentifier> externalIdentifiers, String msgPrefix) throws XDSRegistryTestBeanException {
        loop: for (ExternalIdentifierType eiType : externalIdentifier) {
            String idScheme = eiType.getIdentificationScheme();
            for (ExternalIdentifier ei : externalIdentifiers) {
                if (idScheme.equals(ei.getIdentificationScheme().getId())) {
                    assertEquals(msgPrefix+": ExternalIdentifier ObjectType:", eiType.getObjectType(), ei.getObjectType());
                    assertEquals(msgPrefix+": ExternalIdentifier Value:", eiType.getValue(), ei.getValue());
                    assertEquals(msgPrefix+": ExternalIdentifier registryObject:", eiType.getRegistryObject(), ei.getRegistryObject().getId());
                    checkSlots(eiType.getSlot(), ei.getSlots(), msgPrefix);
                    checkClassificationList(msgPrefix, eiType.getClassification(), ei.getClassifications());
                    checkInternationalString(eiType.getName(), ei.getName(), msgPrefix+": Name");
                    checkInternationalString(eiType.getDescription(), ei.getDescription(), msgPrefix+": Description");
                    checkExternalIdentifier(eiType.getExternalIdentifier(), ei.getExternalIdentifiers(), msgPrefix);
                    continue loop;
                }
            }
            fail(msgPrefix+" Missing ExternalIdentifier! IdentificationScheme:"+idScheme);
        }
        
    }*/

    private void checkSlots(List<SlotType1> slotTypes, List<Slot> slots, String msgPrefix) {
        if (slotTypes == null && slots == null) {
            return; 
        } else if (slotTypes == null) {
            fail(msgPrefix+" Additional Slots stored! slots:"+slots);
        } else if (slots == null) {
            fail(msgPrefix+" Slots are missing! slotTypes:"+slotTypes);
        }
        List<String> slotTypeValues, slotValues;
        String name, value;
        for (int i = 0, len = slotTypes.size() ; i < len ; i++) {
            name = slotTypes.get(i).getName();
            slotValues = XDSValidator.getSlotValues(slots, name);
            assertNotNull(msgPrefix+" Missing SlotValues for "+name);
            slotTypeValues = XDSValidator.getSlotTypeValues(slotTypes, slotTypes.get(i).getName());
            assertEquals(msgPrefix+" Number of Slot values in Slot "+name, slotTypeValues.size(), slotValues.size());
            slotTypeValue: for (int j = 0, jLen=slotTypeValues.size() ; j  < jLen ; j++) {
                value = slotTypeValues.get(j);
                for (int k = 0, kLen = slotValues.size() ; k < kLen ; k++) {
                    if (slotValues.get(k).equals(value)) {
                        continue slotTypeValue;
                    }
                }
                fail(msgPrefix+" Missing Slot value:"+value+" in Slot "+name);
            }
        }
        
    }

    private void checkInternationalString(InternationalStringType is1, Set<? extends InternationalString> name2, String msgPrefix) {
        if (is1 == null && (name2 == null || name2.isEmpty())) {
            return; 
        } else if (is1 == null) { 
            fail(msgPrefix+" Found additional:"+name2);
        } else if (name2 == null) {
            fail(msgPrefix+" Missing:"+is1);
        }
        List<LocalizedStringType> localizedStrings = is1.getLocalizedString();
        String lsCharset, nameCharset;
        loop: for (LocalizedStringType lsType : localizedStrings) {
            for (InternationalString name : name2) {
                if (name.getValue().equals(lsType.getValue())) {
                    lsCharset = lsType.getCharset();
                    nameCharset = name.getCharset();
                    if (lsCharset != null && nameCharset != null) {
                        assertEquals(msgPrefix+" Different 'charset' attribute:", lsCharset, nameCharset);
                    } else if (lsCharset == null) {
                        fail(msgPrefix+" Additional 'charset' attribute:"+nameCharset);
                    } else if (nameCharset == null) {
                        fail(msgPrefix+" Missing 'charset' attribute:"+lsCharset);
                    }
                    String lsLang = lsType.getLang();
                    String nameLang = name.getLang();
                    if (lsLang != null && nameLang != null) {
                        assertEquals(msgPrefix+" Different 'lang' attribute:", lsLang, nameLang);
                    } else if (lsLang == null) {
                        fail(msgPrefix+" Additional 'lang' attribute:"+nameLang);
                    } else if (nameLang == null) {
                        fail(msgPrefix+" Missing 'lang' attribute:"+lsLang);
                    }
                    continue loop;
                }
            }
            fail(msgPrefix+" Missing:"+lsType.getValue());
        }
    }
    
    
}
