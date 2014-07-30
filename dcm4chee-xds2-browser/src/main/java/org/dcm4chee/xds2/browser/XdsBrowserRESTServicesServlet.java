package org.dcm4chee.xds2.browser;

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
 * Portions created by the Initial Developer are Copyright (C) 2012
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

import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.net.Device;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.cdi.Xds;
import org.dcm4chee.xds2.conf.XdsRegistry;
import org.dcm4chee.xds2.conf.XdsRepository;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType.DocumentRequest;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType.DocumentResponse;
import org.dcm4chee.xds2.infoset.rim.*;
import org.dcm4chee.xds2.infoset.util.DocumentRepositoryPortTypeFactory;
import org.dcm4chee.xds2.infoset.ws.repository.DocumentRepositoryPortType;
import org.dcm4chee.xds2.registry.ws.XDSRegistryBeanLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;

/**
 * REST services for the browser's frontend. Mostly these are wrappers around
 * EJB/Webservice interfaces of XDS
 *
 * @author Roman K
 */
@SuppressWarnings("serial")
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class XdsBrowserRESTServicesServlet extends HttpServlet {

    public static final Logger log = LoggerFactory
            .getLogger(XdsBrowserRESTServicesServlet.class);
    /**
     * cached repositories from the config
     */
    private static Map<String, XdsRepository> repositories;
    /**
     * Registry EJBs
     */
    @EJB
    private static XDSRegistryBeanLocal xdsRegistryBean;
    @EJB
    private static XDSRegistryBeanLocal xdsRegistryLocalBean;
    @Inject
    @Xds
    DicomConfiguration config;
    /**
     * Registry device
     */
    @Inject
    private Device device;
    private XdsRegistry cfg;

    @PostConstruct
    private void getRegistryExtension() {
        cfg = device.getDeviceExtension(XdsRegistry.class);
    }

    /*
     * RESTFUL Services
     */

    @GET
    @Path("/reg/patients/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getPatientIdList() {
        return xdsRegistryLocalBean.listPatientIDs("*");
    }

    @POST
    @Path("/reg/query/")
    @Produces(MediaType.APPLICATION_JSON)
    public AdhocQueryResponse query(AdhocQueryRequest req) {
        return xdsRegistryBean.documentRegistryRegistryStoredQuery(req);
    }

    private Map<String, XdsRepository> getAllRepositories()
            throws ConfigurationException {

        synchronized (XdsBrowserRESTServicesServlet.class) {
            if (repositories == null) {

                repositories = Collections
                        .synchronizedMap(new HashMap<String, XdsRepository>());

                // fetch all repos from all devices...
                for (String deviceName : config.listDeviceNames()) {
                    Device d = config.findDevice(deviceName);
                    XdsRepository repo = d
                            .getDeviceExtension(XdsRepository.class);
                    if (repo != null)
                        repositories.put(repo.getRepositoryUID(), repo);
                }

            }

        }
        return repositories;
    }

    @GET
    @Path("/reg/allowedRepos/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getAllowedRepoIds(AdhocQueryRequest req)
            throws ConfigurationException {
        return new ArrayList<String>(getAllRepositories().keySet());
    }

    /**
     * Retrieves a doc from a repository and returns it as a downloadable file
     * with an extension set according to the doc's mime type
     *
     * @param repoId
     * @param docId
     * @return
     * @throws IOException
     */
    @GET
    @Path("/repo/retrieve-download/{repoId}/{docId}")
    public Response repoRetrieve(@PathParam(value = "repoId") String repoId,
                                 @PathParam(value = "docId") String docId) throws IOException {
        try {
            RetrieveDocumentSetRequestType req = new RetrieveDocumentSetRequestType();

            DocumentRequest docreq = new DocumentRequest();
            docreq.setRepositoryUniqueId(repoId);
            docreq.setDocumentUniqueId(docId);

            req.getDocumentRequest().add(docreq);

            // see if we are able to retrieve from this repo
            XdsRepository repo = getAllRepositories().get(repoId);

            // get remote repository service

            DocumentRepositoryPortType docRepo = DocumentRepositoryPortTypeFactory
                    .getDocumentRepositoryPortSoap12(repo.getRetrieveUrl());
            RetrieveDocumentSetResponseType resp = docRepo
                    .documentRepositoryRetrieveDocumentSet(req);

            // retrieve
            DocumentResponse docResp = resp.getDocumentResponse().get(0);

            // set the proper extension for the file based on mime type
            MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();

            String mimeTypeStr = docResp.getMimeType();
            MimeType mimeType;
            String ext = "";

            // just use the preferred ext, or leave emtpy
            try {
                mimeType = allTypes.forName(mimeTypeStr);
                ext = mimeType.getExtension();
            } catch (MimeTypeException e) {
                log.warn("Error while resolving mime type extension for document id " + docId + " from repository " + repoId + ", specified mimetype string is " + mimeTypeStr, e);
            }

            // return the file for download

            return Response
                    .ok(docResp.getDocument().getInputStream())
                    .header("Content-Disposition",
                            "attachment; filename="
                                    + docResp.getDocumentUniqueId() + "." + ext)
                    .build();
        } catch (Exception e) {
            log.warn(
                    "Error while retrieving the document from the remote repository",
                    e);
            return Response
                    .ok("Could not retrieve the document from pre-configured repositories.")
                    .status(Status.BAD_REQUEST).build();
        }

    }


    @POST
    @Path("/reg/delete/")
    public void deleteRegistryObjects(RemoveObjectsRequest removeReq) {
        xdsRegistryLocalBean.deleteObjects(removeReq);
    }

    @GET
    @Path("/reg/delete-all-for-patient/{patientId}")
    public void deleteAllRegistryObjectsForPatient(@PathParam(value = "patientId") String patientId) throws JAXBException {

        //// perform getAll query
        AdhocQueryType getAllQuery = new AdhocQueryType();
        getAllQuery.setId(XDSConstants.XDS_GetAll);

        // pid
        SlotType1 patIdSlot = new SlotType1();
        patIdSlot.setName(XDSConstants.QRY_PATIENT_ID);
        ValueListType v = new ValueListType();
        v.getValue().add(patientId);
        patIdSlot.setValueList(v);

        // all statuses
        ValueListType allStatuses = new ValueListType();
        allStatuses.getValue().add(String.format("('%s','%s','%s')", XDSConstants.STATUS_APPROVED, XDSConstants.STATUS_DEPRECATED, XDSConstants.STATUS_SUBMITTED));

        SlotType1 slotDocEntry = new SlotType1();
        slotDocEntry.setName(XDSConstants.QRY_DOCUMENT_ENTRY_STATUS);
        slotDocEntry.setValueList(allStatuses);
        SlotType1 slotFolder = new SlotType1();
        slotFolder.setName(XDSConstants.QRY_FOLDER_STATUS);
        slotFolder.setValueList(allStatuses);
        SlotType1 slotSubmSet = new SlotType1();
        slotSubmSet.setName(XDSConstants.QRY_SUBMISSIONSET_STATUS);
        slotSubmSet.setValueList(allStatuses);

        getAllQuery.getSlot().add(patIdSlot);
        getAllQuery.getSlot().add(slotSubmSet);
        getAllQuery.getSlot().add(slotDocEntry);
        getAllQuery.getSlot().add(slotFolder);

        AdhocQueryRequest req = new AdhocQueryRequest();
        ResponseOptionType value = new ResponseOptionType();
        value.setReturnType("ObjectRef");
        value.setReturnComposedObjects(true);
        req.setResponseOption(value);
        req.setAdhocQuery(getAllQuery);

        AdhocQueryResponse resp = xdsRegistryBean.documentRegistryRegistryStoredQuery(req);

        //// compose a delete request
        List<String> ids = new ArrayList<>();

        for (JAXBElement<? extends IdentifiableType> jaxbElement : resp.getRegistryObjectList().getIdentifiable())
            ids.add(jaxbElement.getValue().getId());

        RemoveObjectsRequest ror = new RemoveObjectsRequest();
        ObjectRefListType objectRefList = new ObjectRefListType();
        List<ObjectRefType> objectRef = objectRefList.getObjectRef();

        for (String id : ids) {
            ObjectRefType e = new ObjectRefType();
            e.setId(id);
            objectRef.add(e);
        }

        ror.setObjectRefList(objectRefList);

        //// delete
        xdsRegistryLocalBean.deleteObjects(ror);
    }


    @POST
    @Path("/logout")
    public void logout(@Context HttpServletRequest req) {
        req.getSession().invalidate();
    }


}
