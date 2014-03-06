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
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.code.Code;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.src.metadata.exception.MetadataConfigurationException;

/**
 * Folder Metadata Builder.
 * 
 * @author franz.willer@agfa.com
 *
 */
public class XDSFolder {

    public static final String XDS_FOLDER_PATIENT_ID = "XDSFolder.patientId";
    public static final String XDS_FOLDER_UNIQUE_ID = "XDSFolder.uniqueId";
    
    private PnRRequest pnrRequest;
    private RegistryPackageType metadata;
    private XDSAssociation assoc;
    
    private List<Code> codeList = new ArrayList<Code>();
    
    private String id, uniqueID;
    
    private List<Author> authors = new ArrayList<Author>();
    
    private String title, comments;
    
    public XDSFolder(PnRRequest pnrReq, String uniqueID) {
        this.pnrRequest = pnrReq;
        id = pnrReq.nextFolderID();
        metadata = Util.rimFactory.createRegistryPackageType();
        metadata.setId(id);
        List<JAXBElement<? extends IdentifiableType>> list = pnrRequest.getRegistryObjectList().getIdentifiable();
        list.add(Util.rimFactory.createRegistryPackage(metadata));
        metadata.getExternalIdentifier().add(Util.createExternalIdentifier(pnrRequest.nextID(), XDSConstants.UUID_XDSFolder_patientId, 
                id, pnrRequest.getPatientID(), XDS_FOLDER_PATIENT_ID));
        metadata.getExternalIdentifier().add(Util.createExternalIdentifier(pnrRequest.nextID(), XDSConstants.UUID_XDSFolder_uniqueId, 
                id, uniqueID, XDS_FOLDER_UNIQUE_ID));
        metadata.getClassification().add(Util.createClassification(pnrReq.nextID(), XDSConstants.UUID_XDSFolder, id));
        this.uniqueID = uniqueID;
        assoc = pnrReq.addAssociation(PnRRequest.SUBMISSION_SET_ID, id, XDSConstants.HAS_MEMBER);
    }

    public void removeFromRequest() {
        List<JAXBElement<? extends IdentifiableType>> list = pnrRequest.getRegistryObjectList().getIdentifiable();
        for (int i = 0, len = list.size() ; i < len ; i++) {
            if (list.get(i).getValue().getId().equals(id)) {
                list.remove(i);
            }
        }
        pnrRequest.removeAssociation(assoc);
    }

    //R (required) attributes
    public void addCodeList(Code code) {
        codeList.add(code);
    }
    public List<Code> getCodeList() {
        return codeList;
    }

    public String getID() {
        return id;
    }
    public String getUniqueID() {
        return uniqueID;
    }

    //Optional Attributes
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

    public void buildMetadata(boolean check) throws MetadataConfigurationException {
        if (check)
            checkMetadata();
        
        Util.addCodes(pnrRequest.nextID(), metadata, XDSConstants.UUID_XDSFolder_codeList, codeList);
        if (comments != null) {
            Util.setDescription(metadata, comments);
        }
        if (title != null) {
            Util.setName(metadata, title);
        }
}

    public void checkMetadata() throws MetadataConfigurationException {
        List<String> missingAttributes = new ArrayList<String>();
        if (uniqueID == null) 
            missingAttributes.add("XDSFolder.uniqueID");
        if (codeList.isEmpty())
            missingAttributes.add("XDSFolder.codeList");

        if (missingAttributes.size() > 0)
            throw new MetadataConfigurationException("XDSFolder uniqueID="+this.uniqueID, missingAttributes);
        
    }
}