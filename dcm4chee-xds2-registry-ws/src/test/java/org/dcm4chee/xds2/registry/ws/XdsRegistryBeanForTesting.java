package org.dcm4chee.xds2.registry.ws;

import javax.ejb.Stateless;

/**
 * Created by aprvf on 17/07/2014.
 *
 * inject won't work by classname if a class implements an interface,... so this class is a workaround
 */
@Stateless
public class XdsRegistryBeanForTesting  extends XDSRegistryBean{

}
