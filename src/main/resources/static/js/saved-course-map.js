/** 저장 코스의 출발지와 방문 순서를 Leaflet 마커로 표시한다. */
document.addEventListener("DOMContentLoaded", () => {
    const section = document.querySelector("[data-origin-lat]");
    const element = document.getElementById("saved-course-map");
    if (!section || !element) return;
    if (!window.L) {
        element.classList.add("map-empty");
        element.textContent = "인터넷 연결이 없어 지도를 불러오지 못했습니다. 저장된 방문 순서와 장소 정보를 이용해주세요.";
        return;
    }
    const origin = [Number(section.dataset.originLat), Number(section.dataset.originLon)];
    const points = [origin];
    const map = L.map(element);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {maxZoom: 19, attribution: "&copy; OpenStreetMap contributors"}).addTo(map);
    L.circleMarker(origin, {radius: 9, color: "#1565c0", fillOpacity: 1}).addTo(map).bindPopup(section.dataset.originName || "출발지");
    document.querySelectorAll("[data-course-place]").forEach(item => {
        const point = [Number(item.dataset.lat), Number(item.dataset.lon)]; points.push(point);
        const icon = L.divIcon({className: "course-number-marker", html: item.dataset.sequence, iconSize: [28, 28]});
        L.marker(point, {icon}).addTo(map).bindPopup(item.dataset.name);
    });
    L.polyline(points, {color: "#ef5350", weight: 3, opacity: .7, dashArray: "8 8"}).addTo(map);
    map.fitBounds(points, {padding: [30, 30], maxZoom: 14});
});
