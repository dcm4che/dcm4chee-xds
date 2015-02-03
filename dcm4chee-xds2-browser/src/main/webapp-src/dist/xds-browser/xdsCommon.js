var xdsCommon = angular.module('dcm4che.xds.common', []);

// create controllers module, controllers added later
angular.module('dcm4che.xds.controllers',[]);


/**
 * helper methods to deal with ebXml objects 
 */
xdsCommon.factory('xdsEb', [ 'xdsConstants', function(xdsConstants) {

	return {
		extIdValueByIdentScheme : function(obj, scheme) {
			try {
				return _.findWhere(obj.externalIdentifier, {
					identificationScheme : xdsConstants[scheme]
				}).value;
			} catch (e) {
				console.log(e);
				return null;
			}
		},

		slotValueByName : function(obj, name) {
			try {
				return _.findWhere(obj.slot, {
					name : name
				}).valueList.value[0];
			} catch (e) {
				console.log(e);
				return null;
			}
		},

	};

} ]);


xdsCommon.filter('xdsExcludeSlotClassExtId', [ function() {
	return function(input, param) {
		return _.omit(input, "slot", "classification", "externalIdentifier");
	};
} ]);

/**
 * helper to determine the type of identifiable: XDSDocumentEntry, XDSFolder,
 * XDSSubmissionSet, XDSAssociation, or Other.
 */
xdsCommon.factory('xdsGetEntityType', [ 'xdsConstants', function(xdsConstants) {
	var xdsGetEntityType = function(entity) {

        try {

            // if association
            if (entity.value && entity.value.objectType == xdsConstants["XDS_Association"])
                return "XDSAssociation";

			var res = _.intersection(
			// get all extid names
			_.map(entity.value.externalIdentifier, function(extid) {
				return extid.name.localizedString[0].value;
			}),

			// filer only these
			[ "XDSDocumentEntry.uniqueId", "XDSFolder.uniqueId", "XDSSubmissionSet.uniqueId" ]);

			// just reuse the catch below
			if (res[0] == undefined)
				throw 0;

			// there must be only one, so return first without
			// .uniqueId postfix
			return res[0].split(".")[0];
		} catch (e) {
			return "Other";
		}
	};

	return xdsGetEntityType;
} ]);

/**
 * shortened id in a badge
 */
xdsCommon.directive('xdsId', function() {
	return {
		require : '^ngModel',
		scope : {
			ngModel : '='
		},
		template : "<small><span class=\"label label-default\">...{{shorten(ngModel)}}</span></small>",
		link : function(scope) {
			scope.shorten = function(ident) {
				try {
                    return ident.value.id.substr(ident.value.id.length - 5);
                } catch (e) {
                    return "-";
                }
			};
		}

	};

});

/**
 * nice representation of an identifiable with corresponding icon and shortened
 * id
 */
xdsCommon.directive('xdsIdentifiable', [
		'getIconClassForType',
		'xdsGetEntityType',
		function(getIconClassForType, xdsGetEntityType) {
			return {
				require : '^ngModel',
				scope : {
					ngModel : '='
				},
				template : '<span ng-class="getIconClassForType(xdsGetEntityType(ngModel))"></span> ' + '<span xds-id ng-model="ngModel" class="xds-id-badge"></span>'
						+ '{{ngModel.value.name.localizedString[0].value}}',
				link : function(scope) {
					scope.getIconClassForType = getIconClassForType;
					scope.xdsGetEntityType = xdsGetEntityType;
				}

			};

		} ]);
