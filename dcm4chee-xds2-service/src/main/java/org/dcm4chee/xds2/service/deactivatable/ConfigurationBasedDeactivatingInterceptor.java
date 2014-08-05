/*
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 *   The contents of this file are subject to the Mozilla Public License Version
 *   1.1 (the "License"); you may not use this file except in compliance with
 *   the License. You may obtain a copy of the License at
 *   http://www.mozilla.org/MPL/
 *
 *   Software distributed under the License is distributed on an "AS IS" basis,
 *   WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *   for the specific language governing rights and limitations under the
 *   License.
 *
 *   The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 *   Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 *   The Initial Developer of the Original Code is
 *   Agfa Healthcare.
 *   Portions created by the Initial Developer are Copyright (C) 2014
 *   the Initial Developer. All Rights Reserved.
 *
 *   Contributor(s):
 *   See @authors listed below
 *
 *   Alternatively, the contents of this file may be used under the terms of
 *   either the GNU General Public License Version 2 or later (the "GPL"), or
 *   the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 *   in which case the provisions of the GPL or the LGPL are applicable instead
 *   of those above. If you wish to allow use of your version of this file only
 *   under the terms of either the GPL or the LGPL, and not to allow others to
 *   use your version of this file under the terms of the MPL, indicate your
 *   decision by deleting the provisions above and replace them with the notice
 *   and other provisions required by the GPL or the LGPL. If you do not delete
 *   the provisions above, a recipient may use your version of this file under
 *   the terms of any one of the MPL, the GPL or the LGPL.
 */

package org.dcm4chee.xds2.service.deactivatable;

import org.dcm4che3.net.Device;
import org.dcm4che3.net.DeviceExtension;
import org.dcm4chee.xds2.conf.Deactivateable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;



/**
 * @see org.dcm4chee.xds2.service.deactivatable.DeactivateableByConfiguration
 * @author Roman K
 */
@DeactivateableByConfiguration(extension = DeviceExtension.class)
@Interceptor
public class ConfigurationBasedDeactivatingInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationBasedDeactivatingInterceptor.class);

    @Inject
    Device device;

    @AroundInvoke
    public Object processEjbCall(InvocationContext ctx) throws Exception {

        log.info("Intercepted call, details {}", ctx.getContextData());

        DeactivateableByConfiguration anno = ctx.getMethod().getDeclaringClass().getAnnotation(DeactivateableByConfiguration.class);

        Class<? extends DeviceExtension> extension = anno.extension();

        try {

            if (((Deactivateable) device.getDeviceExtension(extension)).isDeactivated())
                throw new Exception();

        } catch (NullPointerException e) {
            log.warn("The specified extension {} was not found for device {}. Service is deactivated.", extension.getSimpleName(), device.getDeviceName());
            throw new XDSServiceDeactivatedException();
        } catch (ClassCastException e) {
            log.error("The specified extension {} must implement Deactivateable interface to support deactivation", extension.getSimpleName());
            throw new XDSServiceDeactivatedException();
        } catch (Exception e) {
            log.warn("Attempted to use the service for deactivated extension {} for device {}", extension.getSimpleName(), device.getDeviceName());
            throw new XDSServiceDeactivatedException();
        }

        return ctx.proceed();
    }

}
