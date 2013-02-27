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

package org.dcm4chee.xds2.ws.repository;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.management.ObjectName;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.ws.BindingType;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.soap.Addressing;
import javax.xml.ws.soap.MTOM;
import javax.xml.ws.soap.SOAPBinding;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.XDSUtil;
import org.dcm4chee.xds2.common.audit.AuditRequestInfo;
import org.dcm4chee.xds2.common.audit.XDSAudit;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.conf.XdsDevice;
import org.dcm4chee.xds2.conf.XdsRepository;
import org.dcm4chee.xds2.infoset.ihe.ProvideAndRegisterDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.ProvideAndRegisterDocumentSetRequestType.Document;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType.DocumentResponse;
import org.dcm4chee.xds2.infoset.rim.ExternalIdentifierType;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.ObjectFactory;
import org.dcm4chee.xds2.infoset.rim.RegistryError;
import org.dcm4chee.xds2.infoset.rim.RegistryErrorList;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.infoset.util.DocumentRegistryPortTypeFactory;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryPortType;
import org.dcm4chee.xds2.infoset.ws.repository.DocumentRepositoryPortType;
import org.dcm4chee.xds2.storage.XDSDocument;
import org.dcm4chee.xds2.ws.handler.LogHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MTOM
@BindingType(value = SOAPBinding.SOAP12HTTP_MTOM_BINDING)
@Stateless
@WebService(endpointInterface="org.dcm4chee.xds2.infoset.ws.repository.DocumentRepositoryPortType", 
        name="b",
        serviceName="XDSbRepository",
        portName="DocumentRepository_Port_Soap12",
        targetNamespace="urn:ihe:iti:xds-b:2007",
        wsdlLocation = "/META-INF/wsdl/XDS.b_DocumentRepository.wsdl"
)

@Addressing(enabled=true, required=true)
@HandlerChain(file="handlers.xml")
public class XDSRepositoryBean implements DocumentRepositoryPortType {
    
    private XdsRepository cfg;
    private ObjectFactory factory = new ObjectFactory();
    private org.dcm4chee.xds2.infoset.ihe.ObjectFactory iheFactory = new org.dcm4chee.xds2.infoset.ihe.ObjectFactory();

    private static Logger log = LoggerFactory.getLogger(XDSRepositoryBean.class);

    @Resource
    WebServiceContext wsContext;

    @Override
    public RegistryResponseType documentRepositoryProvideAndRegisterDocumentSetB(
            ProvideAndRegisterDocumentSetRequestType req) {
        RegistryResponseType rsp;
        String[] storedUIDs = null;
        URL registryURL = null;
        try {
            registryURL = getRegistryWsdlUrl();
            logRequest(req);
            List<ExtrinsicObjectType> extrObjs = checkRequest(req);
            storedUIDs = storeDocuments(req, extrObjs);
            SubmitObjectsRequest submitRequest = req.getSubmitObjectsRequest();
            rsp = dispatchSubmitObjectsRequest(submitRequest, registryURL);
        } catch (Exception x) {
            rsp = factory.createRegistryResponseType();
            if (x instanceof XDSException) {
                XDSUtil.addError(rsp, (XDSException) x);
            } else {
                XDSUtil.addError(rsp, new XDSException(XDSException.XDS_ERR_REPOSITORY_ERROR, 
                        "Unexpected error in XDS service !: "+x.getMessage(),x));
            }
        }
        boolean success = XDSConstants.XDS_B_STATUS_SUCCESS.equals(rsp.getStatus());
        AuditRequestInfo info = new AuditRequestInfo(LogHandler.getInboundSOAPHeader(), wsContext);
        String[] submUIDAndpatid = getSubmissionUIDandPatID(req.getSubmitObjectsRequest());
        if (storedUIDs != null) {
            XDSAudit.logRepositoryPnRExport(submUIDAndpatid[0], submUIDAndpatid[1], info, registryURL, success);
        }
        commit(storedUIDs, success);
        XDSAudit.logRepositoryImport(submUIDAndpatid[0], submUIDAndpatid[1], info, 
                XDSConstants.XDS_B_STATUS_SUCCESS.equals(rsp.getStatus()));
        return rsp;
    }

    private void commit(String[] storedUIDs, boolean success) {
        if (storedUIDs != null) {
            try {
                ManagementFactory.getPlatformMBeanServer().invoke(new ObjectName("dcm4chee.xds2:service=Store"),
                        "commit", 
                        new Object[]{storedUIDs, success}, 
                        new String[]{String[].class.getName(), boolean.class.getName()});
            } catch (Exception e) {
                log.warn("Failed to commit stored documents!");
            }
        }
    }

    @Override
    public RetrieveDocumentSetResponseType documentRepositoryRetrieveDocumentSet(RetrieveDocumentSetRequestType req) {
        RetrieveDocumentSetResponseType rsp = iheFactory.createRetrieveDocumentSetResponseType();
        try {
            String repositoryUID = getRepositoryUniqueId();
            String docUid, reqRepoUid;
            XDSDocument doc;
            RetrieveDocumentSetResponseType.DocumentResponse docRsp;
            List<String> retrievedUIDs = new ArrayList<String>();
            int requestCount = req.getDocumentRequest().size();
            RegistryErrorList regErrors = factory.createRegistryErrorList();
            List<RegistryError> mainErrors = regErrors.getRegistryError();
            for ( RetrieveDocumentSetRequestType.DocumentRequest docReq : req.getDocumentRequest() ) {
                reqRepoUid = docReq.getRepositoryUniqueId();
                docUid = docReq.getDocumentUniqueId();
                if ( reqRepoUid.equals(repositoryUID)) {
                    doc = retrieveDocument(docUid);
                    if ( doc != null ) {
                        try {
                            docRsp = getDocumentResponse(doc, getRepositoryUniqueId());
                            rsp.getDocumentResponse().add(docRsp);
                            retrievedUIDs.add(docUid);
                        } catch (IOException e) {
                            String msg = "Error in building DocumentResponse for document:"+doc;
                            log.error(msg);
                            mainErrors.add(XDSUtil.getRegistryError(XDSException.XDS_ERR_SEVERITY_ERROR, 
                                    XDSException.XDS_ERR_REPOSITORY_ERROR, msg, docUid));
                        }
                    } else {
                        String msg = "Document not found! document UID:"+docUid;
                        log.warn(msg);
                        mainErrors.add(XDSUtil.getRegistryError(XDSException.XDS_ERR_SEVERITY_ERROR, 
                                XDSException.XDS_ERR_REPOSITORY_ERROR, msg, docUid));
                    }
                } else {
                    String msg = "DocumentRepositoryUID="+reqRepoUid+" is unknown! This repository unique ID:"+repositoryUID;
                    log.warn(msg);
                    mainErrors.add(XDSUtil.getRegistryError(XDSException.XDS_ERR_SEVERITY_ERROR, 
                            XDSException.XDS_ERR_UNKNOWN_REPOSITORY_ID, msg, docUid));
                }
            }
            AuditRequestInfo info = new AuditRequestInfo(LogHandler.getInboundSOAPHeader(), wsContext);
            if (retrievedUIDs.size() > 0) {
                XDSAudit.logRepositoryRetrieveExport(info, req, retrievedUIDs, true);
            }
            if (retrievedUIDs.size() < requestCount) {
                XDSAudit.logRepositoryRetrieveExport(info, req, retrievedUIDs, false);
            }
            RegistryResponseType regRsp = factory.createRegistryResponseType();
            
            int nrOfDocs = rsp.getDocumentResponse().size();
            if (nrOfDocs == 0) {
                throw new XDSException(XDSException.XDS_ERR_MISSING_DOCUMENT, 
                        "None of the requested documents were found. This repository unique ID " + repositoryUID, null);
            } else if (nrOfDocs < requestCount) {
                regRsp.setStatus(XDSConstants.XDS_B_STATUS_PARTIAL_SUCCESS);
            } else {
                regRsp.setStatus(XDSConstants.XDS_B_STATUS_SUCCESS);
            }
    
            if (regErrors.getRegistryError().size() > 0) {
                regRsp.setRegistryErrorList(regErrors);
            }
            rsp.setRegistryResponse(regRsp);
            return rsp;
        } catch (Exception x) {
            if (x instanceof XDSException) {
                XDSUtil.addError(rsp, (XDSException) x);
            } else {
                XDSUtil.addError(rsp, new XDSException(XDSException.XDS_ERR_REPOSITORY_ERROR, 
                        "Unexpected error in XDS service !: "+x.getMessage(),x));
            }
        }
        return rsp;
    }
    
    private String[] storeDocuments(ProvideAndRegisterDocumentSetRequestType req, List<ExtrinsicObjectType> extrObjs) throws XDSException {
        Map<String, Document> docs = InfosetUtil.getDocuments(req);
        Document doc;
        ExtrinsicObjectType eo;
        String docUID;
        XDSDocument xdsDoc;
        String[] storedUIDs = new String[extrObjs.size()];
        String[] mimetypes = getConfig().isCheckMimetype() ? cfg.getAcceptedMimeTypes() : null;
        for (int i = 0, len = extrObjs.size() ; i < len ; i++ ) {
            eo = extrObjs.get(i);
            doc = docs.get(eo.getId());
            docUID = InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSDocumentEntry_uniqueId, eo);
            log.info("####### Document id:"+doc.getId());
            log.info("####### Document uuid:"+docUID);
            log.info("####### Document mime:"+eo.getMimeType());
            if (mimetypes != null) {
                boolean unsupportedMimetype = true;
                for (int j = 0 ; j < mimetypes.length ; j++) {
                    if (mimetypes[j].equals(eo.getMimeType())) {
                        unsupportedMimetype = false;
                        break;
                    }
                    if (unsupportedMimetype)
                        throw new XDSException(XDSException.XDS_ERR_REPOSITORY_METADATA_ERROR, "Mimetype not supported:"+eo.getMimeType(), null);
                }
            }
            storedUIDs[i] = docUID;
            try {
                xdsDoc = (XDSDocument) ManagementFactory.getPlatformMBeanServer().invoke(new ObjectName("dcm4chee.xds2:service=Store"),
                        "storeDocument", 
                        new Object[]{docUID, doc.getValue(), eo.getMimeType()}, 
                        new String[]{String.class.getName(), byte[].class.getName(), String.class.getName()});
                if ( xdsDoc != null ) {
                    Map<String, SlotType1> slots = InfosetUtil.addOrOverwriteSlot(eo, XDSConstants.SLOT_NAME_REPOSITORY_UNIQUE_ID, getRepositoryUniqueId());
                    String oldValue = InfosetUtil.addOrCheckedOverwriteSlot(eo, slots, XDSConstants.SLOT_NAME_SIZE, String.valueOf(xdsDoc.getSize()));
                    if (oldValue != null) {
                        throw new XDSException(XDSException.XDS_ERR_REPOSITORY_METADATA_ERROR, "Slot 'size' already exists but has different value! old:"+oldValue+" new:"+xdsDoc.getSize(), null);
                    }
                    oldValue = InfosetUtil.addOrCheckedOverwriteSlot(eo, slots, XDSConstants.SLOT_NAME_HASH, xdsDoc.getHashString());
                    if (oldValue != null) {
                        throw new XDSException(XDSException.XDS_ERR_REPOSITORY_METADATA_ERROR, "Slot 'hash' already exists but has different value! old:"+oldValue+" new:"+xdsDoc.getHashString(), null);
                    }
                } else {
                    log.warn("Document already exists! docUid:"+docUID);
                }
            } catch (XDSException x) {
                throw x;
            } catch (Exception x) {
                throw new XDSException(XDSException.XDS_ERR_REPOSITORY_ERROR, 
                        "Storage of document "+docUID+" failed! : "+x.getMessage(),x);
            }
        }
        return storedUIDs;
    }
    
    private XDSDocument retrieveDocument(String docUID) throws XDSException {
        try {
            XDSDocument xdsDoc = (XDSDocument) ManagementFactory.getPlatformMBeanServer().invoke(new ObjectName("dcm4chee.xds2:service=Store"),
                "retrieveDocument", 
                new Object[]{docUID}, 
                new String[]{String.class.getName()});
            return xdsDoc;
        } catch (Exception x) {
            throw new XDSException(XDSException.XDS_ERR_REPOSITORY_ERROR, 
                    "Retrieve of document "+docUID+" failed! : "+x.getMessage(),x);
        }
    }
    
    private String getRepositoryUniqueId() {
        return getConfig().getRepositoryUID();
    }

    private URL getRegistryWsdlUrl() throws MalformedURLException {
        String url = getConfig().getRegistryURL("*");
        return new URL(url);
        //return new URL("http://localhost:8080/dcm4chee-xds/XDSbRegistry/b?wsdl");
        //return new URL("http://ihexds.nist.gov:12080/tf6/services/xdsregistryb?wsdl");
    }

    private RegistryResponseType dispatchSubmitObjectsRequest(SubmitObjectsRequest submitRequest, URL xdsRegistryURI) throws MalformedURLException,
            JAXBException, XDSException {

        DocumentRegistryPortType port = DocumentRegistryPortTypeFactory.getDocumentRegistryPortSoap12(xdsRegistryURI.toString());
        log.info("####################################################");
        log.info("####################################################");
        log.info("XDS.b: Send register document-b request to registry:"+xdsRegistryURI);
        log.info("####################################################");
        log.info("####################################################");
        RegistryResponseType rsp = null;
        try {
            rsp = port.documentRegistryRegisterDocumentSetB(submitRequest);
        } catch ( Exception x) {
            throw new XDSException( XDSException.XDS_ERR_REG_NOT_AVAIL, "Document Registry not available: "+xdsRegistryURI, x);
        }
        return rsp;
    }

    private List<ExtrinsicObjectType> checkRequest(ProvideAndRegisterDocumentSetRequestType req) throws XDSException {
        SubmitObjectsRequest sor = req.getSubmitObjectsRequest();
        RegistryPackageType submissionSet = InfosetUtil.getRegistryPackage(sor, XDSConstants.UUID_XDSSubmissionSet);
        if ( submissionSet == null ) {
            log.error("No RegistryPackage id=SubmissionSet found!");
            throw new XDSException( XDSException.XDS_ERR_REPOSITORY_ERROR, 
                    XDSException.XDS_ERR_MISSING_REGISTRY_PACKAGE, null);
        }
        List<ExtrinsicObjectType> extrObjs = InfosetUtil.getExtrinsicObjects(req.getSubmitObjectsRequest());
        checkPatientIDs(req, submissionSet, extrObjs);
        Map<String, Document> docs = InfosetUtil.getDocuments(req);
        if ( extrObjs.size() > docs.size() ) {
            log.warn("Missing Documents! Found more ExtrinsicObjects("+extrObjs.size()+") than Documents("+docs.size()+")!");
            throw new XDSException(XDSException.XDS_ERR_MISSING_DOCUMENT,
                    "", null);
        } else if ( extrObjs.size() < docs.size() ) {
            log.warn("Missing Document Metadata! Found less ExtrinsicObjects("+extrObjs.size()+") than Documents("+docs.size()+")!");
            throw new XDSException(XDSException.XDS_ERR_MISSING_DOCUMENT_METADATA,
                    "", null);

        }
        return extrObjs;
    }

    private String checkPatientIDs(ProvideAndRegisterDocumentSetRequestType req, RegistryPackageType submissionSet, List<ExtrinsicObjectType> extrObjs) throws XDSException {
        String submissionPatId = InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSSubmissionSet_patientId, submissionSet);
        String docPatId;
        ExtrinsicObjectType eo;
        for ( int i = 0, len = extrObjs.size() ; i < len ; i++ ) {
            eo = extrObjs.get(i);
            docPatId = InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSDocumentEntry_patientId, eo);
            if ( docPatId != null && !docPatId.equals(submissionPatId)) {
                String msg = "XDSDocumentEntry.patientId ("+docPatId+")and XDSSubmissionSet.patientId ("+submissionPatId+") doesn't match! ExtrinsicObject.Id:"+eo.getId();
                log.warn(msg);
                throw new XDSException(XDSException.XDS_ERR_PATID_DOESNOT_MATCH,
                        msg, null);
            }
        }
        RegistryPackageType folder = InfosetUtil.getRegistryPackage(req.getSubmitObjectsRequest(), XDSConstants.UUID_XDSFolder);
        String folderPatId = InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSFolder_patientId, folder);
        if ( folderPatId != null && !folderPatId.equals(submissionPatId)) {
            String msg = "XDSFolder.patientId ("+folderPatId+")and XDSSubmissionSet.patientId ("+submissionPatId+") doesn't match!";
            log.warn(msg);
            throw new XDSException(XDSException.XDS_ERR_PATID_DOESNOT_MATCH,
                    msg, null);
        }
        return submissionPatId;
    }

    private DocumentResponse getDocumentResponse(XDSDocument doc, String repositoryUniqueId) throws IOException {
        RetrieveDocumentSetResponseType.DocumentResponse docRsp;
        docRsp = iheFactory.createRetrieveDocumentSetResponseTypeDocumentResponse();
        docRsp.setDocumentUniqueId(doc.getUID());
        docRsp.setMimeType(doc.getMimeType());
        docRsp.setRepositoryUniqueId(repositoryUniqueId);
        docRsp.setDocument(doc.getContent());
        return docRsp;
    }

    private void logRequest(ProvideAndRegisterDocumentSetRequestType req) {
        log.info("###### SubmitObjectRequest:"+req.getSubmitObjectsRequest());
        List<Document> docs = req.getDocument();
        log.info("###### Documents:"+docs);
        if (docs != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("######Number of Documents:").append(docs.size());
            int dumpValLen;
            for (Document d : docs) {
                sb.append("\nDocument ID:"+d.getId())
                .append("       size:").append(d.getValue().length)
                .append("       value:");
                try {
                    dumpValLen = Math.min(d.getValue().length, 40);
                    sb.append(new String(d.getValue(), 0, dumpValLen));
                    if (dumpValLen == 40)
                        sb.append("...");
                } catch (Exception x) {
                    log.warn("Failed to convert value in String!", x);
                }
            }
            log.info(sb.toString());
        }
    }
    private XdsRepository getConfig() {
        if (cfg == null)
            cfg = XdsDevice.getXdsRepository();
        return cfg;
    }
    private String[] getSubmissionUIDandPatID(SubmitObjectsRequest req) {
        String[] result = new String[2];
        List<JAXBElement<? extends IdentifiableType>> objs = req.getRegistryObjectList().getIdentifiable();
        IdentifiableType obj;
        whole: for (int i=0,len=objs.size() ; i < len ; i++) {
            obj = objs.get(i).getValue();
            if (obj instanceof RegistryPackageType) {
                List<ExternalIdentifierType> list = ((RegistryPackageType)obj).getExternalIdentifier();
                if (list != null) {
                    for (ExternalIdentifierType eiType : list) {
                        if (XDSConstants.UUID_XDSSubmissionSet_patientId.equals(eiType.getIdentificationScheme())) {
                            result[1] = eiType.getValue();
                        } else if (XDSConstants.UUID_XDSSubmissionSet_uniqueId.equals(eiType.getIdentificationScheme())) {
                            result[0] = eiType.getValue();
                        } else {
                            continue;
                        }
                        if (result[0] != null && result[1] != null)
                            break whole;
                    }
                }
            
            }
        }
        return result;
    }

}
