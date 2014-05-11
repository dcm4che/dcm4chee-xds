angular.module('dcm4che-config.controllers', [])
.controller('ServiceManagerCtrl', [ '$scope', '$http',
	function($scope, $http) {
		$scope.configuration = {};

		$scope.reloadConfig = function() {
			$http.get("data/config/").success(function(data) {
				$scope.configuration = data;
			});
		};

		$scope.reconfigureAll = function() {
			$scope.reconfiguring = true;
			$http.get("data/reconfigure-all/").success(function(data) {
				$scope.reloadConfig();
				$scope.reconfiguring = false;
			}).error(function(data) {
				$scope.reloadConfig();
				$scope.reconfiguring = false;
			});
		};

		// decides whether to go deeper recursively for the viewer for the provided node
		$scope.doShowExtended = function(node) {
			return typeof node === 'object';
		};

		// load config
		$scope.reloadConfig();

	} ]);
