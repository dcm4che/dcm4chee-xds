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

package org.dcm4chee.xds2.registry.hl7;

import java.net.Socket;
import java.net.UnknownHostException;

import org.dcm4che3.audit.AuditMessages.EventActionCode;
import org.dcm4che3.hl7.HL7Exception;
import org.dcm4che3.hl7.HL7Message;
import org.dcm4che3.hl7.HL7Segment;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.hl7.HL7Application;
import org.dcm4che3.net.hl7.HL7MessageListener;
import org.dcm4chee.xds2.common.audit.XDSAudit;
import org.dcm4chee.xds2.ws.registry.XDSRegistryBeanLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XdsHL7Service implements HL7MessageListener {

    private XDSRegistryBeanLocal xdsRegistryBean;
    
    private static final Logger log = LoggerFactory.getLogger(XdsHL7Service.class);
    
    public XdsHL7Service(XDSRegistryBeanLocal bean) {
        this.xdsRegistryBean = bean;
    }
    
    @Override
    public byte[] onMessage(HL7Application hl7App, Connection conn,
            Socket s, HL7Segment msh, byte[] msg, int off, int len,
            int mshlen) throws HL7Exception {
        String pid = "";
        String srcUserID = msh.getField(3, "") + '|' + msh.getField(2, "");
        String destUserID = msh.getField(5, "") + '|' + msh.getField(4, "");
        String msgType = msh.getField(9, "ADT-A01");
        String eventActionCode = msgType.endsWith("08") ? 
                EventActionCode.Update : EventActionCode.Create;
        byte[] msh10 = msh.getField(10, "").getBytes();
        String remoteHost;
        try {
            remoteHost = conn.getEndPoint().getHostName();
        } catch (UnknownHostException e1) {
            log.warn("Failed to get remoteHostName!");
            remoteHost ="UNKNOWN";
        }
        boolean success = false;
        try {
            log.info("HL7 message received from "+s.getInetAddress()+":\n"+ new String(msg));
            log.info("  received from:"+s.getInetAddress());
            HL7Message hl7msg = HL7Message.parse(msg, null);
            HL7Segment pidSeg = hl7msg.getSegment("PID");
            if (pidSeg == null)
                throw new HL7Exception(HL7Exception.AR, "PID segment missing!");
            pid = pidSeg.getField(3, "").trim();
            if (pid.length() < 1)
                throw new HL7Exception(HL7Exception.AR, "Patient ID (PID[3]) is empty!");
            log.info("#######patient ID:"+pid);
            if (xdsRegistryBean.newPatientID(pid)) {
                log.info("New Patient ID created:"+pid);
                success = true;
            } else {
                log.info("Patient ID already exists:"+pid);
            }
            return HL7Message.makeACK(msh, HL7Exception.AA, null).getBytes(null);
        } catch (Exception e) {
            if (e instanceof HL7Exception) {
                throw (HL7Exception) e;
            } else {
                throw new HL7Exception(HL7Exception.AE, e);
            }
        } finally {
            XDSAudit.logPatientFeed(pid, eventActionCode, msh10, srcUserID, remoteHost, destUserID, success);
        }
    }
}
