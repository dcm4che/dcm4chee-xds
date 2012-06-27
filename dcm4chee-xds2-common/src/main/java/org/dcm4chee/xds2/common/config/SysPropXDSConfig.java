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
 * Portions created by the Initial Developer are Copyright (C) 2011
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
package org.dcm4chee.xds2.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SysPropXDSConfig implements XDSConfig {
    //Config System property names
    private static final String CFG_CREATE_MISSING_PID = "org.dcm4chee.xds.cfg.createMissingPID";
    private static final String CFG_CREATE_MISSING_CODES = "org.dcm4chee.xds.cfg.createMissingCodes";
    private static final String DONT_SAVE_CODE_CLASSIFICATIONS = "org.dcm4chee.xds.cfg.dontSaveCodeClassifications";
    private static final String CFG_LOG_DIR = "org.dcm4chee.xds.logdir";

    private final static Logger log = LoggerFactory.getLogger(SysPropXDSConfig.class);
            
    public String getSOAPMsgLoggingDir() {
        return System.getProperty(CFG_LOG_DIR);
    }
    public void setSOAPMsgLoggingDir(String dir) {
        System.setProperty(CFG_LOG_DIR, dir);
    }
    
    public boolean isCreateMissingPIDs() {
        return Boolean.getBoolean(CFG_CREATE_MISSING_PID);
    }
    public void setCreateMissingPIDs(boolean b){
        System.setProperty(CFG_CREATE_MISSING_PID, String.valueOf(b));
    }

    public boolean isCreateMissingCodes() {
        return Boolean.getBoolean(CFG_CREATE_MISSING_CODES);
    }
    public void setCreateMissingCodes(boolean b){
        System.setProperty(CFG_CREATE_MISSING_CODES, String.valueOf(b));
    }

    @Override
    public boolean isDontSaveCodeClassifications() {
        return Boolean.getBoolean(DONT_SAVE_CODE_CLASSIFICATIONS);
    }
    @Override
    public void setDontSaveCodeClassifications(boolean b) {
        System.setProperty(DONT_SAVE_CODE_CLASSIFICATIONS, String.valueOf(b));
    }

    public void logConfig() {
        log.info("#### XDS configuration (System Properties):");
        log.info("SOAP Message log dir:"+System.getProperty(CFG_LOG_DIR));
        log.info("Create missing PIDs :"+System.getProperty(CFG_CREATE_MISSING_PID));
        log.info("Create missing codes:"+System.getProperty(CFG_CREATE_MISSING_CODES));
        log.info("Don't save classifications of XDSCodes:"+System.getProperty(DONT_SAVE_CODE_CLASSIFICATIONS));
    }
}
