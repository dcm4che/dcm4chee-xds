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

package org.dcm4chee.xds2.registry.ctrl;

import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.dcm4chee.xds2.common.code.AffinityDomainCodes;
import org.dcm4chee.xds2.common.code.Code;
import org.dcm4chee.xds2.common.code.XADCfgRepository;
import org.dcm4chee.xds2.conf.XdsRegistry;
import org.dcm4chee.xds2.ctrl.XdsDeviceCtrl;
import org.dcm4chee.xds2.registry.ws.XDSRegistryBeanLocal;
import org.dcm4chee.xds2.service.XdsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/ctrl")
@RequestScoped
public class XdsRegistryCtrl extends XdsDeviceCtrl {

    @EJB
    private XDSRegistryBeanLocal xdsRegistryBean;
    
    public static final Logger log = LoggerFactory.getLogger(XdsRegistryCtrl.class);
    
    @Context
    private HttpServletRequest request;
    
    @Inject
    private XdsService service;
        
    @GET
    @Path("/initCodes/{affinity}")
    public Response initCodes(@PathParam("affinity") String affinityDomain) {
        log.info("################ Init XDS Affinity domain codes!");
        XdsRegistry cfg = service.getDevice().getDeviceExtension(XdsRegistry.class);
        if (cfg != null && cfg.getCodeRepository() != null) {
            try {
                cfg.getCodeRepository().initCodes(affinityDomain);
                log.info("Load codes of affinity domain "+affinityDomain+" finished!");
                return Response.ok().entity(getAffinityDomainResultMsg(affinityDomain, cfg)).build();
            } catch (Exception x) {
                log.error("Load codes of affinity domain "+affinityDomain+" failed!");
                return Response.serverError().entity("Load codes of affinity domain "+affinityDomain+" failed!"+x).build();
            }
        } else {
            return Response.serverError().entity(getConfigurationString("Code Repository not configured!")).build();
        }
    }

    @GET
    @Path("/showPatIDs/{affinity}")
    public Response showPatIDs(@PathParam("affinity") String affinityDomain) {
        log.info("################ Show Patient ID's!");
        try {
            List<String> patIDs = xdsRegistryBean.listPatientIDs(affinityDomain);
            StringBuilder sb = new StringBuilder(patIDs.size()<<4);
            sb.append("<h4>List of Patient IDs for affinity domain ").append(affinityDomain).append("</h4><pre>");
            for (int i = 0, len = patIDs.size() ; i < len ; i++) {
                sb.append("\n    ").append(patIDs.get(i));
            }
            sb.append("</pre>");
            return Response.ok().entity(sb.toString()).build();
        } catch (Exception x) {
            log.error("List patient IDs of affinity domain failed!", x);
            return Response.serverError().entity("List patient IDs of affinity domain "+affinityDomain+" failed!"+x).build();
        }
    }
    
    private String getAffinityDomainResultMsg(String affinityDomain, XdsRegistry cfg) {
        StringBuilder sb = new StringBuilder();
        sb.append("Load codes of affinity domain <b>").append(affinityDomain).
        append(" </b>finished at ").append(new Date());
        XADCfgRepository codeRepository = cfg.getCodeRepository();
        String[] ads = "*".equals(affinityDomain) ?
            codeRepository.getAffinityDomains().toArray(new String[0]) : new String[]{affinityDomain};
        for (int i = 0 ; i < ads.length ; i++) {
            AffinityDomainCodes adCodes = codeRepository.getAffinityDomainCodes(ads[i]);
            sb.append("<h4>Affinity domain:").append(adCodes.getAffinityDomain()).append("</h4>");
            List<Code> codes;
            for (String codeType : adCodes.getCodeTypes()) {
                sb.append("<h5>Code Type:").append(codeType).append("</h5><pre>");
                codes = adCodes.getCodes(codeType);
                for (int j = 0, len = codes.size() ; j < len ; j++) {
                    sb.append("\n   ").append(codes.get(j));
                }
                sb.append("</pre>");
            }
        }
        return sb.toString();
    }

}
