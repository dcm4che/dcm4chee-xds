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
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa-Gevaert AG.
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See listed authors below.
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

package org.dcm4chee.xds2.persistence;

import java.util.List;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.annotations.Index;

/**
 * XDSCode: classified Code value. (grouped codes)
 * XDSCode to XDS ebXML mapping:
 * 1) code value:           Classification.nodeRepresentation
 * 2) code designator:      Classification.Slot(name="codingScheme").value
 * 3) code meaning:         Classification.Name.LocalizedString
 * 4) code classification:  Classification.classificationScheme (UUID)
 * 
 * ebXML example:
 *  <rim:Classification
 *      classificationScheme="[code classification]"
 *      classifiedObject="urn:uuid:0e435f35-e643-43f3-b20f-855c0ac979d5" id="id_4"
 *      nodeRepresentation="[code value]"
 *      objectType="urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:Classification">
 *      <rim:Slot name="codingScheme">
 *          <rim:ValueList>
 *              <rim:Value>[code designator]</rim:Value>
 *          </rim:ValueList>
 *      </rim:Slot>
 *      <rim:Name>
 *          <rim:LocalizedString value="[code meaning]"/>
 *      </rim:Name>
 *  </rim:Classification>
 *  
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Nov 02, 2011
 */

@NamedQueries({
    @NamedQuery(
        name="XDSCode.findByCodeValue",
        query="SELECT c FROM XDSCode c " +
              "WHERE c.codeValue = ?1 AND c.codingSchemeDesignator = ?2 AND c.codeClassification = ?3")
    })

@Entity
@Table(name = "xds_xds_code")
public class XDSCode {
    private static final long serialVersionUID = 513457139488147710L;

    public  static final String FIND_BY_CODE_VALUE = "XDSCode.findByCodeValue";
    
    private static final String SLOT_NAME_CODING_SCHEME = "codingScheme";
    
    @Id
    @GeneratedValue
    @Column(name = "pk")
    private long pk;

    @Basic(optional = false)
    @Column(name = "value")
    @Index(name="xds_code_value_idx")
    private String codeValue;

    @Basic(optional = false)
    @Column(name = "designator")
    @Index(name="xds_code_designator_idx")
    private String codingSchemeDesignator;

    @Column(name = "classification")
    @Index(name="xds_code_classification_idx")
    private String codeClassification;

    @Basic(optional = false)
    @Column(name = "meaning")
    private String codeMeaning;

    public XDSCode() {}

    public XDSCode(String codeValue, String codingSchemeDesignator,
            String codeMeaning, String codeClassification) {
        this.codeValue = codeValue;
        this.codingSchemeDesignator = codingSchemeDesignator;
        this.codeMeaning = codeMeaning;
        this.codeClassification = codeClassification;
    }
    
    /* TODO: Why it was not used? 
      public XDSCode(Classification cl) {
        List<Slot> slots = cl.getSlots();
        if (slots == null || slots.size() != 1 || 
                !SLOT_NAME_CODING_SCHEME.equals(slots.get(0).getName())) {
            throw new IllegalArgumentException("Classification "+cl+
                    " is not an XDS Code! (must have one Slot with name 'codingScheme')");
        }
        Set<Name> names = cl.getName();
        if (names == null || names.isEmpty()) {
            throw new IllegalArgumentException("Classification "+cl+
            " is not a valid XDS Code! (must have a Name element as codeMeaning))");
        }
        codeValue = cl.getNodeRepresentation();
        codingSchemeDesignator = slots.get(0).getValue();
        codeMeaning = cl.getName().iterator().next().getValue();
        codeClassification = cl.getClassificationScheme().getId();
    }*/
    
    public String getCodeValue() {
        return codeValue;
    }

    public void setCodeValue(String codeValue) {
        this.codeValue = codeValue;
    }

    public String getCodingSchemeDesignator() {
        return codingSchemeDesignator;
    }

    public void setCodingSchemeDesignator(String codingSchemeDesignator) {
        this.codingSchemeDesignator = codingSchemeDesignator;
    }

    public String getCodeClassification() {
        return codeClassification;
    }

    public void setCodeClassification(String codeClassification) {
        this.codeClassification = codeClassification;
    }

    public String getCodeMeaning() {
        return codeMeaning;
    }

    public void setCodeMeaning(String codeMeaning) {
        this.codeMeaning = codeMeaning;
    }

    public long getPk() {
        return pk;
    }
    
    @Override
    public String toString() {
        return codeValue+"^^"+this.codingSchemeDesignator+" ("+this.codeClassification+")";
    }

}
