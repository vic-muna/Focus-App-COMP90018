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
                .zone-tooltip {
                    background: #1F2937;
                    color: #10B981;
                    border: 1px solid #10B981;
                    border-radius: 4px;
                    font-weight: bold;
                    font-size: 11px;
                    padding: 2px 6px;
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
                var zoneLayers = [];

                function setViewUpperQuarter(latLng) {
                    map.setView(latLng, 16, { animate: false });
                    var height = map.getSize().y;
                    if (height > 0) {
                        map.panBy([0, height * 0.25], { animate: false });
                    } else {
                        setTimeout(function() {
                            var h = map.getSize().y;
                            if (h > 0) {
                                map.panBy([0, h * 0.25], { animate: false });
                            }
                        }, 100);
                    }
                }

                map.whenReady(function() {
                    setViewUpperQuarter([$centerLat, $centerLng]);
                });

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
                        setViewUpperQuarter(latLng);
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
                    map.setView(latLng, 16);

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

                function setZones(zones) {
                    zoneLayers.forEach(function(l) { map.removeLayer(l); });
                    zoneLayers = [];

                    if (!zones || !Array.isArray(zones)) return;
                    zones.forEach(function(z) {
                        var latLng = [z.latitude, z.longitude];
                        var circle = L.circle(latLng, {
                            radius: z.radiusMeters,
                            color: '#10B981',
                            fillColor: '#34D399',
                            fillOpacity: 0.2,
                            weight: 2
                        }).addTo(map);
                        zoneLayers.push(circle);

                        var marker = L.circleMarker(latLng, {
                            radius: 6,
                            color: '#10B981',
                            fillColor: '#FFFFFF',
                            weight: 2,
                            fillOpacity: 1
                        }).addTo(map);

                        if (z.name) {
                            marker.bindTooltip(z.name, {
                                permanent: true,
                                direction: 'top',
                                className: 'zone-tooltip'
                            });
                        }

                        marker.on('click', function() {
                            if (window.AndroidBridge && window.AndroidBridge.onZoneClick) {
                                window.AndroidBridge.onZoneClick(z.id);
                            }
                        });

                        zoneLayers.push(marker);
                    });
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
