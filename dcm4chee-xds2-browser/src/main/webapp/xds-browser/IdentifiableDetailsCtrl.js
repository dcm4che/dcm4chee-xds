angular.module('dcm4che.xds.controllers').
controller('IdentifiableDetailsCtrl',
		[ '$scope', '$http', 'xdsConstants', 'xdsAdhocQuery', 'xdsGetEntityType', 'xdsEb','xdsConfig','browserLinkedViewXdsService', 'xdsDeleteObjectsQuery',
        function($scope, $http, xdsConstants, xdsAdhocQuery, xdsGetEntityType,xdsEb, xdsConfig, browserLinkedViewXdsService, xdsDeleteObjectsQuery) {

	// gotta watch if it changes
	$scope.$watch('currentIdentifiable', function() {

		if ($scope.currentIdentifiable == null)
			return;

		if ($scope.currentIdentifiable.__extraLists == null)
			$scope.currentIdentifiable.__extraLists = {};

		// not interesting
		if ($scope.currentIdentifiable.value == null)
			return;
		
		
		// get type
		$scope.type = xdsGetEntityType($scope.currentIdentifiable);
		
		// for doc entries
		if ($scope.type == "XDSDocumentEntry") {


			// find the unique id and repo id
			$scope.docUniqueId = xdsEb.extIdValueByIdentScheme($scope.currentIdentifiable.value, "UUID_XDSDocumentEntry_uniqueId");
			$scope.docRepoId = xdsEb.slotValueByName($scope.currentIdentifiable.value, "repositoryUniqueId");
			
			// check if the server is configured for this repo
			$scope.showDocumentDownload = !!_.contains(xdsConfig.xdsAccessibleRepos(), $scope.docRepoId);
				
		}

		var extra = {
			XDSDocumentEntry : {
				"Folders that contain this document" : {
					qid : xdsConstants["XDS_GetFoldersForDocument"],
					queryParams : {
						$XDSDocumentEntryEntryUUID : $scope.currentIdentifiable.value.id
					}
				},
				"Submission sets that contain this document" : {
					qid : xdsConstants["XDS_GetSubmissionSets"],
					queryParams : {
						$uuid : $scope.currentIdentifiable.value.id
					}
				}

			},

			XDSFolder : {
				"Documents in this folder" : {
					qid : xdsConstants["XDS_GetFolderAndContents"],
					queryParams : {
						$XDSFolderEntryUUID : $scope.currentIdentifiable.value.id
					}
				},
				"Submission sets that contain this folder" : {
					qid : xdsConstants["XDS_GetSubmissionSets"],
					queryParams : {
						$uuid : $scope.currentIdentifiable.value.id
					}
				}

			},

			XDSSubmissionSet : {
				"Contents of this submission set" : {
					qid : xdsConstants["XDS_GetSubmissionSetAndContents"],
					queryParams : {
						$XDSSubmissionSetEntryUUID : $scope.currentIdentifiable.value.id
					}
				}

			}

		};

		// fill in the missing extra
		// lists
		if (extra[$scope.type] != null)
			_.map(extra[$scope.type], function(query, label) {

				if ($scope.currentIdentifiable.__extraLists[label] == undefined) {
					xdsAdhocQuery.invoke(query.qid, query.queryParams, function(data) {
						$scope.currentIdentifiable.__extraLists[label] = data.registryObjectList;

						// remove the current identifiable from the list (lot of
						// queries include it)
						data.registryObjectList.identifiable = _.filter(data.registryObjectList.identifiable, function(val) {
							return val.value.id != $scope.currentIdentifiable.value.id;
						});

						$scope.currentIdentifiable.__extraLists[label].__title = label;
						$scope.currentIdentifiable.__extraLists[label].__parentIdentifiable = $scope.currentIdentifiable;
					});
				}
			});


        $scope.deleteSubmissionSet = function() {

            // get the contents of submission set from extra list
            var listOfIdentifiables = $scope.currentIdentifiable.__extraLists['Contents of this submission set'].identifiable;

            // include the submission set itself
            listOfIdentifiables.push($scope.currentIdentifiable);

            // extract uuids
            var uuids = _.chain(listOfIdentifiables).pluck('value').pluck('id').value();

            // send REST
            xdsDeleteObjectsQuery.invoke(uuids, function(succeeded){

                if (succeeded) {

                    // remove'em from ui
                    browserLinkedViewXdsService.removeOccurencesOfIdentifiablesWithUuids(uuids);

                    // cleanup the current view
                    $scope.wipePane();
                };

            });

        }

	});
} ]);
