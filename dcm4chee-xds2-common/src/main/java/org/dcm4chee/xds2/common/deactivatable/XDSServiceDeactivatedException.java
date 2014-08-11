package org.dcm4chee.xds2.common.deactivatable;

/**
 * Indicates that a service is deployed, but deactivated in the configuration
 *
 * @author Roman K
 */
public class XDSServiceDeactivatedException extends Exception {
    public XDSServiceDeactivatedException(String msg) {
        super(msg);
    }

    public XDSServiceDeactivatedException() {
        super();
    }
}
