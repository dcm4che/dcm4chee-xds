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

import java.util.List;

import javax.xml.bind.JAXBException;

import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.ObjectFactory;
import org.dcm4chee.xds2.infoset.rim.RegistryError;
import org.dcm4chee.xds2.infoset.rim.RegistryErrorList;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XDSUtil {

    public static ObjectFactory factory = new ObjectFactory();
    private static Logger log = LoggerFactory.getLogger(XDSUtil.class);
    
    private static final char[] HEX_STRINGS = "0123456789abcdef".toCharArray();
    
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

}
