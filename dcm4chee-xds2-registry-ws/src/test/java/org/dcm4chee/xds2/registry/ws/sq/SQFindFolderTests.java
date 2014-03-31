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



import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.registry.ws.XDSRegistryBean;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */
public class SQFindFolderTests extends AbstractSQTests {
    private XDSRegistryBean session;

    private static final SlotType1[] DEFAULT_PARAMS = new SlotType1[] {
        toQueryParam(XDSConstants.QRY_FOLDER_PATIENT_ID, TEST_PAT_ID),
        toQueryParam(XDSConstants.QRY_FOLDER_STATUS, "('"+XDSConstants.STATUS_APPROVED+"')")
    };

    public SQFindFolderTests(XDSRegistryBean session) {
        this.session = session;
    }

    /**
     * Not in Pre-Con Test 11899
     * Check error handling if required Query Parameters are missing
     */
    public void findFoldersCheckMissingParam() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindFolders, XDSConstants.QUERY_RETURN_TYPE_LEAF, null);
        addQueryParam(XDSConstants.QRY_FOLDER_STATUS, "('"+XDSConstants.STATUS_APPROVED+"')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck().setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        chk.checkResponse(rsp);
        req.getAdhocQuery().getSlot().clear();
        addQueryParam(XDSConstants.QRY_FOLDER_PATIENT_ID, TEST_PAT_ID);
        rsp = session.documentRegistryRegistryStoredQuery(req);
        chk.checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11899: simple
     * Queries for:
     * Basic query using patient ID and status
     * returns LeafClass   
     * must return: 2 Folders
     */
    public void findFoldersSimple() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindFolders, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setFolderUUIDs(FOLDER_DOC_B_UUID, FOLDER_DOC_C_D_UUID)
                .setNrOfSubmissions(0).setNrOfDocs(0).setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11899: in_updatetime
     * Queries for:
     * XDSFolderLastUpdateTimeFrom: (lastYear)
     * XDSFolderLastUpdateTimeTo: now (today)
     * returns LeafClass   
     * must return: 2 Folders
     */
    public void findFoldersLastUpdateTimeIn() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindFolders, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String now = sdf.format(cal.getTime());
        cal.add(Calendar.YEAR, -1);
        String lastYear = sdf.format(cal.getTime());
        addQueryParam(XDSConstants.QRY_FOLDER_LAST_UPDATE_TIME_FROM, lastYear);
        addQueryParam(XDSConstants.QRY_FOLDER_LAST_UPDATE_TIME_TO, now);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setNrOfFolders(2).setNrOfDocs(0).setNrOfSubmissions(0)
            .setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11899: out_updatetime
     * Queries for:
     * XDSFolderLastUpdateTimeFrom: 2001
     * XDSFolderLastUpdateTimeTo: 2003
     * returns LeafClass   
     * must return: 0 Folders
     */
    public void findFoldersLastUpdateTimeOut() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindFolders, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_FOLDER_LAST_UPDATE_TIME_FROM, "2001");
        addQueryParam(XDSConstants.QRY_FOLDER_LAST_UPDATE_TIME_TO, "2003");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setNrOfFolders(0).setNrOfDocs(0).setNrOfSubmissions(0)
            .setNrOfAssocs(0).checkResponse(rsp);
    }
    /**
     * Pre-Con Test 11899: codelist
     * Queries for:
     * XDSFolderCodeList: 2001
     * returns LeafClass   
     * must return: 2 folders
     */
    public void findFoldersCodeList() {
        AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindFolders, XDSConstants.QUERY_RETURN_TYPE_LEAF, DEFAULT_PARAMS);
        addQueryParam(XDSConstants.QRY_FOLDER_CODE_LIST, "('Referrals^^Connect-a-thon folderCodeList')");
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        new QueryResultCheck().setNrOfFolders(2).setNrOfDocs(0).setNrOfSubmissions(0)
            .setNrOfAssocs(0).checkResponse(rsp);
    }

}
