angular.module('dcm4che.xds.controllers').
controller('IdentifiableListCtrl', [ '$scope', 'xdsEb',function($scope,xdsEb) {

	// gotta watch if it changes
	$scope.$watch('currentIdentifiableList.identifiable', function() {

        if ($scope.currentIdentifiableList == null) return;

		$scope.singleItems = $scope.currentIdentifiableList.identifiable;
		$scope.pairedItems = [];

		
		// if the list is related to something, check if we can pair
		// some stuff with associations to that something
		if ($scope.currentIdentifiableList.__parentIdentifiable != null) {
			$scope.pairedItems = _.map($scope.singleItems, function(v) {

				// if this is an association and its source is the
				// list's parent object
				if (v.value.targetObject != null && v.value.sourceObject == $scope.currentIdentifiableList.__parentIdentifiable.value.id) {

					// check if we have the target in this list
					var target = _.findWhere(_.map($scope.singleItems, function(v) {
						return {
							id : v.value.id,
							item : v
						};
					}),
					{
						id : v.value.targetObject
					});

					if (target != null)
						return {
							ass : v,
							target : target.item
						};
				}
				;

				return null;
			});

			// remove nulls
			$scope.pairedItems = _.compact($scope.pairedItems);

			// remove the paired items
			// from singlelist
			$scope.singleItems = _.difference($scope.singleItems, _.pluck($scope.pairedItems, "ass"), _.pluck($scope.pairedItems, "target"));

		};
		
		$scope.isThisSearched = function(i) {
			try {
                return i.value.id == $scope.currentIdentifiableList.__forUUID ||
                    (
                        $scope.currentIdentifiableList.__forUniqueId != null &&
                        (
                            xdsEb.extIdValueByIdentScheme(i.value, "UUID_XDSDocumentEntry_uniqueId") == $scope.currentIdentifiableList.__forUniqueId ||
                            xdsEb.extIdValueByIdentScheme(i.value, "UUID_XDSFolder_uniqueId") == $scope.currentIdentifiableList.__forUniqueId ||
                            xdsEb.extIdValueByIdentScheme(i.value, "UUID_XDSSubmissionSet_uniqueId") == $scope.currentIdentifiableList.__forUniqueId
                            )
                        );
            } catch (e) {
                return false;
            }
		};
		
	});
} ]);
