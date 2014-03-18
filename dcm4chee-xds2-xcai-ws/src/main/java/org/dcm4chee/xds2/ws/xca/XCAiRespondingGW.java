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
import javax.inject.Inject;
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
import org.dcm4chee.xds2.conf.XCAiRespondingGWCfg;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;
import org.dcm4chee.xds2.infoset.iherad.RetrieveImagingDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.iherad.RetrieveImagingDocumentSetRequestType.StudyRequest;
import org.dcm4chee.xds2.infoset.util.ImagingDocumentSourcePortTypeFactory;
import org.dcm4chee.xds2.infoset.ws.src.ImagingDocumentSourcePortType;
import org.dcm4chee.xds2.infoset.ws.xca.XCAIRespondingGatewayPortType;
import org.dcm4chee.xds2.ws.handler.LogHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MTOM
@BindingType(value = SOAPBinding.SOAP12HTTP_MTOM_BINDING)
@Stateless
@WebService(endpointInterface="org.dcm4chee.xds2.infoset.ws.xca.XCAIRespondingGatewayPortType", 
        name="xcai",
        serviceName="XCAIRespondingGateway",
        portName="XCAIRespondingGateway_Port",
        targetNamespace="urn:ihe:rad:xdsi-b:2009",
        wsdlLocation = "/META-INF/wsdl/XCAIRespondingGateway.wsdl"
)

@Addressing(enabled=true, required=true)
@HandlerChain(file="handlers.xml")
public class XCAiRespondingGW implements XCAIRespondingGatewayPortType {
    
    private org.dcm4chee.xds2.infoset.ihe.ObjectFactory iheFactory = new org.dcm4chee.xds2.infoset.ihe.ObjectFactory();

    private static Logger log = LoggerFactory.getLogger(XCAiRespondingGW.class);

    @Resource
    WebServiceContext wsContext;

    @Inject
    XCAiRespondingGWCfg cfg;
    
    @Override
    public Response<RetrieveDocumentSetResponseType> respondingGatewayCrossGatewayRetrieveImagingDocumentSetAsync(
            RetrieveImagingDocumentSetRequestType body) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<?> respondingGatewayCrossGatewayRetrieveImagingDocumentSetAsync(
            RetrieveImagingDocumentSetRequestType body,
            AsyncHandler<RetrieveDocumentSetResponseType> asyncHandler) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RetrieveDocumentSetResponseType respondingGatewayCrossGatewayRetrieveImagingDocumentSet(
            RetrieveImagingDocumentSetRequestType req) {
        return doRetrieve(req);
    }

    private RetrieveDocumentSetResponseType doRetrieve(RetrieveImagingDocumentSetRequestType req) {
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
        XDSUtil.finishResponse(rsp);
        XDSAudit.logImgRetrieve(rsp, new AuditRequestInfo(LogHandler.getInboundSOAPHeader(), wsContext));
        return rsp;
    }
    
    private RetrieveDocumentSetResponseType doSourceRetrieve(String sourceID, RetrieveImagingDocumentSetRequestType req) {
        RetrieveDocumentSetResponseType rsp;
        try {
            String url = cfg.getXDSiSourceURL(sourceID);
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

}
