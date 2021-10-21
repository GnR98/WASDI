/**
 * Created by p.campanella on 18/11/2016.
 */

'use strict';
angular.module('wasdi.ProductService', ['wasdi.ProductService']).
    service('ProductService', ['$http', 'ConstantsService', function ($http, oConstantsService) {
        this.APIURL = oConstantsService.getAPIURL();
        this.m_oConstantsService = oConstantsService;
        this.m_oHttp = $http;

        this.getProductByName = function (sProductName) {
            return this.m_oHttp.get(this.APIURL + '/product/byname?name=' + sProductName + '&workspace=' + oConstantsService.getActiveWorkspace().workspaceId);
        };

        this.getProductListByWorkspace = function (sWorkspaceId) {
            return this.m_oHttp.get(this.APIURL + '/product/byws?workspace=' + sWorkspaceId);
        };

        this.getProductLightListByWorkspace = function (sWorkspaceId) {
            return this.m_oHttp.get(this.APIURL + '/product/bywslight?workspace=' + sWorkspaceId);
        };

        this.addProductToWorkspace = function (sProductName, sWorkspaceId) {
            return this.m_oHttp.get(this.APIURL + '/product/addtows?name=' + sProductName + '&workspace=' + sWorkspaceId);
        };

        this.deleteProductFromWorkspace = function (sProductName, sWorkspaceId, bDeleteFile, bDeleteLayer) {

            var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
            var sUrl = this.APIURL;
            if (oWorkspace.apiUrl != null) sUrl = oWorkspace.apiUrl;

            return this.m_oHttp.get(sUrl + '/product/delete?name=' + sProductName + '&workspace=' + sWorkspaceId + '&deletefile=' + bDeleteFile + '&deletelayer=' + bDeleteLayer);
        };

        this.deleteProductListFromWorkspace = function (sProductNameList, sWorkspaceId, bDeleteFile, bDeleteLayer) {
            if (sProductNameList.length == 0) {
                return 400; // bad parameters
            }
            var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
            var sUrl = this.APIURL;
            if (oWorkspace.apiUrl != null) sUrl = oWorkspace.apiUrl;
            // the list is passed in the body request
            return this.m_oHttp.post(sUrl + '/product/deletelist?workspace=' + sWorkspaceId + '&deletefile=' + bDeleteFile + '&bdeletelayer=' + bDeleteLayer, sProductNameList);
        };

        this.updateProduct = function (oProductViewModel, workspaceId) {
            return this.m_oHttp.post(this.APIURL + '/product/update?workspace=' + workspaceId, oProductViewModel);
        };

        this.getProductMetadata = function (sProductName, sWorkspace) {

            var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
            var sUrl = this.APIURL;
            if (oWorkspace.apiUrl != null) sUrl = oWorkspace.apiUrl;

            return sUrl + "/product/metadatabyname?name=" + sProductName + "&workspace=" + sWorkspace;
        };



        this.uploadFile = function (sWorkspaceInput, oBody, sName) {

            var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
            var sUrl = this.APIURL;
            if (oWorkspace.apiUrl != null) sUrl = oWorkspace.apiUrl;

            var oOptions = {
                transformRequest: angular.identity,
                // headers: {'Content-Type': 'multipart/form-data'}
                headers: { 'Content-Type': undefined }
            };
            return this.m_oHttp.post(sUrl + '/product/uploadfile?workspace=' + sWorkspaceInput + '&name=' + sName, oBody, oOptions);
        };
    }]);
