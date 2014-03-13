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



import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.ws.registry.XDSRegistryBean;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */
public class SQFindDocumentsByReferenceIDsTests extends AbstractSQTests {
    private XDSRegistryBean session;
    
    private static final String REF_ID_ACCESSION_112233 = "112233^^^&1.2.3.4.5.6&ISO^urn:ihe:iti:xds:2013:accession";
    private static final String REF_ID_ACCESSION_445566 = "445566^^^&1.2.3.4.5.6&ISO^urn:ihe:iti:xds:2013:accession";
    private static final String REF_ID_UID_1 = "1.2.3.12.78.23^^^^urn:ihe:iti:xds:2013:uniqueId";
    private static final String REF_ID_UID_2 = "1.2.3.12.78.99.1^^^^urn:ihe:iti:xds:2013:uniqueId";
    private static final String REF_ID_UID_3 = "1.2.3.12.78.99.2^^^^urn:ihe:iti:xds:2013:uniqueId";

    private static final SlotType1[] DEFAULT_PARAMS = new SlotType1[] {
        toQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_PATIENT_ID, TEST_PAT_ID),
        toQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_STATUS, "('"+XDSConstants.STATUS_APPROVED+"')")
    };

    public SQFindDocumentsByReferenceIDsTests(XDSRegistryBean session) {
        this.session = session;
    }

    /**
     * Check error handling if required Query Parameters are missing
     */
    public void findDocumentsByRefenceIDsCheckMissingParam() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocumentsByReferenceId, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_STATUS, "('"+XDSConstants.STATUS_APPROVED+"')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck().setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        chk.checkResponse(rsp);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_PATIENT_ID, TEST_PAT_ID);
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
        req.getAdhocQuery().getSlot().clear();
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_REFERENCED_ID_LIST, REF_ID_ACCESSION_112233);
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
    }

    /**
     * Query for document with Accessionnumber 112233
     * returns LeafClass   
     * must return: DocA
     */
    public void findDocumentsByAccession112233() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocumentsByReferenceId, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_REFERENCED_ID_LIST, REF_ID_ACCESSION_112233);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck();
        chk.setDocUUIDs(DOC_A_UUID);
        chk.setDocNames(DOC_A);
        chk.setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0);
        chk.checkResponse(rsp);
    }
    
}
