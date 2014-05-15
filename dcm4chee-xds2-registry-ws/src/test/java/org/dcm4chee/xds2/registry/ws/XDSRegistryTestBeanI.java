package org.dcm4chee.xds2.registry.ws;

import java.util.Set;
import java.util.concurrent.Semaphore;

import javax.persistence.EntityManager;
import javax.xml.bind.JAXBException;

import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.infoset.rim.AssociationType1;
import org.dcm4chee.xds2.infoset.rim.ClassificationType;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.persistence.RegistryObject;
import org.dcm4chee.xds2.persistence.XADPatient;
import org.dcm4chee.xds2.persistence.XDSDocumentEntry;
import org.dcm4chee.xds2.persistence.XDSFolder;
import org.dcm4chee.xds2.persistence.XDSSubmissionSet;

public interface XDSRegistryTestBeanI {

    public RegistryObject getRegistryObjectByUUID(String id);

    public EntityManager getEm();

    public XDSDocumentEntry getDocumentEntryByUUID(String uuid);

    public XDSSubmissionSet getSubmissionSetByUUID(String uuid);

    public XDSFolder getFolderByUUID(String uuid);

    public void checkExtrinsicObjectType(ExtrinsicObjectType obj) throws XDSRegistryTestBeanException;

    public void checkRegistryPackage(RegistryPackageType obj, boolean isSubmissionSet) throws XDSRegistryTestBeanException;

    public void checkClassification(ClassificationType obj) throws XDSRegistryTestBeanException;

    public void checkAssociation(AssociationType1 obj) throws XDSRegistryTestBeanException;

    public void removeTestPatients(String... patIds);

    public void removeTestIssuerByNamespaceId(String namespaceId);

    public void removeAllIdentifiables(String baseID);

    public void removeAllIdentifiables(Set<String> ids);

    public void removeXDSCodes();

    public Long getTotalIdentifiablesCount();

    public long getConcurrentPatientRecordsNum();

    void concurrentRegister(Semaphore masterSemaphore, Semaphore childrenSemaphore, SubmitObjectsRequest sor) throws InterruptedException,
            XDSException;

}