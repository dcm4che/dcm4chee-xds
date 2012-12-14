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
 * The ExtrinsicObject class is the primary metadata class for a RepositoryItem.
 * 
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Nov 02, 2011
 */
@Entity
@DiscriminatorValue("ExtrinsicObject")
public class ExtrinsicObject extends RegistryObject implements Serializable {
    private static final long serialVersionUID = 513457139488147710L;

    @Basic(optional = false)
    @Column(name = "is_opaque")
    private boolean isOpaque;
    
    @Basic(optional = true)
    @Column(name = "mime_type", length=256)
    private String mimeType;
    //contentVersionInfo flattened
    @Basic(optional = true)
    @Column(name = "contentversion_name", length=16)
    private String contentVersionName;
    
    @Basic(optional = true)
    @Column(name = "contentversion_comment", length=256)
    private String contentVersionComment;

    /**
     * Each ExtrinsicObject instance MAY have an isOpaque attribute defined. This attribute determines
     * whether the content catalogued by this ExtrinsicObject is opaque to (not readable by) the registry. In
     * some situations, a Submitting Organization may submit content that is encrypted and not even
     * readable by the registry.
     * 
     * @return
     */
    public boolean isOpaque() {
        return isOpaque;
    }
    public void setIsOpaque(boolean isOpaque) {
        this.isOpaque = isOpaque;
    }
    
    /**
     * Each ExtrinsicObject instance MAY have a mimeType attribute defined. The mimeType provides
     * information on the type of repository item catalogued by the ExtrinsicObject instance. The value of this
     * attribute SHOULD be a registered MIME media type at http://www.iana.org/assignments/media-types.
     *  
     * @return
     */
    public String getMimeType() {
        return mimeType;
    }
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    /**
     * ContentVersionInfo (flattened: contentVersionName, contentVersionComment)
     * Each ExtrinsicObject instance MAY have a contentVersionInfo attribute. The value of the
     * contentVersionInfo attribute MUST be of type VersionInfo. The contentVersionInfo attribute provides
     * information about the specific version of the RepositoryItem associated with an ExtrinsicObject. The
     * contentVersionInfo attribute is set by the registry.
     * 
     * @return
     */
    public String getContentVersionName() {
        return contentVersionName;
    }
    public void setContentVersionName(String contentVersionName) {
        this.contentVersionName = contentVersionName;
    }
    public String getContentVersionComment() {
        return contentVersionComment;
    }
    public void setContentVersionComment(String contentVersionComment) {
        this.contentVersionComment = contentVersionComment;
    }
}
