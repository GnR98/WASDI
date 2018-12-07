/**
 * Created by a.corrado on 16/06/2017.
 */



var WorkFlowManagerController = (function() {

    function WorkFlowManagerController($scope, oClose,oExtras,oSnapOperationService,oConstantsService, oHttp) {
        this.m_oScope = $scope;
        this.m_oClose = oClose;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_oFile = null;
        this.m_aoProducts = this.m_oExtras.products;
        this.m_sWorkspaceId = this.m_oExtras.workflowId;
        this.m_asProductsName = utilsProjectGetProductsName(this.m_aoProducts);
        this.m_asSelectedProducts = [];
        this.m_aoWorkflows = [];
        this.m_oSelectedWorkflow = null;
        this.m_oSelectedMultiInputWorkflow = null;

        this.m_oConstantsService = oConstantsService;
        this.m_oWorkflowFileData = {
            workflowName:"",
            workflowDescription:""
        };
        this.isUploadingWorkflow = false;
        if( utilsIsObjectNullOrUndefined(this.m_oExtras.defaultTab) === true)
        {
            this.m_sSelectedWorkflowTab = 'WorkFlowTab1';
        }
        else
        {
            this.m_sSelectedWorkflowTab = this.m_oExtras.defaultTab;
        }
        // this.m_sSelectedWorkflowTab = 'WorkFlowTab1';
        this.m_bIsLoadingWorkflows = false;
        this.m_oHttp =  oHttp;
        //$scope.close = oClose;
        var oController = this;

        $scope.close = function(result) {

            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

        //Load workflows
        this.getWorkflowsByUser();


    }

    WorkFlowManagerController.prototype.getWorkflowsByUser = function()
    {
        var oController = this;
        this.m_bIsLoadingWorkflows = true;
        this.m_oSnapOperationService.getWorkflowsByUser().success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                oController.m_aoWorkflows = data;
                //TODO REMOVE IT
                oController.m_aoWorkflows.push({
                    "description": "",
                    "inputFileNames": [],
                    "inputNodeNames": ["read1","read2","read3","read4"],
                    "name": "workflow",
                    "outputFileNames": [],
                    "outputNodeNames": [],
                    "workflowId": "04b0fd8e-92e6-4ba1-83ff-56858795f5d2"
                })
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET WORKFLOWS, THERE AREN'T DATA");
            }

            //it changes the default tab, we can't visualize the 'WorkFlowTab1' because there aren't workflows
            if( (utilsIsObjectNullOrUndefined(oController.m_aoWorkflows) === true) || (oController.m_aoWorkflows.length === 0) )
            {
                oController.m_sSelectedWorkflowTab = 'WorkFlowTab2';
            }
            oController.m_bIsLoadingWorkflows = false;
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET WORKFLOWS");
            oController.m_bIsLoadingWorkflows = false;
        });
    };

    /**
     * runWorkFlowPerProducts
     */
    WorkFlowManagerController.prototype.runWorkFlowPerProducts = function()
    {
        var iNumberOfProducts = this.m_asSelectedProducts.length;
        if(utilsIsObjectNullOrUndefined(this.m_oSelectedWorkflow) === true)
        {
            return false;
        }
        for(var iIndexSelectedProduct = 0 ; iIndexSelectedProduct < iNumberOfProducts; iIndexSelectedProduct++)
        {
            var oProduct = utilsProjectGetProductByName(this.m_asSelectedProducts[iIndexSelectedProduct],this.m_aoProducts);
            if(utilsIsObjectNullOrUndefined(oProduct))
            {
                return false;
            }
            //TODO REMOVE IT
            var sDestinationProductName = oProduct.name + "_workflow";
            //TODO ADD DESCRIPTION
            var oSnapWorkflowViewModel = this.getObjectExecuteGraph(this.m_oSelectedWorkflow.workflowId,oProduct.fileName,"",
                this.m_oSelectedWorkflow.inputNodeNames, this.m_oSelectedWorkflow.inputFileNames,this.m_oSelectedWorkflow.outputNodeNames,
                this.m_oSelectedWorkflow.outputFileNames);

            this.executeGraphFromWorkflowId(this.m_sWorkspaceId,oSnapWorkflowViewModel);
        }
        return true;
    };

    WorkFlowManagerController.prototype.runMultiInputWorkFlow=function()
    {
        //TODO ADD DESCRIPTION
        var oSnapWorkflowViewModel = this.getObjectExecuteGraph(this.m_oSelectedWorkflow.workflowId,oProduct.fileName,"",
            this.m_oSelectedWorkflow.inputNodeNames, this.m_oSelectedWorkflow.inputFileNames,this.m_oSelectedWorkflow.outputNodeNames,
            this.m_oSelectedWorkflow.outputFileNames);

        this.executeGraphFromWorkflowId(this.m_sWorkspaceId,oSnapWorkflowViewModel);

    };

    WorkFlowManagerController.prototype.getObjectExecuteGraph = function(sWorkflowId,sName,sDescription,asInputNodeNames,
                                                                         asInputFileNames,asOutputNodeNames,asOutputFileNames)
    {
        if(this.areOkDataExecuteGraph(sWorkflowId,sName,asInputNodeNames, asInputFileNames) === true)
        {
            return null;
        }
        var oExecuteGraph = this.getEmptyObjectExecuteGraph();
        oExecuteGraph.workflowId = sWorkflowId;
        oExecuteGraph.name = sName;
        oExecuteGraph.description = sDescription;
        oExecuteGraph.inputNodeNames = asInputNodeNames;
        oExecuteGraph.asInputFileNames = asInputFileNames;
        oExecuteGraph.asOutputNodeNames = asOutputNodeNames;
        oExecuteGraph.asOutputFileNames = asOutputFileNames;

        return oExecuteGraph;
    };

    WorkFlowManagerController.prototype.areOkDataExecuteGraph = function(sWorkflowId,sName,asInputNodeNames,
                                                                         asInputFileNames)
    {
        var bReturnValue = true;
        if(utilsIsStrNullOrEmpty(sWorkflowId) || utilsIsStrNullOrEmpty(sName) || utilsIsObjectNullOrUndefined(asInputNodeNames) ||
            utilsIsObjectNullOrUndefined(asInputFileNames) )
        {
            bReturnValue = false;
        }
        if(asInputNodeNames.length !== asInputFileNames.length)
        {
            bReturnValue = false;
        }

        return bReturnValue;
    };

    WorkFlowManagerController.prototype.getEmptyObjectExecuteGraph = function()
    {
        return {
            workflowId:"",
            name:"",
            description:"",
            inputNodeNames:[],
            inputFileNames:[],
            outputNodeNames:[],
            outputFileNames:[]
        }
    };
    /**
     * executeGraphFromWorkflowId
     * @param sWorkspaceId
     * @param sProductNameSelected
     * @param sDestinationProductName
     * @param sWorkflowId
     * @returns {boolean}
     */
    WorkFlowManagerController.prototype.executeGraphFromWorkflowId = function(sWorkspaceId,oObjectWorkFlow)
    {
        if(utilsIsObjectNullOrUndefined(sWorkspaceId) === true)
        {
            return false;
        }
        if(utilsIsObjectNullOrUndefined(oObjectWorkFlow) === true)
        {
            return false;
        }
        var oController = this;
        this.m_oSnapOperationService.executeGraphFromWorkflowId(sWorkspaceId,oObjectWorkFlow).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) === false && data.boolValue === true )
            {
                oController.cleanAllExecuteWorkflowFields();
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN EXECUTE WORKFLOW,");
            }
            oController.closeDialogWithDelay("",500);

        }).error(function (error) {

            oController.cleanAllExecuteWorkflowFields();
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN EXECUTE WORKFLOW");
        });

        return true;
    };
    WorkFlowManagerController.prototype.closeDialogWithDelay = function(result,iDelay) {

        this.m_oClose(result, 700); // close, but give 500ms for bootstrap to animate
    };
    /**
     * deleteWorkflow
     * @param oWorkflow
     * @returns {boolean}
     */
    WorkFlowManagerController.prototype.deleteWorkflow = function(oWorkflow)
    {
        if( utilsIsObjectNullOrUndefined(oWorkflow) === true)
        {
            return false;
        }
        var oController = this;
        this.m_oSnapOperationService.deleteWorkflow(oWorkflow.workflowId).success(function (data)
        {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                oController.getWorkflowsByUser();
            }
            else
            {
                //TODO ERROR
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETE WORKFLOW");
            }
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN DELETE WORKFLOW");
        });

        return true;
    };

    /**
     * uploadUserGraphOnServer
     */

    WorkFlowManagerController.prototype.uploadUserGraphOnServer = function()
    {

        if(utilsIsStrNullOrEmpty(this.m_oWorkflowFileData.workflowName) === true)
        {
            this.m_oWorkflowFileData.workflowName = "workflow";
        }
        var oBody = new FormData();
        oBody.append('file', this.m_oFile[0]);

        this.uploadGraph(this.m_sWorkspaceId, this.m_oWorkflowFileData.workflowName,this.m_oWorkflowFileData.workflowDescription,oBody);
    };

    /**
     * uploadGraph
     * @param sWorkspaceId
     * @param sName
     * @param sDescription
     * @param oBody
     * @returns {boolean}
     */
    WorkFlowManagerController.prototype.uploadGraph = function(sWorkspaceId,sName,sDescription,oBody)
    {
        if(utilsIsObjectNullOrUndefined(sWorkspaceId) === true)
        {
            return false;
        }
        if(utilsIsObjectNullOrUndefined(sName) === true || utilsIsStrNullOrEmpty(sName) === true)
        {
            return false;
        }
        if(utilsIsObjectNullOrUndefined(sDescription) === true )//|| utilsIsStrNullOrEmpty(sDescription) === true
        {
            return false;
        }
        if(utilsIsObjectNullOrUndefined(oBody) === true )
        {
            return false;
        }
        this.isUploadingWorkflow=true;
        var oController = this;
        this.m_oSnapOperationService.uploadGraph(this.m_sWorkspaceId,sName,sDescription,oBody).success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                //Reload list o workFlows
                oController.getWorkflowsByUser();
                oController.cleanAllUploadWorkflowFields();
                var oDialog = utilsVexDialogAlertBottomRightCorner("SUCCESSFUL UPLOAD");
                utilsVexCloseDialogAfterFewSeconds(4000,oDialog);
            }
            else
            {
                //TODO ERROR
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPLOAD WORKFLOW PROCESS");
            }

            oController.isUploadingWorkflow = false;
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN UPLOAD WORKFLOW PROCESS");
            oController.cleanAllUploadWorkflowFields();
            oController.isUploadingWorkflow = false;
        });

        return true;
    };
    /**
     *
     * @returns {boolean}
     */
    WorkFlowManagerController.prototype.isSelectedProduct = function(){
        return (this.m_asSelectedProducts.length > 0);
    };
    /**
     *
     * @returns {boolean}
     */
    WorkFlowManagerController.prototype.isSelectedWorkFlow = function(){
        return !utilsIsObjectNullOrUndefined(this.m_oSelectedWorkflow);
    };
    /**
     *
     * @returns {boolean}
     */
    WorkFlowManagerController.prototype.isUploadedNewWorkFlow = function (){
        return !utilsIsObjectNullOrUndefined(this.m_oFile);
    };

    /**
     *
     */
    WorkFlowManagerController.prototype.cleanAllUploadWorkflowFields = function (){
        this.m_oWorkflowFileData = {
            workflowName:"",
            workflowDescription:""
        };
        this.m_oFile = null;
    };

    /**
     *
     */
    WorkFlowManagerController.prototype.cleanAllExecuteWorkflowFields = function (){
        this.m_asSelectedProducts = [];
        this.m_oSelectedWorkflow = null;
    };

    WorkFlowManagerController.prototype.isPossibleDoUpload = function()
    {
        // this.m_oWorkflowFileData.workflowName,this.m_oWorkflowFileData.workflowDescription    this.m_oFile[0]
        var bReturnValue = false;
        if( (utilsIsStrNullOrEmpty( this.m_oWorkflowFileData.workflowName) === false) && (utilsIsStrNullOrEmpty(this.m_oWorkflowFileData.workflowDescription) === false)
            && (utilsIsObjectNullOrUndefined(this.m_oFile[0]) === false))
        {
            bReturnValue = true;
        }
        return bReturnValue;
    };

    WorkFlowManagerController.prototype.getSingleInputWorkflow = function()
    {
        var iNumberOfWorkflows = this.m_aoWorkflows.length;
        var aoReturnArray = [];
        for(var iIndexWorkflow = 0 ; iIndexWorkflow < iNumberOfWorkflows ; iIndexWorkflow++)
        {
            if(this.m_aoWorkflows[iIndexWorkflow].inputNodeNames.length < 2 )
            {
                aoReturnArray.push(this.m_aoWorkflows[iIndexWorkflow]);
            }
        }
        return aoReturnArray;
    };
    WorkFlowManagerController.prototype.getMultipleInputWorkflow = function()
    {
        var iNumberOfWorkflows = this.m_aoWorkflows.length;
        var aoReturnArray = [];
        for(var iIndexWorkflow = 0 ; iIndexWorkflow < iNumberOfWorkflows ; iIndexWorkflow++)
        {
            if(this.m_aoWorkflows[iIndexWorkflow].inputNodeNames.length >= 2 )
            {
                aoReturnArray.push(this.m_aoWorkflows[iIndexWorkflow]);
            }
        }
        return aoReturnArray;
    };

    WorkFlowManagerController.prototype.addProductInputInNode = function(sNode,sProduct)
    {
        if(utilsIsStrNullOrEmpty(sNode) || utilsIsStrNullOrEmpty(sProduct) )
        {
            return false;
        }

        var iIndexOfElement = utilsFindObjectInArray(this.m_oSelectedMultiInputWorkflow.inputNodeNames,sNode)

        if(iIndexOfElement === -1)
        {
            return false;
        }

        this.m_oSelectedMultiInputWorkflow.inputFileNames[iIndexOfElement] =  sProduct;


        return true;
    };

    WorkFlowManagerController.$inject = [
        '$scope',
        'close',
        'extras',
        'SnapOperationService',
        'ConstantsService',
        '$http'
    ];
    return WorkFlowManagerController;
})();
