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
            numdiv.textContent = this.options['number'] || '';
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
    var points = readPoints();

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        minZoom: 8,
        maxZoom: 16,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(map);

    var markers = new L.LayerGroup();

    for (var i = 0; i < points.length; i++) {

        var nbFreePlaces = points[i]["free"];
        var nbTotalPlaces = points[i]["total"];
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

        var popupContent = textParagraph("Loading " + points[i]["type"] + " " + points[i]["id"] + " " + points[i]["code"] + "...");
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
                        popup.setContent(temp);
                        popup.update();
                    })
                    .catch(function() {
                        var content = document.createElement("div");
                        content.appendChild(textParagraph("Resource unavailable"));
                        content.appendChild(textParagraph(point["url_base"]));
                        popup.setContent(content);
                        popup.update();
                    });
            };
        })(points[i]));

        markers.addLayer(marker);
    }

    map.addLayer(markers);

    // The points are written by search.html in the data attributes of #appointment-search-points
    function readPoints() {
        var list = [];
        var container = document.getElementById("appointment-search-points");
        if (!container) {
            return list;
        }
        container.querySelectorAll("span[data-id]").forEach(function (element) {
            try {
                list.push({
                    "type": "appointment",
                    "code": "appointment",
                    "url_base": element.dataset.urlBase,
                    "id": element.dataset.id,
                    "geojson": JSON.parse(element.dataset.geojson),
                    "free": Number(element.dataset.free),
                    "total": Number(element.dataset.total)
                });
            } catch (e) {
                // a point whose geojson cannot be read is not shown
            }
        });
        return list;
    }

    function textParagraph(text) {
        var paragraph = document.createElement("p");
        paragraph.textContent = text;
        return paragraph;
    }

});
