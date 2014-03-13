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
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Index;

/**
 * ClassificationNode instances are used to define tree structures where each node in the tree is a
 * ClassificationNode. Such ClassificationScheme trees are constructed with ClassificationNode instances
 * under a ClassificationScheme instance, and are used to define Classification schemes or ontologies.
 * 
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Nov 02, 2011
 */
@Entity
@DiscriminatorValue("ClassificationNode")
public class ClassificationNode extends RegistryObject implements Serializable {
    private static final long serialVersionUID = 513457139488147710L;

    @JoinColumn(name = "parent_fk")
    @ManyToOne(cascade=CascadeType.ALL)
    private Identifiable parent;
    @Basic(optional = true)
    @Column(name = "code")
    @Index(name="code_idx")
    private String code;
    @Basic(optional = true)
    @Column(name = "path", length=1024)
    private String path;
    
    /**
     * Each ClassificationNode MAY have a parent attribute. The parent attribute either references a parent
     * ClassificationNode or a ClassificationScheme instance in case of first level ClassificationNode
     * instances.
     * 
     * @return
     */
    public Identifiable getParent() {
        return parent;
    }
    public void setParent(Identifiable parent) {
        if ((parent instanceof ClassificationNode) || (parent instanceof ClassificationScheme)
                || (parent instanceof ObjectRef)) {
            this.parent = parent;
            
        } else {
            throw new IllegalArgumentException("Parent (id:"+parent.getId()
                    + ") must be either ClassificationNode, ClassificationScheme or a ObjectRef! "
                    + parent.getClass().getSimpleName());
        }
    }
    
    /**
     * Each ClassificationNode MAY have a code attribute. The code attribute contains a code within a
     * standard coding scheme. The code attribute of a ClassificationNode MUST be unique with respect to
     * all sibling ClassificationNodes that are immediate children of the same parent ClassificationNode or
     * ClassificationScheme.
     * 
     * @return
     */
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    
    /**
     * Each ClassificationNode MAY have a path attribute. A registry MUST set the path attribute for any
     * ClassificationNode that has a non-null code attribute value, when the ClassificationNode is retrieved
     * from the registry. The path attribute MUST be ignored by the registry when it is specified by the client
     * at the time the object is submitted to the registry. The path attribute contains the canonical path from
     * the root ClassificationScheme or ClassificationNode within the hierarchy of this ClassificationNode as
     * defined by the parent attribute. The path attribute of a ClassificationNode MUST be unique within a
     * registry.
     * 
     * @return
     */
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    
    
}
