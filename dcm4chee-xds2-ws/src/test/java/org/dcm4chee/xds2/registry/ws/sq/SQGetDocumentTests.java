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
 * Tests 'GetDocuments', 'GetDocumentsAndAssociations' and 'GetRelatedDocuments'
 * @author Franz Willer <franz.willer@gmail.com>
 */
public class SQGetDocumentTests extends AbstractSQTests {
    private XDSRegistryBean session;

    public SQGetDocumentTests(XDSRegistryBean session) {
        this.session = session;
    }

    /**
     * Not in Pre-Con Test 
     * Check error handling if required Query Parameters are missing
     * Required: one of uniqueId OR uuid
     */
    public void testGetDocumentsCheckMissingParam() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        List<SlotType1> slots = req.getAdhocQuery().getSlot();
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck().setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        chk.checkResponse(rsp);
        addQueryParam(slots, XDSConstants.QRY_DOCUMENT_ENTRY_STATUS, "not required");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
        addQueryParam(slots, XDSConstants.QRY_DOCUMENT_ENTRY_UUID, "('urn:uuid:1234')");
        addQueryParam(slots, XDSConstants.QRY_DOCUMENT_UNIQUE_ID, "('1.2.3.4')");
        rsp = session.documentRegistryRegistryStoredQuery(req); //only one of uniqueId or uuid
        chk.checkResponse(rsp);
        chk.setStatus(XDSConstants.XDS_B_STATUS_SUCCESS);
        slots.remove(2);//remove uniqueId
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
        slots.remove(1);//remove uuid
        addQueryParam(slots, XDSConstants.QRY_DOCUMENT_UNIQUE_ID, "('1.2.3.4')");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11901: uniqueid
     * Queries for:
     *    XDSDocumentEntryUniqueId: 1.20.3.4.0.2.1
     * returns LeafClass   
     * must return: DocA
     */
    public void testGetDocumentsUniqueId() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_DOCUMENT_UNIQUE_ID, true, DOC_A_UNIQUE_ID);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocUUIDs(DOC_A_UUID).setDocNames(DOC_A)
                .setNrOfSubmissions(0).setNrOfFolders(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11901: uniqueid2
     * Queries for:
     *    XDSDocumentEntryUniqueId: 1.20.3.4.0.2.1, 1.20.3.4.0.2.4
     * returns LeafClass   
     * must return: DocA, DocD
     */
    public void testGetDocumentsUniqueId2() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_DOCUMENT_UNIQUE_ID, true, DOC_A_UNIQUE_ID,DOC_D_UNIQUE_ID);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocUUIDs(DOC_A_UUID,
                DOC_D_UUID).setDocNames(DOC_A,DOC_D)
                .setNrOfSubmissions(0).setNrOfFolders(0).setNrOfAssocs(0).checkResponse(rsp);
    }

    /**
     * Pre-Con Test 11901: uuid
     * Queries for:
     *    XDSDocumentEntryEntryUUID: urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba2001
     * returns LeafClass   
     * must return: DocB
     */
    public void testGetDocumentsUUID() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_UUID, true, DOC_B_UUID);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocUUIDs(DOC_B_UUID).setDocNames(DOC_B)
                .setNrOfSubmissions(0).setNrOfFolders(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11901: uuid2
     * Queries for:
     *    XDSDocumentEntryEntryUUID: urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba3001 
     *                               urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba4001
     * returns LeafClass   
     * must return: DocC, DocE
     */
    public void testGetDocumentsUUID2() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_UUID, true, DOC_C_UUID, DOC_E_UUID);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocUUIDs(DOC_C_UUID, DOC_E_UUID).setDocNames(DOC_C, DOC_E)
                .setNrOfSubmissions(0).setNrOfFolders(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11901: uuid_multiple_slot_values
     * Queries for:
     *    XDSDocumentEntryEntryUUID: urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba3001 
     *                               urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba4001
     * returns LeafClass   
     * must return: DocC, DocE
     */
    public void testGetDocumentsUUIDmultiSlotValue() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        List<SlotType1> slots = req.getAdhocQuery().getSlot();
        SlotType1 slot = toQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_UUID, "('"+DOC_C_UUID+"')");
        slot.getValueList().getValue().add("('"+DOC_E_UUID+"')");
        slots.add(slot);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocUUIDs(DOC_C_UUID, DOC_E_UUID).setDocNames(DOC_C, DOC_E)
                .setNrOfSubmissions(0).setNrOfFolders(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11901: homeCommunityId
     * Queries for:
     *    XDSDocumentEntryUniqueId: 1.20.3.4.0.2.1
     * returns LeafClass   
     * must return: DocA
     */
    public void testGetDocumentsHomeCommunityId() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_DOCUMENT_UNIQUE_ID, true, DOC_A_UNIQUE_ID);
        addMultiQueryParam(XDSConstants.QRY_HOME_COMMUNITY_ID, true, "1.2.3.0.0.1");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocUUIDs(DOC_A_UUID).setDocNames(DOC_A)
                .setNrOfSubmissions(0).setNrOfFolders(0).setNrOfAssocs(0).checkResponse(rsp);
    }

//GetDocumentsAndAssociations 
    /**
     * Not in Pre-Con Test 
     * Check error handling if required Query Parameters are missing
     * Required: one of uniqueId OR uuid
     */
    public void testGetDocumentsAndAssocCheckMissingParam() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetDocumentsAndAssociations, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        List<SlotType1> slots = req.getAdhocQuery().getSlot();
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck().setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        chk.checkResponse(rsp);
        addQueryParam(slots, XDSConstants.QRY_DOCUMENT_ENTRY_STATUS, "not required");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
        addQueryParam(slots, XDSConstants.QRY_DOCUMENT_ENTRY_UUID, "('urn:uuid:1234')");
        addQueryParam(slots, XDSConstants.QRY_DOCUMENT_UNIQUE_ID, "('1.2.3.4')");
        rsp = session.documentRegistryRegistryStoredQuery(req); //only one of uniqueId or uuid
        chk.checkResponse(rsp);
        chk.setStatus(XDSConstants.XDS_B_STATUS_SUCCESS);
        slots.remove(2);//remove uniqueId
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
        slots.remove(1);//remove uuid
        addQueryParam(slots, XDSConstants.QRY_DOCUMENT_UNIQUE_ID, "('1.2.3.4')");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11904: uniqueid
     * Queries for:
     *    XDSDocumentEntryUniqueId: 1.20.3.4.0.2.1
     * returns LeafClass   
     * must return: DocA, Association type: HasMember src:SubmissionSet dest:DocA
     */
    public void testGetDocumentsAndAssocUniqueId() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetDocumentsAndAssociations, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_DOCUMENT_UNIQUE_ID, true, DOC_A_UNIQUE_ID);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocUUIDs(DOC_A_UUID).setDocNames(DOC_A)
                .setNrOfSubmissions(0).setNrOfFolders(0)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_A_UUID, 
                        DOC_A_UUID).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11904: uniqueids
     * Queries for:
     *    XDSDocumentEntryUniqueId: 1.20.3.4.0.2.1, 1.20.3.4.0.2.4
     * returns LeafClass   
     * must return: DocA, DocD,
     *  Association type: HasMember src:SubmissionSet dest:DocA
     *  Association type: HasMember src:SubmissionSet dest:DocB
     *  Association type: HasMember src:Folder dest:DocB
     */
    public void testGetDocumentsAndAssocUniqueIds() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetDocumentsAndAssociations, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_DOCUMENT_UNIQUE_ID, true, "1.20.3.4.0.2.1','1.20.3.4.0.2.2");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocUUIDs(DOC_A_UUID,
                DOC_B_UUID).setDocNames(DOC_A,DOC_B)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_A_UUID,DOC_A_UUID) 
                .addAssoc(XDSConstants.HAS_MEMBER, FOLDER_DOC_B_UUID,DOC_B_UUID) 
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_B_UUID,DOC_B_UUID) 
                .setNrOfSubmissions(0).setNrOfFolders(0).checkResponse(rsp);
    }
    
    /**
     * Pre-Con Test 11904: uuid
     * Queries for:
     *    XDSDocumentEntryEntryUUID: urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba1001
     * returns LeafClass   
     * must return: DocA, Association type: HasMember src:SubmissionSet dest:DocA
     */
    public void testGetDocumentsAndAssocUUID() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetDocumentsAndAssociations, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_UUID, true, 
                DOC_A_UUID);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocUUIDs(DOC_A_UUID).setDocNames(DOC_A)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_A_UUID,DOC_A_UUID) 
                .setNrOfSubmissions(0).setNrOfFolders(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11904: uuids
     * Queries for:
     *    XDSDocumentEntryEntryUUID: urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba3001 
     *                               urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba4001
     * returns LeafClass   
     * must return: DocC, DocE
     */
    public void testGetDocumentsAndAssocUUIDs() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetDocumentsAndAssociations, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_UUID, true, 
                DOC_C_UUID, DOC_E_UUID);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocUUIDs(DOC_C_UUID,
                DOC_E_UUID).setDocNames(DOC_C,DOC_E)
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_E_UUID,
                        DOC_E_UUID) 
                .addAssoc(XDSConstants.HAS_MEMBER, FOLDER_DOC_C_D_UUID,
                        DOC_C_UUID) 
                .addAssoc(XDSConstants.HAS_MEMBER, SUBM_DOC_C_D_UUID,
                        DOC_C_UUID) 
                .addAssoc(XDSConstants.RPLC, DOC_F_UUID,
                        DOC_E_UUID) 
                .setNrOfSubmissions(0).setNrOfFolders(0).checkResponse(rsp);
    }

  //GetRelatedDocuments 
    /**
     * Not in Pre-Con Test 
     * Check error handling if required Query Parameters are missing
     * Required: one of uniqueId OR uuid
     */
    public void testGetRelatedDocumentsCheckMissingParam() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetRelatedDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        List<SlotType1> slots = req.getAdhocQuery().getSlot();
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck().setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        chk.checkResponse(rsp);
        addQueryParam(slots, XDSConstants.QRY_DOCUMENT_ENTRY_STATUS, "not required");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
        addQueryParam(slots, XDSConstants.QRY_DOCUMENT_ENTRY_UUID, "('urn:uuid:1234')");
        rsp = session.documentRegistryRegistryStoredQuery(req); //AssociationTypes is missing
        chk.checkResponse(rsp);
        slots.remove(1);//remove uuid
        addQueryParam(XDSConstants.QRY_ASSOCIATION_TYPES, "('urn:ihe:iti:2007:AssociationType:RPLC')");
        rsp = session.documentRegistryRegistryStoredQuery(req); //uniqueId or uuid is missing
        chk.checkResponse(rsp);
        addQueryParam(slots, XDSConstants.QRY_DOCUMENT_ENTRY_UUID, "('urn:uuid:1234')");
        addQueryParam(slots, XDSConstants.QRY_DOCUMENT_UNIQUE_ID, "('1.2.3.4')");
        rsp = session.documentRegistryRegistryStoredQuery(req); //only one of uniqueId or uuid
        chk.checkResponse(rsp);
        chk.setStatus(XDSConstants.XDS_B_STATUS_SUCCESS);
        slots.remove(3);//remove uniqueId
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
        slots.remove(2);//remove uuid
        addQueryParam(slots, XDSConstants.QRY_DOCUMENT_UNIQUE_ID, "('1.2.3.4')");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11909: no_initial_doc
     * Queries for:
     *    XDSDocumentEntryEntryUUID: urn:uuid:d90e5407-b356-4d91-a89f-873917b4b0e6
     * returns LeafClass   
     * must return: none
     */
    public void testGetRelatedDocumentsNoInitialDoc() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetRelatedDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_UUID, true, 
                "urn:uuid:d90e5407-b356-4d91-a89f-873917b4b0e6");
        addMultiQueryParam(XDSConstants.QRY_ASSOCIATION_TYPES, true, 
                XDSConstants.HAS_MEMBER, XDSConstants.RPLC);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setNrOfDocs(0).setNrOfAssocs(0)
                .setNrOfSubmissions(0).setNrOfFolders(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11909: uniqueid_no_related
     * Queries for:
     *    XDSDocumentEntryUniqueId: 1.20.3.4.0.2.1
     * returns LeafClass   
     * must return: none
     */
    public void testGetRelatedDocumentsUniqueIdNoRelatedDocs() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetRelatedDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_DOCUMENT_UNIQUE_ID, "'"+DOC_A_UNIQUE_ID+"'");
        addMultiQueryParam(XDSConstants.QRY_ASSOCIATION_TYPES, true, 
                XDSConstants.HAS_MEMBER, XDSConstants.RPLC);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setNrOfDocs(0).setNrOfAssocs(0)
                .setNrOfSubmissions(0).setNrOfFolders(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11909: uuid_no_related
     * Queries for:
     *    XDSDocumentEntryEntryUUID: urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba1001
     * returns LeafClass   
     * must return: none
     */
    public void testGetRelatedDocumentsUuidNoRelatedDocs() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetRelatedDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_UUID, 
                "'"+DOC_A_UUID+"'");
        addMultiQueryParam(XDSConstants.QRY_ASSOCIATION_TYPES, true, 
                XDSConstants.HAS_MEMBER, XDSConstants.RPLC);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setNrOfDocs(0).setNrOfAssocs(0)
                .setNrOfSubmissions(0).setNrOfFolders(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11909: uniqueid
     * Queries for:
     *    XDSDocumentEntryUniqueId: 1.20.3.4.0.2.4
     * returns LeafClass   
     * must return: DocE, DocF, Assoc type RPLC, 
     */
    public void testGetRelatedDocumentsUniqueId() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetRelatedDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_DOCUMENT_UNIQUE_ID, "'"+"1.20.3.4.0.2.4"+"'");
        addMultiQueryParam(XDSConstants.QRY_ASSOCIATION_TYPES, true, 
                XDSConstants.HAS_MEMBER, XDSConstants.RPLC);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocNames(DOC_E, "DocF")
            .addAssoc(XDSConstants.RPLC, DOC_F_UUID, DOC_E_UUID)
            .setNrOfSubmissions(0).setNrOfFolders(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11909: near_folder
     * Queries for:
     *    XDSDocumentEntryUniqueId: 1.20.3.4.0.2.3.2
     * returns LeafClass   
     * must return: none
     */
    public void testGetRelatedDocumentsNearFolder() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetRelatedDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_DOCUMENT_UNIQUE_ID, "'"+DOC_D_UNIQUE_ID+"'");
        addMultiQueryParam(XDSConstants.QRY_ASSOCIATION_TYPES, true, 
                XDSConstants.HAS_MEMBER, XDSConstants.RPLC);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setNrOfDocs(0).setNrOfAssocs(0)
            .setNrOfSubmissions(0).setNrOfFolders(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11909: uuid
     * Queries for:
     *    XDSDocumentEntryUniqueId: urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba3002
     * returns LeafClass   
     * must return: none
     */
    public void testGetRelatedDocumentsUuid() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetRelatedDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_UUID, 
                "'"+DOC_D_UUID+"'");
        addMultiQueryParam(XDSConstants.QRY_ASSOCIATION_TYPES, true, 
                XDSConstants.HAS_MEMBER, XDSConstants.RPLC);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setNrOfDocs(0).setNrOfAssocs(0)
            .setNrOfSubmissions(0).setNrOfFolders(0).checkResponse(rsp);
    }
}
