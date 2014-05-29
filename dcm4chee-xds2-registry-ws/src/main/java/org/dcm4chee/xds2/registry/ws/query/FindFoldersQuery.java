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

package org.dcm4chee.xds2.registry.ws.query;

import java.util.List;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.persistence.QXADIssuer;
import org.dcm4chee.xds2.persistence.QXADPatient;
import org.dcm4chee.xds2.persistence.QXDSFolder;
import org.dcm4chee.xds2.persistence.QXDSSubmissionSet;
import org.dcm4chee.xds2.persistence.XDSFolder;
import org.dcm4chee.xds2.registry.ws.XDSPersistenceWrapper;
import org.dcm4chee.xds2.registry.ws.XDSRegistryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.impl.JPAQuery;

/**
 * Stored Query Implementation for FindFolders 
 * (urn:uuid:958f3006-baad-4929-a4de-ff1114824431)
 * 
 * @author franz.willer@gmail.com
 *
 */
public class FindFoldersQuery extends StoredQuery {

    private static Logger log = LoggerFactory.getLogger(FindFoldersQuery.class);

    public FindFoldersQuery(AdhocQueryRequest req, XDSRegistryBean session) throws XDSException {
        super(req, session);
    }
    
    public AdhocQueryResponse query() throws XDSException {
        AdhocQueryResponse rsp = initAdhocQueryResponse();
        BooleanBuilder builder = new BooleanBuilder();
        addPatientIdMatch(builder, getQueryParam(XDSConstants.QRY_FOLDER_PATIENT_ID));
        addStatusMatch(builder, QXDSFolder.xDSFolder.status, getQueryParam(XDSConstants.QRY_FOLDER_STATUS));
        
        addFromToMatch(builder, getQueryParam(XDSConstants.QRY_FOLDER_LAST_UPDATE_TIME_FROM),
                getQueryParam(XDSConstants.QRY_FOLDER_LAST_UPDATE_TIME_TO),
                QXDSFolder.xDSFolder.pk, XDSConstants.SLOT_NAME_LAST_UPDATE_TIME);
        
        addExternalIdentifierMatch(builder, getQueryParam(XDSConstants.QRY_SUBMISSIONSET_SOURCE_ID),
                XDSConstants.UUID_XDSSubmissionSet_sourceId, QXDSSubmissionSet.xDSSubmissionSet.pk);

        /* TODO: DB_RESTRUCT addSlotValueInClassificationMatch(builder, getQueryParam(XDSConstants.QRY_SUBMISSIONSET_AUTHOR_PERSON), 
                XDSConstants.UUID_XDSSubmissionSet_autor, XDSConstants.SLOT_NAME_AUTHOR_PERSON, 
                QXDSSubmissionSet.xDSSubmissionSet.pk); */

        addXdsCodeMatch(builder, getQueryParam(XDSConstants.QRY_SUBMISSIONSET_CONTENT_TYPE), 
                XDSConstants.UUID_XDSSubmissionSet_contentTypeCode, QXDSSubmissionSet.xDSSubmissionSet.xdsCodes);

        JPAQuery query = new JPAQuery(getSession().getEntityManager());
        List<XDSFolder> folders = query.from(QXDSFolder.xDSFolder)
        .innerJoin(QXDSFolder.xDSFolder.patient, QXADPatient.xADPatient)
        .innerJoin(QXADPatient.xADPatient.issuerOfPatientID, QXADIssuer.xADIssuer)
        .where(builder)
        .list(QXDSFolder.xDSFolder);
        log.info("#### Found Folders:"+folders);
        rsp.setRegistryObjectList(new XDSPersistenceWrapper(getSession()).toRegistryObjectListType(folders, isLeafClass()));
        return rsp;
    }

    @Override
    public String[] getRequiredParameterNames() {
        return new String[]{XDSConstants.QRY_FOLDER_PATIENT_ID, XDSConstants.QRY_FOLDER_STATUS};
    }
    
}
