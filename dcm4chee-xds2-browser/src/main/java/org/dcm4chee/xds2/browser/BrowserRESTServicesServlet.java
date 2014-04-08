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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.soap.AddressingFeature;

import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.api.generic.ReflectiveConfig;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigNode;
import org.dcm4che3.conf.api.generic.adapters.ReflectiveAdapter;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DeviceExtension;
import org.dcm4chee.xds2.conf.XCAInitiatingGWCfg;
import org.dcm4chee.xds2.conf.XCARespondingGWCfg;
import org.dcm4chee.xds2.conf.XCAiInitiatingGWCfg;
import org.dcm4chee.xds2.conf.XCAiRespondingGWCfg;
import org.dcm4chee.xds2.conf.XdsBrowser;
import org.dcm4chee.xds2.conf.XdsRegistry;
import org.dcm4chee.xds2.conf.XdsRepository;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType.DocumentRequest;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType.DocumentResponse;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryPortType;
import org.dcm4chee.xds2.infoset.ws.repository.DocumentRepositoryPortType;
import org.dcm4chee.xds2.registry.ws.XDSRegistryBeanLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;






import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * REST services for the browser's frontend. Mostly these are wrappers around
 * EJB/Webservice interfaces of XDS
 * 
 * @author Roman K
 * 
 */
@SuppressWarnings("serial")
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class BrowserRESTServicesServlet extends HttpServlet {


    @Inject 
    DicomConfiguration config;
    
    /**
     * Registry device
     */
    @Inject
    private Device device;
    private XdsRegistry cfg;
    private XdsBrowser browserConfig;

    @PostConstruct
    private void getRegistryExtension() {
        cfg = device.getDeviceExtension(XdsRegistry.class);
        browserConfig = cfg.getXdsBrowser();
    }

    /**
     * Registry EJBs
     */
    @EJB
    private static DocumentRegistryPortType xdsRegistryBean;

    @EJB
    private static XDSRegistryBeanLocal xdsRegistryLocalBean;

    public static final Logger log = LoggerFactory.getLogger(BrowserRESTServicesServlet.class);

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

        AdhocQueryResponse rsp = xdsRegistryBean.documentRegistryRegistryStoredQuery(req);

        return rsp;
    }

    @GET
    @Path("/reg/allowedRepos/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getAllowedRepoIds(AdhocQueryRequest req) {

        return Arrays.asList("1.2.3.4.5.999");
    }

    // TODO: configure repositoryId-url from ldap ?

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
    public Response repoRetrieve(@PathParam(value = "repoId") String repoId, @PathParam(value = "docId") String docId) throws IOException {
        try {
            RetrieveDocumentSetRequestType req = new RetrieveDocumentSetRequestType();

            DocumentRequest docreq = new DocumentRequest();
            docreq.setRepositoryUniqueId(repoId);
            docreq.setDocumentUniqueId(docId);

            req.getDocumentRequest().add(docreq);

            // get remote repository service
            QName name = new QName("urn:ihe:iti:xds-b:2007", "XDSbRepository");
            Service service = Service.create(new URL("http://localhost:8080/dcm4chee-xds/XDSbRepository/b?wsdl"), name);
            DocumentRepositoryPortType docRepo = (DocumentRepositoryPortType) service.getPort(DocumentRepositoryPortType.class,
                    new AddressingFeature());

            RetrieveDocumentSetResponseType resp = docRepo.documentRepositoryRetrieveDocumentSet(req);

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
                log.debug("{}", e);
            }

            // return the file for download

            return Response.ok(docResp.getDocument().getInputStream())
                    .header("Content-Disposition", "attachment; filename=" + docResp.getDocumentUniqueId() + "." + ext).build();
        } catch (Exception e) {
            log.info("{}", e);
            return Response.ok("Could not retrieve the document from pre-configured repositories.").status(Status.BAD_REQUEST).build();
        }

    }

    private class ExtensionData {

        public String devicename;
        public String extensiontype;
        public ConfigNode config;

    }

    
    @GET
    @Path("/config/")
    @Produces(MediaType.APPLICATION_JSON)    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<ExtensionData> getConfig() {

        if (browserConfig == null) {
            log.info("No configuration found for the browser, device {}", (device == null ? "null" :device.getDeviceName()));
            return null;
        }
        
        List<ExtensionData> extData = new ArrayList<ExtensionData>();
        for (DeviceExtension de : browserConfig.getControlledDeviceExtensions()) {

            ExtensionData edata = new ExtensionData();

            // fill in stuff
            edata.devicename = de.getDevice().getDeviceName();
            edata.extensiontype = de.getClass().getSimpleName();

            // serialize the configuration
            
            ReflectiveConfig rconfig = new ReflectiveConfig(null, config);
            ReflectiveAdapter ad = new ReflectiveAdapter(de.getClass());
            
            try {
                edata.config = ad.serialize(de, rconfig, null);
            } catch (ConfigurationException e) {
                log.warn("Unable to serialize configuration for presentation in the browser for " + de.getClass().getSimpleName() + ", device "
                        + de.getDevice().getDeviceName(),e);
            }
            
            extData.add(edata);
            
        }

        return extData;

    }

    @GET
    @Path("/reconfigure-all/")
    @Produces(MediaType.APPLICATION_JSON)    
    public void reconfigureAll(){
        
        
        if (browserConfig == null) {
            log.info("No configuration found for the browser, device {}", (device == null ? "null" :device.getDeviceName()));
            return;
        }
        
        List<ExtensionData> extData = new ArrayList<ExtensionData>();
        for (DeviceExtension de : browserConfig.getControlledDeviceExtensions()) {

            // figure out the URL for reloading the config
            String reconfUrl = null;
            try {

            	// temporary for connectathon
            	URL url = new URL("http://localhost:8080"); 
            	
                if (de.getClass() == XdsRegistry.class) {
                    //URL url = new URL(((XdsRegistry) de).getQueryUrl());
                    reconfUrl = String.format("%s://%s/%s", url.getProtocol(), url.getAuthority(), "xds-reg-rs/ctrl/reload");
                } else if (de.getClass() == XdsRepository.class) {
                    //URL url = new URL(((XdsRepository) de).getProvideUrl());
                    reconfUrl = String.format("%s://%s/%s", url.getProtocol(), url.getAuthority(), "xds-rep-rs/ctrl/reload");
                } else if (de.getClass() == XCAInitiatingGWCfg.class || de.getClass() == XCARespondingGWCfg.class) {
                    //URL url = new URL("http://localhost:8080"); // TODO!!!!!
                    reconfUrl = String.format("%s://%s/%s", url.getProtocol(), url.getAuthority(), "xca-rs/ctrl/reload");
                } else if (de.getClass() == XCAiInitiatingGWCfg.class || de.getClass() == XCAiRespondingGWCfg.class) {
                    //URL url = new URL("http://localhost:8080"); // TODO!!!!!
                    reconfUrl = String.format("%s://%s/%s", url.getProtocol(), url.getAuthority(), "xcai-rs/ctrl/reload");
                } else continue;

                

                // bypass certificate validation - we don't transmit any data here   

        		// Create a trust manager that does not validate certificate chains
/*        		TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
        				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        					return new X509Certificate[0];
        				}
        				public void checkClientTrusted(X509Certificate[] certs, String authType) {
        				}
        				public void checkServerTrusted(X509Certificate[] certs, String authType) {
        				}
        			}
        		};

        		// Install the all-trusting trust manager
        		SSLContext sc = SSLContext.getInstance("TLS");
        		sc.init(null, trustAllCerts, new java.security.SecureRandom());

        		// Create all-trusting host name verifier
        		HostnameVerifier allHostsValid = new HostnameVerifier() {

					@Override
        			public boolean verify(String hostname, SSLSession session) {
        				return true;
        			}

        		};*/

                
                
                URL obj = new URL(reconfUrl);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                
                /*if (obj.getProtocol().equals("https")) {
	                ((HttpsURLConnection) con).setSSLSocketFactory(sc.getSocketFactory());
	                ((HttpsURLConnection) con).setHostnameVerifier(allHostsValid);
                }*/
         
                con.setRequestMethod("GET");
         

                log.info("Calling configuration reload @ {} ...", reconfUrl);
                
                int responseCode = con.getResponseCode();
                
            } catch (MalformedURLException e) {
                log.warn("Url in configuration is malformed for " + de.getClass().getSimpleName() + ", device "
                        + de.getDevice().getDeviceName(), e);
            } catch (Exception e) {
                log.warn("Cannot reconfigure " + de.getClass().getSimpleName() + ", device "
                        + de.getDevice().getDeviceName(), e);
            }


            
        };

    }
    


}
