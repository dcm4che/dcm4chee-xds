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

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.registry.ws.XDSRegistryBean;
import org.dcm4chee.xds2.registry.ws.XDSTestUtil;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */
public class SQFindDocumentsTests extends AbstractSQTests {
    private XDSRegistryBean session;

    private static final SlotType1[] DEFAULT_PARAMS = new SlotType1[] {
        toQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_PATIENT_ID, TEST_PAT_ID),
        toQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_STATUS, "('"+XDSConstants.STATUS_APPROVED+"')")
    };

    public SQFindDocumentsTests(XDSRegistryBean session) {
        this.session = session;
    }

    /**
     * Not in Pre-Con Test 11897
     * Check error handling if required Query Parameters are missing
     */
    public void findDocumentsCheckMissingParam() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_STATUS, "('"+XDSConstants.STATUS_APPROVED+"')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck().setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        chk.checkResponse(rsp);
        req.getAdhocQuery().getSlot().clear();
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_PATIENT_ID, TEST_PAT_ID);
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
    }
    /**
     * Not in Pre-Con Test
     * Queries for:
     * all approved documents, 
     * returns LeafClass   
     * must return: 0 documents (patient is merged)
     */
    public void findDocumentsMergedPID() throws Exception {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        String pid = XDSTestUtil.TEST_PID_1+XDSTestUtil.TEST_ISSUER;
        String mpid = XDSTestUtil.TEST_PID_MERGED+XDSTestUtil.TEST_ISSUER;
        session.getPatient(mpid, true);
        session.linkPatient(pid, mpid);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        session.linkPatient(pid, null);
        QueryResultCheck chk = new QueryResultCheck();
        chk.setNrOfDocs(0).setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0);
        chk.checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: leafclass
     * Queries for:
     * all approved documents, DocE is deprecated so not returned
     * returns LeafClass   
     * must return: DocA, DocB, DocC, DocD, DocF
     */
    public void findDocumentsApproved() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck();
        chk.setDocUUIDs(DOC_A_UUID, 
                DOC_B_UUID, 
                DOC_C_UUID, 
                DOC_D_UUID, 
                DOC_F_UUID);
        chk.setDocNames(DOC_A, DOC_B, DOC_C, DOC_D, DOC_F);
        chk.setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0);
        chk.checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: object_refs
     * Queries for:
     * all approved documents, DocE is deprecated so not returned
     * returns objectRefs   
     * must return: DocA, DocB, DocC, DocD, DocF
     */
    public void findDocumentsObjectRefs() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_OBJREF, DEFAULT_PARAMS);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck();
        chk.setObjRefsUUIDs(DOC_A_UUID, 
                DOC_B_UUID, 
                DOC_C_UUID, 
                DOC_D_UUID, 
                DOC_F_UUID);
        chk.setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0);
        chk.checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: deprecated
     * queries for:
     * status = 'Deprecated'
     * must return: DocE
     */
    public void findDocumentsDeprecated() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_PATIENT_ID, TEST_PAT_ID);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_STATUS, "('"+XDSConstants.STATUS_DEPRECATED+"')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck();
        chk.setDocUUIDs(DOC_E_UUID);
        chk.setDocNames(DOC_E);
        chk.setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0);
        chk.checkResponse(rsp);
    }
    
    /**
     * NOT included in Pre-Con Test 11897
     * Queries for:
     * all approved or deprecated documents (all objects)
     * returns LeafClass   
     * must return: DocA, DocB, DocC, DocD, DocE, DocF
     */
    public void findDocumentsApprovedAndDeprecated() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_PATIENT_ID, TEST_PAT_ID);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_STATUS, 
                "('"+XDSConstants.STATUS_APPROVED+"','"+XDSConstants.STATUS_DEPRECATED+"')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck();
        chk.setDocUUIDs(DOC_A_UUID, 
                DOC_B_UUID, 
                DOC_C_UUID, 
                DOC_D_UUID, 
                DOC_E_UUID, 
                DOC_F_UUID);
        chk.setDocNames(DOC_A, DOC_B, DOC_C, DOC_D, DOC_E, DOC_F);
        chk.setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0);
        chk.checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: classcode_one
     * Queries for: 
     * classCode = 'Consult'
     * status = 'Approved'
     * must return: DocD
     */
    public void findDocumentsClassCodeOne() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CLASS_CODE, 
                "('Consult^^dcm4che classCodes')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck();
        chk.setDocNames(DOC_D);
        chk.setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0);
        chk.checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: classcode_two
     * Queries for: 
     * classCode = 'Consult' or 'History and Physical'
     * status = 'Approved'
     * must return: DocD, DocE, DocA
     */
    public void findDocumentsClassCodeTwo() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CLASS_CODE, 
                "('Consult^^dcm4che classCodes','History and Physical^^dcm4che classCodes')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocUUIDs(DOC_A_UUID,
                DOC_D_UUID,
                DOC_F_UUID)
        .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: classcode_scheme_mismatch
     * Queries for: 
     * classCode = 'Communication^^Connect-a-thon classCodes' or 'a^^'
     * status = 'Approved'
     * must return: Error (second code value has no scheme)
     */
    public void findDocumentsClassCodeMismatch() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CLASS_CODE, 
                "('Communication^^Connect-a-thon classCodes','a^^')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setStatus(XDSConstants.XDS_B_STATUS_FAILURE).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: practicesetting
     * Queries for: 
     * classCode = Dialysis^^dcm4che practiceSettingCodes' or 'Dialysis^^dcm4che practiceSettingCodes 2'
     * status = 'Approved'
     * must return: DocD, DocE, DocA
     */
    public void findDocumentsPracticeSetting() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_PRACTICE_SETTING_CODE, 
                "('Dialysis^^dcm4che practiceSettingCodes','Dialysis^^dcm4che practiceSettingCodes 2')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocUUIDs(DOC_A_UUID,
                DOC_D_UUID,
                DOC_F_UUID)
                .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: practicesetting_scheme
     * Queries for: 
     * classCode = Dialysis^^dcm4che practiceSettingCodes' or 'Dialysis^^dcm4che practiceSettingCodes 2'
     * status = 'Approved'
     * must return: DocA, DocD
     * 
     * NOTE: there is a parameter $XDSDocumentEntryPracticeSettingCodeScheme
     * in this stored query.  This is not a mistake.   Extra parameters
     * must be accepted and ignored by the stored query request parser.
     * This parameter is no longer valid given the Code Type parameter
     * format change but the parameter must still be accepted (and ignored)
     */
    public void findDocumentsPracticeSettingScheme() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_PRACTICE_SETTING_CODE, 
                "('Dialysis^^dcm4che practiceSettingCodes')");
        addQueryParam("$XDSDocumentEntryPracticeSettingCodeScheme", 
                "('dcm4che practiceSettingCodes')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocUUIDs(DOC_A_UUID,
                DOC_D_UUID)
                .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: classcode_practicesetting
     * Queries for:
     * classCode = 'Communication'
     * practiceSettingCode = 'Cardiology'
     * status = 'Approved'
     * must return: DocB
     */
    public void findDocumentsClasscodePracticesetting() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CLASS_CODE, 
                "('Communication^^dcm4che classCodes')");
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_PRACTICE_SETTING_CODE, 
                "('Cardiology^^dcm4che practiceSettingCodes')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocUUIDs(DOC_B_UUID)
        .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: classcode_scheme_2
     * Queries for:
     * classCode = ('Communication', 'Communication')
     * classCodeScheme = ('dcm4che classCodes','dcm4che classCodes 2')
     * status = 'Approved'
     * must return: DocB, DocC
     */
    public void findDocumentsClassCodeScheme2() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CLASS_CODE, 
                "('Communication^^dcm4che classCodes', 'Communication^^dcm4che classCodes 2')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocNames(DOC_B, DOC_C)
        .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: confcode
     * queries for:
     * confidentialityCode = ('1.3.6.1.4.1.21367.2006.7.101',
     *                        '1.3.6.1.4.1.21367.2006.7.103')
     * status = 'Approved'
     * must return: DocB, DocC
     */
    public void findDocumentsConfCode() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CONFIDENTIALITY_CODE, 
                "('1.3.6.1.4.1.21367.2006.7.101^^dcm4che confidentialityCodes', " +
                "'1.3.6.1.4.1.21367.2006.7.103^^dcm4che confidentialityCodes')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocNames(DOC_B, DOC_C)
        .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: creationtime_between
     * queries for:
     *  creationTimeFrom: 20040101
     *  creationTimeTo:  20050101
     * status = 'Approved'
     * must return: DocB
     */
    public void findDocumentsCreationTimeBetween() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CREATION_TIME_FROM,"20040101");
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CREATION_TIME_TO,"20050101");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocNames(DOC_B)
        .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: creationtime_between_long
     * queries for:
     *  creationTimeFrom: 200401010934
     *  creationTimeTo:  200501010856
     * status = 'Approved'
     * must return: DocB
     */
    public void findDocumentsCreationTimeBetweenLong() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CREATION_TIME_FROM,"200401010934");
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CREATION_TIME_TO,"200501010856");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocNames(DOC_B)
        .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: creationtime_left_edge
     * queries for:
     *  creationTimeFrom: 20041224
     *  creationTimeTo:  20050101
     * status = 'Approved'
     * must return: DocB
     */
    public void findDocumentsCreationTimeLeftEdge() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CREATION_TIME_FROM,"20040101");
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CREATION_TIME_TO,"20050101");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocNames(DOC_B)
        .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: creationtime_right_edge
     * queries for:
     *  creationTimeFrom: 20041124
     *  creationTimeTo:  20041224
     * status = 'Approved'
     * must return: none
     */
    public void findDocumentsCreationTimeRightEdge() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CREATION_TIME_FROM,"20041124");
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CREATION_TIME_TO,"20041224");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setNrOfDocs(0).setNrOfFolders(0).setNrOfSubmissions(0)
        .setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: creationtime_practice_setting
     * queries for:
     *  creationTimeFrom: 20041124
     *  creationTimeTo:  20041224
     * status = 'Approved'
     * must return: none
     */
    public void findDocumentsCreationTimePracticeSetting() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CREATION_TIME_FROM,"20020101");
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CREATION_TIME_TO,"20060101");
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_PRACTICE_SETTING_CODE, 
            "('Dialysis^^dcm4che practiceSettingCodes')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocNames(DOC_D)
        .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: servicestarttime
     * queries for:
     *  serviceStartTimeFrom: 2005
     *  serviceStartTimeTo: 2006
     * status = 'Approved'
     * must return: DocC, DocD
     */
    public void findDocumentsServiceStartTime() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_SERVICE_START_TIME_FROM,"2005");
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_SERVICE_START_TIME_TO,"2006");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocNames(DOC_C, DOC_D)
        .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: servicestoptime
     * queries for:
     *  serviceStartTimeFrom: 2005
     *  serviceStartTimeTo: 2006
     * status = 'Approved'
     * must return: DocC, DocD
     */
    public void findDocumentsServiceStopTime() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_SERVICE_STOP_TIME_FROM,"2005");
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_SERVICE_STOP_TIME_TO,"2006");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocNames(DOC_C, DOC_D)
        .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: eventcode
     * queries for:
     *  eventCodeList = 'Colonoscopy'
     * status = 'Approved'
     * must return: DocB
     */
    public void findDocumentsEventCode() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_EVENT_CODE_LIST, 
            "('Colonoscopy^^dcm4che eventCodeList')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocNames(DOC_B)
        .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: eventcode_multi_select (and also test 'and')
     * queries for:
     *  eventCodeList = 'T-D4909^^dcm4che SNM3' AND 'dcm4che T-62002^^SNM3'
     * status = 'Approved'
     * must return: DocA
     */
    public void findDocumentsEventCodeMulti() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_OBJREF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_EVENT_CODE_LIST, 
            "('T-D4909^^dcm4che SNM3')");
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_EVENT_CODE_LIST, 
        "('T-62002^^dcm4che SNM3')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setObjRefsUUIDs(DOC_A_UUID)
        .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Not in Pre-Con Test 11897: 
     * queries for:
     *  eventCodeList = 'Colonoscopy' or both 'T-D4909' and 'T-62002'
     * status = 'Approved'
     * must return: DocA, DocB
     */
    public void findDocumentsEventCodeAndOr() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_EVENT_CODE_LIST, 
            "('T-D4909^^dcm4che SNM3', 'Colonoscopy^^dcm4che eventCodeList')");
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_EVENT_CODE_LIST, 
            "('T-62002^^dcm4che SNM3', 'Colonoscopy^^dcm4che eventCodeList')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocNames(DOC_A, DOC_B)
        .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: formatcode
     * queries for:
     *  eventCodeList = 'CDAR2/IHE 1.0'
     * status = 'Approved'
     * must return: DocA, DocB, DocF, DocC
     */
    public void findDocumentsFormatCode() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_FORMAT_CODE, 
            "('CDAR2/IHE 1.0^^dcm4che formatCodes')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocNames(DOC_A, DOC_B, DOC_F, DOC_C)
        .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: typecode
     * queries for:
     *  typeCode = '34108-1^^dcm4che LOINC'
     * status = 'Approved'
     * must return: DocA, DocB, DocF, DocC
     */
    public void findDocumentsTypeCode() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_OBJREF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_TYPE_CODE, 
            "('34108-1^^dcm4che LOINC')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setObjRefsUUIDs(DOC_A_UUID,
                DOC_B_UUID,DOC_C_UUID,
                DOC_D_UUID,DOC_F_UUID)
        .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: hcftc
     * queries for:
     *  healthcareFacilityTypeCode = 'Outpatient^^dcm4che healthcareFacilityTypeCodes' or 
     *                  'Outpatient^^dcm4che healthcareFacilityTypeCodes 2'
     * status = 'Approved'
     * must return: DocA, DocF, DocE
     */
    public void findDocumentsHealthCareFacilityCode() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_HEALTHCARE_FACILITY_TYPE_CODE, 
            "('Outpatient^^dcm4che healthcareFacilityTypeCodes','Outpatient^^dcm4che healthcareFacilityTypeCodes 2')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocNames(DOC_A, DOC_D, DOC_F)
        .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: hcftc_scheme
     * queries for:
     *  healthcareFacilityTypeCode = 'Outpatient^^dcm4che healthcareFacilityTypeCodes'
     * status = 'Approved'
     * must return: DocF, DocE
     */
    public void findDocumentsHealthCareFacilityCodeScheme() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_HEALTHCARE_FACILITY_TYPE_CODE, 
            "('Outpatient^^dcm4che healthcareFacilityTypeCodes')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setDocNames(DOC_D, DOC_F)
        .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: no_matching_classcode
     * queries for:
     *  classcode = '^^blah'
     * status = 'Approved'
     * must return: none
     */
    public void findDocumentsNoMatchingClassCode() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CLASS_CODE, 
            "('^^blah')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setNrOfDocs(0)
        .setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: author
     * queries for:
     *  XDSDocumentEntryAuthorPerson = '%Ford%'
     * status = 'Approved'
     * must return: none
     */
    public void findDocumentsAuthor() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_OBJREF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_AUTHOR_PERSON, 
            "('%Ford%')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setObjRefsUUIDs(DOC_D_UUID)
        .setNrOfDocs(0).setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11897: old_scheme
     * query using old format.  must return error since code not in CE format
     * Queries for:
     * $XDSDocumentEntryPracticeSettingCode = 'Dialysis'
     * $XDSDocumentEntryPracticeSettingCodeScheme = 'dcm4che practiceSettingCodes'
     * status = 'Approved'
     * must return: Error
     */
    public void findDocumentsOldScheme() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_PRACTICE_SETTING_CODE, 
                "('Dialysis')");
        addQueryParam("$XDSDocumentEntryPracticeSettingCodeScheme", 
            "('dcm4che practiceSettingCodes')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setStatus(XDSConstants.XDS_B_STATUS_FAILURE)
        .setErrorCode(XDSException.XDS_ERR_REGISTRY_ERROR).checkResponse(rsp);
    }
    
}
