package org.dcm4chee.xds2.registry.ws.sq;
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



import java.util.List;

import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryType;
import org.dcm4chee.xds2.infoset.rim.ObjectFactory;
import org.dcm4chee.xds2.infoset.rim.ResponseOptionType;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.infoset.rim.ValueListType;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */
public abstract class AbstractSQTests {
    private static ObjectFactory factory = new ObjectFactory();
    private List<SlotType1> paramSlots;
    public static final String TEST_PAT_ID = "'test1234_1^^^&1.2.3.45.4.3.2.1&ISO'";
    public static final String TEST_PAT_ID2 = "'test1234_2^^^&1.2.3.45.4.3.2.1&ISO'";
    
    protected static final String DOC_A = "DocA";//single_doc.xml
    protected static final String DOC_A_UUID = "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba1001";
    protected static final String DOC_A_UNIQUE_ID = "1.20.3.4.0.2.1";
    protected static final String SUBM_DOC_A_UUID = "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba1002";
    protected static final String SUBM_DOC_A_UNIQUE_ID = "1.20.3.4.1.2.1.1";
    protected static final String ASSOC_DOC_A_SUBM_UUID = "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba1207";

    protected static final String DOC_B = "DocB";//single_doc_w_fol.xml
    protected static final String DOC_B_UUID = "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba2001";
    protected static final String DOC_B_UNIQUE_ID = "1.20.3.4.0.2.2";
    protected static final String SUBM_DOC_B_UUID = "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba2003";
    protected static final String FOLDER_DOC_B_UUID = "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba2002";
    protected static final String FOLDER_DOC_B_UNIQUE_ID = "1.20.3.4.2.2.2";
    protected static final String ASSOC_DOC_B_SUBM_UUID = "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba2004";
    
    protected static final String DOC_C = "DocC";//two_doc_w_fol.xml
    protected static final String DOC_C_UUID = "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba3001";
    protected static final String DOC_C_UNIQUE_ID = "1.20.3.4.0.2.3";
    protected static final String DOC_D = "DocD";//two_doc_w_fol.xml
    protected static final String DOC_D_UUID = "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba3002";
    protected static final String DOC_D_UNIQUE_ID = "1.20.3.4.0.2.3.1";
    protected static final String SUBM_DOC_C_D_UUID = "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba3004";
    protected static final String SUBM_DOC_C_D_UNIQUE_ID = "1.20.3.4.1.2.3";
    protected static final String FOLDER_DOC_C_D_UUID = "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba3003";
    protected static final String FOLDER_DOC_C_D_UNIQUE_ID = "1.20.3.4.2.2.3";
    protected static final String ASSOC_DOC_C_FOLDER_UUID = "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba3005";
    protected static final String ASSOC_DOC_D_FOLDER_UUID = "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba3006";
    
    protected static final String DOC_E = "DocE";//single_doc_for_rplc.xml
    protected static final String DOC_E_UUID = "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba4001";
    protected static final String DOC_E_UNIQUE_ID = "1.20.3.4.0.2.4";
    protected static final String SUBM_DOC_E_UUID = "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba4002";
    
    protected static final String DOC_F = "DocF";//rplc.xml
    protected static final String DOC_F_UUID = "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba5001";
    protected static final String DOC_F_UNIQUE_ID = "1.20.3.4.0.2.5";
    protected static final String SUBM_DOC_F_UUID = "urn:uuid:aabbccdd-bdda-424e-8c96-df4873ba5002";
    
    public AdhocQueryRequest getQueryRequest(String queryId, String returnType, SlotType1[] defaults) {
        AdhocQueryRequest req = factory.createAdhocQueryRequest();
        ResponseOptionType responseOption = factory.createResponseOptionType();
        responseOption.setReturnComposedObjects(true);
        responseOption.setReturnType(returnType);
        req.setResponseOption(responseOption);
        AdhocQueryType adhocQuery = factory.createAdhocQueryType();
        adhocQuery.setId(queryId);
        paramSlots = adhocQuery.getSlot();
        if (defaults != null) {
            for (int i = 0 ; i < defaults.length ; i++) {
                paramSlots.add(defaults[i]);
            }
        }
        req.setAdhocQuery(adhocQuery);
        return req;
    }

    public void addQueryParam(List<SlotType1> slots, String name, String value) {
        SlotType1 slot = toQueryParam(name, value);
        slots.add(slot);
    }
    protected void addQueryParam(String name, String value) {
        addQueryParam(paramSlots, name, value);
    }

    public void addMultiQueryParam(List<SlotType1> slots, String name, boolean isString, String... value) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0 ; i < value.length ; i++) {
            if (isString) {
                sb.append('\'').append(value[i]).append('\'');
            } else {
                sb.append('\'').append(value[i]);
            }
            sb.append(',');
        }
        sb.setLength(sb.length()-1);
        sb.append(')');
        SlotType1 slot = toQueryParam(name, sb.toString());
        slots.add(slot);
    }
    protected void addMultiQueryParam(String name, boolean isString, String... value) {
        addMultiQueryParam(paramSlots, name, isString, value);
    }

    public static SlotType1 toQueryParam(String name, String value) {
        SlotType1 slot = factory.createSlotType1();
        slot.setName(name);
        ValueListType valueList = factory.createValueListType();
        valueList.getValue().add(value);
        slot.setValueList(valueList);
        return slot;
    }
}
