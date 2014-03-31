package org.dcm4chee.xds2.registry.ws.sq;
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



import java.util.List;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.registry.ws.XDSRegistryBean;

/**
 * Tests 'GetSubmissionSets', 'GetSubmissionSetAndContent' and 'GetAssociations'
 * @author Franz Willer <franz.willer@gmail.com>
 */
public class SQGetSubmGetAssocTests extends AbstractSQTests {
    private XDSRegistryBean session;

    public SQGetSubmGetAssocTests(XDSRegistryBean session) {
        this.session = session;
    }

    /**
     * Not in Pre-Con Test 
     * Check error handling if required Query Parameters are missing
     * Required: $uuid
     */
    public void testGetSubmissionSetsCheckMissingParam() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetSubmissionSets, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck().setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        chk.checkResponse(rsp);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_STATUS, "not required");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
        addQueryParam(XDSConstants.QRY_UUID, "('urn:uuid:1234')");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.setStatus(XDSConstants.XDS_B_STATUS_SUCCESS);
        chk.checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11905: doc_uuid
     * Queries for:
     *    XDSDocumentEntryEntryUUID: "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba1001"
     * returns LeafClass   
     * must return: SubmissionSet DocA, Association type HasMember, source:DocA, target SubmissionSet UUID
     */
    public void testGetSubmissionSetsDocUuid() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetSubmissionSets, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_UUID, true, DOC_A_UUID);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setSubmUUIDs(SUBM_DOC_A_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_A_UUID, DOC_A_UUID)
                .setNrOfDocs(0).setNrOfFolders(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11905: two_doc_uuid_two_ss
     * Queries for:
     *    XDSDocumentEntryEntryUUID: "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba1001"
     *                             and "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba2001"
     * returns LeafClass   
     * must return: 2 SubmissionSets, 2 Associations
     */
    public void testGetSubmissionSetsTwoDocTwoSS() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetSubmissionSets, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_UUID, true, DOC_A_UUID, DOC_B_UUID);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setSubmUUIDs(SUBM_DOC_A_UUID, SUBM_DOC_B_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_A_UUID, DOC_A_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_B_UUID, DOC_B_UUID)
                .setNrOfDocs(0).setNrOfFolders(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11905: two_doc_uuid_one_ss
     * Queries for:
     *    XDSDocumentEntryEntryUUID: "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba3001"
     *                             and "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba3002"
     * returns LeafClass   
     * must return: 2 SubmissionSets, 2 Associations
     */
    public void testGetSubmissionSetsTwoDocOneSS() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetSubmissionSets, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_UUID, true, DOC_C_UUID, DOC_D_UUID);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setSubmUUIDs(SUBM_DOC_C_D_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_C_D_UUID, DOC_C_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_C_D_UUID, DOC_D_UUID)
                .setNrOfDocs(0).setNrOfFolders(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11905: fol_uuid
     * Queries for:
     *    XDSFolderUUID: "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba2003"
     * returns LeafClass   
     * must return: 1 SubmissionSets, 1 Associations
     */
    public void testGetSubmissionSetsFolderUuid() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetSubmissionSets, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_UUID, true, FOLDER_DOC_B_UUID);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setSubmUUIDs(SUBM_DOC_B_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_B_UUID, FOLDER_DOC_B_UUID)
                .setNrOfDocs(0).setNrOfFolders(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11905: doc_and_fol
     * Queries for:
     *    XDSDocumentEntryEntryUUID: "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba1001"
     *    XDSFolderUUID: "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba2003"
     * returns LeafClass   
     * must return: 1 SubmissionSets, 1 Associations
     */
    public void testGetSubmissionSetsDocAndFolder() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetSubmissionSets, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_UUID, true, DOC_A_UUID, FOLDER_DOC_B_UUID);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setSubmUUIDs(SUBM_DOC_A_UUID, SUBM_DOC_B_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_A_UUID, DOC_A_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_B_UUID, FOLDER_DOC_B_UUID)
                .setNrOfDocs(0).setNrOfFolders(0).checkResponse(rsp);
    }
//GetSubmissionSetAndContent    
    /**
     * Not in Pre-Con Test 
     * Check error handling if required Query Parameters are missing
     * Required: $uuid
     */
    public void testGetSubmissionSetAndContentsCheckMissingParam() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetSubmissionSetAndContents, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        List<SlotType1> slots = req.getAdhocQuery().getSlot();
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck().setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        chk.checkResponse(rsp);
        addQueryParam(slots, XDSConstants.QRY_DOCUMENT_ENTRY_STATUS, "not required");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
        addQueryParam(slots, XDSConstants.QRY_SUBMISSIONSET_ENTRY_UUID, "'urn:uuid:1234'");
        addQueryParam(slots, XDSConstants.QRY_SUBMISSIONSET_UNIQUE_ID, "'1.2.3.4'");
        rsp = session.documentRegistryRegistryStoredQuery(req); //only one of uniqueId or uuid
        chk.checkResponse(rsp);
        chk.setStatus(XDSConstants.XDS_B_STATUS_SUCCESS);
        slots.remove(2);//remove uniqueId
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
        slots.remove(1);//remove uuid
        addQueryParam(slots, XDSConstants.QRY_SUBMISSIONSET_UNIQUE_ID, "'1.2.3.4'");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11906: uniqueid
     * Queries for:
     *    XDSSubmissionSetUniqueId: 1.20.3.4.1.2.1.1
     * returns LeafClass   
     * must return: 1 SubmissionSets, 1 Associations
     */
    public void testGetSubmissionSetAndContentsUniqueId() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetSubmissionSetAndContents, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_UNIQUE_ID, "'"+SUBM_DOC_A_UNIQUE_ID+"'");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setSubmUUIDs(SUBM_DOC_A_UUID).setDocUUIDs(DOC_A_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_A_UUID, DOC_A_UUID)
                .setNrOfFolders(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11906: folder_and_docs
     * Queries for:
     *    XDSSubmissionSetUniqueId: 1.20.3.4.1.2.3
     * returns LeafClass   
     * must return: 1 SubmissionSet, 2 Docs, 1 Folder, 
     *              7 Associations (DocC-Subm, DocD-Subm, Folder-Subm, DocC-Folder, DocD-Folder, Assoc(DocC-Folder)-Subm, Assoc(DocD-Folder)-Subm 
     */
    public void testGetSubmissionSetAndContentsFolderAndDocs() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetSubmissionSetAndContents, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_UNIQUE_ID, "'"+SUBM_DOC_C_D_UNIQUE_ID+"'");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setSubmUUIDs(SUBM_DOC_C_D_UUID)
            .setDocUUIDs(DOC_C_UUID, DOC_D_UUID)
            .setFolderUUIDs(FOLDER_DOC_C_D_UUID)
            .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_C_D_UUID, DOC_C_UUID)
            .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_C_D_UUID, DOC_D_UUID)
            .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_C_D_UUID, FOLDER_DOC_C_D_UUID)
            .addAssoc(XDSConstants.HAS_MEMBER, FOLDER_DOC_C_D_UUID, DOC_C_UUID)
            .addAssoc(XDSConstants.HAS_MEMBER, FOLDER_DOC_C_D_UUID, DOC_D_UUID)
            .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_C_D_UUID, ASSOC_DOC_C_FOLDER_UUID)
            .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_C_D_UUID, ASSOC_DOC_D_FOLDER_UUID)
            .checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11906: uuid
     * Queries for:
     *    XDSSubmissionSetEntryUUID: urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba1001
     * returns LeafClass   
     * must return: 1 SubmissionSets, DocA, 1 Assoc
     */
    public void testGetSubmissionSetAndContentsUuid() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetSubmissionSetAndContents, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_ENTRY_UUID, "'"+SUBM_DOC_A_UUID+"'");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setSubmUUIDs(SUBM_DOC_A_UUID).setDocUUIDs(DOC_A_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_A_UUID, DOC_A_UUID)
                .setNrOfFolders(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11906: format_code
     * Queries for:
     *    XDSSubmissionSetUniqueId: 1.20.3.4.1.2.3
     *    XDSDocumentEntryFormatCode: ('1.3.6.1.4.1.19376.1.5.3.1.1.2^^dcm4che formatCodes')
     * returns LeafClass   
     * must return: 1 SubmissionSets, DocD, 1 Folder, 4 Assocs (DocD-Subm, Folder-Subm, DocD-Folder, Assoc(DocD-Folder)-Subm)
     */
    public void testGetSubmissionSetAndContentsFormatCode() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetSubmissionSetAndContents, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_UNIQUE_ID, "'"+SUBM_DOC_C_D_UNIQUE_ID+"'");
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_FORMAT_CODE, "('1.3.6.1.4.1.19376.1.5.3.1.1.2^^dcm4che formatCodes')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setSubmUUIDs(SUBM_DOC_C_D_UUID).setDocUUIDs(DOC_D_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_C_D_UUID, DOC_D_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_C_D_UUID, FOLDER_DOC_C_D_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, FOLDER_DOC_C_D_UUID, DOC_D_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_C_D_UUID, ASSOC_DOC_D_FOLDER_UUID)
                .setFolderUUIDs(FOLDER_DOC_C_D_UUID).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11906: conf_code
     * Queries for:
     *    XDSSubmissionSetUniqueId: 1.20.3.4.1.2.3
     *    XDSDocumentEntryConfidentialityCode: ('1.3.6.1.4.1.21367.2006.7.103^^dcm4che confidentialityCodes')
     * returns LeafClass   
     * must return: 1 SubmissionSets, DocC, 1 Folder, 4 Assocs (DocC-Subm, Folder-Subm, DocC-Folder, Assoc(DocC-Folder)-Subm)
     */
    public void testGetSubmissionSetAndContentsConfCode() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetSubmissionSetAndContents, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_UNIQUE_ID, "'"+SUBM_DOC_C_D_UNIQUE_ID+"'");
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CONFIDENTIALITY_CODE, "('1.3.6.1.4.1.21367.2006.7.103^^dcm4che confidentialityCodes')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setSubmUUIDs(SUBM_DOC_C_D_UUID).setDocUUIDs(DOC_C_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_C_D_UUID, DOC_C_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_C_D_UUID, FOLDER_DOC_C_D_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, FOLDER_DOC_C_D_UUID, DOC_C_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_C_D_UUID, ASSOC_DOC_C_FOLDER_UUID)
                .setFolderUUIDs(FOLDER_DOC_C_D_UUID).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11906: objectrefs
     * Queries for:
     *    XDSSubmissionSetUniqueId: 1.20.3.4.0.2.1
     * returns LeafClass   
     * must return: 3 ObjectRefs
     */
    public void testGetSubmissionSetAndContentsObjRefs() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetSubmissionSetAndContents, XDSConstants.QUERY_RETURN_TYPE_OBJREF, null);
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_UNIQUE_ID, "'"+SUBM_DOC_A_UNIQUE_ID+"'");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setObjRefsUUIDs(SUBM_DOC_A_UUID, DOC_A_UUID, ASSOC_DOC_A_SUBM_UUID)
                .setNrOfAssocs(0).setNrOfDocs(0).setNrOfFolders(0).setNrOfSubmissions(0).checkResponse(rsp);
    }
//GetAssociations
    /**
     * Not in Pre-Con Test 
     * Check error handling if required Query Parameters are missing
     * Required: $uuid
     */
    public void testGetAssociationsCheckMissingParam() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetAssociations, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck().setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        chk.checkResponse(rsp);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_STATUS, "not required");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
        addQueryParam(XDSConstants.QRY_UUID, "('urn:uuid:1234')");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.setStatus(XDSConstants.XDS_B_STATUS_SUCCESS);
        chk.checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11903: single_from_doc
     * Queries for:
     *    UUID: "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba1001" (DocA)
     * returns LeafClass   
     * must return: 1 Association
     */
    public void testGetAssociationsSingleFromDoc() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetAssociations, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_UUID, true, DOC_A_UUID);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_A_UUID, DOC_A_UUID)
                .setNrOfDocs(0).setNrOfSubmissions(0).setNrOfFolders(0).setNrOfObjRefs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11903: multiple_assoc_from_doc
     * Queries for:
     *    UUID: "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba2001" (DocB)
     * returns LeafClass   
     * must return: 2 Associations
     */
    public void testGetAssociationsMultiFromDoc() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetAssociations, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_UUID, true, DOC_B_UUID);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_B_UUID, DOC_B_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, FOLDER_DOC_B_UUID, DOC_B_UUID)
                .setNrOfDocs(0).setNrOfSubmissions(0).setNrOfFolders(0).setNrOfObjRefs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11903: multiple_assoc_from_ss
     * Queries for:
     *    UUID: "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba2003" (Subm_DocB)
     * returns LeafClass   
     * must return: 3 Associations
     */
    public void testGetAssociationsMultiFromSubm() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetAssociations, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_UUID, true, SUBM_DOC_B_UUID);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_B_UUID, DOC_B_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_B_UUID, FOLDER_DOC_B_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_B_UUID, ASSOC_DOC_B_SUBM_UUID)
                .setNrOfDocs(0).setNrOfSubmissions(0).setNrOfFolders(0).setNrOfObjRefs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11903: multiple_doc
     * Queries for:
     *    UUID: "('urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba1002','urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba2003')" 
     *    (Subm_DocA,Subm_DocB)
     * returns LeafClass   
     * must return: 4 Associations
     */
    public void testGetAssociationsMultiDocs() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetAssociations, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_UUID, true, SUBM_DOC_A_UUID, SUBM_DOC_B_UUID);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck()
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_A_UUID, DOC_A_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_B_UUID, DOC_B_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_B_UUID, FOLDER_DOC_B_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_B_UUID, ASSOC_DOC_B_SUBM_UUID)
                .setNrOfDocs(0).setNrOfSubmissions(0).setNrOfFolders(0).setNrOfObjRefs(0).checkResponse(rsp);
    }
}
