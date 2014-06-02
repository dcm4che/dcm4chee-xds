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


import javax.ejb.EJB;

import org.dcm4chee.storage.entity.Availability;
import org.dcm4chee.storage.entity.FileSystem;
import org.dcm4chee.storage.entity.FileSystemStatus;
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
    private static final String FILE_TEST_ONLINE_FS1 = "file://test/junit/fs1";
    private static final String FILE_TEST_ONLINE_FS2 = "file://test/junit/fs2";
    private static final String FILE_TEST_NEARLINE_FS1 = "file://test/nearline/n_fs1";
    private static final String FILE_TEST_NEARLINE_FS2 = "file://test/nearline/n_fs2";
	private static final String FS_GROUP_ID_ONLINE = "TEST_ONLINE";
	private static final String FS_GROUP_ID_NEARLINE = "TEST_NEARLINE";
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
        FileSystem fs = testSession.createFileSystem(FS_GROUP_ID_ONLINE, FILE_TEST_ONLINE_FS1, Availability.ONLINE,  FileSystemStatus.RW);
        XdsFileRef f = testSession.createFile("checkCreateFile/doc1.txt", "text/plain", 1024, "dummy", fs);
        testSession.deleteFsGroup(FS_GROUP_ID_ONLINE);
    }

    @Test
    public void checkCreateFiles()  {
        log.info("\n############################# TEST: check create multiple XdsFiles in different filesystems / groups ############################");
        FileSystem fs1 = testSession.createFileSystem(FS_GROUP_ID_ONLINE, FILE_TEST_ONLINE_FS1, Availability.ONLINE,  FileSystemStatus.RW);
        FileSystem fs2 = testSession.createFileSystem(FS_GROUP_ID_ONLINE, FILE_TEST_ONLINE_FS1, Availability.ONLINE,  FileSystemStatus.Rw);
        testSession.linkFileSystems(fs1, fs2);
        XdsFileRef f1 = testSession.createFile("checkCreateFile/online_doc1.txt", "text/plain", 1024, "dummy1", fs1);
        XdsFileRef f2 = testSession.createFile("checkCreateFile/online_doc2.txt", "text/plain", 1024, "dummy2", fs2);
        FileSystem fs3 = testSession.createFileSystem(FS_GROUP_ID_NEARLINE, FILE_TEST_ONLINE_FS1, Availability.NEARLINE,  FileSystemStatus.RO);
        FileSystem fs4 = testSession.createFileSystem(FS_GROUP_ID_NEARLINE, FILE_TEST_ONLINE_FS2, Availability.NEARLINE,  FileSystemStatus.RO);
        XdsFileRef f3 = testSession.createFile("checkCreateFile/nearline_doc1.txt", "text/plain", 1024, "dummy1", fs3);
        XdsFileRef f4 = testSession.createFile("checkCreateFile/nearline_doc2.txt", "text/plain", 1024, "dummy2", fs3);
        XdsFileRef f5 = testSession.createFile("checkCreateFile/nearline_doc3.txt", "text/plain", 1024, "dummy2", fs4);
        testSession.deleteFsGroup(FS_GROUP_ID_ONLINE);
        testSession.deleteFsGroup(FS_GROUP_ID_NEARLINE);
   }
 
}