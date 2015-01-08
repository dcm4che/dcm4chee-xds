angular.module('dcm4che-config.controllers', [])
    .controller('ServiceManagerCtrl', function ($scope, appHttp, appNotifications, ConfigConfig) {
        $scope.configuration = {};

        $scope.loadDeviceConfig = function (device) {
            appHttp.get("data/config/device/" + device.deviceName, null, function (data) {
                device.config = data;
            }, function (data, status) {
                appNotifications.showNotification({
                    level: "danger",
                    text: "Could not load device config",
                    details: [data, status]
                })
            });

            //mock
            //$scope.devices = JSON.parse('[{"deviceName":"dcm4chee-xds","appEntities":[],"deviceExtensions":["XdsSource","XdsRegistry","XdsRepository","XCAiInitiatingGWCfg","XCAInitiatingGWCfg","XCARespondingGWCfg","XCAiRespondingGWCfg"],"manageable":false},{"deviceName":"tstprx","appEntities":["MYAE"],"deviceExtensions":["HL7DeviceExtension"],"manageable":false}]');
        };

        /*$scope.reconfigureAll = function() {
         $scope.reconfiguring = true;
         $http.get("data/reconfigure-all/").success(function(data) {
         $scope.reloadConfig();
         $scope.reconfiguring = false;
         }).error(function(data) {
         $scope.reloadConfig();
         $scope.reconfiguring = false;
         });
         };*/


        $scope.selectDevice = function (device) {
            $scope.selectedExtension = undefined;
            $scope.selectedDevice = device;
            if ($scope.selectedDevice.config == null) {
                $scope.loadDeviceConfig(device);
            }
        };

        $scope.selectExtension = function (extension) {
            $scope.selectedExtension = extension;
        };

        // load devicelist
        ConfigConfig.load(function () {
            $scope.devices = ConfigConfig.devices;
        });

    }
).controller('DeviceEditorController', function ($scope, appHttp, appNotifications, ConfigConfig) {

        $scope.deviceCollapsed = true;
        $scope.editor = {
            a: 1,
            checkModified: function () {
                $scope.selectedExtension.isModified = !angular.equals($scope.selectedExtension.lastPersistedConfig, $scope.selectedExtension.configuration.rootConfigNode);
            }
        };

        $scope.ConfigConfig = ConfigConfig;

        $scope.saveDevice = function () {
            $scope.isSaving = true;
            appHttp.post("data/config/save-extension", _.omit(extension, ['lastPersistedConfig', 'isModified']), function (data) {
                extension.lastPersistedConfig = angular.copy(extension.configuration.rootConfigNode);
                $scope.isSaving = false;
                $scope.editor.checkModified();
            }, function (data, status) {
                $scope.isSaving = false;
                appNotifications.showNotification({
                    level: "danger",
                    text: "Could not save extension",
                    details: [data, status]
                });
            });

        };


        $scope.cancelChangesExtension = function (extension) {
            extension.configuration.rootConfigNode = angular.copy(extension.lastPersistedConfig);
            $scope.editor.checkModified();
        };

    }
).directive(
    'configNodeEditor',
    [
        function () {
            return {
                scope: {
                    'config': '=',
                    'schema': '=',
                    'editor': '='
                },
                link: function (scope) {


                    scope.getLabel = function (metadata, k) {
                        return metadata.attributes[k].label;
                    };

                },
                template: '<div confignode="config" schema="schema" editor="editor"></div>'
            };
        }]
).directive("confignode", function (RecursionHelper, ConfigConfig) {
        return {
            scope: {
                editor: '=',
                confignode: '=',
                schema: '=',
                parentnode: '=',
                index: '='
            },
            controller: function ($scope) {
                $scope.isNodeComposite = function () {
                    return $scope.schema != null && $scope.schema.type=="object" && $scope.schema.class!="Map";
                };

                $scope.isNodePrimitive = function () {
                    return $scope.schema != null && _.contains(ConfigConfig.primitiveTypes, $scope.schema.type);
                };

            },
            templateUrl: "config-browser/config-attributes.html",
            compile: function (element) {
                // Use the compile function from the RecursionHelper,
                // And return the linking function(s) which it returns
                return RecursionHelper.compile(element);
            }
        };
    }
).controller("ConfigSubNodeController", function ($scope, ConfigConfig) {

        var widths = {"boolean": 6};

        // bootstrap widths for elements with default 12
        $scope.getWidthForType = function (type) {
            if (widths[type] != null)
                return widths[type]; else return 12;
        }

        $scope.$watch("schema", function () {
            if ($scope.schema.type=="object" && $scope.schema.class=="Map") {
                $scope.subNodeSchema = $scope.schema.properties['*'];
            } else
            if ($scope.schema.type=="array") {
                $scope.subNodeSchema = $scope.schema.items;
            } else
            // composite object
            if ($scope.schema.type=="object" && $scope.schema.properties != null) {
                $scope.subNodeSchema = $scope.schema.properties[$scope.k];
            }
        });

        $scope.devices = ConfigConfig.devices;
    }
).controller("MapController", function ($scope, appNotifications) {
        $scope.newkey = $scope.k;

        $scope.addEntry = function () {
            $scope.confignode['new'] = (   $scope.schema.elementMetadata.type == 'Array' ||
            $scope.schema.elementMetadata.type == 'Set' ? [] : {});
            $scope.editor.checkModified();
        };

        /**
         * Saves the edited map key
         * @param oldkey
         * @param newkey
         * @returns {boolean} Returns true if the editor should close, false otherwise
         */
        $scope.saveKey = function (oldkey, newkey) {

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
                appNotifications.showNotification({text: "The key " + newkey + " already exists!", level: "warning"});
                return false;
            } else {
                // if not, set new prop, remove old prop
                $scope.confignode[newkey] = $scope.confignode[oldkey];
                delete $scope.confignode[oldkey];

                $scope.editor.checkModified();

                return true;
            }
        };

        $scope.deleteEntry = function (key) {
            delete $scope.confignode[key];
            $scope.editor.checkModified();
        };
    }
// Configuration of configuration
).factory("ConfigConfig", function (appNotifications, appHttp) {

        var customTypes = ['Device'];
        var primitiveTypes = ["integer", "string", "boolean"];
        var collectionTypes = ["array", "Set", "Map"];
        var nonCompositeTypes = _.union(primitiveTypes, collectionTypes);

        var conf = {
            // this array should be modified by plug-ins in future
            customTypes: customTypes,

            primitiveTypes: primitiveTypes,
            collectionTypes: collectionTypes,
            nonCompositeTypes: nonCompositeTypes,

            devices: [],

            schemas: {},

            // initializes things
            load: function (callback) {
                appHttp.get("data/config/devices", null, function (data) {
                    conf.devices = data;

                    appHttp.get("data/config/schemas", null, function (data) {
                        conf.schemas = data;
                        callback();
                    }, function (data, status) {
                        appNotifications.showNotification({
                            level: "danger",
                            text: "Could not load the configuration schemas",
                            details: [data, status]
                        });
                        callback();
                    });

                }, function (data, status) {
                    appNotifications.showNotification({
                        level: "danger",
                        text: "Could not load the list of devices",
                        details: [data, status]
                    });
                    callback();
                });


            }
        };

        return conf;

    }
);

