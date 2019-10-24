 var eDriftFloodEventDialogController = (function() {

    function eDriftFloodEventDialogController($scope, oClose,oExtras,oAuthService,oConstantsService,oCatalogService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oAuthService = oAuthService;
        this.m_oCatalogService = oCatalogService;
        this.m_oConstantsService = oConstantsService;
        this.m_oBoundingBox = {
            northEast: "",
            southWest: ""
        }
        this.m_oSelectedDate = moment();
        var oController = this;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

        setTimeout(function() {
            oController.initMap("mappaedriftfloodevent");
        }, 500);
    }
    /*************** METHODS ***************/
    eDriftFloodEventDialogController.prototype.getBoundingBox = function(){
        return this.m_oBoundingBox;
      // alert("North East " + this.m_oBoundingBox.northEast + " South West " + this.m_oBoundingBox.southWest);
    };
    eDriftFloodEventDialogController.prototype.getDate = function(){
      return this.m_oSelectedDate;
    };

    eDriftFloodEventDialogController.prototype.initMap = function(sMapDiv) {

        /*  it need disabled keyboard, there'is a bug :
        *   https://github.com/Leaflet/Leaflet/issues/1228
        *   thw window scroll vertically when i click (only if the browser window are smaller)
        *   alternative solution (hack):
        *   L.Map.addInitHook(function() {
        *   return L.DomEvent.off(this._container, "mousedown", this.keyboard._onMouseDown);
        *   });
        */

        oOSMBasic = L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution:
                '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors',
            maxZoom: 18,
            // this map option disables world wrapping. by default, it is false.
            continuousWorld: false,
            // this option disables loading tiles outside of the world bounds.
            noWrap: true
        });


        var oMap = L.map(sMapDiv, {
            zoomControl: false,
            layers: [oOSMBasic],
            keyboard: false
            //maxZoom: 22
        });

        // coordinates in map find this plugin in lib folder
        L.control.mousePosition().addTo(oMap);

        //scale control
        L.control.scale({
            position: "bottomright",
            imperial: false
        }).addTo(oMap);


        // center map
        var southWest = L.latLng(0, 0),
            northEast = L.latLng(0, 0),
            oBoundaries = L.latLngBounds(southWest, northEast);

        oMap.fitBounds(oBoundaries);
        oMap.setZoom(3);

        //add draw.search (opensearch)
        var drawnItems = new L.FeatureGroup();
        //this.m_oDrawItems = drawnItems;//save draw items (used in delete shape)
        oMap.addLayer(drawnItems);



        var oOptions={
            position:'topright',//position of menu
            draw:{// what kind of shapes are disable/enable
                marker:false,
                polyline:false,
                circle:false,
                polygon:false,
            },

            edit: {
                featureGroup: drawnItems,//draw items are the "voice" of menu
                edit: false,// hide edit button
                remove: true// hide remove button
            }
        };

        var oDrawControl = new L.Control.Draw(oOptions);

        oMap.addControl(oDrawControl);
        //Without this.m_oWasdiMap.on() the shape isn't saved on map
        var oController = this;
        oMap.on(L.Draw.Event.CREATED, function (event)
        {
            var layer = event.layer;
            oController.m_oBoundingBox.northEast = layer._bounds._northEast;
            oController.m_oBoundingBox.southWest = layer._bounds._southWest;
            //remove old shape
            if(drawnItems && drawnItems.getLayers().length!==0){
                drawnItems.clearLayers();
            }
            //save new shape in map
            drawnItems.addLayer(layer);
        });

        oMap.on(L.Draw.Event.DELETESTOP, function (event) {
            var layer = event.layers;
        });

        return oMap;

    };

    eDriftFloodEventDialogController.$inject = [
        '$scope',
        'close',
        'extras',
        'AuthService',
        'ConstantsService',
        'CatalogService'
    ];
    return eDriftFloodEventDialogController;
})();