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

import static org.dcm4che.audit.AuditMessages.createEventIdentification;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Calendar;

import org.dcm4che.audit.AuditMessage;
import org.dcm4che.audit.AuditMessages;
import org.dcm4che.audit.AuditMessages.EventActionCode;
import org.dcm4che.audit.AuditMessages.EventID;
import org.dcm4che.audit.AuditMessages.EventOutcomeIndicator;
import org.dcm4che.audit.AuditMessages.EventTypeCode;
import org.dcm4che.audit.AuditMessages.ParticipantObjectIDTypeCode;
import org.dcm4che.audit.AuditMessages.RoleIDCode;
import org.dcm4che.audit.ParticipantObjectIdentification;
import org.dcm4che.net.Connection;
import org.dcm4che.net.Device;
import org.dcm4che.net.IncompatibleConnectionException;
import org.dcm4che.net.audit.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Audit logger for XDS.
 * 
 * @author franz.willer@gmail.com
 *
 */
public class XDSAudit {
    private static AuditLogger logger;

    public static final Logger log = LoggerFactory.getLogger(XDSAudit.class);
    
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
    
    public static void logImport(String submissionSetUID, String patID, AuditRequestInfo info, boolean success) {
        if (logger != null && logger.isInstalled()) {
            try {
                Calendar timeStamp = logger.timeStamp();
                AuditMessage msg = XDSAudit.createImport(submissionSetUID, patID, 
                        info.getSourceUserID(), info.getAlternativeSourceUserID(), info.getRemoteHost(), 
                        info.getDestUserID(), timeStamp, 
                        success ? EventOutcomeIndicator.Success : EventOutcomeIndicator.MinorFailure);
                sendAuditMessage(timeStamp, msg);
            } catch (Exception e) {
                log.warn("Audit log of Import failed!");
                log.debug("AuditLog Exception:", e);
            }
        }
    }

    public static void logQuery(String queryUID, String patID, String homeCommunityID, byte[] adhocQuery, 
            AuditRequestInfo info, boolean success) {
        if (logger != null && logger.isInstalled()) {
            try {
                Calendar timeStamp = logger.timeStamp();
                AuditMessage msg = XDSAudit.createQuery(queryUID, patID, homeCommunityID, adhocQuery, 
                        info.getSourceUserID(), info.getAlternativeSourceUserID(), info.getRemoteHost(), info.getDestUserID(), 
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
                AuditMessage msg = XDSAudit.createPatientFeed(patID, EventActionCode.Create, msh10, 
                        srcUserID, null, remoteHost, destUserID, 
                        timeStamp, success ? EventOutcomeIndicator.Success : EventOutcomeIndicator.MinorFailure);
                sendAuditMessage(timeStamp, msg);
            } catch (Exception e) {
                log.warn("Audit log of Patient Feed failed!");
                log.debug("AuditLog Exception:", e);
            }
        }
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

    public static AuditMessage createImport(String submissionSetUID, String patID, 
            String srcUserID, String altSrcUserID, String srcHostName, 
            String destUserID, Calendar timeStamp, String outcomeIndicator) {
        AuditMessage msg = new AuditMessage();
        msg.setEventIdentification(createEventIdentification(
                EventID.Import, 
                EventActionCode.Create,
                timeStamp,
                outcomeIndicator,
                null,
                EventTypeCode.ITI_42_RegisterDocumentSetB));
        msg.getAuditSourceIdentification().add(
                logger.createAuditSourceIdentification());
        String hostName = AuditLogger.localHost().getHostName();
        msg.getActiveParticipant().add(
                AuditMessages.createActiveParticipant(srcUserID, altSrcUserID, null, true,
                        srcHostName, machineOrIP(srcHostName), null, RoleIDCode.Source));
        msg.getActiveParticipant().add(
                AuditMessages.createActiveParticipant(destUserID, AuditLogger.processID(), null, false,
                        hostName, machineOrIP(hostName), null, RoleIDCode.Destination));
        msg.getParticipantObjectIdentification().add(createPatient(null2unknown(patID)));
        msg.getParticipantObjectIdentification().add(createSubmissionSet(null2unknown(submissionSetUID)));
        return msg;
    }

    public static AuditMessage createQuery(String queryUID, String patID, 
            String homeCommunityID, byte[] adhocQuery, 
            String srcUserID, String altSrcUserID, String srcHostName, 
            String destUserID, Calendar timeStamp, String outcomeIndicator) {
        AuditMessage msg = new AuditMessage();
        msg.setEventIdentification(createEventIdentification(
                EventID.Query, 
                EventActionCode.Execute,
                timeStamp,
                outcomeIndicator,
                null,
                EventTypeCode.ITI_18_RegistryStoredQuery));
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
        log.info("Query:\n"+new String(adhocQuery));
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
        log.debug("AuditMessage:"+AuditMessages.toXML(msg));
        logger.write(timeStamp, msg);
    }

    public static ParticipantObjectIdentification createPatient(String patID) {
        return AuditMessages.createParticipantObjectIdentification(
                patID, ParticipantObjectIDTypeCode.PatientNumber, null,
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
    public static ParticipantObjectIdentification createQueryParticipantObjectIdentification(String queryUID, String homeCommunityID, byte[] adhocQuery) {
        return AuditMessages.createParticipantObjectIdentification(queryUID, 
                new ParticipantObjectIDTypeCode("ITI-18", "IHE Transactions", "Registry Stored Query"),
                homeCommunityID, adhocQuery, AuditMessages.ParticipantObjectTypeCode.SystemObject, 
                AuditMessages.ParticipantObjectTypeCodeRole.Query, null, null, null);
    }
    
    private static String null2unknown(String s) {
        return s == null ? "UNKNOWN" : s;
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
