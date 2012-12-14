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
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.dcm4che.conf.api.ConfigurationException;
import org.dcm4che.conf.api.hl7.HL7Configuration;
import org.dcm4che.hl7.HL7Exception;
import org.dcm4che.hl7.HL7Message;
import org.dcm4che.hl7.HL7Segment;
import org.dcm4che.net.Connection;
import org.dcm4che.net.hl7.HL7Application;
import org.dcm4che.net.hl7.HL7MessageListener;
import org.dcm4chee.xds2.registry.ws.XDSRegistryBeanLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class XdsHL7Servlet extends HttpServlet {

    private HL7Configuration hl7Config;
    private HL7Application hl7App;
    
    @EJB
    private XDSRegistryBeanLocal xdsRegistryBean;
    
    public static final Logger log = LoggerFactory.getLogger(XdsHL7Servlet.class);
    
    private final HL7MessageListener handler = new HL7MessageListener() {

        @Override
        public byte[] onMessage(HL7Application hl7App, Connection conn,
                Socket s, HL7Segment msh, byte[] msg, int off, int len,
                int mshlen) throws HL7Exception {
            try {
                log.info("HL7 message received from "+s.getInetAddress()+":\n"+ new String(msg));
                log.info("  received from:"+s.getInetAddress());
                HL7Message hl7msg = HL7Message.parse(msg, null);
                HL7Segment pidSeg = hl7msg.getSegment("PID");
                if (pidSeg == null)
                    throw new HL7Exception(HL7Exception.AR, "PID segment missing!");
                String pid = pidSeg.getField(3, "").trim();
                if (pid.length() < 1)
                    throw new HL7Exception(HL7Exception.AR, "Patient ID (PID[3]) is empty!");
                log.info("#######patient ID:"+pid);
                if (xdsRegistryBean.newPatientID(pid)) {
                    log.info("New Patient ID created:"+pid);
                } else {
                    log.info("Patient ID already exists:"+pid);
                }
                return HL7Message.makeACK(msh, HL7Exception.AA, null).getBytes(null);
            } catch (Exception e) {
                throw new HL7Exception(HL7Exception.AE, e);
            }
        }
    };
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            hl7Config = (HL7Configuration) Class.forName(
                    config.getInitParameter("hl7ConfigurationClass"), false,
                    Thread.currentThread().getContextClassLoader()).newInstance();
            log.info("###### hl7Config:"+hl7Config);
            hl7App = hl7Config.findHL7Application(config.getInitParameter("deviceName"));
            if (hl7App == null) {
                String msg = "HL7 Device '"+config.getInitParameter("deviceName")+"' not found!";
                log.error(msg);
                throw new ConfigurationException(msg);
            }
            log.info("###### HL7 device:"+hl7App.getDevice());
            log.info("###### HL7 Application Name:"+hl7App.getApplicationName());
            log.info("###### HL7 Accepted Message Types:"+Arrays.toString(hl7App.getAcceptedMessageTypes()));
            hl7App.setHL7MessageListener(handler);
            ExecutorService executorService = Executors.newCachedThreadPool();
            hl7App.getDevice().setExecutor(executorService);
            hl7App.getDevice().bindConnections();
        } catch (Exception e) {
            destroy();
            throw new ServletException(e);
        }
        
    }

    @Override
    public void destroy() {
        if (hl7Config != null)
            hl7Config.close();
        if (hl7App != null)
            hl7App.getDevice().unbindConnections();
    }

}
