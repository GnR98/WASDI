'use strict';
/**
 * Module tp handle Geoserver related calls 
 */
angular.module('wasdi.GeoserverSerice', ['wasdi.GeoserverSerice']).
service('GeoserverSerice', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    /**
     * Get all the styles available on the current Geoserver instance
     * @returns A list of string that identifies the availables styles
     */
    this.getStyles = function()
    {
        return this.m_oHttp.get(this.APIURL + "/product/styles");
    };


}]);