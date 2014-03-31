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
import org.dcm4chee.xds2.persistence.QXDSDocumentEntry;
import org.dcm4chee.xds2.persistence.XDSDocumentEntry;
import org.dcm4chee.xds2.registry.ws.XDSPersistenceWrapper;
import org.dcm4chee.xds2.registry.ws.XDSRegistryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.Predicate;

/**
 * Stored Query Implementation for GetDocuments 
 * (urn:uuid:5c4f972b-d56b-40ac-a5fc-c8ca9b40b9d4)
 * 
 * @author franz.willer@gmail.com
 *
 */
public class GetDocumentsQuery extends StoredQuery {

    private static Logger log = LoggerFactory.getLogger(GetDocumentsQuery.class);

    public GetDocumentsQuery(AdhocQueryRequest req, XDSRegistryBean session) throws XDSException {
        super(req, session);
    }
    
    public AdhocQueryResponse query() throws XDSException {
        Predicate predicate = null;
        StoredQueryParam param;
        if ((param = getQueryParam(XDSConstants.QRY_DOCUMENT_UNIQUE_ID)) != null) {
            if (getQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_UUID) != null) {
                throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                        "Both $XDSDocumentEntryEntryUUID and $XDSDocumentEntryUniqueId are specified!", null);
            }
            String[] values = param.getValues();
            predicate = values.length == 1 ? QXDSDocumentEntry.xDSDocumentEntry.uniqueId.eq(values[0]) :
                QXDSDocumentEntry.xDSDocumentEntry.uniqueId.in(values);
        } else if ((param = getQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_UUID)) != null) {
            String[] values = param.getValues();
            predicate = values.length == 1 ? QXDSDocumentEntry.xDSDocumentEntry.id.eq(values[0]) :
                QXDSDocumentEntry.xDSDocumentEntry.id.in(values);
        }
        AdhocQueryResponse rsp = initAdhocQueryResponse();
        JPAQuery query = new JPAQuery(getSession().getEntityManager());
        List<XDSDocumentEntry> docs = query.from(QXDSDocumentEntry.xDSDocumentEntry)
        .where(predicate)
        .list(QXDSDocumentEntry.xDSDocumentEntry);
        log.info("#### Found Documents:"+docs);
        rsp.setRegistryObjectList(new XDSPersistenceWrapper(getSession()).toRegistryObjectListType(docs, isLeafClass()));
        return rsp;
    }

    @Override
    public String[] getRequiredParameterNames() {
        return new String[]{XDSConstants.QRY_DOCUMENT_UNIQUE_ID+"|"+XDSConstants.QRY_DOCUMENT_ENTRY_UUID};
    }
    
}
