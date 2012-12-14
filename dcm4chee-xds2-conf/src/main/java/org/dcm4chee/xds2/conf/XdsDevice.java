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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.LinkedHashMap;

import org.dcm4che.net.Device;
import org.dcm4che.net.hl7.HL7Application;
import org.dcm4che.net.hl7.HL7Device;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 *
 */
public class XdsDevice extends HL7Device {

    private static final long serialVersionUID = 5891363555469614152L;

    private final LinkedHashMap<String, XdsApplication> xdsApps =
        new LinkedHashMap<String, XdsApplication>();
    
    public XdsDevice(String name) {
        super(name);
    }

    public void addXdsApplication(XdsApplication xdsApp) {
        xdsApp.setDevice(this);
        xdsApps.put(xdsApp.getApplicationName(), xdsApp);
    }

    public XdsApplication removeXdsApplication(String name) {
        XdsApplication xdsApp = xdsApps.remove(name);
        if (xdsApp != null)
            xdsApp.setDevice(null);

        return xdsApp;
    }

    public boolean removeXdsApplication(XdsApplication xdsApp) {
        return removeXdsApplication(xdsApp.getApplicationName()) != null;
    }

    public XdsApplication getXdsApplication(String name) {
        XdsApplication xdsApp = xdsApps.get(name);
        if (xdsApp == null)
            xdsApp = xdsApps.get("*");
        return xdsApp;
    }

    public Collection<XdsApplication> getXdsApplications() {
        return xdsApps.values();
    }

    @Override
    public void reconfigure(Device from) throws IOException, GeneralSecurityException {
        super.reconfigure(from);
        reconfigureXdsApplications((XdsDevice) from);
    }

    private void reconfigureXdsApplications(XdsDevice from) {
        xdsApps.keySet().retainAll(from.xdsApps.keySet());
        for (XdsApplication src : from.xdsApps.values()) {
            XdsApplication xdsApp = xdsApps.get(src.getApplicationName());
            if (xdsApp != null)
                xdsApp.reconfigure(src);
            else
                src.addCopyTo(this);
        }
    }
}
