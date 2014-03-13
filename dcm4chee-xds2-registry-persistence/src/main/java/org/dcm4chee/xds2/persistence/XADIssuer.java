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
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Index;

/**
 * A Patient Issuer of XDS Affinity Domain. 
 * 
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Nov 02, 2011
 */
@NamedQueries({
    @NamedQuery(
        name="XADIssuer.findByUID",
        query="SELECT i FROM XADIssuer i WHERE i.universalID = ?1"),
    @NamedQuery(
        name="XADIssuer.findByUIDandType",
        query="SELECT i FROM XADIssuer i WHERE i.universalID = ?1 and i.universalIdType = ?2")
    })

@Entity
@Table(name = "xad_issuer", uniqueConstraints={@UniqueConstraint(columnNames={"universal_id", "universal_id_type"}) })
public class XADIssuer implements Serializable {
    private static final long serialVersionUID = 513457139488147710L;

    public static final String FIND_ISSUER_BY_UID = "XADIssuer.findByUID";
    public static final String FIND_ISSUER_BY_UID_AND_TYPE = "XADIssuer.findByUIDandType";
    
    @Id
    @GeneratedValue
    @Column(name = "pk")
    private int pk;
    
    @Column(name = "namespace_id")
    @Index(name="namespace_id_idx")
    private String namespaceID;

    @Basic(optional = false)
    @Column(name = "universal_id")
    @Index(name="universal_id_idx")
    private String universalID;
    
    @Basic(optional = false)
    @Column(name = "universal_id_type")
    private String universalIdType;
    
    public XADIssuer() {
    }

    public XADIssuer(String issuer) {
        setIssuer(issuer);
    }
    
    public long getPk() {
        return pk;
    }

    /**
     * Return the Authority in XAD format &<universal authority ID>&ISO
     * @return
     */
    public String getXADIssuer() {
        return "&"+universalID+"&"+universalIdType;
    }
    
    /**
     * Return the Authority in HL7 CX format <local authority ID>&<universal authority ID>&ISO
     * @return
     */
    public String getCXIssuer() {
        return namespaceID == null ? getXADIssuer() :
            namespaceID+"&"+universalID+"&"+universalIdType;
    }
    
    public void setIssuer(String authority) {
        int pos, pos1;
        if ( (pos = authority.indexOf('&')) == -1 || 
             (pos1 = authority.indexOf('&', pos+1)) == -1) {
            throw new IllegalArgumentException("Authority has no universal ID/Type!");
        }   
        namespaceID = authority.substring(0, pos);
        universalID = authority.substring(++pos, pos1);
        universalIdType = authority.substring(++pos1);
    }
    
    /**
     * NamespaceID of Assigning Authority is NOT used (and not allowed) in XDS transactions 
     * but is (or should be, if available) part of Assigning Authority in HL7 Patient Identity Feed.
     */
    public String getNamespaceID() {
        return namespaceID;
    }

    public void setNamespaceID(String namespaceID) {
        this.namespaceID = namespaceID;
    }

    public String getUniversalID() {
        return universalID;
    }

    public void setUniversalID(String universalID) {
        this.universalID = universalID;
    }

    public String getUniversalIdType() {
        return universalIdType;
    }

    public void setUniversalIdType(String universalIdType) {
        this.universalIdType = universalIdType;
    }

    @Override
    public String toString() {
        return getXADIssuer();
    }
    
}
