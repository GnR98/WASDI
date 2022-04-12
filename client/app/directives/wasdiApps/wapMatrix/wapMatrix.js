angular.module('wasdi.wapmartix', [])
    .directive('wapmartix', function () {
        "use strict";
        return {
            restrict: 'E',
            scope: {},
            bindToController: {
                inputText: '=',
                tooltip:'='
            },
            template: `<div ng-repeat="row in $ctrl.rowLabels">
             <input type="number"
             class="form-control"  ng-model="$ctrl.inputText" 
             uib-tooltip="{{$ctrl.tooltip}}" 
             tooltip-placement="right" value={{row}}>
             </div>`,
            controller: function() {
                this.rowLabels = ["first" , "second"];
                this.rowColumns = ["first" , "second"];
            },
            controllerAs: '$ctrl'
        };
    });

