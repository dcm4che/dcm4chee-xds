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

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.impl.JPAQuery;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.persistence.*;
import org.dcm4chee.xds2.registry.ws.XDSPersistenceWrapper;
import org.dcm4chee.xds2.registry.ws.XDSRegistryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Stored Query Implementation for FindDocuments 
 * (urn:uuid:14d4debf-8f97-4251-9a74-a90016b0af0d)
 * 
 * @author franz.willer@gmail.com
 *
 */
public class GetAllQuery extends StoredQuery {

    private static Logger log = LoggerFactory.getLogger(GetAllQuery.class);

    public GetAllQuery(AdhocQueryRequest req, XDSRegistryBean session) throws XDSException {
        super(req, session);
    }
    
    public AdhocQueryResponse query() throws XDSException {
        AdhocQueryResponse rsp = initAdhocQueryResponse();
        ArrayList<RegistryObject> objects = new ArrayList<>();
        objects.addAll(getDocuments());
        objects.addAll(getSubmissionSets());
        objects.addAll(getFolders());
        objects.addAll(getAssociations(objects));

        log.info("Found {} Objects", objects.size());
       
        rsp.setRegistryObjectList(new XDSPersistenceWrapper(getSession()).toRegistryObjectListType(objects, isLeafClass()));
        return rsp;
    }

    private List<XDSDocumentEntry> getDocuments() throws XDSException {
        BooleanBuilder builder = new BooleanBuilder();
        addPatientIdMatch(builder, getQueryParam(XDSConstants.QRY_PATIENT_ID));
        addStatusMatch(builder, QXDSDocumentEntry.xDSDocumentEntry.status, getQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_STATUS));
        
        addXdsCodeMatch(builder, getQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CONFIDENTIALITY_CODE), 
                XDSConstants.UUID_XDSDocumentEntry_confidentialityCode, QXDSDocumentEntry.xDSDocumentEntry.xdsCodes);
        addXdsCodeMatch(builder, getQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_FORMAT_CODE), 
                XDSConstants.UUID_XDSDocumentEntry_formatCode, QXDSDocumentEntry.xDSDocumentEntry.xdsCodes);
        
        JPAQuery query = new JPAQuery(getSession().getEntityManager());
        List<XDSDocumentEntry> docs = query.from(QXDSDocumentEntry.xDSDocumentEntry)
        .innerJoin(QXDSDocumentEntry.xDSDocumentEntry.patient, QXADPatient.xADPatient)
        .innerJoin(QXADPatient.xADPatient.issuerOfPatientID, QXADIssuer.xADIssuer)
        .where(builder)
        .list(QXDSDocumentEntry.xDSDocumentEntry);
        log.debug("#### Found Documents:"+docs);
        return docs;
    }

    private List<XDSSubmissionSet> getSubmissionSets() {
        BooleanBuilder builder = new BooleanBuilder();
        addPatientIdMatch(builder, getQueryParam(XDSConstants.QRY_PATIENT_ID));
        addStatusMatch(builder, QXDSSubmissionSet.xDSSubmissionSet.status, getQueryParam(XDSConstants.QRY_SUBMISSIONSET_STATUS));
        JPAQuery query = new JPAQuery(getSession().getEntityManager());
        List<XDSSubmissionSet> submissionSets = query.from(QXDSSubmissionSet.xDSSubmissionSet)
        .innerJoin(QXDSSubmissionSet.xDSSubmissionSet.patient, QXADPatient.xADPatient)
        .innerJoin(QXADPatient.xADPatient.issuerOfPatientID, QXADIssuer.xADIssuer)
        .where(builder)
        .list(QXDSSubmissionSet.xDSSubmissionSet);
        log.debug("#### Found SubmissionSets:"+submissionSets);
        return submissionSets;
    }

    private List<XDSFolder> getFolders() {
        JPAQuery query = new JPAQuery(getSession().getEntityManager());
        BooleanBuilder builder = new BooleanBuilder();
        addPatientIdMatch(builder, getQueryParam(XDSConstants.QRY_PATIENT_ID));
        addStatusMatch(builder, QXDSFolder.xDSFolder.status, getQueryParam(XDSConstants.QRY_FOLDER_STATUS));
        List<XDSFolder> folders = query.from(QXDSFolder.xDSFolder)
        .innerJoin(QXDSFolder.xDSFolder.patient, QXADPatient.xADPatient)
        .innerJoin(QXADPatient.xADPatient.issuerOfPatientID, QXADIssuer.xADIssuer)
        .where(builder)
        .list(QXDSFolder.xDSFolder);
        log.debug("#### Found Folders:"+folders);
        return folders;
    }

    private Collection<? extends Association> getAssociations(
            ArrayList<RegistryObject> objects) {

        // otherwise 'in' query fails
        if (objects.isEmpty()) return new ArrayList<>();

        JPAQuery query = new JPAQuery(getSession().getEntityManager());
        BooleanBuilder builder = new BooleanBuilder();
        builder.or(QAssociation.association.sourceObject.in(objects))
                .or(QAssociation.association.targetObject.in(objects));
        List<Association> assocs = query.from(QAssociation.association)
                .where(builder)
                .list(QAssociation.association);
        log.debug("#### Found Association:" + assocs);
        return assocs;
    }

    @Override
    public String[] getRequiredParameterNames() {
        return new String[]{XDSConstants.QRY_PATIENT_ID, 
                XDSConstants.QRY_DOCUMENT_ENTRY_STATUS, XDSConstants.QRY_SUBMISSIONSET_STATUS, XDSConstants.QRY_FOLDER_STATUS};
    }
    
}
