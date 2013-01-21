/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contentsOfthis file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copyOfthe License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is partOfdcm4che, an implementationOfDICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial DeveloperOfthe Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contentsOfthis file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisionsOfthe GPL or the LGPL are applicable instead
 * of those above. If you wish to allow useOfyour versionOfthis file only
 * under the termsOfeither the GPL or the LGPL, and not to allow others to
 * use your versionOfthis file under the termsOfthe MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your versionOfthis file under
 * the termsOfany oneOfthe MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chee.xds2.conf;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.prefs.Preferences;

import org.dcm4che.conf.prefs.audit.PreferencesAuditLoggerConfiguration;
import org.dcm4che.conf.prefs.audit.PreferencesAuditRecordRepositoryConfiguration;
import org.dcm4che.util.SafeClose;
import org.dcm4chee.xds2.conf.prefs.PreferencesXdsConfiguration;
import org.junit.Before;

public class XdsConfigPrefsTest extends XdsConfigTestBase {


    @Before
    public void setUp() throws Exception {
        testCount++;
        PreferencesXdsConfiguration cfg = new PreferencesXdsConfiguration(Preferences.userRoot());
        cfg.addDicomConfigurationExtension(new PreferencesAuditLoggerConfiguration());
        cfg.addDicomConfigurationExtension(new PreferencesAuditRecordRepositoryConfiguration());
        config = cfg;
        cleanUp();
    }

    @Override
    public void afterPersist() throws Exception{
        String name = System.getProperty("export");
        if (name == null)
            return;

        OutputStream os = new FileOutputStream(name);
        try {
            ((PreferencesXdsConfiguration) config)
                    .getDicomConfigurationRoot().exportSubtree(os);
        } finally {
            SafeClose.close(os);
        }
    }
}
