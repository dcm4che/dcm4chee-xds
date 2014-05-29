package org.dcm4chee.xds2.registry.ws;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes the registry with metadata on startup in case if there are no identifiables defined in the DB backend
 * @author Roman K
 *
 */
@Singleton
@Startup
public class XDSAutoInitializerBean {
    
    private static Logger log = LoggerFactory.getLogger(XDSAutoInitializerBean.class);

    @EJB
    XDSRegistryBeanLocal xdsBean;
    
    @PostConstruct
    public void startup() {
        log.info("Checking if the XDS registry needs to be initialized with metadata...");
        try {
            xdsBean.checkAndAutoInitializeRegistry();
        } catch (RuntimeException e) {
            log.error("Auto initialization of XDS registry failed");
        }
    }
    
    
}
