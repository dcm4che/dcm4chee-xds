var appCommon = angular.module('appCommon', ['mgcrea.ngStrap.popover']);

appCommon.factory('appLoadingIndicator', [ 'xdsConstants', function(xdsConstants) {

	console.log("AppLoading factory!");

	var loadingPoolSize = 0;

	var loading = {

		// call this when make an ajax request
		start : function() {
			loadingPoolSize++;
		},

		// call this when finished
		finish : function() {
			loadingPoolSize--;

			if (loadingPoolSize < 0)
				loadingPoolSize = 0;
		},

		// call this to show the user the status
		isLoading : function() {
			return loadingPoolSize > 0;
		}

	};

	return loading;
} ]);

appCommon.factory('appHttp', [ 'appNotifications', 'appLoadingIndicator', '$http', function(appNotifications, appLoadingIndicator, $http) {

	var appHttp = {};

	var onSuccess = function(data, status, callback, notification) {
		appLoadingIndicator.finish();
		//if (notification.success != null) appNotifications.showNotification({level:success, text: notification.success}); 

		callback(data, status);
	};
	
	var onError = function(data, status, callback, notification) {
		appLoadingIndicator.finish();
		//if (notification.error != null) appNotifications.showNotification({level:error, text: notification.error}); 

		callback(data, status);
	};
	
	
	appHttp.post = function(url, data, callbackSuccess, callbackError, notification) {
		appLoadingIndicator.start();
		$http.post(url, data).success(function(data, status) {
			onSuccess(data, status, callbackSuccess, notification);
		}).error(function(data, status) {
			onError(data,status,callbackError, notification);
		});
	};

	appHttp.get = function(url, data, callbackSuccess, callbackError, notification) {
		appLoadingIndicator.start();
		$http.get(url, data).success(function(data, status) {
			onSuccess(data, status, callbackSuccess, notification);
		}).error(function(data, status) {
			onError(data,status,callbackError, notification);
		});
	};
	
	return appHttp;

} ]);

appCommon.factory('appNotifications', [ 'xdsConstants', '$timeout', function(xdsConstants, $timeout) {

	var messageShowTimeout = 10000;
	
	var notifications = {

		/**
		 * message = {text:"abc",level:"info"}
		 */
		showNotification : function(message) {
			this.notifications.push(message);
			this.notificationsArchive.push({
					level:message.level,
					text:message.text,
					details:message.details,
					time: new Date()
				});
			var notifs = this;
			$timeout(function() {
				notifs.notifications.shift();
			}, messageShowTimeout);
		},
		notifications : [],
		notificationsArchive : []
	};

	return notifications;
} ]);


appCommon.directive('appNotificationsPopover',['appNotifications','$popover', function(appNotifications, $popover) {
	return {
		link:function($scope, element, atttributes) {
			// bind popover to the element
			$scope.appNotifications = appNotifications;
			var myPopover = $popover(element, {trigger: 'manual', placement:'bottom' ,template:'templates/notifications.html', animation: "am-flip-x"});

			$scope.$watch("appNotifications.notifications.length",function() {
				if (appNotifications.notifications.length > 0)
					myPopover.$promise.then(myPopover.show); else 
						myPopover.$promise.then(myPopover.hide);
						
			});
		}
	};
}]);



appCommon.factory('getIconClassForType', function() {

	var getIconClassForType = function(type) {
		switch (type) {
		case "XDSFolder":
			return "icon-folder";
		case "XDSDocumentEntry":
			return "icon-file3";
		case "XDSSubmissionSet":
			return "icon-drawer3";
		case "XDSAssociation":
			return "icon-expand2";
		default:
			return "icon-file4";
		}
	};
	return getIconClassForType;
});
