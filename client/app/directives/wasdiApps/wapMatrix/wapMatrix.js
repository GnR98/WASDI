angular.module('wasdi.wapMatrix', [])
    .directive('wapmatrix', function () {
        "use strict";
        return {
            restrict: 'E',
            scope: {},
            bindToController: {
                inputText: '=',
                tooltip:'=',
                

            },
            templateUrl:"directives/wasdiApps/wapMatrix/wapMatrix.html",
            controller: function() {
                this.rowLabel = ["1","2"];


            },
            controllerAs: '$ctrl'
        };
    });

