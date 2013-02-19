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

package org.dcm4chee.xds2.ws.registry.query;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.SlotType1;

/**
 * Hold values of a single StoredQuery parameter 
 * 
 * ITI TF-2a: 3.18.4.1.2.3.5 Coding of Single/Multiple Values
 * Single values are coded as
 *  # 123 - without quotes for numbers
 *  # 'urn:oasis:names:tc:ebxml-regrep:StatusType:Approved' - in single quotes for strings.
 *  # 'Children''s Hospital' - single quote is inserted in a string by specifying two single quotes
 * Within the LIKE predicate
 * # Underscore ('_') matches an arbitrary character
 * # Percent ('%') matches an arbitrary string
 * Format for multiple values is
 * # (value, value, value, ...) OR
 * # (value) if only one value is to be specified.
 * where each value is coded as described above for single values.
 * 
 * And/or semantics for the coding of parameters shall be available only on parameters for multivalued
 * metadata elements (such as $XDSDocumentEntryEventCodeList). Multi-valued parameters shall be coded
 * in two ways with different interpretations.
 * A parameter specified as a Slot with multiple values shall be interpreted as disjunction 
 * (OR semantics). For example:
 *   <rim:Slot name="$XDSDocumentEntryEventCodeList">
 *     <rim:ValueList>
 *       <rim:Value>('a')</rim:Value>
 *       <rim:Value>('b')</rim:Value>
 *     </rim:ValueList>
 *   </rim:Slot>
 *  shall match an XDSDocumentEntry object with an eventCodeList attribute containing either 'a'
 *  or 'b'. The following coding of the parameter shall yield the same results:
 *    <rim:Slot name="$XDSDocumentEntryEventCodeList">
 *      <rim:ValueList>
 *        <rim:Value>('a','b')</rim:Value>
 *      </rim:ValueList>
 *    </rim:Slot>
 * A parameter specified as multiple Slots shall be interpreted as conjunction (AND semantics). For
 * example:
 *   <rim:Slot name="$XDSDocumentEntryEventCodeList">
 *     <rim:ValueList>
 *       <rim:Value>('a')</rim:Value>
 *     </rim:ValueList>
 *   </rim:Slot>
 *   <rim:Slot name="$XDSDocumentEntryEventCodeList">
 *     <rim:ValueList>
 *       <rim:Value>('b')</rim:Value>
 *     </rim:ValueList>
 *   </rim:Slot>
 * shall match an XDSDocumentEntry object with an eventCodeList attribute containing both 'a' and 'b'.
 * Furthermore, the following specification of the $XDSDocumentEntryEventCodeList parameter:
 *   <rim:Slot name="$XDSDocumentEntryEventCodeList">
 *     <rim:ValueList>
 *       <rim:Value>('a','b')</rim:Value>
 *     </rim:ValueList>
 *   </rim:Slot>
 *   <rim:Slot name="$XDSDocumentEntryEventCodeList">
 *     <rim:ValueList>
 *       <rim:Value>('c')</rim:Value>
 *     </rim:ValueList>
 *   </rim:Slot>
 *   shall be interpreted as matching a document having eventCode (a OR b) AND c.
 *   
 * @author franz.willer@gmail.com
 *
 */
public class StoredQueryParam {

    private String name;
    private List<List<String>> andOrValues;
    private String singleValue;
    private Boolean numeric;
    
    public static Map<String, StoredQueryParam> getQueryParams(AdhocQueryRequest req) throws XDSException {
        List<SlotType1> slots = req.getAdhocQuery().getSlot();
        if (slots != null) {
            Map<String, StoredQueryParam> params = new HashMap<String, StoredQueryParam>();
            SlotType1 slot;
            StoredQueryParam param;
            for (int i = 0, len= slots.size() ; i < len ; i++) {
                slot = slots.get(i);
                param = params.get(slot.getName());
                if (param == null) {
                    param = new StoredQueryParam(slot.getName()); 
                    params.put(param.getName(), param);
                }
                param.addValues(slot.getValueList().getValue());
            }
            return params;
        } else {
            throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, "AdhocQueryRequest: AdhocQuery.Slot is null!", null);
        }
    }
    
    public StoredQueryParam(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isNumeric() {
        return numeric;
    }
    
    public String getStringValue() {
        return singleValue;
    }

    public Number getNumericValue() throws ParseException {
        return NumberFormat.getInstance().parse(singleValue);
    }
    
    public boolean isMultiValue() {
        return andOrValues != null;
    }
    public boolean isConjunction() {
        return andOrValues != null && andOrValues.size() > 1;
    }
    public int getNumberOfANDElements() {
        return andOrValues == null ? 1 : andOrValues.size();
    }
    public String[] getValues() {
        return singleValue != null ? new String[]{singleValue} : (String[])andOrValues.get(0).toArray(new String[0]);
    }
    public List<String> getMultiValues(int idx) {
        return andOrValues == null ? null : andOrValues.get(idx);
    }
    
    private void addValues(List<String> slotValues) throws XDSException {
        if (singleValue != null) {
            throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                    "Add a conjunction failed! Query parameter "+name+" is a Single value!", null);
        }
        if (slotValues.size() == 1 && slotValues.get(0).charAt(0)!='(') {
            singleValue = checkText(slotValues.get(0));
        } else {
            if (this.andOrValues == null) {
                andOrValues = new ArrayList<List<String>>();
            }
            List<String> values = new ArrayList<String>();
            andOrValues.add(values);
            for (String s : slotValues) {
                if (s.charAt(0)=='(' && s.charAt(s.length()-1)==')') {
                    StringTokenizer st = new StringTokenizer(s.substring(1,s.length()-1), ",");
                    while (st.hasMoreElements()) {
                        values.add(checkText(st.nextToken().trim()));
                    }
                } else {
                    throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                            "Wrong format of slot value! MultiValue Query parameter must be '(<value>[,<value>[..]])' name:"+name, null);
                }
            }
        }
    }

    private String checkText(String s) throws XDSException {
        boolean b = s.charAt(0) != '\'';
        if (numeric == null) {
            numeric = b;
        } else if (numeric != b) {
            throw new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                    "Query parameter "+name+" contains numeric AND text values!", null);
        }
        if (numeric) {
            return s;
        } else {
            return s.substring(1,s.length()-1);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof StoredQueryParam))
            return false;
        return name.equals(((StoredQueryParam) o).name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append('=');
        if (isMultiValue())
            sb.append('(');
        if (this.isNumeric()) {
            for (String v :getValues()) { 
                sb.append(v).append(',');
            }
        } else {
            for (String v :getValues()) { 
                sb.append('\'').append(v).append("',");
            }
        }            
        sb.setLength(sb.length()-1);
        if (isMultiValue())
            sb.append(')');
        return sb.toString();
    }

}
