'use strict';
/**
 * Module to handle Style related calls 
 */
angular.module('wasdi.StyleService', ['wasdi.StyleService']).
service('StyleService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    /**
     * Get all the styles available on the main Geoserver instance
     * Retrieves the files available on the main server
     * @returns A list of string that identifies the availables styles
     */
    this.getStyles = function()
    {
        return this.m_oHttp.get(this.APIURL + "/style/getlist");
    };


}]);