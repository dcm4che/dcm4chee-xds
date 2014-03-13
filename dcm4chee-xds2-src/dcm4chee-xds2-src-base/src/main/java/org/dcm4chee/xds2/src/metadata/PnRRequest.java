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
import org.dcm4chee.xds2.common.code.Code;
import org.dcm4chee.xds2.infoset.ihe.ObjectFactory;
import org.dcm4chee.xds2.infoset.ihe.ProvideAndRegisterDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectListType;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.src.metadata.exception.MetadataConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide and Register Document Set Builder.
 * 
 * @author franz.willer@agfa.com
 *
 */
public class PnRRequest {

    protected static final String SUBMISSION_SET_ID = "SubmissionSet01";
    public static final Logger log = LoggerFactory.getLogger(PnRRequest.class);
    protected static ObjectFactory iheFactory = new ObjectFactory();
    
    private ProvideAndRegisterDocumentSetRequestType provideAndRegisterDocumentSetRequest;
    private String submissionSetUID;
    private String patientID;
    private SlotType1 srcPatIDSlot;
    private SlotType1 srcPatInfoSlot;
    private RegistryObjectListType registryObjectList;
    private RegistryPackageType submissionSet;

    private Code contentTypeCode;
    private Date submissionTime;

    private List<Author> authors = new ArrayList<Author>();
    private String title, comments;
    private List<String> intendedRecipient;

    private List<DocumentEntry> documents = new ArrayList<DocumentEntry>();
    private List<XDSFolder> folders = new ArrayList<XDSFolder>();
    private List<XDSAssociation> associations = new ArrayList<XDSAssociation>();
    
    private int docCounter = 1;
    private int folderCounter = 1;
    private int idCounter = 1;
    
    public PnRRequest(String sourceID, String submissionSetUID, String patID, String srcPatID) {
        if (sourceID == null || submissionSetUID == null || patID == null || srcPatID == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Parameter values must not be null! (");
            if (sourceID == null) sb.append("sourceID,");
            if (submissionSetUID == null) sb.append("submissionSetUID,");
            if (patID == null) sb.append("patID,");
            if (srcPatID == null) sb.append("srcPatID,");
            sb.setCharAt(sb.length()-1, ')');
            throw new IllegalArgumentException(sb.toString());
        }
        this.submissionSetUID = submissionSetUID;
        patientID = patID;
        srcPatIDSlot = Util.createSlot(XDSConstants.SLOT_NAME_SOURCE_PATIENT_ID, null, srcPatID);
        provideAndRegisterDocumentSetRequest = iheFactory.createProvideAndRegisterDocumentSetRequestType();
        SubmitObjectsRequest sor = Util.rimFactory.createSubmitObjectsRequest();
        registryObjectList = Util.rimFactory.createRegistryObjectListType();
        sor.setRegistryObjectList(registryObjectList);
        provideAndRegisterDocumentSetRequest.setSubmitObjectsRequest(sor);
        submissionSet = Util.rimFactory.createRegistryPackageType();
        submissionSet.setId(SUBMISSION_SET_ID);
        submissionSet.getExternalIdentifier().add(Util.createExternalIdentifier(nextID(), XDSConstants.UUID_XDSSubmissionSet_uniqueId, 
                SUBMISSION_SET_ID, submissionSetUID, "XDSSubmissionSet.uniqueId"));
        submissionSet.getExternalIdentifier().add(Util.createExternalIdentifier(nextID(), XDSConstants.UUID_XDSSubmissionSet_patientId, 
                SUBMISSION_SET_ID, patID, "XDSSubmissionSet.patientId"));
        submissionSet.getExternalIdentifier().add(Util.createExternalIdentifier(nextID(), XDSConstants.UUID_XDSSubmissionSet_sourceId, 
                SUBMISSION_SET_ID, sourceID, "XDSSubmissionSet.sourceId"));
        registryObjectList.getIdentifiable().add(Util.rimFactory.createRegistryPackage(submissionSet));
        submissionSet.getClassification().add(Util.createClassification(nextID(), XDSConstants.UUID_XDSSubmissionSet, SUBMISSION_SET_ID));
    }

    public PnRRequest(String sourceID, String submissionSetUID, String patID, String srcPatID, String patName, String sex, 
            String birthdate, String address) {
        this(sourceID, submissionSetUID, patID, srcPatID);
        if (patName != null) {
            srcPatInfoSlot = Util.createSlot(XDSConstants.SLOT_NAME_SOURCE_PATIENT_INFO, null, 
                    "PID-3|"+srcPatID, "PID-5|"+patName);
            List<String> values = srcPatInfoSlot.getValueList().getValue();
            addValue(values, birthdate, "PID-7|");
            addValue(values, sex, "PID-8|");
            addValue(values, address, "PID-11|");
        }
    }
    
    protected ProvideAndRegisterDocumentSetRequestType getProvideAndRegisterDocumentSetRequest() {
        return provideAndRegisterDocumentSetRequest;
    }

    public String nextDocumentID() {
        return formatTwoDigits("Document", docCounter++);
    }
    public String nextFolderID() {
        return formatTwoDigits("Folder", folderCounter++);
    }
    public String nextID() {
        return "id_"+idCounter++;
    }
    private String formatTwoDigits(String prefix, int i) {
        if ( i < 10) {
            return prefix+"0"+i;
        } else {
            return prefix+i;
        }
    }
    
    public DocumentEntry addDocumentEntry(String docUID, byte[] data, String mime) {
        DocumentEntry doc = new DocumentEntry(this, docUID, data, mime);
        documents.add(doc);
        return doc;
    }
    public void removeDocumentEntry(DocumentEntry doc) {
        documents.remove(doc);
        doc.removeFromRequest();
    }
    public List<DocumentEntry> getDocumentEntries() {
        return documents;
    }

    public XDSFolder addFolder(String folderUID) {
        XDSFolder folder = new XDSFolder(this, folderUID);
        folders.add(folder);
        return folder;
    }
    public void removeFolder(XDSFolder folder) {
        folders.remove(folder);
        folder.removeFromRequest();
    }
    public List<XDSFolder> getFolders() {
        return folders;
    }
    
    public XDSAssociation addAssociation(String src, String target, String type) {
        XDSAssociation assoc = new XDSAssociation(nextID(), src, target, type);
        associations.add(assoc);
        return assoc;
    }
    public void removeAssociation(XDSAssociation assoc) {
        associations.remove(assoc);
    }
    
    public String getSubmissionSetUID() {
        return submissionSetUID;
    }

    public String getPatientID() {
        return patientID;
    }
    //Required SubmissionSet attributes
    public Code getContentTypeCode() {
        return contentTypeCode;
    }

    public void setContentTypeCode(Code contentTypeCode) {
        this.contentTypeCode = contentTypeCode;
    }

    public Date getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(Date submissionTime) {
        this.submissionTime = submissionTime;
    }

    //R2 SubmissionSet attributes:
    public Author addAuthor() {
        Author author = Author.newSubmissionSetAuthor(nextID(), submissionSet);
        authors.add(author);
        return author;
    }
    public void removeAuthor(Author author) {
        authors.remove(author);
        Util.removeClassification(submissionSet, author.getID());
    }
    public List<Author> getAuthors() {
        return authors;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public List<String> getIntendedRecipient() {
        return intendedRecipient;
    }

    /**
     * Represents the organization(s) or person(s) for whom the
     * Submission set is intended. If present, shall have one or more
     * values. Each slot value shall include at least one of the
     * organization, person, or telecommunications address fields
     * described below. Example below shows two doctors from the
     * same organization, another doctor without precision of the
     * organization, another organization without the precision of the
     * person, and just a telecommunications address. The receipt of this
     * attribute in any transaction will not cause the request to be
     * rejected but the attribute may be ignored.
     * Format: XON/XCN
     * e.g.: 
     * 1) Some Hospital^^^^^^^^^1.2.3.4.5.6.7.8.9.1789.45|^Wel^Marcus^^^Dr^MD|^^Internet^mwel@healthcare.example.org
     * 2) Some Hospital^^^^^^^^^1.2.3.4.5.6.7.8.9.1789.45|^Al^Peter^^^Dr^MD
     * 3) |12345^John^Smith^^^Dr^MD
     * 4) Main Hospital^^^^^^^^^1.2.3.4.5.6.7.8.9.1789.2364
     * 
     * @param recipient
     */
    public void addIntendedRecipient(String recipient) {
        if (intendedRecipient == null)
            intendedRecipient = new ArrayList<String>();
        intendedRecipient.add(recipient);
    }

    public RegistryObjectListType getRegistryObjectList() {
        return registryObjectList;
    }

    public ProvideAndRegisterDocumentSetRequestType createInfoset() throws MetadataConfigurationException {
        checkMetadata();
        Util.addCode(nextID(), submissionSet, XDSConstants.UUID_XDSSubmissionSet_contentTypeCode, contentTypeCode);
        Util.addSlot(submissionSet, XDSConstants.SLOT_NAME_SUBMISSION_TIME, null, Util.toUTCTimeString(submissionTime));
        if (title != null)
            Util.setName(submissionSet, title);
        if (comments != null)
            Util.setDescription(submissionSet, comments);
        if (intendedRecipient != null)
            Util.setSlot(submissionSet, XDSConstants.SLOT_NAME_INTENDED_RECIPIENT, null, intendedRecipient);
        
        for (int i = 0, len = documents.size() ; i < len ; i++) {
            documents.get(i).buildMetadata(false);
        }
        for (int i = 0, len = folders.size() ; i < len ; i++) {
            folders.get(i).buildMetadata(false);
        }
        List<JAXBElement<? extends IdentifiableType>> list = this.registryObjectList.getIdentifiable();
        for (int i = 0, len = associations.size() ; i < len ; i++) {
            XDSAssociation assoc = associations.get(i);
            String targetID = assoc.getTargetObject();
            String sourceID = assoc.getSourceObject();
            if(isSubmissionSet(sourceID) && isDocumentID(targetID)) {
                Util.addAssociation(list, assoc);
            } else {
                Util.addNonDocumentAssociation(list, assoc);
            }
        }
        return this.provideAndRegisterDocumentSetRequest;
    }
    
    private boolean isSubmissionSet(String sourceID) {
        return sourceID!=null && sourceID.equals(submissionSet.getId());
    }

    private boolean isDocumentID(String targetID) {
        for(DocumentEntry docEntry : documents) {
            if(docEntry.getID() != null && docEntry.getID().equals(targetID)) {
                return true;
            }
        }
        return false;
    }

    public void checkMetadata() throws MetadataConfigurationException {
        MetadataConfigurationException x = new MetadataConfigurationException("Provide and Register Document Set Metadata error:");
        checkSubmissionSet(x);
        for (int i = 0, len = documents.size() ; i < len ; i++) {
            try {
                documents.get(i).checkMetadata();
            } catch (MetadataConfigurationException x1) {
                x.addSubError(x1);
            }
        }
        for (int i = 0, len = folders.size() ; i < len ; i++) {
            try {
                folders.get(i).checkMetadata();
            } catch (MetadataConfigurationException x1) {
                x.addSubError(x1);
            }
        }
        if (x.getSubErrors() != null)
            throw x;
    }
    
    private void checkSubmissionSet(MetadataConfigurationException x) {
        List<String> missingAttributes = new ArrayList<String>();
        if (contentTypeCode == null) 
            missingAttributes.add("SubmissionSet.contentTypeCode");
        if (submissionTime == null) 
            missingAttributes.add("SubmissionSet.submissionTime");

        if (missingAttributes.size() > 0)
            x.addSubError(new MetadataConfigurationException("SubmissionSet:", missingAttributes));

    }

    protected SlotType1 getSrcPatIDSlot() {
        return srcPatIDSlot;
    }

    protected SlotType1 getSrcPatInfo() {
        return srcPatInfoSlot;
    }
    
    @Override
    public String toString() {
        return "ProvideAndRegisterDocumentSet.b (PatID:"+patientID+", SubmissionSetUID:"+submissionSetUID+
        " docs:"+this.documents.size()+" folders:"+folders.size()+")";
    }

    private void addValue(List<String> values, String val, String prefix) {
        if (val != null) {
            values.add(prefix+val);
        }
    }
}