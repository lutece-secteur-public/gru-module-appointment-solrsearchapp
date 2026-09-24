var map;
window.addEventListener('load', function () {
    L.NumberedDivIcon = L.Icon.extend({
        options: {
        // EDIT THIS TO POINT TO THE FILE AT http://www.charliecroom.com/marker_hole.png (or your own marker)
        iconUrl: L.Icon.Default.imagePath + '/marker_hole.png',
        number: '',
        shadowUrl: null,
        iconSize: new L.Point(25, 41),
            iconAnchor: new L.Point(13, 41),
            popupAnchor: new L.Point(0, -33),
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

    map = L.map(document.querySelector('#map .leaflet-appointment-search')).setView([48.85632, 2.33272], 12);
    var points = window.lutece_appointment_solrsearchapp_points;
    var freePlaces = window.lutece_appointment_solrsearchapp_freePlaces;

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        minZoom: 8,
        maxZoom: 16,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(map);

    var markers = new L.LayerGroup();

    for (var i = 0; i < points.length; i++) {

        var nbFreePlaces = freePlaces[points[i]["id"]]["free"];
        var nbTotalPlaces = freePlaces[points[i]["id"]]["total"];
		var ratioPlaces = nbFreePlaces / nbTotalPlaces ;
		var color = "";
		if ( ratioPlaces < 0.1 ) {
			color = "_red";
		} else if ( ratioPlaces < 0.2 ) {
			color = "_yellow";
		}
        var marker = L.marker( [
                points[i]["geojson"]["geometry"]["coordinates"][1],
                points[i]["geojson"]["geometry"]["coordinates"][0]
            ], {icon: new L.NumberedDivIcon({number: nbFreePlaces,  iconUrl: L.Icon.Default.imagePath + '/marker_hole' + color + '.png'})});

        var popupContent = "<p>Loading " + points[i]["type"] + " " + points[i]["id"] + " " + points[i]["code"] + "...</p>";
        marker.bindPopup(popupContent)
        marker.on('click', (function(point) {
            return function(e) {
                var properties = point["properties"];
                var popup = e.target.getPopup();
                var nId = point["id"].split("_")[1];
                var url = point["url_base"] + "rest/leaflet/popup/" + point["type"] + "/" + nId + "/" + point["code"];

                fetch(url)
                    .then(function(response) {
                        return response.text();
                    })
                    .then(function(data) {
                        var parser = new DOMParser();
                        var doc = parser.parseFromString(data, 'text/html');
                        var temp = doc.body.firstChild;

                        var firstSlot = document.getElementById("link_" + point["id"] + "_first_slot");

                        if (firstSlot) {
                            var slotLink = firstSlot.cloneNode(true);
                            slotLink.removeAttribute("id");
                            var paragraph = document.createElement("p");
                            paragraph.appendChild(slotLink);
                            temp.appendChild(paragraph);
                        }
                        var final_data = temp.outerHTML;
                        popup.setContent(final_data);
                        popup.update();
                    })
                    .catch(function() {
                        popup.setContent("<p>Resource unavailable</p><p>" + point["url_base"] + "</p>");
                        popup.update();
                    });
            };
        })(points[i]));

        markers.addLayer(marker);
    }

    map.addLayer(markers);

});
