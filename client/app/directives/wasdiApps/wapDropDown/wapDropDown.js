angular.module('wasdi.wapDropDown', [])
    .directive('wapdropdown', function () {
        "use strict";
        return{
            restrict : 'EAC',
            templateUrl:"directives/wasdiApps/wapDropDown/wapDropDown.html",
            // * Text binding ('@' or '@?') *
            // * One-way binding ('<' or '<?') *
            // * Two-way binding ('=' or '=?') *
            // * Function binding ('&' or '&?') *
            scope :{
                onClickFunction:"&",
                selectedValue:"=",
                listOfValues:"=",
                enableSearchFilter:"=",
                dropdownName:"="
            },
            link: function(scope, elem, attrs)
            {
                if(  typeof scope.enableSearchOption !== "boolean")
                {
                    scope.enableSearchOption = false;
                }
                if( utilsIsObjectNullOrUndefined(scope.dropdownName) === true)
                {
                    scope.dropdownName = "";
                }
                scope.isSelectedValue = false;
                scope.selectedValue = {
                    name:"",
                    id:""
                };

                scope.onClickValue = function(oSelectedValue){
                    scope.isSelectedValue = true;
                    scope.selectedValue = oSelectedValue;
                }

                scope.setDefaultSelectedValue = function(){
                    scope.selectedValue = {
                        name:"",
                        id:""
                    };
                }

                scope.setDefaultSelectedValue();
            }
        };
    });
