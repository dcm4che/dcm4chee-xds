var appCommon= angular.module('appCommon', []);

appCommon.factory('appLoadingIndicator', ['xdsConstants',function(xdsConstants) {

	var loadingPoolSize = 0;
	
	var loading = {
			
		// call this when make an ajax request
		start: function(){
			loadingPoolSize++;
		}, 
		
		// call this when finished
		finish: function(){
			loadingPoolSize--;
			
			if (loadingPoolSize<0) loadingPoolSize = 0;
		},
		
		// call this to show the user the status
		isLoading: function() {
			return loadingPoolSize > 0;
		}
		
	};	
			
	return loading;
}]);

appCommon.factory('getIconClassForType', function() {

	var getIconClassForType = function(type) {
		switch (type){
			case "XDSFolder" : return "icon-folder"; 
			case "XDSDocumentEntry" : return "icon-file3"; 
			case "XDSSubmissionSet" : return "icon-drawer3"; 
			case "XDSAssociation" 	: return "icon-expand2";
			default: return "icon-file4";
		}
	}
	return getIconClassForType;
});
