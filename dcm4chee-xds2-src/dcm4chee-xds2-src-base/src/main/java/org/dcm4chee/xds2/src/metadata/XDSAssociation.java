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

/**
 * 
 * @author franz.willer@agfa.com
 *
 */
public class XDSAssociation {
    public static final String STATUS_ORIGINAL = "Original";
    
    private String id;
    private String sourceObject;
    private String targetObject;
    private String associationType;
    private String submissionSetStatus = STATUS_ORIGINAL;

    public XDSAssociation(String id, String src, String target, String type) {
        this.id = id;
        sourceObject = src;
        targetObject = target;
        associationType = type;
    }
    public String getID() {
        return id;
    }
    public String getSourceObject() {
        return sourceObject;
    }
    public String getTargetObject() {
        return targetObject;
    }
    public String getAssociationType() {
        return associationType;
    }
    public String getSubmissionSetStatus() {
        return submissionSetStatus;
    }
    public void setSubmissionSetStatus(String submissionSetStatus) {
        this.submissionSetStatus = submissionSetStatus;
    }
}