package org.dcm4chee.xds2.registry.ws;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes the registry with metadata on startup in case if there are no identifiables defined in the DB backend
 * @author Roman K
 *
 */
@Singleton
@Startup
@DependsOn("XdsServiceImpl")
public class XDSAutoInitializerBean {
    
    private static Logger log = LoggerFactory.getLogger(XDSAutoInitializerBean.class);

    @Inject
    XDSRegistryBeanLocal xdsBean;
    
    @PostConstruct
    public void startup() {
        log.info("Checking if the XDS registry needs to be initialized with metadata (XDS Registry auto-initizlization)...");
        try {
            xdsBean.checkAndAutoInitializeRegistry();
            log.info("Finished XDS Registry auto-initizlization");
        } catch (RuntimeException e) {
            log.error("Auto initialization of XDS registry failed");
        }
    }
    
    
}
