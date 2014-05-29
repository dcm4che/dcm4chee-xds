package org.dcm4chee.xds2.persistence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.jxpath.JXPathContext;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.persistence.RegistryObject.XDSSearchIndexKey;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XPathTest {

    private static Logger log = LoggerFactory.getLogger(XPathTest.class);
    
    @Test
    public void checkXpaths() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(SubmitObjectsRequest.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        SubmitObjectsRequest req = (SubmitObjectsRequest) unmarshaller.unmarshal(
                XPathTest.class.getResourceAsStream("RegisterOneDocument.xml")); 
        
/*        for (JAXBElement<? extends IdentifiableType> e : req.getRegistryObjectList().getIdentifiable()) {
            log.info("type is {}", e.getDeclaredType());
        }*/
        
        JAXBElement<ExtrinsicObjectType> el = (JAXBElement<ExtrinsicObjectType>) req.getRegistryObjectList().getIdentifiable().get(0);
        ExtrinsicObjectType obj = el.getValue();
        log.info("Id {}, ", obj.getId());
        
        JXPathContext context = JXPathContext.newContext(obj);
        String XDSDocUniqIdXpath = String.format("externalIdentifier[identificationScheme='%s']/value", XDSConstants.UUID_XDSDocumentEntry_uniqueId);
        
        String XDSDocEntryAuthorXpath = String.format("classification[classificationScheme='%s']/slot[name='%s']/valueList/value" ,XDSConstants.UUID_XDSDocumentEntry_author, XDSConstants.SLOT_NAME_AUTHOR_PERSON);
        String XDSSubmSetAuthorXpath = String.format("classification[classificationScheme='%s']/slot[name='%s']/valueList/value" ,XDSConstants.UUID_XDSSubmissionSet_autor, XDSConstants.SLOT_NAME_AUTHOR_PERSON);
        
        
        
        String uniqid = (String)context.getValue(XDSDocUniqIdXpath);
        Iterator author = (Iterator) context.iterate(XDSDocEntryAuthorXpath); 
        

//        List<ClassificationType> cl = (List<ClassificationType>) context.getValue(authorXpath1); 
        
        
        log.info("Uniq ID {}, ", uniqid);
        
        String auth;
        try {
        while (( auth = (String) author.next() ) != null) {
            log.info("Author {}, ", auth);
        }   
        } catch (Exception e) {
            // TODO: handle exception
        }
  //      log.info("Author* {} ", cl);
        
    }
    
    @Test
    public void equalsTest() {
        
        RegistryObjectIndex i1 = new RegistryObjectIndex();
        RegistryObjectIndex i2 = new RegistryObjectIndex();
        
        i1.setKey(XDSSearchIndexKey.DOCUMENT_ENTRY_AUTHOR);
        i2.setKey(XDSSearchIndexKey.DOCUMENT_ENTRY_AUTHOR);

        i1.setValue("Yeah!");
        i2.setValue("Yeah!");
        
        i1.setPk(0);
        i2.setPk(34);

        RegistryObject ro = new RegistryObject() {
        };
        
        
        i1.setSubject(ro);
        i2.setSubject(ro);
        
        Assert.assertEquals(i1, i2);
        
        List<RegistryObjectIndex> l1 = new ArrayList<>();
        List<RegistryObjectIndex> l2 = new ArrayList<>();
        
        l1.add(i1);
        l2.add(i2);
        
        l1.retainAll(l2);
        
        Assert.assertEquals(1, l1.size());
        
        Set<RegistryObjectIndex> hs1 = new HashSet<>();
        Set<RegistryObjectIndex> hs2 = new HashSet<>();
        
        hs1.add(i1);
        hs2.add(i2);
        
        hs2.retainAll(hs1);
        
        Assert.assertEquals(1, hs2.size());
    }
    
}
