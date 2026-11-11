document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("[data-course-form]");
    if (!form) return;
    const submit = form.querySelector("[data-course-submit]");
    const error = document.querySelector("[data-course-error]");
    const stage = document.querySelector("[data-roulette-stage]");
    const current = document.querySelector("[data-roulette-current]");
    const summary = document.querySelector("[data-course-summary]");
    const results = document.querySelector("[data-course-results]");
    const mapElement = document.getElementById("course-map");
    const disclaimer = document.querySelector("[data-course-map-disclaimer]");
    const savePanel = document.querySelector("[data-course-save-panel]");
    const loginPanel = document.querySelector("[data-course-login-panel]");
    const saveButton = document.querySelector("[data-course-save]");
    const saveTitle = document.querySelector("[data-course-save-title]");
    const saveStatus = document.querySelector("[data-course-save-status]");
    const range = form.querySelector("[data-course-randomness]");
    const rangeOutput = form.querySelector("[data-course-randomness-output]");
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
    let map;
    let running = false;
    let latestSaveToken = null;

    range.addEventListener("input", () => { rangeOutput.textContent = range.value; });
    form.addEventListener("submit", async event => {
        event.preventDefault();
        if (running) return;
        reset();
        const latitude = document.querySelector("[data-departure-latitude]")?.value;
        const longitude = document.querySelector("[data-departure-longitude]")?.value;
        if (!latitude || !longitude) {
            showError("먼저 페이지 위쪽에서 출발지를 검색하고 선택해주세요.");
            return;
        }
        running = true;
        form.setAttribute("aria-busy", "true");
        submit.disabled = true;
        submit.textContent = "코스를 만들고 있습니다…";
        const data = new FormData(form);
        const body = {
            originLatitude: Number(latitude),
            originLongitude: Number(longitude),
            categories: data.getAll("courseCategories"),
            district: data.get("courseDistrict") || null,
            availableMinutes: numberOrNull(data.get("courseMinutes")),
            budget: numberOrNull(data.get("courseBudget")),
            randomness: Number(data.get("courseRandomness")),
            placeCount: Number(data.get("placeCount")),
            randomSeed: numberOrNull(data.get("courseSeed"))
        };
        try {
            const token = form.querySelector("input[name='_csrf']")?.value;
            const headers = {"Content-Type": "application/json"};
            if (token) headers["X-CSRF-TOKEN"] = token;
            const response = await fetch("/api/courses/generate", {
                method: "POST", headers, body: JSON.stringify(body)
            });
            const payload = await response.json();
            if (!response.ok) throw new Error(payload.message || "코스를 생성하지 못했습니다.");
            await animate(payload.places);
            render(payload);
            submit.textContent = "🎡 다시 뽑기";
        } catch (exception) {
            showError(exception.message || "코스 생성 중 오류가 발생했습니다.");
            submit.textContent = "🎡 룰렛 시작";
        } finally {
            running = false;
            submit.disabled = false;
            form.removeAttribute("aria-busy");
        }
    });
    if (saveButton) saveButton.addEventListener("click", async () => {
        if (!latestSaveToken || !saveTitle.value.trim()) return;
        saveButton.disabled = true; saveStatus.textContent = "저장 중…";
        try {
            const token = form.querySelector("input[name='_csrf']")?.value;
            const headers = {"Content-Type": "application/json"};
            if (token) headers["X-CSRF-TOKEN"] = token;
            const response = await fetch("/api/courses", {method: "POST", headers,
                body: JSON.stringify({title: saveTitle.value.trim(), generationToken: latestSaveToken})});
            const payload = await response.json();
            if (!response.ok) throw new Error(payload.message || "코스를 저장하지 못했습니다.");
            latestSaveToken = null;
            saveStatus.replaceChildren();
            const link = document.createElement("a"); link.href = `/my/courses/${payload.id}`; link.textContent = "저장된 코스 보기";
            saveStatus.append("저장했습니다. ", link); saveButton.hidden = true;
        } catch (exception) { saveStatus.textContent = exception.message; }
        finally { saveButton.disabled = false; }
    });

    function numberOrNull(value) { return value === null || value === "" ? null : Number(value); }
    function reset() {
        error.hidden = true; summary.hidden = true; stage.hidden = true; disclaimer.hidden = true;
        results.replaceChildren(); mapElement.hidden = true;
        latestSaveToken = null;
        if (savePanel) savePanel.hidden = true;
        if (loginPanel) loginPanel.hidden = true;
        if (saveButton) saveButton.hidden = false;
        if (saveStatus) saveStatus.textContent = "";
        if (map) { map.remove(); map = null; }
    }
    function showError(message) {
        error.textContent = message;
        error.hidden = false;
        error.setAttribute("tabindex", "-1");
        error.focus();
    }
    async function animate(places) {
        if (!places.length) return;
        stage.hidden = false;
        if (reducedMotion.matches) {
            current.textContent = places.map(place => place.placeName).join(" → ");
            return;
        }
        for (const selected of places) {
            stage.classList.add("is-spinning");
            for (let frame = 0; frame < 12; frame++) {
                current.textContent = places[frame % places.length].placeName;
                await delay(45 + frame * 7);
            }
            current.textContent = `${selected.sequence}번 · ${selected.placeName}`;
            stage.classList.remove("is-spinning");
            await delay(260);
        }
    }
    function delay(milliseconds) { return new Promise(resolve => window.setTimeout(resolve, milliseconds)); }
    function render(payload) {
        summary.hidden = false;
        summary.textContent = `${payload.courseTitle} — ${payload.message}`;
        summary.setAttribute("tabindex", "-1");
        summary.focus();
        payload.places.forEach(place => results.append(courseCard(place)));
        latestSaveToken = payload.saveToken;
        if (savePanel && latestSaveToken) savePanel.hidden = false;
        if (loginPanel && payload.places.length) loginPanel.hidden = false;
        renderMap(payload);
    }
    function courseCard(place) {
        const item = document.createElement("li"); item.className = "course-card";
        const badge = document.createElement("span"); badge.className = "course-sequence"; badge.textContent = place.sequence;
        const imageFrame = document.createElement("div"); imageFrame.className = "result-place-image course-place-image";
        const image = document.createElement("img");
        image.src = place.imageUrl || "/images/place-placeholder.svg";
        image.alt = `${place.placeName} 대표 이미지`; image.loading = "lazy";
        image.addEventListener("error", () => {
            if (!image.src.endsWith("/images/place-placeholder.svg")) image.src = "/images/place-placeholder.svg";
        }, {once: true});
        imageFrame.append(image);
        const copy = document.createElement("div");
        const heading = document.createElement("h3"); heading.textContent = place.placeName;
        const meta = document.createElement("p"); meta.className = "place-meta";
        meta.textContent = `${place.category || "미분류"} · 이전 지점에서 직선 ${Math.round(place.distanceFromPreviousMeters)}m`;
        const score = document.createElement("strong");
        score.textContent = `${place.recommendationScore.toFixed(1)}점 · 교통 ${place.transitVerificationStatus}`;
        const reasons = document.createElement("ul");
        place.recommendationReasons.forEach(reason => { const li = document.createElement("li"); li.textContent = reason; reasons.append(li); });
        const link = document.createElement("a"); link.href = `/places/${place.placeId}`;
        link.className = "secondary-button"; link.textContent = "장소 상세 보기";
        copy.append(heading, meta, score, reasons, link); item.append(badge, imageFrame, copy); return item;
    }
    function renderMap(payload) {
        if (!payload.places.length) return;
        if (!window.L) {
            mapElement.hidden = false; disclaimer.hidden = false;
            mapElement.classList.add("map-empty");
            mapElement.textContent = "인터넷 연결이 없어 지도를 불러오지 못했습니다. 아래 방문 순서와 장소 카드를 이용해주세요.";
            return;
        }
        mapElement.hidden = false; disclaimer.hidden = false;
        map = L.map(mapElement);
        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
            maxZoom: 19, attribution: "&copy; OpenStreetMap contributors"
        }).addTo(map);
        const points = [[payload.origin.latitude, payload.origin.longitude]];
        L.circleMarker(points[0], {radius: 9, color: "#1565c0", fillOpacity: 1})
            .addTo(map).bindPopup("출발지");
        payload.places.forEach(place => {
            const point = [place.latitude, place.longitude]; points.push(point);
            const icon = L.divIcon({className: "course-number-marker", html: String(place.sequence), iconSize: [28, 28]});
            const marker = L.marker(point, {icon}).addTo(map);
            const link = document.createElement("a"); link.href = `/places/${place.placeId}`; link.textContent = place.placeName;
            marker.bindPopup(link);
        });
        L.polyline(points, {color: "#ef5350", weight: 3, opacity: .7, dashArray: "8 8"}).addTo(map);
        map.fitBounds(points, {padding: [30, 30], maxZoom: 14});
    }
});
