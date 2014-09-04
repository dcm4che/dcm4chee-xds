package org.dcm4chee.xds2.registry.ws;
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



import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.dcm4che3.net.Device;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.conf.XdsRegistry;
import org.dcm4chee.xds2.infoset.rim.AssociationType1;
import org.dcm4chee.xds2.infoset.rim.ClassificationType;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.ObjectRefListType;
import org.dcm4chee.xds2.infoset.rim.ObjectRefType;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectType;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.infoset.rim.RemoveObjectsRequest;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.persistence.RegistryObject;
import org.dcm4chee.xds2.persistence.RegistryPackage;
import org.dcm4chee.xds2.persistence.XDSDocumentEntry;
import org.dcm4chee.xds2.registry.AuditTestManager;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */

@RunWith(Arquillian.class)
public class RegisterDocumentSetTest {
    private static final int NUMBER_OF_TEST_METHODS = XDSTestUtil.getNumberOfTestMethods(RegisterDocumentSetTest.class);

    private final static Logger log = LoggerFactory.getLogger(RegisterDocumentSetTest.class);

    private static int testCount = 0;
    
    @Deployment
    public static WebArchive createDeployment() {
           WebArchive wa = XDSTestUtil.createDeploymentArchive(RegisterDocumentSetTest.class)
            .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/RegisterOneDocumentConcurrent1.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/RegisterOneDocumentConcurrent1.xml") 
            .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/RegisterOneDocumentConcurrent2.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/RegisterOneDocumentConcurrent2.xml") 
            .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/RegisterOneDocument.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/RegisterOneDocument.xml") 
            .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/RegisterAssocBeforeDoc.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/RegisterAssocBeforeDoc.xml") 
            .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/RegisterTwoDocuments.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/RegisterTwoDocuments.xml")
            .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/RegisterTwoDocumentsForDeletion.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/RegisterTwoDocumentsForDeletion.xml")
            .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/ResubmitInitialDoc.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/ResubmitInitialDoc.xml") 
            .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/ResubmitWrongHash.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/ResubmitWrongHash.xml") 
            .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/CreateFolder.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/CreateFolder.xml") 
            .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/CreateFolderWithDocument.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/CreateFolderWithDocument.xml");

        return wa;
    }

    @EJB
    private XdsRegistryBeanForTesting session;

    @EJB
    private XDSRegistryTestBeanI testSession;

    @EJB
    private XDSRegistryTestBeanI testSession2;
    
    @Inject
    Device xdsDevice;
   
    // Audit logger	testing
    @Before
    public void prepareAuditLogger() {
    	AuditTestManager.prepareAuditLogger(); 
    }
    @After
    public void checkAudits() 
    {
    	AuditTestManager.checkAudits();
    }
    
    
    @Before
    public void prepare() {
        if (testCount++ == 0) {
            XDSTestUtil.prepareTests(session, log);
        }
    }

    @After
    public void clearDB() {
        if (testCount == NUMBER_OF_TEST_METHODS) {
            XDSTestUtil.clearDB(testSession, log);
        }
    }

    @Test
    public void registerOneDoc() throws Exception {
        doRegisterDocumentTest("RegisterOneDocument");
    }
    
    /**
     * Check correct handling if Association is before DocumentEntry.
     * 
     * @throws Exception
     */
    @Test
    public void registerAssocBeforeDoc() throws Exception {
        doRegisterDocumentTest("RegisterAssocBeforeDoc");
    }

    @Test
    public void registerTwoDocs() throws Exception {
        doRegisterDocumentTest("RegisterTwoDocuments");
    }
    @Test
    public void createFolder() throws Exception {
        doRegisterDocumentTest("CreateFolder");
    }

    @Test
    public void registerOneDocWithFolder() throws Exception {
        doRegisterDocumentTest("CreateFolderWithDocument");
    }

    @Test
    public void checkErrorNonIdenticalHash() throws Exception {
        log.info("\n############################# TEST: check resubmit document with different hash ############################");
        SubmitObjectsRequest req = XDSTestUtil.getSubmitObjectsRequest("ResubmitInitialDoc.xml");
        RegistryResponseType rsp = session.documentRegistryRegisterDocumentSetB(req);
        assertEquals("First Document Submission: response status", XDSConstants.XDS_B_STATUS_SUCCESS, rsp.getStatus());
        RegistryObjectType obj = (RegistryObjectType) req.getRegistryObjectList().getIdentifiable().get(0).getValue();
        String hash = XDSTestUtil.getSlotValue(obj.getSlot(), XDSConstants.SLOT_NAME_HASH);
        req = XDSTestUtil.getSubmitObjectsRequest("ResubmitWrongHash.xml");
        doRegisterDocumentAndCheck(req, XDSException.XDS_ERR_NON_IDENTICAL_HASH);
        obj = (RegistryObjectType) req.getRegistryObjectList().getIdentifiable().get(0).getValue();
        XDSTestUtil.setSlotValue(obj.getSlot(), XDSConstants.SLOT_NAME_HASH, hash);
        rsp = session.documentRegistryRegisterDocumentSetB(req);
        assertEquals("Resubmission with same hash: response status", XDSConstants.XDS_B_STATUS_SUCCESS, rsp.getStatus());
        
        AuditTestManager.expectNumberOfMessages(3);
    }

    private void doRegisterDocumentTest(String testName) throws Exception {
        log.info("\n############################# TEST: "+testName+" ############################");
        long t1 = System.currentTimeMillis();
        doRegisterDocumentAndCheck(XDSTestUtil.getSubmitObjectsRequest(testName+".xml"), null);
        log.info("\n###### "+testName+" done in "+(System.currentTimeMillis()-t1)+"ms ######");
    }

    
    private void doRegisterDocumentAndCheck(SubmitObjectsRequest req, String errorCode) {
        RegistryResponseType rsp = null;
        try {
            rsp = session.documentRegistryRegisterDocumentSetB(req);
        } catch (Exception x) {
            fail("Register document failed unexpected! Error:"+x);
        }
        if (errorCode == null) {
            if (XDSConstants.XDS_B_STATUS_FAILURE.equals(rsp.getStatus())) {
                fail("Register document failed! Error:"+XDSTestUtil.toErrorMsg(rsp));
            }
        } else {
            if (!XDSConstants.XDS_B_STATUS_FAILURE.equals(rsp.getStatus())) {
                fail("Register document should fail with error code:"+errorCode);
            }
            String error = rsp.getRegistryErrorList().getRegistryError().get(0).getErrorCode();
            assertEquals("Error code:", errorCode, error);
            return;
        }
        IdentifiableType obj;
        for (JAXBElement<? extends IdentifiableType> jaxbObj : req.getRegistryObjectList().getIdentifiable()) {
            obj = jaxbObj.getValue();
            if (obj instanceof ExtrinsicObjectType) {
                checkExtrinsicObjectType((ExtrinsicObjectType)obj);
            } else if (obj instanceof RegistryPackageType) {
                checkRegistryPackage((RegistryPackageType)obj);
            } else if (obj instanceof ClassificationType) {
                try {
                    testSession.checkClassification((ClassificationType)obj);
                } catch (XDSRegistryTestBeanException x) {
                    throw x.getAssertionEror();
                }
            } else if (obj instanceof AssociationType1) {
                try {
                    testSession.checkAssociation((AssociationType1)obj);
                } catch (XDSRegistryTestBeanException x) {
                    throw x.getAssertionEror();
                }
            } else {
                fail("Unchecked RegistryObject! obj:"+obj);
            }
        }
    }

    private void checkExtrinsicObjectType(ExtrinsicObjectType obj) {
        String msgPrefix = "ExtrinsicObject "+obj.getId();
        String uniqueId = XDSTestUtil.getExternalIdentifierValue(obj.getExternalIdentifier(), 
                XDSConstants.UUID_XDSDocumentEntry_uniqueId);
        if (uniqueId != null) {
            List<XDSDocumentEntry> docs = session.getDocumentEntriesByUniqueId(uniqueId);
            assertFalse(msgPrefix+" not found by uniqueId! :"+uniqueId, docs.isEmpty());
            try {
                testSession.checkExtrinsicObjectType(obj);
            } catch (XDSRegistryTestBeanException x) {
                throw x.getAssertionEror();
            }
            for (XDSDocumentEntry doc : docs)
            	assertNotNull("Check SourcePatientID", doc.getSourcePatient());
        } else {
            fail(msgPrefix+" is not an XDSDocumentEntry! (missing uniqueId)");  
        }
    }

    private void checkRegistryPackage(RegistryPackageType obj) {
        String uniqueId = XDSTestUtil.getExternalIdentifierValue(obj.getExternalIdentifier(), XDSConstants.UUID_XDSSubmissionSet_uniqueId);
        if (uniqueId != null) {
            RegistryPackage rp = session.getSubmissionSetByUniqueId(uniqueId);
            assertNotNull("SubmissionSet not found by uniqueId! :"+uniqueId, rp);
            try {
                testSession.checkRegistryPackage(obj, true);
            } catch (XDSRegistryTestBeanException x) {
                throw x.getAssertionEror();
            }
        } else {
            uniqueId = XDSTestUtil.getExternalIdentifierValue(obj.getExternalIdentifier(), XDSConstants.UUID_XDSFolder_uniqueId);
            if (uniqueId != null) {
                RegistryPackage rp = ((XDSRegistryBean)session).getFolderByUniqueId(uniqueId);
                assertNotNull("Folder not found by uniqueId! :"+uniqueId, rp);
                try {
                    testSession.checkRegistryPackage(obj, false);
                } catch (XDSRegistryTestBeanException x) {
                    throw x.getAssertionEror();
                }
            } else {
              fail("RegistryPackage is neither SubmissionSet nor Folder!");  
            }
        }
    }
    
    
    @Test
    public void checkDelete() throws JAXBException {
        log.info("\n############################# TEST: Deleting entities ############################");
        
        Long total;
        total = testSession.getTotalIdentifiablesCount();
        log.info("Total identifiables before registering: {}",total);
        
        SubmitObjectsRequest sor = XDSTestUtil.getSubmitObjectsRequest("RegisterTwoDocumentsForDeletion.xml");
        RegistryResponseType rsp = session.documentRegistryRegisterDocumentSetB(sor);
        
        List<String> uuids = new ArrayList<String>();
        
        for (JAXBElement<? extends IdentifiableType> elem : sor.getRegistryObjectList().getIdentifiable()) {
            String uuid = elem.getValue().getId();
            if (elem.getValue().getClass() != ClassificationType.class)
                uuids.add(uuid);
        }
        
        // check that identifiables stored and generate the delete request
        
        log.info("Total identifiables after registering: {}",testSession.getTotalIdentifiablesCount());

        RemoveObjectsRequest ror = new RemoveObjectsRequest();
        ObjectRefListType lt = new ObjectRefListType();

        for (String uuid : uuids) {
            RegistryObject ro = testSession.getRegistryObjectByUUID(uuid);
            if (ro == null) {
                log.warn("The identifiable was supposed to be stored but not found! - uuid "+uuid);
            } else {
                log.info("Stored identifiable id {}, type {}", uuid, ro.getObjectType());
            }
            
            ObjectRefType oref = new ObjectRefType();
            oref.setId(uuid);
            lt.getObjectRef().add(oref);
        }
        
        ror.setObjectRefList(lt);
        
        
        // do the deletion   
        
        session.deleteObjects(ror);
        
        // check
        
        for (String uuid : uuids) {
            RegistryObject ro = testSession.getRegistryObjectByUUID(uuid);
            assertEquals("Identifiable "+uuid+" should have been deleted", ro, null);
        }
        
        assertEquals("Amount of identifiables must be equal before adding and after deleting objects", total, testSession.getTotalIdentifiablesCount());
    }
    
    @Test
    public void checkConcurrentRegister() throws JAXBException, InterruptedException {
        
        // allow missing pids
        xdsDevice.getDeviceExtension(XdsRegistry.class).setCreateMissingPIDs(true);
        
        try {
            
            log.info("\n############################# TEST: Concurrent registering ############################");
    
            int threads = 2;
            
            final Semaphore masterSemaphore = new Semaphore(0);
            final Semaphore childrenSemaphore = new Semaphore(0);
    
            
            final SubmitObjectsRequest sor1 = XDSTestUtil.getSubmitObjectsRequest("RegisterOneDocumentConcurrent1.xml");
            final SubmitObjectsRequest sor2 = XDSTestUtil.getSubmitObjectsRequest("RegisterOneDocumentConcurrent2.xml");
            
            final Set<Exception> errors = Collections.synchronizedSet(new HashSet<Exception>());
            
            // launch concurrent modifications
            Thread thread1 = new Thread() {
              @Override
                public void run() {
                    try {
                        testSession.concurrentRegister(masterSemaphore, childrenSemaphore, sor1);
                    } catch (Exception e) {
                        log.error("Error in a concurrent registrator thread 1",e);
                        masterSemaphore.release();
                        errors.add(e);
                    } finally {
                        log.info("Finished concurrent registrator thread 1");
                        masterSemaphore.release();
                    }
                    super.run();
                }  
            };
            
            
            Thread thread2 = new Thread() {
                @Override
                  public void run() {
                      try {
                          testSession2.concurrentRegister(masterSemaphore, childrenSemaphore, sor2);
                      } catch (Exception e) {
                          log.error("Error in a concurrent registrator thread 2",e);
                          masterSemaphore.release();
                          errors.add(e);
                      } finally {
                          log.info("Finished concurrent registrator thread 2");
                          masterSemaphore.release();
                      }
                      super.run();
                  }  
              };
              
            thread1.start();
            thread2.start();

            int wait = 10;
            
            // wait until all of them are ready to commit
            // by trying to acquire N master's semaphore permits, where each modifier will release one permit
            if (!masterSemaphore.tryAcquire(threads, wait, TimeUnit.SECONDS)) throw new RuntimeException("Child threads got broken somewhere");
    
            // let the children finish the job, each of them will need 1 permit
            childrenSemaphore.release(threads);
            
            // wait until the transactions are committed
            if (!masterSemaphore.tryAcquire(threads, wait, TimeUnit.SECONDS)) throw new RuntimeException("Child threads got broken somewhere");
    
            // check how many patients were added
            assertEquals("Only one patient record should end up in the database",
                    1 , testSession.getConcurrentPatientRecordsNum());
            
            AuditTestManager.expectNumberOfMessages(2);
            
            if (!errors.isEmpty()) {
                
                String s = "";
                try {
                    throw new RuntimeException("There were errors in threads:");
                } finally {
                    for (Exception e : errors) e.printStackTrace();
                }
            }

        } finally {
            xdsDevice.getDeviceExtension(XdsRegistry.class).setCreateMissingPIDs(false);
        }
        
    }
    
}