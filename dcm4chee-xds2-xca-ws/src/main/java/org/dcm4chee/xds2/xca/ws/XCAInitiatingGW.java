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

package org.dcm4chee.xds2.xca.ws;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.xml.bind.JAXBElement;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.BindingType;
import javax.xml.ws.Response;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.soap.Addressing;
import javax.xml.ws.soap.MTOM;
import javax.xml.ws.soap.SOAPBinding;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.hl7.IHL7ApplicationCache;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.hl7.HL7Application;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.XDSUtil;
import org.dcm4chee.xds2.common.audit.AuditRequestInfo;
import org.dcm4chee.xds2.common.audit.XDSAudit;
import org.dcm4chee.xds2.common.deactivatable.DeactivateableByConfiguration;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.conf.XCAInitiatingGWCfg;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType.DocumentRequest;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType.DocumentResponse;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryType;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.ObjectFactory;
import org.dcm4chee.xds2.infoset.rim.ObjectRefType;
import org.dcm4chee.xds2.infoset.rim.RegistryError;
import org.dcm4chee.xds2.infoset.rim.RegistryErrorList;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.infoset.util.DocumentRegistryPortTypeFactory;
import org.dcm4chee.xds2.infoset.util.DocumentRepositoryPortTypeFactory;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.dcm4chee.xds2.infoset.util.RespondingGatewayPortTypeFactory;
import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryPortType;
import org.dcm4chee.xds2.infoset.ws.repository.DocumentRepositoryPortType;
import org.dcm4chee.xds2.infoset.ws.xca.InitiatingGatewayPortType;
import org.dcm4chee.xds2.infoset.ws.xca.RespondingGatewayPortType;
import org.dcm4chee.xds2.pix.PixQueryClient;
import org.dcm4chee.xds2.ws.handler.AsyncResponseHandler;
import org.dcm4chee.xds2.ws.handler.LogHandler;
import org.dcm4chee.xds2.ws.util.CxfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MTOM
@BindingType(value = SOAPBinding.SOAP12HTTP_MTOM_BINDING)
@Stateless
@WebService(endpointInterface="org.dcm4chee.xds2.infoset.ws.xca.InitiatingGatewayPortType", 
        name="xca",
        serviceName="InitiatingGateway",
        portName="InitiatingGateway_Port_Soap12",
        targetNamespace="urn:ihe:iti:xds-b:2007",
        wsdlLocation = "/META-INF/wsdl/XCAInitiatingGateway.wsdl"
)

@Addressing(enabled=true, required=true)
@HandlerChain(file="handlers.xml")
@DeactivateableByConfiguration(extension = XCAInitiatingGWCfg.class)
public class XCAInitiatingGW implements InitiatingGatewayPortType {
    
    private ObjectFactory factory = new ObjectFactory();
    private org.dcm4chee.xds2.infoset.ihe.ObjectFactory iheFactory = new org.dcm4chee.xds2.infoset.ihe.ObjectFactory();

    private PixQueryClient pixClient;
    
    private static Logger log = LoggerFactory.getLogger(XCAInitiatingGW.class);

    @Resource
    private WebServiceContext wsContext;
    
    @Inject
    private Device device;
    private XCAInitiatingGWCfg cfg;
    
    @Inject
    private IHL7ApplicationCache hl7AppCache;
    
    @PostConstruct
    public void init() {
    	cfg = device.getDeviceExtension(XCAInitiatingGWCfg.class);
    }

    @Override
    public Response<AdhocQueryResponse> documentRegistryRegistryStoredQueryAsync(AdhocQueryRequest req) {
        log.info("### documentRegistryRegistryStoredQueryAsync (callback) called");
        return null;
    }

    @Override
    public Future<?> documentRegistryRegistryStoredQueryAsync(AdhocQueryRequest req,
            AsyncHandler<AdhocQueryResponse> asyncHandler) {
        log.info("### documentRegistryRegistryStoredQueryAsync (handler) called");
        return null;
    }

    @Override
    public AdhocQueryResponse documentRegistryRegistryStoredQuery(AdhocQueryRequest req) {
        log.info("### documentRegistryRegistryStoredQuery called");
        AdhocQueryResponse rsp = doStoredQuery(req);
        return rsp;
    }

    @Override
    public Response<RetrieveDocumentSetResponseType> documentRepositoryRetrieveDocumentSetAsync(RetrieveDocumentSetRequestType req) {
        log.info("### documentRepositoryRetrieveDocumentSetAsync (callback) called");
        return null;
    }

    @Override
    public Future<?> documentRepositoryRetrieveDocumentSetAsync(RetrieveDocumentSetRequestType req,
            AsyncHandler<RetrieveDocumentSetResponseType> asyncHandler) {
        log.info("### documentRepositoryRetrieveDocumentSetAsync (handler) called");
        return null;
    }

    @Override
    public RetrieveDocumentSetResponseType documentRepositoryRetrieveDocumentSet(RetrieveDocumentSetRequestType req) {
        log.info("### documentRepositoryRetrieveDocumentSet called");
        RetrieveDocumentSetResponseType rsp = this.doRetrieve(req);
        return rsp;
    }

    private AdhocQueryResponse doStoredQuery(AdhocQueryRequest req) {
        AdhocQueryResponse rsp = null;
        String home = req.getAdhocQuery().getHome();
        try {
            boolean isHome = cfg.getHomeCommunityID().equals(home);
            CxfUtil.disableMTOMResponse(wsContext);
            // if request is for our home community, or homeCommunityid is not specified
            if (home == null || isHome) {
                String url = cfg.getRegistryURL();
                if (url != null) {
                    rsp = sendStoredQuery(url, req);
                }
            }
            
            // if request is for another community or homeCommunityid is not specified
            if (!isHome) {

            	// if homeCommunityId is specified but not found among configured responding gateways, throw exception 
                if (home != null && !cfg.getHomeCommunityIDs().contains(home))
                    throw new XDSException(XDSException.XDS_ERR_UNKNOWN_COMMUNITY, "Unknown communityID "+home, null);
                SlotType1 patSlotType = null;
                for (SlotType1 slot : req.getAdhocQuery().getSlot()) {
                    if (slot.getName().endsWith("atientId")) {
                        patSlotType = slot;
                    }
                    break;
                }
                
                PatSlot patSlot = patSlotType == null ? null : pixQuery(req, patSlotType, cfg.getAssigningAuthorities());
                for (String communityID : home == null ? cfg.getHomeCommunityIDs() : Arrays.asList(home)) {
                    AdhocQueryResponse xcaRsp = sendXCAQuery(communityID, req, patSlot, cfg);
                    if (rsp == null) {
                        rsp = xcaRsp;
                    } else {
                        if (xcaRsp.getRegistryObjectList() != null) {
                            if (rsp.getRegistryObjectList() == null) {
                                rsp.setRegistryObjectList(xcaRsp.getRegistryObjectList());
                            } else {
                                rsp.getRegistryObjectList().getIdentifiable().addAll(xcaRsp.getRegistryObjectList().getIdentifiable());
                            }
                        }
                        if (xcaRsp.getRegistryErrorList() != null) {
                        	RegistryErrorList errList = rsp.getRegistryErrorList() == null ? 
                        			factory.createRegistryErrorList() : rsp.getRegistryErrorList();
                            List<RegistryError> errors = errList.getRegistryError();
                            for (RegistryError err : xcaRsp.getRegistryErrorList().getRegistryError()) {
                                if (!XDSException.XDS_ERR_UNKNOWN_PATID.equals(err.getErrorCode()))
                                    errors.add(err);
                            }
                            if (!errors.isEmpty())
                            	rsp.setRegistryErrorList(errList);
                        }
                    }
                }
            }
        } catch (Exception x) {
            rsp = InfosetUtil.emptyAdhocQueryResponse();
            if (x instanceof XDSException) {
                XDSUtil.addError(rsp, (XDSException)x);
            } else {
                XDSUtil.addError(rsp, new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                        "Unexpected error in XCA Initiating Gateway service!: "+x.getMessage(),x));
            }
        }
        if (rsp == null) {
            rsp = InfosetUtil.emptyAdhocQueryResponse();
        }
        if (rsp.getRegistryErrorList() == null || rsp.getRegistryErrorList().getRegistryError().isEmpty()) {
            rsp.setStatus(XDSConstants.XDS_B_STATUS_SUCCESS);
        } else if (rsp.getRegistryObjectList() == null || rsp.getRegistryObjectList().getIdentifiable().isEmpty()) {
            rsp.setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        } else {
            rsp.setStatus(XDSConstants.XDS_B_STATUS_PARTIAL_SUCCESS);
        }
        XDSAudit.logRegistryQuery(req, new AuditRequestInfo(LogHandler.getInboundSOAPHeader(), wsContext), 
                XDSConstants.XDS_B_STATUS_SUCCESS.equals(rsp.getStatus()));
        return rsp;
    }

    private AdhocQueryResponse sendStoredQuery(String url, AdhocQueryRequest req) {
        AdhocQueryResponse rsp;
        URL registryURL = null;
        try {
            DocumentRegistryPortType port = DocumentRegistryPortTypeFactory.getDocumentRegistryPortSoap12(url);
            log.info("####################################################");
            log.info("####################################################");
            log.info("XCA Initiating Gateway: Send Stored Query Request to registry:"+url);
            log.info("####################################################");
            log.info("####################################################");
            log.info("org.jboss.security.ignoreHttpsHost:"+System.getProperty("org.jboss.security.ignoreHttpsHost"));
            registryURL = new URL(url);
            try {
                rsp = port.documentRegistryRegistryStoredQuery(req);
            } catch ( Exception x) {
                throw new XDSException( XDSException.XDS_ERR_REG_NOT_AVAIL, "Document Registry not available: "+url, x);
            }
        } catch (Exception x) {
            rsp = InfosetUtil.emptyAdhocQueryResponse();
            if (x instanceof XDSException) {
                XDSUtil.addError(rsp, (XDSException) x);
            } else {
                XDSUtil.addError(rsp, new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                        "Unexpected error in XDS service !: "+x.getMessage(),x));
            }
        }
        XDSAudit.logClientQuery(req, XDSConstants.WS_ADDRESSING_ANONYMOUS, null, registryURL, !XDSConstants.XDS_B_STATUS_FAILURE.equals(rsp.getStatus()));
        return addHomeCommunityId(rsp);    
    }

    private AdhocQueryResponse sendXCAQuery(String communityID, AdhocQueryRequest req, PatSlot patSlot, XCAInitiatingGWCfg cfg) {
        String url = cfg.getRespondingGWQueryURL(communityID);
        AdhocQueryResponse rsp;
        URL registryURL = null;
        try {
            registryURL = new URL(url);
            if (patSlot != null) {
                String domain = cfg.getAssigningAuthority(communityID);
                if (!patSlot.updateSlotValuesForDomain(domain)) {
                    String msg = "No patientID found for HomeCommunityID "+communityID+" (Assigning Authority:"+domain+
                                ")! Skip Cross Gateway Query to:"+url;
                    log.info(msg);
                    XDSException x = new XDSException(XDSException.XDS_ERR_UNKNOWN_PATID, msg, null)
                    .setSeverity(XDSException.XDS_ERR_SEVERITY_WARNING)
                    .setLocation(communityID);
                    throw x;
                }
            }
            RespondingGatewayPortType port = RespondingGatewayPortTypeFactory.getRespondingGatewayPortSoap12(url);
            log.info("####################################################");
            log.info("####################################################");
            log.info("XCA Initiating Gateway: Send Cross Gateway Query Request to responding gateway:"+url);
            log.info("####################################################");
            log.info("####################################################");
            log.info("org.jboss.security.ignoreHttpsHost:"+System.getProperty("org.jboss.security.ignoreHttpsHost"));
            try {
                if (cfg.isAsyncHandler()) {
                    AsyncResponseHandler<AdhocQueryResponse> handler = new AsyncResponseHandler<AdhocQueryResponse>();
                    Future<?> rspAh = port.respondingGatewayCrossGatewayQueryAsync(req, handler);
                    
                    while (!rspAh.isDone()) {
                        Thread.sleep(100);
                    }
                    rsp = handler.getResponse();
                    log.info("Async Response via handler:"+rsp);
                } else if (cfg.isAsync()) {
                    Response<AdhocQueryResponse> rspA = port.respondingGatewayCrossGatewayQueryAsync(req);
                    
                    while (!rspA.isDone()) {
                        Thread.sleep(100);
                    }
                    rsp = rspA.get();
                    log.info("Async Response via callback:"+rsp);
                } else {
                    rsp = port.respondingGatewayCrossGatewayQuery(req);
                    log.info("Sync Respons:"+rsp);
                }
            } catch ( Exception x) {
                throw new XDSException( XDSException.XDS_ERR_UNAVAILABLE_COMMUNITY, 
                        "Responding Gateway not available: "+url, x).setLocation(communityID);
            }
        } catch (Exception x) {
            rsp = InfosetUtil.emptyAdhocQueryResponse();
            if (x instanceof XDSException) {
                XDSUtil.addError(rsp, (XDSException) x);
            } else {
                XDSUtil.addError(rsp, new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                        "Unexpected error in XDS service !: "+x.getMessage(),x));
            }
        }
        XDSAudit.logClientXCAQuery(req, XDSConstants.WS_ADDRESSING_ANONYMOUS, null, registryURL, !XDSConstants.XDS_B_STATUS_FAILURE.equals(rsp.getStatus()));
        return addHomeCommunityId(rsp);    
    }

    private RetrieveDocumentSetResponseType doRetrieve(RetrieveDocumentSetRequestType req) {
        RetrieveDocumentSetResponseType rsp = null;
        List<DocumentRequest> docReq = req.getDocumentRequest();
        try {
            String home = cfg.getHomeCommunityID();
            HashMap<String, List<DocumentRequest>> xcaRequests = new HashMap<String, List<DocumentRequest>>();
            HashMap<String, List<DocumentRequest>> repoRequests = new HashMap<String, List<DocumentRequest>>();
            int requestCount = docReq == null ? -1 : docReq.size();
            if (requestCount > 0) {
                List<DocumentRequest> tmpList;
                String tmpHomeID, tmpRepoID;
                DocumentRequest tmpReq;
                for (int i = 0, len = docReq.size() ; i < len ; i++) {
                    tmpReq = docReq.get(i);
                    tmpHomeID = tmpReq.getHomeCommunityId();
                    if (tmpHomeID == null || tmpHomeID.trim().length() == 0) {
                        throw new XDSException(XDSException.XDS_ERR_MISSING_HOME_COMMUNITY_ID, 
                                "Missing HomeCommunityID for doc.uniqueID "+tmpReq.getDocumentUniqueId(), 
                                null);
                    }
                    if (tmpHomeID.equals(home)) {
                        tmpRepoID = tmpReq.getRepositoryUniqueId();
                        tmpList = repoRequests.get(tmpRepoID);
                        if (tmpList == null) {
                            tmpList = new ArrayList<DocumentRequest>();
                            repoRequests.put(tmpRepoID, tmpList);
                        }
                    } else {
                        tmpList = xcaRequests.get(tmpHomeID);
                        if (tmpList == null) {
                            tmpList = new ArrayList<DocumentRequest>();
                            xcaRequests.put(tmpHomeID, tmpList);
                        }
                    }
                    tmpList.add(tmpReq);
                }
                for (Map.Entry<String, List<DocumentRequest>> entry : repoRequests.entrySet()) {
                    req.getDocumentRequest().clear();
                    req.getDocumentRequest().addAll(entry.getValue());
                    rsp = addResponse(rsp, doRepoRetrieve(entry.getKey(), req));
                }
                for (Map.Entry<String, List<DocumentRequest>> entry : xcaRequests.entrySet()) {
                    req.getDocumentRequest().clear();
                    req.getDocumentRequest().addAll(entry.getValue());
                    rsp = addResponse(rsp, doXCARetrieve(entry.getKey(), req));
                }
            }
        } catch (Exception x) {
            rsp = iheFactory.createRetrieveDocumentSetResponseType();
            if (x instanceof XDSException) {
                XDSUtil.addError(rsp, (XDSException) x);
            } else {
                XDSUtil.addError(rsp, new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                        "Unexpected error in XCA Initiating Gateway service!: "+x.getMessage(),x));
            }
        }
        if (rsp == null) {
            rsp = iheFactory.createRetrieveDocumentSetResponseType();
        }
        RegistryResponseType regRsp = rsp.getRegistryResponse();
        if (regRsp == null) {
            regRsp = factory.createRegistryResponseType();
            rsp.setRegistryResponse(regRsp);
        }
        if (regRsp.getRegistryErrorList() == null || regRsp.getRegistryErrorList().getRegistryError().isEmpty()) {
            regRsp.setStatus(XDSConstants.XDS_B_STATUS_SUCCESS);
        } else if (rsp.getDocumentResponse() == null || rsp.getDocumentResponse().isEmpty()) {
            regRsp.setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        } else {
            regRsp.setStatus(XDSConstants.XDS_B_STATUS_PARTIAL_SUCCESS);
        }
        AuditRequestInfo info = new AuditRequestInfo(LogHandler.getInboundSOAPHeader(), wsContext);
        XDSAudit.logRepositoryRetrieveExport(req, rsp, info);
        return rsp;
    }

    private RetrieveDocumentSetResponseType addResponse(RetrieveDocumentSetResponseType rsp, RetrieveDocumentSetResponseType tmpRsp) {
        if (tmpRsp == null) {
            return rsp;
        }
        if (rsp == null) {
            return tmpRsp;
        } else {
            if (tmpRsp.getDocumentResponse() != null)
                rsp.getDocumentResponse().addAll(tmpRsp.getDocumentResponse());
            if (tmpRsp.getRegistryResponse() != null) {
                RegistryErrorList errs = tmpRsp.getRegistryResponse().getRegistryErrorList();
                if (errs != null && errs.getRegistryError().size() > 0) {
                    RegistryErrorList rspErr = rsp.getRegistryResponse().getRegistryErrorList();
                    if (rspErr == null) {
                        rsp.getRegistryResponse().setRegistryErrorList(errs);
                    } else {
                        rspErr.getRegistryError().addAll(errs.getRegistryError());
                    }
                }
            }
        }
        return rsp;
    }
    
    private RetrieveDocumentSetResponseType doRepoRetrieve(String repositoryID, RetrieveDocumentSetRequestType req) {
        RetrieveDocumentSetResponseType rsp;
        URL repositoryURL = null;
        try {
            String url = cfg.getRepositoryURL(repositoryID);
            if (url == null) {
                log.warn("Unknown home XDS Repository:"+repositoryID);
                return null;
            }
            repositoryURL = new URL(url);
            DocumentRepositoryPortType port = DocumentRepositoryPortTypeFactory.getDocumentRepositoryPortSoap12(url);
            log.info("####################################################");
            log.info("####################################################");
            log.info("XCA Initiating Gateway: Send Retrieve DocumentSet Request to repository:"+repositoryID+" URL:"+url);
            log.info("####################################################");
            log.info("####################################################");
            log.info("org.jboss.security.ignoreHttpsHost:"+System.getProperty("org.jboss.security.ignoreHttpsHost"));
            try {
                rsp = port.documentRepositoryRetrieveDocumentSet(req);
            } catch ( Exception x) {
                throw new XDSException( XDSException.XDS_ERR_REPOSITORY_BUSY, "Document Repository not available: "+url, x);
            }
        } catch (Exception x) {
            rsp = iheFactory.createRetrieveDocumentSetResponseType();
            if (x instanceof XDSException) {
                XDSUtil.addError(rsp, (XDSException) x);
            } else {
                XDSUtil.addError(rsp, new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                        "Unexpected error in Initiating Gateway !: "+x.getMessage(),x));
            }
        }
        XDSAudit.logConsumerImport(null, repositoryURL, req, !XDSConstants.XDS_B_STATUS_FAILURE.equals(rsp.getRegistryResponse().getStatus()));
        return addHomeCommunityID(rsp);
    }

    private RetrieveDocumentSetResponseType doXCARetrieve(String homeCommunityID, RetrieveDocumentSetRequestType req) {
        RetrieveDocumentSetResponseType rsp;
        URL gatewayURL = null;
        try {
            String url = cfg.getRespondingGWRetrieveURL(homeCommunityID);
            if (url == null) {
                log.warn("Unknown Responding Gateway for homeCommunityID:"+homeCommunityID);
                return null;
            }
            gatewayURL = new URL(url);
            RespondingGatewayPortType port = RespondingGatewayPortTypeFactory.getRespondingGatewayPortSoap12(url);
            log.info("####################################################");
            log.info("####################################################");
            log.info("XCA Initiating Gateway: Send Cross Gateway Retrieve request to responding gateway:"+homeCommunityID+" URL:"+url);
            log.info("####################################################");
            log.info("####################################################");
            log.info("org.jboss.security.ignoreHttpsHost:"+System.getProperty("org.jboss.security.ignoreHttpsHost"));
            try {
                
                if (cfg.isAsyncHandler()) {
                    AsyncResponseHandler<RetrieveDocumentSetResponseType> handler = new AsyncResponseHandler<RetrieveDocumentSetResponseType>();
                    Future<?> rspAh = port.respondingGatewayCrossGatewayRetrieveAsync(req, handler);
                    while (!rspAh.isDone()) {
                        Thread.sleep(100);
                    }
                    rsp = handler.getResponse();
                    log.info("Async Response via handler:"+rsp);
                } else if (cfg.isAsync()) {
                    Response<RetrieveDocumentSetResponseType> rspA = port.respondingGatewayCrossGatewayRetrieveAsync(req);
                    while (!rspA.isDone()) {
                        Thread.sleep(100);
                    }
                    rsp = rspA.get();
                    log.info("Async Response via callback:"+rsp);
                } else {
                    rsp = port.respondingGatewayCrossGatewayRetrieve(req);
                    log.info("Sync Response:"+rsp);
                }
            } catch ( Exception x) {
                throw new XDSException( XDSException.XDS_ERR_REPOSITORY_BUSY, "Responding Gateway not available: "+url, x);
            }
        } catch (Exception x) {
            rsp = iheFactory.createRetrieveDocumentSetResponseType();
            if (x instanceof XDSException) {
                XDSUtil.addError(rsp, (XDSException) x);
            } else {
                XDSUtil.addError(rsp, new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                        "Unexpected error in Initiating Gateway!: "+x.getMessage(),x));
            }
        }
        XDSAudit.logConsumerXCAImport(null, gatewayURL, req, !XDSConstants.XDS_B_STATUS_FAILURE.equals(rsp.getRegistryResponse().getStatus()));
        return addHomeCommunityID(rsp);
    }
    
    private PatSlot pixQuery(AdhocQueryRequest req, SlotType1 patSlotType, String... domains) throws XDSException {
        PatSlot patSlot = new PatSlot(patSlotType);
        if (domains != null && domains.length > 0 && getPixClient() != null) {
            AdhocQueryType qry = req.getAdhocQuery();
            try {
                for (String pid : patSlotType.getValueList().getValue()) {
                    patSlot.addPatIDs(pixClient.queryXadPIDs(pid.substring(1, pid.length()-1), domains));
                }
            } catch (Exception x) {
                log.error("PIX QUERY FAILED!", x);
                throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, "PIX Query failed!", x);
            }
            return patSlot;
        } else {
            File f = new File(System.getProperty("jboss.server.config.dir","/tmp"),"pix_patids.properties");
            if (f.exists()) {
                String reqPID = patSlotType.getValueList().getValue().get(0);
                Properties p = new Properties();
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(f);
                    p.load(fis);
                    for (Entry<Object, Object> e : p.entrySet()) {
                        String pid = e.getValue().toString();
                        if (pid.charAt(0) != '\'')
                            pid = "'" + pid + "'";
                        if (pid.charAt(1) == '*')
                            pid = "'" + reqPID + pid.substring(2);
                        patSlot.addPatID(e.getKey().toString(), pid);
                    }
                    return patSlot;
                } catch (Exception x) {
                    log.info("Failed to read dummy PIX patient IDs! file:"+f, x);
                } finally {
                    if (fis != null)
                        try {
                            fis.close();
                        } catch (IOException e) {}
                }
            }
            String reason = domains == null ? "No domains (Assigning authorities) configured!" : "Missing or wrong HL7 configuration!";
            log.warn("PIX Query skipped!"+reason);
        }
        return null;
    }

    public PixQueryClient getPixClient() {
        if (pixClient == null && cfg.getLocalPIXConsumerApplication() != null) {
            log.info("########### set PIXClient!");
            try {
                HL7Application pix = getHL7Application(cfg.getLocalPIXConsumerApplication());
                if (pix != null) {
                    HL7Application pixMgr = getHL7Application(cfg.getRemotePIXManagerApplication());
                    if (pixMgr != null)
                        pixClient = new PixQueryClient(pix, pixMgr);
                }
            } catch (Exception e) {
                log.error("Failed to create PixQueryClient!",e);
            }
        }
        return pixClient;
    }
    
    private HL7Application getHL7Application(String hlAppName) throws ConfigurationException {
    	return hl7AppCache.findHL7Application(hlAppName);
    }

    private AdhocQueryResponse addHomeCommunityId(AdhocQueryResponse rsp) {
        String home = cfg.getHomeCommunityID();
        IdentifiableType obj;
        if (rsp.getRegistryObjectList() != null) {
            List<JAXBElement<? extends IdentifiableType>> objList = rsp.getRegistryObjectList().getIdentifiable();
            for (int i = 0, len = objList.size() ; i < len ; i++ ) {
                obj = objList.get(i).getValue();
                if (obj.getHome() == null && ((obj instanceof ExtrinsicObjectType) || (obj instanceof RegistryPackageType) ||
                     obj instanceof ObjectRefType)) {
                    obj.setHome(home);
                }
            }
        }
        if (rsp.getRegistryErrorList() != null) {
            for ( RegistryError err : rsp.getRegistryErrorList().getRegistryError()) {
                if (err.getLocation() == null) 
                    err.setLocation(home);
            }
        }
        return rsp;
    }

    private RetrieveDocumentSetResponseType addHomeCommunityID(RetrieveDocumentSetResponseType rsp) {
        String home = cfg.getHomeCommunityID();
        if (rsp.getDocumentResponse() != null) {
            for (DocumentResponse docRsp : rsp.getDocumentResponse()) {
                if (docRsp.getHomeCommunityId() == null)
                    docRsp.setHomeCommunityId(home);
            }
        }
        RegistryResponseType regRsp = rsp.getRegistryResponse();
        if (regRsp != null && regRsp.getRegistryErrorList() != null) {
            for ( RegistryError err : regRsp.getRegistryErrorList().getRegistryError()) {
                if (err.getLocation() == null)
                    err.setLocation(home);
            }
        }
        return rsp;
    }
    
    private class PatSlot extends HashMap<String, List<String>> {
		private static final long serialVersionUID = 1L;
		private List<String> slotValues;
        
        private PatSlot(SlotType1 slot) {
            slotValues = slot.getValueList().getValue();
        }
        
        private void addPatIDs(Map<String, String> pidOfDomain) {
            if (pidOfDomain != null) {
                List<String> pids;
                for (Map.Entry<String, String> e : pidOfDomain.entrySet()) {
                    pids = this.get(e.getKey());
                    if (pids == null) {
                        pids = new ArrayList<String>();
                        put(e.getKey(), pids);
                    }
                    pids.add(e.getValue());
                }
            }
        }
        private void addPatID(String domain, String pid) {
            log.info("######## Add PIX PatID:"+pid);
            List<String> pids = this.get(domain);
            if (pids == null) {
                pids = new ArrayList<String>();
                put(domain, pids);
            }
            log.info("######## Add PIX PatID:"+pid);
            pids.add(pid);
        }
        
        private boolean updateSlotValuesForDomain(String domain) {
            slotValues.clear();
            int pos = domain.lastIndexOf('&');
            if (pos != -1) {
                int pos1 = domain.lastIndexOf('&', pos-1);
                domain = pos1 == -1 ? domain.substring(0, pos) : domain.substring(++pos1, pos);
            }
            List<String> pids = this.get(domain);
            if (pids != null) {
                slotValues.addAll(pids);
                return true;
            }
            return false;
        }
    }
}
