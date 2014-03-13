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

import org.hibernate.annotations.Index;

/**
 * A Classification instance classifies a RegistryObject instance by referencing a node defined within a
 * particular ClassificationScheme. An internal Classification will always reference the node directly, by its
 * id, while an external Classification will reference the node indirectly by specifying a representation of its
 * value that is unique within the external classification scheme.
 * 
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Nov 02, 2011
 */
@Entity
@DiscriminatorValue("Classification")
public class Classification extends RegistryObject implements Serializable {
    private static final long serialVersionUID = 513457139488147710L;

    @Basic(optional = false)
    @JoinColumn(name = "classificationScheme_fk")
    @ManyToOne
    private ClassificationScheme classificationScheme;
    @JoinColumn(name = "classificationNode_fk")
    @ManyToOne
    private ClassificationNode classificationNode;
    
    @Basic(optional = false)
    @JoinColumn(name = "classifiedObject_fk")
    @ManyToOne
    private RegistryObject classifiedObject;
    
    @Basic(optional = true)
    @Column(name = "nodeRepresentation")
    @Index(name="nodeRepresentation_idx")
    private String nodeRepresentation;

    public ClassificationScheme getClassificationScheme() {
        return classificationScheme;
    }

    /**
     * If the Classification instance represents an external classification, then the classificationScheme
     * attribute is required. The classificationScheme value MUST reference a ClassificationScheme
     * instance.
     * 
     * @param classificationScheme
     */
    public void setClassificationScheme(ClassificationScheme classificationScheme) {
        this.classificationScheme = classificationScheme;
    }

    /**
     * If the Classification instance represents an internal classification, then the classificationNode attribute
     * is required. The classificationNode value MUST reference a ClassificationNode instance.
     * 
     * @return
     */
    public ClassificationNode getClassificationNode() {
        return classificationNode;
    }

    public void setClassificationNode(ClassificationNode classificationNode) {
        this.classificationNode = classificationNode;
    }

    /**
     * For both internal and external classifications, the classifiedObject attribute is required and it references
     * the RegistryObject instance that is classified by this Classification.
     * 
     * @return
     */
    public RegistryObject getClassifiedObject() {
        return classifiedObject;
    }

    public void setClassifiedObject(RegistryObject classifiedObject) {
        this.classifiedObject = classifiedObject;
    }

    /**
     * If the Classification instance represents an external classification, then the nodeRepresentation
     * attribute is required. It is a representation of a taxonomy element from a classification scheme. It is the
     * responsibility of the registry to distinguish between different types of nodeRepresentation, like between
     * the classification scheme node code and the classification scheme node canonical path. This allows
     * the client to transparently use different syntaxes for nodeRepresentation.
     * 
     * @return
     */
    public String getNodeRepresentation() {
        return nodeRepresentation;
    }

    public void setNodeRepresentation(String nodeRepresentation) {
        this.nodeRepresentation = nodeRepresentation;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Classification id:").append(getId()).append(classifiedObject);
        if (classificationNode != null)
            sb.append(classificationNode);
        else
            sb.append(classificationScheme);
        return sb.toString();
        
    }
}
