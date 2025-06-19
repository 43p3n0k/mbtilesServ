$(document).ready(function(e) {
    jQuery.support.cors = true;
    window.onresize = adjustMapSize;

    Map = createMap('map');
    adjustMapSize();
    setView();
});

function createMap(divId) {
    let osmAttr = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';

    let osm = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: osmAttr
    });
	
    let host = location.protocol + "//" + location.hostname + ":3650";
    let maptiler = L.tileLayer(host + '/api/maps/europe-liechtenstein/256/{z}/{x}/{y}@2x.png', {
        attribution: '<a href="' + host + '/admin/login">MapTiler Server</a>'
    });

    let mbtiles = L.tileLayer(location.origin + '/mvt/{z}/{x}/{y}.png', {
        attribution: '<a target="_blank" rel="noopener noreferrer" href="https://github.com/43p3n0k/mbtilesServ">github</a>'
    });

    let map = L.map(divId, {
        layers: [mbtiles]
    });

    L.control.layers({
        "OpenStreetMap": osm,
        "Self-hosted MVT": mbtiles
    }).addTo(map);

    return map;
}

function adjustMapSize() {
    $("#map").height($(window).height()).width($(window).width());
    Map.invalidateSize(false);
}

function setView() {
    let req = location.origin + '/getCenterZoom?wndWidth=' + $(window).width();
    doRequest(req)
        .then(function(res) {
            Map.setView(...eval(res));
        })
        .catch(function(text) {
            alert(text);
        });
}

function doRequest(req) {
    return new Promise(function(resolve, reject) {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', req, true);
        xhr.onreadystatechange = function() {
            if (xhr.readyState !== XMLHttpRequest.DONE) return;
            if (xhr.status !== 200) {
                reject("HttpRequest: " + xhr.status + ': ' + xhr.statusText);
            } else {
                resolve(xhr.response);
            }
        };
        xhr.send();
    });
}