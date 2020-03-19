angular.module('wasdi.TableOfProductsDirective', [])
    .directive('tableofproductsdirective', function () {
        "use strict";
        return {
            restrict: 'E',
            scope: {},

            // * Text binding ('@' or '@?') *
            // * One-way binding ('<' or '<?') *
            // * Two-way binding ('=' or '=?') *
            // * Function binding ('&' or '&?') *
            bindToController: {
                productsList: '=',
                heightTable: '=',
                parentController: '=',
                loadingData: '='
                // deleted: '&'
            },

            template: `
        <div class="table-of-products-directive"  ng-style="$ctrl.oTableStyle">

<!--            &lt;!&ndash; TODO HIDE BUTTON IF PARENT CONTROLLER IS NULL&ndash;&gt;-->
<!--            <button class="btn btn-primary btn-wasdi search-button"  ng-click="$ctrl.parentController.loadMore()">-->
<!--                Load more-->
<!--            </button>-->

            <table class="table table-hover layers "  >
                <tbody ng-repeat="layer in $ctrl.productsList ">
                    <!------------------------------- SUMMARY ------------------------------->
                    <tr class="info-layer" >

                        <!-- IMAGE CELL -->
                        <td class="image-cell">
                            <span class="label label-success">{{layer.summary.Mode}}</span>&nbsp;<!--Mode-->
                            <span class="label label-success"> {{layer.summary.Instrument}}</span><!--Instrument-->
                            <div class="preview-layer-image" ><img src={{layer.preview}} alt="Image" style="height: 82px;width: 82px; "></div>
                            <span class="badge">{{layer.summary.Size}}</span><!--Size-->
                        </td>

                        <!-- INFO CELL -->
                        <td class="info-cell">

                            <div>{{layer.summary.Date|date:'medium'}}</div><!--Date-->
                            </br>

                            <div>
                                <span>Name: </span>
                                <span><b>{{layer.title}}</b></span>
                            </div>

                            <div>
                                <span>Polarisation: </span>
                                <span><b>{{layer.summary.Mode}}</b></span>
                            </div>

                            <div>
                                <span>Relative Orbit: </span>
                                <span><b>{{layer.properties.relativeorbitnumber}}</b></span>
                            </div>

                            <!--<div>{{"layer"+layer.id}}</div>-->
                            <div>
                                <span>Provider:</span>
                                <span><b>{{layer.provider}}</b></span>
                            </div>

                            <div>
                                <span>Platform name:</span>
                                <span><b>{{layer.properties.platformname}}</b></span>
                            </div>
                            <div>
                                <span>Sensor Operational mode:</span>
                                <span><b>{{layer.properties.sensoroperationalmode}}</b></span>
                            </div>
                        </td>

                    </tr>

                </tbody>
                    <tr>
                        <td colspan="2">
                            <div ng-show = "true" class="loadIcon" ng-show = "$ctrl.loadingData">
                                <invader></invader>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2" class="cell-centered">
                            <!--TODO END IT -->
                            <button class="btn btn-default btn-wasdi" ng-click="$ctrl.parentController.loadMore()">
                                <i class="fa fa-refresh mr-2" aria-hidden="true"></i>Load more...
                            </button>

                        </td>

                    </tr>
            </table>
        </div>

         `,
            controller: function() {
                this.oTableStyle = { height: this.heightTable + 'px' };

            },
            controllerAs: '$ctrl'
        };
    });


