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

/**
 * A ClassificationScheme instance describes a taxonomy. The taxonomy hierarchy may be defined
 * internally to the registry by instances of ClassificationNode, or it may be defined externally to the
 * Registry, in which case the structure and values of the taxonomy elements are not known to the Registry.
 * 
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Nov 02, 2011
 */
@Entity
@DiscriminatorValue("ClassificationScheme")
public class ClassificationScheme extends RegistryObject implements Serializable {
    private static final long serialVersionUID = 513457139488147710L;

    @Basic(optional = false)
    @Column(name = "isInternal")
    private boolean isInternal;
    @Basic(optional = false)
    @Column(name = "nodeType")
    private String nodeType;
    
    /**
     * When submitting a ClassificationScheme instance the submitter MUST declare whether the
     * ClassificationScheme instance represents an internal or an external taxonomy. This allows the registry
     * to validate the subsequent submissions of ClassificationNode and Classification instances in order to
     * maintain the type of ClassificationScheme consistent throughout its lifecycle.
     * 
     * @return
     */
    public boolean isInternal() {
        return isInternal;
    }
    public void setInternal(boolean isInternal) {
        this.isInternal = isInternal;
    }
    
    /**
     * When submitting a ClassificationScheme instance the Submitting Organization MUST declare the
     * structure of taxonomy nodes within the ClassificationScheme via the nodeType attribute. The value of
     * the nodeType attribute MUST be a reference to a ClassificationNode within the canonical NodeType
     * ClassificationScheme. A Registry MUST support the node types as defined by the canonical NodeType
     * ClassificationScheme. The canonical NodeType ClassificationScheme MAY easily be extended by
     * adding additional ClassificationNodes to it.
     * The following canonical values are defined for the NodeType ClassificationScheme:
     * - UniqueCode: This value indicates that each node of the taxonomy has a unique code assigned to it.
     * - EmbeddedPath: This value indicates that the unique code assigned to each node of the
     *                 taxonomy also encodes its path. This is the case in the NAICS taxonomy.
     * - NonUniqueCode: In some cases nodes are not unique, and it is necessary to use the full path
     *                  (from ClassificationScheme to the node of interest) in order to identify the node. 
     *                  For example, in a geography taxonomy Moscow could be under both Russia and the USA, 
     *                  where there are five cities of that name in different states.
     * 
     * @return
     */
    public String getNodeType() {
        return nodeType;
    }
    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }
    
}
