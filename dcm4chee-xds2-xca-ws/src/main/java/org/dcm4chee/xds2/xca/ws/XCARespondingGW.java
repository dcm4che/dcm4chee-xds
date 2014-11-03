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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
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

import org.dcm4che3.net.Device;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.XDSUtil;
import org.dcm4chee.xds2.common.audit.AuditRequestInfo;
import org.dcm4chee.xds2.common.audit.XDSAudit;
import org.dcm4chee.xds2.common.deactivatable.DeactivateableByConfiguration;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.conf.XCARespondingGWCfg;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType.DocumentRequest;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType.DocumentResponse;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.ObjectRefType;
import org.dcm4chee.xds2.infoset.rim.RegistryError;
import org.dcm4chee.xds2.infoset.rim.RegistryErrorList;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.infoset.util.DocumentRegistryPortTypeFactory;
import org.dcm4chee.xds2.infoset.util.DocumentRepositoryPortTypeFactory;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryPortType;
import org.dcm4chee.xds2.infoset.ws.repository.DocumentRepositoryPortType;
import org.dcm4chee.xds2.infoset.ws.xca.RespondingGatewayPortType;
import org.dcm4chee.xds2.ws.handler.LogHandler;
import org.dcm4chee.xds2.ws.util.CxfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MTOM
@BindingType(value = SOAPBinding.SOAP12HTTP_MTOM_BINDING)
@Stateless
@WebService(endpointInterface="org.dcm4chee.xds2.infoset.ws.xca.RespondingGatewayPortType", 
        name="xca",
        serviceName="RespondingGateway",
        portName="RespondingGateway_Port_Soap12",
        targetNamespace="urn:ihe:iti:xds-b:2007",
        wsdlLocation = "/META-INF/wsdl/XCARespondingGateway.wsdl"
)

@Addressing(enabled=true, required=true)
@HandlerChain(file="handlers.xml")
@DeactivateableByConfiguration(extension = XCARespondingGWCfg.class)
@PermitAll
public class XCARespondingGW implements RespondingGatewayPortType {
    
    private org.dcm4chee.xds2.infoset.ihe.ObjectFactory iheFactory = new org.dcm4chee.xds2.infoset.ihe.ObjectFactory();

    private static Logger log = LoggerFactory.getLogger(XCARespondingGW.class);

    @Resource
    private WebServiceContext wsContext;

    @Inject 
    private Device device;
    private XCARespondingGWCfg cfg;
    
    @PostConstruct
    public void init() {
    	cfg = device.getDeviceExtension(XCARespondingGWCfg.class);
    }
    
    @Override
    public Response<RetrieveDocumentSetResponseType> respondingGatewayCrossGatewayRetrieveAsync(
            RetrieveDocumentSetRequestType req) {
        log.info("### respondingGatewayCrossGatewayRetrieveAsync called");
        return null;
    }

    @Override
    public Future<?> respondingGatewayCrossGatewayRetrieveAsync(
            RetrieveDocumentSetRequestType req,
            AsyncHandler<RetrieveDocumentSetResponseType> asyncHandler) {
        log.info("###respondingGatewayCrossGatewayRetrieveAsync(handler) called");
        return null;
    }

    @Override
    public RetrieveDocumentSetResponseType respondingGatewayCrossGatewayRetrieve(
            RetrieveDocumentSetRequestType req) {
        log.info("###respondingGatewayCrossGatewayRetrieve called");
        RetrieveDocumentSetResponseType rsp = doRetrieve(req);
        return rsp;
    }

    @Override
    public Response<AdhocQueryResponse> respondingGatewayCrossGatewayQueryAsync(
            AdhocQueryRequest req) {
        log.info("###respondingGatewayCrossGatewayQueryAsync called");
        return null;
    }

    @Override
    public Future<?> respondingGatewayCrossGatewayQueryAsync(
            AdhocQueryRequest req,
            AsyncHandler<AdhocQueryResponse> asyncHandler) {
        log.info("###respondingGatewayCrossGatewayQueryAsync(handler called");
        return null;
    }

    @Override
    public AdhocQueryResponse respondingGatewayCrossGatewayQuery(
            AdhocQueryRequest req) {
        log.info("###respondingGatewayCrossGatewayQuery called");
        AdhocQueryResponse rsp = doStoredQuery(req);
        return rsp;
    }

    private AdhocQueryResponse doStoredQuery(AdhocQueryRequest req) {
        AdhocQueryResponse rsp;
        URL registryURL = null;
        try {
            String url = cfg.getRegistryURL();
            registryURL = new URL(url);
            DocumentRegistryPortType port = DocumentRegistryPortTypeFactory.getDocumentRegistryPortSoap12(url);
            log.info("####################################################");
            log.info("####################################################");
            log.info("XCA Responding Gateway: Send Stored Query Request to registry:"+url);
            log.info("####################################################");
            log.info("####################################################");
            log.info("org.jboss.security.ignoreHttpsHost:"+System.getProperty("org.jboss.security.ignoreHttpsHost"));
            try {
                CxfUtil.disableMTOMResponse(wsContext);
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

    private RetrieveDocumentSetResponseType doRetrieve(RetrieveDocumentSetRequestType req) {
        RetrieveDocumentSetResponseType rsp = null;
        URL repositoryURL = null;
        List<DocumentRequest> docReq = req.getDocumentRequest();
        HashMap<String, List<DocumentRequest>> repoRequests = new HashMap<String, List<DocumentRequest>>();
        if (docReq != null && docReq.size() > 0) {
            List<DocumentRequest> tmpList;
            String repositoryID = docReq.get(0).getRepositoryUniqueId();
            String tmpRepoID;
            DocumentRequest tmpReq;
            for (int i = docReq.size()-1 ; i > 0 ; i--) {
                tmpRepoID = docReq.get(i).getRepositoryUniqueId();
                if (!repositoryID.equals(tmpRepoID)) {
                    tmpReq = docReq.remove(i);
                    tmpList = repoRequests.get(tmpRepoID);
                    if (tmpList == null) {
                        tmpList = new ArrayList<DocumentRequest>();
                        repoRequests.put(repositoryID, tmpList);
                    }
                    tmpList.add(tmpReq);
                }
            }
            rsp = doRepoRetrieve(repositoryID, req);
            RetrieveDocumentSetResponseType tmpRsp;
            for (Map.Entry<String, List<DocumentRequest>> entry : repoRequests.entrySet()) {
                req.getDocumentRequest().clear();
                req.getDocumentRequest().addAll(entry.getValue());
                tmpRsp = doRepoRetrieve(entry.getKey(), req);
                if (rsp == null) {
                    rsp = tmpRsp;
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
                XDSAudit.logConsumerImport(null, repositoryURL, req, 
                        !XDSConstants.XDS_B_STATUS_FAILURE.equals(tmpRsp.getRegistryResponse().getStatus()));
            }
        }
        AuditRequestInfo info = new AuditRequestInfo(LogHandler.getInboundSOAPHeader(), wsContext);
        XDSAudit.logXCARetrieveExport(req, rsp, info);
        return XDSUtil.finishResponse(rsp);
    }

    private RetrieveDocumentSetResponseType doRepoRetrieve(String repositoryID, RetrieveDocumentSetRequestType req) {
        RetrieveDocumentSetResponseType rsp;
        URL xdsRepositoryURI = null;
        try {
            String home = req.getDocumentRequest().get(0).getHomeCommunityId();
            if (home == null)
                throw new XDSException( XDSException.XDS_ERR_MISSING_HOME_COMMUNITY_ID, "Missing Home Community ID in request! repositoryID: "+repositoryID, null);
            String url = cfg.getRepositoryURL(repositoryID);
            if (url == null) {
                return iheFactory.createRetrieveDocumentSetResponseType();
            }
            xdsRepositoryURI = new URL(url);
            DocumentRepositoryPortType port = DocumentRepositoryPortTypeFactory.getDocumentRepositoryPortSoap12(url);
            log.info("####################################################");
            log.info("####################################################");
            log.info("XCA Responding Gateway: Send Retrieve DocumentSet Request to repository:"+repositoryID+" URL:"+url);
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
                        "Unexpected error in XDS service !: "+x.getMessage(),x));
            }
        }
        XDSAudit.logConsumerImport(null, xdsRepositoryURI, req, !XDSConstants.XDS_B_STATUS_FAILURE.equals(rsp.getRegistryResponse().getStatus()));
        return addHomeCommunityID(rsp);
    }

    
    private AdhocQueryResponse addHomeCommunityId(AdhocQueryResponse rsp) {
        String home = cfg.getHomeCommunityID();
        IdentifiableType obj;
        if (rsp.getRegistryObjectList() != null) {
            List<JAXBElement<? extends IdentifiableType>> objList = rsp.getRegistryObjectList().getIdentifiable();
            for (int i = 0, len = objList.size() ; i < len ; i++ ) {
                obj = objList.get(i).getValue();
                if ((obj instanceof ExtrinsicObjectType) || (obj instanceof RegistryPackageType) ||
                     obj instanceof ObjectRefType) {
                    obj.setHome(home);
                }
            }
        }
        if (rsp.getRegistryErrorList() != null) {
            for ( RegistryError err : rsp.getRegistryErrorList().getRegistryError()) {
                err.setLocation(home);
            }
        }
        return rsp;
    }

    private RetrieveDocumentSetResponseType addHomeCommunityID(RetrieveDocumentSetResponseType rsp) {
        String home = cfg.getHomeCommunityID();
        if (rsp.getDocumentResponse() != null) {
            for (DocumentResponse docRsp : rsp.getDocumentResponse()) {
                docRsp.setHomeCommunityId(home);
            }
        }
        RegistryResponseType regRsp = rsp.getRegistryResponse();
        if (regRsp != null && regRsp.getRegistryErrorList() != null) {
            for ( RegistryError err : regRsp.getRegistryErrorList().getRegistryError()) {
                err.setLocation(home);
            }
        }
        return rsp;
    }

}
