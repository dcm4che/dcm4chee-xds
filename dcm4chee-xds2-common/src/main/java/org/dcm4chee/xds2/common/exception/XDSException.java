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
 * Java(TM), available at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * TIANI Medgraph AG.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunter.zeilinger@tiani.com>
 * Franz Willer <franz.willer@gmail.com>
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
package org.dcm4chee.xds2.common.exception;

/**
 * Exception for handling XDS RegistryError.
 * Note: use Exception.message for RegistryError.codeContext. 
 * (value (body) of RegistryError should be empty)
 * 
 * @author franz.willer@gmail.com
 *
 */
public class XDSException extends Exception {

    private static final long serialVersionUID = 1L;

    public static final String XDS_ERR_MISSING_DOCUMENT = "XDSMissingDocument";
    public static final String XDS_ERR_MISSING_DOCUMENT_METADATA = "XDSMissingDocumentMetadata";
    public static final String XDS_ERR_REG_NOT_AVAIL = "XDSRegistryNotAvailable";
    public static final String XDS_ERR_REGISTRY_ERROR = "XDSRegistryError";
    public static final String XDS_ERR_REPOSITORY_ERROR = "XDSRepositoryError";
    public static final String XDS_ERR_REGISTRY_DUPLICATE_UNIQUE_ID_IN_MSG = "XDSRegistryDuplicateUniqueIdInMessage";
    public static final String XDS_ERR_REPOSITORY_DUPLICATE_UNIQUE_ID_IN_MSG = "XDSRepositoryDuplicateUniqueIdInMessage";
    public static final String XDS_ERR_DUPLICATE_UNIQUE_ID_IN_REGISTRY = "XDSDuplicateUniqueIdInRegistry";
    public static final String XDS_ERR_NON_IDENTICAL_HASH = "XDSNonIdenticalHash";
    public static final String XDS_ERR_REGISTRY_BUSY = "XDSRegistryBusy";
    public static final String XDS_ERR_REPOSITORY_BUSY = "XDSRepositoryBusy";
    public static final String XDS_ERR_REGISTRY_OUT_OF_RESOURCES = "XDSRegistryOutOfResources";
    public static final String XDS_ERR_REPOSITORY_OUT_OF_RESOURCES = "XDSRepositoryOutOfResources";
    public static final String XDS_ERR_REGISTRY_METADATA_ERROR = "XDSRegistryMetadataError";
    public static final String XDS_ERR_REPOSITORY_METADATA_ERROR = "XDSRepositoryMetadataError";
    public static final String XDS_ERR_TOO_MANY_RESULTS = "XDSTooManyResults";
    public static final String XDS_ERR_EXTRA_METADATA_NOT_SAVED = "XDSExtraMetadataNotSaved";
    public static final String XDS_ERR_UNKNOWN_PATID = "XDSUnknownPatientId";
    public static final String XDS_ERR_PATID_DOESNOT_MATCH = "XDSPatientIdDoesNotMatch";
    public static final String XDS_ERR_UNKNOWN_STORED_QUERY_ID = "XDSUnknownStoredQuery";
    public static final String XDS_ERR_STORED_QUERY_MISSING_PARAM = "XDSStoredQueryMissingParam";
    public static final String XDS_ERR_STORED_QUERY_PARAM_NUMBER = "XDSStoredQueryParamNumber";
    public static final String XDS_ERR_REGISTRY_DEPRECATED_DOC_ERROR = "XDSRegistryDeprecatedDocumentError";
    public static final String XDS_ERR_UNKNOWN_REPOSITORY_ID = "XDSUnknownRepositoryId";
    public static final String XDS_ERR_QRY_DOCUMENT_UNIQUE_IDE_ID_ERROR = "XDSDocumentUniqueIdError";
    public static final String XDS_ERR_RESULT_NOT_SINGLE_PATIENT = "XDSResultNotSinglePatient";
    public static final String XDS_ERR_PARTIAL_FOLDER_CONTENT_NOT_PROCESSED = "PartialFolderContentNotProcessed";
    public static final String XDS_ERR_PARTIAL_REPLACE_CONTENT_NOT_PROCESSED = "PartialReplaceContentNotProcessed";
    public static final String XDS_ERR_UNKNOWN_COMMUNITY = "XDSUnknownCommunity";
    public static final String XDS_ERR_MISSING_HOME_COMMUNITY_ID = "XDSMissingHomeCommunityId";
    public static final String XDS_ERR_UNAVAILABLE_COMMUNITY = "XDSUnavailableCommunity";

    public static final String XDS_ERR_SEVERITY_WARNING = "urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:Warning";
    public static final String XDS_ERR_SEVERITY_ERROR = "urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:Error";

    //Errors not defined in IHE
    public static final String XDS_ERR_MISSING_REGISTRY_PACKAGE = "RegistryPackage missing";

    //?? referenced in ITI_TF_Rev8-0_Vol3: 4.1.11 XDS Registry Adaptor:
    //    The error XDSReplaceFailed shall be thrown if this object is not contained in the registry or has status other than Approved.
    public static final String XDS_ERR_REPLACE_FAILED = "XDSReplaceFailed";
    
    private String errorCode, location;
    private String severity = XDS_ERR_SEVERITY_ERROR;

    public XDSException( String errorCode, String msg, Throwable t) {
        super(msg, t);
        this.errorCode = errorCode;
    }
    public String getErrorCode() {
        return errorCode;
    }
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
    public String getSeverity() {
        return severity;
    }
    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String toString() {
        return "["+errorCode+"]:"+getMessage();
    }

}
