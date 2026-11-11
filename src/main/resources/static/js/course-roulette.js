/**
 * 두 코스 기능은 같은 서버 API를 사용하지만 입력과 최근 추천 기록은 서로 독립적으로 관리한다.
 * 룰렛 애니메이션은 서버가 확정한 결과를 보여주기만 하며 장소를 다시 선택하지 않는다.
 */
document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-course-feature]").forEach(initializeCourseFeature);
});

function initializeCourseFeature(feature) {
    const form = feature.querySelector("[data-course-form]");
    if (!form) return;
    const mode = form.dataset.courseMode || "PERSONALIZED";
    const submit = form.querySelector("[data-course-submit]");
    const error = feature.querySelector("[data-course-error]");
    const stage = feature.querySelector("[data-roulette-stage]");
    const current = feature.querySelector("[data-roulette-current]");
    const summary = feature.querySelector("[data-course-summary]");
    const results = feature.querySelector("[data-course-results]");
    const mapElement = feature.querySelector("[data-course-map]");
    const disclaimer = feature.querySelector("[data-course-map-disclaimer]");
    const savePanel = feature.querySelector("[data-course-save-panel]");
    const loginPanel = feature.querySelector("[data-course-login-panel]");
    const saveButton = feature.querySelector("[data-course-save]");
    const saveTitle = feature.querySelector("[data-course-save-title]");
    const saveStatus = feature.querySelector("[data-course-save-status]");
    const range = form.querySelector("[data-course-randomness]");
    const rangeOutput = form.querySelector("[data-course-randomness-output]");
    const historyKey = "cityRoulette.recentInstantPlaces";
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
    let map;
    let running = false;
    let latestSaveToken = null;

    if (range && rangeOutput) range.addEventListener("input", () => { rangeOutput.textContent = range.value; });
    feature.querySelector("[data-reset-history]")?.addEventListener("click", event => {
        localStorage.removeItem(historyKey);
        event.currentTarget.textContent = "추천 기록을 초기화했습니다";
        window.setTimeout(() => { event.currentTarget.textContent = "추천 기록 초기화"; }, 1600);
    });

    form.addEventListener("submit", async event => {
        event.preventDefault();
        if (running) return;
        reset();
        const latitude = document.querySelector("[data-departure-latitude]")?.value;
        const longitude = document.querySelector("[data-departure-longitude]")?.value;
        if (mode === "PERSONALIZED" && (!latitude || !longitude)) {
            showError("맞춤 코스를 만들려면 위의 출발지를 검색하거나 현재 위치를 선택해주세요.");
            return;
        }
        running = true;
        form.setAttribute("aria-busy", "true");
        submit.disabled = true;
        submit.textContent = "코스를 만들고 있습니다…";
        const data = new FormData(form);
        const seed = numberOrNull(data.get("randomSeed"));
        const body = {
            originLatitude: latitude ? Number(latitude) : null,
            originLongitude: longitude ? Number(longitude) : null,
            categories: data.getAll("categories"),
            district: data.get("district") || null,
            availableMinutes: numberOrNull(data.get("availableMinutes")),
            budget: numberOrNull(data.get("budget")),
            randomness: mode === "INSTANT" ? 100 : Number(data.get("randomness")),
            placeCount: Number(data.get("placeCount")),
            randomSeed: seed,
            categoryMode: data.get("categoryMode") || "PREFERRED",
            generationMode: mode,
            excludedPlaceIds: mode === "INSTANT" && seed === null ? recentIds(historyKey) : []
        };
        try {
            const headers = {"Content-Type": "application/json"};
            const csrf = form.querySelector("input[name='_csrf']")?.value;
            if (csrf) headers["X-CSRF-TOKEN"] = csrf;
            const response = await fetch("/api/courses/generate", {method: "POST", headers, body: JSON.stringify(body)});
            const payload = await response.json();
            if (!response.ok) throw new Error(payload.message || "코스를 생성하지 못했습니다.");
            await animate(payload.places);
            render(payload);
            if (mode === "INSTANT" && seed === null) remember(payload.places, historyKey);
            submit.textContent = mode === "INSTANT" ? "🎡 다시 뽑기" : "🧭 조건으로 다시 만들기";
        } catch (exception) {
            showError(exception.message || "코스 생성 중 오류가 발생했습니다.");
            submit.textContent = mode === "INSTANT" ? "🎡 바로 뽑기" : "🧭 맞춤 코스 만들기";
        } finally {
            running = false; submit.disabled = false; form.removeAttribute("aria-busy");
        }
    });

    saveButton?.addEventListener("click", async () => {
        if (!latestSaveToken || !saveTitle?.value.trim()) return;
        saveButton.disabled = true; saveStatus.textContent = "저장 중…";
        try {
            const headers = {"Content-Type": "application/json"};
            const csrf = form.querySelector("input[name='_csrf']")?.value;
            if (csrf) headers["X-CSRF-TOKEN"] = csrf;
            const response = await fetch("/api/courses", {method: "POST", headers,
                body: JSON.stringify({title: saveTitle.value.trim(), generationToken: latestSaveToken})});
            const payload = await response.json();
            if (!response.ok) throw new Error(payload.message || "코스를 저장하지 못했습니다.");
            latestSaveToken = null; saveStatus.replaceChildren();
            const link = document.createElement("a"); link.href = `/my/courses/${payload.id}`; link.textContent = "저장된 코스 보기";
            saveStatus.append("저장했습니다. ", link); saveButton.hidden = true;
        } catch (exception) { saveStatus.textContent = exception.message; }
        finally { saveButton.disabled = false; }
    });

    function numberOrNull(value) { return value === null || value === "" ? null : Number(value); }
    function recentIds(key) {
        try { return JSON.parse(localStorage.getItem(key) || "[]").filter(Number.isFinite).slice(-30); }
        catch (_) { return []; }
    }
    function remember(places, key) {
        const combined = [...recentIds(key), ...places.map(place => place.placeId)];
        localStorage.setItem(key, JSON.stringify([...new Set(combined)].slice(-30)));
    }
    function reset() {
        error.hidden = true; summary.hidden = true; stage.hidden = true; disclaimer.hidden = true;
        results.replaceChildren(); mapElement.hidden = true; mapElement.classList.remove("map-empty"); mapElement.textContent = "";
        latestSaveToken = null;
        if (savePanel) savePanel.hidden = true; if (loginPanel) loginPanel.hidden = true;
        if (saveButton) saveButton.hidden = false; if (saveStatus) saveStatus.textContent = "";
        if (map) { map.remove(); map = null; }
    }
    function showError(message) { error.textContent = message; error.hidden = false; error.tabIndex = -1; error.focus(); }
    async function animate(places) {
        if (!places.length) return;
        stage.hidden = false;
        if (reducedMotion.matches) { current.textContent = places.map(place => place.placeName).join(" → "); return; }
        for (const selected of places) {
            stage.classList.add("is-spinning");
            for (let frame = 0; frame < 10; frame++) {
                current.textContent = places[frame % places.length].placeName;
                await new Promise(resolve => window.setTimeout(resolve, 45 + frame * 7));
            }
            current.textContent = `${selected.sequence}번 · ${selected.placeName}`;
            stage.classList.remove("is-spinning");
            await new Promise(resolve => window.setTimeout(resolve, 220));
        }
    }
    function render(payload) {
        summary.hidden = false;
        const shortage = payload.insufficientCandidates ? " 요청 수보다 후보가 적어 가능한 장소만 표시했습니다." : "";
        summary.replaceChildren();
        const heading = document.createElement("strong");
        heading.textContent = `${payload.courseTitle} — ${payload.message}${shortage}`;
        summary.append(heading);
        if (payload.dataLimitations?.length) {
            const limitations = document.createElement("ul");
            limitations.className = "course-limitations";
            payload.dataLimitations.forEach(message => {
                const item = document.createElement("li"); item.textContent = message; limitations.append(item);
            });
            summary.append(limitations);
        }
        summary.tabIndex = -1; summary.focus();
        payload.places.forEach(place => results.append(courseCard(place)));
        latestSaveToken = payload.saveToken;
        if (savePanel && latestSaveToken) savePanel.hidden = false;
        if (loginPanel && payload.places.length) loginPanel.hidden = false;
        renderMap(payload);
    }
    function transitLabel(status) {
        return {VERIFIED: "교통 연결 확인", PARTIAL_DATA: "교통 정보 일부 확인",
            UNVERIFIED: "교통 정보 확인 필요", NOT_EVALUATED: "교통 정보 확인 필요"}[status] || "교통 정보 확인 필요";
    }
    function courseCard(place) {
        const item = document.createElement("li"); item.className = "course-card";
        const badge = document.createElement("span"); badge.className = "course-sequence"; badge.textContent = place.sequence;
        const frame = document.createElement("div"); frame.className = "result-place-image course-place-image";
        const image = document.createElement("img"); image.src = place.imageUrl || "/images/place-placeholder.svg";
        image.alt = `${place.placeName} 대표 이미지`; image.loading = "lazy";
        image.addEventListener("error", () => { image.src = "/images/place-placeholder.svg"; }, {once: true}); frame.append(image);
        const copy = document.createElement("div"); const heading = document.createElement("h3"); heading.textContent = place.placeName;
        const meta = document.createElement("p"); meta.className = "place-meta";
        meta.textContent = `${place.category || "미분류"} · 이전 지점에서 직선 ${Math.round(place.distanceFromPreviousMeters)}m`;
        const address = document.createElement("p"); address.className = "field-help";
        address.textContent = place.address || place.district || "위치 상세 정보 없음";
        const score = document.createElement("strong"); score.textContent = `${place.recommendationScore.toFixed(1)}점 · ${transitLabel(place.transitVerificationStatus)}`;
        const reasons = document.createElement("ul");
        (place.recommendationReasons || []).forEach(reason => { const li = document.createElement("li"); li.textContent = reason; reasons.append(li); });
        const link = document.createElement("a"); link.href = `/places/${place.placeId}`; link.className = "secondary-button"; link.textContent = "장소 상세 보기";
        copy.append(heading, meta, address, score, reasons, link); item.append(badge, frame, copy); return item;
    }
    function renderMap(payload) {
        if (!payload.places.length) return;
        mapElement.hidden = false; disclaimer.hidden = false;
        if (!window.L) { mapElement.classList.add("map-empty"); mapElement.textContent = "지도를 불러오지 못했습니다. 아래 장소 카드에서 방문 순서를 확인해주세요."; return; }
        map = L.map(mapElement); L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {maxZoom: 19, attribution: "&copy; OpenStreetMap contributors"}).addTo(map);
        const points = [[payload.origin.latitude, payload.origin.longitude]];
        L.circleMarker(points[0], {radius: 9, color: "#1565c0", fillOpacity: 1}).addTo(map).bindPopup("출발지");
        payload.places.forEach(place => {
            const point = [place.latitude, place.longitude]; points.push(point);
            const icon = L.divIcon({className: "course-number-marker", html: String(place.sequence), iconSize: [28, 28]});
            const marker = L.marker(point, {icon}).addTo(map); const link = document.createElement("a");
            link.href = `/places/${place.placeId}`; link.textContent = place.placeName; marker.bindPopup(link);
        });
        L.polyline(points, {color: "#ef5350", weight: 3, opacity: .7, dashArray: "8 8"}).addTo(map);
        map.fitBounds(points, {padding: [30, 30], maxZoom: 14});
    }
}
