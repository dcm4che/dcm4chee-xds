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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.spi.ServiceRegistry;


/**
 * Code repository to maintain defined codes for affinity domain(s).
 * 
 * @author franz.willer@agfa.com
 *
 */
public class XADCfgRepository implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private XADCfgProviderSPI codeProvider;
    private HashMap<String, AffinityDomainCodes> adCodesMap = new HashMap<String,AffinityDomainCodes>();
    private HashMap<String, List<String>> adMimetypes = new HashMap<String,List<String>>();
    private HashMap<String, Map<String,String>> adMimetypeFileExt = new HashMap<String,Map<String,String>>();
    
    public XADCfgRepository(String providerName, String... cfg) {
        init(providerName, cfg);
    }
    
    private void init(String providerName, String[] cfg) {
        Iterator<XADCfgProviderSPI> iter = ServiceRegistry.lookupProviders(XADCfgProviderSPI.class);
        XADCfgProviderSPI p;
        while (iter.hasNext()) {
            p = iter.next();
            if (providerName == null || providerName.equals(p.getName())) {
                codeProvider = p;
                codeProvider.init(cfg);
            }
        }
    }
    
    protected AffinityDomainCodes getCodesOfDomain(String affinityDomain) {
    	return adCodesMap.get(affinityDomain);
    }
    public AffinityDomainCodes getAffinityDomainCodes(String affinityDomain) {
        AffinityDomainCodes codes = adCodesMap.get(affinityDomain);
        return codes == null ? initCodes(affinityDomain) : codes;
    }
    
    public void addAffinityDomainCodes(String affinityDomain, AffinityDomainCodes adCodes) {
        this.adCodesMap.put(affinityDomain, adCodes);
    }

    public void addMimetype(String affinityDomain, String mimetype, String mimeFileExtension) {
        List<String> mimes = adMimetypes.get(affinityDomain);
        if (mimes == null) {
            mimes = new ArrayList<String>();
            this.adMimetypes.put(affinityDomain, mimes);
            
        }
        mimes.add(mimetype);
        if (mimeFileExtension != null) {
            Map<String,String> ext = adMimetypeFileExt.get(affinityDomain);
            if (ext == null) {
                ext = new HashMap<String,String>();
                adMimetypeFileExt.put(affinityDomain, ext);
            }
            ext.put(mimeFileExtension, mimetype);
        }
    }

    public Collection<String> getAffinityDomains() {
        return codeProvider.getAffinityDomains();
    }
    
    public List<String> getMimetypes(String affinityDomain) {
        List<String> mimes = null;
        if (codeProvider.supportMimetypeCfg()) {
            mimes = adMimetypes.get(affinityDomain);
            if (mimes == null) {
                initCodes(affinityDomain);
                mimes = adMimetypes.get(affinityDomain);
            }
        }
        return mimes;
    }
    
    public String getMimetypeForFileExtension(String affinityDomain, String mimetype, String defaultMime) {
        String mime = null;
        if (codeProvider.supportMimetypeCfg()) {
            Map<String, String> ext = adMimetypeFileExt.get(affinityDomain);
            if (ext != null) 
                mime = ext.get(mimetype);
        }
        return mime == null ? defaultMime : mime;
    }
    
    public AffinityDomainCodes initCodes(String affinityDomain) {
        return codeProvider.initCodes(this, affinityDomain);
    }

    public boolean configChanged(String providerName, String... cfg) {
        return codeProvider.configChanged(providerName, cfg);
    }
}