package org.dcm4chee.conf.browser;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.api.hl7.HL7Configuration;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.BeanVitalizer;
import org.dcm4che3.conf.dicom.DicomConfigurationManager;
import org.dcm4che3.conf.core.adapters.ConfigTypeAdapter;
import org.dcm4che3.conf.core.api.ConfigurableClass;
import org.dcm4che3.conf.dicom.DicomPath;
import org.dcm4che3.net.AEExtension;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DeviceExtension;
import org.dcm4che3.net.hl7.HL7ApplicationExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static class SchemasJSON {

        public SchemasJSON() {
        }

        public Map<String, Object> device;
        /**
         * Simple class name to schema
         */
        public Map<String, Map<String,Object>> deviceExtensions;

        /**
         * Simple class name to schema
         */
        public Map<String, Map<String,Object>> aeExtensions;

        /**
         * Simple class name to schema
         */
        public Map<String, Map<String,Object>> hl7AppExtensions;

    }

    public static class ConfigObjectJSON {

        public ConfigObjectJSON() {
        }

        /**
         * Object here is either a primitive, an array, a list, or Map<String, Object>
         */
        public Map<String,Object> rootConfigNode;
        public Map<String, Object> schema;

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
    DicomConfigurationManager configurationManager;

    @GET
    @Path("/devices")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DeviceJSON> listDevices() throws ConfigurationException {

        List<DeviceJSON> list = new ArrayList<DeviceJSON>();
        for (String deviceName : configurationManager.listDeviceNames()) {
            Device d = configurationManager.findDevice(deviceName);

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
    @Path("/device/{deviceName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String,Object> getDeviceConfig(@PathParam(value = "deviceName") String deviceName) throws ConfigurationException {
        return (Map<String, Object>) configurationManager.getConfigurationStorage().getConfigurationNode(DicomPath.DeviceByName.set("deviceName", deviceName).path(), Device.class);
    }





    @POST
    @Path("/device/{deviceName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyDeviceConfig(@Context UriInfo ctx, @PathParam(value = "deviceName") String deviceName, Map<String, Object> config) throws ConfigurationException {

        // vitalize device to perform basic validation
        configurationManager.vitalizeDevice(deviceName, config);

        configurationManager.getConfigurationStorage().persistNode(DicomPath.DeviceByName.set("deviceName", deviceName).path(), config, Device.class);

        try {
            reloadAllExtensionsOfDevice(ctx, deviceName);
            log.info("Configuration for device {} stored successfully",deviceName);
        } catch (ConfigurationException e) {
            log.warn("Error while reloading the configuration for device "+deviceName,e);
        }

        return Response.ok().build();
    }

    @GET
    @Path("/schemas")
    @Produces(MediaType.APPLICATION_JSON)
    public SchemasJSON getSchema() throws ConfigurationException {


        SchemasJSON schemas = new SchemasJSON();
        schemas.device = getSchemaForConfigurableClass(Device.class);

        schemas.deviceExtensions = new HashMap<>();
        for (Class<? extends DeviceExtension> deviceExt : configurationManager.getRegisteredDeviceExtensions())
            schemas.deviceExtensions.put(deviceExt.getSimpleName(), getSchemaForConfigurableClass(deviceExt));

        schemas.aeExtensions = new HashMap<>();
        for (Class<? extends AEExtension> aeExt : configurationManager.getRegisteredAEExtensions())
            schemas.aeExtensions.put(aeExt.getSimpleName(), getSchemaForConfigurableClass(aeExt));

        schemas.hl7AppExtensions = new HashMap<>();
        for (Class<? extends HL7ApplicationExtension> hl7Ext : configurationManager.getDicomConfigurationExtension(HL7Configuration.class).getRegisteredHL7ApplicationExtensions())
            schemas.aeExtensions.put(hl7Ext.getSimpleName(), getSchemaForConfigurableClass(hl7Ext));

        // TODO: PERFORMANCE: cache schemas
        return schemas;
    }

    private Map<String, Object> getSchemaForConfigurableClass(Class<?> clazz) throws ConfigurationException {
        BeanVitalizer vitalizer = configurationManager.getVitalizer();
        return vitalizer.lookupDefaultTypeAdapter(clazz).getSchema(new AnnotatedConfigurableProperty(clazz), vitalizer);
    }

    /***
     * this method is just left for backwards-compatibility
     * @param ctx
     * @param extJson
     * @throws ConfigurationException
     */
    @Deprecated
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
        if (extClass.getAnnotation(ConfigurableClass.class) == null)
            throw new ConfigurationException("Extension " + extJson.extensionType + " is not configured");

        // get current config
        Device d = configurationManager.findDevice(extJson.deviceName);
        DeviceExtension currentDeviceExt = d.getDeviceExtension(extClass);

        ConfigTypeAdapter ad = configurationManager.getVitalizer().lookupDefaultTypeAdapter(extClass);

        // serialize current
        Map<String, Object> configmap = (Map<String, Object>) ad.toConfigNode(currentDeviceExt, null, configurationManager.getVitalizer());

        // copy all the filled submitted fields
        configmap.putAll(extJson.configuration.rootConfigNode);

        // deserialize back

        DeviceExtension de = (DeviceExtension) ad.fromConfigNode(configmap, new AnnotatedConfigurableProperty(extClass), configurationManager.getVitalizer());

        // merge config
        d.removeDeviceExtension(de);
        d.addDeviceExtension(de);

        configurationManager.merge(d);

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
        XDS_REST_PATH.put("XCAiRespondingGWCfg", "xcai-rs");
        XDS_REST_PATH.put("XCAInitiatingGWCfg", "xca-rs");
        XDS_REST_PATH.put("XCARespondingGWCfg", "xca-rs");
    }

    @GET
    @Path("/reconfigure-all-extensions/{deviceName}")
    public void reloadAllExtensionsOfDevice(@Context UriInfo ctx,@PathParam("deviceName")  String deviceName) throws ConfigurationException {
        Device device = configurationManager.findDevice(deviceName);

        for (DeviceExtension deviceExtension : device.listDeviceExtensions()) {
            String extensionName = deviceExtension.getClass().getSimpleName();
            if (XDS_REST_PATH.get(extensionName)!=null)
                reconfigureExtension(ctx, deviceName, extensionName);
        }


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
