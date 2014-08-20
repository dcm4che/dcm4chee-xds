var appCommon = angular.module('appCommon', ['mgcrea.ngStrap.popover']);

appCommon.controller('NavbarController', ['$scope','$http', function($scope, $http) {
    $scope.logout = function () {
        $http({method:"POST", url:"data/logout"}).success(function(response, status) {
            window.location.reload();
        });
    };
}]);

appCommon.factory('appLoadingIndicator', function() {

	console.log("AppLoading factory!");

	var loadingPoolSize = 0;

	var loading = {

		// call this when make an ajax request
		start : function() {
			loadingPoolSize++;
		},

		// call this when finished
		finish : function() {
			if (loadingPoolSize > 0)
                loadingPoolSize--;
		},

		// call this to show the user the status
		isLoading : function() {
			return loadingPoolSize > 0;
		}

	};

	return loading;
});

appCommon.factory('appHttp', function(appNotifications, appLoadingIndicator, $http) {

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

    var makeMethod = function(method) {
        return function(url, data, callbackSuccess, callbackError, notification) {
            appLoadingIndicator.start();
            $http({method: method, data: data, url: url}).success(function (data, status) {
                onSuccess(data, status, callbackSuccess, notification);
            }).error(function (data, status) {
                onError(data, status, callbackError, notification);
            });
        };
    }

    appHttp.post = makeMethod("POST");
    appHttp.get =  makeMethod("GET");

	return appHttp;

});

appCommon.factory('appNotifications', [ 'xdsConstants', '$timeout', function(xdsConstants, $timeout) {

	var messageShowTimeout = 5000;
	
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
		link:function(scope, element, atttributes) {
			// bind popover to the element
            scope.appNotifications = appNotifications;
			var myPopover = $popover(element, {trigger: 'manual', placement:'bottom' ,template:'dcm4che-web-common/notifications.html', animation: "am-flip-x"});

			scope.$watch("appNotifications.notifications.length",function() {
				if (appNotifications.notifications.length > 0)
					myPopover.$promise.then(myPopover.show); else 
						myPopover.$promise.then(myPopover.hide);
						
			});
		}
	};
}]);

appCommon.controller('appNotificationsPopoverController', function($scope, appNotifications) {

    $scope.appNotifications = appNotifications;

});


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

/* taken from http://stackoverflow.com/questions/14430655/recursion-in-angular-directives
* to enable recursive object trees output*/
appCommon.factory('RecursionHelper', ['$compile', function($compile){
    return {
        /**
         * Manually compiles the element, fixing the recursion loop.
         * @param element
         * @param [link] A post-link function, or an object with function(s) registered via pre and post properties.
         * @returns An object containing the linking functions.
         */
        compile: function(element, link){
            // Normalize the link parameter
            if(angular.isFunction(link)){
                link = { post: link };
            }

            // Break the recursion loop by removing the contents
            var contents = element.contents().remove();
            var compiledContents;
            return {
                pre: (link && link.pre) ? link.pre : null,
                /**
                 * Compiles and re-adds the contents
                 */
                post: function(scope, element){
                    // Compile the contents
                    if(!compiledContents){
                        compiledContents = $compile(contents);
                    }
                    // Re-add the compiled contents to the element
                    compiledContents(scope, function(clone){
                        element.append(clone);
                    });

                    // Call the post-linking function, if any
                    if(link && link.post){
                        link.post.apply(null, arguments);
                    }
                }
            };
        }
    };
}]);
