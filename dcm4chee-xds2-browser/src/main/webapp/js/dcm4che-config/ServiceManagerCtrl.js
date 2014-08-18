angular.module('dcm4che-config.controllers', [])
.controller('ServiceManagerCtrl', function($scope, appHttp, appNotifications, ConfigConfig) {
		$scope.configuration = {};

        $scope.loadExtensions = function(device) {
            appHttp.get("data/config/extensions/"+device.deviceName, null, function(data) {
                device.extensions = data;

                _.each(device.extensions, function(ext) {
                   ext.lastPersistedConfig = angular.copy(ext.configuration.rootConfigNode);
                });

            }, function(data, status) {
                appNotifications.showNotification({
                    level : "danger",
                    text : "Could not load device extensions",
                    details : [ data, status ]
                })
            });

            //mock
            //$scope.devices = JSON.parse('[{"deviceName":"dcm4chee-xds","appEntities":[],"deviceExtensions":["XdsSource","XdsRegistry","XdsRepository","XCAiInitiatingGWCfg","XCAInitiatingGWCfg","XCARespondingGWCfg","XCAiRespondingGWCfg"],"manageable":false},{"deviceName":"tstprx","appEntities":["MYAE"],"deviceExtensions":["HL7DeviceExtension"],"manageable":false}]');
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
        ConfigConfig.load(function() {
            $scope.devices = ConfigConfig.devices;
        });

	}

).controller('ExtensionEditorController', function($scope, appHttp, appNotifications){

    $scope.editor = {
        a:1,
        checkModified: function() {
            $scope.selectedExtension.isModified = ! angular.equals($scope.selectedExtension.lastPersistedConfig, $scope.selectedExtension.configuration.rootConfigNode);
        }
    };

    $scope.saveExtension = function(extension) {
        $scope.isSaving = true;
        appHttp.post("data/config/save-extension", _.omit(extension, ['lastPersistedConfig','isModified']), function(data) {
            extension.lastPersistedConfig = angular.copy(extension.configuration.rootConfigNode);
            $scope.isSaving = false;
            $scope.editor.checkModified();
        }, function(data, status) {
            $scope.isSaving = false;
            appNotifications.showNotification({
                level : "danger",
                text : "Could not save extension",
                details : [ data, status ]
            });
        });

        //mock
        //$scope.devices = JSON.parse('[{"deviceName":"dcm4chee-xds","appEntities":[],"deviceExtensions":["XdsSource","XdsRegistry","XdsRepository","XCAiInitiatingGWCfg","XCAInitiatingGWCfg","XCARespondingGWCfg","XCAiRespondingGWCfg"],"manageable":false},{"deviceName":"tstprx","appEntities":["MYAE"],"deviceExtensions":["HL7DeviceExtension"],"manageable":false}]');
    };


    $scope.cancelChangesExtension = function(extension) {
        extension.configuration.rootConfigNode = angular.copy(extension.lastPersistedConfig);
        $scope.editor.checkModified();
    };

    }
).directive(
    'configNodeEditor',
    [
        function() {
            return {
                scope : {
                    'config' : '=',
                    'editor' : '='
                },
                link : function(scope) {


                    scope.getLabel = function(metadata, k) {
                        return metadata.attributes[k].label;
                    };

                    scope.metadata = {};

                    scope.$watch(
                        'config.rootConfigNode', function() {
                            scope.confignode = scope.config.rootConfigNode;
                         }
                    );
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
                template : '<div confignode="confignode" metadata="metadata" editor="editor"></div>'
            };
        } ]
).directive("confignode", function(RecursionHelper, ConfigConfig) {
        return {
            scope: {
                editor: '=',
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

        $scope.$watch("metadata", function() {
            // Map, array, or set
            if (_.contains(ConfigConfig.collectionTypes, $scope.metadata.type)) {
                $scope.nodeMetadata = $scope.metadata.elementMetadata;
            } else
            // composite object
            if ($scope.metadata.attributes != null) {
                $scope.nodeMetadata = $scope.metadata.attributes[$scope.k];
            }
        });

        $scope.devices = ConfigConfig.devices;
    }
).controller("MapController", function($scope, appNotifications) {
    $scope.newkey = $scope.k;

    $scope.addEntry= function() {
        $scope.confignode['new'] = (   $scope.metadata.elementMetadata.type == 'Array' ||
            $scope.metadata.elementMetadata.type == 'Set' ? [] : {});
        $scope.editor.checkModified();
    };

    /**
     * Saves the edited map key
     * @param oldkey
     * @param newkey
     * @returns {boolean} Returns true if the editor should close, false otherwise
     */
    $scope.saveKey = function(oldkey,  newkey) {

        // check if not empty
        if (newkey == "") {
            appNotifications.showNotification({text: "The key cannot be empty!", level: "warning"});
            return false;
        }

        // check if not changed
        if (oldkey == newkey) {
            return true;
        }

        if (_.has($scope.confignode, newkey)) {
            // if new key already exists - show notification and abort
            appNotifications.showNotification({text:"The key "+newkey+" already exists!", level:"warning"});
            return false;
        } else {
            // if not, set new prop, remove old prop
            $scope.confignode[newkey] =  $scope.confignode[oldkey];
            delete $scope.confignode[oldkey];

            $scope.editor.checkModified();

            return true;
        }
    };

     $scope.deleteEntry = function(key) {
         delete $scope.confignode[key];
         $scope.editor.checkModified();
     };
}
// Configuration of configuration
).factory("ConfigConfig", function(appNotifications, appHttp) {

        var customTypes = ['Device'];
        var primitiveTypes = ["Integer", "String", "Boolean"];
        var collectionTypes = ["Array", "Set", "Map" ];
        var nonCompositeTypes = _.union(primitiveTypes, collectionTypes);

        var conf = {
            // this array should be modified by plug-ins in future
            customTypes: customTypes,

            primitiveTypes: primitiveTypes,
            collectionTypes:collectionTypes,
            nonCompositeTypes:nonCompositeTypes,

            devices: [],

            // initializes things
            load: function(callback) {
                appHttp.get("data/config/devices", null, function(data) {
                    conf.devices = data;
                    callback();
                }, function(data, status) {
                    appNotifications.showNotification({
                        level : "danger",
                        text : "Could not load the list of devices",
                        details : [ data, status ]
                    });
                    callback();
                });
            }
        };

        return conf;

    }
);

