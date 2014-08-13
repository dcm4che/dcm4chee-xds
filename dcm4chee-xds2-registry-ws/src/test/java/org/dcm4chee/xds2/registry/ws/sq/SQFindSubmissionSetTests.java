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
 * @author Franz Willer <franz.willer@gmail.com>
 */
public class SQFindSubmissionSetTests extends AbstractSQTests {
    private XDSRegistryBean session;

    private static final SlotType1[] DEFAULT_PARAMS = new SlotType1[] {
        toQueryParam(XDSConstants.QRY_SUBMISSIONSET_PATIENT_ID, TEST_PAT_ID),
        toQueryParam(XDSConstants.QRY_SUBMISSIONSET_STATUS, "('"+XDSConstants.STATUS_APPROVED+"')")
    };

    public SQFindSubmissionSetTests(XDSRegistryBean session) {
        this.session = session;
    }

    /**
     * Not in Pre-Con Test 11898
     * Check error handling if required Query Parameters are missing
     */
    public void findSubmissionSetCheckMissingParam() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindSubmissionSets, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_STATUS, "('"+XDSConstants.STATUS_APPROVED+"')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck().setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        chk.checkResponse(rsp);
        req.getAdhocQuery().getSlot().clear();
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_PATIENT_ID, "'test1234_1^^^&1.2.3.45.4.3.2.1&ISO'");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11898: simple
     * Queries for:
     * Basic query using patient ID and status
     * returns LeafClass   
     * must return: 5 SubmissionSets
     */
    public void findSubmissionSetSimple() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindSubmissionSets, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck();
        chk.setSubmUUIDs(SUBM_DOC_A_UUID,
                SUBM_DOC_B_UUID,SUBM_DOC_E_UUID,
                SUBM_DOC_C_D_UUID,SUBM_DOC_F_UUID
                );
        chk.setNrOfFolders(0).setNrOfDocs(0).setNrOfAssocs(0);
        chk.checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11898: other_source_id
     * Queries for:
     * XDSSubmissionSetSourceId: '3670984664a'
     * returns LeafClass   
     * must return: none
     */
    public void findSubmissionSetOtherSourceId() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindSubmissionSets, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_SOURCE_ID, "('3670984664a')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setNrOfDocs(0).setNrOfFolders(0)
            .setNrOfSubmissions(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11898: submissiontime_in
     * Queries for:
     * XDSSubmissionSetSubmissionTimeFrom: '200412'
     * XDSSubmissionSetSubmissionTimeTo: '200501'
     * returns LeafClass   
     * must return: 5 SubmissionSets
     */
    public void findSubmissionSetSubmissionTimeIn() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindSubmissionSets, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_SUBMISSION_TIME_FROM, "200412");
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_SUBMISSION_TIME_TO, "200501");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setSubmUUIDs(SUBM_DOC_A_UUID,
                SUBM_DOC_B_UUID,SUBM_DOC_E_UUID, SUBM_DOC_C_D_UUID,SUBM_DOC_F_UUID
                ).setNrOfDocs(0).setNrOfFolders(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11898: submissiontime_out
     * Queries for:
     * XDSSubmissionSetSubmissionTimeFrom: '200312'
     * XDSSubmissionSetSubmissionTimeTo: '200401'
     * returns LeafClass   
     * must return: none
     */
    public void findSubmissionSetSubmissionTimeOut() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindSubmissionSets, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_SUBMISSION_TIME_FROM, "200312");
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_SUBMISSION_TIME_TO, "200401");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setNrOfSubmissions(0).setNrOfDocs(0).setNrOfFolders(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11898: submissiontime_no_start
     * Queries for:
     * XDSSubmissionSetSubmissionTimeTo: '200501'
     * returns LeafClass   
     * must return: 5 SubmissionSets
     */
    public void findSubmissionSetSubmissionTimeNoStart() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindSubmissionSets, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_SUBMISSION_TIME_TO, "200501");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setNrOfSubmissions(5).setNrOfDocs(0).setNrOfFolders(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11898: submissiontime_no_end
     * Queries for:
     * XDSSubmissionSetSubmissionTimeFrom: '200412'
     * returns LeafClass   
     * must return: 5 SubmissionSets
     */
    public void findSubmissionSetSubmissionTimeNoEnd() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindSubmissionSets, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_SUBMISSION_TIME_FROM, "200412");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setNrOfSubmissions(5).setNrOfDocs(0).setNrOfFolders(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11898: submissiontime_no_end
     * Queries for:
     * XDSSubmissionSetSubmissionTimeFrom: '200412'
     * returns LeafClass   
     * must return: 5 SubmissionSets
     */
    public void findSubmissionSetSubmissionTimeEdge() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindSubmissionSets, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        List<SlotType1> slots = req.getAdhocQuery().getSlot();
        addQueryParam(slots, XDSConstants.QRY_SUBMISSIONSET_SUBMISSION_TIME_FROM, "20041225235050");
        addQueryParam(slots, XDSConstants.QRY_SUBMISSIONSET_SUBMISSION_TIME_TO, "20041226235050");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck().setNrOfSubmissions(5).setNrOfDocs(0).setNrOfFolders(0).setNrOfAssocs(0);
        chk.checkResponse(rsp);
        slots.remove(3);//remove to
        addQueryParam(slots, XDSConstants.QRY_SUBMISSIONSET_SUBMISSION_TIME_TO, "20041226235049");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.setNrOfSubmissions(4).checkResponse(rsp);
        slots.remove(3);slots.remove(2);//remove from to
        addQueryParam(slots, XDSConstants.QRY_SUBMISSIONSET_SUBMISSION_TIME_FROM, "20041225235051");
        addQueryParam(slots, XDSConstants.QRY_SUBMISSIONSET_SUBMISSION_TIME_TO, "20041226235051");
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.setNrOfSubmissions(1).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11898: author_all
     * Queries for:
     * XDSSubmissionSetAuthorPerson: '%Dopplemeyer%'
     * returns LeafClass   
     * must return: 5 SubmissionSets
     */
    public void findSubmissionSetAuthorAll() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindSubmissionSets, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_AUTHOR_PERSON, "'%Dopplemeyer%'");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setNrOfSubmissions(5).setNrOfDocs(0).setNrOfFolders(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11898: author_none
     * Queries for:
     * XDSSubmissionSetAuthorPerson: '%Smith%'
     * returns LeafClass   
     * must return: 0 SubmissionSets
     */
    public void findSubmissionSetAuthorNone() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindSubmissionSets, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_AUTHOR_PERSON, "'%Smith%'");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setNrOfSubmissions(0).setNrOfDocs(0).setNrOfFolders(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11898: contenttype_all
     * Queries for:
     * XDSSubmissionSetContentType: 'History and Physical^^dcm4che contentTypeCodes'
     * returns LeafClass   
     * must return: 0 SubmissionSets
     */
    public void findSubmissionSetContentTypeAll() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindSubmissionSets, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_CONTENT_TYPE, "('History and Physical^^dcm4che contentTypeCodes')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setNrOfSubmissions(5).setNrOfDocs(0).setNrOfFolders(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11898: contenttype_none
     * Queries for:
     * XDSSubmissionSetContentType: 'Surgery Report^^Fake Code Set'
     * returns LeafClass   
     * must return: 0 SubmissionSets
     */
    public void findSubmissionSetContentTypeNone() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindSubmissionSets, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_SUBMISSIONSET_CONTENT_TYPE, "('Surgery Report^^Fake Code Set')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setNrOfSubmissions(0).setNrOfDocs(0).setNrOfFolders(0).setNrOfAssocs(0).checkResponse(rsp);
    }

}
