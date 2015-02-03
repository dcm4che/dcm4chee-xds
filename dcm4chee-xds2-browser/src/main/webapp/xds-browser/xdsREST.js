var xdsREST = angular.module('dcm4che.xds.REST', []);

xdsREST.factory('xdsConfig', [ 'appNotifications', 'appHttp', function(appNotifications, appHttp) {

    console.log("Loading config...");

    var xdsAccessibleRepos = [];
    var patientIds = [];

	var config = {
		xdsAccessibleRepos : function() { return xdsAccessibleRepos },
        patientIds: function() { return patientIds }
	};

	appHttp.get("data/reg/allowedRepos", null, function(data, status) {
        xdsAccessibleRepos = data;

/*        appNotifications.showNotification({
            level : "success",
            text : "Allowed repositories loaded",
            details : [ data, status ]
        })*/
	}, function(data, status) {
		appNotifications.showNotification({
			level : "danger",
			text : "Could not load the list of accessible repositories",
			details : [ data, status ]
		})
	});

    appHttp.get("data/reg/patients/", null, function(data, status) {

        patientIds = data;

/*        appNotifications.showNotification({
            level : "success",
            text : "Patient ids loaded",
            details : [ data, status ]
        })*/
    }, function(data, status) {
        appNotifications.showNotification({
            level : "danger",
            text : "Could not load patient ids",
            details : [ data, status ]
        });
    });

	return config;

} ]);

xdsREST.factory('xdsAdhocQuery', [ 'appHttp', 'appNotifications', function(appHttp, appNotifications) {

	var xdsAdhocQuery = {};

	/**
	 * Invokes adhoc query and calls the callback in angular http format (data,
	 * status, headers, config)
	 */
	xdsAdhocQuery.invoke = function(id, params, callback) {

		var makeslot = function(value, arg) {
			return {
				"valueList" : {
					"value" : [ value ]
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
				level : "danger",
				text : "Could not execute adhoc query",
				details : [ data, status ]
			});
		});

	};

	return xdsAdhocQuery;
} ]);

xdsREST.factory('xdsDeleteObjectsQuery', [ 'appHttp', 'appNotifications', function(appHttp, appNotifications) {

	return {

        invoke: function (uuids, callback) {

            // construct the RemoveObjectRequest
            var objRefList = _.map(uuids, function (val) {
                return {
                    "slot": [

                    ],
                    "id": val,
                    "home": null,
                    "createReplica": false
                }
            });

            var request = {
                "requestSlotList": null,
                "id": null,
                "comment": null,
                "adhocQuery": null,
                "objectRefList": {
                    "objectRef": objRefList
                },
                "deletionScope": "urn:oasis:names:tc:ebxml-regrep:DeletionScopeType:DeleteAll"
            };

            // make rest call
            appHttp.post("data/reg/delete/", request, function (data, status) {
                callback(true);
                appNotifications.showNotification({
                    level: "success",
                    text: "Objects deleted",
                    details: [ data, status ]
                });
            }, function (data, status) {
                callback(false);
                appNotifications.showNotification({
                    level: "warning",
                    text: "Could not delete objects",
                    details: [ data, status ]
                });
            });
        }
    }
} ]);
