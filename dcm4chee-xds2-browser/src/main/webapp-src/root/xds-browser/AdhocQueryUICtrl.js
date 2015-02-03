angular.module('dcm4che.xds.controllers').controller('AdhocQueryUICtrl',
    function ($scope, $http, xdsConstants, xdsAdhocQuery, xdsConfig) {

        // services
        $scope.xdsConstants = xdsConstants;

        // 3 different query options
        $scope.entities = {
            "Document entries": "XDS_FindDocuments",
            "Folders": "XDS_FindFolders",
            "Submission sets": "XDS_FindSubmissionSets"
        };

        // is query str is a patient id?
        $scope.isSearchPid = function () {
            return $scope.searchStr.search(/.*&ISO$/) > -1;
        };

        // is query str is a unique id?
        $scope.isSearchUniqueId = function () {
            return $scope.searchStr.search(/^[0-9\.]*$/) > -1;
        };

        // is query str is a uuid?
        $scope.isSearchUUID = function () {
            return $scope.searchStr.search(/^urn:uuid:.*/) > -1;
        };

        $scope.search = function () {

            // we need to blur from search field, otherwise it is confusing as the
            // typeahead is not popping up until re-focused
            //angular.element("#searchButton").focus();

            // search depending on type of input

            // if uuid/uniqueid
            if ($scope.isSearchUUID() || $scope.isSearchUniqueId())
                $scope.searchUUIDUniqueId();

            // if patient id
            if ($scope.isSearchPid()) {
                $scope.browseEntity("XDS_FindDocuments");
            }
            ;


        };

        // when search str is changed, clean up the browser panes,
        // and reset the current entity, so the button is enabled
        $scope.searchChanged = function () {
            $scope.currentEntity = "";
            $scope.delegates.clean();
        };

        /*
         * $scope.$watch('currentStatus', function() { browseEntity(currentEntity);
         * });
         */

        $scope.searchUUIDUniqueId = function () {

            var searchWhat = ($scope.isSearchUUID() ? 'uuid' : 'uniqueId' );

            var queries = {
                uuid: [
                    {
                        qid: xdsConstants.XDS_GetDocuments,
                        params: {
                            '$XDSDocumentEntryEntryUUID': $scope.searchStr
                        }
                    },
                    {
                        qid: xdsConstants.XDS_GetFolders,
                        params: {
                            '$XDSFolderEntryUUID': $scope.searchStr
                        }
                    },
                    {
                        qid: xdsConstants.XDS_GetSubmissionSetAndContents,
                        params: {
                            '$XDSSubmissionSetEntryUUID': $scope.searchStr
                        }
                    },
                    {
                        qid: xdsConstants.XDS_GetAssociations,
                        params: {
                            '$uuid': $scope.searchStr
                        }
                    }
                ],

                uniqueId: [
                    {
                        qid: xdsConstants.XDS_GetDocuments,
                        params: {
                            '$XDSDocumentEntryUniqueId': $scope.searchStr
                        }
                    },
                    {
                        qid: xdsConstants.XDS_GetFolders,
                        params: {
                            '$XDSFolderUniqueId': $scope.searchStr
                        }
                    },
                    {
                        qid: xdsConstants.XDS_GetSubmissionSetAndContents,
                        params: {
                            '$XDSSubmissionSetUniqueId': $scope.searchStr
                        }
                    }
                ]
            };

            // put all the found entities together here.. although should be only
            // one..
            var list = {
                identifiable: [],
                __title: "Identifiable(s) for " + searchWhat + " = " + $scope.searchStr
            };

            // save in the list object the id of what we are searching, so the list view can highlight it
            list[(searchWhat == 'uuid' ? "__forUUID" : "__forUniqueId")] = $scope.searchStr;

            // show it right away, will be populated as responses arrive
            $scope.delegates.showList(list);

            // call all queries and merge results into list
            _.map(queries[searchWhat], function (query) {
                xdsAdhocQuery.invoke(query.qid, query.params, function (data) {
                    try {
                        list.identifiable = _.union(list.identifiable, data.registryObjectList.identifiable);
                    } catch (e) {
                        // just no entries found
                    }
                });
            });
        };

        // when user changes the entity/makes search
        $scope.browseEntity = function (name) {

            $scope.currentEntity = name;
            var qid = xdsConstants.B_FindEntities[name];
            var queryParams;

            // make up the query params
            switch (name) {

                case "XDS_FindFolders":
                    queryParams = {
                        "$XDSFolderPatientId": $scope.searchStr,
                        "$XDSFolderStatus": $scope.currentStatus
                    };

                    break;

                case "XDS_FindSubmissionSets":
                    queryParams = {
                        "$XDSSubmissionSetPatientId": $scope.searchStr,
                        "$XDSSubmissionSetStatus": $scope.currentStatus
                    };
                    break;
                case "XDS_FindDocuments":
                    queryParams = {
                        "$XDSDocumentEntryPatientId": $scope.searchStr,
                        "$XDSDocumentEntryStatus": $scope.currentStatus
                    };
                    break;
            }
            ;

            xdsAdhocQuery.invoke(qid, queryParams, function (data, status) {

                $scope.responseStatus = status;

                $scope.delegates.showList(data.registryObjectList);

                // set label like "Document entries for john^^^&1.2.3.4.5&ISO" (used
                // in list title)
                data.registryObjectList.__title = _.invert($scope.entities)[$scope.currentEntity] + " for " + $scope.searchStr;
            });
        };

        // init
        // $scope.browseEntity("XDS_FindDocuments");

    });
