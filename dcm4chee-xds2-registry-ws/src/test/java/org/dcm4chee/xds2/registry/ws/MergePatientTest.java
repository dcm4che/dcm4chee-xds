package org.dcm4chee.xds2.registry.ws;
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



import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.List;

import javax.ejb.EJB;
import javax.xml.bind.JAXBElement;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.AssociationType1;
import org.dcm4chee.xds2.infoset.rim.ClassificationType;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectType;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.persistence.RegistryPackage;
import org.dcm4chee.xds2.persistence.XDSDocumentEntry;
import org.dcm4chee.xds2.registry.AuditTestManager;
import org.dcm4chee.xds2.registry.ws.XDSRegistryBean;
import org.dcm4chee.xds2.registry.ws.query.StoredQuery;
import org.dcm4chee.xds2.registry.ws.sq.AbstractSQTests;
import org.dcm4chee.xds2.registry.ws.sq.QueryResultCheck;
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

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */
@RunWith(Arquillian.class)
public class MergePatientTest extends AbstractSQTests {
    private final static Logger log = LoggerFactory.getLogger(MergePatientTest.class);

    private static final SlotType1[] PARAMS_PAT1 = new SlotType1[] {
        toQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_PATIENT_ID, TEST_PAT_ID),
        toQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_STATUS, "('"+XDSConstants.STATUS_APPROVED+"')")
    };
    private static final SlotType1[] PARAMS_PAT2 = new SlotType1[] {
        toQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_PATIENT_ID, TEST_PAT_ID2),
        toQueryParam(XDSConstants.QRY_DOCUMENT_ENTRY_STATUS, "('"+XDSConstants.STATUS_APPROVED+"')")
    };

    @Deployment
    public static WebArchive createDeployment() {
        return XDSTestUtil.createDeploymentArchive(MergePatientTest.class)
            .addPackage(StoredQuery.class.getPackage())
            .addPackage(AbstractSQTests.class.getPackage())
        	.add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/RegisterOneDocument.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/RegisterOneDocument.xml") 
            .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/RegisterAssocBeforeDoc.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/RegisterAssocBeforeDoc.xml") 
            .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/RegisterTwoDocuments.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/RegisterTwoDocuments.xml")
            .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/CreateFolderWithDocument.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/CreateFolderWithDocument.xml"); 
    }
    @EJB
    private XDSRegistryBean session;
    
    @EJB
    private XDSRegistryTestBean testSession;

    // Audit logger	testing 
    @Before
    public void prepareAuditLogger() {
    	AuditTestManager.prepareAuditLogger(); 
    }
    @After
    public void checkAudits() 
    {
    	AuditTestManager.checkAudits();
    }
    
    
    @Before
    public void prepare() {
        XDSTestUtil.prepareTests(session, log);
    }

    @After
    public void clearDB() {
        XDSTestUtil.clearDB(testSession, log);
    }

    @Test
    public void checkMerge() throws Exception {
        doRegisterDocumentTest("RegisterOneDocument.xml");//pat1
        doRegisterDocumentTest("RegisterAssocBeforeDoc.xml");//pat1
        doRegisterDocumentTest("RegisterTwoDocuments.xml");//pat2
        doRegisterDocumentTest("CreateFolderWithDocument.xml");//pat1
        checkDocuments(PARAMS_PAT1, 3);
        checkDocuments(PARAMS_PAT2, 2);
        session.mergePatient(removeStringvalueMarker(TEST_PAT_ID), removeStringvalueMarker(TEST_PAT_ID2));
        checkDocuments(PARAMS_PAT1, 0);
        checkDocuments(PARAMS_PAT2, 5);
    }
    
	private void checkDocuments(SlotType1[] params, int nrOfDocs) {
		AdhocQueryRequest req = getQueryRequest(XDSConstants.XDS_FindDocuments, XDSConstants.QUERY_RETURN_TYPE_LEAF, params);
        AdhocQueryResponse rsp = session.documentRegistryRegistryStoredQuery(req);
        QueryResultCheck chk = new QueryResultCheck();
        chk.setNrOfDocs(nrOfDocs).setNrOfFolders(0).setNrOfSubmissions(0).setNrOfAssocs(0);
        chk.checkResponse(rsp);
	}


    private void doRegisterDocumentTest(String filename) throws Exception {
        log.info("Prepare with "+filename);
        long t1 = System.currentTimeMillis();
        SubmitObjectsRequest req = XDSTestUtil.getSubmitObjectsRequest(filename);
        RegistryResponseType rsp = null;
        try {
            rsp = session.documentRegistryRegisterDocumentSetB(req);
            if (XDSConstants.XDS_B_STATUS_FAILURE.equals(rsp.getStatus())) {
                fail("Preparation (Register document) failed! Error:"+XDSTestUtil.toErrorMsg(rsp));
            }
        } catch (Exception x) {
            fail("Register document failed unexpected! Error:"+x);
        }
        log.info("\n###### "+filename+" done in "+(System.currentTimeMillis()-t1)+"ms ######");
    }

    private String removeStringvalueMarker(String s) {
    	return s.substring(1, s.length()-1);
    }
}