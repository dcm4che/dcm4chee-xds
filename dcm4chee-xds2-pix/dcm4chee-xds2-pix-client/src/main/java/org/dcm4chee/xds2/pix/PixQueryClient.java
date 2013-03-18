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
package org.dcm4chee.xds2.pix;

import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.dcm4che.hl7.HL7Exception;
import org.dcm4che.hl7.HL7Message;
import org.dcm4che.hl7.HL7Segment;
import org.dcm4che.hl7.MLLPConnection;
import org.dcm4che.net.Connection;
import org.dcm4che.net.Device;
import org.dcm4che.net.IncompatibleConnectionException;
import org.dcm4chee.xds2.common.audit.XDSAudit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * PIX Query Client.
 * 
 * @author franz.willer@agfa.com
 *
 */
public class PixQueryClient {

    private String sendingApplication, sendingFacility;
    private String receivingApplication, receivingFacility;
    private String charset;

    private Connection conn;
    private Connection remote;

    private boolean addDomainToQuery = true;
    
    private static final Logger log = LoggerFactory.getLogger(PixQueryClient.class);

    public PixQueryClient(String sendingApplication, String sendingFacility, String hostname, int port, 
            String receivingApplication, String receivingFacility, String bindHost) {
        Device d1 = new Device("local");
        conn = new Connection();
        conn.setProtocol(Connection.Protocol.HL7);
        d1.addConnection(conn);
        Device d2 = new Device("remote");
        remote = new Connection();
        remote.setProtocol(Connection.Protocol.HL7);
        d2.addConnection(remote);
        this.sendingApplication = sendingApplication;
        this.sendingFacility = sendingFacility;
        this.receivingApplication = receivingApplication;
        this.receivingFacility = receivingFacility;
        remote.setHostname(hostname);
        remote.setPort(port);
        if (bindHost != null)
            conn.setHostname(bindHost);
    }
    
    public String getSendingApplication() {
        return sendingApplication;
    }

    public void setSendingApplication(String sendingApplication) {
        this.sendingApplication = sendingApplication;
    }

    public String getSendingFacility() {
        return sendingFacility;
    }

    public void setSendingFacility(String sendingFacility) {
        this.sendingFacility = sendingFacility;
    }

    public String getReceivingApplication() {
        return receivingApplication;
    }

    public void setReceivingApplication(String receivingApplication) {
        this.receivingApplication = receivingApplication;
    }

    public String getReceivingFacility() {
        return receivingFacility;
    }

    public void setReceivingFacility(String receivingFacility) {
        this.receivingFacility = receivingFacility;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }
    
    public boolean isAddDomainToQuery() {
        return addDomainToQuery;
    }

    public void setAddDomainToQuery(boolean addDomainToQuery) {
        this.addDomainToQuery = addDomainToQuery;
    }

    public void setHttpProxy(String proxy) {
        remote.setHttpProxy(proxy);
    }
    
    public String getHttpProxy() {
        return remote.getHttpProxy();
    }
    
    public void setTlsCipherSuites(String... cipherSuites) {
        conn.setTlsCipherSuites(cipherSuites);
        remote.setTlsCipherSuites(cipherSuites);
    }
    public void setTlsProtocols(String... protocols) {
        conn.setTlsProtocols(protocols);
        remote.setTlsProtocols(protocols);
    }

    public void setKeyStore(String keyStoreURL, String keyStoreType, String keyStorePin, String keyStoreKeyPin) {
        conn.getDevice().setKeyStoreURL(keyStoreURL);
        conn.getDevice().setKeyStoreType(keyStoreType);
        conn.getDevice().setKeyStorePin(keyStorePin);
        conn.getDevice().setKeyStoreKeyPin(keyStoreKeyPin);
    }
    public void setTrustStore(String trustStoreURL, String trustStoreType, String trustStorePin) {
        conn.getDevice().setTrustStoreURL(trustStoreURL);
        conn.getDevice().setTrustStoreType(trustStoreType);
        conn.getDevice().setTrustStorePin(trustStorePin);
    }
    
    public void setKeyStore(KeyStore ks, char[] password) {            
        KeyManagerFactory kmf;
        try {
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, password);
            conn.getDevice().setKeyManager(kmf.getKeyManagers()[0]);
        } catch (Exception e) {
            log.error( "Failed to set key-store from configured certificates", e );
        }        
    }

    
    public void setTrustStore(KeyStore ts) {            
        TrustManagerFactory tmf;
        try {
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
            conn.getDevice().setTrustManager(tmf.getTrustManagers()[0]);
        } catch (Exception e) {
            log.error("Failed to set trust-store from configured certificates", e );
        }        
    }
    
    public void setResponseTimeout(int timeout) {
        conn.setResponseTimeout(timeout);
    }
    
    public String queryXAD(String patID, String domain) throws IOException, IncompatibleConnectionException, GeneralSecurityException, HL7Exception {
        if ( !domain.endsWith("&ISO")) {
            if (domain.indexOf('&') == -1) {
                domain = "&"+domain+"&ISO";
            } else {
                throw new HL7Exception(HL7Exception.AR, "Universal ID type must be ISO!");
            }
        }
        return query(patID, domain, true);
    }
    
    public String query(String patID, String domain, boolean xadQuery) throws IOException, IncompatibleConnectionException, GeneralSecurityException, HL7Exception {
        HL7Segment pidSeg = doQuery(patID, domain.startsWith("^^^") ? domain : "^^^"+domain);
        if (pidSeg == null) {
            log.warn("PatID not found or no corresponding patID in given domain! patID:"+patID+" domain:"+domain);
            return null;
        }
        String[] cx, hd, dhd;
        String[] pids = HL7Segment.split(pidSeg.getField(3, ""), pidSeg.getRepetitionSeparator());
        char cSep = pidSeg.getComponentSeparator();
        char scSep = pidSeg.getSubcomponentSeparator();
        for (int i = 0; i < pids.length ; i++) {
            log.debug("#### patientID:"+pids[i]);
            cx = HL7Segment.split(pids[i], cSep);
            if (cx.length > 3 && cx[3].indexOf(domain) != -1) {
                hd = HL7Segment.split(cx[3], scSep);
                dhd = HL7Segment.split(domain, scSep);
                if (dhd.length > 2 && hd.length > 2 && dhd[1].equals(hd[1]) && dhd[2].equals(hd[2])) {
                    return xadQuery ? cx[0]+"^^^&"+hd[1]+"&ISO" : pids[i];
                } else if (dhd[0].equals(hd[0])) {
                    return pids[i];
                }
            }
        }
        return null;
    }

    public HL7Segment doQuery(String patID, String... domains) throws IOException, IncompatibleConnectionException, GeneralSecurityException, HL7Exception {
        Socket sock = conn.connect(remote);
        HL7Segment pidSeg = null;
        byte[] msg = null, msh10 = null;
        boolean success = false;
        try {
            sock.setSoTimeout(conn.getResponseTimeout());
            MLLPConnection mllp = new MLLPConnection(sock);
            HL7Message qbp = HL7Message.makePixQuery(patID, domains);
            HL7Segment msh = qbp.get(0);
            msh.setSendingApplicationWithFacility(sendingApplication+"^"+sendingFacility);
            msh.setReceivingApplicationWithFacility(receivingApplication+"^"+receivingFacility);
            msh.setField(17, charset);
            msg = qbp.getBytes(charset);
            msh10 = msh.getField(10, "").getBytes(charset);
            log.debug("Send Query message:\n{}",new String(msg));
            mllp.writeMessage(msg);
            byte[] rspMsg = mllp.readMessage();
            if (rspMsg == null)
                throw new IOException("Connection closed by receiver");
            log.debug("Query Response:\n{}",new String(rspMsg));
            HL7Message hl7msg = HL7Message.parse(rspMsg, charset);
            HL7Segment ack = hl7msg.getSegment("MSA");
            if (!"AA".equals(ack.getField(1, "AE"))) {
                throw new HL7Exception(ack.getField(1, "AE"), "Error response: MSA:"+ack+" ERR:"+hl7msg.getSegment("ERR"));
            }
            pidSeg = hl7msg.getSegment("PID");
            if (pidSeg != null && pidSeg.getField(3, null) == null)
                throw new HL7Exception(HL7Exception.AR, "Patient ID (PID[3]) is empty!");
            success = true;
            return pidSeg;
        } finally {
            conn.close(sock);
            if (XDSAudit.getAuditLogger() != null) {
                XDSAudit.logPixQuery(patID, sendingApplication, sendingFacility, receivingApplication, receivingFacility, 
                        remote.getHostname(), msg, msh10, success);
            }
        }
    }
}