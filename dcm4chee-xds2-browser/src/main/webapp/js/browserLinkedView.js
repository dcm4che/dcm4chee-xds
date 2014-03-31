var browserLinkedView= angular.module('browserLinkedView', []);

browserLinkedView
		.controller(
				'RouteCtrl',
				[
						'$scope',
						'$http',
						'$route',
						'$location',
						'xdsConstants',
						function($scope, $http, $route, $location, xdsConstants) {

							// how many
							$scope.panes = 2;

							$scope.routeparams = $route.current.params;

							// if we came from somewhere else using this url,
							// just go home
							if (parseInt($scope.routeparams.stepNum)
									+ $scope.panes - 1 > $scope.path.length) {

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
									$scope.path[parseInt($scope.routeparams.stepNum)+pane] = undefined;
								}
								
							};
							
							
							// init columns
							$scope.column = [];

							// for back button support. If this controller is recreated, put what we have in path for this stepNum into panes
							for (var pane = 0; pane < $scope.panes; pane++)
								$scope.column[pane] = $scope.path[parseInt($scope.routeparams.stepNum)
										+ pane];

							$scope.doExplore = function(pane, obj) {

								// make sure
								pane = parseInt(pane);

								// put the obj to explore into next step of the path
								$scope.path[parseInt($scope.routeparams.stepNum)
										+ pane + 1] = obj;

								// if no scrolling needed
								if (pane < $scope.panes - 1) {
									$scope.column[pane + 1] = obj;

									// clean the rest of panes and path entries
									// if any
									for (var i = pane + 2; i < $scope.panes; i++) {
										$scope.column[i] = undefined;
										$scope.path[parseInt($scope.routeparams.stepNum)
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
						} ]);

browserLinkedView
		.directive(
				'browserLinkedView',
				[
						'xdsGetEntityType',
						'getIconClassForType',
						function(xdsGetEntityType, getIconClassForType) {
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

														if (scope.browserSubject == null) {
															scope.template = "";
															return;
														}

														scope.currentIdentifiable = scope.browserSubject;
														scope.currentIdentifiableList = scope.browserSubject;

														if (angular
																.isArray(scope.browserSubject.identifiable))
															scope.template = 'templates/IdentifiableList.html';
														else
															scope.template = 'templates/IdentifiableDetails.html';

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

									scope.xdsGetEntityType = xdsGetEntityType;
									scope.getIconClassForType = getIconClassForType;
									
								},
								template : "<div ng-include = \"template\"></div>"
							};
						} ]);