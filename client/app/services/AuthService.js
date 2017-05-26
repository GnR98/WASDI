/**
 * Created by p.campanella on 21/10/2016.
 */

'use strict';
angular.module('wasdi.AuthService', ['wasdi.ConstantsService']).
service('AuthService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.login = function(oCredentials) {
        return this.m_oHttp.post(this.APIURL + '/auth/login',oCredentials);
        //return this.m_oHttp.post('http://localhost:8080/wasdiwebserver/rest//auth/login',oCredentials);
    }

    this.logout = function() {
        //CLEAN COOKIE
        return this.m_oHttp.get(this.APIURL + '/auth/logout');
    }

    this.checkSession = function(sSession)
    {
        return this.m_oHttp.get(this.APIURL + '/auth/checksession');
    }

    /* USER UPLOAD AUTH API */
    this.createAccountUpload = function(sEmailInput)
    {
        if(utilsIsEmail(sEmailInput) === false)
            return null;

        return this.m_oHttp.post(this.APIURL + '/auth/upload/createaccount',sEmailInput );
    };

    this.deleteAccountUpload = function(sIdInput)
    {
        if(utilsIsObjectNullOrUndefined(sIdInput))
            return null;

        return this.m_oHttp.delete(this.APIURL + '/auth/upload/removeaccount',sIdInput);
    };

    this.updatePasswordUpload = function(sEmailInput)
    {
        if(utilsIsEmail(sEmailInput) === false)
            return null;

        return this.m_oHttp.post(this.APIURL + '/auth/upload/updatepassword',sEmailInput);
    };

    this.isCreatedAccountUpload = function()
    {
        return this.m_oHttp.get(this.APIURL + '/auth/upload/existsaccount');
    };

    this.getListFilesUpload = function()
    {
        return this.m_oHttp.get(this.APIURL + '/auth/upload/list');
    };

}]);