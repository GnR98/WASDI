/**
 * Created by m.menapace on 9/02/2021.
 */


var WorkspaceDetailsController = (function () {

    function WorkspaceDetailsController($scope, oExtras, oWorkspaceService) {

        /**
         * Angular Scope
         */
        this.m_oScope = $scope;
        /**
         * Reference to this controller
         */
        this.m_oScope.m_oController = this;
        /**
         * import the extras
         */
        this.m_oExtras = oExtras;
        /**
         * workspace id
         */
        this.m_workspaceId = this.m_oExtras.WorkSpaceId;

        /**
         * workspace view model
         */
        this.m_oWorkspaceViewModel = this.m_oExtras.WorkSpaceViewModel;
        /**
         * count of the products in the current workspace
         */
        this.m_oCountProduct = this.m_oExtras.ProductCount;

        // the workspace id passed through extras to the modal
        // then, if ok, call the other methods from angular starting from here
        // get WS viewmodel, date and co [...]

        // This controller must implement the logic to change node
        // all the other static parameters are passed to the model via extras
        // (WorkspaceViewModel,

    } // end constructor

    WorkspaceDetailsController.prototype.getLastTouchDate = function () {

                if (this.m_oWorkspaceViewModel === null) {
            return "";
        } else {
            return new Date(this.m_oWorkspaceViewModel.lastEditDate).toString().replace("\"", "");
        }
    }

    WorkspaceDetailsController.$inject = [
        '$scope',
        'extras'
    ];
    return WorkspaceDetailsController;
})();
