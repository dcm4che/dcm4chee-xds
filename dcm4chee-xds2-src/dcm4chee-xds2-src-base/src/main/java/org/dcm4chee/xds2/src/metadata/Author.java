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
package org.dcm4chee.xds2.src.metadata;

import java.util.Arrays;
import java.util.List;

import org.dcm4chee.xds2.infoset.rim.ClassificationType;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectType;

/**
 * Document Entry Builder.
 * 
 * @author franz.willer@agfa.com
 *
 */
public class Author {
    private static final int PERSON = 0;
    private static final int INSTITUTION = 1;
    private static final int ROLE = 2;
    private static final int SPECIALITY = 3;
    private static final int TELECOMMUNICATION = 4;
    
    private String id;
    private ClassificationType author;
    @SuppressWarnings("rawtypes")
    List[] subAuthors = new List[5];
    String[] slotNames = new String[]{"authorPerson", "authorInstitution", "authorRole", "authorSpecialty", "authorTelecommunication"};
    
    private Author(String id, String classificationScheme, RegistryObjectType ro) {
        this.id = id;
        author = Util.createClassification(id, classificationScheme, ro.getId(), "");
        ro.getClassification().add(author);
    }
    
    public static Author newDocumentEntryAuthor(String id, RegistryObjectType ro) {
        return new Author(id, "urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d", ro);
    }
    
    public static Author newSubmissionSetAuthor(String id, RegistryObjectType ro) {
        return new Author(id, "urn:uuid:a7058bb9-b4e4-4307-ba5b-e3f0ab85e12d", ro);
    }
    
    public String getID() {
        return id;
    }

    public Author setAuthorPerson(String value) {
        setValues(PERSON, value == null ? null : Arrays.asList(value));
        return this;
    }
    
    public String getAuthorPerson() {
         return subAuthors[PERSON] == null ? null : (String)subAuthors[PERSON].get(0); 
    }
    
    public Author setAuthorInstitutions(List<String> values) {
        setValues(INSTITUTION, values);
        return this;
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getAuthorInstitutions() {
        return subAuthors[INSTITUTION];
    }
    
    public Author setAuthorRoles(List<String> values) {
        setValues(ROLE, values);
        return this;
    }
    @SuppressWarnings("unchecked")
    public List<String> getAuthorRoles() {
        return subAuthors[ROLE];
    }

    public Author setAuthorSpecialities(List<String> values) {
        setValues(SPECIALITY, values);
        return this;
    }
    @SuppressWarnings("unchecked")
    public List<String> getAuthorSpecialities() {
        return subAuthors[SPECIALITY];
    }

    public Author setAuthorTelecommunications(List<String> values) {
        setValues(TELECOMMUNICATION, values);
        return this;
    }
    @SuppressWarnings("unchecked")
    public List<String> getAuthorTelecommunications() {
        return subAuthors[TELECOMMUNICATION];
    }

    @SuppressWarnings("unchecked")
    private void setValues(int idx, List<String> values) {
        if (subAuthors[idx] == null) {
            if (values != null && values.size() > 0) {
                subAuthors[idx] = Util.setSlot(author, slotNames[idx], null, values).getValueList().getValue();
            }
        } else {
            if (values == null) {
                Util.removeSlot(author, slotNames[idx]);
                subAuthors[idx] = null;
            } else {
                subAuthors[idx].clear();
                subAuthors[idx].addAll(values);
            }
        }
    }
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    @Override
    public boolean equals(Object o) {
        return (o instanceof Author) ? o == null ? false : id.equals(((Author) o).getID()) : false;
    }
}