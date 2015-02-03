var browserLinkedView= angular.module('dcm4che.browserLinkedView', []);

browserLinkedView.factory('browserLinkedViewPath',[function() {
	return [ null ];
}]);

browserLinkedView.factory('browserLinkedViewXdsService',['browserLinkedViewPath',function(path) {
	return {
		removeOccurencesOfIdentifiablesWithUuids: function(uuids, scope) {
			_.each(path, function(pathItem, index) {
				try {
					// nullify the path item if the identifiable with this uuid was deleted
					if ( _.contains(uuids, pathItem.value.id) )
						path[index].value = null;
				} catch (e) {
					// noop, it was not an identifiable
				}

				try {
					// kill the deleted elements from lists
					_.each(pathItem.identifiable, function(ident, index) {
						if ( _.contains(uuids, ident.value.id) )
                            pathItem.identifiable.splice(index, 1);
					});
				} catch (e) {
					// noop, it was not an identifiable list
				}
			});
		}
	};
}]);


browserLinkedView
		.controller(
				'RouteCtrl', function($scope, $http, $route, $location, xdsConstants, browserLinkedViewPath) {

							// how many
							$scope.panes = 2;

							$scope.routeparams = $route.current.params;

							// if we came from somewhere else using this url,
							// just go home
							if (parseInt($scope.routeparams.stepNum)
									+ $scope.panes - 1 > browserLinkedViewPath.length) {

								$location.path('/');
								return;
							}

							// override parent's function
							$scope.delegates.showList = function(ilist) {
								// trick the linkedview like its a regular
								// routine
								$scope.doExplore(-1, ilist);
							};

							// kill panes and path for the current view
							$scope.delegates.clean = function() {

								for (var pane = 0; pane < $scope.panes; pane++) {
									$scope.column[pane] = undefined;
                                    browserLinkedViewPath[parseInt($scope.routeparams.stepNum)+pane] = undefined;
								}

							};


							// init columns
							$scope.column = [];

							// for back button support. If this controller is recreated, put what we have in path for this stepNum into panes
							for (var pane = 0; pane < $scope.panes; pane++)
								$scope.column[pane] = browserLinkedViewPath[parseInt($scope.routeparams.stepNum)
										+ pane];

							$scope.doExplore = function(pane, obj) {

								// make sure
								pane = parseInt(pane);

								// put the obj to explore into next step of the path
                                browserLinkedViewPath[parseInt($scope.routeparams.stepNum)
										+ pane + 1] = obj;

								// if no scrolling needed
								if (pane < $scope.panes - 1) {
									$scope.column[pane + 1] = obj;

									// clean the rest of panes and path entries
									// if any
									for (var i = pane + 2; i < $scope.panes; i++) {
										$scope.column[i] = undefined;
                                        browserLinkedViewPath[parseInt($scope.routeparams.stepNum)
												+ i] = undefined;
									}
								} else {
									// if pane > panes then pan right
									// change route, move step +1
									$location
											.path('/step/'
													+ (parseInt($scope.routeparams.stepNum) + 1));
								}

							};
						});

browserLinkedView
		.directive(
				'browserLinkedView',
				[
						'xdsGetEntityType',
						'getIconClassForType',
						'$location',
						'$route',
						function(xdsGetEntityType, getIconClassForType, $location, $route) {
							return {
								scope : {
									'browserSubject' : '=',
									'browserExploreFurther' : '&',
									'browserPane' : '@',
									'chosenToExplore' : '=browserChosen'
								},
								link : function(scope) {

									scope
											.$watch(
													'browserSubject',
													function() {

														scope.currentIdentifiable = scope.browserSubject;
														scope.currentIdentifiableList = scope.browserSubject;

                                                        try {
                                                            if (angular
                                                                .isArray(scope.browserSubject.identifiable))
                                                                scope.template = 'xds-browser/identifiable-list.html';
                                                            else if (scope.browserSubject.value.id != null)
                                                                scope.template = 'xds-browser/identifiable-details.html'; else
                                                            throw "No template found";
                                                        } catch (e) {
                                                            scope.template = "";
                                                        }
													});

									// scope.chosenToExplore = undefined;

									scope.explore = function(obj) {

										// remember for use in this template
										// scope.chosenToExplore = obj;

										// call parent's explore for this view
										scope.browserExploreFurther({
											pane : scope.browserPane,
											obj : obj
										});

									};

                                    scope.wipePane = function() {
                                      scope.browserSubject = null;
                                    };

									scope.xdsGetEntityType = xdsGetEntityType;
									scope.getIconClassForType = getIconClassForType;

								},
								template : "<div ng-include = \"template\"></div>"
							};
						} ]);