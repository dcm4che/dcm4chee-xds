angular.module('com.agfa.agility.xdsConfig', ['dcm4che.config.manager'])
    .controller('AgilityXDSConfigManagerCtrl', function ($scope, customizations, ConfigEditorService) {

        // TODO: refactor to use events

        $scope.ConfigEditorService = ConfigEditorService;
        $scope.$watchCollection('ConfigEditorService.devices', function () {
            $scope.xdsDevice = _.findWhere(ConfigEditorService.devices, {deviceName: customizations.xdsDeviceName});
            if ($scope.xdsDevice)
                $scope.loadDeviceConfig($scope.xdsDevice, function () {
                    $scope.tab = 'registry';
                    try {
                        $scope.xdsHL7AppName = _.keys($scope.xdsDevice.config.deviceExtensions.HL7DeviceExtension.hl7Apps)[0];
                    } catch (e) {
                        //noop
                    }

                });

        });

        $scope.$watchCollection('xdsDevice.config.dicomConnection', function () {
            if (!$scope.xdsDevice || !$scope.xdsDevice.config) $scope.hl7Connection = null;
            else
                $scope.hl7Connection = _.findWhere($scope.xdsDevice.config.dicomConnection, {cn: 'hl7-conn'})
        });
    }).directive('staticInclude', function ($http, $templateCache, $compile) {
        return function (scope, element, attrs) {
            var templatePath = attrs.staticInclude;
            $http.get(templatePath, {cache: $templateCache}).success(function (response) {
                var contents = element.html(response).contents();
                $compile(contents)(scope);
            });
        };
    });