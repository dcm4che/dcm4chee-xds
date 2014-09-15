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
 * Java(TM), available at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * TIANI Medgraph AG.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunter.zeilinger@tiani.com>
 * Franz Willer <franz.willer@gmail.com>
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
package org.dcm4chee.xds2.common.audit;

import static org.dcm4che3.audit.AuditMessages.createEventIdentification;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.activation.DataHandler;

import org.dcm4che3.audit.AuditMessage;
import org.dcm4che3.audit.AuditMessages;
import org.dcm4che3.audit.AuditMessages.EventActionCode;
import org.dcm4che3.audit.AuditMessages.EventID;
import org.dcm4che3.audit.AuditMessages.EventOutcomeIndicator;
import org.dcm4che3.audit.AuditMessages.EventTypeCode;
import org.dcm4che3.audit.AuditMessages.ParticipantObjectIDTypeCode;
import org.dcm4che3.audit.AuditMessages.RoleIDCode;
import org.dcm4che3.audit.Instance;
import org.dcm4che3.audit.ParticipantObjectDescription;
import org.dcm4che3.audit.ParticipantObjectDetail;
import org.dcm4che3.audit.ParticipantObjectIdentification;
import org.dcm4che3.audit.SOPClass;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.IncompatibleConnectionException;
import org.dcm4che3.net.audit.AuditLogger;
import org.dcm4chee.xds2.common.InputStreamDataSource;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.XDSUtil;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType.DocumentRequest;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType.DocumentResponse;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryType;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Audit logger for XDS.
 * 
 * A configured AuditLogger must be set via setAuditLogger.
 * 
 * AuditLogger can be configured via LDAP:
 * 
 *      InputStream ldapConf = new URL(ldapPropertiesURL).openStream();
 *      Properties p = new Properties();
 *      p.load(ldapConf);
 *      ExtendedLdapDicomConfiguration config = new ExtendedLdapDicomConfiguration(p);
 *      config.addDicomConfigurationExtension(new LdapAuditLoggerConfiguration());
 *      config.addDicomConfigurationExtension(new LdapAuditRecordRepositoryConfiguration());
 *      Device device = config.findDevice("XDSAuditLogger");
 *      AuditLogger logger = device.getDeviceExtension(AuditLogger.class);
 *
 *      LDAP properties example (openDs):
 *      java.naming.factory.initial=com.sun.jndi.ldap.LdapCtxFactory
 *      java.naming.ldap.attributes.binary=dicomVendorData
 *      java.naming.provider.url=ldap://localhost:1389/dc=example,dc=com
 *      java.naming.security.principal=cn=Directory Manager
 *      java.naming.security.credentials=secret
 *
 * or Java Preferences:
 *
 *      PreferencesDicomConfiguration prefConfig = new PreferencesDicomConfiguration();
 *      prefConfig.addDicomConfigurationExtension(new PreferencesAuditLoggerConfiguration());
 *      prefConfig.addDicomConfigurationExtension(new PreferencesAuditRecordRepositoryConfiguration());
 *      Device device = prefConfig.findDevice("XDSAuditLogger");
 *      AuditLogger logger = device.getDeviceExtension(AuditLogger.class);
 *      
 * 
 * or by using AuditLogger directly:
 * 
 *     ..
 *     AuditLogger logger = createLoggerDevice("XDSLoggerDevice").getDeviceExtension(AuditLogger.class);
 *     ..
 *     private Device createLoggerDevice(String name) {
 *       Device device = new Device(name);
 *       Connection udp = new Connection("audit-udp", "host.dcm4che.org");
 *       udp.setProtocol(Connection.Protocol.SYSLOG_UDP);
 *       Connection tls = new Connection("audit-tls", "host.dcm4che.org");
 *       tls.setProtocol(Connection.Protocol.SYSLOG_TLS);
 *       tls.setTlsCipherSuites("TLS_RSA_WITH_AES_128_CBC_SHA");
 *       device.addConnection(udp);
 *       device.addConnection(tls);
 *       udp.setPort(514);
 *       tls.setPort(6514);
 *       addAuditRecordRepository(device, udp, tls);
 *       arrDevice = device;
 *       addAuditLogger(device, udp, tls, arrDevice);
 *       return device ;
 *   }
 *
 *   private void addAuditRecordRepository(Device device, Connection udp, Connection tls) {
 *       AuditRecordRepository arr = new AuditRecordRepository();
 *       device.addDeviceExtension(arr);
 *       arr.addConnection(udp);
 *       arr.addConnection(tls);
 *   }
 *
 *   private void addAuditLogger(Device device, Connection udp, Connection tls, Device arrDevice) {
 *       AuditLogger logger = new AuditLogger();
 *       device.addDeviceExtension(logger);
 *       logger.addConnection(udp);
 *       logger.addConnection(tls);
 *       logger.setAuditRecordRepositoryDevice(arrDevice);
 *       logger.setSchemaURI(AuditMessages.SCHEMA_URI);
 *   }
 *
 * 
 * @author franz.willer@gmail.com
 *
 */
public class XDSAudit {
    private static final String UNKNOWN = "UNKNOWN";

    private static AuditLogger logger;
    private static final int BUF_SIZE = 8192;

    private static final Logger log = LoggerFactory.getLogger(XDSAudit.class);
    
    public static AuditLogger getAuditLogger() {
        return logger;
    }

    public static void setAuditLogger(AuditLogger logger) {
        XDSAudit.logger = logger;
    }
    
    public static void logApplicationActivity(EventTypeCode eventType, boolean success) {
        if (logger != null && logger.isInstalled()) {
            try {
                Calendar timeStamp = logger.timeStamp();
                AuditMessage msg = createApplicationActivity(eventType, timeStamp,
                        success ? EventOutcomeIndicator.Success : EventOutcomeIndicator.MajorFailure);
                sendAuditMessage(timeStamp, msg);
            } catch (Exception e) {
                log.warn("Audit log of ApplicationActivity failed!");
                log.debug("AuditLog Exception:", e);
            }
        }
    }
    
    /**
     * Send a ITI-41 Import Audit message. (Document Repository: PnR)
     * patID(1)
     * srcUserID         content of wsa:ReplyTo. ('http://www.w3.org/2005/08/addressing/anonymous' if null).
     * altSrcUserID      not specialized
     * destUserID        SOAP endpoint URI.
     * altDestUserID     Process ID (consumer)
     * SubmissionSet(1)
     * Document(0)
     * 
     * @param submissionSetUID
     * @param patID
     * @param info
     * @param success
     */
   public static void logRepositoryImport(String submissionSetUID, String patID, AuditRequestInfo info, boolean success) {
       if (logger != null && logger.isInstalled()) {
           logImport(EventTypeCode.ITI_41_ProvideAndRegisterDocumentSetB, submissionSetUID, patID, info.getReplyTo(),
               null, info.getRemoteHost(), info.getRequestURI(), AuditLogger.processID(), 
               info.getLocalHost(), null, success);
       }
    }
    /**
     * Send a ITI-42 Import Audit message. (Document Registry: register document set)
     * patID(1)
     * srcUserID         content of wsa:ReplyTo. ('http://www.w3.org/2005/08/addressing/anonymous' if null).
     * altSrcUserID      not specialized
     * destUserID        SOAP endpoint URI.
     * altDestUserID     Process ID (consumer)
     * SubmissionSet(1)
     * Document(0)
     * 
     * @param submissionSetUID
     * @param patID
     * @param info
     * @param success
     */
    public static void logRegistryImport(String submissionSetUID, String patID, AuditRequestInfo info, boolean success) {
        if (logger != null && logger.isInstalled()) {
            logImport(EventTypeCode.ITI_42_RegisterDocumentSetB, submissionSetUID, patID, info.getReplyTo(),
                null, info.getRemoteHost(), info.getRequestURI(), AuditLogger.processID(), 
                info.getLocalHost(), null, success);
        }
    }
    
    /**
     * Send a ITI-43 Import Audit message. (document consumer: Retrieve document set)
     * patID(0..1)
     * srcUserID         SOAP Endpoint URI of repository
     * altSrcUserID      not specialized
     * destUserID        content of wsa:ReplyTo. ('http://www.w3.org/2005/08/addressing/anonymous' if null).
     * altDestUserID     Process ID (consumer)
     * SubmissionSet(0)
     * Document(1..n)
     * 
     * @param patID
     * @param repositoryURL
     * @param docReq
     * @param success
     */
    public static void logConsumerImport(String patID, URL repositoryURL, RetrieveDocumentSetRequestType docReq, boolean success) {
    	logImport(EventTypeCode.ITI_43_RetrieveDocumentSet,patID,repositoryURL,docReq,success);
    }

    public static void logConsumerXCAImport(String patID, URL repositoryURL, RetrieveDocumentSetRequestType docReq, boolean success) {
    	logImport(EventTypeCode.ITI_39_CrossGatewayRetrieve,patID,repositoryURL,docReq,success);
    }	

    public static void logImport(EventTypeCode eventTypeCode, String patID, URL repositoryURL, RetrieveDocumentSetRequestType docReq, boolean success) {
	    try {
	    	if (logger != null && logger.isInstalled()) {
	            logImport(eventTypeCode, null, patID, repositoryURL.toExternalForm(),
	                null, repositoryURL.getHost(), XDSConstants.WS_ADDRESSING_ANONYMOUS, AuditLogger.processID(), 
	                AuditLogger.localHost().getHostName(), docReq, success);
	        }
	    } catch (Exception e ){
	        log.warn("Audit log of Import ({}) failed!",eventTypeCode.getDisplayName());    	
            log.debug("AuditLog Exception:", e);
	    }
    }
    
    public static void logImport(EventTypeCode eventTypeCode, String submissionSetUID, String patID, 
            String srcUserID, String altSrcUserID, String srcHostName, 
            String destUserID, String altDestUserID, String destHostName, RetrieveDocumentSetRequestType docReq, boolean success) {
        try {
            Calendar timeStamp = logger.timeStamp();
            AuditMessage msg = XDSAudit.createImport(eventTypeCode, submissionSetUID, patID, 
                    srcUserID, altSrcUserID, srcHostName, 
                    destUserID, altDestUserID, destHostName, docReq, timeStamp, 
                    success ? EventOutcomeIndicator.Success : EventOutcomeIndicator.MinorFailure);
            sendAuditMessage(timeStamp, msg);
        } catch (Exception e) {
            log.warn("Audit log of Import ("+eventTypeCode.getDisplayName()+") failed!");
            log.debug("AuditLog Exception:", e);
        }
    }

    /**
     * Send a ITI-41 Export Audit message. (document source)
     * patID(1)
     * srcUserID         wsa:ReplyTo. ('http://www.w3.org/2005/08/addressing/anonymous' if null
     * altSrcUserID      Process ID (consumer)
     * destUserID        SOAP endpoint URI
     * altDestUserID     not spezialized
     * SubmissionSet(1)
     * Document(0)
     * 
     * @param submissionSetUID
     * @param patID
     * @param srcUserID
     * @param altSrcUserID
     * @param srcHostName
     * @param destUserID
     * @param destHostName
     * @param success
     */
    public static void logSourceExport(String submissionSetUID, String patID, String srcUserID, String altSrcUserID, String srcHostName, 
            String destUserID, String destHostName, boolean success) {
        if (logger != null && logger.isInstalled()) {
            if (patID == null) {
                log.error("Audit ITI-41 Export! patient ID is null. set to 'UNKNOWN'!");
                patID = UNKNOWN;
            }
            if (srcUserID == null) {
                srcUserID = XDSConstants.WS_ADDRESSING_ANONYMOUS;
            }
            logExport(EventTypeCode.ITI_41_ProvideAndRegisterDocumentSetB, submissionSetUID, patID, 
                    srcUserID, altSrcUserID, srcHostName, destUserID, null, destHostName, null, null, success);
        }
    }
    /**
     * Send a ITI-42 Export Audit message. (document repository: register document set)
     * patID(1)
     * srcUserID         wsa:ReplyTo. ('http://www.w3.org/2005/08/addressing/anonymous' if null).
     * altSrcUserID      Process ID
     * destUserID        SOAP endpoint URI.
     * altDestUserID     not spezialized
     * SubmissionSet(1)
     * Document(0)
     * 
     * @param submissionSetUID
     * @param patID
     * @param info
     * @param registryURL
     * @param success
     */
    public static void logRepositoryPnRExport(String submissionSetUID, String patID, AuditRequestInfo info, URL registryURL, boolean success) {
        if (logger != null && logger.isInstalled()) {
            logExport(EventTypeCode.ITI_42_RegisterDocumentSetB, submissionSetUID, patID, 
                XDSConstants.WS_ADDRESSING_ANONYMOUS, AuditLogger.processID(), info.getLocalHost(), registryURL.toExternalForm(), null, registryURL.getHost(), 
                null, null, success);
        }
    }
    /**
     * Send a ITI-43 Export Audit message. (document repository: Retrieve document set)
     * patID(0)
     * srcUserID         SOAP Endpoint URI of repository
     * altSrcUserID      Process ID
     * destUserID        content of wsa:ReplyTo. ('http://www.w3.org/2005/08/addressing/anonymous' if null).
     * altDestUserID     not spezialized
     * SubmissionSet(0)
     * Document(1..n)
     * 
     * @param info
     * @param docReq
     * @param docUIDs
     * @param success
     */
    public static void logRepositoryRetrieveExport(AuditRequestInfo info, 
            RetrieveDocumentSetRequestType docReq, List<String> docUIDs, boolean success) {
        if (logger != null && logger.isInstalled()) {
            logExport(EventTypeCode.ITI_43_RetrieveDocumentSet, null, null, info.getRequestURI(), 
                AuditLogger.processID(), info.getLocalHost(), info.getReplyTo(), null, info.getRemoteHost(),
                docReq, docUIDs, success);
        }
    }
    public static void logRepositoryRetrieveExport(RetrieveDocumentSetRequestType req, RetrieveDocumentSetResponseType rsp, AuditRequestInfo info) {
        logExport(EventTypeCode.ITI_43_RetrieveDocumentSet, req, rsp, info); 
    }
    public static void logXCARetrieveExport(RetrieveDocumentSetRequestType req, RetrieveDocumentSetResponseType rsp, AuditRequestInfo info) {
        logExport(EventTypeCode.ITI_39_CrossGatewayRetrieve, req, rsp, info); 
    }
    public static void logExport(EventTypeCode eventTypeCode, RetrieveDocumentSetRequestType req, RetrieveDocumentSetResponseType rsp, AuditRequestInfo info) {
        if (logger == null || !logger.isInstalled())
            return;
        List<DocumentRequest> docReq = req.getDocumentRequest();
        List<String> retrievedUIDs = new ArrayList<String>();
        for (DocumentResponse doc : rsp.getDocumentResponse())
            retrievedUIDs.add(doc.getDocumentUniqueId());
        if (retrievedUIDs.size() > 0) {
            logExport(eventTypeCode, null, null, info.getRequestURI(), AuditLogger.processID(), 
                    info.getLocalHost(), info.getReplyTo(), null, info.getRemoteHost(), req, retrievedUIDs, true);
        }
        if (retrievedUIDs.size() < docReq.size()) {
            List<String> failedUIDs = new ArrayList<String>();
            for (DocumentRequest doc : req.getDocumentRequest())
                if (!retrievedUIDs.contains(doc.getDocumentUniqueId())) 
                    failedUIDs.add(doc.getDocumentUniqueId());
            logExport(eventTypeCode, null, null, info.getRequestURI(), AuditLogger.processID(), 
                    info.getLocalHost(), info.getReplyTo(), null, info.getRemoteHost(), req, failedUIDs, false);
        }
    }
    
    public static void logExport(EventTypeCode eventTypeCode, String submissionSetUID, String patID,
            String srcUserID, String altSrcUserID, String srcHostName, String destUserID, String altDestUserID, String destHostName,
            RetrieveDocumentSetRequestType docReq, List<String> docUIDs, boolean success) {
        try {
            Calendar timeStamp = logger.timeStamp();
            AuditMessage msg = XDSAudit.createExport(eventTypeCode, submissionSetUID, patID, 
                    srcUserID, altSrcUserID, srcHostName, 
                    destUserID, altDestUserID, destHostName, 
                    docReq, docUIDs, timeStamp, success ? EventOutcomeIndicator.Success : EventOutcomeIndicator.MinorFailure);
            sendAuditMessage(timeStamp, msg);
        } catch (Exception e) {
            log.warn("Audit log of Export ("+eventTypeCode.getDisplayName()+") failed!");
            log.debug("AuditLog Exception:", e);
        }
    }
    
    /**
     * Send a ITI-18 Stored Query Audit message. (Document Source/Consumer)
     * 
     * srcUserID         wsa:ReplyTo
     * altSrcUserID      process ID
     * destUserID        SOAP Endpoint URI (registry)
     * altDestUserID     not specialized
     * Patient (0..1)
     * Query Parameters(1) 
     * 
     * @param queryUID
     * @param patID
     * @param homeCommunityID
     * @param adhocQuery
     * @param srcUserID
     * @param srcHostName
     * @param registryURL
     * @param success
     */
    public static void logClientQuery(AdhocQueryRequest req, 
            String srcUserID, String srcHostName, URL registryURL, boolean success) {
        logQuery(EventTypeCode.ITI_18_RegistryStoredQuery, req, srcUserID, AuditLogger.processID(), srcHostName, 
                registryURL.toExternalForm(), null, registryURL.getHost(), success);
    }
    public static void logClientXCAQuery(AdhocQueryRequest req, 
            String srcUserID, String srcHostName, URL registryURL, boolean success) {
        logQuery(EventTypeCode.ITI_38_CrossGatewayQuery, req, srcUserID, AuditLogger.processID(), srcHostName, 
                registryURL.toExternalForm(), null, registryURL.getHost(), success);
    }
    /**
     * Send a ITI-18 Stored Query Audit message. (Document Registry)
     * 
     * srcUserID         wsa:ReplyTo
     * altSrcUserID      not spezialized
     * destUserID        SOAP Endpoint URI (registry)
     * altDestUserID     process ID
     * Patient (0..1)
     * Query Parameters(1) 
     * 
     * @param queryUID
     * @param patID
     * @param homeCommunityID
     * @param adhocQuery
     * @param info
     * @param success
     */
    public static void logRegistryQuery(AdhocQueryRequest req, AuditRequestInfo info, boolean success) {
        logRegistryQuery(EventTypeCode.ITI_18_RegistryStoredQuery, req, info, success);
    }
    public static void logXCAQuery(AdhocQueryRequest req, AuditRequestInfo info, boolean success) {
        logRegistryQuery(EventTypeCode.ITI_38_CrossGatewayQuery, req, info, success);
    }

    public static void logRegistryQuery(EventTypeCode eventTypeCode, AdhocQueryRequest req, AuditRequestInfo info, boolean success) {
        logQuery(eventTypeCode, req, info.getReplyTo(), null, info.getRemoteHost(), 
            info.getRequestURI(), AuditLogger.processID(), info.getLocalHost(), success);
    }
    public static void logQuery(EventTypeCode eventTypeCode, AdhocQueryRequest req, 
            String srcUserID, String altSrcUserID, String srcHostName, String destUserID, String altDestUserID, 
            String destHostName, boolean success) {

        if (logger != null && logger.isInstalled()) {
            try {
                AdhocQueryType qry = req.getAdhocQuery();
                String patID = XDSUtil.getQueryPatID(qry.getSlot());
                String homeCommunityID = InfosetUtil.getSlotValue(qry.getSlot(), XDSConstants.QRY_HOME_COMMUNITY_ID, null);
                if (srcHostName == null)
                    srcHostName = AuditLogger.localHost().getHostName();
                Calendar timeStamp = logger.timeStamp();
                AuditMessage msg = XDSAudit.createQuery(eventTypeCode, qry.getId(), patID, homeCommunityID, InfosetUtil.marshallObject(req, true).getBytes("UTF-8"), 
                        srcUserID, altSrcUserID, srcHostName, destUserID, altDestUserID, destHostName, 
                        timeStamp, success ? EventOutcomeIndicator.Success : EventOutcomeIndicator.MinorFailure);
                sendAuditMessage(timeStamp, msg);
            } catch (Exception e) {
                log.warn("Audit log of Stored Query failed!");
                log.debug("AuditLog Exception:", e);
            }
        }
    }

    public static void logPatientFeed(String patID, String eventActionCode, byte[] msh10, 
            String srcUserID, String remoteHost, String destUserID, boolean success) {
        if (logger != null && logger.isInstalled()) {
            try {
                Calendar timeStamp = logger.timeStamp();
                AuditMessage msg = XDSAudit.createPatientFeed(patID, eventActionCode, msh10, 
                        srcUserID, null, remoteHost, destUserID, 
                        timeStamp, success ? EventOutcomeIndicator.Success : EventOutcomeIndicator.MinorFailure);
                sendAuditMessage(timeStamp, msg);
            } catch (Exception e) {
                log.warn("Audit log of Patient Feed failed!");
                log.debug("AuditLog Exception:", e);
            }
        }
    }

    public static void logPixQuery(String patID, String sendingApp, String sendingFacility, 
            String receivingApp, String receivingFacility, String remoteHost, byte[] query, byte[] msh10, boolean success) {
        if (logger != null && logger.isInstalled()) {
            try {
                Calendar timeStamp = logger.timeStamp();
                AuditMessage msg = new AuditMessage(); 
                msg.setEventIdentification(createEventIdentification(
                        EventID.Query, 
                        EventActionCode.Execute,
                        timeStamp,
                        success ? EventOutcomeIndicator.Success : EventOutcomeIndicator.MinorFailure,
                        null,
                        EventTypeCode.ITI_9_PIXQuery));
                msg.getAuditSourceIdentification().add(
                        logger.createAuditSourceIdentification());
                String hostName = AuditLogger.localHost().getHostName();
                msg.getActiveParticipant().add(
                        AuditMessages.createActiveParticipant(sendingFacility+"|"+sendingApp, 
                                AuditLogger.processID(), null, true,
                                hostName, machineOrIP(hostName), null, RoleIDCode.Source));
                msg.getActiveParticipant().add(
                        AuditMessages.createActiveParticipant(sendingFacility+"|"+sendingApp, null, null, false,
                                remoteHost, machineOrIP(remoteHost), null, RoleIDCode.Destination));
                msg.getParticipantObjectIdentification().add(createPatient(patID));
                msg.getParticipantObjectIdentification().add(AuditMessages.createParticipantObjectIdentification(
                        null, new ParticipantObjectIDTypeCode("ITI-9", "IHE Transactions", "PIX Query"),
                        null, query, AuditMessages.ParticipantObjectTypeCode.SystemObject, 
                        AuditMessages.ParticipantObjectTypeCodeRole.Query, null, null, null, 
                        AuditMessages.createParticipantObjectDetail("MSH-10", msh10)));
                msg.getParticipantObjectIdentification().add(AuditMessages.createParticipantObjectIdentification(
                        patID, ParticipantObjectIDTypeCode.ITI_PatientNumber, null,
                        (byte[])null, AuditMessages.ParticipantObjectTypeCode.Person, 
                        AuditMessages.ParticipantObjectTypeCodeRole.Patient, null, null, null));
                sendAuditMessage(timeStamp, msg);
            } catch (Exception e) {
                log.warn("Audit log of PIX query failed!");
                log.debug("AuditLog Exception:", e);
            }
        }
    }
    
    public static void logImgRetrieve(RetrieveDocumentSetResponseType rsp, AuditRequestInfo info) {
        if (logger == null || !logger.isInstalled())
            return;
        try {
            Calendar timeStamp = logger.timeStamp();
            AuditMessage msg = createRetrieveImgLogMessage(rsp, info.getRemoteHost(), info.getRemoteUser(), timeStamp);
            sendAuditMessage(timeStamp, msg);
        } catch (Exception e) {
            log.warn("Audit log instances stored failed!");
            log.debug("AuditLog Exception:", e);
        }
    }

    public static void logStudyUsed(RetrieveDocumentSetResponseType rsp, AuditRequestInfo info) {
        if (logger == null || !logger.isInstalled())
            return;
        try {
            Calendar timeStamp = logger.timeStamp();
            AuditMessage msg = createStudyUsedLogMessage(rsp, info.getRemoteHost(), info.getRemoteUser(), timeStamp);
            sendAuditMessage(timeStamp, msg);
        } catch (Exception e) {
            log.warn("Audit log of Study Used failed!");
            log.debug("AuditLog Exception:", e);
        }
    }

    public static AuditMessage createRetrieveImgLogMessage(RetrieveDocumentSetResponseType rsp, String remoteHost, 
            String user, Calendar timeStamp) {
        AuditMessage msg = new AuditMessage();
        msg.setEventIdentification(AuditMessages.createEventIdentification(
                EventID.DICOMInstancesTransferred, EventActionCode.Read, timeStamp, 
                XDSConstants.XDS_B_STATUS_FAILURE.equals(rsp.getRegistryResponse().getStatus()) ? 
                        EventOutcomeIndicator.MinorFailure : EventOutcomeIndicator.Success, 
                null));
        msg.getActiveParticipant().add(logger.createActiveParticipant(false, RoleIDCode.Source));
        msg.getActiveParticipant().add(AuditMessages.createActiveParticipant(
                user != null ? user : "ANONYMOUS", null, null, true, remoteHost, 
                machineOrIP(remoteHost), null, AuditMessages.RoleIDCode.Destination));
        String patientID = addStudyPOIsAndGetPatientID(msg.getParticipantObjectIdentification(), rsp);
        msg.getParticipantObjectIdentification().add(AuditMessages.createParticipantObjectIdentification(
                patientID,
                AuditMessages.ParticipantObjectIDTypeCode.PatientNumber,
                null,
                null,
                AuditMessages.ParticipantObjectTypeCode.Person,
                AuditMessages.ParticipantObjectTypeCodeRole.Patient,
                null,
                null,
                null));
        return msg;
    }

    public static AuditMessage createStudyUsedLogMessage(RetrieveDocumentSetResponseType rsp, String remoteHost, 
            String user, Calendar timeStamp) {
        AuditMessage msg = new AuditMessage();
        msg.setEventIdentification(AuditMessages.createEventIdentification(
                EventID.DICOMInstancesAccessed, EventActionCode.Read, timeStamp, 
                XDSConstants.XDS_B_STATUS_FAILURE.equals(rsp.getRegistryResponse().getStatus()) ? 
                        EventOutcomeIndicator.MinorFailure : EventOutcomeIndicator.Success, 
                null));
        msg.getActiveParticipant().add(logger.createActiveParticipant(false, RoleIDCode.Application));
        msg.getActiveParticipant().add(AuditMessages.createActiveParticipant(
                user != null ? user : "ANONYMOUS", null, null, true, remoteHost, 
                machineOrIP(remoteHost), null, AuditMessages.RoleIDCode.Application));
        String patientID = addStudyPOIsAndGetPatientID(msg.getParticipantObjectIdentification(), rsp);
        msg.getParticipantObjectIdentification().add(AuditMessages.createParticipantObjectIdentification(
                patientID, AuditMessages.ParticipantObjectIDTypeCode.PatientNumber,
                null, null, AuditMessages.ParticipantObjectTypeCode.Person,
                AuditMessages.ParticipantObjectTypeCodeRole.Patient, null, null, null));
        return msg;
    }

    private static String addStudyPOIsAndGetPatientID(List<ParticipantObjectIdentification> studyPOIs, RetrieveDocumentSetResponseType rsp) {
        Attributes attrs = null;
        String studyIUID, classUID;
        HashMap<String,HashMap<String,List<String>>> studySopClassMap = new HashMap<String,HashMap<String,List<String>>>();
        HashMap<String,List<String>> sopClassInstanceMap;
        List<String> instances;
        for (DocumentResponse doc : rsp.getDocumentResponse()) {
            BufferedInputStream is = null;
            DicomInputStream dis = null;
            try {
                DataHandler dh = doc.getDocument();
                is = new BufferedInputStream(dh.getInputStream(), BUF_SIZE);
                is.mark(BUF_SIZE);
                dis = new DicomInputStream(new BufferedInputStream(is));
                attrs = dis.readDataset(-1, Tag.SeriesInstanceUID);
                is.reset();
                doc.setDocument(new DataHandler(new InputStreamDataSource(is, dh.getContentType())));
                studyIUID = attrs.getString(Tag.StudyInstanceUID);
                classUID = attrs.getString(Tag.SOPClassUID);
                sopClassInstanceMap = studySopClassMap.get(studyIUID);
                if (sopClassInstanceMap == null) {
                    sopClassInstanceMap = new HashMap<String,List<String>>();
                    studySopClassMap.put(studyIUID, sopClassInstanceMap);
                    instances = null;
                } else {
                    instances = sopClassInstanceMap.get(classUID);
                }
                if (instances == null) {
                    instances = new ArrayList<String>();
                    sopClassInstanceMap.put(classUID, instances);
                }
                instances.add(attrs.getString(Tag.SOPInstanceUID));
            } catch (IOException x) {
                log.warn("Failed to read DICOM attachment! instanceUID:"+doc.getDocumentUniqueId(),x);
            }
        }
        for ( Entry<String, HashMap<String, List<String>>> e : studySopClassMap.entrySet()) {
            ParticipantObjectDescription pod = new ParticipantObjectDescription();
            studyPOIs.add(createStudyPOI(e.getKey(), pod));
            for (Entry<String, List<String>> e1 : e.getValue().entrySet()) {
                SOPClass sc = new SOPClass();
                sc.setUID(e1.getKey());
                sc.setNumberOfInstances(e1.getValue().size());
                for (String iuid : e1.getValue()) {
                    Instance inst = new Instance();
                    inst.setUID(iuid);
                    sc.getInstance().add(inst);
                }
                pod.getSOPClass().add(sc);
                
            }
        }
        return attrs == null ? null : attrs.getString(Tag.PatientID);
    }

    private static ParticipantObjectIdentification createStudyPOI(String studyIUID,
            ParticipantObjectDescription pod) {
        return AuditMessages.createParticipantObjectIdentification(
                studyIUID, 
                AuditMessages.ParticipantObjectIDTypeCode.StudyInstanceUID, 
                null, 
                null, 
                AuditMessages.ParticipantObjectTypeCode.SystemObject, 
                AuditMessages.ParticipantObjectTypeCodeRole.Report, 
                null, 
                null, 
                pod);
    }

    private static AuditMessage createApplicationActivity(
            EventTypeCode eventType, Calendar timeStamp, String outcomeIndicator) {
        AuditMessage msg = new AuditMessage();
        msg.setEventIdentification(createEventIdentification(
                EventID.ApplicationActivity, 
                EventActionCode.Execute,
                timeStamp,
                outcomeIndicator,
                null,
                eventType));
        msg.getAuditSourceIdentification().add(
                logger.createAuditSourceIdentification());
        msg.getActiveParticipant().add(
                logger.createActiveParticipant(true, RoleIDCode.Application));
        return msg;
    }

    public static AuditMessage createImport(EventTypeCode eventTypeCode, String submissionSetUID, String patID, 
            String srcUserID, String altSrcUserID, String srcHostName, 
            String destUserID, String altDestUserID, String destHostName, RetrieveDocumentSetRequestType docReq, Calendar timeStamp, String outcomeIndicator) {
        AuditMessage msg = new AuditMessage();
        msg.setEventIdentification(createEventIdentification(
                EventID.Import, 
                EventActionCode.Create,
                timeStamp,
                outcomeIndicator,
                null,
                eventTypeCode));
        msg.getAuditSourceIdentification().add(
                logger.createAuditSourceIdentification());
        msg.getActiveParticipant().add(
                AuditMessages.createActiveParticipant(srcUserID, altSrcUserID, null, true,
                        srcHostName, machineOrIP(srcHostName), null, RoleIDCode.Source));
        msg.getActiveParticipant().add(
                AuditMessages.createActiveParticipant(destUserID, altDestUserID, null, false,
                        destHostName, machineOrIP(destHostName), null, RoleIDCode.Destination));
        if (patID != null)
            msg.getParticipantObjectIdentification().add(createPatient(null2unknown(patID)));
        if (submissionSetUID != null)
            msg.getParticipantObjectIdentification().add(createSubmissionSet(null2unknown(submissionSetUID)));
        if (docReq != null) {
            addDocuments(msg.getParticipantObjectIdentification(), docReq, null, EventOutcomeIndicator.Success.equals(outcomeIndicator));
        }
        return msg;
    }
    
    /**
     * 
     * @param eventTypeCode
     * @param submissionSetUID
     * @param patID
     * @param srcUserID
     * @param srcHostName
     * @param destUserID
     * @param altDestUserID
     * @param docReq            The RetrieveDocumentRequest with list of requested documents (null if no documents are involved)
     * @param docUIDs           List of retrieved Document UIDs. Used to filter docUIDs for ParticipantObjectIdentification 'Document' according outcomeIndicator. null means no filtering.
     * @param timeStamp
     * @param outcomeIndicator
     * @return
     */
    public static AuditMessage createExport(EventTypeCode eventTypeCode, String submissionSetUID, String patID, 
            String srcUserID, String altSrcUserID, String srcHostName, String destUserID, String altDestUserID, String destHostName,
            RetrieveDocumentSetRequestType docReq, List<String> docUIDs, Calendar timeStamp, String outcomeIndicator) {
        AuditMessage msg = new AuditMessage();
        msg.setEventIdentification(createEventIdentification(
                EventID.Export, 
                EventActionCode.Read,
                timeStamp,
                outcomeIndicator,
                null,
                eventTypeCode));
        msg.getAuditSourceIdentification().add(
                logger.createAuditSourceIdentification());
        msg.getActiveParticipant().add(
                AuditMessages.createActiveParticipant(srcUserID, altSrcUserID, null, true,
                        srcHostName, machineOrIP(srcHostName), null, RoleIDCode.Source));
        msg.getActiveParticipant().add(
                AuditMessages.createActiveParticipant(destUserID, altDestUserID, null, false,
                        destHostName, machineOrIP(destHostName), null, RoleIDCode.Destination));
        if (patID != null)
            msg.getParticipantObjectIdentification().add(createPatient(patID));
        if (submissionSetUID != null)
            msg.getParticipantObjectIdentification().add(createSubmissionSet(null2unknown(submissionSetUID)));
        if (docReq != null) {
            addDocuments(msg.getParticipantObjectIdentification(), docReq, docUIDs, EventOutcomeIndicator.Success.equals(outcomeIndicator));
        }
        return msg;
    }

    public static AuditMessage createQuery(EventTypeCode eventTypeCode, String queryUID, String patID, 
            String homeCommunityID, byte[] adhocQuery, 
            String srcUserID, String altSrcUserID, String srcHostName, 
            String destUserID, String altDestUserID, String destHostName, 
            Calendar timeStamp, String outcomeIndicator) {
        AuditMessage msg = new AuditMessage();
        msg.setEventIdentification(createEventIdentification(
                EventID.Query, 
                EventActionCode.Execute,
                timeStamp,
                outcomeIndicator,
                null,
                eventTypeCode));
        msg.getAuditSourceIdentification().add(
                logger.createAuditSourceIdentification());
        String hostName = AuditLogger.localHost().getHostName();
        msg.getActiveParticipant().add(
                AuditMessages.createActiveParticipant(srcUserID, altSrcUserID, null, true,
                        srcHostName, machineOrIP(srcHostName), null, RoleIDCode.Source));
        msg.getActiveParticipant().add(
                AuditMessages.createActiveParticipant(destUserID, AuditLogger.processID(), null, false,
                        hostName, machineOrIP(hostName), null, RoleIDCode.Destination));
        if (patID != null)
            msg.getParticipantObjectIdentification().add(createPatient(patID));
        if (log.isDebugEnabled())
            log.debug("Query:\n {}", new String(adhocQuery));
        msg.getParticipantObjectIdentification().add(createQueryParticipantObjectIdentification(queryUID, homeCommunityID, adhocQuery));
        return msg;
    }

    public static AuditMessage createPatientFeed(String patID, String eventActionCode, byte[] msh10,
            String srcUserID, String altSrcUserID, String srcHostName, 
            String destUserID, Calendar timeStamp, String outcomeIndicator) {
        AuditMessage msg = new AuditMessage();
        msg.setEventIdentification(createEventIdentification(
                EventID.PatientRecord, 
                eventActionCode,
                timeStamp,
                outcomeIndicator,
                null,
                EventTypeCode.ITI_8_PatientIdentityFeed));
        msg.getAuditSourceIdentification().add(
                logger.createAuditSourceIdentification());
        String hostName = AuditLogger.localHost().getHostName();
        msg.getActiveParticipant().add(
                AuditMessages.createActiveParticipant(srcUserID, altSrcUserID, null, true,
                        srcHostName, machineOrIP(srcHostName), null, RoleIDCode.Source));
        msg.getActiveParticipant().add(
                AuditMessages.createActiveParticipant(destUserID, AuditLogger.processID(), null, false,
                        hostName, machineOrIP(hostName), null, RoleIDCode.Destination));
        ParticipantObjectIdentification patPOI = createPatient(patID);
        patPOI.getParticipantObjectDetail().add(AuditMessages.createParticipantObjectDetail("MSH-10", msh10));
            msg.getParticipantObjectIdentification().add(patPOI);
        return msg;
    }

    private static String machineOrIP(String hostName) {
        return AuditMessages.isIP(hostName) 
                ? AuditMessages.NetworkAccessPointTypeCode.IPAddress
                : AuditMessages.NetworkAccessPointTypeCode.MachineName;
    }

    public static void sendAuditMessage(Calendar timeStamp, AuditMessage msg) throws IncompatibleConnectionException, GeneralSecurityException, IOException {
        log.debug("Send audit message! EventId: {}", msg.getEventIdentification().getEventID().getDisplayName());
        if (log.isDebugEnabled())
            log.debug("Sent audit message:"+AuditMessages.toXML(msg));
        logger.write(timeStamp, msg);
        log.debug("Audit message sent! EventId: {}", msg.getEventIdentification().getEventID().getDisplayName());
    }

    public static ParticipantObjectIdentification createPatient(String patID) {
        return AuditMessages.createParticipantObjectIdentification(
                patID, ParticipantObjectIDTypeCode.ITI_PatientNumber, null,
                (byte[])null, AuditMessages.ParticipantObjectTypeCode.Person, 
                AuditMessages.ParticipantObjectTypeCodeRole.Patient, null, null, null);
    }
    
    public static ParticipantObjectIdentification createSubmissionSet(String submissionSetUID) {
        return AuditMessages.createParticipantObjectIdentification(
                submissionSetUID, 
                new ParticipantObjectIDTypeCode("urn:uuid:a54d6aa5-d40d-43f9-88c5-b4633d873bdd",
                        "IHE XDS Metadata","submission set classificationNode"),
                null, (byte[])null, AuditMessages.ParticipantObjectTypeCode.SystemObject, 
                AuditMessages.ParticipantObjectTypeCodeRole.Job, null, null, null);
    }

    private static void addDocuments(List<ParticipantObjectIdentification> pois,
            RetrieveDocumentSetRequestType req, List<String> docUIDs, boolean success) {
        List<DocumentRequest> docRequests = req.getDocumentRequest();
        List<ParticipantObjectDetail> details;
        for (DocumentRequest docReq : docRequests) {
            if (isAddDocument(docReq.getDocumentUniqueId(), docUIDs, success)) {
                ParticipantObjectIdentification poi = AuditMessages.createParticipantObjectIdentification(
                        docReq.getDocumentUniqueId(), ParticipantObjectIDTypeCode.ITI_ReportNumber,
                        null, (byte[])null, AuditMessages.ParticipantObjectTypeCode.SystemObject, 
                        AuditMessages.ParticipantObjectTypeCodeRole.Report, null, null, null);
                details = poi.getParticipantObjectDetail();
                details.add(AuditMessages.createParticipantObjectDetail("Repository Unique Id", docReq.getRepositoryUniqueId().getBytes()));
                if (docReq.getHomeCommunityId() != null)
                    details.add(AuditMessages.createParticipantObjectDetail("ihe:homeCommunityID", docReq.getHomeCommunityId().getBytes()));
                pois.add(poi);
            }
        }
        
    }
    private static boolean isAddDocument(String docUID, List<String> docUIDs, boolean success) {
        if (docUIDs == null) {
            return success;
        }
        if (docUIDs.contains(docUID)) {
            return success;
        }
        return !success;
    }

    public static ParticipantObjectIdentification createQueryParticipantObjectIdentification(String queryUID, String homeCommunityID, byte[] adhocQuery) {
        return AuditMessages.createParticipantObjectIdentification(queryUID, 
                new ParticipantObjectIDTypeCode("ITI-18", "IHE Transactions", "Registry Stored Query"),
                homeCommunityID, adhocQuery, AuditMessages.ParticipantObjectTypeCode.SystemObject, 
                AuditMessages.ParticipantObjectTypeCodeRole.Query, null, null, null);
    }
    
    private static String null2unknown(String s) {
        return s == null ? UNKNOWN : s;
    }

    public static String info() {
        if (logger == null) {
            return "AuditLogger not configured!";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Audit Source ID:").append(logger.getAuditSourceID())
        .append("\nInstalled:").append(logger.isInstalled());
        for (Connection c : logger.getConnections()) {
            sb.append("\nConnection: ").append(c.getHostname())
            .append(":").append(c.getPort())
            .append('(').append(c.getProtocol()).append(')');
        }
        Device arrDevice = logger.getAuditRecordRepositoryDevice();
        sb.append("\nAudit Record Repository:").append(arrDevice);
        sb.append("\nProcess ID:").append(AuditLogger.processID())
        .append("\nLocalHost:").append(AuditLogger.localHost());
        return sb.toString();
    }
    
}
