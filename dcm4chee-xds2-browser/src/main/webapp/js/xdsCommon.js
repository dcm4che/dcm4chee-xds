'use strict';

var xdsCommon = angular.module('xdsCommon', []);

xdsCommon.factory('xdsConfig', [ 'xdsConstants', '$http', function(xdsConstants, $http) {

	var config = {
		xdsAccessibleRepos : []
	};

	$http({
		method : "GET",
		url : "data/reg/allowedRepos"
	}).success(function(data) {
		config.xdsAccessibleRepos = data;
	});

	return config;

} ]);

xdsCommon.factory('xdsEb', [ 'xdsConstants', function(xdsConstants) {

	return {
		extIdValueByIdentScheme : function(obj, scheme) {
			try {
				return _.findWhere(obj.externalIdentifier, {
					identificationScheme : xdsConstants[scheme]
				}).value;
			} catch (e) {
				console.log(e);
				return null;
			}
		},

		slotValueByName : function(obj, name) {
			try {
				return _.findWhere(obj.slot, {
					name : name
				}).valueList.value[0];
			} catch (e) {
				console.log(e);
				return null;
			}
		},

	};

} ]);

xdsCommon.controller('LandingCtrl', [ '$scope', '$http', 'xdsConstants', 'xdsAdhocQuery', 'appLoadingIndicator', 'xdsPatientIds',
		function($scope, $http, xdsConstants, xdsAdhocQuery, appLoadingIndicator, xdsPatientIds) {

			xdsPatientIds.get();
	
			// set the function for checking loading status
			$scope.isLoading = appLoadingIndicator.isLoading;

			$scope.xdsConstants = xdsConstants;
			$scope.xdsPatientIds = xdsPatientIds;

			// current search string
			$scope.searchStr = ""; // 'john^^^&1.2.3.4.5&ISO';

			// adhoc query uuid
			$scope.qid = "";// xdsConstants.B_FindEntities[$scope.currentEntity];
			
			// currently browsed entity (for search by patient id)
			$scope.currentEntity = "";// XDS_FindDocuments";
			$scope.currentStatus = xdsConstants.STATUS_APPROVED;

			// currently browsed instance (or null)
			$scope.currentIdentifiable = null;

			// init path
			$scope.path = [ null ];

			// these are overridden by the linkedbrowser
			$scope.delegates = {
				'showList' : function() {
					console.log('noop');
				},
				'clean' : function() {
					console.log('noop');
				}
			};

		} ]);


xdsCommon.filter('xdsExcludeSlotClassExtId', [ function() {
	return function(input, param) {
		return _.omit(input, "slot", "classification", "externalIdentifier");
	};
} ]);

/**
 * helper to determine the type of identifiable: XDSDocumentEntry, XDSFolder,
 * XDSSubmissionSet, XDSAssociation, or Other.
 */
xdsCommon.factory('xdsGetEntityType', [ 'xdsConstants', function(xdsConstants) {
	var xdsGetEntityType = function(entity) {

		// if association
		if (entity.value && entity.value.objectType == xdsConstants["XDS_Association"])
			return "XDSAssociation";

		try {
			var res = _.intersection(
			// get all extid names
			_.map(entity.value.externalIdentifier, function(extid) {
				return extid.name.localizedString[0].value;
			}),

			// filer only these
			[ "XDSDocumentEntry.uniqueId", "XDSFolder.uniqueId", "XDSSubmissionSet.uniqueId" ]);

			// just reuse the catch below
			if (res[0] == undefined)
				throw 0;

			// there must be only one, so return first without
			// .uniqueId postfix
			return res[0].split(".")[0];
		} catch (e) {
			return "Other";
		}
	};

	return xdsGetEntityType;
} ]);

/**
 * shortened id in a badge
 */
xdsCommon.directive('xdsId', function() {
	return {
		require : '^ngModel',
		scope : {
			ngModel : '='
		},
		template : "<small><span class=\"label label-default\">...{{shorten(ngModel.value.id)}}</span></small>",
		link : function(scope) {
			scope.shorten = function(str) {
				return str.substr(str.length - 5);
			};
		}

	};

});

/**
 * nice representation of an identifiable with corresponding icon and shortened
 * id
 */
xdsCommon.directive('xdsIdentifiable', [
		'getIconClassForType',
		'xdsGetEntityType',
		function(getIconClassForType, xdsGetEntityType) {
			return {
				require : '^ngModel',
				scope : {
					ngModel : '='
				},
				template : '<span ng-class="getIconClassForType(xdsGetEntityType(ngModel))"></span> ' + '<span xds-id ng-model="ngModel" class="xds-id-badge"></span>'
						+ '{{ngModel.value.name.localizedString[0].value}}',
				link : function(scope) {
					scope.getIconClassForType = getIconClassForType;
					scope.xdsGetEntityType = xdsGetEntityType;
				}

			};

		} ]);
