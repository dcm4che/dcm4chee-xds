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

package org.dcm4chee.xds2.ws.xca;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.BindingType;
import javax.xml.ws.Response;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.soap.Addressing;
import javax.xml.ws.soap.MTOM;
import javax.xml.ws.soap.SOAPBinding;

import org.dcm4chee.xds2.common.XDSUtil;
import org.dcm4chee.xds2.common.audit.AuditRequestInfo;
import org.dcm4chee.xds2.common.audit.XDSAudit;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.conf.XCAiInitiatingGWCfg;
import org.dcm4chee.xds2.conf.XdsDevice;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;
import org.dcm4chee.xds2.infoset.iherad.RetrieveImagingDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.iherad.RetrieveImagingDocumentSetRequestType.StudyRequest;
import org.dcm4chee.xds2.infoset.util.ImagingDocumentSourcePortTypeFactory;
import org.dcm4chee.xds2.infoset.util.XCAiRespondingGatewayPortTypeFactory;
import org.dcm4chee.xds2.infoset.ws.src.ImagingDocumentSourcePortType;
import org.dcm4chee.xds2.infoset.ws.xca.XCAIRespondingGatewayPortType;
import org.dcm4chee.xds2.ws.handler.LogHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MTOM
@BindingType(value = SOAPBinding.SOAP12HTTP_MTOM_BINDING)
@Stateless
@WebService(endpointInterface="org.dcm4chee.xds2.infoset.ws.src.ImagingDocumentSourcePortType", 
        name="xcai",
        serviceName="XCAI_InitiatingGateway",
        portName="XCAI_InitiatingGateway_Port_Soap12",
        targetNamespace="urn:ihe:rad:xdsi-b:2009",
        wsdlLocation = "/META-INF/wsdl/XCA-I_InitiatingGateway.wsdl"
)

@Addressing(enabled=true, required=true)
@HandlerChain(file="handlers.xml")
public class XCAiInitiatingGW implements ImagingDocumentSourcePortType {
    
    private org.dcm4chee.xds2.infoset.ihe.ObjectFactory iheFactory = new org.dcm4chee.xds2.infoset.ihe.ObjectFactory();

    private static Logger log = LoggerFactory.getLogger(XCAiInitiatingGW.class);

    @Resource
    WebServiceContext wsContext;

    @Override
    public Response<RetrieveDocumentSetResponseType> imagingDocumentSourceRetrieveImagingDocumentSetAsync(RetrieveImagingDocumentSetRequestType body) {
        return null;
    }

    @Override
    public Future<?> imagingDocumentSourceRetrieveImagingDocumentSetAsync(RetrieveImagingDocumentSetRequestType body,
            AsyncHandler<RetrieveDocumentSetResponseType> asyncHandler) {
        return null;
    }

    @Override
    public RetrieveDocumentSetResponseType imagingDocumentSourceRetrieveImagingDocumentSet(RetrieveImagingDocumentSetRequestType req) {
        return doRetrieve(req);
    }

    private RetrieveDocumentSetResponseType doRetrieve(RetrieveImagingDocumentSetRequestType req) {
        RetrieveDocumentSetResponseType rsp = null, tmpRsp;
        XCAiInitiatingGWCfg cfg = XdsDevice.getXCAiInitiatingGW();
        String homeCommunityID = cfg.getHomeCommunityID();
        String home;
        for (Entry<String, List<StudyRequest>> entry : XDSUtil.splitRequestPerHomeCommunityID(req).entrySet()) {
            req.getStudyRequest().clear();
            req.getStudyRequest().addAll(entry.getValue());
            home = entry.getKey();
            if (homeCommunityID.equals(home)) {
                tmpRsp = doSourceRetrieve(req);
            } else if (cfg.getRespondingGWURL(home) != null) {
                tmpRsp = doXcaiRetrieve(entry.getKey(), req, cfg);
            } else {
                log.warn("Unknown HomeCommunityID! :"+home);
                XDSException x = new XDSException(XDSException.XDS_ERR_UNKNOWN_COMMUNITY, 
                        "HomeCommunityID '"+home+"' not known by XCA-I Initiating Gateway!", null);
                x.setSeverity(XDSException.XDS_ERR_SEVERITY_WARNING);
                if (rsp == null)
                    rsp = iheFactory.createRetrieveDocumentSetResponseType();
                XDSUtil.addError(rsp, x);
                continue;
            }
            if (rsp == null) {
                rsp = tmpRsp;
            } else {
                XDSUtil.addResponse(tmpRsp, rsp);
            }
        }
        if (rsp == null)
            rsp = iheFactory.createRetrieveDocumentSetResponseType();
        XDSUtil.finishResponse(rsp);
        XDSAudit.logImgRetrieve(rsp, new AuditRequestInfo(LogHandler.getInboundSOAPHeader(), wsContext));
        return rsp;
    }

    private RetrieveDocumentSetResponseType doSourceRetrieve(RetrieveImagingDocumentSetRequestType req) {
        RetrieveDocumentSetResponseType rsp = null, tmpRsp;
        for (Entry<String, List<StudyRequest>> entry : XDSUtil.splitRequestPerSrcID(req).entrySet()) {
            req.getStudyRequest().clear();
            req.getStudyRequest().addAll(entry.getValue());
            tmpRsp = doSourceRetrieve(entry.getKey(), req);
            if (rsp == null) {
                rsp = tmpRsp;
            } else {
                XDSUtil.addResponse(tmpRsp, rsp);
            }
        }
        if (rsp == null)
            rsp = iheFactory.createRetrieveDocumentSetResponseType();
        return rsp;
    }
    private RetrieveDocumentSetResponseType doSourceRetrieve(String sourceID, RetrieveImagingDocumentSetRequestType req) {
        RetrieveDocumentSetResponseType rsp;
        try {
            String url = XdsDevice.getXCAiInitiatingGW().getXDSiSourceURL(sourceID);
            if (url == null) {
                return iheFactory.createRetrieveDocumentSetResponseType();
            }
            ImagingDocumentSourcePortType port = ImagingDocumentSourcePortTypeFactory.getImagingDocumentSourcePort(url);
            log.info("####################################################");
            log.info("####################################################");
            log.info("XCA-I Responding Gateway: Send Retrieve Imaging DocumentSet Request to xdsi source:"+sourceID+" URL:"+url);
            log.info("####################################################");
            log.info("####################################################");
            log.info("org.jboss.security.ignoreHttpsHost:"+System.getProperty("org.jboss.security.ignoreHttpsHost"));
            try {
                rsp = port.imagingDocumentSourceRetrieveImagingDocumentSet(req);
            } catch ( Exception x) {
                throw new XDSException( XDSException.XDS_ERR_REPOSITORY_BUSY, "XDS-I Imaging Source not available: "+url, x);
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
        XDSAudit.logStudyUsed(rsp, new AuditRequestInfo(LogHandler.getInboundSOAPHeader(), wsContext));
        return rsp;
    }

    private RetrieveDocumentSetResponseType doXcaiRetrieve(String home, RetrieveImagingDocumentSetRequestType req, XCAiInitiatingGWCfg cfg) {
        RetrieveDocumentSetResponseType rsp = null;
        try {
            String url = cfg.getRespondingGWURL(home);
            if (url == null) {
                return iheFactory.createRetrieveDocumentSetResponseType();
            }
            XCAIRespondingGatewayPortType port = XCAiRespondingGatewayPortTypeFactory.getXCAIRespondingGatewayPortSoap12(url);
            log.info("####################################################");
            log.info("####################################################");
            log.info("XCA-I Initiating Gateway: Send Retrieve Imaging DocumentSet Request to XCA-I Responding Gateway HomeCommunityID:"+home+" URL:"+url);
            log.info("####################################################");
            log.info("####################################################");
            log.info("org.jboss.security.ignoreHttpsHost:"+System.getProperty("org.jboss.security.ignoreHttpsHost"));
            try {
                rsp = port.respondingGatewayCrossGatewayRetrieveImagingDocumentSet(req);
            } catch ( Exception x) {
                throw new XDSException( XDSException.XDS_ERR_REPOSITORY_BUSY, "XDS-I Imaging Source not available: "+url, x);
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
        XDSAudit.logStudyUsed(rsp, new AuditRequestInfo(LogHandler.getInboundSOAPHeader(), wsContext));
        return rsp;
    }

}
