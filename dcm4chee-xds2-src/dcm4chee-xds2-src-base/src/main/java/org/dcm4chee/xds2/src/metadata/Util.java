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
package org.dcm4chee.xds2.src.metadata;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.code.Code;
import org.dcm4chee.xds2.infoset.rim.AssociationType1;
import org.dcm4chee.xds2.infoset.rim.ClassificationType;
import org.dcm4chee.xds2.infoset.rim.ExternalIdentifierType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.InternationalStringType;
import org.dcm4chee.xds2.infoset.rim.LocalizedStringType;
import org.dcm4chee.xds2.infoset.rim.ObjectFactory;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectType;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.infoset.rim.ValueListType;

public class Util {

    private static final String OBJECT_TYPE_CLASSIFICATION = "urn:oasis:names:tc:ebxmlregrep:ObjectType:RegistryObject:Classification";
    protected static ObjectFactory rimFactory = new ObjectFactory();
    
    private static SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
    
    public static ExternalIdentifierType createExternalIdentifier(String id,
            String scheme, String registryObject, String value, String name) {
        ExternalIdentifierType ei = rimFactory.createExternalIdentifierType();
        ei.setId(id);
        ei.setIdentificationScheme(scheme);
        ei.setObjectType("urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:ExternalIdentifier");
        ei.setRegistryObject(registryObject);
        ei.setValue(value);
        InternationalStringType is = rimFactory.createInternationalStringType();
        LocalizedStringType v = rimFactory.createLocalizedStringType();
        v.setValue(name);
        is.getLocalizedString().add(v);
        ei.setName(is);
        return ei;
    }
    
    public static ClassificationType createClassification(String id, String classificationScheme, String classifiedObject, String nodeRepresentation) {
        ClassificationType cl = rimFactory.createClassificationType();
        cl.setId(id);
        cl.setClassificationScheme(classificationScheme);
        cl.setClassifiedObject(classifiedObject);
        cl.setObjectType(OBJECT_TYPE_CLASSIFICATION);
        cl.setNodeRepresentation(nodeRepresentation);
        return cl;
    }
    
    public static ClassificationType createClassification(String id, String classificationNode, String classifiedObject) {
        ClassificationType cl = rimFactory.createClassificationType();
        cl.setId(id);
        cl.setClassificationNode(classificationNode);
        cl.setClassifiedObject(classifiedObject);
        cl.setObjectType(OBJECT_TYPE_CLASSIFICATION);
        return cl;
    }
    public static ClassificationType removeClassification(RegistryObjectType ro, String id) {
        List<ClassificationType> list = ro.getClassification();
        for (int i = 0, len = list.size() ; i < len ; i++) {
            if (list.get(i).getId().equals(id)) {
                return list.remove(i);
            }
        }
        return null;
    }

    public static void addCode(String id, RegistryObjectType ro, String type, Code code) {
        ro.getClassification().add(createCodeClassification(id, ro.getId(), type, code));
    }
    public static void addCodes(String id, RegistryObjectType ro, String type, List<Code> codes) {
        String objID = ro.getId();
        for (int i = 0, len = codes.size() ; i < len ; i++) {
            ro.getClassification().add(createCodeClassification(id, objID, type, codes.get(i)));
        }
    }
    public static ClassificationType createCodeClassification(String id, String classifiedObjectID, String type, Code code) {
        ClassificationType cl = createClassification(id, type, classifiedObjectID, code.getValue());
        LocalizedStringType lst = rimFactory.createLocalizedStringType();
        lst.setValue(code.getMeaning());
        InternationalStringType is = rimFactory.createInternationalStringType();
        is.getLocalizedString().add(lst);
        cl.setName(is);
        cl.getSlot().add(createSlot("codingScheme", null, code.getDesignator()));
        return cl;
    }

    public static SlotType1 createSlot(String name, String type, String...values) {
        SlotType1 slot = Util.rimFactory.createSlotType1();
        slot.setName(name);
        slot.setSlotType(type);
        ValueListType valueList = rimFactory.createValueListType();
        slot.setValueList(valueList);
        if (values != null) {
            List<String> l = valueList.getValue();
            for (int i = 0 ; i < values.length ; i++) {
                l.add(values[i]);
            }
        }
        return slot;
    }

    public static SlotType1 addSlot(RegistryObjectType ro, String name, String type, String...values) {
        SlotType1 slot = createSlot(name, type, values);
        ro.getSlot().add(slot);
        return slot;
    }
    public static SlotType1 setSlot(RegistryObjectType ro, String name, String type, Collection<String> values) {
        SlotType1 slot = createSlot(name, type);
        if (values != null && values.size() > 0)
            slot.getValueList().getValue().addAll(values);
        List<SlotType1> slots = ro.getSlot();
        for (int i = 0, len = slots.size() ; i < len ; i++) {
            if (slots.get(i).getName().equals(name)) {
                slots.remove(i);
                break;
            }
        }
        slots.add(slot);
        return slot;
    }
    
    public static SlotType1 removeSlot(RegistryObjectType ro, String name) {
        List<SlotType1> slots = ro.getSlot();
        for (int i = 0, len = slots.size() ; i < len ; i++) {
            if (name.equals(slots.get(i).getName())) {
                return slots.remove(i);
            }
        }
        return null;
    }
    
    public static SlotType1 addOrOverwriteSlot(RegistryObjectType ro, SlotType1 slot) {
        List<SlotType1> slots = ro.getSlot();
        String name = slot.getName();
        SlotType1 oldSlot = null;
        for (int i = 0, len = slots.size() ; i < len ; i++) {
            if (slots.get(i).getName().equals(name)) {
                oldSlot = slots.remove(i);
                break;
            }
        }
        slots.add(slot);
        return oldSlot;
    }
    
    public static void addAssociation(List<JAXBElement<? extends IdentifiableType>> list, XDSAssociation xdsAssoc) {
        list.add(rimFactory.createAssociation(createAssociation(xdsAssoc)));
    }
    public static AssociationType1 createAssociation(XDSAssociation xdsAssoc) {
        AssociationType1 assoc = rimFactory.createAssociationType1();
        assoc.setId(xdsAssoc.getID());
        assoc.setSourceObject(xdsAssoc.getSourceObject());
        assoc.setTargetObject(xdsAssoc.getTargetObject());
        assoc.setAssociationType(xdsAssoc.getAssociationType());
        assoc.setObjectType("urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:Association");
        assoc.getSlot().add(Util.createSlot(XDSConstants.SLOT_NAME_SUBMISSIONSET_STATUS, null, xdsAssoc.getSubmissionSetStatus()));
        return assoc;
    }

    public static List<ClassificationType> getClassifications(RegistryObjectType ro, String scheme) {
        List<ClassificationType> result = new ArrayList<ClassificationType>();
        List<ClassificationType> classifications = ro.getClassification();
        for (int i = 0, len = classifications.size() ; i < len ; i++) {
            if (classifications.get(i).getClassificationScheme().equals(scheme)) {
                result.add(classifications.get(i));
            }
        }
        return result;
    }
    
    public static void setDescription(RegistryObjectType ro, String value) {
        LocalizedStringType lst = rimFactory.createLocalizedStringType();
        lst.setValue(value);
        InternationalStringType is = rimFactory.createInternationalStringType();
        is.getLocalizedString().add(lst);
        ro.setDescription(is);
    }
    public static void setName(RegistryObjectType ro, String value) {
        LocalizedStringType lst = rimFactory.createLocalizedStringType();
        lst.setValue(value);
        InternationalStringType is = rimFactory.createInternationalStringType();
        is.getLocalizedString().add(lst);
        ro.setName(is);
    }
    
    public static String toTimeString(Date d) {
        return d == null ? null : df.format(d);
    }
    public static Date toDate(String d) throws ParseException {
        return d == null ? null : "now()".equals(d) ? new Date() : df.parse(d);
    }
}
