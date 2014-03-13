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



import java.util.List;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.ws.registry.XDSRegistryBean;

/**
 * Tests 'GetFolders', 'GetFolderContent' and 'GetFoldersForDocument
 * @author Franz Willer <franz.willer@gmail.com>
 */
public class SQGetFoldersTests extends AbstractSQTests {
    private XDSRegistryBean session;

    public SQGetFoldersTests(XDSRegistryBean session) {
        this.session = session;
    }

//GetFolders    
    /**
     * Not in Pre-Con Test 
     * Check error handling if required Query Parameters are missing
     * Required: $XDSFolderEntryUUID or $XDSFolderUniqueID
     */
    public void testGetFolderCheckMissingParam(String queryName) {
        AdhocQueryRequest req = getQueryRequest(queryName, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        List<SlotType1> slots = req.getAdhocQuery().getSlot();
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck().setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        chk.checkResponse(rsp);
        addQueryParam(slots, XDSConstants.QRY_FOLDER_STATUS, "not required");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
        addQueryParam(slots, XDSConstants.QRY_FOLDER_ENTRY_UUID, "'urn:uuid:1234'");
        addQueryParam(slots, XDSConstants.QRY_FOLDER_UNIQUE_ID, "'1.2.3.4'");
        rsp = session.documentRegistryRegistryStoredQuery(req); //only one of uniqueId or uuid
        chk.checkResponse(rsp);
        chk.setStatus(XDSConstants.XDS_B_STATUS_SUCCESS);
        slots.remove(2);//remove uniqueId
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
        slots.remove(1);//remove uuid
        addQueryParam(slots, XDSConstants.QRY_FOLDER_UNIQUE_ID, "'1.2.3.4'");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11902: uniqueid
     * Queries for:
     *    XDSFolderUniqueId: 1.20.3.4.2.2.2, 1.20.3.4.2.2.3
     * returns LeafClass   
     * must return: 2 Folders
     */
    public void testGetFoldersUniqueId() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetFolders, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_FOLDER_UNIQUE_ID, true, FOLDER_DOC_B_UNIQUE_ID, FOLDER_DOC_C_D_UNIQUE_ID);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setFolderUUIDs(FOLDER_DOC_B_UUID,FOLDER_DOC_C_D_UUID)
                .setNrOfDocs(0).setNrOfAssocs(0).setNrOfObjRefs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11902: uuid
     * Queries for:
     *    XDSFolderEntryUUID: UIUID of FOLDER_DOC_B and FOLDER_DOC_C_D
     * returns LeafClass   
     * must return: 2 Folders
     */
    public void testGetFoldersUuid() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetFolders, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addMultiQueryParam(XDSConstants.QRY_FOLDER_ENTRY_UUID, true, FOLDER_DOC_B_UUID, FOLDER_DOC_C_D_UUID);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setFolderUUIDs(FOLDER_DOC_B_UUID,FOLDER_DOC_C_D_UUID)
                .setNrOfDocs(0).setNrOfAssocs(0).setNrOfSubmissions(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11907: uniqueid
     * Queries for:
     *    XDSFolderUniqueId: 1.20.3.4.2.2.2
     * returns LeafClass   
     * must return: 1 Folder, 1 Doc, 1 Submission
     */
    public void testGetFolderAndContentUniqueId() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetFolderAndContents, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_FOLDER_UNIQUE_ID, "'"+FOLDER_DOC_B_UNIQUE_ID+"'");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setFolderUUIDs(FOLDER_DOC_B_UUID).setDocNames(DOC_B)
                .addAssoc(XDSConstants.HAS_MEMBER, FOLDER_DOC_B_UUID, DOC_B_UUID)
                .setNrOfSubmissions(0).setNrOfObjRefs(0).checkResponse(rsp);
    }
//GetFolderAndContent
    /**
     * Pre-Con Test 11907: uuid
     * Queries for:
     *    XDSFolderUniqueId: UUID of FOLDER_DOC_B
     * returns LeafClass   
     * must return: 1 Folder, 1 Doc, 1 Assoc
     */
    public void testGetFolderAndContentUuid() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetFolderAndContents, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_FOLDER_ENTRY_UUID, "'"+FOLDER_DOC_B_UUID+"'");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setFolderUUIDs(FOLDER_DOC_B_UUID).setDocNames(DOC_B)
                .addAssoc(XDSConstants.HAS_MEMBER, FOLDER_DOC_B_UUID, DOC_B_UUID)
                .setNrOfSubmissions(0).setNrOfObjRefs(0).checkResponse(rsp);
    }
    /**
     * NOT in Pre-Con Test 11907: uuid2
     * Queries for:
     *    XDSFolderUniqueId: UUID of FOLDER_DOC_C_D
     * returns LeafClass   
     * must return: 1 Folder, 2 Doc, 2 Assoc
     */
    public void testGetFolderAndContentUuid2() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetFolderAndContents, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_FOLDER_ENTRY_UUID, "'"+FOLDER_DOC_C_D_UUID+"'");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setFolderUUIDs(FOLDER_DOC_C_D_UUID).setDocNames(DOC_C, DOC_D)
                .addAssoc(XDSConstants.HAS_MEMBER, FOLDER_DOC_C_D_UUID, DOC_C_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, FOLDER_DOC_C_D_UUID, DOC_D_UUID)
                .setNrOfSubmissions(0).setNrOfObjRefs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11907: conf_code
     * Queries for:
     *    XDSFolderUniqueId: UUID of FOLDER_DOC_C_D
     *    XDSDocumentEntryConfidetialityCode: ('1.3.6.1.4.1.21367.2006.7.103^^dcm4che confidentialityCodes')
     * returns LeafClass   
     * must return: 1 Folder, DocC, 1 Assoc Folder-DocC
     */
    public void testGetFolderAndContentConfCode() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetFolderAndContents, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_FOLDER_ENTRY_UUID, "'"+FOLDER_DOC_C_D_UUID+"'");
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CONFIDENTIALITY_CODE, 
                "('1.3.6.1.4.1.21367.2006.7.103^^dcm4che confidentialityCodes')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setFolderUUIDs(FOLDER_DOC_C_D_UUID).setDocNames(DOC_C)
                .addAssoc(XDSConstants.HAS_MEMBER, FOLDER_DOC_C_D_UUID, DOC_C_UUID)
                .setNrOfSubmissions(0).setNrOfObjRefs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11907: both_conf_code
     * Queries for:
     *    XDSFolderUniqueId: UUID of FOLDER_DOC_C_D
     *    XDSDocumentEntryConfidetialityCode: 
     *       ('1.3.6.1.4.1.21367.2006.7.103^^dcm4che confidentialityCodes','1.3.6.1.4.1.21367.2006.7.104^^dcm4che confidentialityCodes')
     * returns LeafClass   
     * must return: 1 Folder, DocC and DocD, 2 Assocs Folder-DocC and Folder-DocD
     */
    public void testGetFolderAndContentBothConfCode() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetFolderAndContents, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_FOLDER_ENTRY_UUID, "'"+FOLDER_DOC_C_D_UUID+"'");
        addMultiQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CONFIDENTIALITY_CODE, true,
                "1.3.6.1.4.1.21367.2006.7.103^^dcm4che confidentialityCodes",
                "1.3.6.1.4.1.21367.2006.7.104^^dcm4che confidentialityCodes"
                );
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setFolderUUIDs(FOLDER_DOC_C_D_UUID).setDocNames(DOC_C, DOC_D)
                .addAssoc(XDSConstants.HAS_MEMBER, FOLDER_DOC_C_D_UUID, DOC_C_UUID)
                .addAssoc(XDSConstants.HAS_MEMBER, FOLDER_DOC_C_D_UUID, DOC_D_UUID)
                .setNrOfSubmissions(0).setNrOfObjRefs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11907: format_code
     * Queries for:
     *    XDSFolderUniqueId: UUID of FOLDER_DOC_C_D
     *    XDSDocumentEntryFormatCode: ('CDAR2/IHE 1.0^^dcm4che formatCodes')
     * returns LeafClass   
     * must return: 1 Folder, DocC, 1 Assoc Folder-DocC
     */
    public void testGetFolderAndContentFormatCode() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetFolderAndContents, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_FOLDER_ENTRY_UUID, "'"+FOLDER_DOC_C_D_UUID+"'");
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_FORMAT_CODE, 
                "('CDAR2/IHE 1.0^^dcm4che formatCodes')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setFolderUUIDs(FOLDER_DOC_C_D_UUID).setDocNames(DOC_C)
                .addAssoc(XDSConstants.HAS_MEMBER, FOLDER_DOC_C_D_UUID, DOC_C_UUID)
                .setNrOfSubmissions(0).setNrOfObjRefs(0).checkResponse(rsp);
    }
  //GetFolders    
    /**
     * Not in Pre-Con Test 
     * Check error handling if required Query Parameters are missing
     * Required: $XDSFolderEntryUUID or $XDSFolderUniqueID
     */
    public void testGetFoldersForDocCheckMissingParam() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetFoldersForDocument, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        List<SlotType1> slots = req.getAdhocQuery().getSlot();
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck().setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        chk.checkResponse(rsp);
        addQueryParam(slots, XDSConstants.QRY_FOLDER_STATUS, "not required");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
        addQueryParam(slots, XDSConstants.QRY_DOCUMENT_ENTRY_UUID, "'urn:uuid:1234'");
        addQueryParam(slots, XDSConstants.QRY_DOCUMENT_UNIQUE_ID, "'1.2.3.4'");
        rsp = session.documentRegistryRegistryStoredQuery(req); //only one of uniqueId or uuid
        chk.checkResponse(rsp);
        chk.setStatus(XDSConstants.XDS_B_STATUS_SUCCESS);
        slots.remove(2);//remove uniqueId
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
        slots.remove(1);//remove uuid
        addQueryParam(slots, XDSConstants.QRY_DOCUMENT_UNIQUE_ID, "'1.2.3.4'");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11908: uniqueid
     * Queries for:
     *    XDSDocumentUniqueId: 1.20.3.4.2.2.2
     * returns LeafClass   
     * must return: 1 Folder
     */
    public void testGetFoldersForDocUniqueId() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetFoldersForDocument, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_DOCUMENT_UNIQUE_ID, "'"+DOC_B_UNIQUE_ID+"'");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setFolderUUIDs(FOLDER_DOC_B_UUID)
                .setNrOfDocs(0).setNrOfAssocs(0).setNrOfObjRefs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11908: uuid
     * Queries for:
     *    XDSDocumentEntryUUID: UIUID of DOC_C
     * returns LeafClass   
     * must return: 1 Folder
     */
    public void testGetFoldersForDocUuid() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_GetFoldersForDocument, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_UUID, "'"+DOC_C_UUID+"'");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setFolderUUIDs(FOLDER_DOC_C_D_UUID)
                .setNrOfDocs(0).setNrOfAssocs(0).setNrOfSubmissions(0).checkResponse(rsp);
    }
    
}
