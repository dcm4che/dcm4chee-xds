'use strict';

/* App Module */


var xdsBrowserApp = angular.module('xdsbrowserApp', [
  'ngRoute',
  'ngAnimate',
  'ngSanitize',
  'mgcrea.ngStrap',  

  'appCommon',
  'browserLinkedView',
  
  'xdsCommon',
  'xdsConstants',
  'xdsQueries',
  
  'IdentifiableDetailsCtrl',
  'IdentifiableListCtrl',
  'AdhocQueryUICtrl'
  
]);

xdsBrowserApp.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
    when('/step/:stepNum', {
        templateUrl: 'templates/2col-view-browser.html',
        controller: 'RouteCtrl'
      }).
      when('/service-manager', {
          templateUrl: 'templates/service-manager.html',
          controller: 'ServiceManagerCtrl'
      }).
      otherwise({
        redirectTo: '/step/0'
      });
  }]);