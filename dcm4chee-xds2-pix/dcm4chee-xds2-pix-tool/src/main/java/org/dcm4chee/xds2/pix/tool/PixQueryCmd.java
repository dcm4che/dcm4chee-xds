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

package org.dcm4chee.xds2.pix.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.audit.AuditLogger;
import org.dcm4che3.net.audit.AuditRecordRepository;
import org.dcm4che3.util.StringUtils;
import org.dcm4chee.xds2.common.audit.XDSAudit;
import org.dcm4chee.xds2.pix.PixQueryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 *
 */
public class PixQueryCmd {

    private PixQueryClient client;
    private Device device;
    private static ResourceBundle rb = ResourceBundle.getBundle("org.dcm4chee.xds2.pix.tool.messages");
    private static final Logger log = LoggerFactory.getLogger(PixQueryCmd.class);
    
    private static Properties props = new Properties();
    static {
        try {
            props.load(new FileInputStream(new File("../conf/pixq.properties")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public PixQueryCmd() {
        client = new PixQueryClient(props.getProperty("sendingApplication"), props.getProperty("sendingFacility"),
                props.getProperty("host"), Integer.parseInt(props.getProperty("port")), 
                props.getProperty("receivingApplication"), props.getProperty("receivingFacility"),
                props.getProperty("bindHost"));
        client.setCharset(props.getProperty("charset"));
        client.setAddDomainToQuery(Boolean.valueOf(props.getProperty("addDomainToQuery")));
        client.setHttpProxy(props.getProperty("http.proxy"));
        if (Boolean.valueOf(props.getProperty("useTLS")))
            configureTLS();
        if (Boolean.parseBoolean(props.getProperty("auditTLS"))) {
            initAuditLoggerTLS();
        } else {
            initAuditLoggerUDP();
        }
    }

    private void configureTLS() {
        client.setKeyStore(props.getProperty("keyStoreURL"), props.getProperty("keyStoreType"),
                props.getProperty("keyStorePin"), props.getProperty("keyStoreKeyPin"));
        client.setTrustStore(props.getProperty("trustStoreURL"), props.getProperty("trustStoreType"), 
                props.getProperty("trustStorePin"));
        String ciphers = props.getProperty("cipherSuites");
        if (ciphers != null)
            client.setTlsCipherSuites(StringUtils.split(ciphers, ','));
        String tlsProtocols = props.getProperty("tlsProtocols");
        if (tlsProtocols != null)
            client.setTlsProtocols(StringUtils.split(tlsProtocols, ','));
    }
    private void initAuditLoggerUDP() {
        device = new Device("test");
        Device arrDevice = createARRDevice("arr", props.getProperty("audit.host"), 
                Integer.parseInt(props.getProperty("audit.port")));
        AuditLogger logger = new AuditLogger();
        device.addDeviceExtension(logger);
        Connection auditUDP = new Connection("audit-udp", "localhost");
        auditUDP.setProtocol(Connection.Protocol.SYSLOG_UDP);
        device.addConnection(auditUDP);
        logger.addConnection(auditUDP);
        logger.setAuditSourceTypeCodes("4");
        logger.setAuditRecordRepositoryDevice(arrDevice);
        logger.setIncludeBOM(false);
        XDSAudit.setAuditLogger(logger);
    }
    private void initAuditLoggerTLS() {
        device = new Device("test");
        Device arrDevice = createARRDevice("arr", props.getProperty("audit.host"), 
                Integer.parseInt(props.getProperty("audit.port")));
        AuditLogger logger = new AuditLogger();
        device.addDeviceExtension(logger);
        Connection auditTLS = new Connection("audit-tls", "localhost");
        auditTLS.setProtocol(Connection.Protocol.SYSLOG_TLS);
        auditTLS.setTlsCipherSuites("TLS_RSA_WITH_AES_128_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA");
        auditTLS.setTlsNeedClientAuth(true);
        auditTLS.setTlsProtocols("TLSv1");
        try {
            device.setKeyStoreURL(new File(System.getProperty("javax.net.ssl.keyStore")).toURI().toURL().toString());
            device.setTrustStoreURL(new File(System.getProperty("javax.net.ssl.trustStore")).toURI().toURL().toString());
        } catch (Exception x) {
            log.error("Failed to set keystore URL!", x);
        }
        device.setKeyStoreType("JKS");
        device.setTrustStoreType("JKS");
        device.setKeyStoreKeyPin(System.getProperty("javax.net.ssl.keyStorePassword"));
        device.setKeyStorePin(System.getProperty("javax.net.ssl.keyPassword", System.getProperty("javax.net.ssl.keyStorePassword")));
        device.setTrustStorePin(System.getProperty("javax.net.ssl.trustStorePassword"));
        device.addConnection(auditTLS);
        logger.addConnection(auditTLS);
        logger.setAuditSourceTypeCodes("4");
        logger.setAuditRecordRepositoryDevice(arrDevice);
        logger.setIncludeBOM(false);
        XDSAudit.setAuditLogger(logger);
    }
    private Device createARRDevice(String name, String host, int port) {
        log.info("####ARR:"+host+":"+port);
        Device arrDevice = new Device(name);
        AuditRecordRepository arr = new AuditRecordRepository();
        arrDevice.addDeviceExtension(arr);
        Connection auditUDP = new Connection("audit-udp", host, port);
        auditUDP.setProtocol(Connection.Protocol.SYSLOG_UDP);
        arrDevice.addConnection(auditUDP);
        arr.addConnection(auditUDP);

        Connection auditTLS = new Connection("audit-tls", host, port);
        auditTLS.setProtocol(Connection.Protocol.SYSLOG_TLS);
        auditTLS.setTlsCipherSuites("TLS_RSA_WITH_AES_128_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA");
        auditTLS.setTlsProtocols("TLSv1");
        auditTLS.setHttpProxy(props.getProperty("audit.http.proxy"));
        arrDevice.addConnection(auditTLS);
        arr.addConnection(auditTLS);
        return arrDevice ;
    }

    private static CommandLine parseComandLine(String[] args)
            throws ParseException{
        Options opts = new Options();
        opts.addOption("h", "help", false, rb.getString("help"));
        opts.addOption("V", "version", false, rb.getString("version"));
        CommandLineParser parser = new PosixParser();
        CommandLine cl = parser.parse(opts, args);
        if (cl.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(
                    rb.getString("usage"),
                    rb.getString("description"), opts,
                    rb.getString("example"));
            System.exit(0);
        }
        if (cl.hasOption("V")) {
            Package p = PixQueryCmd.class.getPackage();
            String s = p.getName();
            System.out.println(s.substring(s.lastIndexOf('.')+1) + ": " +
                   p.getImplementationVersion());
            System.exit(0);
        }
        return cl;
    }


    public static void main(String[] args) {
        try {
            CommandLine cl = parseComandLine(args);
            PixQueryCmd main = new PixQueryCmd();
            main.query();
        } catch (ParseException e) {
            log.error("pixq: " + e.getMessage());
            log.error(rb.getString("try"));
            System.exit(2);
        } catch (Exception e) {
            log.error("pixq: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    private void query() throws Exception {
        String xadPID = client.queryXAD(props.getProperty("patID"), props.getProperty("domain"));
        log.info("####### XAD Patient ID:"+xadPID);
    }

}
