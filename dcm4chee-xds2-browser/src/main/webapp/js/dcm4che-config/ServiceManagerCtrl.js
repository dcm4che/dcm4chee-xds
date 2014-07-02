angular.module('dcm4che-config.controllers', [])
.controller('ServiceManagerCtrl', [ '$scope', 'appHttp', 'appNotifications',
	function($scope, appHttp, appNotifications) {
		$scope.configuration = {};

		$scope.reloadDeviceList = function() {
            appHttp.get("data/config/devices", null, function(data) {
				$scope.devices = data;
			}, function(data, status) {
                appNotifications.showNotification({
                    level : "danger",
                    text : "Could not load the list of devices",
                    details : [ data, status ]
                })
            });

            //mock
            //$scope.devices = JSON.parse('[{"deviceName":"dcm4chee-xds","appEntities":[],"deviceExtensions":["XdsSource","XdsRegistry","XdsRepository","XCAiInitiatingGWCfg","XCAInitiatingGWCfg","XCARespondingGWCfg","XCAiRespondingGWCfg"],"managable":false},{"deviceName":"tstprx","appEntities":["MYAE"],"deviceExtensions":["HL7DeviceExtension"],"managable":false}]');
		};

        $scope.loadExtensions = function(device) {
            appHttp.get("data/config/extensions/"+device.deviceName, null, function(data) {
                device.extensions = data;
            }, function(data, status) {
                appNotifications.showNotification({
                    level : "danger",
                    text : "Could not load device extensions",
                    details : [ data, status ]
                })
            });

            //mock
            //$scope.devices = JSON.parse('[{"deviceName":"dcm4chee-xds","appEntities":[],"deviceExtensions":["XdsSource","XdsRegistry","XdsRepository","XCAiInitiatingGWCfg","XCAInitiatingGWCfg","XCARespondingGWCfg","XCAiRespondingGWCfg"],"managable":false},{"deviceName":"tstprx","appEntities":["MYAE"],"deviceExtensions":["HL7DeviceExtension"],"managable":false}]');
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

        $scope.selectDevice = function(device){
            $scope.selectedExtension = undefined;
            $scope.selectedDevice = device;
            if ($scope.selectedDevice.extensions == null) {
                $scope.loadExtensions(device);
            }
        };

        $scope.selectExtension = function(extension){
            $scope.selectedExtension = extension;
        };

		// load devicelist
		$scope.reloadDeviceList();

	} ]

).directive(
    'configNodeEditor',
    [
        function() {
            return {
                scope : {
                    'config' : '='
                },
                link : function(scope) {


                    scope.getLabel = function(metadata, k) {
                        return metadata.attributes[k].label;
                    };

                    scope.metadata = {};

                    scope.$watch(
                        'config',
                        function() {
                            if (scope.config == null) {
                                scope.confignode = null;
                                scope.metadata = null;
                            } else {
                                scope.confignode = scope.config.rootConfigNode;
                                scope.metadata = scope.config.metadata;
                                //scope.reindexmetadata(scope.config.rootConfigNode, scope.config.metadata);
                            }}
                    );
                },
                template : '<div confignode="confignode" metadata="metadata"></div>'
            };
        } ]
).directive("confignode", function(RecursionHelper, ConfigConfig) {
        return {
            scope: {
                confignode: '=',
                metadata : '=',
                parentnode : '=',
                index : '='
            },
            controller: function($scope) {
                $scope.isNodeComposite= function() {
                    return $scope.metadata != null && !_.contains(ConfigConfig.nonCompositeTypes, $scope.metadata.type);
                };

                $scope.isNodePrimitive= function() {
                    return $scope.metadata != null && _.contains(ConfigConfig.primitiveTypes, $scope.metadata.type);
                };

            },
            templateUrl: "templates/dcm4che-config/config-attributes.html",
            compile: function(element) {
                // Use the compile function from the RecursionHelper,
                // And return the linking function(s) which it returns
                return RecursionHelper.compile(element);
            }
        };
    }
).controller("ConfigNodeController", function($scope, ConfigConfig) {

        var widths= {"Boolean":6};

        // bootstrap widths for elements with default 12
        $scope.getWidthForType = function(type) {
            if (widths[type] != null)
            return widths[type]; else return 12;
        }

        // Map, array, or set
        if (_.contains(ConfigConfig.collectionTypes, $scope.metadata.type)) {
            $scope.nodeMetadata = $scope.metadata.elementMetadata;
        } else
        // composite object
        if ($scope.metadata.attributes != null) {
            $scope.nodeMetadata = $scope.metadata.attributes[$scope.k];
        }

        // decides whether to go deeper recursively for the viewer for the provided node
        $scope.doShowExtended = function(node) {
            return typeof node === 'object';
        };
    }
// Configuration of configuration
).factory("ConfigConfig", function() {

        var customTypes = ['Device'];
        var primitiveTypes = ["Integer", "String", "Boolean"];
        var collectionTypes = ["Array", "Set", "Map" ];
        var nonCompositeTypes = _.union(primitiveTypes, collectionTypes);

        return {
            // this array should be modified by plug-ins in future
            customTypes: customTypes,

            primitiveTypes: primitiveTypes,
            collectionTypes:collectionTypes,
            nonCompositeTypes:nonCompositeTypes
        };

    });

