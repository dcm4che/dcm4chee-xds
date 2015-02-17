'use strict';

/* App Module */


var dcm4cheApp = angular.module('dcm4cheApp', [
    'ngRoute',
    'ngSanitize',
    'ngAnimate',
    'mgcrea.ngStrap',

    'dcm4che.appCommon',
    'dcm4che.appCommon.customizations',

    'dcm4che.browserLinkedView',

    'dcm4che.xds.common',
    'dcm4che.xds.controllers',
    'dcm4che.xds.REST',

    'dcm4che.config.manager'

]);

dcm4cheApp.config(
    function ($routeProvider, customizations) {
        $routeProvider.
            when('/step/:stepNum', {
                templateUrl: 'xds-browser/xds-browser.html',
                controller: 'XdsBrowserCtrl'
            }).when('/service-manager', {
                templateUrl: customizations.customConfigIndexPage ? customizations.customConfigIndexPage : 'config-browser/service-manager.html',
                controller: 'ServiceManagerCtrl'
            }).otherwise({
                redirectTo: '/service-manager'
            });
    });

dcm4cheApp.controller('dcm4cheAppController', function ($scope, appConfiguration) {
    $scope.appConfiguration = appConfiguration;
});