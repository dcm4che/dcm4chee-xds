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



import java.io.File;
import java.lang.reflect.Method;
import java.util.Properties;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.slf4j.Logger;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */
public class XDSTestUtil {

    public static final String TEST_ISSUER ="^^^dcm4che_test&1.2.3.45.4.3.2.1&ISO";
    public static final String TEST_PID_1 = "test1234_1";
    public static final String TEST_PID_2 = "test1234_2";
    public static final String TEST_PID_MERGED = "test1234_merged";
    public static final String CONCURRENT_PATID = "concurrentTestPatId";

    @SuppressWarnings("rawtypes")
    public static int getNumberOfTestMethods(Class clazz) {
        int count = 0;
        Method[] methods = clazz.getMethods(); 
        for (int i = 0 ; i < methods.length ; i++) {
            if (methods[i].getAnnotation(Test.class) != null)
                count++;
        }
        return count;
    }

    public static WebArchive createDeploymentArchive(@SuppressWarnings("rawtypes") Class testClazz) {
        Properties p = new Properties();
        try {
            p.load(XDSTestUtil.class.getResourceAsStream("/version.properties"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load version.properties!", e);
        }
        String version = p.getProperty("version");
        String storageVersion = p.getProperty("storage-version");
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war")
                .addClasses(testClazz,  XdsStorageTestBeanLocal.class, XdsStorageTestBean.class, 
                        XDSTestUtil.class)
                        .addAsLibraries(Maven.resolver().offline().resolve("org.dcm4che:dcm4chee-xds2-repository-entity:jar:" 
                                + System.getProperty("db") + ":"+version).withoutTransitivity().as(File.class))
                                .addAsLibraries(Maven.resolver().offline().resolve("org.dcm4che:dcm4chee-storage-entity:jar:" 
                                        + System.getProperty("db") + ":"+storageVersion).withoutTransitivity().as(File.class));

        war.addAsManifestResource(new FileAsset(new File("src/test/resources/META-INF/MANIFEST.MF")), "MANIFEST.MF");
        return war;
    }


    public static void clearDB(XdsStorageTestBeanLocal testSession, Logger log) {
        long t1 = System.currentTimeMillis();
        log.info("\n################################# CLEAR DB #################################");
        log.info("\n###### CLEAR done in "+(System.currentTimeMillis()-t1)+"ms ######");
    }

}