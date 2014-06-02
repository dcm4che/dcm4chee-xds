package org.dcm4chee.xds2.tool;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.ObjectFactory;
import org.dcm4chee.xds2.infoset.rim.ResponseOptionType;
import org.dcm4chee.xds2.infoset.rim.SlotType1;
import org.dcm4chee.xds2.infoset.rim.ValueListType;
import org.dcm4chee.xds2.infoset.util.DocumentRegistryPortTypeFactory;
import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryPortType;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Reg2RegMigration {

    private static Logger log = LoggerFactory.getLogger(Reg2RegMigration.class);

    @Test
    public void migrateDataAsTest() {
        String[] patids = {"roman^^^&1.2.3.44.55&ISO"};
        //XDS-Pat1^^^&1.2.3.4.5.99&ISO
        //John^^^&1.2.3.4.5&ISO
        //http://10.231.161.57:8180/dcm4chee-xds/XDSbRegistry/b
        //http://localhost:8080/xds/registry
        migrateData(Arrays.asList(patids), "http://10.231.161.57:8180/dcm4chee-xds/XDSbRegistry/b", "");
    }

    public void migrateData(List<String> patientIds, String reg1QueryEndpointURL, String reg2RegisterEndpointURL) {

        ObjectFactory objectFactory = new ObjectFactory();

        DocumentRegistryPortType port = DocumentRegistryPortTypeFactory.getDocumentRegistryPortSoap12(reg1QueryEndpointURL);

        for (String pId : patientIds) {
                log.info("Getting list of submission sets for patient {} ..", pId);

                AdhocQueryResponse rsp = port.documentRegistryRegistryStoredQuery(getAllSubmissionSetsRequest(pId));
                
                for (JAXBElement<? extends IdentifiableType> elem : rsp.getRegistryObjectList().getIdentifiable()) {
                    IdentifiableType i = elem.getValue();
                    log.info("identifiable with id {} ", i.getId());
                };
                
        }

    }
    
    
    private AdhocQueryRequest getSubmissionSetRequest(String pId) {
        return null;
    }
    
    private AdhocQueryRequest getAllSubmissionSetsRequest(String pId) {
        AdhocQueryRequest req = new AdhocQueryRequest();
        
        // query
        AdhocQueryType query = new AdhocQueryType();

        query.setId(XDSConstants.XDS_FindSubmissionSets);

        // resp option
        ResponseOptionType responseOption = new ResponseOptionType();
        responseOption.setReturnType("ObjectRef");
        responseOption.setReturnComposedObjects(false);
//      responseOption.setReturnType("LeafClass");
//      responseOption.setReturnComposedObjects(true);
        req.setResponseOption(responseOption);
        
        // pid
        SlotType1 slotPid = new SlotType1();
        slotPid.setName(XDSConstants.QRY_SUBMISSIONSET_PATIENT_ID);
        ValueListType pidValues = new ValueListType();
        pidValues.getValue().add(pId);
        slotPid.setValueList(pidValues);
        query.getSlot().add(slotPid);

        // status
        SlotType1 slotStatus = new SlotType1();
        slotStatus.setName(XDSConstants.QRY_SUBMISSIONSET_STATUS);
        ValueListType statusValues = new ValueListType();
        statusValues.getValue().add("('"+XDSConstants.STATUS_APPROVED+"')");
        statusValues.getValue().add("('"+XDSConstants.STATUS_DEPRECATED+"')");
        statusValues.getValue().add("('"+XDSConstants.STATUS_SUBMITTED+"')");
        slotStatus.setValueList(statusValues);
        query.getSlot().add(slotStatus);

        req.setAdhocQuery(query);

        return req;
        
    }
}
