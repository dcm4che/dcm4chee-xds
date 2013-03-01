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
package org.dcm4chee.xds2.common.code;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Code repository to maintain defined codes for affinity domain(s).
 * 
 * @author franz.willer@agfa.com
 *
 */
public class CodeRepository {

    private File baseDir;
    private HashMap<String, AffinityDomainCodes> adCodesMap = new HashMap<String,AffinityDomainCodes>();
    
    public static final String DEFAULT_DOMAIN = "default";
    
    private static Logger log = LoggerFactory.getLogger(CodeRepository.class);
    
    public CodeRepository(String baseDir) {
        this(new File(baseDir));
    }
    public CodeRepository(File baseDir) {
        this.baseDir = baseDir;
    }
    
    public AffinityDomainCodes getAffinityDomainCodes(String affinityDomain) {
        AffinityDomainCodes codes = adCodesMap.get(affinityDomain);
        return codes == null ? readCodes(affinityDomain) : codes;
    }
    
    public void addAffinityDomainCodes(String affinityDomain, AffinityDomainCodes adCodes) {
        this.adCodesMap.put(affinityDomain, adCodes);
    }

    public File getBaseDir() {
        return baseDir;
    }
    
    public Set<String> getAffinityDomains() {
        return adCodesMap.keySet();
    }
    
    public void init(String affinityDomain) {
        if ("*".equals(affinityDomain)) {
            String[] adDirs = baseDir.list();
            if (adDirs == null) {
                log.error("Invalid affinity domain base directory! Does not contain any directory.");
            } else {
                for (int i = 0 ; i < adDirs.length ; i++) {
                    readCodes(adDirs[i]);
                }
            }
        } else {
            if (affinityDomain == null)
                affinityDomain = DEFAULT_DOMAIN;
            readCodes(affinityDomain);
        }
    }

    private AffinityDomainCodes readCodes(String affinityDomain) {
        AffinityDomainCodes codes = readCodeFile(affinityDomain);
        if (codes == null)
            codes = adCodesMap.get(DEFAULT_DOMAIN);
        if (codes == null)
            codes = readCodeFile(DEFAULT_DOMAIN);
        return codes == null ? new AffinityDomainCodes() : codes;
    }
    
    private AffinityDomainCodes readCodeFile(String affinityDomain) {
        File f = new File(new File(baseDir, affinityDomain), "codes.xml");
        if (f.isFile()) {
            try {
                final AffinityDomainCodes codes = new AffinityDomainCodes();
                SAXParserFactory.newInstance().newSAXParser().parse(f, new DefaultHandler() {
                    private List<Code> typedCodes = null;
                    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                        if (qName.equals("CodeType")) {
                            String name = attributes.getValue("name");
                            if ("mimeType".equals(name)) {//Ignore mimeType pseudo codes
                                typedCodes = null;
                                return;
                            }
                            String scheme = attributes.getValue("classScheme");
                            codes.addClassSchemeToCodeType(scheme, name);
                            typedCodes = new ArrayList<Code>();
                            codes.addCodes(name, typedCodes);
                        } else if (qName.equals("Code")) {
                            if (typedCodes != null)
                                typedCodes.add( new Code(attributes.getValue("code"),
                                    attributes.getValue("codingScheme"), attributes.getValue("display")));
                        }
                    }
                });
                codes.setAffinityDomain(affinityDomain);
                adCodesMap.put(affinityDomain, codes);
                return codes;
            } catch (Exception x) {
                log.error("Reading codes for affinitydomain "+affinityDomain+" failed!", x);
            }
        } else {
            log.info("File not found: codes.xml file for affinitydomain "+affinityDomain+"! file:"+f);
        }
        return null;
    }
}