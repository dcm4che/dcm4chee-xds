var xdsQueries = angular.module('xdsQueries', []);

xdsQueries.factory('xdsPatientIds', ['$http',function($http) {
	
	var pids = {};
	var tries = 0;
	
	pids.get = function() {
		
		// singleton 
		if (pids.list != null) return pids.list;
		
		// get the list from the reg service
		if (tries++ > 0) return null;

		$http.get("data/reg/patients/").success(function(data,status){
			pids.list = data;
		});
		
		return null;
	};
	
	return pids;
	
}]);

xdsQueries.factory('xdsAdhocQuery', ['$http','appLoadingIndicator',function($http,appLoadingIndicator) {

	var xdsAdhocQuery = {};
	
	/**
	 * Invokes adhoc query and calls the callback in angular http format (data, status, headers, config)
	 */
	xdsAdhocQuery.invoke = function(id,params,callback) {

		
		var makeslot = function(value, arg){
			return	{
	        "valueList": {
	          "value": [
	                  value
	          ],
	        },
	        "name":arg,
	        "slotType": null
			};
	     };
	    
		
		var request = {
			  "requestSlotList": null,
			  "id": null,
			  "comment": null,
			  "responseOption": {
			    "returnType": "LeafClass",
			    "returnComposedObjects": true
			  },
			  "adhocQuery": {
			    "slot": _.map(params, makeslot),
			    "id": id,
			    "home": null,
			    "name": null,
			    "description": null,
			    "versionInfo": null,
			    "classification": [
			      
			    ],
			    "externalIdentifier": [
			      
			    ],
			    "lid": null,
			    "objectType": null,
			    "status": null,
			    "queryExpression": null
			  },
			  "federated": false,
			  "federation": null,
			  "startIndex": 0,
			  "maxResults": -1
			};
		
		var onResponse = function(data,status,headers, config) {
			appLoadingIndicator.finish();
			callback(data,status,headers, config);
		};
		
		appLoadingIndicator.start();
		$http.post("data/reg/query/",request).success(onResponse).error(onResponse);
		
	};

	return xdsAdhocQuery;
}]);
