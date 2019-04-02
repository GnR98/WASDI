/**
 * Created by a.corrado on 24/05/2017.
 */


var GetListOfWorkspacesController = (function() {

    function GetListOfWorkspacesController($scope, oClose,oWorkspaceService,oExtras,oConstantsService,oSnapOperationService,oModalService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras
        this.m_sButtonName = oExtras.buttonName;
        this.m_sTitleModal = oExtras.titleModal;
        this.m_bSelectedAllWorkspaces = false;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_aoWorkspaceList = [];
        this.m_aoWorkspacesSelected = [];
        this.m_bisLoadingWorkspacesList = true;
        this.m_bIsCreatingWorskapce = false;
        this.m_oConstantsService = oConstantsService;
        this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
        this.m_oSnapOperationService = oSnapOperationService;
        this.m_oSelectedWorkflow = "";
        this.m_oModalService = oModalService;
        this.m_oClose = oClose;
        this.m_oActiveWorkspace = this.m_oConstantsService.getActiveWorkspace();
        this.m_aoWorkflows = [];
        this.m_bisLoadingWorkflows = false;
        this.m_bIsVisibleWorkFlowsOption = false;
        //$scope.close = oClose;
        $scope.close = function(result) {
            oClose(result, 200); // close, but give 500ms for bootstrap to animate
        };

        this.getWorkspaces();
        this.getWorkflowsByUser();
    }

    /**
     * selecteAllWorkspaces
     */
    GetListOfWorkspacesController.prototype.selectAllWorkspaces = function ()
    {
        this.m_bSelectedAllWorkspaces = true;
        var iNumberOfWorkspaces = this.m_aoWorkspaceList.length;
        for(var iIndexWorkspace = 0 ; iIndexWorkspace < iNumberOfWorkspaces ; iIndexWorkspace++)
        {
            this.selectWorkspace(this.m_aoWorkspaceList[iIndexWorkspace]);
        }
    }
    /**
     * deselectAllWorkspaces
     */
    GetListOfWorkspacesController.prototype.deselectAllWorkspaces = function ()
    {
        this.m_bSelectedAllWorkspaces = false;
        var iNumberOfWorkspaces = this.m_aoWorkspaceList.length;
        for(var iIndexWorkspace = 0 ; iIndexWorkspace < iNumberOfWorkspaces ; iIndexWorkspace++)
        {
            this.deselectWorkspace(this.m_aoWorkspaceList[iIndexWorkspace]);
        }
    }
    /**
     * getWorkspaces
     */
    GetListOfWorkspacesController.prototype.getWorkspaces = function()
    {
        var oController = this;
        this.m_bisLoadingWorkspacesList = true;
        this.m_oWorkspaceService.getWorkspacesInfoListByUser().success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    oController.m_aoWorkspaceList = data;
                    var oDefaultWorkspace = oController.getDefaultWorkspace(oController.m_oActiveWorkspace,oController.m_aoWorkspaceList);
                    if( utilsIsObjectNullOrUndefined(oDefaultWorkspace) === false)
                    {
                        oController.selectWorkspace(oDefaultWorkspace);
                    }
                }
            }
            oController.m_bisLoadingWorkspacesList = false;
        }).error(function (data,status) {
            //alert('error');
            oController.m_bisLoadingWorkspacesList = false;
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IN WORKSPACESINFO');
        });
    };

    GetListOfWorkspacesController.prototype.getDefaultWorkspace = function(oActiveWorkspace,aoWorkspaceList){
        if(utilsIsObjectNullOrUndefined(aoWorkspaceList) === true)
        {
            return null;
        }
        var iNumberOfWorkspaces = aoWorkspaceList.length;
        for(var iIndexWorkspace = 0; iIndexWorkspace < iNumberOfWorkspaces; iIndexWorkspace++)
        {
            if(utilsIsObjectNullOrUndefined(oActiveWorkspace) === false)
            {
                if(aoWorkspaceList[iIndexWorkspace].workspaceId === oActiveWorkspace.workspaceId)
                {
                    return aoWorkspaceList[iIndexWorkspace];
                }
            }

        }
        return null;
    };
    /**
     * selectedWorkspace
     * @param oWorkspace
     * @returns {boolean}
     */
    GetListOfWorkspacesController.prototype.selectWorkspace = function(oWorkspace){
        if(utilsIsObjectNullOrUndefined(oWorkspace) === true)
            return false;

        this.m_aoWorkspacesSelected.push(oWorkspace);
        return true;
    };

    /**
     * deselectWorkspace
     * @param oWorkspace
     * @returns {boolean}
     */
    GetListOfWorkspacesController.prototype.deselectWorkspace = function(oWorkspace) {
        if(utilsIsObjectNullOrUndefined(oWorkspace) === true)
            return false;
        utilsRemoveObjectInArray(this.m_aoWorkspacesSelected,oWorkspace);
        return true;
    };

    GetListOfWorkspacesController.prototype.isSelectedWorkspace = function(oWorkspace)
    {
        if(utilsIsObjectNullOrUndefined(oWorkspace) === true)
            return false;
        return utilsIsElementInArray(this.m_aoWorkspacesSelected,oWorkspace);
    };

    /**
     * closeModal
     */
    GetListOfWorkspacesController.prototype.closeModal= function(){

        this.m_oClose(null, 500); // close, but give 500ms for bootstrap to animate

    };

    /**
     * closeModalAndReturnSelectedWorkspaces
     */
    GetListOfWorkspacesController.prototype.closeModalAndReturnSelectedWorkspaces= function(){
        var aoResult = this.m_aoWorkspacesSelected;
        this.m_oClose(aoResult, 500); // close, but give 500ms for bootstrap to animate

    };

    /**
     * createWorkspace
     */
    GetListOfWorkspacesController.prototype.createWorkspace = function () {

        var oController = this;
        this.m_bIsCreatingWorskapce = true;
        this.m_oWorkspaceService.createWorkspace().success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    // var sWorkspaceId = data.stringValue;
                    // oController.openWorkspace(sWorkspaceId);
                    oController.getWorkspaces();
                }
            }
            oController.m_bIsCreatingWorskapce = false;
        }).error(function (data,status) {
            //alert('error');
            utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IN CREATE WORKSPACE');
            oController.m_bIsCreatingWorskapce = false;
        });
    };
    /**
     *
     */
    GetListOfWorkspacesController.prototype.getWorkflowsByUser = function()
    {
        var oController = this;
        // this.m_bIsLoadingWorkflows = true;
        oController.m_bisLoadingWorkflows = true;

        this.m_oSnapOperationService.getWorkflowsByUser().success(function (data) {
            if(utilsIsObjectNullOrUndefined(data) == false)
            {
                oController.m_aoWorkflows = data;
            }
            else
            {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET WORKFLOWS, THERE AREN'T DATA");
            }
            oController.m_bisLoadingWorkflows=false;

            //it changes the default tab, we can't visualize the 'WorkFlowTab1' because there aren't workflows
            // if( (utilsIsObjectNullOrUndefined(oController.m_aoWorkflows) === true) || (oController.m_aoWorkflows.length === 0) )
            // {
            //     oController.m_sSelectedWorkflowTab = 'WorkFlowTab2';
            // }
            // oController.m_bIsLoadingWorkflows = false;
        }).error(function (error) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET WORKFLOWS");
            // oController.m_bIsLoadingWorkflows = false;
            oController.m_bisLoadingWorkflows=false;

        });
    };

    /**
     *
     * @returns {boolean}
     */
    GetListOfWorkspacesController.prototype.openWorkflowManagerDialog = function() {
        var oController = this;
        // if(utilsIsObjectNullOrUndefined(this.m_oActiveWorkspace) === true)
        //     return false;
        var workspaceId;
        if (utilsIsObjectNullOrUndefined(oController.m_oActiveWorkspace) === true)
        {
            workspaceId = null;
        }
        else
        {
            workspaceId = oController.m_oActiveWorkspace.workspaceId;
        }
        oController.m_oModalService.showModal({
            templateUrl: "dialogs/workflow_manager/WorkFlowManagerView.html",
            controller: "WorkFlowManagerController",
            inputs: {
                extras: {
                    products:[],
                    workflowId:workspaceId,
                    defaultTab:'WorkFlowTab2'
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function (oResult) {

                oController.getWorkflowsByUser();
                // oController.m_oProcessesLaunchedService.loadProcessesFromServer(oController.m_oActiveWorkspace.workspaceId);
            });
        });

        return true;
    };

    GetListOfWorkspacesController.prototype.isEmptyWorkspaceList = function()
    {
        if( utilsIsObjectNullOrUndefined(this.m_aoWorkspaceList) === true || this.m_aoWorkspaceList.length <= 0 )
        {
            return true;
        }
        return false;
    };

    GetListOfWorkspacesController.$inject = [
        '$scope',
        'close',
        'WorkspaceService',
        'extras',
        'ConstantsService',
        'SnapOperationService',
        'ModalService'
    ];
    return GetListOfWorkspacesController;
})();
