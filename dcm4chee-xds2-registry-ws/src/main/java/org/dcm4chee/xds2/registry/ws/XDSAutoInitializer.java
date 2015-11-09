package org.dcm4chee.xds2.registry.ws;

import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.dcm4chee.xds2.service.ReconfigureEvent;
import org.dcm4chee.xds2.service.XdsStartUpEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes the registry with metadata on startup in case if there are no identifiables defined in the DB backend
 *
 * @author Roman K
 */
public class XDSAutoInitializer {

    private static Logger log = LoggerFactory.getLogger(XDSAutoInitializer.class);

    @Inject
    XDSRegistryBeanLocal xdsBean;

    private void checkAndInitialize() {
        xdsBean.checkAndAutoInitializeRegistry();
    }

    public void onReconfigure(@Observes ReconfigureEvent reconfigureEvent) {
        checkAndInitialize();
    }

    /**
     * init only AFTER_SUCCESS since before it's too early and don't even try if startup failed
     * @param xdsStartUpEvent
     */
    public void onStartUp(@Observes(during = TransactionPhase.AFTER_SUCCESS) XdsStartUpEvent xdsStartUpEvent) {
        checkAndInitialize();
    }

}
