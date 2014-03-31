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

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

/**
 * ExternalIdentifier instances provide the additional identifier information to RegistryObject such as
 * DUNS number, Social Security Number, or an alias name of the organization. The attribute
 * identificationScheme is used to reference the identification scheme (e.g., 'DUNS', 'Social Security #'),
 * and the attribute value contains the actual information (e.g., the DUNS number, the social security
 * number). Each RegistryObject MAY contain 0 or more ExternalIdentifier instances.
 * 
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Nov 02, 2011
 */
@Entity
@DiscriminatorValue("ExternalIdentifier")
public class ExternalIdentifier extends RegistryObject implements Serializable {
    private static final long serialVersionUID = 513457139488147710L;

    @OneToOne
    @JoinColumn(name = "identificationScheme_fk")
    private ClassificationScheme identificationScheme;
    @JoinColumn(name = "registryObject_fk")
    @ManyToOne
    private RegistryObject registryObject;
    @Basic(optional = false)
    @Column(name = "value")
    private String value;
    
    /**
     * Each ExternalIdentifier instance MUST have an identificationScheme attribute that references a
     * ClassificationScheme. This ClassificationScheme defines the namespace within which an identifier is
     * defined using the value attribute for the RegistryObject referenced by the RegistryObject attribute.
     * 
     * @param identificationScheme
     */
    public void setIdentificationScheme(ClassificationScheme identificationScheme) {
        this.identificationScheme = identificationScheme;
    }
    public ClassificationScheme getIdentificationScheme() {
        return identificationScheme;
    }
    
    /**
     * Each ExternalIdentifier instance MUST have a registryObject attribute that references the parent
     * RegistryObject for which this is an ExternalIdentifier.
     * 
     * @param registryObject
     */
    public void setRegistryObject(RegistryObject registryObject) {
        this.registryObject = registryObject;
    }
    public RegistryObject getRegistryObject() {
        return registryObject;
    }
    
    /**
     * Each ExternalIdentifier instance MUST have a value attribute that provides the identifier value for this
     * ExternalIdentifier (e.g., the actual social security number).
     * 
     * @param value
     */
    public void setValue(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
    
}
