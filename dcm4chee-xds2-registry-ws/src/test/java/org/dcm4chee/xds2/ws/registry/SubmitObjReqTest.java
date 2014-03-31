package org.dcm4chee.xds2.ws.registry;
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



import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.InputStream;

import javax.ejb.EJB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.ws.registry.XDSRegistryBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */
@RunWith(Arquillian.class)
public class SubmitObjReqTest {
    
    private final static Logger log = LoggerFactory.getLogger(SubmitObjReqTest.class);

    @Deployment
    public static WebArchive createDeployment() {
        return XDSTestUtil.createDeploymentArchive(SubmitObjReqTest.class)
            .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/ws/registry/initialize.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/ws/registry/initialize.xml")
            .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/ws/registry/SubmitObjectsRequest_AssociationTypeScheme.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/ws/registry/SubmitObjectsRequest_AssociationTypeScheme.xml"); 
    }
    @EJB
    private XDSRegistryBean session;

    @After
    public void clearDB() {
        long t1 = System.currentTimeMillis();
        log.info("\n################################# CLEAR DB #################################");
        log.info("\n###### CLEAR done in "+(System.currentTimeMillis()-t1)+"ms ######");
    }

    @Test
    public void storeAssocTypes() throws Exception {
        log.info("\n############################# TEST: storeAssocTypes ############################");
        JAXBContext jaxbContext = JAXBContext.newInstance(SubmitObjectsRequest.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        InputStream is = this.getClass().getResourceAsStream("SubmitObjectsRequest_AssociationTypeScheme.xml");
        log.info("###### InputStream is:"+is);
        SubmitObjectsRequest req = (SubmitObjectsRequest) unmarshaller.unmarshal(is);
        assertNotNull("SubmitObjectsRequest:", req);
        assertNotNull("XDSRegistryBean:", session);
        RegistryResponseType rsp = session.documentRegistryRegisterDocumentSetB(req);
        assertNotNull("RegistryResponseType:", rsp);
    }
    
    @Test
    public void storeSubmitObjectsRequest() throws Exception {
        log.info("\n############################# TEST:SubmitObjectsRequest ############################");
        JAXBContext jaxbContext = JAXBContext.newInstance(SubmitObjectsRequest.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        SubmitObjectsRequest req = (SubmitObjectsRequest) unmarshaller.unmarshal(
                this.getClass().getResourceAsStream("initialize.xml"));
        assertNotNull("SubmitObjectsRequest:", req);
        assertNotNull("XDSRegistryBean:", session);
        RegistryResponseType rsp = session.documentRegistryRegisterDocumentSetB(req);
        assertNotNull("RegistryResponseType:", rsp);
        
    }

}
