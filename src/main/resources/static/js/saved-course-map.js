/** 저장 코스의 출발지와 방문 순서를 Leaflet 지도에 표시합니다. */
document.addEventListener("DOMContentLoaded", () => {
    const section = document.querySelector(".course-detail-map");
    const element = document.getElementById("saved-course-map");
    if (!section || !element) return;

    const showFallback = message => {
        element.classList.add("map-empty");
        element.textContent = message;
    };
    if (!window.L) {
        showFallback("인터넷 연결이 없어 지도를 불러오지 못했습니다. 저장된 방문 순서와 장소 정보를 이용해주세요.");
        return;
    }

    const originName = section.dataset.originName;
    const origin = originName && originName !== "출발지 미지정"
        ? readCoordinates(section.dataset.originLat, section.dataset.originLon)
        : null;
    const places = Array.from(section.querySelectorAll("[data-course-place]"), item => ({
        point: readCoordinates(item.dataset.lat, item.dataset.lon),
        name: item.dataset.name,
        sequence: item.dataset.sequence
    })).filter(place => place.point);
    const points = places.map(place => place.point);
    if (origin) points.unshift(origin);

    if (points.length === 0) {
        showFallback("지도에 표시할 위치 정보가 없습니다. 저장된 방문 순서와 장소 정보를 이용해주세요.");
        return;
    }

    const map = L.map(element);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution: "&copy; OpenStreetMap contributors"
    }).addTo(map);

    if (origin) {
        L.circleMarker(origin, {radius: 9, color: "#1565c0", fillOpacity: 1})
            .addTo(map)
            .bindPopup(mapText(originName));
    }

    for (const place of places) {
        const icon = L.divIcon({
            className: "course-number-marker",
            html: mapText(place.sequence),
            iconSize: [28, 28]
        });
        L.marker(place.point, {icon}).addTo(map).bindPopup(mapText(place.name));
    }

    // 점선은 방문 순서를 보여 주는 직선이며 실제 이동 경로가 아닙니다.
    if (points.length > 1) {
        L.polyline(points, {color: "#ef5350", weight: 3, opacity: 0.7, dashArray: "8 8"}).addTo(map);
    }
    map.fitBounds(points, {padding: [30, 30], maxZoom: 14});
});

function readCoordinates(latitude, longitude) {
    if (!latitude?.trim() || !longitude?.trim()) return null;
    const lat = Number(latitude);
    const lon = Number(longitude);
    return Number.isFinite(lat) && Number.isFinite(lon)
        && Math.abs(lat) <= 90 && Math.abs(lon) <= 180
        ? [lat, lon]
        : null;
}

// Leaflet에 전달할 요소의 내용은 텍스트로 설정합니다.
function mapText(value) {
    const label = document.createElement("span");
    label.textContent = value ?? "";
    return label;
}
