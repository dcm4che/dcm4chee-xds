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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.ObjectFactory;
import org.dcm4chee.xds2.persistence.Association;
import org.dcm4chee.xds2.persistence.Identifiable;
import org.dcm4chee.xds2.persistence.QAssociation;
import org.dcm4chee.xds2.persistence.QClassificationScheme;
import org.dcm4chee.xds2.persistence.QSlot;
import org.dcm4chee.xds2.persistence.QXADIssuer;
import org.dcm4chee.xds2.persistence.QXADPatient;
import org.dcm4chee.xds2.persistence.QXDSCode;
import org.dcm4chee.xds2.persistence.QXDSDocumentEntry;
import org.dcm4chee.xds2.persistence.RegistryObject;
import org.dcm4chee.xds2.persistence.XADPatient;
import org.dcm4chee.xds2.persistence.XDSCode;
import org.dcm4chee.xds2.persistence.XDSDocumentEntry;
import org.dcm4chee.xds2.registry.ws.XDSRegistryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPASubQuery;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.ExpressionUtils;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.CollectionPath;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;

/**
 * Interface for StoredQuery 
 * 
 * @author franz.willer@gmail.com
 *
 */
public abstract class StoredQuery {

    private AdhocQueryRequest req;
    private XDSRegistryBean session;
    private ObjectFactory factory = new ObjectFactory();

    Map<String, StoredQueryParam> queryParam;
    String patID;
    
    private static Logger log = LoggerFactory.getLogger(StoredQuery.class);

    public StoredQuery(AdhocQueryRequest req, XDSRegistryBean session) throws XDSException {
        this.req = req;
        this.session = session;
        queryParam = StoredQueryParam.getQueryParams(req);
        log.info("AdhocQueryRequest "+req.getAdhocQuery().getId()+": params:"+queryParam.values());
        checkRequiredParameter();
    }
    
    public abstract AdhocQueryResponse query() throws XDSException;
    
    /**
     * Get list of required parameter names for this Stored Query.
     * For a 'one of name1 or name2 or ..' you can specify "name1|name2"
     * 
     * e.g.: a) {"$XDSDocumentEntryUniqueId|$XDSDocumentEntryEntryUUID"}:
     *          one of $XDSDocumentEntryUniqueId or $XDSDocumentEntryEntryUUID must be set
     *       b) {"$XDSDocumentEntryPatientId", "$XDSDocumentEntryStatus"}
     *          $XDSDocumentEntryPatientId AND $XDSDocumentEntryStatus must be set
     * @return
     */
    public abstract String[] getRequiredParameterNames();
    
    public AdhocQueryRequest getRequest() {
        return req;
    }

    public XDSRegistryBean getSession() {
        return session;
    }
    
    public String getPatientID() {
        return patID;
    }

    public static StoredQuery getStoredQuery(AdhocQueryRequest req, XDSRegistryBean session) throws XDSException {
        String qryId = req.getAdhocQuery().getId();
        if (XDSConstants.XDS_FindDocuments.equals(qryId)) {
            return new FindDocumentsQuery(req, session);
        } else if (XDSConstants.XDS_FindDocumentsByReferenceId.equals(qryId)) {
            return new FindDocumentsByReferenceIdQuery(req, session);
        } else if (XDSConstants.XDS_FindSubmissionSets.equals(qryId)) {
            return new FindSubmissionSetQuery(req, session);
        } else if (XDSConstants.XDS_FindFolders.equals(qryId)) {
            return new FindFoldersQuery(req, session);
        } else if (XDSConstants.XDS_GetAll.equals(qryId)) {
            return new GetAllQuery(req, session);
        } else if (XDSConstants.XDS_GetDocuments.equals(qryId)) {
            return new GetDocumentsQuery(req, session);
        } else if (XDSConstants.XDS_GetFolders.equals(qryId)) {
            return new GetFoldersQuery(req, session);
        } else if (XDSConstants.XDS_GetAssociations.equals(qryId)) {
            return new GetAssociationsQuery(req, session);
        } else if (XDSConstants.XDS_GetDocumentsAndAssociations.equals(qryId)) {
            return new GetDocumentsAndAssociationsQuery(req, session);
        } else if (XDSConstants.XDS_GetSubmissionSets.equals(qryId)) {
            return new GetSubmissionSetsQuery(req, session);
        } else if (XDSConstants.XDS_GetSubmissionSetAndContents.equals(qryId)) {
            return new GetSubmissionSetAndContentsQuery(req, session);
        } else if (XDSConstants.XDS_GetFolderAndContents.equals(qryId)) {
            return new GetFolderAndContentsQuery(req, session);
        } else if (XDSConstants.XDS_GetFoldersForDocument.equals(qryId)) {
            return new GetFoldersForDocumentQuery(req, session);
        } else if (XDSConstants.XDS_GetRelatedDocuments.equals(qryId)) {
            return new GetRelatedDocumentsQuery(req, session);
        }
        throw new XDSException(XDSException.XDS_ERR_UNKNOWN_STORED_QUERY_ID, 
                "Unknown Stored Query id:"+qryId, null);
    }
    
    public StoredQueryParam getQueryParam(String name) {
        return queryParam.get(name);
    }
    
    public boolean isLeafClass() {
        return XDSConstants.QUERY_RETURN_TYPE_LEAF.equals(req.getResponseOption().getReturnType());
    }
    
    protected AdhocQueryResponse initAdhocQueryResponse() {
        AdhocQueryResponse rsp = factory.createAdhocQueryResponse();
        rsp.setStatus(XDSConstants.XDS_B_STATUS_SUCCESS);
        return rsp;
    }
    
    protected void checkRequiredParameter() throws XDSException {
        String[] required = getRequiredParameterNames();
        if (required != null && required.length > 0) {
            StringBuilder sb = new StringBuilder();
            StringTokenizer st;
            loop: for (int i = 0 ; i < required.length ; i++) {
                st = new StringTokenizer(required[i], "|");
                while (st.hasMoreElements()) {
                    if (queryParam.containsKey(st.nextToken().trim()) ) {
                        continue loop;
                    }
                }
                sb.append(required[i]).append(", ");
            }
            if (sb.length() > 0) {
                throw new XDSException(XDSException.XDS_ERR_STORED_QUERY_MISSING_PARAM, 
                        this.getClass().getSimpleName()+" - Missing required query parameters: "
                        +sb.substring(0, sb.length()-2), null);
            }
        }
    }

    protected void addPatientIdMatch(BooleanBuilder builder, StoredQueryParam pid) {
        XADPatient qryPat = new XADPatient(pid.getStringValue());
        patID = qryPat.getCXPatientID();
        builder.and(QXADPatient.xADPatient.patientID.eq(qryPat.getPatientID()));
        builder.and(QXADIssuer.xADIssuer.universalID.eq(qryPat.getIssuerOfPatientID().getUniversalID()));
        builder.and(QXADPatient.xADPatient.linkedPatient.isNull());
    }

    protected void addStatusMatch(BooleanBuilder builder, StringPath status, StoredQueryParam statusParam) {
        List<String> stati = statusParam.getMultiValues(0);
        if (stati != null && stati.size() > 0) {
            if (stati.size() == 1) {
                builder.and(status.eq(stati.get(0)));
            } else {
                builder.and(status.in(stati));
            }
        }
    }

    /**
     * Add code match in ebXML style (Classification with Slot and Name)
     * @param builder           BooleanBuilder to add matches for given code parameter
     * @param codeParam         Stored Query Parameter with code values
     * @param codeType          The classification of the codes (Classification.classificationScheme)
     * @param subselectJoinPk   Path to pk to bind subselect to parent select.
     * @throws XDSException 
     */
    protected void addCodeMatch(BooleanBuilder builder, StoredQueryParam codeParam, String codeType, 
            NumberPath<Long> subselectJoinPk) throws XDSException {
        if (codeParam != null) {
            List<String> codeValues;
            String[] codeAndScheme;
            for (int i = 0, len=codeParam.getNumberOfANDElements() ; i < len ; i++) {
                codeValues = codeParam.getMultiValues(i);
                BooleanBuilder codesBuilder = new BooleanBuilder();
                for (int j = 0, jLen = codeValues.size() ; j < jLen ; j++) {
                    codeAndScheme = toCodeValueAndScheme(codeValues.get(j));
                    codesBuilder.or(ExpressionUtils.allOf(QClassificationScheme.classificationScheme.id.eq(codeType),
                            /* TODO: DB_RESTRUCT QClassification.classification.nodeRepresentation.eq(codeAndScheme[0]),*/
                            QSlot.slot.value.eq(codeAndScheme[1])));
                }
               
                /* TODO: DB_RESTRUCT builder.and(new JPASubQuery().from(QClassification.classification)
                .innerJoin(QClassification.classification.classificationScheme, QClassificationScheme.classificationScheme)
                .innerJoin(QClassification.classification.slots, QSlot.slot)
                .where(QClassification.classification.classifiedObject.pk.eq(subselectJoinPk), 
                        codesBuilder).exists());*/
            }
        }
    }
    
    /**
     * Add code matches in XDS form using XDSCode entity
     * 
     * @param builder     BooleanBuilder to add matches for given code parameter
     * @param codeParam   Stored Query Parameter with code values
     * @param codeType    The classification of the codes (Classification.classificationScheme)
     * @param xdsCodes    The CollectionPath for XDSCodes of parent select (to bind subselect)
     * @throws XDSException 
     */
    protected void addXdsCodeMatch(BooleanBuilder builder, StoredQueryParam codeParam, String codeClassification, 
            CollectionPath<XDSCode, QXDSCode> xdsCodes) throws XDSException {
        if (codeParam != null) {
            List<String> codeValues;
            String[] codeAndScheme;
            for (int i = 0, len=codeParam.getNumberOfANDElements() ; i < len ; i++) {
                codeValues = codeParam.getMultiValues(i);
                BooleanBuilder codesBuilder = new BooleanBuilder();
                for (int j = 0, jLen = codeValues.size() ; j < jLen ; j++) {
                    codeAndScheme = toCodeValueAndScheme(codeValues.get(j));
                    codesBuilder.or(ExpressionUtils.allOf(QXDSCode.xDSCode.codeClassification.eq(codeClassification),
                            QXDSCode.xDSCode.codeValue.eq(codeAndScheme[0]),
                            QXDSCode.xDSCode.codingSchemeDesignator.eq(codeAndScheme[1])));
                }
                builder.and(new JPASubQuery().from(QXDSCode.xDSCode)
                .where(QXDSCode.xDSCode.in(xdsCodes), 
                        codesBuilder).exists());
            }
        }
    }

    protected void addFromToMatch(BooleanBuilder builder, StoredQueryParam from,
            StoredQueryParam to, NumberPath<Long> subselectJoinPk,
            String slotName) {
        if (from != null || to != null) {
            Predicate fromTo = from == null ? QSlot.slot.value.lt(to.getStringValue()) : to != null ? 
                    ExpressionUtils.allOf(QSlot.slot.value.goe(from.getStringValue()), QSlot.slot.value.lt(to.getStringValue())) :
                    QSlot.slot.value.goe(from.getStringValue());
            builder.and(new JPASubQuery().from(QSlot.slot)
                    .where(QSlot.slot.parent.pk.eq(subselectJoinPk), 
                            QSlot.slot.name.eq(slotName), fromTo).exists());
        }
    }

    protected void addSlotValueMatch(BooleanBuilder builder, StoredQueryParam param, String slotName, 
            NumberPath<Long> subselectJoinPk) {
        if (param != null) {
            List<Predicate> predicates = getValuePredicate(param, QSlot.slot.value);
            for (Predicate predicate : predicates) {
                builder.and(new JPASubQuery().from(QSlot.slot)
                        .where(QSlot.slot.parent.pk.eq(subselectJoinPk), 
                                QSlot.slot.name.eq(slotName), predicate).exists());
            }
        }
    }

    /*protected void addSlotValueInClassificationMatch(BooleanBuilder builder, StoredQueryParam param, 
            String classificationId, String slotName, NumberPath<Long> subselectJoinPk) {
        if (param != null) {
            List<Predicate> predicates = getValuePredicate(param, QSlot.slot.value);
            for (Predicate predicate : predicates) {
                builder.and(new JPASubQuery().from(QClassification.classification)
                    .innerJoin(QClassification.classification.classificationScheme, QClassificationScheme.classificationScheme)
                    .innerJoin(QClassification.classification.slots, QSlot.slot)
                    .where(QClassification.classification.classifiedObject.pk.eq(subselectJoinPk), 
                            QSlot.slot.name.eq(slotName), predicate).exists());
            }
        }
    }*/

    protected void addExternalIdentifierMatch(BooleanBuilder builder, StoredQueryParam param, String externalIdentifierID, 
            NumberPath<Long> subselectJoinPk) {
        
        /* TODO: DB_RESTRUCT
        if (param != null) {
            List<Predicate> predicates = getValuePredicate(param, QExternalIdentifier.externalIdentifier.value);
            for (Predicate predicate : predicates) {
                builder.and(new JPASubQuery().from(QExternalIdentifier.externalIdentifier)
                        .where(QExternalIdentifier.externalIdentifier.registryObject.pk.eq(subselectJoinPk), 
                                QExternalIdentifier.externalIdentifier.id.eq(externalIdentifierID),
                                predicate).exists());
            }
        } */
    }
    
    protected String[] toCodeValueAndScheme(String codeString) throws XDSException {
        int pos = codeString.indexOf("^^");
        if (pos == -1 || (codeString.length() - pos) < 3 )
            throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, "Invalid code String:"+codeString, null);
        return new String[]{codeString.substring(0, pos), codeString.substring(pos+2)};
    }
    
    protected XDSDocumentEntry getDocumentEntry() throws XDSException {
        StoredQueryParam param;
        XDSDocumentEntry doc;
        if ((param = getQueryParam(XDSConstants.QRY_DOCUMENT_UNIQUE_ID)) != null) {
            if (getQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_UUID) != null) {
                throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                        "Both $XDSDocumentEntryUUID and $XDSDocumentEntryUniqueId are specified!", null);
            }
            doc = getSession().getDocumentEntryByUniqueId(param.getStringValue());
        } else {
            param = getQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_UUID);
            doc = getSession().getDocumentEntryByUUID(param.getStringValue());
        }
        log.info("#### Found Document:"+doc);
        return doc;
    }


    protected List<Identifiable> getObjectsFor(RegistryObject ro) throws XDSException {
        List<Identifiable> objects = new ArrayList<Identifiable>();
        if (ro != null) {
            objects.add(ro);
            JPAQuery query = new JPAQuery(getSession().getEntityManager()).from(QAssociation.association);
            StoredQueryParam paramConfidentialityCode = getQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CONFIDENTIALITY_CODE);
            StoredQueryParam paramFormatCode = getQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_FORMAT_CODE);
            if (paramConfidentialityCode != null || paramFormatCode != null) {
                BooleanBuilder docBuilder = new BooleanBuilder();
                docBuilder.and(QXDSDocumentEntry.xDSDocumentEntry.eq(QAssociation.association.targetObject));
                addXdsCodeMatch(docBuilder, getQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_CONFIDENTIALITY_CODE), 
                        XDSConstants.UUID_XDSDocumentEntry_confidentialityCode, QXDSDocumentEntry.xDSDocumentEntry.xdsCodes);
                addXdsCodeMatch(docBuilder, getQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_FORMAT_CODE), 
                        XDSConstants.UUID_XDSDocumentEntry_formatCode, QXDSDocumentEntry.xDSDocumentEntry.xdsCodes);
        
                BooleanBuilder orBuilder = new BooleanBuilder();
                orBuilder.orNot(QAssociation.association.targetObject.instanceOf(XDSDocumentEntry.class))
                .or(new JPASubQuery().from(QXDSDocumentEntry.xDSDocumentEntry)
                        .where(docBuilder).exists());
                query.where(QAssociation.association.sourceObject.eq(ro), 
                        QAssociation.association.assocType.id.eq(XDSConstants.HAS_MEMBER), orBuilder);
            } else {
                query.where(QAssociation.association.sourceObject.eq(ro), QAssociation.association.assocType.id.eq(XDSConstants.HAS_MEMBER));
            }
            List<Association> associations = query.list(QAssociation.association);
            log.info("#### Found Associations:"+associations);
            objects.addAll(associations);
            RegistryObject obj;
            List<Association[]> childAssocs = new ArrayList<Association[]>();
            for (Association assoc : associations) {
                obj = assoc.getTargetObject();
                if (obj instanceof Association) {
                    childAssocs.add(new Association[]{(Association)obj, assoc});
                } else {
                    objects.add(obj);
                }
            }
            for (Association[] assoc : childAssocs) {
                if (objects.contains(assoc[0].getTargetObject()) && // Add only Associations with source AND target object in result!
                    objects.contains(assoc[0].getSourceObject())) { 
                    objects.add(assoc[0]);
                } else {
                    objects.remove(assoc[1]); //Remove association because target (Association) is not in result
                }
            }
        }
        return objects;
    }


    private List<Predicate> getValuePredicate(StoredQueryParam param, StringPath valuePath) {
        List<Predicate> predicates = new ArrayList<Predicate>();
        List<String> values;
        for (int i = 0, len=param.getNumberOfANDElements() ; i < len ; i++) {
            values = param.isMultiValue() ? param.getMultiValues(i) : Arrays.asList(param.getStringValue());
            BooleanBuilder predicate = new BooleanBuilder();
            List<String> eqValues = new ArrayList<String>();
            for (String v : values) {
                if (v.indexOf('%') != -1 || v.indexOf('_') != -1) {
                    predicate.or(valuePath.like(v));
                } else {
                    eqValues.add(v);
                }
            }
            if (eqValues.size() > 0) {
                if (eqValues.size() > 1) {
                    predicate.or(valuePath.in(eqValues));
                } else {
                    predicate.or(valuePath.eq(eqValues.get(0)));
                }
            }
            predicates.add(predicate);
        }
        return predicates;
    }

}
