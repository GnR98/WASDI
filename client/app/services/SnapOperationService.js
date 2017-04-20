/**
 * Created by s.adamo on 09/02/2017.
 */


'use strict';
angular.module('wasdi.SnapOperationService', ['wasdi.SnapOperationService']).
service('SnapOperationService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    /************************************ RADAR OPERATIONS ************************************/
    this.ApplyOrbit = function (sSourceProductName, sDestinationProductName, sWorkspaceId,oOptionsInput) {

        return this.Operation("radar/applyOrbit", sSourceProductName, sDestinationProductName, sWorkspaceId, oOptionsInput);//orbit
    };
    this.Calibrate = function (sSourceProductName, sDestinationProductName, sWorkspaceId,oOptionsInput) {

        return this.Operation("radar/radiometricCalibration", sSourceProductName, sDestinationProductName, sWorkspaceId,oOptionsInput);//calibrate
    };
    this.Multilooking = function (sSourceProductName, sDestinationProductName, sWorkspaceId,oOptionsInput) {

        return this.Operation("radar/multilooking", sSourceProductName, sDestinationProductName, sWorkspaceId,oOptionsInput);//multilooking
    };

    /************************************ GEOMETRIC OPERATIONS ************************************/
    this.TerrainCorrection = function (sSourceProductName, sDestinationProductName, sWorkspaceId) {

        return this.Operation("geometric/rangeDopplerTerrainCorrection", sSourceProductName, sDestinationProductName, sWorkspaceId);//terrain
    };

    /************************************ NDVI OPERATIONS ************************************/
    this.NDVI = function (sSourceProductName, sDestinationProductName, sWorkspaceId,oOptionsInput) {

        return this.Operation("optical/ndvi", sSourceProductName, sDestinationProductName, sWorkspaceId,oOptionsInput);//ndvi
    };

    this.Operation = function(sOperation, sSourceProductName, sDestinationProductName, sWorkspaceId, oOptionsInput)
    {
        // //'/snap/
        var sUrl = this.APIURL + '/processing/{sOperation}?sSourceProductName=' + sSourceProductName + '&sDestinationProductName=' + sDestinationProductName + '&sWorkspaceId=' + sWorkspaceId;
        //sUrl = sUrl.replace("{sOperation}", sOperation);
        // return this.m_oHttp.get(sUrl);
        var oConfig = {header:""};
        /*var oData = {
            settings:oOptionsInput,
            sourceProductName:sSourceProductName,
            destinationProductName:sDestinationProductName,
            workspaceId:sWorkspaceId,
            exchange: sWorkspaceId,
            userId: oConstantsService.getUser().userId,
        }*/
        //var sUrl = this.APIURL + '/processing/{sOperation}';
        sUrl = sUrl.replace("{sOperation}", sOperation);
        return this.m_oHttp.post(sUrl,oOptionsInput,oConfig);
    }

}]);

