/**
 * Created by p.campanella on 24/10/2016.
 */
var EditorController = (function () {
    function EditorController($scope, $location, $interval, oConstantsService, oAuthService, oMapService, oFileBufferService,
                              oProductService,$state,oWorkspaceService,oGlobeService,oProcessesLaunchedService, oRabbitStompService) {

        // Reference to the needed Services
        this.m_oScope = $scope;
        this.m_oLocation = $location;
        this.m_oInterval = $interval;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oMapService = oMapService;
        this.m_oFileBufferService = oFileBufferService;
        this.m_oProductService = oProductService;
        this.m_oScope.m_oController = this;
        this.m_oGlobeService=oGlobeService;
        this.m_oState=$state;
        this.m_oProcessesLaunchedService=oProcessesLaunchedService;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oRabbitStompServive = oRabbitStompService;
        this.m_b2DMapModeOn=true;
        this.m_b3DMapModeOn=false;

        //layer list
        this.m_aoLayersList=[];//only id
        //this.m_aoProcessesRunning=[];
        // Array of products to show
        this.m_aoProducts = [];

        /* ---------------------- SET COOKIE (m_aoProcessesRunning)-------------*/
        /* USE DELETE COOKIE FOR DEBUG*/
        //this.m_oProcessesLaunchedService.loadProcessesByCookie()
        //this.m_aoProcessesRunning =  this.m_oProcessesLaunchedService.getProcesses();

        //this.m_oConstantsService.deleteCookie("m_aoProcessesRunning");

        //if(!utilsIsObjectNullOrUndefined( this.m_aoProcessesRunning) && this.m_aoProcessesRunning.length == 0)
        //{
        //    var oResult = this.m_oConstantsService.getCookie("m_aoProcessesRunning");
        //    if(utilsIsObjectNullOrUndefined(oResult) || oResult.length == 0 )
        //        this.m_oConstantsService.setCookie("m_aoProcessesRunning", [], 1);
        //    else
        //    {
        //        this.m_aoProcessesRunning = oResult;
        //    }
        //}

        // Reconnection promise to stop the timer if the reconnection succeed or if the user change page
        //this.m_oReconnectTimerPromise = null;
        //this.m_oRabbitReconnect = null;

        // Web Socket to receive workspace messages
        //var oWebSocket = new WebSocket(this.m_oConstantsService.getStompUrl());
        //this.m_oClient = Stomp.over(oWebSocket);

        // Here a Workpsace is needed... if it is null create a new one..
        this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
        this.m_oUser = this.m_oConstantsService.getUser();

        //if there isn't workspace
        if(utilsIsObjectNullOrUndefined( this.m_oActiveWorkspace) && utilsIsStrNullOrEmpty( this.m_oActiveWorkspace))
        {
            //if this.m_oState.params.workSpace in empty null or undefined create new workspace
            if(!(utilsIsObjectNullOrUndefined(this.m_oState.params.workSpace) && utilsIsStrNullOrEmpty(this.m_oState.params.workSpace)))
            {
                this.openWorkspace(this.m_oState.params.workSpace);
                //this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
            }
            else
            {

                //TODO CREATE NEW WORKSPACE OR GO HOME
            }
        }
        else
        {
            this.m_oProcessesLaunchedService.loadProcessesFromServer(this.m_oActiveWorkspace.workspaceId);
            this.getProductListByWorkspace();
        }
        this.m_sDownloadFilePath = "";

        // Rabbit subscription
        //var m_oSubscription = {};


        // Self reference for callbacks
        var oController = this;

        //Set default value tree
        this.m_oTree = null;//IMPORTANT NOTE: there's a 'WATCH' for this.m_oTree in TREE DIRECTIVE

        /*Start Rabbit WebStomp*/
        this.m_oRabbitStompServive.initWebStomp(this.m_oActiveWorkspace,"EditorController",this);

        /**
         * Rabbit Callback: receives the Messages
         * @param message
         */
        //var oRabbitCallback = function (message) {
        //    // called when the client receives a STOMP message from the server
        //    if (message.body) {
        //        console.log("got message with body " + message.body)
        //
        //        // Get The Message View Model
        //        var oMessageResult = JSON.parse(message.body);
        //
        //        if (oMessageResult == null) return;
        //        if (oMessageResult.messageResult=="KO") {
        //            //TODO REMOVE ELEMENT IN PROCESS QUEUE
        //            alert('There was an error in the RabbitCallback');
        //            return;
        //        }
        //
        //        // Route the message
        //        if (oMessageResult.messageCode == "DOWNLOAD") {
        //            oController.receivedDownloadMessage(oMessageResult);
        //        }
        //        else if (oMessageResult.messageCode == "PUBLISH") {
        //            oController.receivedPublishMessage(oMessageResult);
        //        }
        //        else if (oMessageResult.messageCode == "PUBLISHBAND") {
        //
        //            oController.receivedPublishBandMessage(oMessageResult.payload.layerId);
        //        }
        //
        //    } else {
        //        console.log("got empty message");
        //    }
        //
        //    //oController.addTestLayer(message.body);
        //
        //}


        /**
         * Callback of the Rabbit On Connect
         */

        //var on_connect = function () {
        //    console.log('Web Stomp connected');
        //
        //    //CHECK IF sWorkSpaceId is null
        //    var sWorkSpaceId = null;
        //    if(utilsIsObjectNullOrUndefined(oController.m_oActiveWorkspace))
        //        sWorkSpaceId = oController.m_oState.params.workSpace;
        //    else
        //        sWorkSpaceId = oController.m_oActiveWorkspace.workspaceId;
        //
        //    oController.m_oSubscription = oController.m_oClient.subscribe(sWorkSpaceId, oRabbitCallback);
        //
        //    // Is this a re-connection?
        //    if (oController.m_oReconnectTimerPromise != null) {
        //        // Yes it is: clear the timer
        //        oController.m_oInterval.cancel(oController.m_oReconnectTimerPromise);
        //        oController.m_oReconnectTimerPromise = null;
        //    }
        //};


        /**
         * Callback for the Rabbit On Error
         */
        //var on_error = function (sMessage) {
        //    console.log('Web Stomp Error');
        //    if (sMessage == "LOST_CONNECTION") {
        //        console.log('LOST Connection');
        //
        //        if (oController.m_oReconnectTimerPromise == null) {
        //            // Try to Reconnect
        //            oController.m_oReconnectTimerPromise = oController.m_oInterval(oController.m_oRabbitReconnect, 5000);
        //        }
        //    }
        //};

        // Keep local reference to the callbacks to use it in the reconnection callback
        //this.m_oOn_Connect = on_connect;
        //this.m_oOn_Error = on_error;

        // Call back for rabbit reconnection
        //var rabbit_reconnect = function () {
        //
        //    console.log('Web Stomp Reconnection Attempt');
        //
        //    // Connect again
        //    oController.oWebSocket = new WebSocket(oController.m_oConstantsService.getStompUrl());
        //    oController.m_oClient = Stomp.over(oController.oWebSocket);
        //    oController.m_oClient.connect(oController.m_oConstantsService.getRabbitUser(), oController.m_oConstantsService.getRabbitPassword(), oController.m_oOn_Connect, oController.m_oOn_Error, '/');
        //};
        //this.m_oRabbitReconnect = rabbit_reconnect;

        //connect to the queue
        //this.m_oClient.connect(oController.m_oConstantsService.getRabbitUser(), oController.m_oConstantsService.getRabbitPassword(), on_connect, on_error, '/');
        //$scope.$watch('m_oController.m_oConstantsService.m_oActiveWorkspace', function (newValue, oldValue, scope) {
        //    $scope.m_oController.m_oActiveWorkspace = newValue;
        //    if (!utilsIsObjectNullOrUndefined( $scope.m_oController.m_oActiveWorkspace)) {
        //    }
        //});

        // Initialize the map

        oMapService.initMap('wasdiMap');
        this.m_oGlobeService.initGlobe('cesiumContainer2');

        // Clean Up when exit!!
        //this.m_oScope.$on('$destroy', function () {
        //    // Is this a re-connection?
        //    if (oController.m_oReconnectTimerPromise != null) {
        //        // Yes it is: clear the timer
        //        oController.m_oInterval.cancel(oController.m_oReconnectTimerPromise);
        //        oController.m_oReconnectTimerPromise = null;
        //    }
        //    else {
        //        if (oController.m_oClient != null) {
        //            oController.m_oClient.disconnect();
        //        }
        //    }
        //});

        /* ---------------- WATCH -----------------*/
        // Read Product List
        // watch when m_oController.m_oConstantsService.m_oActiveWorkspace change (usually after a load/reload of page)
        // reload tree
        //$scope.$watch('m_oController.m_oConstantsService.m_oActiveWorkspace', function (newValue, oldValue, scope)
        //{
        //    //$scope.m_oController.m_oActiveWorkspace = newValue;
        //
        //    if(!utilsIsObjectNullOrUndefined($scope.m_oController.m_oActiveWorkspace) )
        //    {
        //        $scope.m_oController.m_oProductService.getProductListByWorkspace($scope.m_oController.m_oActiveWorkspace.workspaceId).success(function (data, status) {
        //            if (data != null) {
        //                if (data != undefined) {
        //                    //push all products
        //                    for(var iIndex = 0; iIndex < data.length; iIndex++)
        //                    {
        //                        $scope.m_oController.m_aoProducts.push(data[iIndex]);
        //                    }
        //                    // i need to make the tree after the products are loaded
        //                    $scope.m_oController.m_oTree = oController.generateTree();
        //                    //oController.m_oScope.$apply();
        //                }
        //            }
        //        }).error(function (data, status) {
        //            utilsVexDialogAlertTop('Error reading product list');
        //            //console.log('Error reading product list');
        //        });
        //
        //
        //    }
        //
        //});

    }

    /********************METHODS********************/

    /**
     * Handler of the "download" message
     * @param oMessage Received Message
     */
    /* THIS FUNCTION ARE CALLED IN RABBIT SERVICE */
    EditorController.prototype.receivedDownloadMessage = function (oMessage) {
        if (oMessage == null) return;
        if (oMessage.messageResult=="KO") {
            //alert('There was an error in the download');
            utilsVexDialogAlertTop('There was an error in the download');
            return;
        }
        var oController = this;
        this.m_oProductService.addProductToWorkspace(oMessage.payload.fileName,this.m_oActiveWorkspace.workspaceId).success(function (data, status) {
            if(data == true )
            {
                //console.log('Product added to the ws');
                utilsVexDialogAlertBottomRightCorner('Product added to the ws');
                oController.getProductListByWorkspace();

                //oController.m_aoProducts.push(oMessage.payload);
                ////oController.getProductListByWorkspace();
                //oController.m_oTree = oController.generateTree();
                //oController.m_oProcessesLaunchedService.removeProcessByPropertySubstringVersion("processName",oMessage.payload.fileName,
                //    oController.m_oActiveWorkspace.workspaceId,oController.m_oUser.userId);

                oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);

                //oController.m_aoProcessesRunning =  this.m_oProcessesLaunchedService.getProcesses();
            }
            else
            {
                utilsVexDialogAlertTop("Error in add product to workspace");
            }


        }).error(function (data,status) {
            utilsVexDialogAlertTop('Error adding product to the ws')
            //console.log('Error adding product to the ws');
        });

        //this.m_oScope.$apply();
    }


    /**
     * Handler of the "publish" message
     * @param oMessage
     */
    /* THIS FUNCTION ARE CALLED IN RABBIT SERVICE */
    EditorController.prototype.receivedPublishMessage = function (oMessage) {
        if (oMessage == null) return;
        if (oMessage.messageResult=="KO") {
            //alert('There was an error in the publish');
            utilsVexDialogAlertTop('There was an error in the publish');
            return;
        }

    }


    /**
     * Handler of the "PUBLISHBAND" message
     * @param oMessage
     */
    /* THIS FUNCTION ARE CALLED IN RABBIT SERVICE */
    EditorController.prototype.receivedPublishBandMessage = function (oLayer) {

        if(utilsIsObjectNullOrUndefined(oLayer))
        {
            console.log("Error LayerID is empty...");
            return false;
        }

        //add layer in list
        this.m_aoLayersList.push(oLayer);
        this.addLayerMap2D(oLayer.layerId);
        this.addLayerMap3D(oLayer.layerId);
        this.zoomOnLayer3DGlobe(oLayer.layerId);
        this.zoomOnLayer2DMap(oLayer.layerId);
        //this.removeProcessInListOfRunningProcesses(oLayerId);
        //TODO REMOVE PROCESS IN LIST
        //this.m_oProcessesLaunchedService.removeProcessByPropertySubstringVersion("processName",oLayerId,
        //    this.m_oActiveWorkspace.workspaceId,this.m_oUser.userId);
        //this.m_aoProcessesRunning =  this.m_oProcessesLaunchedService.getProcessesByLocalStorage(
        //    this.m_oActiveWorkspace.workspaceId,this.m_oUser.userId);//TODO
        //this.m_oScope.$apply();
    }

    /**
     * Change location to path
     * @param sPath
     */
    EditorController.prototype.moveTo = function (sPath) {
        this.m_oLocation.path(sPath);
    }

    /**
     * Get the user name
     * @returns {*}
     */
    EditorController.prototype.getUserName = function () {
        var oUser = this.m_oConstantsService.getUser();

        if (oUser != null) {
            if (oUser != undefined) {
                var sName = oUser.name;
                if (sName == null) sName = "";
                if (sName == undefined) sName = "";

                if (oUser.surname != null) {
                    if (oUser.surname != undefined) {
                        sName += " " + oUser.surname;

                        return sName;
                    }
                }
            }
        }

        return "";
    }

    /**
     * Check if the user is logged or not
     */
    EditorController.prototype.isUserLogged = function () {
        return this.m_oConstantsService.isUserLogged();
    }

    /**
     * Add test layer
     * @param sLayerId
     */
    EditorController.prototype.addLayerMap2D = function (sLayerId) {
        //
        var oMap = this.m_oMapService.getMap();
        var sUrl=this.m_oConstantsService.getWmsUrlGeoserver();//'http://localhost:8080/geoserver/ows?'

        var wmsLayer = L.tileLayer.wms(sUrl, {
            layers: 'wasdi:' + sLayerId,
            format: 'image/png',
            transparent: true,
            noWrap:true
        });
        wmsLayer.addTo(oMap);

    }

    ///**
    // * Add layer for Cesium Globe
    // * @param sLayerId
    // */
    EditorController.prototype.addLayerMap3D = function (sLayerId) {
        var oGlobeLayers=this.m_oGlobeService.getGlobeLayers();
        var sUrlGeoserver = this.m_oConstantsService.getWmsUrlGeoserver();
        var oWMSOptions= { // wms options
            transparent: true,
            format: 'image/png',
            //crossOriginKeyword: null
        };//crossOriginKeyword: null

        // WMS get GEOSERVER
        var oProvider = new Cesium.WebMapServiceImageryProvider({
            url : sUrlGeoserver,
            layers:'wasdi:' + sLayerId,
            parameters : oWMSOptions,

        });
        oGlobeLayers.addImageryProvider(oProvider);
        //this.test=oGlobeLayers.addImageryProvider(oProvider);
    }

    /**
     * Call Download Image Service
     * @param sUrl
     */
    EditorController.prototype.downloadEOImage = function (sUrl) {
        this.m_oFileBufferService.download(sUrl,this.m_oActiveWorkspace.workspaceId).success(function (data, status) {
            utilsVexDialogAlertBottomRightCorner('downloading');
            //console.log('downloading');
        }).error(function (data, status) {
            utilsVexDialogAlertTop('download error');
            //console.log('download error');
        });
    }

    /**
     * Call publish service
     * @param sUrl
     */
    EditorController.prototype.publish = function (sUrl) {
        this.m_oFileBufferService.publish(sUrl,this.m_oActiveWorkspace.workspaceId).success(function (data, status) {
            utilsVexDialogAlertBottomRightCorner('publishing');
            //console.log('publishing');
        }).error(function (data, status) {
            utilsVexDialogAlertTop('publish error');
            //console.log('publish error');
        });
    }

    /**
     * Get a list of product items with only name and index linked to the m_aoProducts ProductViewModel array
     * @returns {Array}
     */
    EditorController.prototype.getProductList = function () {
        var aoProductItems = [];

        var iProductsCount = this.m_aoProducts.length;

        for (var i=0; i<iProductsCount; i++){
            var oProduct = this.m_aoProducts[i];

            var oProductItem = [];
            oProductItem.name = oProduct.name;
            oProductItem.fileName = oProduct.fileName;
            oProductItem.index = i;
            aoProductItems.push(oProductItem);
        }

        return aoProductItems;
    }

    /**
     * Get a list of bands for a product
     * @param oProductItem
     * @returns {Array}
     */
    EditorController.prototype.getBandsForProduct = function (oProductItem) {
        var asBands = [];

        var iProductsCount = this.m_aoProducts.length;

        if (oProductItem.index>=iProductsCount) return asBands;

        var oProduct = this.m_aoProducts[oProductItem.index];

        var aoBands = oProduct.bandsGroups.bands;
        if(!utilsIsObjectNullOrUndefined(aoBands))
            var iBandCount = aoBands.length;
        else
            var iBandCount = 0;

        for (var i=0; i<iBandCount; i++) {
            var oBandItem = {};
            oBandItem.name = aoBands[i].name;
            oBandItem.productName = oProductItem.name;
            oBandItem.productIndex = oProductItem.index;

            asBands.push(oBandItem);
        }

        return asBands;
    }

    // OPEN BAND
    // oIdBandNodeInTree is the node id in tree, in some case when the page is reload
    // we need know the node id for change the icon
    EditorController.prototype.openBandImage = function (oBand,oIdBandNodeInTree) {
        var oController=this;
        var sFileName = this.m_aoProducts[oBand.productIndex].fileName;

        this.m_oFileBufferService.publishBand(sFileName,this.m_oActiveWorkspace.workspaceId, oBand.name).success(function (data, status) {
            var oDialog = utilsVexDialogAlertBottomRightCorner('publishing band ' + oBand.name);
            utilsVexCloseDialogAfterFewSeconds(3000,oDialog);
            //console.log('publishing band ' + oBand.name);
            if(!utilsIsObjectNullOrUndefined(data) &&  data.messageResult != "KO" && utilsIsObjectNullOrUndefined(data.messageResult))
            {
                /*if the band was published*/
                if(data.messageCode == "PUBLISHBAND" )
                    oController.receivedPublishBandMessage(data.payload);
                else
                    oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
                    //oController.m_oProcessesLaunchedService.addProcessesByLocalStorage(oBand.productName + "_" + oBand.name,
                    //                                                    oIdBandNodeInTree,
                    //                                                    oController.m_oProcessesLaunchedService.getTypeOfProcessPublishingBand(),
                    //                                                    oController.m_oActiveWorkspace.workspaceId,
                    //                                                    oController.m_oUser.userId);

                /*{processName:oBand.productName + "_" + oBand.name,
                 nodeId:oIdBandNodeInTree,
                 typeOfProcess:oController.m_oProcessesLaunchedService.getTypeOfProcessPublishingBand()}
                *
                * */
                //else
                //    oController.pushProcessInListOfRunningProcesses(oBand.productName + "_" + oBand.name,oIdBandNodeInTree);
                //TODO PUSH PROCESS WITH SERVICE
            }
            else
            {
                //TODO ERROR
                utilsVexDialogAlertTop("Error in publish band");
                //console.log("Error in publish band");
            }

        }).error(function (data, status) {
            console.log('publish band error');
            utilsVexDialogAlertTop("Error in publish band");
            //TODO ERROR
        });
    }

    //REMOVE BAND
    EditorController.prototype.removeBandImage = function (oBand)
    {
        if(utilsIsObjectNullOrUndefined(oBand) == true)
        {
            console.log("Error in removeBandImage")
            return false;
        }

        var oController = this;
        var sLayerId="wasdi:"+oBand.productName+ "_" +oBand.name;

        var oMap2D = oController.m_oMapService.getMap();
        var oGlobeLayers = oController.m_oGlobeService.getGlobeLayers();


        //remove layer in 2D map
        oMap2D.eachLayer(function(layer)
        {
            if(utilsIsStrNullOrEmpty(sLayerId) == false && layer.options.layers == sLayerId)
                oMap2D.removeLayer(layer);
        });

        //remove layer in 3D globe
        var oLayer = null;
        var bCondition = true;
        var iIndexLayer = 0;

        while(bCondition)
        {
            oLayer = oGlobeLayers.get(iIndexLayer);

            if(utilsIsStrNullOrEmpty(sLayerId) == false && utilsIsObjectNullOrUndefined(oLayer) == false
                && oLayer.imageryProvider.layers == sLayerId)
            {
                bCondition=false;
                oLayer=oGlobeLayers.remove(oLayer);
            }
            iIndexLayer++;
        }

        //Remove layer from layers list
        var iLenghtLayersList;
        if(utilsIsObjectNullOrUndefined(oController.m_aoLayersList))
            iLenghtLayersList = 0;
        else
            iLenghtLayersList=oController.m_aoLayersList.length;

        for (var iIndex=0; iIndex < iLenghtLayersList ;iIndex++)
        {
            if(utilsIsStrNullOrEmpty(sLayerId) == false && utilsIsSubstring(sLayerId,oController.m_aoLayersList[iIndex].layerId))
            {
                oController.m_aoLayersList.splice(iIndex);
            }
        }
    }

    // GENERATE TREE
    // Expected format of the node (there are no required fields)
    //{
    //    id          : "string" // will be autogenerated if omitted
    //    text        : "string" // node text
    //    icon        : "string" // string for custom
    //    state       : {
    //        opened    : boolean  // is the node open
    //        disabled  : boolean  // is the node disabled
    //        selected  : boolean  // is the node selected
    //    },
    //    children    : []  // array of strings or objects
    //    li_attr     : {}  // attributes for the generated LI node
    //    a_attr      : {}  // attributes for the generated A node
    //}
    // Alternative format of the node (id & parent are required)
    //{
    //    id          : "string" // required
    //    parent      : "string" // required
    //    text        : "string" // node text
    //    icon        : "string" // string for custom
    //    state       : {
    //        opened    : boolean  // is the node open
    //        disabled  : boolean  // is the node disabled
    //        selected  : boolean  // is the node selected
    //    },
    //    li_attr     : {}  // attributes for the generated LI node
    //    a_attr      : {}  // attributes for the generated A node
    //}
    EditorController.prototype.generateTree = function ()
    {
        var oController = this;
        var oTree =
        {
            'core': {'data': [], "check_callback": true},
            "plugins" : [ "contextmenu" ],  // all plugin i use
            "contextmenu" : { // my right click menu
                "items" : function ($node)
                {
                    //only the band has property $node.original.band
                    var oReturnValue = null;
                    if(utilsIsObjectNullOrUndefined($node.original.band) == false && $node.icon == "assets/icons/check.png")
                    {
                        //BAND
                        var oBand=$node.original.band;

                        oReturnValue =
                        {
                            "Zoom2D" : {
                                "label" : "Zoom Band 2D Map",
                                "action" : function (obj) {
                                    if(utilsIsObjectNullOrUndefined(oBand) == false)
                                        oController.zoomOnLayer2DMap(oBand.productName+"_"+oBand.name);
                                }
                            },
                            "Zoom3D" : {
                                "label" : "Zoom Band 3D Map",
                                "action" : function (obj) {
                                    if(utilsIsObjectNullOrUndefined(oBand) == false)
                                        oController.zoomOnLayer3DGlobe(oBand.productName+"_"+oBand.name);
                                }
                            }

                        };
                    }
                    //only products has $node.original.fileName
                    if(utilsIsObjectNullOrUndefined($node.original.fileName) == false)
                    {
                        //PRODUCT
                        oReturnValue =
                        {
                            "prova1" : {
                                "label" : "Delete Product",
                                "action" : function (obj) {

                                    utilsVexDialogConfirm("Deleting product. Do you want to delete files on file system?", function(value) {
                                        var bDeleteFile = false;
                                        if (value)
                                            bDeleteFile = true;

                                        oController.m_oProductService.deleteProductFromWorkspace($node.original.fileName, oController.m_oActiveWorkspace.workspaceId, bDeleteFile)
                                            .success(function (data) {

                                            }).error(function (error) {

                                        });

                                    });


                                }
                            },
                            "prova2" : {
                                "label" : "operazione2",
                                "action" : function (obj) {  }
                            },
                            "prova3" : {
                                "label" : "operazione3",
                                "action" : function (obj) {  }
                            }
                        };
                    }


                    return oReturnValue;
                }
            }
        }



        var productList = this.getProductList();
        //for each product i generate sub-node
        for (var iIndexProduct = 0; iIndexProduct < productList.length; iIndexProduct++) {

            //product node
            var oNode = new Object();
            oNode.text=productList[iIndexProduct].name;//LABEL NODE
            oNode.fileName=productList[iIndexProduct].fileName;//LABEL NODE
            oNode.children=[{"text": "metadata"},{"text":"Bands", "children": []}];//CHILDREN
            oTree.core.data.push(oNode);


            var oaBandsItems = this.getBandsForProduct(productList[iIndexProduct]);

            for (var iIndexBandsItems = 0; iIndexBandsItems < oaBandsItems.length; iIndexBandsItems++)
            {
                var oNode=new Object();
                oNode.text = oaBandsItems[iIndexBandsItems].name;//LABEL NODE
                oNode.band = oaBandsItems[iIndexBandsItems];//BAND
                oNode.icon = "assets/icons/uncheck.png";
                oTree.core.data[iIndexProduct].children[1].children.push(oNode);

            }

        }

        return oTree;
    }

    EditorController.prototype.getProductListByWorkspace = function()
    {
        var oController = this;
        oController.m_aoProducts = [];

        this.m_oProductService.getProductListByWorkspace(oController.m_oActiveWorkspace.workspaceId).success(function (data, status) {
            if (data != null) {
                if (data != undefined) {
                    //push all products
                    for(var iIndex = 0; iIndex < data.length; iIndex++)
                    {
                        oController.m_aoProducts.push(data[iIndex]);
                    }
                    // i need to make the tree after the products are loaded
                    oController.m_oTree = oController.generateTree();
                    //oController.m_oScope.$apply();
                }
            }
        }).error(function (data, status) {
            utilsVexDialogAlertTop('Error reading product list');
            //console.log('Error reading product list');
        });
    }
    /* Search element in tree
    * */

    //---------------- OPENWORKSPACE -----------------------
    // ReLOAD workspace when reload page
    EditorController.prototype.openWorkspace = function (sWorkspaceId) {

        var oController = this;

        this.m_oWorkspaceService.getWorkspaceEditorViewModel(sWorkspaceId).success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    oController.m_oConstantsService.setActiveWorkspace(data);
                    oController.m_oActiveWorkspace = oController.m_oConstantsService.getActiveWorkspace();
                    /*Start Rabbit WebStomp*/
                    oController.m_oRabbitStompServive.initWebStomp(oController.m_oActiveWorkspace,"EditorController",oController);
                    oController.getProductListByWorkspace();
                    oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
                }
            }
        }).error(function (data,status) {
            //alert('error');
            utilsVexDialogAlertTop('error Impossible get workspace in editorController.js')
        });
    }

    /**************** MAP 3D/2D MODE ON/OFF  (SWITCH)*************************/
    EditorController.prototype.onClickChangeMap=function()
    {
        var oController=this;

        oController.m_b2DMapModeOn = !oController.m_b2DMapModeOn;
        oController.m_b3DMapModeOn = !oController.m_b3DMapModeOn;

        //3D MAP
        if (oController.m_b2DMapModeOn == false && oController.m_b3DMapModeOn == true) {
            oController.m_oMapService.clearMap();
            oController.m_oGlobeService.clearGlobe();
            oController.m_oGlobeService.initGlobe('cesiumContainer');
            oController.m_oMapService.initMap('wasdiMap2');

            //setTimeout(function(){ oController.m_oMapService.getMap().invalidateSize()}, 400);
            oController.delayInLoadMaps();
            // Load Layers
            oController.loadLayersMap2D();
            oController.loadLayersMap3D();
        }//2D MAP
        else if (oController.m_b2DMapModeOn == true && oController.m_b3DMapModeOn == false) {
            oController.m_oMapService.clearMap();
            oController.m_oGlobeService.clearGlobe();
            oController.m_oMapService.initMap('wasdiMap');
            oController.m_oGlobeService.initGlobe('cesiumContainer2');
            //setTimeout(function(){ oController.m_oMapService.getMap().invalidateSize()}, 400);
            oController.delayInLoadMaps();
            // Load Layers
            oController.loadLayersMap2D();
            oController.loadLayersMap3D();
        }

    }

    EditorController.prototype.delayInLoadMaps=function()
    {
        var oController=this;
        setTimeout(function(){ oController.m_oMapService.getMap().invalidateSize()}, 400);
    }

    //Use it when switch mapd 2d/3d
    EditorController.prototype.loadLayersMap2D=function()
    {
        var oController=this;
        if(utilsIsObjectNullOrUndefined(oController.m_aoLayersList))
        {
            console.log('Error in layers list');
            return false;
        }

        for(var iIndexLayers=0; iIndexLayers < oController.m_aoLayersList.length; iIndexLayers++)
        {
            oController.addLayerMap2D(oController.m_aoLayersList[iIndexLayers].layerId);
        }
    }

    //Use it when switch map 2d/3d
    EditorController.prototype.loadLayersMap3D=function()
    {
        var oController=this;
        if(utilsIsObjectNullOrUndefined(oController.m_aoLayersList))
        {
            console.log('Error in layers list');
            return false;
        }

        for(var iIndexLayers=0; iIndexLayers < oController.m_aoLayersList.length; iIndexLayers++)
        {
            oController.addLayerMap3D(oController.m_aoLayersList[iIndexLayers].layerId);
        }
    }

    /*
        synchronize the 3D Map and 2D map
     */
    EditorController.prototype.synchronize3DMap = function() {

        var oMap = this.m_oMapService.getMap();
        var oBoundsMap = oMap.getBounds();/* it take the edge of 2d map*/

        var oGlobe = this.m_oGlobeService.getGlobe();
        /* set view of globe*/
        oGlobe.camera.setView({
            destination: Cesium.Rectangle.fromDegrees(oBoundsMap.getWest(), oBoundsMap.getSouth(), oBoundsMap.getEast(), oBoundsMap.getNorth()),
            orientation: {
                heading: 0.0,
                pitch: -Cesium.Math.PI_OVER_TWO,
                roll: 0.0
            }

        });

    }
    /*
     synchronize the 2D Map and 3D map
     */
    EditorController.prototype.synchronize2DMap = function() {

        var oMap = this.m_oMapService.getMap();
        var oGlobe = this.m_oGlobeService.getGlobe();
        var oCenter = this.m_oGlobeService.getMapCenter();
        if(utilsIsObjectNullOrUndefined(oCenter))
            return false;
        oMap.flyTo(oCenter);
        //var oRectangle = oGlobe.scene.camera.computeViewRectangle(oGlobe.scene.globe.ellipsoid);
        //if(utilsIsObjectNullOrUndefined(oRectangle))
        //    return false;
        //// center map
        //var oBoundaries = L.latLngBounds(oRectangle.south,oRectangle.west,oRectangle.north,oRectangle.east);
        //oMap.fitBounds(oBoundaries);
        return true;
    }

    EditorController.prototype.zoomOnLayer3DGlobe = function(oLayerId)
    {
        if(utilsIsObjectNullOrUndefined(oLayerId))
            return false;
        if(utilsIsObjectNullOrUndefined(this.m_aoLayersList))
            return false
        var iNumberOfLayers = this.m_aoLayersList.length;

        for(var iIndexLayer = 0; iIndexLayer < iNumberOfLayers; iIndexLayer++)
        {
            if(this.m_aoLayersList[iIndexLayer].layerId == oLayerId)
                break;
        }

        if( !(iIndexLayer < iNumberOfLayers))//there isn't layer in layerList
            return false;

        var oBoundingBox = JSON.parse(this.m_aoLayersList[iIndexLayer].boundingBox);

        if(utilsIsObjectNullOrUndefined(oBoundingBox)== true)
            return false;

        var oGlobe = this.m_oGlobeService.getGlobe();
        /* set view of globe*/
        oGlobe.camera.setView({
            destination:  Cesium.Rectangle.fromDegrees( oBoundingBox.minx , oBoundingBox.miny , oBoundingBox.maxx,oBoundingBox.maxy),
            orientation: {
                heading: 0.0,
                pitch: -Cesium.Math.PI_OVER_TWO,
                roll: 0.0
            }

        });

        return true;
    }

    EditorController.prototype.zoomOnLayer2DMap = function(oLayerId)
    {
        if(utilsIsObjectNullOrUndefined(oLayerId))
            return false;
        if(utilsIsObjectNullOrUndefined(this.m_aoLayersList))
            return false
        var iNumberOfLayers = this.m_aoLayersList.length;

        for(var iIndexLayer = 0; iIndexLayer < iNumberOfLayers; iIndexLayer++)
        {
            if(this.m_aoLayersList[iIndexLayer].layerId == oLayerId)
                break;
        }

        if( !(iIndexLayer < iNumberOfLayers))//there isn't layer in layerList
            return false;

        var oBoundingBox = JSON.parse(this.m_aoLayersList[iIndexLayer].boundingBox);

        if(utilsIsObjectNullOrUndefined(oBoundingBox)== true)
            return false;

        var oMap = this.m_oMapService.getMap();
        var corner1 = L.latLng(oBoundingBox.maxy,oBoundingBox.maxx),
            corner2 = L.latLng( oBoundingBox.miny,oBoundingBox.minx  ),
            bounds = L.latLngBounds(corner1, corner2);
        oMap.fitBounds(bounds);

        return true;
    }
    /* Push process in List of Running Processes (in server)
    * */

    //EditorController.prototype.pushProcessInListOfRunningProcesses=function(sProcess,oIdBandNodeInTree)
    //{
    //    //if str == null or == ""
    //    if(utilsIsStrNullOrEmpty(sProcess))
    //        return false;
    //
    //    var iNumberOfProcessesRunning;
    //    var bFind=false;
    //    var oController=this;
    //
    //    //check the number of processes
    //    if(utilsIsObjectNullOrUndefined(oController.m_aoProcessesRunning)==true)
    //    {
    //        iNumberOfProcessesRunning = 0;
    //    }
    //    else
    //    {
    //        iNumberOfProcessesRunning = oController.m_aoProcessesRunning.length;
    //    }
    //    //it doesn't push the process if it already exist
    //    for( var iIndexProcesses = 0; iIndexProcesses < iNumberOfProcessesRunning ; iIndexProcesses++)
    //    {
    //        // if it find a process in ProcessesRunningList it doesn't need to push it
    //         if(oController.m_aoProcessesRunning[iIndexProcesses].processName == sProcess)
    //         {
    //             bFind=true;
    //             break;
    //         }
    //    }
    //
    //    if(bFind == false) {
    //
    //        oController.m_aoProcessesRunning.push({processName:sProcess,nodeId:oIdBandNodeInTree});
    //        //update cookie
    //        oController.m_oConstantsService.setCookie("m_aoProcessesRunning", oController.m_aoProcessesRunning, 1);
    //    }
    //
    //}

    /*
    Remove process in List of Running processes
    * */
    //EditorController.prototype.removeProcessInListOfRunningProcesses=function(sProcess)
    //{
    //    if(utilsIsStrNullOrEmpty(sProcess))
    //        return false;
    //
    //    var oController=this;
    //    var iLength;
    //
    //    if(utilsIsObjectNullOrUndefined(oController.m_aoProcessesRunning) == true )
    //    {
    //        iLength = 0;
    //    }
    //    else
    //    {
    //        iLength = oController.m_aoProcessesRunning.length;
    //    }
    //
    //    for(var iIndex = 0;iIndex < iLength ;iIndex++ )
    //    {
    //        if(oController.m_aoProcessesRunning[iIndex].processName == sProcess)
    //        {
    //            oController.m_aoProcessesRunning.splice(iIndex);
    //            //update cookie
    //            oController.m_oConstantsService.setCookie("m_aoProcessesRunning", oController.m_aoProcessesRunning, 1);
    //        }
    //    }
    //
    //    //oController.m_oScope.$apply();CHECK IF WE NEED IT
    //}

    /*
    * */
    //EditorController.prototype.isEmptyListOfRunningProcesses = function()
    //{
    //    var oController = this;
    //    if(utilsIsObjectNullOrUndefined(oController.m_aoProcessesRunning) == false)
    //    {
    //        if(oController.m_aoProcessesRunning.length == 0)
    //            return true;
    //        else
    //            return false;
    //    }
    //    else
    //    {
    //        return true;
    //    }
    //
    //

    /* fetch (after a page reload)all the running processes, check all the uncheck nodes
    *  corresponding to the process
    * */

    //TODO CHANGE IT
    //EditorController.prototype.checkNodesInTree = function()
    //{
    //    var oController = this;
    //
    //    if(utilsIsObjectNullOrUndefined(oController.m_aoProcessesRunning) || oController.m_aoProcessesRunning.length == 0) {
    //        return false;
    //    }
    //
    //    var iLength = oController.m_aoProcessesRunning.length;
    //    for(var iIndex = 0; iIndex < iLength; iIndex ++)
    //    {
    //        var sNodeId = oController.m_aoProcessesRunning[iIndex].nodeId;
    //        $('#jstree').jstree(true).set_icon(sNodeId,"assets/icons/check.png");
    //    }
    //    return true;
    //
    //}

    EditorController.$inject = [
        '$scope',
        '$location',
        '$interval',
        'ConstantsService',
        'AuthService',
        'MapService',
        'FileBufferService',
        'ProductService',
        '$state',
        'WorkspaceService',
        'GlobeService',
        'ProcessesLaunchedService',
        'RabbitStompService'
    ];

    return EditorController;
})();
