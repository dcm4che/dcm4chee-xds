package org.dcm4chee.xds2.ws.registry.sq;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;

import javax.ejb.EJB;
import javax.xml.bind.JAXBException;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.persistence.XDSDocumentEntry;
import org.dcm4chee.xds2.ws.registry.XDSRegistryBean;
import org.dcm4chee.xds2.ws.registry.XDSRegistryTestBean;
import org.dcm4chee.xds2.ws.registry.XDSTestUtil;
import org.dcm4chee.xds2.ws.registry.query.StoredQuery;
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
public class StoredQueryTest {
    private static final int NUMBER_OF_TEST_METHODS = 
        XDSTestUtil.getNumberOfTestMethods(StoredQueryTest.class);

    private static SQFindDocumentsTests findDocTests;
    private static SQFindSubmissionSetTests findSubmTests;
    private static SQFindFolderTests findFolderTests;
    private static SQGetDocumentTests getDocTests;
    private static SQGetSubmGetAssocTests getSubmAssocTests;
    private static SQGetFoldersTests getFoldersTests;
    //TODO: all getXX Stored Queries
    
    private final static Logger log = LoggerFactory.getLogger(StoredQueryTest.class);

    private static int testCount = 0;

    @Deployment
    public static WebArchive createDeployment() {
        return XDSTestUtil.createDeploymentArchive(StoredQueryTest.class)
        .addPackage(StoredQueryTest.class.getPackage())
        .addPackage(StoredQuery.class.getPackage())
        .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/ws/registry/sq/single_doc.xml")), 
        "WEB-INF/classes/org/dcm4chee/xds2/ws/registry/sq/single_doc.xml") 
        .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/ws/registry/sq/single_doc_w_fol.xml")), 
        "WEB-INF/classes/org/dcm4chee/xds2/ws/registry/sq/single_doc_w_fol.xml") 
        .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/ws/registry/sq/two_doc_w_fol.xml")), 
        "WEB-INF/classes/org/dcm4chee/xds2/ws/registry/sq/two_doc_w_fol.xml") 
        .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/ws/registry/sq/single_doc_for_rplc.xml")), 
        "WEB-INF/classes/org/dcm4chee/xds2/ws/registry/sq/single_doc_for_rplc.xml") 
        .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/ws/registry/sq/rplc.xml")), 
        "WEB-INF/classes/org/dcm4chee/xds2/ws/registry/sq/rplc.xml"); 
    }
    @EJB
    private XDSRegistryBean session;

    @EJB
    private XDSRegistryTestBean testSession;

    /**
     * Preload test data for Stored Query testing.
     * Uses SubmitObjectsRequest's with same data as test 12346 of Pre-Con Test tool (XDS Toolkit build.number 144)
     * - All objects have fixed UUIDs starting with 'urn:uuid:aabbccdd-bdda'
     * - Code schemes are 'dcm4che' ('dcm4che classCodes', 'dcm4che SNM3', ...)
     * - XAD PatId: 'test1234_1^^^&;1.2.3.45.4.3.2.1&;ISO'
     * - SubmissionTime of rplc changed from 20041225235050 to 20041226235050 for edge testing
     * 
     * Note: Initialize the registry with ebXML and XDS Classifications (and remove these after last test)
     * if the Registry is not already initialized.
     * 
     * @throws Exception
     */
    @Before
    public void prepare() {
        if (testCount++ == 0) {
            findDocTests = new SQFindDocumentsTests(session);
            findSubmTests = new SQFindSubmissionSetTests(session);
            findFolderTests = new SQFindFolderTests(session);
            getDocTests = new SQGetDocumentTests(session);
            getSubmAssocTests = new SQGetSubmGetAssocTests(session);
            getFoldersTests = new SQGetFoldersTests(session);
            XDSTestUtil.prepareTests(session, log);
            log.info("\n################################# Prepare Registry for StoredQuery Tests #################################");
            try {
                prepareWithMetadataFile("sq/single_doc.xml");
                prepareWithMetadataFile("sq/single_doc_w_fol.xml");
                prepareWithMetadataFile("sq/two_doc_w_fol.xml");
                prepareWithMetadataFile("sq/single_doc_for_rplc.xml");
                prepareWithMetadataFile("sq/rplc.xml");
            } catch (AssertionError e) {
                throw e;
            } catch (Exception x) {
                fail("Prepare StoredQuery tests failed unexpected!");
            }
            log.error("Prepare StoredQuery tests failed unexpected!");
            log.info("\n################################# Prepare Registry for StoredQuery Tests done #################################");
        }
    }

    private void prepareWithMetadataFile(String metadata) throws JAXBException {
        RegistryResponseType rsp = session.documentRegistryRegisterDocumentSetB(
                XDSTestUtil.getSubmitObjectsRequest(metadata));
        if (!XDSConstants.XDS_B_STATUS_SUCCESS.equals(rsp.getStatus())) {
            fail("Prepare Registry for StoredQuery with "+metadata+" failed:"+
                    rsp.getRegistryErrorList().getRegistryError().get(0).getValue());
        }
    }

    @After
    public void clearDB() {
        if (testCount == NUMBER_OF_TEST_METHODS) {
            XDSTestUtil.clearDB(testSession, log);
            findDocTests = null;
        }
    }

    @Test
    public void checkPreparation() throws Exception {
        log.info("\n############################# TEST: check preparation ############################");
        XDSDocumentEntry obj = session.getDocumentEntryByUUID("urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba1001");
        assertNotNull("Preparation failed! Missing single_doc.", obj);
        assertEquals("single_doc uniqueId:", "1.20.3.4.0.2.1", obj.getUniqueId());
        obj = session.getDocumentEntryByUUID("urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba2001");
        assertNotNull("Preparation failed! Missing single_doc_w_fol.", obj);
        assertEquals("single_doc_w_fol uniqueId:", "1.20.3.4.0.2.2", obj.getUniqueId());
        obj = session.getDocumentEntryByUUID("urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba3001");
        assertNotNull("Preparation failed! Missing two_doc_w_fol document1.", obj);
        assertEquals("two_doc_w_fol uniqueId:", "1.20.3.4.0.2.3", obj.getUniqueId());
        obj = session.getDocumentEntryByUUID("urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba3002");
        assertNotNull("Preparation failed! Missing two_doc_w_fol document2.", obj);
        assertEquals("two_doc_w_fol uniqueId:", "1.20.3.4.0.2.3.1", obj.getUniqueId());
        obj = session.getDocumentEntryByUUID("urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba4001");
        assertNotNull("Preparation failed! Missing single_doc_for_rplc.", obj);
        assertEquals("single_doc_for_rplc uniqueId:", "1.20.3.4.0.2.4", obj.getUniqueId());
        obj = session.getDocumentEntryByUUID("urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba5001");
        assertNotNull("Preparation failed! Missing rplc document.", obj);
        assertEquals("rplc uniqueId:", "1.20.3.4.0.2.5", obj.getUniqueId());
        assertEquals("rplc document status:", XDSConstants.STATUS_APPROVED, obj.getStatus());
        obj = session.getDocumentEntryByUUID("urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba4001");
        assertNotNull("Preparation failed! Missing replaced document.", obj);
        assertEquals("replaced document status:", XDSConstants.STATUS_DEPRECATED, obj.getStatus());
    }
// FindDocuments Tests. (like Pre-Con Test 11897 SQ.b FindDocuments Stored Query)
    @Test
    public void findDocumentsCheckMissingParam() {
        findDocTests.findDocumentsCheckMissingParam();
    }
    @Test
    public void findDocumentsMergedPID() throws Exception {
        findDocTests.findDocumentsMergedPID();
    }
    @Test
    public void findDocumentsApproved() {
        findDocTests.findDocumentsApproved();
    }
    @Test
    public void findDocumentsObjectRefs() {
        findDocTests.findDocumentsObjectRefs();
    }
    @Test
    public void findDocumentsDeprecated() {
        findDocTests.findDocumentsDeprecated();
    }
    
    @Test
    public void findDocumentsApprovedAndDeprecated() {
        findDocTests.findDocumentsApprovedAndDeprecated();
    }
    @Test
    public void findDocumentsClassCodeOne() {
        findDocTests.findDocumentsClassCodeOne();
    }
    @Test
    public void findDocumentsClassCodeTwo() {
        findDocTests.findDocumentsClassCodeTwo();
    }
    @Test
    public void findDocumentsClassCodeMismatch() {
        findDocTests.findDocumentsClassCodeMismatch();
    }
    @Test
    public void findDocumentsPracticeSetting() {
        findDocTests.findDocumentsPracticeSetting();
    }
    @Test
    public void findDocumentsPracticeSettingScheme() {
        findDocTests.findDocumentsPracticeSettingScheme();
    }
    @Test
    public void findDocumentsClasscodePracticesetting() {
        findDocTests.findDocumentsClasscodePracticesetting();
    }
    @Test
    public void findDocumentsClassCodeScheme2() {
        findDocTests.findDocumentsClassCodeScheme2();
    }
    @Test
    public void findDocumentsConfCode() {
        findDocTests.findDocumentsConfCode();
    }
    @Test
    public void findDocumentsCreationTimeBetween() {
        findDocTests.findDocumentsCreationTimeBetween();
    }
    @Test
    public void findDocumentsCreationTimeBetweenLong() {
        findDocTests.findDocumentsCreationTimeBetweenLong();
    }
    @Test
    public void findDocumentsCreationTimeLeftEdge() {
        findDocTests.findDocumentsCreationTimeLeftEdge();
    }
    @Test
    public void findDocumentsCreationTimeRightEdge() { 
        findDocTests.findDocumentsCreationTimeRightEdge();
    }
    @Test
    public void findDocumentsCreationTimePracticeSetting() {
        findDocTests.findDocumentsCreationTimePracticeSetting();
    }
    @Test
    public void findDocumentsServiceStartTime() {
        findDocTests.findDocumentsServiceStartTime();
    }
    @Test
    public void findDocumentsServiceStopTime() {
        findDocTests.findDocumentsServiceStartTime();
    }
    @Test
    public void findDocumentsEventCode() {
        findDocTests.findDocumentsEventCode();
    }
    @Test
    public void findDocumentsEventCodeMulti() {
        findDocTests.findDocumentsEventCodeMulti();
    }
    @Test
    public void findDocumentsEventCodeAndOr() {
        findDocTests.findDocumentsEventCodeAndOr();
    }
    @Test
    public void findDocumentsFormatCode() {
        findDocTests.findDocumentsFormatCode();
    }
    @Test
    public void findDocumentsTypeCode() {
        findDocTests.findDocumentsTypeCode();
    }
    @Test
    public void findDocumentsHealthCareFacilityCode() {
        findDocTests.findDocumentsHealthCareFacilityCode();
    }
    @Test
    public void findDocumentsHealthCareFacilityCodeScheme() {
        findDocTests.findDocumentsHealthCareFacilityCodeScheme();
    }
    @Test
    public void findDocumentsNoMatchingClassCode() {
        findDocTests.findDocumentsNoMatchingClassCode();
    }
    @Test
    public void findDocumentsAuthor() {
        findDocTests.findDocumentsAuthor();
    }
    @Test
    public void findDocumentsOldScheme() {
        findDocTests.findDocumentsOldScheme();
    }
// End FindDocuments
    
// FindSubmissionSet tests    
    @Test
    public void findSubmissionSetCheckMissingParam() {
        findSubmTests.findSubmissionSetCheckMissingParam();
    }
    @Test
    public void findSubmissionSetSimple() {
        findSubmTests.findSubmissionSetSimple();
    }
    @Test
    public void findSubmissionOtherSourceId() {
        findSubmTests.findSubmissionSetOtherSourceId();
    }
    @Test
    public void findSubmissionSubmissionTimeIn() {
        findSubmTests.findSubmissionSetSubmissionTimeIn();
    }
    @Test
    public void findSubmissionSubmissionTimeOut() {
        findSubmTests.findSubmissionSetSubmissionTimeOut();
    }
    @Test
    public void findSubmissionSubmissionTimeNoStart() {
        findSubmTests.findSubmissionSetSubmissionTimeNoStart();
    }
    @Test
    public void findSubmissionSetSubmissionTimeNoEnd() {
        findSubmTests.findSubmissionSetSubmissionTimeNoEnd();
    }
    @Test
    public void findSubmissionSetSubmissionTimeEdge() {
        findSubmTests.findSubmissionSetSubmissionTimeEdge();
    }
    @Test
    public void findSubmissionSetAuthorAll() {
        findSubmTests.findSubmissionSetAuthorAll();
    }
    @Test
    public void findSubmissionSetAuthorNone() {
        findSubmTests.findSubmissionSetAuthorNone();
    }
    @Test
    public void findSubmissionSetContentTypeAll() {
        findSubmTests.findSubmissionSetContentTypeAll();
    }
    @Test
    public void findSubmissionSetContentTypeNone() {
        findSubmTests.findSubmissionSetContentTypeNone();
    }
//END FindSubmissionSet
    
// FindFolders tests    
    @Test
    public void findFoldersCheckMissingParam() {
        findFolderTests.findFoldersCheckMissingParam();
    }
    @Test
    public void findFoldersSimple() {
        findFolderTests.findFoldersSimple();
    }
    @Test
    public void findFoldersLastUpdateTimeIn() {
        findFolderTests.findFoldersLastUpdateTimeIn();
    }
    @Test
    public void findFoldersLastUpdateTimeOut() {
        findFolderTests.findFoldersLastUpdateTimeOut();
    }
    @Test
    public void findFoldersCodeList() {
        findFolderTests.findFoldersCodeList();
    }
//END findFolders

// GetDocuments tests    
    @Test
    public void testGetDocumentsCheckMissingParam() {
        getDocTests.testGetDocumentsCheckMissingParam();
    }
    @Test
    public void testGetDocumentsUniqueId() {
        getDocTests.testGetDocumentsUniqueId();
    }
    @Test
    public void testGetDocumentsUniqueId2() {
        getDocTests.testGetDocumentsUniqueId2();
    }
    @Test
    public void testGetDocumentsUUID() {
        getDocTests.testGetDocumentsUUID();
    }
    @Test
    public void testGetDocumentsUUID2() {
        getDocTests.testGetDocumentsUUID2();
    }
    @Test
    public void testGetDocumentsUUIDmultiSlotValue() {
        getDocTests.testGetDocumentsUUIDmultiSlotValue();
    }
//GetDocumentsAndAssoc    
    @Test
    public void testGetDocumentsAndAssocCheckMissingParam() {
        getDocTests.testGetDocumentsAndAssocCheckMissingParam();
    }
    @Test
    public void testGetDocumentsAndAssocUniqueId() {
        getDocTests.testGetDocumentsAndAssocUniqueId();
    }
    @Test
    public void testGetDocumentsAndAssocUniqueIds() {
        getDocTests.testGetDocumentsAndAssocUniqueIds();
    }
    @Test
    public void testGetDocumentsAndAssocUUID() {
        getDocTests.testGetDocumentsAndAssocUUID();
    }
    @Test
    public void testGetDocumentsAndAssocUUIDs() {
        getDocTests.testGetDocumentsAndAssocUUIDs();
    }
  //GetRelatedDocuments    
    @Test
    public void testGetRelatedDocumentsCheckMissingParam() {
        getDocTests.testGetRelatedDocumentsCheckMissingParam();
    }
    @Test
    public void testGetRelatedDocumentsNoInitialDoc() {
        getDocTests.testGetRelatedDocumentsNoInitialDoc();
    }
    @Test
    public void testGetRelatedDocumentsUniqueIdNoRelatedDocs() {
        getDocTests.testGetRelatedDocumentsUniqueIdNoRelatedDocs();
    }
    @Test
    public void testGetRelatedDocumentsUuidNoRelatedDocs() {
        getDocTests.testGetRelatedDocumentsUuidNoRelatedDocs();
    }
    @Test
    public void testGetRelatedDocumentsUniqueId() {
        getDocTests.testGetRelatedDocumentsUniqueId();
    }
    @Test
    public void testGetRelatedDocumentsNearFolder() {
        getDocTests.testGetRelatedDocumentsNearFolder();
    }
    @Test
    public void testGetRelatedDocumentsUuid() {
        getDocTests.testGetRelatedDocumentsUuid();
    }
    // GetSubmissionSet and GetAssociation
    @Test
    public void testGetSubmissionSetsCheckMissingParam() {
        getSubmAssocTests.testGetSubmissionSetsCheckMissingParam();
    }
    @Test
    public void testGetSubmissionSetsDocUuid() {
        getSubmAssocTests.testGetSubmissionSetsDocUuid();
    }
    @Test
    public void testGetSubmissionSetsFolderUuid() {
        getSubmAssocTests.testGetSubmissionSetsFolderUuid();
    }
    @Test
    public void testGetSubmissionSetsTwoDocOneSS() {
        getSubmAssocTests.testGetSubmissionSetsTwoDocOneSS();
    }
    @Test
    public void testGetSubmissionSetsTwoDocTwoSS() {
        getSubmAssocTests.testGetSubmissionSetsTwoDocTwoSS();
    }
    @Test
    public void testGetSubmissionSetsDocAndFolder() {
        getSubmAssocTests.testGetSubmissionSetsDocAndFolder();
    }
//GetSubmissionSetAndContents
    @Test
    public void testGetSubmissionSetAndContentsCheckMissingParam() {
        getSubmAssocTests.testGetSubmissionSetAndContentsCheckMissingParam();
    }
    @Test
    public void testGetSubmissionSetAndContentsUniqueId() {
        getSubmAssocTests.testGetSubmissionSetAndContentsUniqueId();
    }
    @Test
    public void testGetSubmissionSetAndContentsFolderAndDocs() {
        getSubmAssocTests.testGetSubmissionSetAndContentsFolderAndDocs();
    }
    @Test
    public void testGetSubmissionSetAndContentsUuid() {
        getSubmAssocTests.testGetSubmissionSetAndContentsUuid();
    }
    @Test
    public void testGetSubmissionSetAndContentsFormatCode() {
        getSubmAssocTests.testGetSubmissionSetAndContentsFormatCode();
    }
    @Test
    public void testGetSubmissionSetAndContentsConfCode() {
        getSubmAssocTests.testGetSubmissionSetAndContentsConfCode();
    }
    @Test
    public void testGetSubmissionSetAndContentsObjRefs() {
        getSubmAssocTests.testGetSubmissionSetAndContentsObjRefs();
    }
//GetAssociations
    @Test
    public void testGetAssociationsCheckMissingParam() {
        getSubmAssocTests.testGetAssociationsCheckMissingParam();
    }
    @Test
    public void testGetAssociationsSingleFromDoc() {
        getSubmAssocTests.testGetAssociationsSingleFromDoc();
    }
    @Test
    public void testGetAssociationsMultiFromDoc() {
        getSubmAssocTests.testGetAssociationsMultiFromDoc();
    }
    @Test
    public void testGetAssociationsMultiFromSubm() {
        getSubmAssocTests.testGetAssociationsMultiFromSubm();
    }
    @Test
    public void testGetAssociationsMultiDocs() {
        getSubmAssocTests.testGetAssociationsMultiDocs();
    }
  //GetFolders
    @Test
    public void testGetFoldersCheckMissingParam() {
        getFoldersTests.testGetFolderCheckMissingParam(XDSConstants.XDS_GetFolders);
    }
    @Test
    public void testGetFoldersUniqueId() {
        getFoldersTests.testGetFoldersUniqueId();
    }
    @Test
    public void testGetFoldersUuid() {
        getFoldersTests.testGetFoldersUuid();
    }
//GetFolderAndContent    
    @Test
    public void testGetFolderAndContentCheckMissingParam() {
        getFoldersTests.testGetFolderCheckMissingParam(XDSConstants.XDS_GetFolderAndContents);
    }
    @Test
    public void testGetFolderAndContentUniqueId() {
        getFoldersTests.testGetFolderAndContentUniqueId();
    }
    @Test
    public void testGetFolderAndContentUuid() {
        getFoldersTests.testGetFolderAndContentUuid();
    }
    @Test
    public void testGetFolderAndContentUuid2() {
        getFoldersTests.testGetFolderAndContentUuid2();
    }
    @Test
    public void testGetFolderAndContentConfCode() {
        getFoldersTests.testGetFolderAndContentConfCode();
    }
    @Test
    public void testGetFolderAndContentBothConfCode() {
        getFoldersTests.testGetFolderAndContentBothConfCode();
    }
    @Test
    public void testGetFolderAndContentFormatCode() {
        getFoldersTests.testGetFolderAndContentFormatCode();
    }
  //GetFoldersForDocument    
    @Test
    public void testGetFolderForDocCheckMissingParam() {
        getFoldersTests.testGetFoldersForDocCheckMissingParam();
    }
    @Test
    public void testGetFolderForDocUniqueId() {
        getFoldersTests.testGetFoldersForDocUniqueId();
    }
    @Test
    public void testGetFoldersForDocUuid() {
        getFoldersTests.testGetFoldersForDocUuid();
    }
}
