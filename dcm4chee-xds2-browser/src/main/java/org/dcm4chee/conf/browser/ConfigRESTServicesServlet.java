package org.dcm4chee.conf.browser;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.api.generic.ConfigClass;
import org.dcm4che3.conf.api.generic.ReflectiveConfig;
import org.dcm4che3.conf.api.generic.adapters.ReflectiveAdapter;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DeviceExtension;
import org.dcm4chee.xds2.common.cdi.Xds;
import org.dcm4chee.xds2.ctrl.ConfigObjectJSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigRESTServicesServlet {

    public static final Logger log = LoggerFactory.getLogger(ConfigRESTServicesServlet.class);

    public enum OnlineStatus {
        ONLINE,
        OFFLINE,
        UNSUPPORTED
    }


    public class DeviceJSON {

        public String deviceName;
        public Collection<String> appEntities;
        public Collection<String> deviceExtensions;
        public boolean manageable;

    }

    public static class ExtensionJSON {

        public ExtensionJSON() {
        }

        public String deviceName;
        /**
         * user-friendly name
         */
        public String extensionName;
        /**
         * Classname that will also be used for de-serialization
         */
        public String extensionType;
        /**
         * Is the device currently running 
         */
        //public OnlineStatus isOnline;
        /**
         * Can the user restart the device
         */
        public boolean restartable;
        /**
         * Can the user reconfigure the device
         */
        public boolean reconfigurable;
        public ConfigObjectJSON configuration;

    }

    @Inject
    @Xds
    DicomConfiguration config;

    ReflectiveConfig reflectiveConfig;

    @PostConstruct
    private void init() {
        reflectiveConfig = new ReflectiveConfig(null, config);
    }


    @GET
    @Path("/devices")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DeviceJSON> listDevices() throws ConfigurationException {

        List<DeviceJSON> list = new ArrayList<DeviceJSON>();
        for (String deviceName : config.listDeviceNames()) {
            Device d = config.findDevice(deviceName);

            DeviceJSON jd = new DeviceJSON();
            jd.deviceName = deviceName;
            jd.manageable = false;
            jd.appEntities = d.getApplicationAETitles();

            jd.deviceExtensions = new ArrayList<String>();
            for (DeviceExtension de : d.listDeviceExtensions()) {
                jd.deviceExtensions.add(de.getClass().getSimpleName());
            }

            list.add(jd);
        }

        return list;
    }

    @GET
    @Path("/extensions/{deviceName}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ExtensionJSON> getExtensions(@PathParam(value = "deviceName") String deviceName) throws ConfigurationException {
        Device d = config.findDevice(deviceName);

        List<ExtensionJSON> extList = new ArrayList<ExtensionJSON>();

        for (DeviceExtension de : d.listDeviceExtensions()) {
            if (de.getClass().getAnnotation(ConfigClass.class) == null) continue;

            ExtensionJSON extJson = new ExtensionJSON();

            extJson.deviceName = deviceName;
            extJson.reconfigurable = false;
            extJson.restartable = false;
            extJson.extensionName = de.getClass().getSimpleName();
            extJson.extensionType = de.getClass().getName();
            extJson.configuration = ConfigObjectJSON.serializeDeviceExtension(de);

            extList.add(extJson);
        }
        return extList;
    }

    @SuppressWarnings("unchecked")
    @POST
    @Path("/save-extension")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public void saveConfigForExtension(@Context UriInfo ctx, ExtensionJSON extJson) throws ConfigurationException {


        Class<? extends DeviceExtension> extClass;
        try {
            extClass = (Class<? extends DeviceExtension>) Class.forName(extJson.extensionType);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Extension " + extJson.extensionType + " is not configured", e);
        }

        // check if the supplied classname is actually a configclass
        if (extClass.getAnnotation(ConfigClass.class) == null)
            throw new ConfigurationException("Extension " + extJson.extensionType + " is not configured");

        // get current config
        Device d = config.findDevice(extJson.deviceName);
        DeviceExtension currentDeviceExt = d.getDeviceExtension(extClass);

        ReflectiveAdapter<DeviceExtension> ad = new ReflectiveAdapter(extClass);

        // serialize current
        Map<String, Object> configmap = ad.serialize(currentDeviceExt, reflectiveConfig, null);

        // copy all the filled submitted fields
        configmap.putAll(extJson.configuration.rootConfigNode);

        // deserialize back

        DeviceExtension de = ad.deserialize(configmap, reflectiveConfig, null);

        // merge config
        d.removeDeviceExtension(de);
        d.addDeviceExtension(de);

        config.merge(d);

        // also try to call reconfigure after saving
        try {
            Response response = reconfigureExtension(ctx, extJson.deviceName, extJson.extensionName);
            if (response.getStatus() != 204)
                throw new ConfigurationException("Reconfiguration unsuccessful (HTTP status " + response.getStatus() + ")");
        } catch (ConfigurationException e) {
            log.warn("Unable to reconfigure extension " + extJson.extensionName + " for device " + extJson.deviceName + " after saving", e);
        }

    }

    public static final Map<String, String> XDS_REST_PATH = new HashMap<>();

    static {
        XDS_REST_PATH.put("StorageConfiguration", "xds-rep-rs");
        XDS_REST_PATH.put("XdsRegistry", "xds-reg-rs");
        XDS_REST_PATH.put("XdsRepository", "xds-rep-rs");
        XDS_REST_PATH.put("XCAiInitiatingGWCfg", "xcai-rs");
        XDS_REST_PATH.put("XCAiInitiatingGWCfg", "xcai-rs");
        XDS_REST_PATH.put("XCAInitiatingGWCfg", "xca-rs");
        XDS_REST_PATH.put("XCAInitiatingGWCfg", "xca-rs");
    }

    @GET
    @Path("/reconfigure-extension/{deviceName}/{extension}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reconfigureExtension(@Context UriInfo ctx, @PathParam("deviceName") String deviceName, @PathParam("extension") String extension) throws ConfigurationException {

        String connectedDeviceUrl = System.getProperty("org.dcm4chee.device." + deviceName);

        if (connectedDeviceUrl == null)
            throw new ConfigurationException("The device is not controlled (connected), please inspect the JBoss configuration");

        if (!connectedDeviceUrl.startsWith("http")) {
            URL url = null;
            try {
                url = ctx.getAbsolutePath().toURL();
            } catch (MalformedURLException e1) {
                throw new ConfigurationException("Unexpected exception - protocol must be http"); // should not happen
            }

            String formatStr;
            if (connectedDeviceUrl.startsWith("/"))
                formatStr = "%s://%s%s";
            else
                formatStr = "%s://%s/%s";

            connectedDeviceUrl = String.format(formatStr, url.getProtocol(), url.getAuthority(), connectedDeviceUrl);
        }


        // figure out the URL for reloading the config
        String ext_path = XDS_REST_PATH.get(extension);
        if (ext_path == null)
            throw new ConfigurationException(String.format("Extension not recognized (%s)", extension));


        String reconfUrl = connectedDeviceUrl + (connectedDeviceUrl.endsWith("/")?"":"/") + ext_path + "/ctrl/reload";




        try {
            URL obj = new URL(reconfUrl);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            log.info("Calling configuration reload @ {} ...", reconfUrl);
            int responseCode = con.getResponseCode();
            return Response.status(con.getResponseCode()).build();
        } catch (java.io.IOException e1) {
            throw new ConfigurationException(e1);
        }
    }
}
