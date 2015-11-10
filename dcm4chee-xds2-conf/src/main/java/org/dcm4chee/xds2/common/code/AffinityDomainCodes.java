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
package org.dcm4chee.xds2.common.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


/**
 * Code repository to maintain defined codes for affinity domain(s).
 * 
 * @author franz.willer@agfa.com
 *
 */
public class AffinityDomainCodes {

    private HashMap<String, List<Code>> codes = new HashMap<String, List<Code>>();
    private HashMap<String, String> classSchemesToCodeTypes = new HashMap<String, String>();
    private String affinityDomain = "UNDEFINED";
    
    public static final Code MATCH_ALL_CODE = new Code("*", "dcm4che", "Match all");
    
    public void addClassSchemeToCodeType(String scheme, String codeType) {
        classSchemesToCodeTypes.put(scheme, codeType);
    }
    public void addCodes(String name, List<Code> codes) {
        this.codes.put(name, codes);
    }
    public void addCode(String scheme, String codeType, Code code) {
        List<Code> l = codes.get(codeType);
        if (l == null) {
            l = new ArrayList<Code>();
            codes.put(codeType, l);
            classSchemesToCodeTypes.put(scheme, codeType);
        }
        l.add(code);
    }
    
    public Set<String> getCodeTypes() {
        return codes.keySet();
    }
    public List<Code> getCodes(String codeType) {
        return codes.get(codeType);
    }
    
    public String getAffinityDomain() {
        return affinityDomain;
    }
    public void setAffinityDomain(String affinityDomain) {
        this.affinityDomain = affinityDomain;
    }
    public boolean isEmpty() {
        return codes.isEmpty();
    }
    
    public boolean isDefined(String codeType, Code code) {
        return codes.get(codeType).contains(code);
    }
    
    public boolean isDefined(String codeType, String codeValue, String codeDesignator) {
        return codes.get(codeType).contains(new Code(codeValue, codeDesignator, ""));
    }
    public boolean isClassSchemeCodeDefined(String scheme, String codeValue, String codeDesignator) {
        return isClassSchemeCodeDefined(scheme, new Code(codeValue, codeDesignator, ""));
    }
    public boolean isClassSchemeCodeDefined(String scheme, Code code) {
        List<Code> l = codes.get(classSchemesToCodeTypes.get(scheme));
        return l == null ? false : l.contains(code) ? true : l.contains(MATCH_ALL_CODE);
    }
}