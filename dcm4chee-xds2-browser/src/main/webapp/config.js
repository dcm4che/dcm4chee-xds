
angular.module('dcm4che.appCommon.customizations', ['com.agfa.agility.xdsConfig'])
    .constant('customizations', {

        // Change values in this file to customize the app's behavior

        // Agility XDS
        customConfigIndexPage: 'agility-xds-config/xds-config.html',
        xdsDeviceName:'agility-xds',

        logoutEnabled: false,
        useNICETheme: true

    }
);
