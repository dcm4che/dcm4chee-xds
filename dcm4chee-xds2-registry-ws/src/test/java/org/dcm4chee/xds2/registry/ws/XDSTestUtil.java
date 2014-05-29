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



import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.code.AffinityDomainCodes;
import org.dcm4chee.xds2.common.code.Code;
import org.dcm4chee.xds2.conf.XdsRegistry;
import org.dcm4chee.xds2.infoset.rim.ClassificationNodeType;
import org.dcm4chee.xds2.infoset.rim.ClassificationSchemeType;
import org.dcm4chee.xds2.infoset.rim.ClassificationType;
import org.dcm4chee.xds2.infoset.rim.ExternalIdentifierType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.RegistryError;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectType;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.persistence.XDSCode;
import org.dcm4chee.xds2.registry.AuditTestManager;
import org.dcm4chee.xds2.registry.XdsTestServiceImpl;
import org.dcm4chee.xds2.registry.ws.XDSPersistenceWrapper;
import org.dcm4chee.xds2.registry.ws.XDSRegistryBean;
import org.dcm4chee.xds2.registry.ws.XDSRegistryBeanLocal;
import org.dcm4chee.xds2.registry.ws.XDSValidator;
import org.dcm4chee.xds2.service.XdsService;
import org.dcm4chee.xds2.ws.handler.LogHandler;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
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

    private static Set<String> ebXmlClassificationSchemeIds = null;
    private static Set<String> xdsClassificationSchemeIds = null;
    private static boolean oldCreateMissingPatient, oldCreateMissingCodes, oldCheckAffinity, oldPreMetadataCheck;
    
    private static XdsRegistry xdsRegistry;
    
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
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war")
        .addClasses(testClazz, XDSRegistryBeanLocal.class, XDSRegistryTestBeanI.class, XDSRegistryBean.class, XDSRegistryTestBean.class, XDSRegistryTestBeanException.class,
                XDSTestUtil.class, XDSPersistenceWrapper.class, XDSValidator.class, LogHandler.class, AuditTestManager.class, XdsService.class, XdsTestServiceImpl.class)
        .add(new FileAsset(new File("src/main/resources/org/dcm4chee/xds2/registry/ws/handlers.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/handlers.xml")
        .add(new FileAsset(new File("src/main/resources/META-INF/wsdl/XDS.b_DocumentRegistry.wsdl")),
                "META-INF/wsdl/XDS.b_DocumentRegistry.wsdl")
        .add(new FileAsset(new File("src/main/resources/META-INF/wsdl/schema/xml.xsd")),
                "META-INF/wsdl/schema/xml.xsd")
        .add(new FileAsset(new File("src/main/resources/META-INF/wsdl/schema/ebRS/cms.xsd")),
                "META-INF/wsdl/schema/ebRS/cms.xsd")
        .add(new FileAsset(new File("src/main/resources/META-INF/wsdl/schema/ebRS/lcm.xsd")),
                "META-INF/wsdl/schema/ebRS/lcm.xsd")
        .add(new FileAsset(new File("src/main/resources/META-INF/wsdl/schema/ebRS/query.xsd")),
                "META-INF/wsdl/schema/ebRS/query.xsd")
        .add(new FileAsset(new File("src/main/resources/META-INF/wsdl/schema/ebRS/rim.xsd")),
                "META-INF/wsdl/schema/ebRS/rim.xsd")
        .add(new FileAsset(new File("src/main/resources/META-INF/wsdl/schema/ebRS/rs.xsd")),
                "META-INF/wsdl/schema/ebRS/rs.xsd")
//We need the schema files also in WEB-INF (cross referenced -> not found. Why? ??!!)                
        .add(new FileAsset(new File("src/main/resources/META-INF/wsdl/schema/ebRS/lcm.xsd")),
                "WEB-INF/wsdl/schema/ebRS/lcm.xsd")
        .add(new FileAsset(new File("src/main/resources/META-INF/wsdl/schema/ebRS/rim.xsd")),
                "WEB-INF/wsdl/schema/ebRS/rim.xsd")
        .add(new FileAsset(new File("src/main/resources/META-INF/wsdl/schema/ebRS/rs.xsd")),
                "WEB-INF/wsdl/schema/ebRS/rs.xsd")
        .add(new FileAsset(new File("src/main/resources/META-INF/wsdl/schema/ebRS/query.xsd")),
                "WEB-INF/wsdl/schema/ebRS/query.xsd")
        .add(new FileAsset(new File("src/main/resources/META-INF/wsdl/schema/xml.xsd")),
                "WEB-INF/wsdl/schema/xml.xsd")
        .add(new FileAsset(new File("src/test/resources/WEB-INF/beans.xml")),
                "WEB-INF/beans.xml")
        .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/initialize.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/initialize.xml")
        .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/SubmitObjectsRequest_AssociationTypeScheme.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/SubmitObjectsRequest_AssociationTypeScheme.xml") 
        .add(new FileAsset(new File("src/test/resources/org/dcm4chee/xds2/registry/ws/testCodeClassifications.xml")), 
                "WEB-INF/classes/org/dcm4chee/xds2/registry/ws/testCodeClassifications.xml") 
        .addAsWebResource(new ByteArrayAsset(new byte[0]), ArchivePaths.create("WEB-INF/beans.xml"))
        .addAsLibraries(Maven.resolver().offline().resolve("org.dcm4che:dcm4chee-xds2-infoset:"+version).withoutTransitivity().as(File.class))
        .addAsLibraries(Maven.resolver().offline().resolve("org.dcm4che:dcm4chee-xds2-common:"+version).withoutTransitivity().asSingle(File.class))
        .addAsLibraries(Maven.resolver().offline().resolve("org.dcm4che:dcm4chee-xds2-conf:"+version).withoutTransitivity().asSingle(File.class))
        //.addAsLibraries(Maven.resolver().offline().resolve("org.dcm4che:dcm4chee-xds2-service:"+version).withoutTransitivity().asSingle(File.class))
        .addAsLibraries(Maven.resolver().offline().resolve("org.dcm4che:dcm4chee-xds2-registry-persistence:jar:" + System.getProperty("db") + ":"+version).withTransitivity().asFile());

        war.addAsManifestResource(new FileAsset(new File("src/test/resources/META-INF/MANIFEST.MF")), "MANIFEST.MF");
        return war;
    }
    
    public static SubmitObjectsRequest getSubmitObjectsRequest(String metadataFilename) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(SubmitObjectsRequest.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        SubmitObjectsRequest req = (SubmitObjectsRequest) unmarshaller.unmarshal(
                XDSTestUtil.class.getResourceAsStream(metadataFilename));
        return req;
    }

    public static String getExternalIdentifierValue(List<ExternalIdentifierType> eit, String idScheme) {
        if (eit != null) {
            for (int i=0, len=eit.size() ; i < len ; i++) {
                if (idScheme.equals(eit.get(i).getIdentificationScheme())) {
                    return eit.get(i).getValue();
                }
            }
        }
        return null;
    }

    public static String setExternalIdentifierValue(List<ExternalIdentifierType> eit, 
            String idScheme, String value) {
        if (eit != null) {
            ExternalIdentifierType ei;
            for (int i=0, len=eit.size() ; i < len ; i++) {
                ei = eit.get(i);
                if (idScheme.equals(ei.getIdentificationScheme())) {
                    String oldValue = ei.getValue();
                    ei.setValue(value);
                    return oldValue;
                }
            }
        }
        return null;
    }

    public static ExternalIdentifierType removeExternalIdentifier(List<ExternalIdentifierType> eit, 
            String idScheme) {
        ExternalIdentifierType ei;
        for (Iterator<ExternalIdentifierType> iter=eit.iterator() ; iter.hasNext() ;) {
            ei = iter.next();
            if (idScheme.equals(ei.getIdentificationScheme())) {
                iter.remove();
                return ei;
            }
        }
        return null;
    }
    
    public static String setSlotValue(List<SlotType1> slots, String slotName, String value) {
        for (SlotType1 slot : slots) {
            if (slot.getName().equals(slotName)) {
                return slot.getValueList().getValue().set(0, value);
            }
        }
        return null;
    }
    public static String getSlotValue(List<SlotType1> slots, String slotName) {
        for (SlotType1 slot : slots) {
            if (slot.getName().equals(slotName)) {
                return slot.getValueList().getValue().get(0);
            }
        }
        return null;
    }

    public static SlotType1 removeSlot(List<SlotType1> slots, String slotName) {
        SlotType1 slot;
        for (Iterator<SlotType1> iter = slots.iterator() ; iter.hasNext() ; ) {
            slot = iter.next();
            if (slot.getName().equals(slotName)) {
                iter.remove();
                return slot;
            }
        }
        return null;
    }

    public static List<ClassificationType> removeCode(List<ClassificationType> clList, String codeType) {
        ArrayList<ClassificationType> l = new ArrayList<ClassificationType>(); 
        for (ClassificationType cl : clList) {
            if (cl.getClassificationScheme().equals(codeType)) {
                l.add(cl);
            }
        }
        clList.removeAll(l);
        return l;
    }


    public static String toErrorMsg(RegistryResponseType rsp) {
        StringBuilder sb = new StringBuilder();
        List<RegistryError> errors = rsp.getRegistryErrorList().getRegistryError();
        for ( RegistryError err : errors) {
            sb.append(err.getErrorCode()).append(" - ")
            .append(err.getCodeContext()).append(" Severity:")
            .append(err.getSeverity());
        }
        return sb.toString();
    }

    public static void prepareTests(XDSRegistryBean session, Logger log) {
        long t1 = System.currentTimeMillis();
        log.info("\n################################# Prepare Tests #################################");
        if (session.getRegistryObjectByUUID("urn:oasis:names:tc:ebxml-regrep:classificationScheme:AssociationType") == null) {
            log.info("#### prepare with ebXML ClassificationSchemes!");
            ebXmlClassificationSchemeIds = prepareRegistryWithSubmissionRequest(session, 
                    "SubmitObjectsRequest_AssociationTypeScheme.xml", log);
        }
        if (session.getRegistryObjectByUUID("urn:uuid:96fdda7c-d067-4183-912e-bf5ee74998a8") == null) {
            log.info("#### prepare with XDS ClassificationSchemes!");
            xdsClassificationSchemeIds = prepareRegistryWithSubmissionRequest(session, 
                    "initialize.xml", log);
        }
        xdsRegistry = session.getXdsRegistryConfig();
        oldCreateMissingPatient = xdsRegistry.isCreateMissingPIDs();
        oldCreateMissingCodes = xdsRegistry.isCreateMissingCodes();
        oldCheckAffinity = xdsRegistry.isCheckAffinityDomain();
        oldPreMetadataCheck = xdsRegistry.isPreMetadataCheck();
        xdsRegistry.setCreateMissingCodes(true);
        xdsRegistry.setCreateMissingPIDs(true);
        xdsRegistry.setCheckAffinityDomain(false);
        xdsRegistry.setPreMetadataCheck(false);
        XDSTestUtil.prepareTestPatients(session, log);
        XDSTestUtil.prepareTestCodes(session, log);
        xdsRegistry.setCreateMissingCodes(false);
        xdsRegistry.setCreateMissingPIDs(false);
        log.info("\n###### PREPARE done in "+(System.currentTimeMillis()-t1)+"ms ######");
    }

    public static void clearDB(XDSRegistryTestBeanI testSession, Logger log) {
        long t1 = System.currentTimeMillis();
        log.info("\n################################# CLEAR DB #################################");
        testSession.removeAllIdentifiables("urn:uuid:aabbccdd-bdda");
        if (xdsClassificationSchemeIds != null) {
            log.info("remove XDS ClassificationSchemes");
            testSession.removeAllIdentifiables(xdsClassificationSchemeIds);
        }
        if (ebXmlClassificationSchemeIds != null) {
            log.info("remove ebXML ClassificationSchemes");
            testSession.removeAllIdentifiables(ebXmlClassificationSchemeIds);
        }
        log.info("remove test patients");
        testSession.removeTestPatients(TEST_PID_MERGED);
        testSession.removeTestPatients(TEST_PID_1, TEST_PID_2);
        testSession.removeTestPatients(CONCURRENT_PATID);
        
        log.info("remove test issuer");
        testSession.removeTestIssuerByNamespaceId("dcm4che_test");
        log.info("remove test XDSCodes");
        testSession.removeXDSCodes();
        log.info("\n###### CLEAR done in "+(System.currentTimeMillis()-t1)+"ms ######");
        xdsRegistry.setCreateMissingCodes(oldCreateMissingPatient);
        xdsRegistry.setCreateMissingPIDs(oldCreateMissingCodes);
        xdsRegistry.setCheckAffinityDomain(oldCheckAffinity);
        xdsRegistry.setPreMetadataCheck(oldPreMetadataCheck);
    }


    public static void prepareTestPatients(XDSRegistryBean session, Logger log) {
        log.info("#### Prepare Test patients");
        try {
            session.getPatient(XDSTestUtil.TEST_PID_1+XDSTestUtil.TEST_ISSUER, true);
            session.getPatient(XDSTestUtil.TEST_PID_2+XDSTestUtil.TEST_ISSUER, true);
            session.getPatient(XDSTestUtil.TEST_PID_MERGED+XDSTestUtil.TEST_ISSUER, true);
        } catch (Exception x) {
            log.error("Failed to add test patients!");
        }
    }
    
    public static void prepareTestCodes(XDSRegistryBean session, Logger log) {
        log.info("#### Prepare Test codes");
        try {
            SubmitObjectsRequest req = getSubmitObjectsRequest("testCodeClassifications.xml");
            RegistryObjectType obj = (RegistryObjectType) req.getRegistryObjectList().getIdentifiable().get(0).getValue();
            AffinityDomainCodes adCodes = new AffinityDomainCodes();
            for (ClassificationType clType : obj.getClassification()) {
                XDSCode code = session.getXDSCode(clType, true);
                Code c = new Code(code.getCodeValue(), code.getCodingSchemeDesignator(), code.getCodeMeaning());
                adCodes.addCode(code.getCodeClassification(), code.getCodeClassification(), c);
            }
            session.getXdsRegistryConfig().setAffinityDomainConfigDir("dummy");
            session.getXdsRegistryConfig().getCodeRepository().addAffinityDomainCodes("default", adCodes);
        } catch (Exception x) {
            log.error("Failed to add test codes!", x);
        }
    }

    private static Set<String> prepareRegistryWithSubmissionRequest(XDSRegistryBean session, String metadataFilename, Logger log) {
        try {
            SubmitObjectsRequest req = getSubmitObjectsRequest(metadataFilename);
            RegistryResponseType rsp = session.documentRegistryRegisterDocumentSetB(req);
            if (XDSConstants.XDS_B_STATUS_SUCCESS.equals(rsp.getStatus())) {
               List<JAXBElement<? extends IdentifiableType>> objs = req.getRegistryObjectList().getIdentifiable();
               HashSet<String> ids = new HashSet<String>(objs.size());
               for (JAXBElement<? extends IdentifiableType> obj : objs) {
                   ids.add(obj.getValue().getId());
                   if (obj.getValue() instanceof ClassificationNodeType) {
                       addClassificationNodeIds(
                               ((ClassificationNodeType) obj.getValue()).getClassificationNode(), ids);
                   } else if (obj.getValue() instanceof ClassificationSchemeType) {
                       addClassificationNodeIds(
                               ((ClassificationSchemeType) obj.getValue()).getClassificationNode(), ids);
                   }
               }
               return ids;
            }
        } catch (Exception x) {
            log.error("prepareRegistryWithSubmissionRequest failed for:"+metadataFilename);
        }
        return null;
    }
    private static void addClassificationNodeIds(List<ClassificationNodeType> nodes, Set<String> ids) {
        if (nodes != null && nodes.size() > 0) {
            for (ClassificationNodeType n : nodes) {
                ids.add(n.getId());
                addClassificationNodeIds(n.getClassificationNode(), ids);
            }
        }
    }
    
    private static String resolveMavenConfigFile() {
		String filePath = null;
		Assert.assertTrue(new File(filePath = 
				System.getenv().get("M2_HOME") != null ?
						System.getenv().get("M2_HOME") + "/conf/settings.xml" :
						System.getenv().get("HOME") + "/.m2/settings.xml"
				).exists());
    	return filePath;
    }
}