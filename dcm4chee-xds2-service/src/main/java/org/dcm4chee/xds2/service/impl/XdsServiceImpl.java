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
package org.dcm4chee.xds2.service.impl;

import org.dcm4che3.audit.AuditMessages.EventTypeCode;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.core.api.ConfigurationException;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DeviceExtension;
import org.dcm4che3.net.audit.AuditLogger;
import org.dcm4che3.net.hl7.HL7DeviceExtension;
import org.dcm4che3.net.hl7.service.HL7Service;
import org.dcm4che3.net.hl7.service.HL7ServiceRegistry;
import org.dcm4chee.xds2.common.XdsService;
import org.dcm4chee.xds2.common.audit.XDSAudit;
import org.dcm4chee.xds2.common.cdi.Xds;
import org.dcm4chee.xds2.common.deactivatable.Deactivateable;
import org.dcm4chee.xds2.conf.XdsDeviceNameProvider;
import org.dcm4chee.xds2.service.ReconfigureEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */
@Singleton
@Startup
//@Typed(XdsService.class)
@Xds
public class XdsServiceImpl implements XdsService {

    private static final Logger log = LoggerFactory.getLogger(XdsServiceImpl.class);


    private static final String DO_DEFAULT_CONFIG_PROPERTY =
            "org.dcm4chee.xds.initializeDefaultConfiguration";

    private static String[] JBOSS_PROPERTIES = {
            "jboss.home",
            "jboss.modules",
            "jboss.server.base",
            "jboss.server.config",
            "jboss.server.data",
            "jboss.server.deploy",
            "jboss.server.log",
            "jboss.server.temp",
    };


    @Inject
    private DicomConfiguration conf;

    @Inject
    private Instance<HL7Service> hl7Services;

    @Inject
    @Named("usedDeviceExtension")
    private Instance<String[]> usedDeviceExtension;

    @Inject
    Event<ReconfigureEvent> reconfigureEvent;

    @Inject
    @Xds
    @Named("xdsServiceType")
    private String xdsServiceType;


    @Inject
    @Named("deviceNameProperty")
    private String deviceNameProperty;


    @Inject
    private XdsDeviceNameProvider deviceNameProvider;

    private Device device;

    private boolean running;

    boolean hl7serviceAvail = false;

    private final HL7ServiceRegistry hl7ServiceRegistry = new HL7ServiceRegistry();

    private void addJBossDirURLSystemProperties() {
        for (String key : JBOSS_PROPERTIES) {
            String url = new File(System.getProperty(key + ".dir"))
                    .toURI().toString();
            System.setProperty(key + ".url", url.substring(0, url.length() - 1));
        }
    }

    private Device findDevice() throws ConfigurationException {
        return conf.findDevice(getDeviceName());
    }

    private String getDeviceName() {
        String deviceName = System.getProperty(deviceNameProperty);
        if (deviceName == null)
            deviceName = System.getProperty(DEVICE_NAME_PROPERTY, DEF_DEVICE_NAME);
        return deviceName;
    }


    @PostConstruct
    public void init() {
        log.info("Initializing XDS service for {}", xdsServiceType);
        addJBossDirURLSystemProperties();
        try {
            device = findDevice();

            AuditLogger logger = device.getDeviceExtension(AuditLogger.class);
            AuditLogger.setDefaultLogger(logger);
            XDSAudit.setAuditLogger(logger);
            HL7DeviceExtension hl7Extension = device.getDeviceExtension(HL7DeviceExtension.class);
            if (hl7Extension != null) {

                int hl7ServicesCount = 0;

                // Ugly, but works.
                // The issue seems to be correlated to late webservice start (thus fails if webservice does not start until now),
                // therefore it will most likely succeed once webservice is up.
                //
                // Latest observation: failed after 10 retries,
                // but before "JBAS015539: Starting service jboss.ws.endpoint."xds-registry.ear"."dcm4chee-xds2-registry-ws-2.0.6.v20150303_1255.jar".XDSRegistryBeanWS"

                int retries = Integer.valueOf(System.getProperty("org.dcm4chee.xds.hl7IteratorRetriesSec", "300"));
                Iterator<HL7Service> hl7ServiceIterator = null;
                while (retries>0)
                    try {
                        hl7ServiceIterator = hl7Services.iterator();
                        break;
                    } catch (RuntimeException e) {
                        log.warn("(JBoss issue?) NPE while iterating over HL7Service beans. Re-trying...", e);
                        retries--;
                        Thread.sleep(1000);
                    }
                // endof workaround

                while (hl7ServiceIterator.hasNext()) {
                    HL7Service service = hl7ServiceIterator.next();
                    hl7ServiceRegistry.addHL7Service(service);
                    hl7serviceAvail = true;
                    hl7ServicesCount++;
                }

                log.info("Registering HL7 services for {} @ {} ({} in total) ...", new Object[]{xdsServiceType, device.getDeviceName(), hl7ServicesCount});

                if (hl7serviceAvail) {
                    ExecutorService executorService = Executors.newCachedThreadPool();
                    device.setExecutor(executorService);
                    hl7Extension.setHL7MessageListener(hl7ServiceRegistry);
                }
            }

            start();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void destroy() {
        stop();
    }

    @Override
    public void start() throws Exception {
        log.info("Starting XDS extension {} @ {}", xdsServiceType, device.getDeviceName());
        running = true;
        if (hl7serviceAvail) {
            log.info("Starting HL7 XDS services...");
            device.bindConnections();
            log.info("HL7 XDS services started");
        }
        logApplicationActivity(true);
    }

    @Override
    public void stop() {
        log.info("Stopping XDS extension {} @ {}", xdsServiceType, device.getDeviceName());
        running = false;
        if (hl7serviceAvail) {
            log.info("Stopping HL7 XDS services...");
            device.unbindConnections();
            log.info("HL7 XDS services stopped");
        }
        logApplicationActivity(false);
    }

    @Override
    public void reload() {
        try {
            log.info("Reloading XDS extension {} @ {}", xdsServiceType, device.getDeviceName());
            device.reconfigure(findDevice());
            if (hl7serviceAvail) {
                device.rebindConnections();
            }
            reconfigureEvent.fire(new ReconfigureEvent());
        } catch (Exception e) {
            throw new RuntimeException("Cannot reload XDS configuration (device '"+getDeviceName()+"')",e);
        }
    }

    private void logApplicationActivity(boolean started) {
        EventTypeCode event = started ? EventTypeCode.ApplicationStart : EventTypeCode.ApplicationStop;
        if (usedDeviceExtension.isUnsatisfied()) {
            XDSAudit.logApplicationActivity(device.getDeviceName(), event, true);
        } else {
            for (String className : usedDeviceExtension.get()) {
                try {
                    Class<? extends DeviceExtension> extClass = (Class<? extends DeviceExtension>) Class.forName(className);
                    DeviceExtension ext = device.getDeviceExtension(extClass);
                    if (ext instanceof Deactivateable) {
                        if (!((Deactivateable) ext).isDeactivated()) {
                            XDSAudit.logApplicationActivity(getFullApplicationName(ext), event, true);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    log.error("DeviceExtension class not found! className:" + className, e);
                    ;
                }
            }
        }
    }

    private String getFullApplicationName(DeviceExtension ext) {
        try {
            Method m = ext.getClass().getMethod("getApplicationName", (Class<?>[]) null);
            String appName = (String) m.invoke(ext, (Object[]) null);
            return appName + "@" + device.getDeviceName();
        } catch (Exception e) {
            return device.getDeviceName();
        }
    }

    @Override
    @Produces
    public Device getDevice() {
        return device;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

}
