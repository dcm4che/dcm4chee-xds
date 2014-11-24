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

package org.dcm4chee.xds2.imgsrc;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;

import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DicomObjectWADOProvider implements DicomObjectProvider {
    private static Logger log = LoggerFactory.getLogger(DicomObjectWADOProvider.class);
    private static final int BUF_SIZE = 65535;

    @Override
    public String getProviderName() {
        return "WADO";
    }
    
    @Override
    public DataHandler getDataHandler(String studyIUID, String seriesIUID, String sopIUID, 
            String cfg, List<String> supportedTS) throws IOException {
        log.debug("getDataHandler for sopIUID {} with config string '{}'",sopIUID, cfg);
        int pos = cfg.indexOf(':');
        StringBuilder sb = new StringBuilder(200);
        sb.append(pos == -1 ? cfg : cfg.substring(++pos));
        int qryPos = sb.indexOf("?");
        if (sb.indexOf("requestType=") == -1) {
            sb.append( qryPos == -1 ? "?" : "&");
            sb.append("requestType=WADO");
        }
        if (qryPos == -1 || sb.indexOf("studyUID=", qryPos) == -1) {
            sb.append("&studyUID=").append(studyIUID);
        }
        if (qryPos == -1 || sb.indexOf("seriesUID=", qryPos) == -1) {
            sb.append("&seriesUID=").append(seriesIUID);
        }
        sb.append("&objectUID=").append(sopIUID)
        .append("&contentType=application/dicom&transferSyntax=");
        String compressedTS = getCompressedTS(supportedTS);
        sb.append(compressedTS != null ? compressedTS : UID.ExplicitVRLittleEndian);
        log.info("Get Datahandler for WADO URL {}!", sb);
        URL url = new URL(sb.toString());
        UrlDataSource ds = new UrlDataSource(url);
        String ts = checkWADORequest(ds, supportedTS);
        log.info("TransfersyntaxUID:{}", ts);
        if (!supportedTS.contains(ts)) {
            throw new IOException("No Transfersyntax of "+supportedTS+" is supported! TS of object:"+ts);
        }
        return new DataHandler(ds);
    }

    private String getCompressedTS(List<String> supportedTS) {
        for (String ts : supportedTS) {
            if (!UID.ExplicitVRLittleEndian.equals(ts) && !UID.ImplicitVRLittleEndian.equals(ts)) {
                return ts;
            }
        }
        return null;
    }

    @SuppressWarnings("resource")
    private String checkWADORequest(UrlDataSource ds, List<String> supportedTS) throws IOException {
        InputStream is = null;
        DicomInputStream dis = null;
        try {
            is = ds.getInputStream();
            is.mark(BUF_SIZE);
            dis = new DicomInputStream(new BufferedInputStream(is));
            dis.readDataset(-1, Tag.SeriesInstanceUID);
            is.reset();
            return dis.getFileMetaInformation().getString(Tag.TransferSyntaxUID);
        } catch (IOException x) {
            log.error("Failed to get DICOM object from:"+ds.getURL(), x);
            throw x;
        }
    }
    
    private class UrlDataSource implements DataSource {

        private URL url;
        private URLConnection con;
        private InputStream is;
        private OutputStream os;
        
        public UrlDataSource(URL url) {
            this.url = url;
        }
        @Override
        public InputStream getInputStream() throws IOException {
            if (is == null) {
                is = getConnection().getInputStream();
                if (!(is instanceof BufferedInputStream)) {
                    is = new BufferedInputStream(is, BUF_SIZE);
                }
            }
            return is;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            if (os == null) {
                os = getConnection().getOutputStream();
            }
            return os;
        }

        @Override
        public String getContentType() {
            String type = null;
            try {
                getConnection().getContentType();
            } catch (IOException e) { }
            
            return type != null ? type : "application/octet-stream";
        }
        
        private URLConnection getConnection() throws IOException {
            if (con == null)
                con = url.openConnection();
            con.setConnectTimeout(120000);
            con.setReadTimeout(3600000);
            return con;
        }

        @Override
        public String getName() {
            return url.getFile();
        }
        
        public URL getURL() {
            return url;
        }
    }
}
