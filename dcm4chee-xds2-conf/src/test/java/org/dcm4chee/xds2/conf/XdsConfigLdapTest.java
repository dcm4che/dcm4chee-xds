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

import org.dcm4che.conf.ldap.LdapDicomConfiguration;
import org.dcm4che.conf.ldap.audit.LdapAuditLoggerConfiguration;
import org.dcm4che.conf.ldap.audit.LdapAuditRecordRepositoryConfiguration;
import org.dcm4che.conf.ldap.hl7.LdapHL7Configuration;
import org.dcm4chee.xds2.conf.ldap.LdapXCAInitiatingGWConfiguration;
import org.dcm4chee.xds2.conf.ldap.LdapXCARespondingGWConfiguration;
import org.dcm4chee.xds2.conf.ldap.LdapXCAiInitiatingGWConfiguration;
import org.dcm4chee.xds2.conf.ldap.LdapXCAiRespondingGWConfiguration;
import org.dcm4chee.xds2.conf.ldap.LdapXDSRegistryConfiguration;
import org.dcm4chee.xds2.conf.ldap.LdapXDSRepositoryConfiguration;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(IgnoreableJUnitTestRunner.class)
public class XdsConfigLdapTest extends XdsConfigTestBase{

    //Used by IgnoreableJUnitTestRunner to check if all tests of this class should be ignored
    public static boolean ignoreTests() {
        return System.getProperty("ldap") == null;
    }
    
    @Before
    public void setUp() throws Exception {
        testCount++;
        LdapDicomConfiguration cfg = new LdapDicomConfiguration();
        cfg.addDicomConfigurationExtension(new LdapXDSRegistryConfiguration());
        cfg.addDicomConfigurationExtension(new LdapXDSRepositoryConfiguration());
        cfg.addDicomConfigurationExtension(new LdapXCARespondingGWConfiguration());
        cfg.addDicomConfigurationExtension(new LdapXCAInitiatingGWConfiguration());
        cfg.addDicomConfigurationExtension(new LdapXCAiRespondingGWConfiguration());
        cfg.addDicomConfigurationExtension(new LdapXCAiInitiatingGWConfiguration());
        cfg.addDicomConfigurationExtension(new LdapHL7Configuration());
        cfg.addDicomConfigurationExtension(new LdapAuditLoggerConfiguration());
        cfg.addDicomConfigurationExtension(new LdapAuditRecordRepositoryConfiguration());
        config = cfg;
        cleanUp();
    }
}
