var xdsQueries = angular.module('xdsQueries', []);

xdsQueries.factory('xdsPatientIds', [ 'appHttp', 'appNotifications', function(appHttp, appNotifications) {

	var pids = {};
	var tries = 0;

	pids.get = function() {

		// singleton
		if (pids.list != null)
			return pids.list;

		// get the list from the reg service
		if (tries++ > 0)
			return null;

		appHttp.get("data/reg/patients/", null, function(data, status) {
			pids.list = data;
		}, function(data, status) {
			appNotifications.showNotification({
				level : "error",
				text : "Could not load patient ids",
				details : [ data, status ]
			});
		});

		return null;
	};

	return pids;

} ]);

xdsQueries.factory('xdsConfig', [ 'appNotifications', 'appHttp', function(appNotifications, appHttp) {

	var config = {
		xdsAccessibleRepos : []
	};

	appHttp.get("data/reg/allowedRepos", null, function(data) {
		config.xdsAccessibleRepos = data;
	}, function(data, status) {
		appNotifications.showNotification({
			level : "error",
			text : "Could not load the list of accessible repositories",
			details : [ data, status ]
		})
	});

	return config;

} ]);

xdsQueries.factory('xdsAdhocQuery', [ 'appHttp', 'appNotifications', function(appHttp, appNotifications) {

	var xdsAdhocQuery = {};

	/**
	 * Invokes adhoc query and calls the callback in angular http format (data,
	 * status, headers, config)
	 */
	xdsAdhocQuery.invoke = function(id, params, callback) {

		var makeslot = function(value, arg) {
			return {
				"valueList" : {
					"value" : [ value ],
				},
				"name" : arg,
				"slotType" : null
			};
		};

		var request = {
			"requestSlotList" : null,
			"id" : null,
			"comment" : null,
			"responseOption" : {
				"returnType" : "LeafClass",
				"returnComposedObjects" : true
			},
			"adhocQuery" : {
				"slot" : _.map(params, makeslot),
				"id" : id,
				"home" : null,
				"name" : null,
				"description" : null,
				"versionInfo" : null,
				"classification" : [

				],
				"externalIdentifier" : [

				],
				"lid" : null,
				"objectType" : null,
				"status" : null,
				"queryExpression" : null
			},
			"federated" : false,
			"federation" : null,
			"startIndex" : 0,
			"maxResults" : -1
		};

		appHttp.post("data/reg/query/", request, callback, function(data, status) {
			appNotifications.showNotification({
				level : "error",
				text : "Could not execute adhoc query",
				details : [ data, status ]
			});
		});

	};

	return xdsAdhocQuery;
} ]);

xdsQueries.factory('xdsDeleteObjectsService', [ 'appHttp', 'appNotifications', function(appHttp, appNotifications) {

	var xdsDeleteObjectsService = {};

	xdsDeleteObjectsService.invoke = function(uuids) {

		// construct the RemoveObjectRequest
		var objRefList = _.map(uuids, function(val) {
			return {
				"slot" : [

				],
				"id" : val,
				"home" : null,
				"createReplica" : false
			}
		});

		var request = {
			"requestSlotList" : null,
			"id" : null,
			"comment" : null,
			"adhocQuery" : null,
			"objectRefList" : {
				"objectRef" : objRefList
			},
			"deletionScope" : "urn:oasis:names:tc:ebxml-regrep:DeletionScopeType:DeleteAll"
		};

		// make rest call
		appHttp.post("data/reg/delete/", request, function(data, status) {
			appNotifications.showNotification({
				level : "success",
				text : "Objects deleted",
				details : [ data, status ]
			});
		}, function(data, status) {
			appNotifications.showNotification({
				level : "error",
				text : "Could not delete objects",
				details : [ data, status ]
			});
		});
	}
} ]);
