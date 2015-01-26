angular.module('com.agfa.agility.xdsConfig', ['dcm4che.config.manager'])
    .controller('AgilityXDSConfigManagerCtrl', function ($scope, customizations, ConfigEditorService){
        $scope.selectedDeviceName = customizations.xdsDeviceName;
        $scope.$watch('schemas')
        $scope.ConfigEditorService = ConfigEditorService;

        $scope.tab = 'registry';

    });