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

package org.dcm4chee.xds2.imgsrc.ws;

import java.util.List;
import java.util.concurrent.Future;

import javax.activation.DataHandler;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.enterprise.inject.Instance;
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

import org.dcm4che3.net.Device;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.XDSUtil;
import org.dcm4chee.xds2.common.audit.AuditRequestInfo;
import org.dcm4chee.xds2.common.audit.XDSAudit;
import org.dcm4chee.xds2.common.deactivatable.DeactivateableByConfiguration;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.conf.XDSiSourceCfg;
import org.dcm4chee.xds2.imgsrc.DicomObjectProvider;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType.DocumentRequest;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType.DocumentResponse;
import org.dcm4chee.xds2.infoset.iherad.RetrieveImagingDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.iherad.RetrieveImagingDocumentSetRequestType.StudyRequest;
import org.dcm4chee.xds2.infoset.iherad.RetrieveImagingDocumentSetRequestType.StudyRequest.SeriesRequest;
import org.dcm4chee.xds2.infoset.rim.ObjectFactory;
import org.dcm4chee.xds2.infoset.rim.RegistryErrorList;
import org.dcm4chee.xds2.infoset.ws.src.ImagingDocumentSourcePortType;
import org.dcm4chee.xds2.ws.handler.LogHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MTOM
@BindingType(value = SOAPBinding.SOAP12HTTP_MTOM_BINDING)
@Stateless
@WebService(endpointInterface="org.dcm4chee.xds2.infoset.ws.src.ImagingDocumentSourcePortType", 
        name="xdsi-src",
        serviceName="ImagingDocumentSource",
        portName="ImagingDocumentSource_Port_Soap12",
        targetNamespace="urn:ihe:rad:xdsi-b:2009",
        wsdlLocation = "/META-INF/wsdl/XDS-I.b_ImagingDocumentSource.wsdl"
)

@Addressing(enabled=true, required=true)
@HandlerChain(file="handlers.xml")
@DeactivateableByConfiguration(extension = XDSiSourceCfg.class)
public class XDSiSource implements ImagingDocumentSourcePortType {
    
    private ObjectFactory factory = new ObjectFactory();
    private org.dcm4chee.xds2.infoset.ihe.ObjectFactory iheFactory = new org.dcm4chee.xds2.infoset.ihe.ObjectFactory();

    private static Logger log = LoggerFactory.getLogger(XDSiSource.class);

    @Resource
    private WebServiceContext wsContext;

    @Inject
    private Device device;
    private XDSiSourceCfg cfg;

    @Inject
    private Instance<DicomObjectProvider> provider;
    
    @PostConstruct
    private void init() {
    	cfg = device.getDeviceExtension(XDSiSourceCfg.class);
    }
    
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
        log.info("######## imagingDocumentSourceRetrieveImagingDocumentSet called!");
        RetrieveDocumentSetResponseType rsp = iheFactory.createRetrieveDocumentSetResponseType();
        rsp.setRegistryResponse(factory.createRegistryResponseType());
        List<String> supportedTS = req.getTransferSyntaxUIDList().getTransferSyntaxUID();
        try {
            DicomObjectProvider dicomProvider = getDicomObjectProvider();
            for (StudyRequest study : req.getStudyRequest()) {
                for (SeriesRequest series : study.getSeriesRequest()) {
                    for ( DocumentRequest doc : series.getDocumentRequest()) {
                        addDicomObject(dicomProvider, rsp, study.getStudyInstanceUID(), series.getSeriesInstanceUID(), doc, supportedTS);
                    }
                }
            }
        } catch (XDSException x) {
            XDSUtil.addError(rsp, x);
        }
        RegistryErrorList errList = rsp.getRegistryResponse().getRegistryErrorList();
        if (errList == null || errList.getRegistryError().isEmpty()) {
            rsp.getRegistryResponse().setStatus(XDSConstants.XDS_B_STATUS_SUCCESS);
        } else if (rsp.getDocumentResponse().isEmpty()) {
            rsp.getRegistryResponse().setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        } else {
            rsp.getRegistryResponse().setStatus(XDSConstants.XDS_B_STATUS_PARTIAL_SUCCESS);
        }
        XDSAudit.logImgRetrieve(rsp, new AuditRequestInfo(LogHandler.getInboundSOAPHeader(), wsContext));
        return rsp;
    }

    private void addDicomObject(DicomObjectProvider dicomProvider, final RetrieveDocumentSetResponseType rsp, final String studyUID, final String seriesUID, final DocumentRequest doc, List<String> supportedTS) {
        try {
            DataHandler dh = dicomProvider.getDataHandler(studyUID, seriesUID, doc.getDocumentUniqueId(), cfg.getDicomObjectProvider(), supportedTS);
            DocumentResponse docRsp = iheFactory.createRetrieveDocumentSetResponseTypeDocumentResponse();
            docRsp.setDocument(dh);
            docRsp.setDocumentUniqueId(doc.getDocumentUniqueId());
            docRsp.setHomeCommunityId(doc.getHomeCommunityId());
            docRsp.setMimeType("application/dicom");
            docRsp.setRepositoryUniqueId(doc.getRepositoryUniqueId());
            rsp.getDocumentResponse().add(docRsp);
        } catch (Exception x) {
            XDSUtil.addError(rsp, new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                    "Failed to retrieve DicomObject! Reason:"+x.getMessage(), null).setLocation(doc.getDocumentUniqueId()));
        }
    }
    
    private DicomObjectProvider getDicomObjectProvider() throws XDSException {
        String pCfg = cfg.getDicomObjectProvider();
        int pos = pCfg.indexOf(':');
        String name = pos == -1 ? pCfg : pCfg.substring(0, pos);
        if (provider.isUnsatisfied())
            throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, "Deployment error! No DicomObjectProvider available!", null);
        for (DicomObjectProvider p : provider) {
            if (name.equalsIgnoreCase(p.getProviderName()))
                return p;
        }
        throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, "Configuration error! '"+name+"' DicomObjectProvider not found!", null);
    }
    
}
