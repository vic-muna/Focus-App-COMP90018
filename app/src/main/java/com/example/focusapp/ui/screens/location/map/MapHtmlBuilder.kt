package com.example.focusapp.ui.screens.location.map

internal fun buildMapHtml(centerLat: Double, centerLng: Double): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map {
                    width: 100%;
                    height: 100%;
                    margin: 0;
                    padding: 0;
                    background: #111827;
                }
                .leaflet-control-attribution { display: none !important; }
                .current-location-dot {
                    width: 16px;
                    height: 16px;
                    background-color: #3B82F6;
                    border: 3px solid #FFFFFF;
                    border-radius: 50%;
                    box-shadow: 0 0 10px rgba(59, 130, 246, 0.9);
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', { zoomControl: false }).setView([$centerLat, $centerLng], 16);
                
                L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}', {
                    maxZoom: 19,
                    attribution: 'Tiles &copy; Esri'
                }).addTo(map);

                var currentMarker = null;
                var pinMarker = null;
                var pinCircle = null;
                var hasCenteredOnUser = false;

                function setLocation(lat, lng) {
                    var latLng = [lat, lng];
                    if (!currentMarker) {
                        var icon = L.divIcon({
                            className: 'current-location-dot',
                            iconSize: [20, 20],
                            iconAnchor: [10, 10]
                        });
                        currentMarker = L.marker(latLng, { icon: icon }).addTo(map);
                    } else {
                        currentMarker.setLatLng(latLng);
                    }

                    if (!hasCenteredOnUser) {
                        map.setView(latLng, 16);
                        hasCenteredOnUser = true;
                    }
                }

                function setPin(lat, lng, radiusMeters) {
                    var latLng = [lat, lng];
                    if (!pinMarker) {
                        pinMarker = L.marker(latLng).addTo(map);
                    } else {
                        pinMarker.setLatLng(latLng);
                    }

                    if (radiusMeters && radiusMeters > 0) {
                        if (!pinCircle) {
                            pinCircle = L.circle(latLng, {
                                radius: radiusMeters,
                                color: '#6366F1',
                                fillColor: '#818CF8',
                                fillOpacity: 0.25,
                                weight: 2
                            }).addTo(map);
                        } else {
                            pinCircle.setLatLng(latLng);
                            pinCircle.setRadius(radiusMeters);
                        }
                    } else if (pinCircle) {
                        map.removeLayer(pinCircle);
                        pinCircle = null;
                    }
                }

                function removePin() {
                    if (pinMarker) {
                        map.removeLayer(pinMarker);
                        pinMarker = null;
                    }
                    if (pinCircle) {
                        map.removeLayer(pinCircle);
                        pinCircle = null;
                    }
                }

                map.on('click', function(e) {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onMapClick(e.latlng.lat, e.latlng.lng);
                    }
                });

                map.on('contextmenu', function(e) {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onMapClick(e.latlng.lat, e.latlng.lng);
                    }
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}
