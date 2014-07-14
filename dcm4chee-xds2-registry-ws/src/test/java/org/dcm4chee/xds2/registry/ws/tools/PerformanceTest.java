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
 * Portions created by the Initial Developer are Copyright (C) 2014
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
/**
 * @author Roman K
 */

package org.dcm4chee.xds2.registry.ws.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;

import javax.ejb.EJB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.dcm4chee.xds2.common.XDSUtil;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.registry.ws.CheckErrorsTest;
import org.dcm4chee.xds2.registry.ws.XDSRegistryBean;
import org.dcm4chee.xds2.registry.ws.XDSRegistryTestBeanI;
import org.dcm4chee.xds2.registry.ws.XDSTestUtil;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.common.io.Resources;

@RunWith(Arquillian.class)
public class PerformanceTest {

        private final static Logger log = LoggerFactory.getLogger(PerformanceTest.class);

        private static final String DOC_NAME = "RegisterGenericDocument.xml";
        
        @Deployment
        public static WebArchive createDeployment() {
            return XDSTestUtil.createDeploymentArchive(CheckErrorsTest.class)
                .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/"+DOC_NAME)), 
                    "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/"+DOC_NAME); 
                    
        }
        @EJB
        private XDSRegistryBean session;
        
        @EJB
        private XDSRegistryTestBeanI testSession;
        
        
        @Before
        public void prepare() {
                XDSTestUtil.prepareTests(session, log);
        }

        @After
        public void clearDB() {
               //XDSTestUtil.clearDB(testSession, log);
        }

        private String fileContent = null;
        private Unmarshaller unmarshaller = null;
        
        private SubmitObjectsRequest getParsedSubmitObjectsRequest(String uuidPrefix, String patientId, String submSetUid, String docEntryUid) throws JAXBException, IOException {

           
            if (fileContent == null) {
                InputStream stream = PerformanceTest.class.getResourceAsStream(DOC_NAME);
                fileContent = CharStreams.toString(new InputStreamReader(stream, Charsets.UTF_8));
                //Closeables.closeQuietly(stream);
            }

            if (unmarshaller == null) {
                JAXBContext jaxbContext = JAXBContext.newInstance(SubmitObjectsRequest.class);
                unmarshaller = jaxbContext.createUnmarshaller();
            }

            
            String doc = fileContent
                    .replaceAll("UUIDPREFIX", uuidPrefix)
                    .replaceAll("DOCUMENTENTRYUID", docEntryUid)
                    .replaceAll("MYPATIENTID", patientId)
                    .replaceAll("SUBMISSIONSETUID", submSetUid);
            
            return (SubmitObjectsRequest) unmarshaller.unmarshal(new StringReader(doc));            
        }
        
        @Test 
        public void check1000DocsPerPatient() throws JAXBException, IOException {
            String patId = "test1234_1^^^&amp;1.2.3.45.4.3.2.1&amp;ISO"; 
            for (int i=1000;i<=1100;i++) {
                SubmitObjectsRequest req = getParsedSubmitObjectsRequest("aaaa-"+i, patId, "1.42.20120430135820."+i, "1.42.20120430135821."+i);
                session.documentRegistryRegisterDocumentSetB(req);
                log.info("Registered {}th submission",i);
            }
        }
        
}
