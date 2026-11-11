document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("[data-personalized-recommend-form]");
    if (!form) return;
    const results = document.querySelector("[data-personalized-results]");
    const summary = document.querySelector("[data-personalized-summary]");
    const error = document.querySelector("[data-personalized-error]");
    const loading = document.querySelector("[data-personalized-loading]");
    const submit = form.querySelector("[data-personalized-submit]");
    const range = form.querySelector("[data-randomness]");
    const output = form.querySelector("[data-randomness-output]");
    const mapElement = document.getElementById("recommendation-map");
    let map;

    range.addEventListener("input", () => { output.textContent = range.value; });
    form.addEventListener("submit", async event => {
        event.preventDefault();
        clearResults();
        const latitude = document.querySelector("[data-departure-latitude]")?.value;
        const longitude = document.querySelector("[data-departure-longitude]")?.value;
        if (!latitude || !longitude) {
            showError("먼저 위의 출발지 검색 결과에서 한 곳을 선택해주세요.");
            return;
        }
        const data = new FormData(form);
        const body = {
            originLatitude: Number(latitude),
            originLongitude: Number(longitude),
            categories: data.getAll("categories"),
            district: data.get("district") || null,
            availableMinutes: numberOrNull(data.get("availableMinutes")),
            budget: numberOrNull(data.get("budget")),
            randomness: Number(data.get("randomness")),
            limit: Number(data.get("personalizedLimit"))
        };
        loading.hidden = false;
        form.setAttribute("aria-busy", "true");
        submit.disabled = true;
        submit.textContent = "추천 후보를 찾는 중…";
        try {
            const token = form.querySelector("input[name='_csrf']")?.value;
            const headers = {"Content-Type": "application/json"};
            if (token) headers["X-CSRF-TOKEN"] = token;
            const response = await fetch("/api/recommendations", {
                method: "POST", headers, body: JSON.stringify(body)
            });
            const payload = await response.json();
            if (!response.ok) throw new Error(payload.message || "추천 요청을 처리하지 못했습니다.");
            render(payload);
        } catch (exception) {
            showError(exception.message || "추천 요청 중 오류가 발생했습니다.");
        } finally {
            loading.hidden = true;
            form.removeAttribute("aria-busy");
            submit.disabled = false;
            submit.textContent = "조건 기반 추천 받기";
        }
    });

    function numberOrNull(value) {
        return value === null || value === "" ? null : Number(value);
    }
    function clearResults() {
        error.hidden = true;
        summary.hidden = true;
        results.replaceChildren();
        mapElement.hidden = true;
        if (map) { map.remove(); map = null; }
    }
    function showError(message) {
        error.textContent = message;
        error.hidden = false;
        error.setAttribute("tabindex", "-1");
        error.focus();
    }
    function render(payload) {
        summary.hidden = false;
        summary.textContent = `${payload.message} 후보 ${payload.candidateCount}곳 중 ${payload.recommendations.length}곳을 표시합니다.`;
        summary.focus();
        if (!payload.recommendations.length) {
            const empty = document.createElement("p");
            empty.className = "empty-result";
            empty.textContent = "추천 결과가 없습니다. 카테고리나 지역 조건을 완화해보세요.";
            results.append(empty);
            return;
        }
        payload.recommendations.forEach((item, index) => results.append(card(item, index + 1)));
        renderMap(payload.recommendations);
    }
    function card(item, rank) {
        const article = document.createElement("article");
        article.className = "personalized-card";
        const image = placeImage(item.imageUrl, item.placeName);
        const heading = document.createElement("h3");
        heading.textContent = `${rank}. ${item.placeName}`;
        const meta = document.createElement("p");
        meta.className = "place-meta";
        meta.textContent = `${item.category || "미분류"} · ${item.address || "주소 없음"}`;
        const score = document.createElement("strong");
        score.className = "recommendation-score";
        score.textContent = `${item.score.toFixed(1)}점 · ${Math.round(item.distanceMeters)}m · 교통 ${item.transitVerificationStatus}`;
        const reasons = document.createElement("ul");
        item.recommendationReasons.forEach(reason => {
            const li = document.createElement("li"); li.textContent = reason; reasons.append(li);
        });
        const limitations = document.createElement("details");
        const label = document.createElement("summary"); label.textContent = "데이터 제한사항";
        const list = document.createElement("ul");
        item.dataLimitations.forEach(value => { const li = document.createElement("li"); li.textContent = value; list.append(li); });
        limitations.append(label, list);
        const link = document.createElement("a");
        link.href = `/places/${item.placeId}`; link.className = "secondary-button"; link.textContent = "장소 상세 보기";
        article.append(image, heading, meta, score, reasons, limitations, link);
        return article;
    }
    function placeImage(imageUrl, placeName) {
        const frame = document.createElement("div");
        frame.className = "result-place-image";
        const image = document.createElement("img");
        image.src = imageUrl || "/images/place-placeholder.svg";
        image.alt = `${placeName} 대표 이미지`;
        image.loading = "lazy";
        image.addEventListener("error", () => {
            if (!image.src.endsWith("/images/place-placeholder.svg")) {
                image.src = "/images/place-placeholder.svg";
            }
        }, {once: true});
        frame.append(image);
        return frame;
    }
    function renderMap(items) {
        if (!window.L) {
            mapElement.hidden = false;
            mapElement.classList.add("map-empty");
            mapElement.textContent = "인터넷 연결이 없어 지도를 불러오지 못했습니다. 추천 카드의 주소와 상세 링크를 이용해주세요.";
            return;
        }
        const located = items.filter(item => Number.isFinite(item.latitude) && Number.isFinite(item.longitude));
        if (!located.length) return;
        mapElement.hidden = false;
        map = L.map(mapElement);
        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
            maxZoom: 19, attribution: "&copy; OpenStreetMap contributors"
        }).addTo(map);
        const bounds = [];
        located.forEach(item => {
            const point = [item.latitude, item.longitude]; bounds.push(point);
            const marker = L.marker(point).addTo(map);
            const link = document.createElement("a"); link.href = `/places/${item.placeId}`; link.textContent = item.placeName;
            marker.bindPopup(link);
        });
        map.fitBounds(bounds, {padding: [30, 30], maxZoom: 14});
    }
});
