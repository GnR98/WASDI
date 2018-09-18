

'use strict';
angular.module('wasdi.AuthServiceGoogle', []).
service('AuthServiceGoogle', ['$http','ConstantsService', function ($http,oConstantsService) {
    this.m_oUser = {};

    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.loginGoogleUser = function(oIdToken) {
        return this.m_oHttp.post(this.APIURL + '/auth/logingoogleuser',oIdToken);
    }
}]);