package org.dcm4chee.xds2.registry.ws;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.dcm4chee.xds2.service.ReconfigureEvent;
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
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class XDSAutoInitializerBean {
    
    private static Logger log = LoggerFactory.getLogger(XDSAutoInitializerBean.class);

    @Inject
    XDSRegistryBeanLocal xdsBean;
    
    @PostConstruct
    public void checkAndInitialize() {
        log.info("Checking if the XDS registry needs to be initialized with metadata (XDS Registry auto-initialization)...");
        try {
            xdsBean.checkAndAutoInitializeRegistry();
            log.info("Finished XDS Registry auto-initialization");
        } catch (RuntimeException e) {
            log.error("Auto initialization of XDS registry failed");
        }
    }

    public void onReconfigure(@Observes ReconfigureEvent reconfigureEvent) {
        checkAndInitialize();
    }
    
}
