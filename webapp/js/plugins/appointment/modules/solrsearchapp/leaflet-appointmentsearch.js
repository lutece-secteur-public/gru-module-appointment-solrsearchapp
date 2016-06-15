$(window).load(function () {
    L.NumberedDivIcon = L.Icon.extend({
        options: {
        // EDIT THIS TO POINT TO THE FILE AT http://www.charliecroom.com/marker_hole.png (or your own marker)
        iconUrl: L.Icon.Default.imagePath + '/marker_hole.png',
        number: '',
        shadowUrl: null,
        iconSize: new L.Point(25, 41),
            iconAnchor: new L.Point(13, 41),
            popupAnchor: new L.Point(0, -33),
            /*
            iconAnchor: (Point)
            popupAnchor: (Point)
            */
            className: 'leaflet-div-icon'
        },

        createIcon: function () {
            var div = document.createElement('div');
            var img = this._createImg(this.options['iconUrl']);
            var numdiv = document.createElement('div');
            numdiv.setAttribute ( "class", "number" );
            numdiv.innerHTML = this.options['number'] || '';
            div.appendChild ( img );
            div.appendChild ( numdiv );
            this._setIconStyles(div, 'icon');
            return div;
        },

        //you could change this to add a shadow like in the normal marker if you really wanted
        createShadow: function () {
            return null;
        }
    });

    var map = L.map('map').setView([48.85632, 2.33272], 12);
    var points = window.lutece_appointment_solrsearchapp_points;
    var freePlaces = window.lutece_appointment_solrsearchapp_freePlaces;

    // create the tile layer with correct attribution
    var esri_streets = L.esri.basemapLayer('Streets').addTo(map);
    var osmUrl='http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
    var osmAttrib='Map data © <a href="http://openstreetmap.org">OpenStreetMap</a> contributors';
    var osm = new L.TileLayer(osmUrl, {minZoom: 8, maxZoom: 16, attribution: osmAttrib});

    var markers = new L.MarkerClusterGroup();

    for (var i = 0; i < points.length; i++) {

        var nbFreePlaces = freePlaces[points[i]["id"]];
        var marker = L.marker( [
                points[i]["geojson"]["geometry"]["coordinates"][1],
                points[i]["geojson"]["geometry"]["coordinates"][0]
            ], {icon: new L.NumberedDivIcon({number: nbFreePlaces})});

        var popupContent = "<p>Loading " + points[i]["type"] + " " + points[i]["id"] + " " + points[i]["code"] + "...</p>";
        marker.bindPopup(popupContent)
        marker.on('click', (function(point) {
            return function(e) {
                var properties = point["properties"];
                var popup = e.target.getPopup();
                var nId = point["id"].split("_")[1];
                var url = point["url_base"] + "rest/leaflet/popup/" + point["type"] + "/" + nId + "/" + point["code"];

                $.get(url).done(function(data) {
                    popup.setContent(data);
                    popup.update();
                }).fail(function() {
                    map.closePopup();
                });
            };
        })(points[i]));

        markers.addLayer(marker);
    }

    map.addLayer(markers);

  var baseMaps = {
      "Esri Streets": esri_streets,
      "OpenStreetMap": osm
  };
  var overlayMaps = { "rendez-vous": markers };
  // paramétrage et ajout du L.control.layers à la carte
  L.control.layers(baseMaps, overlayMaps).addTo(map);
});
