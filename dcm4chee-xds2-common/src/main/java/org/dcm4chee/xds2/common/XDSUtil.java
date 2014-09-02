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
package org.dcm4chee.xds2.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType.DocumentRequest;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;
import org.dcm4chee.xds2.infoset.iherad.RetrieveImagingDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.iherad.RetrieveImagingDocumentSetRequestType.StudyRequest;
import org.dcm4chee.xds2.infoset.iherad.RetrieveImagingDocumentSetRequestType.StudyRequest.SeriesRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.ObjectFactory;
import org.dcm4chee.xds2.infoset.rim.RegistryError;
import org.dcm4chee.xds2.infoset.rim.RegistryErrorList;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XDSUtil {

    public static ObjectFactory factory = new ObjectFactory();
    public static org.dcm4chee.xds2.infoset.ihe.ObjectFactory iheFactory = new org.dcm4chee.xds2.infoset.ihe.ObjectFactory();
    
    private static Logger log = LoggerFactory.getLogger(XDSUtil.class);
    
    private static final char[] HEX_STRINGS = "0123456789abcdef".toCharArray();
    private static final String DTM_FORMAT = "yyyyMMddHHmmss.SSS";
    private static final String DTM_DEFAULT = "19700101000000.000";
    private static final SimpleDateFormat DTM_PARSER = new SimpleDateFormat();
    private static final SimpleDateFormat DTM_FORMATTER = new SimpleDateFormat(DTM_FORMAT);
    
    public static void addError(RegistryResponseType rsp, XDSException x) {
        rsp.setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        try {
            RegistryErrorList errList = rsp.getRegistryErrorList();
            if (errList == null) {
                errList = factory.createRegistryErrorList();
                rsp.setRegistryErrorList( errList );
            }
            List<RegistryError> errors = errList.getRegistryError();
            RegistryError error = getRegistryError(x);
            errors.add(error);
        } catch (JAXBException e) {
            log.error("Failed to set ErrorList in response!", e);
        }
    }
    public static void addError(AdhocQueryResponse rsp, XDSException x) {
        rsp.setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        try {
            RegistryErrorList errList = rsp.getRegistryErrorList();
            if (errList == null) {
                errList = factory.createRegistryErrorList();
                rsp.setRegistryErrorList( errList );
            }
            List<RegistryError> errors = errList.getRegistryError();
            RegistryError error = getRegistryError(x);
            errors.add(error);
        } catch (JAXBException e) {
            log.error("Failed to set ErrorList in response!", e);
        }
    }
    public static void addError(RetrieveDocumentSetResponseType rsp, XDSException x) {
        try {
            rsp.setRegistryResponse(getErrorRegistryResponse(x));
        } catch (JAXBException e) {
            log.error("Failed to set RegistryResponse in RetrieveDocumentSetResponse!", e);
        }
    }

    public static RegistryResponseType getErrorRegistryResponse(XDSException x) throws JAXBException {
        RegistryResponseType rsp = factory.createRegistryResponseType();
        rsp.setStatus(XDSConstants.XDS_B_STATUS_FAILURE);
        RegistryErrorList errList = factory.createRegistryErrorList();
        List<RegistryError> errors = errList.getRegistryError();
        rsp.setRegistryErrorList( errList );
        RegistryError error = getRegistryError(x);
        errors.add(error);
        return rsp;
    }
   
    public static RegistryError getRegistryError(XDSException xdsException) throws JAXBException {
        RegistryError error = factory.createRegistryError();
        error.setErrorCode(xdsException.getErrorCode());
        error.setCodeContext(xdsException.getMessage());
        error.setSeverity(xdsException.getSeverity());
        error.setLocation(xdsException.getLocation());
        return error;
    }
 
    public static RegistryError getRegistryError(String severity, String code, String msg, String location) {
        try {
            RegistryError error = factory.createRegistryError();
            error.setSeverity(severity);
            error.setErrorCode(code);
            error.setCodeContext(msg);
            error.setLocation(location);
            error.setValue(msg);
            return error;
        } catch (Exception x) {
            log.debug("Could not create registry exception with serverity: " + severity + ", code: " + code +
                    ", context: " + msg + ", location: " + location);
            return null;
        }
    }

    public static RetrieveDocumentSetResponseType finishResponse(RetrieveDocumentSetResponseType rsp) {
        if (rsp == null)
            rsp = iheFactory.createRetrieveDocumentSetResponseType();
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
        return rsp;
    }

    public static final String toHexString(byte[] ba) {
        StringBuilder sb = new StringBuilder();
        int h;
        for(int i=0 ; i < ba.length ; i++) {
            h = ba[i] & 0xff;
            sb.append(HEX_STRINGS[h>>>4]);
            sb.append(HEX_STRINGS[h&0x0f]);
        }
        return sb.toString();
    }

    public static String getQueryPatID(List<SlotType1> slots) {
        String name;
        for (SlotType1 slot : slots) {
            name = slot.getName();
            if ((XDSConstants.QRY_DOCUMENT_ENTRY_PATIENT_ID.equals(name) ||
                    XDSConstants.QRY_FOLDER_PATIENT_ID.equals(name) ||
                    XDSConstants.QRY_SUBMISSIONSET_PATIENT_ID.equals(name)) &&
                    slot.getValueList() != null && slot.getValueList().getValue().size() > 0) {
                return slot.getValueList().getValue().get(0);
            }
        }
        return null;
    }
    
    public static String[] map2keyValueStrings(Map<String,String> map, char delimiter) {
        String[] sa = new String[map.size()];
        int i = 0;
        for (Map.Entry<String, String> e : map.entrySet()) {
            sa[i++] = e.getKey()+delimiter+e.getValue();
        }
        return sa;
    }

    public static Map<String, String> storeKeyValueStrings2map(String[] keyValues, char delimiter, String defaultKey, Map<String,String> map) {
        if (map == null) {
            map = new HashMap<String,String>();
        } else {
            map.clear();
        }
        int pos;
        String value;
        for (int i = 0 ; i < keyValues.length ; i++) {
            value = keyValues[i];
            pos = value.indexOf(delimiter);
            if (pos == -1) {
                map.put(defaultKey, value);
            } else {
                map.put(value.substring(0, pos), value.substring(++pos));
            }
        }
        return map;
    }
    
    public static String getValue(String key, String defaultKey, Map<String,String> map) {
        String value = map.get(key);
        if (value == null)
            value = map.get(defaultKey);
        return value;
    }
    
    public static Map<String, List<StudyRequest>> splitRequestPerSrcID(RetrieveImagingDocumentSetRequestType req) {
        return splitRequest(req, false);
    }
    public static Map<String, List<StudyRequest>> splitRequestPerHomeCommunityID(RetrieveImagingDocumentSetRequestType req) {
        return splitRequest(req, true);
    }
    private static Map<String, List<StudyRequest>> splitRequest(RetrieveImagingDocumentSetRequestType req, boolean splitByHomeCommunity) {
        List<StudyRequest> docReq = req.getStudyRequest();
        HashMap<String, List<StudyRequest>> srcRequests = new HashMap<String, List<StudyRequest>>();
        if (docReq.size() > 0) {
            HashMap<String, StudyRequest> srcId2StudyReq = new HashMap<String, StudyRequest>();
            HashMap<String, SeriesRequest> srcId2SeriesReq = new HashMap<String, SeriesRequest>();
            List<StudyRequest> tmpStudyList;
            String key;
            StudyRequest tmpStudy;
            SeriesRequest tmpSeries;
            for (StudyRequest study : docReq) {
                srcId2StudyReq.clear();
                for (SeriesRequest series : study.getSeriesRequest()) {
                    srcId2SeriesReq.clear();
                    for (DocumentRequest doc : series.getDocumentRequest()) {
                        key = splitByHomeCommunity ? doc.getHomeCommunityId() : doc.getRepositoryUniqueId();
                        tmpSeries = srcId2SeriesReq.get(key);
                        if (tmpSeries == null) {
                            tmpSeries = new SeriesRequest();
                            tmpSeries.setSeriesInstanceUID(series.getSeriesInstanceUID());
                            srcId2SeriesReq.put(key, tmpSeries);
                            tmpStudy = srcId2StudyReq.get(key);
                            if (tmpStudy == null) {
                                tmpStudy = new StudyRequest();
                                tmpStudy.setStudyInstanceUID(study.getStudyInstanceUID());
                                srcId2StudyReq.put(key, tmpStudy);
                                tmpStudyList = srcRequests.get(key);
                                if (tmpStudyList == null) {
                                    tmpStudyList = new ArrayList<StudyRequest>();
                                    srcRequests.put(key, tmpStudyList);
                                }
                                tmpStudyList.add(tmpStudy);
                            }
                            tmpStudy.getSeriesRequest().add(tmpSeries);
                        }
                        tmpSeries.getDocumentRequest().add(doc);
                    }
                }
            }
        }
        return srcRequests;
    }
    
    public static void addResponse(RetrieveDocumentSetResponseType source, RetrieveDocumentSetResponseType target) {
        if (source.getDocumentResponse().size() > 0)
            target.getDocumentResponse().addAll(source.getDocumentResponse());
        if (source.getRegistryResponse() != null) {
            RegistryErrorList errs = source.getRegistryResponse().getRegistryErrorList();
            if (errs != null && errs.getRegistryError().size() > 0) {
                RegistryErrorList rspErr = target.getRegistryResponse().getRegistryErrorList();
                if (rspErr == null) {
                    target.getRegistryResponse().setRegistryErrorList(errs);
                } else {
                    rspErr.getRegistryError().addAll(errs.getRegistryError());
                }
            }
        }
    }
    
    public static String normalizeDTM(String dtm, boolean to) {
        switch (dtm == null ? 0 : dtm.length()) {
        case 4: case 6: case 8: case 10: case 12: case 14:
            break;
        case 18:
            log.info("return unchanged! :"+dtm);
            return dtm;
        default:
            throw new IllegalArgumentException("DateTime value has wrong length!");
        }
        String normalized;
        if (to) {
            try {
                synchronized (DTM_PARSER) {
                    DTM_PARSER.applyPattern(DTM_FORMAT.substring(0, dtm.length()));
                    DTM_PARSER.parse(dtm);
                    Calendar cal = DTM_PARSER.getCalendar();
                    switch (dtm.length()) {
                        case 4:
                            cal.set(Calendar.MONTH, 11);
                        case 6:
                            cal.getTime();
                            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                        case 8:
                            cal.set(Calendar.HOUR_OF_DAY, 23);
                        case 10:
                            cal.set(Calendar.MINUTE, 59);
                        case 12:
                            cal.set(Calendar.SECOND, 59);
                        case 14:
                            cal.set(Calendar.MILLISECOND, 999);
                    }
                    normalized = DTM_FORMATTER.format(cal.getTime());
                }
            } catch (ParseException x) {
                throw new IllegalArgumentException("DateTime value has wrong format!", x);
            }
        } else {
            normalized = dtm + DTM_DEFAULT.substring(dtm.length());
        }
        log.debug("######### Normalized DTM:\nfrom {} to {}", dtm, normalized);
        if (!normalized.startsWith(dtm))
            log.warn("%%%%%%%%%%%%%%%% Normalize DTM value has changed the base! from {} to {}", dtm, normalized);
        return normalized;
    }
    
}


