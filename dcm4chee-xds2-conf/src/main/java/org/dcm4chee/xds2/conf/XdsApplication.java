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
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2012
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
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

package org.dcm4chee.xds2.conf;

import java.io.Serializable;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Franz Willer <franz.willer@gmail.com>
 */
public class XdsApplication implements Serializable {

    private static final long serialVersionUID = -8258532093950989486L;

    private XdsDevice device;
    private String name;
    private String affinityDomain;
    private String[] mimeTypes;
    private String soapLogDir;
    
    private boolean createMissingPIDs;
    private boolean createMissingCodes;

    public XdsApplication(String name) {
        this.name = name;
    }

    public final XdsDevice getDevice() {
        return device;
    }

    void setDevice(XdsDevice device) {
        if (device != null  && this.device != null)
            throw new IllegalStateException("already owned by " + this.device);
        this.device = device;
    }

    public String getApplicationName() {
        return name;
    }

    public void setApplicationName(String name) {
        if (name.isEmpty())
            throw new IllegalArgumentException("name cannot be empty");
        XdsDevice device = this.device;
        if (device != null)
            device.removeXdsApplication(this.name);
        this.name = name;
        if (device != null)
            device.addXdsApplication(this);
    }

    void reconfigure(XdsApplication src) {
        setXdsApplicationAttributes(src);
    }

    protected void setXdsApplicationAttributes(XdsApplication src) {
        this.setAffinityDomain(src.getAffinityDomain());
        this.setAcceptedMimeTypes(src.getAcceptedMimeTypes());
        this.setCreateMissingCodes(src.isCreateMissingCodes());
        this.setCreateMissingPIDs(src.isCreateMissingPIDs());
        this.setSoapLogDir(src.getSoapLogDir());
    }

    void addCopyTo(XdsDevice xdsDevice) {
        try {
            XdsApplication xdsApp = (XdsApplication) clone();
            xdsApp.device = null;
            device.addXdsApplication(xdsApp);
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public String getAffinityDomain() {
        return affinityDomain;
    }

    public void setAffinityDomain(String affinityDomain) {
        this.affinityDomain = affinityDomain;
    }

    public String[] getAcceptedMimeTypes() {
        return mimeTypes;
    }

    public void setAcceptedMimeTypes(String[] mimeTypes) {
        this.mimeTypes = mimeTypes;
    }

    public String getSoapLogDir() {
        return soapLogDir;
    }

    public void setSoapLogDir(String soapLogDir) {
        this.soapLogDir = soapLogDir;
    }

    public boolean isCreateMissingPIDs() {
        return createMissingPIDs;
    }

    public void setCreateMissingPIDs(boolean createMissingPIDs) {
        this.createMissingPIDs = createMissingPIDs;
    }

    public boolean isCreateMissingCodes() {
        return createMissingCodes;
    }

    public void setCreateMissingCodes(boolean createMissingCodes) {
        this.createMissingCodes = createMissingCodes;
    }
    
}
