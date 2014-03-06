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



import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.AssociationType1;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.InternationalStringType;
import org.dcm4chee.xds2.infoset.rim.ObjectRefType;
import org.dcm4chee.xds2.infoset.rim.RegistryError;
import org.dcm4chee.xds2.infoset.rim.RegistryErrorList;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectType;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.ws.registry.XDSTestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */
public class QueryResultCheck {
    
    private int nrOfDocs = -1;
    private String[] docUUIDs;
    private String[] docNames;
    private int nrOfObjRefs = -1;
    private String[] objRefUUIDs;
    private int nrOfSubmissions = -1;
    private String[] submUUIDs;
    private int nrOfFolders = -1;
    private String[] folderUUIDs;
    private String errorCode;

    private int nrOfAssocs = -1;
    private List<String[]> assocTypeSourceTargets;
    
    private String status = XDSConstants.XDS_B_STATUS_SUCCESS;
    
    private List<ExtrinsicObjectType> docEntries;
    private List<RegistryPackageType> submissions;
    private List<RegistryPackageType> folders;
    private List<AssociationType1> associations;
    private List<ObjectRefType> objRefs;
    
    private final static Logger log = LoggerFactory.getLogger(QueryResultCheck.class);

    public QueryResultCheck setNrOfDocs(int i) {
        nrOfDocs = i;
        if (i == 0) {
            docUUIDs = docNames = null;
        }
        return this;
    }
    
    public QueryResultCheck setDocUUIDs(String... uuids) {
        nrOfDocs = uuids != null ? uuids.length : docNames != null ? docNames.length : -1;
        docUUIDs = uuids;
        return this;
    }
    
    public QueryResultCheck setDocNames(String... names) {
        nrOfDocs = names != null ? names.length : docUUIDs != null ? docUUIDs.length : -1;
        docNames = names;
        return this;
    }
    
    public QueryResultCheck setNrOfObjRefs(int i) {
        nrOfObjRefs = i;
        return this;
    }
    
    public QueryResultCheck setObjRefsUUIDs(String... uuids) {
        nrOfObjRefs = uuids == null ? -1 : uuids.length;
        objRefUUIDs = uuids;
        return this;
    }
    
    public QueryResultCheck setNrOfSubmissions(int i) {
        nrOfSubmissions = i;
        return this;
    }
    
    public QueryResultCheck setSubmUUIDs(String... uuids) {
        submUUIDs = uuids;
        nrOfSubmissions = uuids == null ? -1 : uuids.length;
        return this;
    }
    
    public QueryResultCheck setNrOfFolders(int i) {
        nrOfFolders = i;
        return this;
    }
    
    public QueryResultCheck setFolderUUIDs(String... uuids) {
        folderUUIDs = uuids;
        nrOfFolders = uuids == null ? -1 : uuids.length;
        return this;
    }
    
    public QueryResultCheck setNrOfAssocs(int i) {
        nrOfAssocs = i;
        return this;
    }
    
    public QueryResultCheck addAssoc(String type, String source, String target) {
        if (assocTypeSourceTargets == null)
            assocTypeSourceTargets = new ArrayList<String[]>();
        assocTypeSourceTargets.add(new String[]{type, source, target});
        nrOfAssocs = assocTypeSourceTargets.size();
        return this;
    }
    
    public QueryResultCheck setStatus(String status) {
        this.status = status;
        return this;
    }

    public QueryResultCheck setErrorCode(String errorCode) {
        this.errorCode = errorCode;
        status = XDSConstants.XDS_B_STATUS_FAILURE;
        return this;
    }
    
    public void checkResponse(AdhocQueryResponse rsp) {
        assertNotNull("StoredQuery failed! Response is null!", rsp);
        checkResponseStatus(rsp);
        if (nrOfDocs != -1) checkDocuments(rsp);
        if (nrOfSubmissions != -1) checkSubmissions(rsp);
        if (nrOfFolders != -1) checkFolders(rsp);
        if (nrOfAssocs != -1) checkAssociations(rsp);
        if (nrOfObjRefs != -1) checkObjectRefs(rsp);
        docEntries = null;
        submissions = null;
        folders = null;
        associations = null;
        objRefs = null;
    }

    private void checkResponseStatus(AdhocQueryResponse rsp) {
        assertEquals("Response status:", status, rsp.getStatus());
        if (errorCode != null) {
            RegistryErrorList errorList = rsp.getRegistryErrorList();
            assertNotNull("RegistryErrorList:", errorList);
            List<RegistryError> errors = errorList.getRegistryError();
            assertEquals("Number Of RegistryErrors:", 1, errors.size());
            assertEquals("ErrorCode:", errorCode, errors.get(0).getErrorCode());
        }
    }
    
    private void checkDocuments(AdhocQueryResponse rsp) {
        prepareObjectsFromResponse(rsp);
        if (nrOfDocs != docEntries.size())
            log.info("#####################Found docs:"+dumpIDs(docEntries));
        assertEquals("Number of DocumentEntries:", nrOfDocs, docEntries.size());
        if (docUUIDs != null) {
            for (String uuid : docUUIDs) {
                if (!containsUUID(uuid, docEntries)) {
                    fail("Missing XDSDocumentEntry uuid:"+uuid+ " found: "+dumpIDs(docEntries));
                }
            }
        }
        if (docNames != null) {
            InternationalStringType ist;
            name: for (String name : docNames) {
                for (RegistryObjectType obj : docEntries) {
                    if ((ist = obj.getName()) == null)
                        continue;
                    if (ist.getLocalizedString().isEmpty())
                        continue;
                    if (name.equals(ist.getLocalizedString().get(0).getValue()))
                        continue name;
                }
                fail("Missing XDSDocumentEntry with name:"+name);
            }
        }
    }

    private void checkSubmissions(AdhocQueryResponse rsp) {
        prepareObjectsFromResponse(rsp);
        assertEquals("Number of SubmissionSets:", nrOfSubmissions, submissions.size());
        if (submUUIDs != null) {
            for (String uuid : submUUIDs) {
                if (!containsUUID(uuid, submissions)) {
                    fail("Missing XDSSubmissionSet uuid:"+uuid+ " found: "+dumpIDs(submissions));
                }
            }
        }
    }
    
    private void checkFolders(AdhocQueryResponse rsp) {
        prepareObjectsFromResponse(rsp);
        assertEquals("Number of Folders:", nrOfFolders, folders.size());
        if (folderUUIDs != null) {
            for (String uuid : folderUUIDs) {
                if (!containsUUID(uuid, folders)) {
                    fail("Missing XDSFolder uuid:"+uuid+ " found: "+dumpIDs(folders));
                }
            }
        }
    }
    
    private void checkObjectRefs(AdhocQueryResponse rsp) {
        prepareObjectsFromResponse(rsp);
        assertEquals("Number of ObjectRefs:", nrOfObjRefs, objRefs.size());
        if (objRefUUIDs != null) {
            for (String uuid : objRefUUIDs) {
                if (!containsUUID(uuid, objRefs)) {
                    fail("Missing ObjectRef uuid:"+uuid+ " found: "+dumpIDs(objRefs));
                }
            }
        }
    }
    
    private void checkAssociations(AdhocQueryResponse rsp) {
        prepareObjectsFromResponse(rsp);
        assertEquals("Number of Associations:", nrOfAssocs, associations.size());
        if (assocTypeSourceTargets != null) {
            type: for (String[] typeSourceTarget : assocTypeSourceTargets) {
                for (AssociationType1 assoc : associations) {
                    if (typeSourceTarget[0].equals(assoc.getAssociationType()) &&
                        typeSourceTarget[1].equals(assoc.getSourceObject()) &&
                        typeSourceTarget[2].equals(assoc.getTargetObject())) {
                        continue type;
                    }
                }
                fail("Missing Association type:"+typeSourceTarget[0]+
                        " source:"+typeSourceTarget[1]+" target:"+typeSourceTarget[2]);
            }
        }
    }
    
    private boolean containsUUID(String uuid, List<? extends IdentifiableType> objects) {
        for (IdentifiableType obj : objects) {
            if (obj.getId().equals(uuid))
                return true;
        }
        return false;
    }

    private void prepareObjectsFromResponse(AdhocQueryResponse rsp) {
        if (docEntries != null) 
            return;
        docEntries = new ArrayList<ExtrinsicObjectType>();
        submissions = new ArrayList<RegistryPackageType>();
        folders = new ArrayList<RegistryPackageType>();
        associations = new ArrayList<AssociationType1>();
        objRefs = new ArrayList<ObjectRefType>();

        List<JAXBElement<? extends IdentifiableType>> objects = rsp.getRegistryObjectList().getIdentifiable();
        IdentifiableType obj;
        for (JAXBElement<? extends IdentifiableType> jaxb : objects) {
            obj = jaxb.getValue();
            if (obj instanceof ExtrinsicObjectType) {
                if (((ExtrinsicObjectType) obj).getObjectType().equals(XDSConstants.UUID_XDSDocumentEntry)) {
                    docEntries.add((ExtrinsicObjectType) obj);
                } else {
                    log.warn("################Found ExtrinsicObject which is not an XDSDocumentEntry! id:"+obj.getId());
                }
            } else if (obj instanceof RegistryPackageType) {
                RegistryPackageType rp = (RegistryPackageType) obj;
                if (XDSTestUtil.getExternalIdentifierValue(rp.getExternalIdentifier(), 
                        XDSConstants.UUID_XDSSubmissionSet_uniqueId) != null) {
                    submissions.add(rp);
                } else if (XDSTestUtil.getExternalIdentifierValue(rp.getExternalIdentifier(), 
                        XDSConstants.UUID_XDSFolder_uniqueId) != null) {
                    folders.add(rp);
                } else {
                    log.warn("Found RegistryPackage which is neither a XDSSubmissionSet nor a XDSFolder! id:"+obj.getId());
                }
            } else if (obj instanceof AssociationType1) {
                associations.add((AssociationType1) obj);
            } else if (obj instanceof ObjectRefType) {
                objRefs.add((ObjectRefType) obj);
            }
        }
    }
    
    private String dumpIDs(List<? extends IdentifiableType> objs) {
        StringBuilder sb = new StringBuilder();
        for (IdentifiableType obj : objs) {
            sb.append(obj.getId()).append(',');
        }
        return sb.toString();
    }
}
