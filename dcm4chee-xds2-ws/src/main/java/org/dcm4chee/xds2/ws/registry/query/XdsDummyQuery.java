package org.dcm4chee.xds2.ws.registry.query;

import org.dcm4chee.xds2.common.exception.XDSException;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.dcm4chee.xds2.ws.registry.XDSRegistryBean;

public class XdsDummyQuery extends StoredQuery {

    public XdsDummyQuery(AdhocQueryRequest req, XDSRegistryBean session) throws XDSException {
        super(req, session);
    }

    @Override
    public AdhocQueryResponse query() {
        return initAdhocQueryResponse();
    }

    @Override
    public String[] getRequiredParameterNames() {
        return null;
    }

}
