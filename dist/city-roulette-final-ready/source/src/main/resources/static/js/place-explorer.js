/** 장소 이미지 대체 처리와 로그인 회원의 즐겨찾기 추가·해제를 담당한다. */
// 공공데이터 이미지가 만료되거나 표시 정책에서 제외되면 공통 대체 이미지를 사용한다.
document.querySelectorAll("[data-image-fallback]").forEach((image) => {
    image.addEventListener("error", () => {
        image.src = "/images/place-placeholder.svg";
        image.removeAttribute("data-image-fallback");
    });
});

// 즐겨찾기 상태는 서버를 기준으로 변경하고 성공한 뒤에만 버튼 UI를 갱신한다.
document.querySelectorAll("[data-favorite-button]").forEach((button) => {
    button.addEventListener("click", async () => {
        if (button.disabled) {
            return;
        }
        const placeId = Number(button.dataset.placeId);
        const favorite = button.getAttribute("aria-pressed") === "true";
        const feedback = button
            .closest(".place-card-body, .detail-copy")
            ?.querySelector("[data-favorite-feedback]");
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
        button.disabled = true;
        if (feedback) {
            feedback.textContent = "처리 중입니다…";
        }
        try {
            const headers = {};
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }
            if (!favorite) {
                headers["Content-Type"] = "application/json";
            }
            const response = await fetch(favorite ? `/api/favorites/${placeId}` : "/api/favorites", {
                method: favorite ? "DELETE" : "POST",
                headers,
                body: favorite ? undefined : JSON.stringify({ placeId })
            });
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            const nowFavorite = !favorite;
            button.setAttribute("aria-pressed", String(nowFavorite));
            button.classList.toggle("is-favorite", nowFavorite);
            button.textContent = nowFavorite ? "★ 저장됨" : "☆ 즐겨찾기";
            if (feedback) {
                feedback.textContent = nowFavorite
                    ? "즐겨찾기에 저장했습니다."
                    : "즐겨찾기에서 해제했습니다.";
            }
            if (!nowFavorite && button.dataset.removeCard === "true") {
                const card = button.closest("[data-favorite-card]");
                if (card) {
                    card.remove();
                }
            }
        } catch (error) {
            if (feedback) {
                feedback.textContent = "요청을 처리하지 못했습니다. 잠시 후 다시 시도해주세요.";
            }
        } finally {
            button.disabled = false;
        }
    });
});

// 상세 화면의 장소와 주변 정류장을 서로 다른 마커로 표시한다.
document.querySelectorAll("[data-place-map]").forEach((container) => {
    const status = container.querySelector("[data-map-status]");
    const latitude = Number(container.dataset.placeLat);
    const longitude = Number(container.dataset.placeLon);
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
        status.textContent = "표시할 좌표가 없습니다.";
        return;
    }
    if (!window.L) {
        status.textContent = "지도 라이브러리를 불러오지 못했습니다. 아래 정류장 목록을 이용해주세요.";
        return;
    }
    status.remove();
    const map = L.map(container, { scrollWheelZoom: false }).setView([latitude, longitude], 15);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);
    const placeIcon = L.divIcon({ className: "map-pin map-pin-place", html: "<span aria-hidden='true'>★</span>", iconSize: [36, 36], iconAnchor: [18, 18] });
    const stopIcon = L.divIcon({ className: "map-pin map-pin-stop", html: "<span aria-hidden='true'>B</span>", iconSize: [30, 30], iconAnchor: [15, 15] });
    const bounds = L.latLngBounds([[latitude, longitude]]);
    const placePopup = document.createElement("div");
    const placeName = document.createElement("strong");
    placeName.textContent = container.dataset.placeName;
    const placeLink = document.createElement("a");
    placeLink.href = container.dataset.placeUrl;
    placeLink.textContent = "상세 정보";
    placePopup.append(placeName, document.createElement("br"), placeLink);
    L.marker([latitude, longitude], { icon: placeIcon, title: container.dataset.placeName, alt: container.dataset.placeName + " 위치" }).addTo(map).bindPopup(placePopup).openPopup();
    document.querySelectorAll("[data-stop-marker]").forEach((marker) => {
        const lat = Number(marker.dataset.lat);
        const lon = Number(marker.dataset.lon);
        if (!Number.isFinite(lat) || !Number.isFinite(lon)) {
            return;
        }
        const popup = document.createElement("div");
        const name = document.createElement("strong");
        name.textContent = marker.dataset.name;
        const ars = document.createElement("span");
        ars.textContent = marker.dataset.ars ? "ARS " + marker.dataset.ars : "ARS 번호 없음";
        popup.append(name, document.createElement("br"), ars);
        L.marker([lat, lon], { icon: stopIcon, title: marker.dataset.name, alt: marker.dataset.name + " 정류장" }).addTo(map).bindPopup(popup);
        bounds.extend([lat, lon]);
    });
    if (bounds.isValid()
        && bounds.getNorthEast().distanceTo(bounds.getSouthWest()) > 0) {
        map.fitBounds(bounds.pad(0.16), {maxZoom: 16});
    }
});
