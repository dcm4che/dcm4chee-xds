package org.dcm4chee.xds2.browser;

import java.util.HashMap;
import java.util.Map;

import org.dcm4che3.conf.api.generic.ReflectiveConfig;
import org.dcm4che3.conf.api.generic.ReflectiveConfig.ConfigNode;
import org.dcm4che3.conf.api.generic.adapters.ReflectiveAdapter;
import org.dcm4che3.net.Device;
import org.dcm4chee.xds2.conf.XdsRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(BlockJUnit4ClassRunner.class)
public class UnwrappingSerializationTest {

    private static final String[] MIME_TYPES2 = new String[] { "application/xml", "application/dicom", "application/pdf", "text/plain",
            "text/xml" };

    private XdsRepository createRepo() throws Exception {
        // create registry which will be referenced
        Device regd = new Device("RegDevice");

        Device srcd = new Device("source_device");

        XdsRepository rep = new XdsRepository();

        rep.setApplicationName("AppNNName");
        rep.setRepositoryUID("1.2.3");
        rep.setAcceptedMimeTypes(MIME_TYPES2);
        rep.setSoapLogDir(null);
        rep.setCheckMimetype(true);
        rep.setAllowedCipherHostname("*");
        rep.setLogFullMessageHosts(new String[] {});
        rep.setRetrieveUrl("http://retrieve");
        rep.setProvideUrl("http://provide");
        rep.setForceMTOM(true);

        // reference registry
        Map<String, Device> deviceBySrcUid = new HashMap<String, Device>();

        deviceBySrcUid.put("3.4.5", srcd);
        rep.setSrcDevicebySrcIdMap(deviceBySrcUid);

        XdsRepository repo = rep;
        return repo;
    }

   
    @SuppressWarnings("unchecked")
    @Test
    public void testSerializeUnwrap() throws Exception {


        ObjectMapper om = new ObjectMapper();

        XdsRepository repo = createRepo();

        ReflectiveConfig rconfig = new ReflectiveConfig(null, null);
        ReflectiveAdapter ad = new ReflectiveAdapter(XdsRepository.class);

        ConfigNode cn = ad.serialize(repo, rconfig, null);

        System.out.println(om.writerWithDefaultPrettyPrinter().writeValueAsString(cn));

    }

}
