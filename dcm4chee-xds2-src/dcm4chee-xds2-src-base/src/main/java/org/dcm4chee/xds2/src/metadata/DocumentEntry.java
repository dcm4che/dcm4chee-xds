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
package org.dcm4chee.xds2.src.metadata;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.ihe.ProvideAndRegisterDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.ProvideAndRegisterDocumentSetRequestType.Document;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.src.metadata.exception.MetadataConfigurationException;

/**
 * Document Entry Builder.
 * 
 * @author franz.willer@agfa.com
 *
 */
public class DocumentEntry {

    public static final String XDS_DOCUMENT_ENTRY_PATIENT_ID = "XDSDocumentEntry.patientId";
    public static final String XDS_DOCUMENT_ENTRY_UNIQUE_ID = "XDSDocumentEntry.uniqueId";
    
    private PnRRequest pnrRequest;
    private Document doc;
    private ExtrinsicObjectType metadata;
    private XDSAssociation assoc;
    
    private Code classCode, formatCode, healthcareFacilityTypeCode, practiceSettingCode, typeCode;
    private List<Code> confidentialityCodes = new ArrayList<Code>();
    private List<Code> eventCodeList = new ArrayList<Code>();
    
    private String id, uniqueID, languageCode;
    private Date creationTime, serviceStartTime, serviceStopTime;
    
    private List<Author> authors = new ArrayList<Author>();
    
    public DocumentEntry(PnRRequest pnrReq, String uniqueID, byte[] content, String mime) {
        this.pnrRequest = pnrReq;
        doc = PnRRequest.iheFactory.createProvideAndRegisterDocumentSetRequestTypeDocument();
        ProvideAndRegisterDocumentSetRequestType pnr = pnrRequest.getProvideAndRegisterDocumentSetRequest();
        pnr.getDocument().add(doc);
        id = pnrReq.nextDocumentID();
        doc.setId(id);
        doc.setValue(content);
        metadata = Util.rimFactory.createExtrinsicObjectType();
        metadata.setObjectType(XDSConstants.UUID_XDSDocumentEntry);
        metadata.setId(id);
        metadata.setMimeType(mime);
        List<JAXBElement<? extends IdentifiableType>> list = pnrRequest.getRegistryObjectList().getIdentifiable();
        list.add(Util.rimFactory.createExtrinsicObject(metadata));
        metadata.getExternalIdentifier().add(Util.createExternalIdentifier(pnrRequest.nextID(), XDSConstants.UUID_XDSDocumentEntry_patientId, 
                id, pnrRequest.getPatientID(), XDS_DOCUMENT_ENTRY_PATIENT_ID));
        metadata.getSlot().add(pnrRequest.getSrcPatIDSlot());
        if (pnrRequest.getSrcPatInfo() != null) {
            metadata.getSlot().add(pnrRequest.getSrcPatInfo());
        }
        metadata.getExternalIdentifier().add(Util.createExternalIdentifier(pnrRequest.nextID(), XDSConstants.UUID_XDSDocumentEntry_uniqueId, 
                id, uniqueID, XDS_DOCUMENT_ENTRY_UNIQUE_ID));
        this.uniqueID = uniqueID;
        assoc = pnrReq.addAssociation(PnRRequest.SUBMISSION_SET_ID, id, XDSConstants.HAS_MEMBER);
    }

    public void removeFromRequest() {
        pnrRequest.getProvideAndRegisterDocumentSetRequest().getDocument().remove(doc);
        List<JAXBElement<? extends IdentifiableType>> list = pnrRequest.getRegistryObjectList().getIdentifiable();
        for (int i = 0, len = list.size() ; i < len ; i++) {
            if (list.get(i).getValue().getId().equals(id)) {
                list.remove(i);
            }
        }
        pnrRequest.removeAssociation(assoc);
    }

    //R (required) attributes
    public void setClassCode(Code code) {
        classCode = code;
    }
    public Code getClassCode() {
        return classCode;
    }

    public void addConfidentialityCode(Code code) {
        confidentialityCodes.add(code);
    }
    public List<Code> getConfidentialityCodes() {
        return confidentialityCodes;
    }
    
    public void addEventCode(Code code) {
        eventCodeList.add(code);
    }
    public List<Code> getEventCodeList() {
        return eventCodeList;
    }


    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public Code getFormatCode() {
        return formatCode;
    }

    public void setFormatCode(Code formatCode) {
        this.formatCode = formatCode;
    }

    public Code getHealthcareFacilityTypeCode() {
        return healthcareFacilityTypeCode;
    }

    public void setHealthcareFacilityTypeCode(Code healthcareFacilityTypeCode) {
        this.healthcareFacilityTypeCode = healthcareFacilityTypeCode;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public Code getPracticeSettingCode() {
        return practiceSettingCode;
    }

    public void setPracticeSettingCode(Code practiceSettingCode) {
        this.practiceSettingCode = practiceSettingCode;
    }

    public Code getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(Code typeCode) {
        this.typeCode = typeCode;
    }

    public String getID() {
        return id;
    }
    public String getUniqueID() {
        return uniqueID;
    }

    //R2 DocumentEntry attributes:
    public Author addAuthor() {
        Author author = Author.newDocumentEntryAuthor(pnrRequest.nextID(), metadata);
        authors.add(author);
        return author;
    }
    public void removeAuthor(Author author) {
        authors.remove(author);
        Util.removeClassification(metadata, author.getID());
    }
    public List<Author> getAuthors() {
        return authors;
    }
    
    public Date getServiceStartTime() {
        return serviceStartTime;
    }

    public void setServiceStartTime(Date serviceStartTime) {
        this.serviceStartTime = serviceStartTime;
    }

    public Date getServiceStopTime() {
        return serviceStopTime;
    }

    public void setServiceStopTime(Date serviceStopTime) {
        this.serviceStopTime = serviceStopTime;
    }

    public void buildMetadata(boolean check) throws MetadataConfigurationException {
        if (check)
            checkMetadata();
        
        Util.addCode(pnrRequest.nextID(), metadata, XDSConstants.UUID_XDSDocumentEntry_classCode, classCode);
        Util.addCode(pnrRequest.nextID(), metadata, XDSConstants.UUID_XDSDocumentEntry_formatCode, formatCode);
        Util.addCode(pnrRequest.nextID(), metadata, XDSConstants.UUID_XDSDocumentEntry_healthCareFacilityTypeCode, healthcareFacilityTypeCode);
        Util.addCode(pnrRequest.nextID(), metadata, XDSConstants.UUID_XDSDocumentEntry_practiceSettingCode, practiceSettingCode);
        Util.addCode(pnrRequest.nextID(), metadata, XDSConstants.UUID_XDSDocumentEntry_typeCode, typeCode);

        Util.addCodes(pnrRequest.nextID(), metadata, XDSConstants.UUID_XDSDocumentEntry_confidentialityCode, confidentialityCodes);
        Util.addCodes(pnrRequest.nextID(), metadata, XDSConstants.UUID_XDSDocumentEntry_eventCodeList, eventCodeList);
        Util.addSlot(metadata, XDSConstants.SLOT_NAME_CREATION_TIME, null, Util.toTimeString(creationTime));
        Util.addSlot(metadata, XDSConstants.SLOT_NAME_LANGUAGE_CODE, null, languageCode);

        Util.addSlot(metadata, XDSConstants.SLOT_NAME_SERVICE_START_TIME, null, Util.toTimeString(serviceStartTime));
        Util.addSlot(metadata, XDSConstants.SLOT_NAME_SERVICE_STOP_TIME, null, Util.toTimeString(serviceStopTime));
}

    public void checkMetadata() throws MetadataConfigurationException {
        List<String> missingAttributes = new ArrayList<String>();
        if (uniqueID == null) 
            missingAttributes.add("DocumentEntry.uniqueID");
        if (classCode == null) 
            missingAttributes.add("DocumentEntry.classCode");
        if (formatCode == null) 
            missingAttributes.add("DocumentEntry.formatCode");
        if (healthcareFacilityTypeCode == null) 
            missingAttributes.add("DocumentEntry.healthcareFacilityTypeCode");
        if (languageCode == null) 
            missingAttributes.add("DocumentEntry.languageCode");
        if (practiceSettingCode == null) 
            missingAttributes.add("DocumentEntry.practiceSettingCode");
        if (typeCode == null) 
            missingAttributes.add("DocumentEntry.typeCode");

        if (confidentialityCodes.isEmpty())
            missingAttributes.add("DocumentEntry.confidentialityCodes");
        if (eventCodeList.isEmpty())
            missingAttributes.add("DocumentEntry.eventCodeList");

        if (creationTime == null)
            missingAttributes.add("DocumentEntry.creationTime");
        
        if (missingAttributes.size() > 0)
            throw new MetadataConfigurationException("DocumentEntry uniqueID="+this.uniqueID, missingAttributes);
        
    }
}