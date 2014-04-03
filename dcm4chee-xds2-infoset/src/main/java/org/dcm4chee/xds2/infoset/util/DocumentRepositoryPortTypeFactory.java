package org.dcm4chee.xds2.infoset.util;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.AddressingFeature;

import org.dcm4chee.xds2.infoset.ws.repository.DocumentRepositoryPortType;
import org.dcm4chee.xds2.infoset.ws.repository.DocumentRepositoryService;

public class DocumentRepositoryPortTypeFactory extends BasePortTypeFactory {

    protected static DocumentRepositoryService service = null;
    private final static URL WSDL_LOCATION = DocumentRepositoryService.class.getResource("/wsdl/XDS.b_DocumentRepository.wsdl");
    
    static {
        service = new DocumentRepositoryService(WSDL_LOCATION, new QName(URN_IHE_ITI, "DocumentRepository_Service"));
        service.getDocumentRepositoryPortSoap12();
    }

    public static DocumentRepositoryPortType getDocumentRepositoryPortSoap12() {
        return service.getPort(new QName(URN_IHE_ITI, "DocumentRepository_Port_Soap12"), 
                DocumentRepositoryPortType.class, new AddressingFeature(true, true));
    }

    public static DocumentRepositoryPortType getDocumentRepositoryPortSoap12(String endpointAddress) {
        
        DocumentRepositoryPortType port = getDocumentRepositoryPortSoap12();
        
        // the handler may only be added if MTOM is active so we tie it together
        final boolean MTOM_ACTIVE = true;
                
        if(MTOM_ACTIVE) {
            addHandler((BindingProvider)port, new XDSDocumentAttachmentHandler());
        }
        configurePort((BindingProvider)port, endpointAddress, MTOM_ACTIVE, true, true);
        return port;
    }
        
}
