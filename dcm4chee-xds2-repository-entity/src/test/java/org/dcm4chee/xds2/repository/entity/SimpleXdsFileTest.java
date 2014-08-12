package org.dcm4chee.xds2.repository.entity;
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
 * Portions created by the Initial Developer are Copyright (C) 2011
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


import java.util.List;

import javax.ejb.EJB;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.dcm4chee.xds2.repository.persistence.XdsDocument;
import org.dcm4chee.xds2.repository.persistence.XdsFileRef;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */
@RunWith(Arquillian.class)
public class SimpleXdsFileTest {
    private static final String TEXT_PLAIN = "text/plain";
    private static final String ONLINE_GROUP ="GRP_TEST_ONLINE";
    private static final String NEARLINE_GROUP ="GRP_TEST_NEARLINE";
    private static final String ONLINE_FILESYSTEM_1 = "online_fs_1";
    private static final String ONLINE_FILESYSTEM_2 = "online_fs_2";
    private static final String NEARLINE_FILESYSTEM_1 = "nearline_fs_1";
    private static final String NEARLINE_FILESYSTEM_2 = "nearline_fs_2";
    private static final String DOC_UID_1 = "1.2.3.4.5.0.0.1";
    private static final String DOC_UID_2 = "1.2.3.4.5.0.0.2";
    private static final String DOC_UID_3 = "1.2.3.4.5.0.0.3";
    private static final String DOC_UID_4 = "1.2.3.4.5.0.0.4";
    private static final String DIGEST_DUMMY = "DUMMY_digest";

    private final static Logger log = LoggerFactory.getLogger(SimpleXdsFileTest.class);

    @Deployment
    public static WebArchive createDeployment() {
        return XDSTestUtil.createDeploymentArchive(SimpleXdsFileTest.class);
    }

    @EJB (mappedName="java:global/test/XdsStorageTestBean")
    private XdsStorageTestBeanLocal testSession;


    @Test
    public void checkCreateFile()  {
        log.info("\n############################# TEST: check create XdsFile ############################");
        try {
            XdsFileRef f1 = testSession.createFile(ONLINE_GROUP, ONLINE_FILESYSTEM_1, DOC_UID_1, "checkCreateFile/online_doc1.txt", TEXT_PLAIN, 1024l, DIGEST_DUMMY);
        } finally {
            testSession.deleteGroup(ONLINE_GROUP);
        }
    }

    @Test
    public void checkCreateFiles()  {
        try {
            log.info("\n############################# TEST: check create multiple XdsFiles in different filesystems / groups ############################");
            XdsFileRef f1 = testSession.createFile(ONLINE_GROUP, ONLINE_FILESYSTEM_1, DOC_UID_1, "checkCreateFile/online_doc1.txt", TEXT_PLAIN, 1024l, DIGEST_DUMMY);
            XdsFileRef f2 = testSession.createFile(ONLINE_GROUP, ONLINE_FILESYSTEM_2, DOC_UID_2, "checkCreateFile/online_doc2.txt", TEXT_PLAIN, 1024l, DIGEST_DUMMY);
            XdsFileRef f3 = testSession.createFile(NEARLINE_GROUP, NEARLINE_FILESYSTEM_1, DOC_UID_3, "checkCreateFile/nearline_doc1.txt", TEXT_PLAIN, 1024, DIGEST_DUMMY);
            XdsFileRef f4 = testSession.createFile(NEARLINE_GROUP, NEARLINE_FILESYSTEM_1, DOC_UID_4, "checkCreateFile/nearline_doc2.txt", TEXT_PLAIN, 1024, DIGEST_DUMMY);
            XdsFileRef f5 = testSession.createFile(NEARLINE_GROUP, NEARLINE_FILESYSTEM_2, DOC_UID_4, "checkCreateFile/nearline_doc3.txt", TEXT_PLAIN, 1024, DIGEST_DUMMY);
            XdsDocument doc = testSession.findDocument(DOC_UID_1);
            assertNotNull("Document_1 not found", doc);
            List<XdsFileRef> fileRefs = testSession.findFileRefs(DOC_UID_1);
            assertEquals("findFileRefs(document_1) should return one file", 1, fileRefs.size());
            assertEquals("findFileRefs for Document_1", f1, fileRefs.get(0));
            
            fileRefs = testSession.findFileRefs(DOC_UID_4);
            assertEquals("findFileRefs (document_4) should return two file", 2, fileRefs.size());
            assertTrue("findFileRefs (document_4) should contain file "+f4, fileRefs.contains(f4));
            assertTrue("findFileRefs (document_4) should contain file "+f5, fileRefs.contains(f5));
        } finally {
            testSession.deleteGroup(ONLINE_GROUP);
            testSession.deleteGroup(NEARLINE_GROUP);
        }
    }

}