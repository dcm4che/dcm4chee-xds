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
package org.dcm4chee.xds2.infoset.util;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.dcm4chee.xds2.infoset.ihe.ProvideAndRegisterDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.ProvideAndRegisterDocumentSetRequestType.Document;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.ClassificationType;
import org.dcm4chee.xds2.infoset.rim.ExternalIdentifierType;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.ObjectFactory;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectType;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.infoset.rim.ValueListType;

public class InfosetUtil {

    private static JAXBContext jaxbCtx;
    private static ObjectFactory objFac = new ObjectFactory();

    public static JAXBContext jaxbCtx() throws JAXBException {
        if (InfosetUtil.jaxbCtx == null)
            InfosetUtil.jaxbCtx = JAXBContext.newInstance(AdhocQueryRequest.class);
        return InfosetUtil.jaxbCtx;
    }
    
    public static String marshallObject(Object o, boolean indent) throws JAXBException {
        StringWriter sw = new StringWriter();
        Marshaller m = jaxbCtx().createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, indent);
        m.marshal(o, sw);
        return sw.toString();
    }

    public static RegistryPackageType getRegistryPackage(SubmitObjectsRequest sor, String classificationUUID) {
        List<JAXBElement<? extends IdentifiableType>> list = sor.getRegistryObjectList().getIdentifiable();
        String id = null;
        IdentifiableType identifiable;
        if ( classificationUUID != null ) {
            ClassificationType ct;
            for ( int i = 0, len = list.size() ; i < len ; i++) {
                identifiable = list.get(i).getValue();
                if ( identifiable instanceof ClassificationType) {
                    ct = (ClassificationType) identifiable;
                    if ( classificationUUID.equals( ct.getClassificationNode())) {
                        id = ct.getClassifiedObject();
                        break;
                    }
                }
            }
        }
        RegistryPackageType rp;
        for ( int i = 0, len = list.size() ; i < len ; i++ ) {
            identifiable = list.get(i).getValue();
            if ( identifiable instanceof RegistryPackageType) {
                rp = (RegistryPackageType) identifiable;
                if ( id == null || id.equals( rp.getId())) {
                    return rp;
                }
            }
        }
        return null;
    }

    public static List<ExtrinsicObjectType> getExtrinsicObjects(SubmitObjectsRequest so) {
        List<ExtrinsicObjectType> extrObjs = new ArrayList<ExtrinsicObjectType>();
        List<JAXBElement<? extends IdentifiableType>> list = so.getRegistryObjectList().getIdentifiable();
        IdentifiableType o;
        for ( int i = 0, len = list.size() ; i < len ; i++ ) {
            o = list.get(i).getValue();
            if ( o instanceof ExtrinsicObjectType) {
                extrObjs.add((ExtrinsicObjectType) o);
            }
        }
        return extrObjs;
    }

    public static Map<String,Document> getDocuments(ProvideAndRegisterDocumentSetRequestType req) {
        List<Document> docs = req.getDocument();
        Map<String,Document> map = new HashMap<String,Document>(docs.size());
        Document doc;
        for ( int i = 0, len = docs.size() ; i < len ; i++) {
            doc = docs.get(i);
            map.put(doc.getId(), doc);
        }
        return map;
    }
    
    public static Map<String, SlotType1> getSlotsFromRegistryObject(RegistryObjectType ro) throws JAXBException {
        List<SlotType1> slots = ro.getSlot();
        Map<String, SlotType1> slotByName = new HashMap<String, SlotType1>(slots == null ? 0 : slots.size());
        if (slots != null) {
            SlotType1 slot;
            for (int i = 0, len = slots.size() ; i < len ; i++) {
                slot = slots.get(i);
                slotByName.put(slot.getName(), slot);
            }
        }
        return slotByName;
    }

    public static String getExternalIdentifierValue(String urn, RegistryObjectType ro) {
        List<ExternalIdentifierType> list = ro.getExternalIdentifier();
        ExternalIdentifierType ei;
        for ( int i = 0, len = list.size() ; i < len ; i++ ) {
            ei = list.get(i);
            if ( ei.getIdentificationScheme().equals(urn)) {
                return ei.getValue();
            }
        }
        return null;
    }
    public static String setExternalIdentifierValue(String urn, String value, RegistryObjectType ro) {
        List<ExternalIdentifierType> list = ro.getExternalIdentifier();
        ExternalIdentifierType ei;
        for ( int i = 0, len = list.size() ; i < len ; i++ ) {
            ei = list.get(i);
            if ( ei.getIdentificationScheme().equals(urn)) {
                String oldValue = ei.getValue();
                ei.setValue(value);
                return oldValue;
            }
        }
        ei = new ExternalIdentifierType();
        ei.setIdentificationScheme(urn);
        ei.setValue(value);
        list.add(ei);
        return null;
    }

    public static String getSlotValue(List<SlotType1> slots, String slotName, String def) {
        for (SlotType1 slot : slots) {
            if ( slot.getName().equals(slotName)) {
                List<String> l = slot.getValueList().getValue();
                return l.isEmpty() ? def : l.get(0);
            }
        }
        return def;
    }

    public static Map<String, SlotType1> addOrOverwriteSlot(RegistryObjectType ro, String slotName, String... val) throws JAXBException {
        Map<String, SlotType1> slots = getSlotsFromRegistryObject(ro);
        addOrOverwriteSlot(ro, slots, slotName, val);
        return slots;
    }
    /*
     * Return null if slot has not exist or contains the same value, the old value if values are different.
     */
    public static String addOrCheckedOverwriteSlot(RegistryObjectType ro, Map<String, SlotType1> slots, 
            String slotName, String val, boolean ignoreCase) throws JAXBException {
        if (slots == null)
            slots = getSlotsFromRegistryObject(ro);
        SlotType1 oldSlot = (SlotType1)slots.get(slotName);
        if (oldSlot != null) {
            List<String> values = oldSlot.getValueList().getValue();
            if (values != null && values.size() > 0 && ! 
                    (ignoreCase ? val.equalsIgnoreCase(values.get(0)) : val.equals(values.get(0))) ) {
                return values.get(0);
            }
        }            
        addOrOverwriteSlot(ro, slots, slotName, val);
        return null;
    }
    public static void addOrOverwriteSlot(RegistryObjectType ro, Map<String, SlotType1> slots, String slotName, String... val) throws JAXBException {
        if ( slots.containsKey(slotName) ) {
            SlotType1 oldSlot = (SlotType1)slots.get(slotName); 
            ro.getSlot().remove(oldSlot);
        }   
        SlotType1 slot = objFac.createSlotType1();
        slot.setName(slotName);
        ValueListType valueList = objFac.createValueListType();
        for ( int i = 0 ; i < val.length ; i++) {
            valueList.getValue().add(val[i]);
        }
        slot.setValueList(valueList);
        ro.getSlot().add(slot);
    }
    
    public static AdhocQueryResponse emptyAdhocQueryResponse() {
        AdhocQueryResponse rsp = objFac.createAdhocQueryResponse();
        rsp.setRegistryObjectList(objFac.createRegistryObjectListType());
        return rsp;
    }

}
