
angular.module('dcm4che.appCommon.customizations', ['dcm4che.config.xds'])
    .constant('customizations', {

        // Change values in this file to customize the app's behavior

        appName: "XDS administration",

        // Agility XDS
        customConfigIndexPage: 'xds-config/xds-config.html',
        xdsDeviceName:'dcm4chee-xds',

        logoutEnabled: true,
        useNICETheme: false

    }
);
