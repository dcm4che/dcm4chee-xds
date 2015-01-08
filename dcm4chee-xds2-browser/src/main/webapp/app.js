'use strict';

/* App Module */


var dcm4cheBrowserApp = angular.module('dcm4cheBrowserApp', [
  'ngRoute',
  'ngSanitize',
  'ngAnimate',
  'mgcrea.ngStrap',  

  'appCommon',
  'browserLinkedView',
  
  'xds.common',
  'xds.controllers',
  'xds.REST',

  'dcm4che-config.controllers'

]);

dcm4cheBrowserApp.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
    when('/step/:stepNum', {
        templateUrl: 'xds-browser/xds-browser.html',
        controller: 'XdsBrowserCtrl'
      }).
      when('/service-manager', {
          templateUrl: 'config-browser/service-manager.html',
          controller: 'ServiceManagerCtrl'
      }).
      otherwise({
        redirectTo: '/step/0'
      });
  }]);