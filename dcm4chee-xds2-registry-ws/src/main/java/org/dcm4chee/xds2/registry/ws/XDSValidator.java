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

package org.dcm4chee.xds2.registry.ws;

import java.util.ArrayList;
import java.util.List;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.infoset.rim.ClassificationType;
import org.dcm4chee.xds2.infoset.rim.ExternalIdentifierType;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectType;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.persistence.Association;
import org.dcm4chee.xds2.persistence.Identifiable;
import org.dcm4chee.xds2.persistence.RegistryObject;
import org.dcm4chee.xds2.persistence.Slot;
import org.dcm4chee.xds2.persistence.XADPatient;
import org.dcm4chee.xds2.persistence.XDSDocumentEntry;
import org.dcm4chee.xds2.persistence.XDSObject;
import org.eclipse.jetty.util.log.Log;

/**
 * XDS Validator
 * 
 * Maps local ID's to UUID's
 * 
 * @author franz.willer@gmail.com
 *
 */
public class XDSValidator {

    public static void checkSamePatient(List<? extends Identifiable> objects) throws XDSException {
        Identifiable obj, obj1 = null;
        XADPatient pat, pat1 = null;
        for (int i = 0,len=objects.size() ; i < len ; i++) {
            obj = objects.get(i);
            if (obj instanceof XDSObject) {
                pat = ((XDSObject) obj).getPatient();
                if (pat1 == null) {
                    pat1 = pat;
                    obj1 = obj;
                } else {
                    if (pat1.getPk() != pat.getPk()) 
                        throw new XDSException(XDSException.XDS_ERR_RESULT_NOT_SINGLE_PATIENT, 
                                "List contains objects of different patients! "+pat1+"("+obj1+
                                ") vs. "+pat+"("+obj+")", null);
                }
            }
        }
    }

    public static void checkSamePatient(Association assoc) throws XDSException {
        RegistryObject src = assoc.getSourceObject();
        if (src instanceof XDSObject) {
            RegistryObject dst = assoc.getTargetObject();
            if (dst instanceof XDSObject) {
                if (!((XDSObject)src).getPatient().equals(((XDSObject)dst).getPatient())) {
                	if (XDSConstants.RPLC.equals(assoc.getAssocType().getId())) {
                    	Log.info("RPLC: Replace with different Patient IDs! source:"+((XDSObject)src).getPatient()+
                    			"and target:"+((XDSObject)dst).getPatient());
                	} else {
                		throw new XDSException(XDSException.XDS_ERR_PATID_DOESNOT_MATCH, 
                            "Association id:"+assoc.getId()+
                            " source PID:"+((XDSObject)src).getPatient()+
                            " target PID:"+((XDSObject)dst).getPatient(), null);
                	}
                }
            }
        }
    }
    
    public static void checkAlreadyExists(String uniqueId, ExtrinsicObjectType eoType, XDSRegistryBean session) throws XDSException {
        List<XDSDocumentEntry> docs = session.getDocumentEntriesByUniqueId(uniqueId);
        if (docs.size() > 0) {
            List<String> hash1 = getSlotTypeValues(eoType.getSlot(), XDSConstants.SLOT_NAME_HASH);
            if (hash1 == null || hash1.isEmpty()) {
                throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                        "Slot 'hash' missing in DocumentEntry uniqueId:"+uniqueId, null);
            } else if (!hash1.get(0).equalsIgnoreCase(docs.get(0).getHash())) {
                throw new XDSException(XDSException.XDS_ERR_NON_IDENTICAL_HASH, 
                        "DocumentEntry uniqueId:"+uniqueId+
                        " already exists but has different hash value! hash:'"+hash1+
                        "' vs. '"+docs.get(0).getHash()+"'", null);
            }
            List<String> size1 = getSlotTypeValues(eoType.getSlot(), XDSConstants.SLOT_NAME_SIZE);
            if (size1 == null || size1.isEmpty()) {
                throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                        "Slot 'size' missing in DocumentEntry uniqueId:"+uniqueId, null);
            } else if (!size1.get(0).equals(docs.get(0).getSize())) {
                throw new XDSException(XDSException.XDS_ERR_NON_IDENTICAL_SIZE, 
                        "DocumentEntry uniqueId:"+uniqueId+
                        " already exists but has different size value! size:'"+size1+
                        "' vs. '"+docs.get(0).getSize()+"'", null);
            }
        }
    }

    public static void checkDocumentEntryMetadata(ExtrinsicObjectType eoType) throws XDSException {
        if (eoType.getId() == null || eoType.getId().trim().length() < 2) {
            throw new XDSException(XDSException.XDS_ERR_REGISTRY_METADATA_ERROR, 
                    "DocumentEntry: Missing entryUUID attribute!", null);
        }
        if (eoType.getMimeType() == null || eoType.getMimeType().trim().length() < 2) {
            throw new XDSException(XDSException.XDS_ERR_REGISTRY_METADATA_ERROR, 
                    "DocumentEntry: Missing mimeType attribute! ExtrinsicObject:"+eoType.getId(), null);
        }
        
        checkSlots(eoType, toList(
                XDSConstants.SLOT_NAME_CREATION_TIME,
                XDSConstants.SLOT_NAME_REPOSITORY_UNIQUE_ID,
                XDSConstants.SLOT_NAME_SIZE,
                XDSConstants.SLOT_NAME_HASH,
                XDSConstants.SLOT_NAME_SOURCE_PATIENT_ID,
                XDSConstants.SLOT_NAME_LANGUAGE_CODE
                ));
        
        checkCodes(eoType, toList(
                XDSConstants.UUID_XDSDocumentEntry_classCode,
                XDSConstants.UUID_XDSDocumentEntry_confidentialityCode,
                XDSConstants.UUID_XDSDocumentEntry_formatCode,
                XDSConstants.UUID_XDSDocumentEntry_healthCareFacilityTypeCode,
                XDSConstants.UUID_XDSDocumentEntry_practiceSettingCode,
                XDSConstants.UUID_XDSDocumentEntry_typeCode
                ));

        checkExternalIdentifiers(eoType, toList(
                XDSConstants.UUID_XDSDocumentEntry_patientId,
                XDSConstants.UUID_XDSDocumentEntry_uniqueId
                ));
    }

    public static void checkSubmissionSetMetadata(RegistryPackageType rpType) throws XDSException {
        if (rpType.getId() == null || rpType.getId().trim().length() == 1) {
            throw new XDSException(XDSException.XDS_ERR_REGISTRY_METADATA_ERROR, 
                    "SubmissionSet: Missing entryUUID attribute!", null);
        }
        
        checkSlots(rpType, toList(XDSConstants.SLOT_NAME_SUBMISSION_TIME));
        
        checkCodes(rpType, toList(XDSConstants.UUID_XDSSubmissionSet_contentTypeCode));

        checkExternalIdentifiers(rpType, toList(
                XDSConstants.UUID_XDSSubmissionSet_patientId,
                XDSConstants.UUID_XDSSubmissionSet_uniqueId,
                XDSConstants.UUID_XDSSubmissionSet_sourceId
                ));
    }

    public static void checkFolderMetadata(RegistryPackageType rpType) throws XDSException {
        if (rpType.getId() == null || rpType.getId().trim().length() == 1) {
            throw new XDSException(XDSException.XDS_ERR_REGISTRY_METADATA_ERROR, 
                    "Folder: Missing entryUUID attribute!", null);
        }
        
        checkCodes(rpType, toList(XDSConstants.UUID_XDSFolder_codeList));

        checkExternalIdentifiers(rpType, toList(XDSConstants.UUID_XDSFolder_patientId,
                XDSConstants.UUID_XDSFolder_uniqueId));
    }

    private static void checkSlots(IdentifiableType obj, List<String> requiredSlotNames) throws XDSException {
        for (SlotType1 s : obj.getSlot()) {
            requiredSlotNames.remove(s.getName());
        }
        if (requiredSlotNames.size() > 0)
            throw new XDSException(XDSException.XDS_ERR_REGISTRY_METADATA_ERROR, 
                    "Missing attributes in '"+obj.getId()+"'!:"+requiredSlotNames, null);
    }    

    private static void checkCodes(RegistryObjectType roType, List<String> requiredCodes) throws XDSException {
        List<ClassificationType> clList = roType.getClassification();
        String objId = roType.getId();
        for (ClassificationType cl : clList) {
            if( objId.equals(cl.getClassifiedObject())) {
                if (requiredCodes.remove(cl.getClassificationScheme())) {
                    if (cl.getName() == null || cl.getName().getLocalizedString() == null ||
                            cl.getName().getLocalizedString().isEmpty()) {
                        throw new XDSException(XDSException.XDS_ERR_REGISTRY_METADATA_ERROR, 
                                "Missing code display name in code '"+cl.getNodeRepresentation()+
                                "(code classification:"+cl.getClassificationScheme(), null);
                    }
                }
            }
        }
        if (requiredCodes.size() > 0)
            throw new XDSException(XDSException.XDS_ERR_REGISTRY_METADATA_ERROR, 
                    "Missing code values in '"+objId+"'! codes:"+requiredCodes, null);
    }
    
    private static void checkExternalIdentifiers(RegistryObjectType roType,
            List<String> requiredExternalIdentifiers) throws XDSException {
        List<ExternalIdentifierType> eiList = roType.getExternalIdentifier();
        for (ExternalIdentifierType ei : eiList) {
            requiredExternalIdentifiers.remove(ei.getIdentificationScheme());
        }
        if (requiredExternalIdentifiers.size() > 0) 
            throw new XDSException(XDSException.XDS_ERR_REGISTRY_METADATA_ERROR, 
                    "Missing attributes in '"+roType.getId()+"'! codes:"+requiredExternalIdentifiers, null);
    }

    private static List<String> toList(String...strings) {
        List<String> l = new ArrayList<String>(strings.length);
        for (String s : strings) {
            l.add(s);
        }
        return l;
    }
    
    public static List<String> getSlotTypeValues(List<SlotType1> slots, String name) {
        if (slots != null) {
            for (int i=0, len=slots.size() ; i < len ; i++) {
                if (name.equals(slots.get(i).getName()) && slots.get(i).getValueList() != null) {
                    return slots.get(i).getValueList().getValue();
                }
            }
        }
        return null;
    }
    public static List<String> getSlotValues(List<Slot> slots, String name) {
        ArrayList<String> values = null;
        if (slots != null) {
            values = new ArrayList<String>();
            for (int i=0, len=slots.size() ; i < len ; i++) {
                if (name.equals(slots.get(i).getName())) {
                    values.add(slots.get(i).getValue());
                }
            }
        }
        return values;
    }

}
