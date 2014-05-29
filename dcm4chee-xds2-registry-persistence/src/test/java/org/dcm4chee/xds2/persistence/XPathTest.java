package org.dcm4chee.xds2.persistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.persistence.RegistryObject.XDSSearchIndexKey;
import org.hibernate.mapping.Collection;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

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
        
        JAXBElement<ExtrinsicObjectType> docEntryEl = (JAXBElement<ExtrinsicObjectType>) req.getRegistryObjectList().getIdentifiable().get(0);
        JAXBElement<RegistryPackageType> submsetEl = (JAXBElement<RegistryPackageType>) req.getRegistryObjectList().getIdentifiable().get(1);
        ExtrinsicObjectType docEntry = docEntryEl.getValue();
        RegistryPackageType submset = submsetEl.getValue();
        log.debug("Id {}, ", docEntry.getId());
        
        ////// doc entry //////
        JXPathContext docEntryContext = JXPathContext.newContext(docEntry);
        
        Iterator author = (Iterator) docEntryContext.iterate(RegistryObject.INDEX_XPATHS.get(XDSSearchIndexKey.DOCUMENT_ENTRY_AUTHOR)); 
        Assert.assertArrayEquals(new String[] {"^Smitty^Gerald^^^","^Dopplemeyer^Sherry^^^"}, toArray(author));

        ////// submission set //////
        JXPathContext submSetContext = JXPathContext.newContext(submset);
        
        String srcid = (String) submSetContext.getValue(RegistryObject.INDEX_XPATHS.get(XDSSearchIndexKey.SUBMISSION_SET_SOURCE_ID));
        Assert.assertEquals("1.3.6.1.4.1.21367.13.2250", srcid);
        
        Iterator ssauthor = (Iterator) submSetContext.iterate(RegistryObject.INDEX_XPATHS.get(XDSSearchIndexKey.SUBMISSION_SET_AUTHOR)); 
        Assert.assertArrayEquals(new String[] {"^Dopplemeyer^Sherry^^^"}, toArray(ssauthor));
        
    }

    
    private String[] toArray(Iterator author) {
        
        ArrayList<String> l = new ArrayList<>();
        while (author.hasNext()) {
            l.add((String) author.next());
        }   
        return l.toArray(new String[] {});

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
