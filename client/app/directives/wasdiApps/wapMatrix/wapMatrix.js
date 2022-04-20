angular.module('wasdi.wapMatrix', [])
    .directive('wapmatrix', function () {
        "use strict";
        return {
            restrict: 'E',
            scope: {},
            bindToController: {
                tooltip:'='
                
            },
            templateUrl:"directives/wasdiApps/wapMatrix/wapMatrix.html",
            controller: function() {
                this.rowLabels = ["A","B","C","D","E","F"];
                this.columnLabels = ["1","2"];
                // get number of rows and number of colums maybe 
                let nCol = this.columnLabels.length;
                let nRow = this.rowLabels.length;
                this.matrix = []; 
                for(var i=0; i<nCol; i++) {
                    this.matrix[i] = [];
                    for(var j=0; j<nRow; j++) {
                        this.matrix[i][j] = 0;
                    }
                }
                // test it should be 
                console.log(this.matrix);
            },
            controllerAs: '$ctrl'
        };
    });

