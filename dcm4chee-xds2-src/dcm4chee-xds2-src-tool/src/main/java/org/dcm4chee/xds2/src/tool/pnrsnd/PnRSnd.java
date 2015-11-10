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

package org.dcm4chee.xds2.src.tool.pnrsnd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.audit.AuditLogger;
import org.dcm4che3.net.audit.AuditRecordRepository;
import org.dcm4che3.util.StringUtils;
import org.dcm4che3.util.UIDUtils;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.common.audit.XDSAudit;
import org.dcm4chee.xds2.common.code.AffinityDomainCodes;
import org.dcm4chee.xds2.common.code.Code;
import org.dcm4chee.xds2.common.code.XADCfgRepository;
import org.dcm4chee.xds2.infoset.rim.RegistryError;
import org.dcm4chee.xds2.infoset.rim.RegistryErrorList;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;
import org.dcm4chee.xds2.src.metadata.Author;
import org.dcm4chee.xds2.src.metadata.DocumentEntry;
import org.dcm4chee.xds2.src.metadata.PnRRequest;
import org.dcm4chee.xds2.src.metadata.Util;
import org.dcm4chee.xds2.src.metadata.XDSFolder;
import org.dcm4chee.xds2.src.metadata.exception.MetadataConfigurationException;
import org.dcm4chee.xds2.src.ws.XDSSourceWSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 *
 */
public class PnRSnd {

    private static ResourceBundle rb = ResourceBundle.getBundle("org.dcm4chee.xds2.src.tool.pnrsnd.messages");
    private static final Logger log = LoggerFactory.getLogger(PnRSnd.class);
    
    private XDSSourceWSClient client = new XDSSourceWSClient();
    
    Device device;
    
    private static Properties props = new Properties();
    static {
        try {
            props.load(new FileInputStream(new File("../conf/pnrsnd.properties")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public PnRSnd() {
        if (Boolean.parseBoolean(props.getProperty("auditTLS"))) {
            initAuditLoggerTLS();
        } else {
            initAuditLoggerUDP();
        }
    }

    private static CommandLine parseComandLine(String[] args)
            throws ParseException{
        Options opts = new Options();
        opts.addOption("h", "help", false, rb.getString("help"));
        opts.addOption("V", "version", false, rb.getString("version"));
        opts.addOption("c", "showCodes", false, "Show defined codes for affinity domain");
        opts.addOption("a", "audit", false, "Send a dummy audit message");
        addURLOption(opts);
        addPropertyOption(opts);
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
            Package p = PnRSnd.class.getPackage();
            String s = p.getName();
            System.out.println(s.substring(s.lastIndexOf('.')+1) + ": " +
                   p.getImplementationVersion());
            System.exit(0);
        }
        if (cl.hasOption("c")) {
            showCodes(cl);
            System.exit(0);
        }
        if (cl.hasOption("a")) {
            new PnRSnd().audit();
            System.exit(0);
        }
        if (cl.hasOption("u")) {
            props.setProperty("URL", cl.getOptionValue("u"));
        }
        if (cl.hasOption("p")) {
            String[] codes = cl.getOptionValues("p");
            int pos;
            for (int i = 0 ; i < codes.length ; i++) {
                pos = codes[i].indexOf('=');
                props.setProperty(codes[i].substring(0, pos), codes[i].substring(++pos));
            }
        }
        return cl;
    }

    @SuppressWarnings("static-access")
    private static void addURLOption(Options opts) {
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("url")
                .withDescription("Repository URL")
                .withLongOpt("url")
                .create("u"));
    }
    @SuppressWarnings("static-access")
    private static void addPropertyOption(Options opts) {
        opts.addOption(OptionBuilder
                .hasArg()
                .withArgName("prop")
                .withDescription("<property name>=<property value>")
                .withLongOpt("property")
                .create("p"));
    }

    public static void main(String[] args) {
        try {
            CommandLine cl = parseComandLine(args);
            PnRSnd main = new PnRSnd();
            PnRRequest pnrReq = main.createPnR(cl);
            main.send(pnrReq);
        } catch (ParseException e) {
            System.err.println("pnrsnd: " + e.getMessage());
            System.err.println(rb.getString("try"));
            System.exit(2);
        } catch (Exception e) {
            System.err.println("pnrsnd: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    private PnRRequest createPnR(CommandLine cl) throws IOException, java.text.ParseException {
        String submUID = props.getProperty("subm.UID");
        if ("new()".equals(submUID)) {
            submUID = UIDUtils.createUID();
            log.info("### submissionUID created:"+submUID);
        }
        PnRRequest pnrReq = new PnRRequest(props.getProperty("sourceID"), submUID, props.getProperty("patID"), props.getProperty("srcPatID"));
        addDocument(cl, pnrReq);
        
        addAuthor(pnrReq.addAuthor());
        pnrReq.setContentTypeCode(new Code(props.getProperty("contentTypeCode")));
        pnrReq.setSubmissionTime(Util.toDate(props.getProperty("submissionTime")));
        addOptional(pnrReq);
        
        addFolder(pnrReq);
        return pnrReq;
    }

    private void addDocument(CommandLine cl, PnRRequest pnrReq)
            throws FileNotFoundException, IOException, java.text.ParseException {
        @SuppressWarnings("unchecked")
        List<String> args = cl.getArgList();
        for (int i = 0, len = args.size() ; i < len ; i++) {
            String docUID = getProperty("doc.UID", i);
            if ("new()".equals(docUID)) {
                docUID = UIDUtils.createUID();
                log.info("### documentUID created:"+docUID);
            }
            File docFile = new File(args.get(i));
            byte[] buffer = new byte[(int)docFile.length()];
            FileInputStream fis = new FileInputStream(docFile);
            fis.read(buffer);
            DocumentEntry doc = pnrReq.addDocumentEntry(docUID, buffer, getProperty("mime", i));
            addAuthor(doc.addAuthor());
            doc.addConfidentialityCode(new Code(getProperty("confidentialityCode", i)));
            doc.addEventCodeList(new Code(getProperty("eventCode", i)));
            doc.setClassCode(new Code(getProperty("classCode", i)));
            doc.setFormatCode(new Code(getProperty("formatCode", i)));
            doc.setHealthcareFacilityTypeCode(new Code(getProperty("healthcareFacilityTypeCode", i)));
            doc.setPracticeSettingCode(new Code(getProperty("practiceSettingCode", i)));
            doc.setTypeCode(new Code(getProperty("typeCode", i)));
            doc.setCreationTime(Util.toDate(getProperty("creationTime", i)));
            doc.setServiceStartTime(Util.toDate(getProperty("serviceStartTime", i)));
            doc.setServiceStopTime(Util.toDate(getProperty("serviceStopTime", i)));
            doc.setLanguageCode(getProperty("languageCode", i));
            addOptional(doc);
        }
    }
    
    private void addFolder(PnRRequest pnrReq) {
        String folderUID = props.getProperty("folder.UID");
        if (folderUID != null) {
            if ("new()".equals(folderUID)) {
                folderUID = UIDUtils.createUID();
                log.info("### folderUID created:"+folderUID);
            }
            XDSFolder folder = pnrReq.addFolder(folderUID);
            folder.addCodeList(new Code(props.getProperty("codeList")));
            folder.setTitle(props.getProperty("folder.title"));
            folder.setComments(props.getProperty("folder.comments"));
            if (Boolean.parseBoolean(props.getProperty("addDocs"))) {
                for ( DocumentEntry doc : pnrReq.getDocumentEntries()) {
                    pnrReq.addAssociation(folder.getID(), doc.getID(), XDSConstants.HAS_MEMBER);
                }
            }
        }
        
    }

    private void addOptional(PnRRequest pnrReq) {
        pnrReq.setTitle(props.getProperty("subm.title"));
        pnrReq.setComments(props.getProperty("subm.comments"));
        pnrReq.addIntendedRecipient(props.getProperty("intendedRecipient"));
    }

    private void addOptional(DocumentEntry doc) {
        doc.setTitle(props.getProperty("doc.title"));
        doc.setComments(props.getProperty("doc.comments"));
        doc.setLegalAuthenticator("legalAuthenticator");
    }

    private void addAuthor(Author author) {
        author.setAuthorPerson(props.getProperty("authorPerson"));
        author.setAuthorInstitutions(getAuthorAttributeValues("authorInstitutions"));
        author.setAuthorRoles(getAuthorAttributeValues("authorRoles"));
        author.setAuthorSpecialities(getAuthorAttributeValues("authorSpecialities"));
        author.setAuthorTelecommunications(getAuthorAttributeValues("authorTelecommunications"));
    }
    
    private List<String> getAuthorAttributeValues(String propName) {
        String value = props.getProperty(propName);
        return value == null ? null : Arrays.asList(StringUtils.split(value, '|'));
    }
    
    private String getProperty(String propName, int idx) {
        String v = props.getProperty(propName+"."+idx);
        return v == null ? props.getProperty(propName) : v;
    }

    private void send(PnRRequest pnrReq) throws MalformedURLException, MetadataConfigurationException {
        configTLS();
        configTimeout();
        URL xdsRepositoryURL = new URL(props.getProperty("URL"));
        RegistryResponseType rsp = client.sendProvideAndRegister(pnrReq, xdsRepositoryURL);
        InetAddress addr = AuditLogger.localHost();
        String localhost = addr == null ? "localhost" : addr.getHostName();//DNS!
        XDSAudit.logSourceExport(pnrReq.getSubmissionSetUID(), pnrReq.getPatientID(), 
                XDSConstants.WS_ADDRESSING_ANONYMOUS, AuditLogger.processID(), localhost, 
                xdsRepositoryURL.toExternalForm(), xdsRepositoryURL.getHost(), 
                XDSConstants.XDS_B_STATUS_SUCCESS.equals(rsp.getStatus()));
        log.info("Response:"+rsp.getStatus());
        RegistryErrorList errors = rsp.getRegistryErrorList();
        if (errors != null) {
            List<RegistryError> errList = errors.getRegistryError();
            RegistryError err;
            for (int i = 0, len = errList.size() ; i < len ;) {
                err = errList.get(i);
                log.info("Error "+(++i)+":");
                log.info("  ErrorCode  :"+err.getErrorCode());
                log.info("  CodeContext:"+err.getCodeContext());
                log.info("  Severity   :"+err.getSeverity());
                log.info("  Value      :"+err.getValue());
                log.info("  Location   :"+err.getLocation());
            }
        }
    }

    private void configTimeout() {
        String connectionTimeout = props.getProperty("connectionTimeout");
        if (connectionTimeout != null)
            System.setProperty("sun.net.client.defaultConnectTimeout", connectionTimeout);
        String receiveTimeout = props.getProperty("receiveTimeout");
        if (receiveTimeout != null)
            System.setProperty("sun.net.client.defaultReadTimeout", receiveTimeout);
        
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

        if (!logger.getAuditRecordRepositoryDeviceNames().contains(arrDevice.getDeviceName()))
            logger.getAuditRecordRepositoryDevices().add(arrDevice);

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

        if (!logger.getAuditRecordRepositoryDeviceNames().contains(arrDevice.getDeviceName()))
            logger.getAuditRecordRepositoryDevices().add(arrDevice);

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
        auditTLS.setHttpProxy(props.getProperty("http.proxy"));
        arrDevice.addConnection(auditTLS);
        arr.addConnection(auditTLS);
        return arrDevice ;
    }

    private void configTLS() {
        final HostnameVerifier origHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        final String allowedUrlHost = props.getProperty("allowedUrlHost");
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String urlHostName, SSLSession session) {
                if ( !origHostnameVerifier.verify ( urlHostName, session)) {
                    if ( isAllowedUrlHost(urlHostName)) {
                        log.warn("Warning: URL Host: "+urlHostName+" vs. "+session.getPeerHost());
                    } else {
                        return false;
                    }
                }
                return true;
            }

            private boolean isAllowedUrlHost(String urlHostName) {
                if (allowedUrlHost == null || "CERT".equals(allowedUrlHost)) return false;
                if ( allowedUrlHost.equals("*")) return true;
                return allowedUrlHost.equals(urlHostName);
            }

        };

        HttpsURLConnection.setDefaultHostnameVerifier(hv);
    }

    private static void showCodes(CommandLine cl) {
        @SuppressWarnings("unchecked")
        Collection<String> args = cl.getArgList();
        log.info("Show codes of affinity domain(s):"+args);
        XADCfgRepository rep = new XADCfgRepository(null, "../conf/affinitydomain");
        if (args.isEmpty() || args.contains("*")) {
            args = rep.getAffinityDomains();
        }
        for (String domain : args) {
            AffinityDomainCodes adCodes = rep.getAffinityDomainCodes(domain);
            log.info("  Affinity domain:"+domain+" (source:"+adCodes.getAffinityDomain()+"):");
            for (String codeType : adCodes.getCodeTypes()) {
                log.info("CodeType:"+codeType);
                for (Code c : adCodes.getCodes(codeType)) {
                    log.info("    "+c);
                }
            }
        }
    }

    private void audit() {
        XDSAudit.logSourceExport("1.2.3.3.2.1", "test^^^&1.2.3.3.2.1&ISO",
                XDSConstants.WS_ADDRESSING_ANONYMOUS, AuditLogger.processID(), "localhost", 
                "http://localhost:8080/testweb", "localhost", true);

    }
}
