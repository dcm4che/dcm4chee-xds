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

package org.dcm4chee.xds2.registry.ws;

import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.JAXBElement;
import javax.xml.ws.Action;
import javax.xml.ws.BindingType;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.soap.Addressing;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.XDSUtil;
import org.dcm4chee.xds2.common.audit.AuditRequestInfo;
import org.dcm4chee.xds2.common.audit.XDSAudit;
import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.rim.ExternalIdentifierType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.ObjectFactory;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryPortType;
import org.dcm4chee.xds2.ws.handler.LogHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Web service wrapper for the main EJB, adds audit logging 
 * @author franz.willer@gmail.com
 * @author Roman K
 */
@Stateless
@WebService(endpointInterface="org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryPortType",
        name="b",
        serviceName="XDSbRegistry",
        portName="DocumentRegistry_Port_Soap12",
        targetNamespace="urn:ihe:iti:xds-b:2007",
        wsdlLocation = "/META-INF/wsdl/XDS.b_DocumentRegistry.wsdl"
)
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@Addressing(enabled=true, required=true)
@HandlerChain(file="handlers.xml")
public class XDSRegistryBeanWS implements DocumentRegistryPortType {

    private static final String UNKNOWN = "UNKNOWN";

    @EJB
    XDSRegistryBeanLocal xdsEjb;

    private static Logger log = LoggerFactory.getLogger(XDSRegistryBeanWS.class);
    
    public XDSRegistryBeanWS() {
    }
    
    @Override
    @WebMethod(operationName = "DocumentRegistry_RegisterDocumentSet-b", action = "urn:ihe:iti:21111:RegisterDocumentSet-b")
    @WebResult(name = "RegistryResponse", targetNamespace = "urn:oasis:names:tc:ebxml-regrep:xsd:rs:3.0", partName = "body")
    @Action(input="urn:ihe:iti:2007:RegisterDocumentSet-b", 
            output="urn:ihe:iti:2007:RegisterDocumentSet-bResponse")
    public RegistryResponseType documentRegistryRegisterDocumentSetB(
            SubmitObjectsRequest req) {
        RegistryResponseType rsp;
        // call ejb
        try {
            rsp = xdsEjb.documentRegistryRegisterDocumentSetB(req);
        } catch (Exception x) {
            String erruuid = "xdserror:"+UUID.randomUUID().toString();
            log.error("Unexpected error in XDS service (register document), error id = "+erruuid, x);
            XDSException e = new XDSException(XDSException.XDS_ERR_REGISTRY_ERROR, 
                    "Unexpected error in XDS service, error id = "+erruuid, null);
            rsp = new ObjectFactory().createRegistryResponseType();
            XDSUtil.addError(rsp, e);
        }
        return rsp;
    }
    
    @Override
    @Action(input="urn:ihe:iti:2007:RegistryStoredQuery", 
            output="urn:ihe:iti:2007:RegistryStoredQueryResponse")
    public AdhocQueryResponse documentRegistryRegistryStoredQuery(AdhocQueryRequest req) {

        // call ejb
        AdhocQueryResponse rsp = xdsEjb.documentRegistryRegistryStoredQuery(req);
        
        return rsp;
    }


}

