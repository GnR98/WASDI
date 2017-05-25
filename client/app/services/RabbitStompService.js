/**
 * Created by a.corrado on 23/01/2017.
 */
'use strict';
angular.module('wasdi.RabbitStompService', ['wasdi.RabbitStompService']).
service('RabbitStompService', ['$http',  'ConstantsService','$interval','ProcessesLaunchedService', function ($http, oConstantsService,$interval,oProcessesLaunchedService,$scope) {

    // Reconnection promise to stop the timer if the reconnection succeed or if the user change page
    this.m_oInterval = $interval;
    // Reference to the Constant Service
    this.m_oConstantsService = oConstantsService;
    // Scope
    this.m_oScope = $scope;

    this.m_oReconnectTimerPromise = null;
    this.m_oRabbitReconnect = null;

    // STOMP Client
    this.m_oClient = null;
    // Rabbit Connect Callback
    this.m_oOn_Connect = null;
    // Rabbit Error Callback
    this.m_oOn_Error = null;
    // Rabbit Reconnect Callback
    this.m_oRabbitReconnect = null;

    // Reference to the ProcessLaunched Service
    this.m_oProcessesLaunchedService = oProcessesLaunchedService;

    this.m_oSubscription = null;
    this.m_oUser = null;

    this.m_aoErrorsList = [];

    this.m_fMessageCallBack = null;

    this.m_oActiveController = null;

    this.setMessageCallback = function (fCallback) {
        this.m_fMessageCallBack = fCallback;
    }

    this.setActiveController = function (oController) {
        this.m_oActiveController = oController;
    }

    /*@Params: WorkspaceID, Name of controller, Controller
    * it need the Controller for call the methods (the methods are inside the active controllers)
    * the methods are call in oRabbitCallback
    * */
    this.initWebStomp = function()
    {
        // Web Socket to receive workspace messages
        //var oWebSocket = new WebSocket(this.m_oConstantsService.getStompUrl());
        var oWebSocket = new SockJS(this.m_oConstantsService.getStompUrl());
        var oThisService = this;
        this.m_oClient = Stomp.over(oWebSocket);
        this.m_oClient.heartbeat.outgoing = 0;
        this.m_oClient.heartbeat.incoming = 0;
        this.m_oClient.debug = null;


        /**
         * Called when the client receives a STOMP message from the server
         * Rabbit Callback: receives the Messages
         * @param message
         */
        var oRabbitCallback = function (message) {

            // Check message Body
            if (message.body)
            {
                console.log("RabbitStompService: got message with body " + message.body)

                // Get The Message View Model
                var oMessageResult = JSON.parse(message.body);

                // Check parsed object
                if (oMessageResult == null) {
                    console.log("RabbitStompService: there was an error parsing result in JSON. Message lost")
                    return;
                }

                // Get the Active Workspace Id
                var sActiveWorkspaceId = "";

                if (!utilsIsObjectNullOrUndefined(this.m_oConstantsService.getActiveWorkspace())) {
                    sActiveWorkspaceId = this.m_oConstantsService.getActiveWorkspace().workspaceId;
                }
                else {
                    console.log("Rabbit Stomp Service: Active Workspace is null.")
                }



                if (oMessageResult.messageResult == "KO") {

                    // Get the operation NAme
                    var sOperation = "null";
                    if (utilsIsStrNullOrEmpty(oMessageResult.messageCode) == false  ) sOperation = oMessageResult.messageCode;

                    // Add an error Message
                    oThisService.m_aoErrorsList.push({text:"There was an error in the " + sOperation + " operation"});
                    // Update Process Messages
                    oThisService.m_oProcessesLaunchedService.loadProcessesFromServer(sActiveWorkspaceId);

                    return;
                }

                //Reject the message if it is for another workspace
                if(oMessageResult.workspaceId != sActiveWorkspaceId) return false;


                var sUserMessage = "";

                if(!utilsIsObjectNullOrUndefined(oThisService.m_fMessageCallBack)) {
                    // Call the Message Callback
                    oThisService.m_fMessageCallBack(oMessageResult, oThisService.m_oActiveController);
                }
                // Update the process List
                oThisService.m_oProcessesLaunchedService.loadProcessesFromServer(sActiveWorkspaceId);

                // Get extra operations
                switch(oMessageResult.messageCode)
                {
                    case "DOWNLOAD":
                        sUserMessage = "File now available on WASDI Server";
                        break;
                    case "PUBLISH":
                        sUserMessage = "Publish done";
                        break;
                    case "PUBLISHBAND":
                        sUserMessage = "Band published. Product: " + oMessageResult.payload.productName;
                        break;
                    case "UPDATEPROCESSES":
                        console.log("UPDATE PROCESSES"+" " +utilsGetTimeStamp());
                        break;
                    case "APPLYORBIT":
                        sUserMessage = "Apply orbit Completed";
                        break;
                    case "CALIBRATE":
                        sUserMessage = "Radiometric Calibrate Completed";
                        break;
                    case "MULTILOOKING":
                        sUserMessage = "Multilooking Completed";
                        break;
                    case "NDVI":
                        sUserMessage = "NDVI Completed";
                        break;
                    case "TERRAIN":
                        sUserMessage = "Range doppler terrain correction Completed";
                        break;

                    default:
                        console.log("RABBIT ERROR: got empty message ");
                }

                // Is there a feedback for the user?
                if (!utilsIsStrNullOrEmpty(sUserMessage)) {
                    // Give the short message
                    var oDialog = utilsVexDialogAlertBottomRightCorner(sUserMessage);
                    utilsVexCloseDialogAfterFewSeconds(3000,oDialog);
                }

            }
        }


        /**
         * Callback of the Rabbit On Connect
         */
        var on_connect = function () {
            console.log('Web Stomp connected');

            //CHECK IF the session is valid
            var oSessionId = oThisService.m_oConstantsService.getSessionId();
            if(utilsIsObjectNullOrUndefined(oSessionId))
            {
                console.log("Error session id Null in on_connect");
                return false;
            }
            oThisService.m_oSubscription = oThisService.m_oClient.subscribe(oSessionId, oRabbitCallback);

            // Is this a re-connection?
            if (oThisService.m_oReconnectTimerPromise != null) {
                // Yes it is: clear the timer
                oThisService.m_oInterval.cancel(oThisService.m_oReconnectTimerPromise);
                oThisService.m_oReconnectTimerPromise = null;
            }
        };


        /**
         * Callback for the Rabbit On Error
         */
        var on_error = function (sMessage) {

            console.log('WEB STOMP ERROR, message:'+" "+utilsGetTimeStamp());
            //TODO OLD VERSION REMOVE IT
            if (sMessage == "LOST_CONNECTION") {
                console.log('LOST Connection');

                if (oThisService.m_oReconnectTimerPromise == null) {
                    // Try to Reconnect
                    oThisService.m_oReconnectTimerPromise = oThisService.m_oInterval(oThisService.m_oRabbitReconnect, 5000);
                }
            }

            //TODO new version of error remove comment
            if (sMessage == "Whoops! Lost connection to undefined") {
                console.log('Whoops! Lost connection to undefined');

                if (oThisService.m_oReconnectTimerPromise == null) {
                    // Try to Reconnect
                    oThisService.m_oReconnectTimerPromise = oThisService.m_oInterval(oThisService.m_oRabbitReconnect, 5000);
                }
            }
        };

        // Keep local reference to the callbacks to use it in the reconnection callback
        this.m_oOn_Connect = on_connect;
        this.m_oOn_Error = on_error;

        // Call back for rabbit reconnection
        var rabbit_reconnect = function () {

            console.log('Web Stomp Reconnection Attempt');

            // Connect again
            //oThisService.oWebSocket = new WebSocket(oThisService.m_oConstantsService.getStompUrl());
            oThisService.oWebSocket = new SockJS(oThisService.m_oConstantsService.getStompUrl());
            oThisService.m_oClient = Stomp.over(oThisService.oWebSocket);
            oThisService.m_oClient.heartbeat.outgoing = 0;
            oThisService.m_oClient.heartbeat.incoming = 0;
            oThisService.m_oClient.debug = null;

            oThisService.m_oClient.connect(oThisService.m_oConstantsService.getRabbitUser(), oThisService.m_oConstantsService.getRabbitPassword(), oThisService.m_oOn_Connect, oThisService.m_oOn_Error, '/');
        };


        this.m_oRabbitReconnect = rabbit_reconnect;
        //connect to the queue
        this.m_oClient.connect(oThisService.m_oConstantsService.getRabbitUser(), oThisService.m_oConstantsService.getRabbitPassword(), on_connect, on_error, '/');


        /*
        //// Clean Up when exit!!
        oControllerActive.m_oScope.$on('$destroy', function () {
            // Is this a re-connection?
            if (oThisService.m_oReconnectTimerPromise != null) {
                // Yes it is: clear the timer
                oThisService.m_oInterval.cancel(oThisService.m_oReconnectTimerPromise);
                oThisService.m_oReconnectTimerPromise = null;
            }
            else {

                if (oThisService.m_oClient != null) {
                    oThisService.m_oClient.disconnect();//TODO RESOLVE BUG IF THE USER SWITCH CONTROLLER
                }
            }
        });
        */

        return true;
    }

    this.initWebStomp();

    //This method remove a message in all queues
    //this.removeMessageInQueues = function(oMessage)
    //{
    //    if(utilsIsObjectNullOrUndefined(oMessage))
    //        return false;
    //    var iIndexMessageInEditoControllerQueue = utilsFindObjectInArray(this.m_aoEditorControllerQueueMessages,oMessage) ;
    //    var iIndexMessageInImportControllerQueue = utilsFindObjectInArray(this.m_aoImportControllerQueueMessages,oMessage) ;
    //    // TODO REMOVE TO CACHE
    //    /*Remove in editor controller*/
    //    if (iIndexMessageInEditoControllerQueue > -1) {
    //        this.m_aoEditorControllerQueueMessages.splice(iIndexMessageInEditoControllerQueue, 1);
    //    }
    //    /*remove in Import Controller*/
    //    if ( iIndexMessageInImportControllerQueue > -1) {
    //        this.m_aoImportControllerQueueMessages.splice( iIndexMessageInImportControllerQueue, 1);
    //    }
    //
    //}



}]);

