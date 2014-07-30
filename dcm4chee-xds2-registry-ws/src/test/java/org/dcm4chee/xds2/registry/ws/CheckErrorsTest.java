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


import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.infoset.rim.*;
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

import javax.ejb.EJB;
import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */
@RunWith(Arquillian.class)
public class CheckErrorsTest {
    private static final int NUMBER_OF_TEST_METHODS = 
        XDSTestUtil.getNumberOfTestMethods(CheckErrorsTest.class);
    private static int testCount = 0;

    private static final String TEST_METADATA_FILENAME = "CreateFolderWithDocument.xml";

    private final static Logger log = LoggerFactory.getLogger(CheckErrorsTest.class);

    @Deployment
    public static WebArchive createDeployment() {
        return XDSTestUtil.createDeploymentArchive(CheckErrorsTest.class)
            .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/"+TEST_METADATA_FILENAME)), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/"+TEST_METADATA_FILENAME); 
                
    }
    @EJB
    private XdsRegistryBeanForTesting session;
    
    @EJB
    private XDSRegistryTestBeanI testSession;

    
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
        } else {
            testSession.removeAllIdentifiables("urn:uuid:aabbccdd-bdda");
        }
    }


    @Test
    public void checkErrorUnknownPatId() throws Exception {
        log.info("\n############################# TEST: check unknown PatId ############################");
        SubmitObjectsRequest req = XDSTestUtil.getSubmitObjectsRequest(TEST_METADATA_FILENAME);
        RegistryObjectType obj = (RegistryObjectType) req.getRegistryObjectList().getIdentifiable().get(0).getValue();
        XDSTestUtil.setExternalIdentifierValue(obj.getExternalIdentifier(), 
                XDSConstants.UUID_XDSDocumentEntry_patientId, "11111^^^&1.2.3&ISO");
        doRegisterDocumentAndCheckError(req, XDSException.XDS_ERR_UNKNOWN_PATID, "Check Unknown PID");
    }

    @Test
    public void checkErrorMergedPatId() throws Exception {
        log.info("\n############################# TEST: check merged PatId ############################");
        String mergedPID = XDSTestUtil.TEST_PID_MERGED+XDSTestUtil.TEST_ISSUER;
        session.linkPatient(mergedPID, XDSTestUtil.TEST_PID_1+XDSTestUtil.TEST_ISSUER);
        SubmitObjectsRequest req = XDSTestUtil.getSubmitObjectsRequest(TEST_METADATA_FILENAME);
        RegistryObjectType obj = (RegistryObjectType) req.getRegistryObjectList().getIdentifiable().get(0).getValue();
        XDSTestUtil.setExternalIdentifierValue(obj.getExternalIdentifier(), 
                XDSConstants.UUID_XDSDocumentEntry_patientId, mergedPID);
        doRegisterDocumentAndCheckError(req, XDSException.XDS_ERR_UNKNOWN_PATID, "Check merged PID");
        session.linkPatient(mergedPID, null);
    }
    /**
     * Check if expected error (XDSPatientIdDoesNotMatch) is returned if an Association
     * references two XDSObjects with different patientId's.
     * 
     * @throws Exception
     */
    @Test
    public void checkErrorPatIdDoesntMatch() throws Exception {
        log.info("\n############################# TEST: check error PatId doesn't Match  ############################");
        SubmitObjectsRequest req = XDSTestUtil.getSubmitObjectsRequest(TEST_METADATA_FILENAME);
        RegistryObjectType obj = (RegistryObjectType) req.getRegistryObjectList().getIdentifiable().get(0).getValue();
        XDSTestUtil.setExternalIdentifierValue(obj.getExternalIdentifier(), 
                XDSConstants.UUID_XDSDocumentEntry_patientId, "test1234_2^^^&1.2.3.45.4.3.2.1&ISO");
        doRegisterDocumentAndCheckError(req, XDSException.XDS_ERR_PATID_DOESNOT_MATCH, "Check Unknown PID");
    }

    @Test
    public void checkMissingEntryUUID() throws Exception {
        log.info("\n############################# TEST: missing entryUUID (RegistryObject.id) ############################");
        SubmitObjectsRequest req = XDSTestUtil.getSubmitObjectsRequest(TEST_METADATA_FILENAME);
        checkEntryUUID(req, req.getRegistryObjectList().getIdentifiable().get(0).getValue(), 
        "Check Missing EntryUUID of XDSDocument");
        checkEntryUUID(req, req.getRegistryObjectList().getIdentifiable().get(1).getValue(), 
        "Check Missing EntryUUID of Folder");
        checkEntryUUID(req, req.getRegistryObjectList().getIdentifiable().get(2).getValue(), 
        "Check Missing EntryUUID of SubmissionSet");
        
        AuditTestManager.expectNumberOfMessages(3);
    }

    @Test
    public void checkMissingMimeType() throws Exception {
        log.info("\n############################# TEST: missing mimeType in DocumentEntry  ############################");
        SubmitObjectsRequest req = XDSTestUtil.getSubmitObjectsRequest(TEST_METADATA_FILENAME);
        ExtrinsicObjectType doc = (ExtrinsicObjectType) req.getRegistryObjectList().getIdentifiable().get(0).getValue();
        doc.setMimeType("");
        doRegisterDocumentAndCheckError(req, XDSException.XDS_ERR_REGISTRY_METADATA_ERROR, "Check missing mimeType");
    }
    
    /**
     * Check if expected error (XDSRegistryMetadataError) is returned 
     * if a required attribute coded as Slot is missing.
     * 
     * Required (see ITI TF-3: Cross-Transaction and Content Specifications, R and cP):
     * XDSDocumentEntry: creationTime, hash, repositoryUniqueId, size, sourcePatientId and languageCode
     * XDSSubmissionSet: submissionTime
     * 
     * @throws Exception
     */
    @Test
    public void checkErrorMissingSlot() throws Exception {
        log.info("\n############################# TEST: check missing slot ############################");
        SubmitObjectsRequest req = XDSTestUtil.getSubmitObjectsRequest(TEST_METADATA_FILENAME);
        //XDSDocument
        checkRequiredSlots(req, new String[]{XDSConstants.SLOT_NAME_CREATION_TIME, XDSConstants.SLOT_NAME_REPOSITORY_UNIQUE_ID,
                XDSConstants.SLOT_NAME_SIZE, XDSConstants.SLOT_NAME_HASH,
                XDSConstants.SLOT_NAME_SOURCE_PATIENT_ID, XDSConstants.SLOT_NAME_LANGUAGE_CODE}, 
                req.getRegistryObjectList().getIdentifiable().get(0).getValue().getSlot(),
                "Check missing Slots in XDSDocument");
        //SubmissionSet
        checkRequiredSlots(req, new String[]{XDSConstants.SLOT_NAME_SUBMISSION_TIME}, 
                req.getRegistryObjectList().getIdentifiable().get(2).getValue().getSlot(),
                "Check missing Slots in SubmissionSet");

        AuditTestManager.expectNumberOfMessages(7);
    }

    /**
     * Check if expected error (XDSRegistryMetadataError) is returned 
     * if a required code attribute (coded as Classification with classificationScheme) is missing.
     * 
     * Required (see ITI TF-3: Cross-Transaction and Content Specifications, R and cP):
     * XDSDocumentEntry: confidentialityCode, formatCode, healthCareFacilityTypeCode, practiceSettingCode, typeCode
     * XDSFolder: codeList
     * XDSSubmissionSet: contentTypeCode
     * 
     * @throws Exception
     */
    @Test
    public void checkErrorMissingCode() throws Exception {
        log.info("\n############################# TEST: check missing code ############################");
        SubmitObjectsRequest req = XDSTestUtil.getSubmitObjectsRequest(TEST_METADATA_FILENAME);
        //DocumentEntry
        checkRequiredCodes(req, new String[]{XDSConstants.UUID_XDSDocumentEntry_classCode,
                XDSConstants.UUID_XDSDocumentEntry_confidentialityCode, XDSConstants.UUID_XDSDocumentEntry_formatCode,
                XDSConstants.UUID_XDSDocumentEntry_healthCareFacilityTypeCode, XDSConstants.UUID_XDSDocumentEntry_practiceSettingCode,
                XDSConstants.UUID_XDSDocumentEntry_typeCode}, 
                ((RegistryObjectType) req.getRegistryObjectList().getIdentifiable().get(0).getValue()).getClassification(),
                "Check missing Code in XDSDocument");
        //Folder
        checkRequiredCodes(req, new String[]{XDSConstants.UUID_XDSFolder_codeList}, 
                ((RegistryObjectType) req.getRegistryObjectList().getIdentifiable().get(1).getValue()).getClassification(),
                "Check missing Code in Folder");
        //SubmissionSet
        checkRequiredCodes(req, new String[]{XDSConstants.UUID_XDSSubmissionSet_contentTypeCode}, 
                ((RegistryObjectType) req.getRegistryObjectList().getIdentifiable().get(2).getValue()).getClassification(),
                "Check missing Slots in SubmissionSet");

        AuditTestManager.expectNumberOfMessages(8);
    }

    /**
     * Check if expected error (XDSRegistryMetadataError) is returned 
     * if a required attribute (coded as ExternalIdentifier with identificationScheme) is missing.
     * 
     * Required (see ITI TF-3: Cross-Transaction and Content Specifications, R and cP):
     * XDSDocumentEntry: patientId, uniqueId
     * XDSFolder: patientId, uniqueId
     * XDSSubmissionSet: patientId, uniqueId, sourceId
     * 
     * @throws Exception
     */
    @Test
    public void checkErrorMissingExternalIdentifier() throws Exception {
        log.info("\n############################# TEST: check missing ExternalIdentifier ##################");
        SubmitObjectsRequest req = XDSTestUtil.getSubmitObjectsRequest(TEST_METADATA_FILENAME);
        //DocumentEntry
        checkRequiredExternalIdentifiers(req, new String[]{XDSConstants.UUID_XDSDocumentEntry_patientId,
                XDSConstants.UUID_XDSDocumentEntry_uniqueId}, 
                ((RegistryObjectType) req.getRegistryObjectList().getIdentifiable().get(0).getValue()).getExternalIdentifier(),
                "Check missing ExternalIdentifier in XDSDocument");
        //Folder
        checkRequiredExternalIdentifiers(req, new String[]{XDSConstants.UUID_XDSFolder_patientId,
                XDSConstants.UUID_XDSFolder_uniqueId}, 
                ((RegistryObjectType) req.getRegistryObjectList().getIdentifiable().get(1).getValue()).getExternalIdentifier(),
                "Check missing ExternalIdentifier in Folder");
        //SubmissionSet
        checkRequiredExternalIdentifiers(req, new String[]{XDSConstants.UUID_XDSSubmissionSet_patientId,
                XDSConstants.UUID_XDSSubmissionSet_uniqueId, XDSConstants.UUID_XDSSubmissionSet_sourceId}, 
                ((RegistryObjectType) req.getRegistryObjectList().getIdentifiable().get(2).getValue()).getExternalIdentifier(),
                "Check missing ExternalIdentifier in SubmissionSet");

        AuditTestManager.expectNumberOfMessages(7);
    }

    private void doRegisterDocumentAndCheckError(SubmitObjectsRequest req, String errorCode, String prefix) {
        RegistryResponseType rsp = null;
        try {
            rsp = session.documentRegistryRegisterDocumentSetB(req);
        } catch (Exception x) {
            fail(prefix+": Register document failed unexpected! Error:"+x);
        }
        if (!XDSConstants.XDS_B_STATUS_FAILURE.equals(rsp.getStatus())) {
            fail(prefix+": Register document should fail with error code:"+errorCode);
        }
        String error = rsp.getRegistryErrorList().getRegistryError().get(0).getErrorCode();
        assertEquals(prefix+": Error code:", errorCode, error);
    }

    private void checkRequiredSlots(SubmitObjectsRequest req, String[] slotNames, 
            List<SlotType1> slots, String prefix) {
        SlotType1 slot;
        for (int i = 0 ; i < slotNames.length ; i++) {
            slot = XDSTestUtil.removeSlot(slots, slotNames[i]);
            doRegisterDocumentAndCheckError(req, XDSException.XDS_ERR_REGISTRY_METADATA_ERROR, prefix);
            slots.add(slot);
        }
    }

    private void checkRequiredCodes(SubmitObjectsRequest req,
            String[] codeTypes, List<ClassificationType> cl, String prefix) {
        List<ClassificationType> codes;
        for (int i = 0 ; i < codeTypes.length ; i++) {
            codes = XDSTestUtil.removeCode(cl, codeTypes[i]);
            doRegisterDocumentAndCheckError(req, XDSException.XDS_ERR_REGISTRY_METADATA_ERROR, prefix);
            cl.addAll(codes);
        }
    }

    private void checkRequiredExternalIdentifiers(SubmitObjectsRequest req,
            String[] required, List<ExternalIdentifierType> eis, String prefix) {
        for (int i = 0 ; i < required.length ; i++) {
            ExternalIdentifierType ei = XDSTestUtil.removeExternalIdentifier(eis, required[i]);
            doRegisterDocumentAndCheckError(req, XDSException.XDS_ERR_REGISTRY_METADATA_ERROR, prefix);
            eis.add(ei);
        }
    }

    private void checkEntryUUID(SubmitObjectsRequest req, IdentifiableType obj,
            String prefix) {
        String id = obj.getId();
        obj.setId("");
        doRegisterDocumentAndCheckError(req, XDSException.XDS_ERR_REGISTRY_METADATA_ERROR, prefix);
        obj.setId(id);
    }

}