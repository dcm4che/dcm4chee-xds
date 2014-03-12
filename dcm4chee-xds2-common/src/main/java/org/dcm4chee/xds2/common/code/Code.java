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


/**
 * Simple Code (without coding scheme version, equality only by value and designator).
 * 
 * @author franz.willer@agfa.com
 *
 */
public class Code {

    /**
     * Code Value
     * e.g.:(<b>TRID1001</b>, RSNA2008 eventCodeList, "XRAY CHEST Orderable")
     */
    private String value;
    /**
     * Code Scheme Designator (issuer of this code)
     * e.g.:(TRID1001, <b>RSNA2008 eventCodeList</b>, "XRAY CHEST Orderable")
     */
    private String designator;
    /**
     * Code Meaning
     * User friendly description (for display) of the Code
     * e.g.:(TRID1001, RSNA2008 eventCodeList, <b>"XRAY CHEST Orderable"</b>)
     */
    private String meaning;
    
    /**
     * Create a Code object with given value, designator and meaning.
     * 
     * @param value       Code Value
     * @param designator  Code Scheme Designator (Issuer of code)
     * @param meaning     Code Meaning (User friendly description for display)
     */
    public Code(String value, String designator, String meaning) {
        if (designator == null || meaning == null || value == null) {
            throw new IllegalArgumentException("Code value, designator and meaning must not be null! code:("+value+", "+designator+",\""+meaning+"\")");
        }
        this.value = value;
        this.designator = designator;
        this.meaning = meaning;
    }
    /**
     * Create a Code object with given Code string.
     * 
     * @param s Code String with format: '(&lt;code value&gt;, &lt;code scheme designator&gt;, "&lt;code meaning&gt;")'
     */
    public Code(String s) {
        int len = s.length();
        if (len < 9 
                || s.charAt(0) != '('
                || s.charAt(len-2) != '"'
                || s.charAt(len-1) != ')')
            throw new IllegalArgumentException(s);
        
        int endVal = s.indexOf(',');
        int endScheme = s.indexOf(',', endVal + 1);
        int startMeaning = s.indexOf('"', endScheme + 1) + 1;
        this.value = trimsubstring(s, 1, endVal);
        this.designator = trimsubstring(s, endVal+1, endScheme);
        this.meaning = trimsubstring(s, startMeaning, len-2);
    }

    private String trimsubstring(String s, int start, int end) {
        try {
            String trim = s.substring(start, end).trim();
            if (!trim.isEmpty())
                return trim;
        } catch (StringIndexOutOfBoundsException e) {}
        throw new IllegalArgumentException(s);
    }

    public String getValue() {
        return value;
    }
    public String getDesignator() {
        return designator;
    }
    public String getMeaning() {
        return meaning;
    }
    
    @Override
    public String toString() {
        return "Code: ("+value+", "+designator+",\""+meaning+"\")";
    }
    @Override
    public int hashCode() {
        return value.hashCode() * 37 + designator.hashCode();
    }
    @Override
    public boolean equals(Object o) {
        if ( o == null || !(o instanceof Code) ) {
            return false;
        } else {
            Code c = (Code) o;
            return value.equals(c.value) && designator.equals(c.designator);
        }
    }
}