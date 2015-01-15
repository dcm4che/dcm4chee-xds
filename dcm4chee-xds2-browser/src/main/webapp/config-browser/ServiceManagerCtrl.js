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

        };


        $scope.$watch("selectedDevice", function () {
            if ($scope.selectedDevice != null && $scope.selectedDevice.config == null) {
                $scope.loadDeviceConfig($scope.selectedDevice);
            }
        });

        // load devicelist
        ConfigConfig.load(function () {
            $scope.devices = ConfigConfig.devices;
            if ($scope.devices.length > 0)
                $scope.selectedDevice = $scope.devices[0];
        });

    }
).controller('DeviceEditorController', function ($scope, appHttp, appNotifications, ConfigConfig) {

        $scope.ConfigConfig = ConfigConfig;

        $scope.editor = {
            a: 1,
            checkModified: function () {
                //$scope.selectedExtension.isModified = !angular.equals($scope.selectedExtension.lastPersistedConfig, $scope.selectedExtension.configuration.rootConfigNode);
            },
            options: null
        };

        // refresh things on device selection
        $scope.$watch("selectedDevice.config", function () {
            $scope.selectedConfigNode = null;
            $scope.selectedConfigNodeSchema = null;
            $scope.editor.options = null;

            if ($scope.selectedDevice && $scope.selectedDevice.config) {
                $scope.editor.deviceRefs = _.map(ConfigConfig.devices, function (device) {
                    return {
                        name: device.deviceName,
                        ref: "/dicomConfigurationRoot/dicomDevicesRoot/*[dicomDeviceName='" + device.deviceName.replace("'", "&apos;") + "']"
                    }
                });

                $scope.editor.connectionRefs = $scope.selectedDevice ? _.map($scope.selectedDevice.config.dicomConnection, function (connection) {
                    return {
                        name: connection.cn + "," + connection.dcmProtocol + "(" + connection.dicomHostname + ":" + connection.dicomPort + ")",
                        ref: "/dicomConfigurationRoot/dicomDevicesRoot/*[dicomDeviceName='" + $scope.selectedDevice.config.dicomDeviceName.replace("'", "&apos;") + "']/dicomConnection[cn='" + connection.cn.replace("'", "&apos;") + "']"
                    };
                }) : {};
            }

        });


        $scope.selectConfigNode = function (node, schema, options) {
            if (schema == null) throw "Schema not defined";
            $scope.selectedConfigNode = node;
            $scope.selectedConfigNodeSchema = schema;
            $scope.editor.options = options;
        };

        $scope.cancelChangesExtension = function (extension) {
            extension.configuration.rootConfigNode = angular.copy(extension.lastPersistedConfig);
            $scope.editor.checkModified();
        };

    }
).directive("confignode", function (RecursionHelper, ConfigConfig, $filter) {
        return {
            scope: {
                editor: '=',
                confignode: '=',
                schema: '=',
                parentnode: '=',
                index: '=',
                noLabel: '@'
            },
            controller: function ($scope) {


                $scope.isNodePrimitive = function () {
                    return $scope.schema != null && _.contains(ConfigConfig.primitiveTypes, $scope.schema.type);
                };

                $scope.getLabel = function (node, schema) {

                    if (schema.properties.cn != null)
                        return node.cn;
                    else
                        return $filter('limitTo')(angular.toJson(node), 20);
                };

                $scope.toggleShowAllProps = function () {
                    !$scope.doShowAllProps ? $scope.doShowAllProps = true : $scope.doShowAllProps = false;
                };


                $scope.$watch("schema", function () {

                    $scope.doShowAllProps = true;
                    $scope.isShowAllTogglable = false;

                    // determine if there are any primary props
                    if ($scope.schema != null)
                        angular.forEach($scope.schema.properties, function (value, index) {
                            if (_.contains(value.tags, "PRIMARY")) {
                                $scope.doShowAllProps = false;
                                $scope.isShowAllTogglable = true;
                            }
                        });

                    // generate tooltip
                    if ($scope.schema == null)
                        $scope.propertyTooltip = null;
                    else
                        $scope.propertyTooltip = {
                            title: ($scope.schema.description ? $scope.schema.description + "<br/>" : "") +
                            ($scope.schema.default ? "Default: <strong>" + $scope.schema.default + "</strong>" : "")

                        }

                });


            },
            templateUrl: "config-browser/config-attributes.html",
            compile: function (element) {
                // Use the compile function from the RecursionHelper,
                // And return the linking function(s) which it returns
                return RecursionHelper.compile(element);
            }
        };
    }
).controller("CompositeNodeController", function ($scope, ConfigConfig) {
        $scope.$watch("schema", function () {

            $scope.groups = null;

            if ($scope.schema) {
                $scope.groups = _.chain($scope.schema.properties)
                    .map(function (prop) {
                        if (prop.uiGroup) return prop.uiGroup;
                    })
                    .uniq().value();
            }

        });


    }).controller("CollectionController", function ($scope, ConfigConfig) {

        $scope.$watch('confignode', function () {
            $scope.selectedItemConfig = null;
            $scope.selectedItemIndex = null;
        });

        $scope.deleteMapEntry = function (key) {
            delete $scope.confignode[key];

            if (key == $scope.selectedItemIndex) {
                $scope.selectedItemConfig = null;
                $scope.selectedItemIndex = null;
            }

            $scope.editor.checkModified();
        };

        $scope.deleteArrayItem = function (key) {
            $scope.confignode.splice(key, 1);

            // reset the current index if an item is selected
            if ($scope.selectedItemConfig) {
                var newInd = _.indexOf($scope.confignode, $scope.selectedItemConfig);
                if (newInd == -1) {
                    $scope.selectedItemConfig = null;
                    $scope.selectedItemIndex = null;
                } else {
                    $scope.selectedItemIndex = newInd;
                }
            }

            $scope.editor.checkModified();
        };


        $scope.selectItem = function (key, item) {
            $scope.selectedItemConfig = item;
            $scope.selectedItemIndex = key;
        };

        $scope.isCollectionEmpty = function () {
            return _.isEmpty($scope.confignode);
        };


        $scope.addMapEntry = function () {
            $scope.confignode['new'] = null;
            $scope.editor.checkModified();
        };

        /**
         * Saves the edited map key
         * @param oldkey
         * @param newkey
         * @returns {boolean} Returns true if the editor should close, false otherwise
         */
        $scope.saveMapKey = function (oldkey, newkey) {

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


            groupOrder: [
                "General",
                "Affinity domain",
                "XDS profile strictness",
                "Endpoints",
                "Other",
                "Logging"
            ],

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

    }).filter("primaryPropsOnly", function () {
        return function (value, showAllProps) {

            if (showAllProps) return value;

            return _.filter(value, function (value) {
                return _.contains(value.tags, "PRIMARY");
            });
        }
    }).filter("filterProperties", function () {
        return function (value, options) {

            if (options == null) return value;

            return _.filter(value, function (value) {
                return !_.contains(options.excludeProps, value.$key);
            });
        }
    }).filter('toArray', function () {
        return function (obj) {
            if (!(obj instanceof Object)) return obj;
            var map = _.map(obj, function (val, key) {
                return Object.defineProperty(val, '$key', {__proto__: null, value: key});
            });
            return map;
        }
    }).filter('groupSorter', function (ConfigConfig) {
        return function (groups) {
            // force order provided by groupOrder
            return _.chain(ConfigConfig.groupOrder).intersection(groups).union(groups).value();
        };

    });


