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
        templateUrl: 'templates/2col-view.html',
        controller: 'RouteCtrl'
      }).
      otherwise({
        redirectTo: '/step/0'
      });
  }]);