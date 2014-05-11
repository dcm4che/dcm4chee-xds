'use strict';

/* App Module */


var xdsBrowserApp = angular.module('xdsBrowserApp', [
  'ngRoute',
  'ngAnimate',
  'ngSanitize',
  'mgcrea.ngStrap',  

  'appCommon',
  'browserLinkedView',
  
  'xds.common',
  'xds.controllers',
  'xds.REST',

  'dcm4che-config.controllers'

]);

xdsBrowserApp.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
    when('/step/:stepNum', {
        templateUrl: 'templates/xds/xds-browser.html',
        controller: 'XdsBrowserCtrl'
      }).
      when('/service-manager', {
          templateUrl: 'templates/dcm4che-config/service-manager.html',
          controller: 'ServiceManagerCtrl'
      }).
      otherwise({
        redirectTo: '/step/0'
      });
  }]);