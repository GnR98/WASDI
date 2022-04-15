angular.module('wasdi.wapMatrix', [])
    .directive('wapmatrix', function () {
        "use strict";
        return {
            restrict: 'E',
            scope: {},
            bindToController: {
                inputText: '=',
                tooltip:'='
            },
            template: `<input type="number" class="form-control"  ng-model="$ctrl.inputText" uib-tooltip="{{$ctrl.tooltip}}" tooltip-placement="right">`,
            controller: function() {

            },
            controllerAs: '$ctrl'
        };
    });

