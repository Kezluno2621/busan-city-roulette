/**
 * 추천 화면에는 '즉흥 룰렛'과 '맞춤 코스'가 함께 존재한다.
 * 각 영역은 같은 서버 API를 사용하지만 폼 입력과 결과 화면은 독립적으로 관리한다.
 *
 * 중요: 룰렛 애니메이션은 서버가 확정한 결과를 순서대로 보여주는 시각 효과일 뿐,
 * 브라우저에서 장소를 다시 선택하거나 추천 결과를 변경하지 않는다.
 */
document.addEventListener("DOMContentLoaded", () => {
    document
        .querySelectorAll("[data-course-feature]")
        .forEach(initializeCourseFeature);
});

function initializeCourseFeature(feature) {
    const form = feature.querySelector("[data-course-form]");

    // 결과 fragment만 존재하고 입력 폼이 없는 경우에는 초기화하지 않는다.
    if (!form) {
        return;
    }

    const mode = form.dataset.courseMode || "PERSONALIZED";
    const submitButton = form.querySelector("[data-course-submit]");
    const errorMessage = feature.querySelector("[data-course-error]");
    const rouletteStage = feature.querySelector("[data-roulette-stage]");
    const rouletteCurrent = feature.querySelector("[data-roulette-current]");
    const summary = feature.querySelector("[data-course-summary]");
    const results = feature.querySelector("[data-course-results]");
    const listSection = feature.querySelector("[data-course-list-section]");
    const mapElement = feature.querySelector("[data-course-map]");
    const mapSection = feature.querySelector("[data-course-map-section]");
    const savePanel = feature.querySelector("[data-course-save-panel]");
    const loginPanel = feature.querySelector("[data-course-login-panel]");
    const saveButton = feature.querySelector("[data-course-save]");
    const saveTitle = feature.querySelector("[data-course-save-title]");
    const saveStatus = feature.querySelector("[data-course-save-status]");
    const randomnessRange = form.querySelector("[data-course-randomness]");
    const randomnessOutput = form.querySelector("[data-course-randomness-output]");
    const historyKey = "cityRoulette.recentInstantPlaces";
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");

    let map;
    let running = false;
    let latestSaveToken = null;

    bindRandomnessOutput();
    bindHistoryReset();
    bindCourseGeneration();
    bindCourseSave();

    /** 랜덤성 슬라이더 값을 사용자가 즉시 확인할 수 있게 숫자로 동기화한다. */
    function bindRandomnessOutput() {
        if (!randomnessRange || !randomnessOutput) {
            return;
        }

        randomnessRange.addEventListener("input", () => {
            randomnessOutput.textContent = randomnessRange.value;
        });
    }

    /** 즉흥 룰렛의 최근 추천 기록만 지우며 서버나 DB 데이터는 변경하지 않는다. */
    function bindHistoryReset() {
        feature.querySelector("[data-reset-history]")?.addEventListener("click", event => {
            localStorage.removeItem(historyKey);
            event.currentTarget.textContent = "추천 기록을 초기화했습니다";

            window.setTimeout(() => {
                event.currentTarget.textContent = "추천 기록 초기화";
            }, 1600);
        });
    }

    /**
     * 입력값을 기존 코스 생성 API 형식으로 변환하고 결과를 렌더링한다.
     * 실행 중 버튼을 잠가 애니메이션 도중 중복 요청이 발생하지 않게 한다.
     */
    function bindCourseGeneration() {
        form.addEventListener("submit", async event => {
            event.preventDefault();

            if (running) {
                return;
            }

            resetOutput();

            const latitude = document.querySelector("[data-departure-latitude]")?.value;
            const longitude = document.querySelector("[data-departure-longitude]")?.value;

            if (mode === "PERSONALIZED" && (!latitude || !longitude)) {
                showError("맞춤 코스를 만들려면 위의 출발지를 검색하거나 현재 위치를 선택해주세요.");
                return;
            }

            running = true;
            form.setAttribute("aria-busy", "true");
            submitButton.disabled = true;
            submitButton.textContent = "코스를 만들고 있습니다…";

            const formData = new FormData(form);
            const seed = numberOrNull(formData.get("randomSeed"));
            const requestBody = createRequestBody(formData, latitude, longitude, seed);

            try {
                const response = await fetch("/api/courses/generate", {
                    method: "POST",
                    headers: requestHeaders(),
                    body: JSON.stringify(requestBody)
                });
                const payload = await response.json();

                if (!response.ok) {
                    throw new Error(payload.message || "코스를 생성하지 못했습니다.");
                }

                await animateSelection(payload.places);
                renderCourse(payload);

                // 명시적 seed가 없는 즉흥 추천만 최근 기록에 저장한다.
                if (mode === "INSTANT" && seed === null) {
                    rememberPlaces(payload.places, historyKey);
                }

                submitButton.textContent = mode === "INSTANT"
                    ? "🎡 다시 뽑기"
                    : "🧭 조건으로 다시 만들기";
            } catch (exception) {
                showError(exception.message || "코스 생성 중 오류가 발생했습니다.");
                submitButton.textContent = mode === "INSTANT"
                    ? "🎡 바로 뽑기"
                    : "🧭 맞춤 코스 만들기";
            } finally {
                running = false;
                submitButton.disabled = false;
                form.removeAttribute("aria-busy");
            }
        });
    }

    /** 서버가 발급한 일회성 토큰을 사용해 로그인 회원의 코스를 저장한다. */
    function bindCourseSave() {
        saveButton?.addEventListener("click", async () => {
            const title = saveTitle?.value.trim();

            if (!latestSaveToken || !title) {
                return;
            }

            saveButton.disabled = true;
            saveStatus.textContent = "저장 중…";

            try {
                const response = await fetch("/api/courses", {
                    method: "POST",
                    headers: requestHeaders(),
                    body: JSON.stringify({
                        title,
                        generationToken: latestSaveToken
                    })
                });
                const payload = await response.json();

                if (!response.ok) {
                    throw new Error(payload.message || "코스를 저장하지 못했습니다.");
                }

                latestSaveToken = null;
                saveStatus.replaceChildren();

                const savedCourseLink = document.createElement("a");
                savedCourseLink.href = `/my/courses/${payload.id}`;
                savedCourseLink.textContent = "저장된 코스 보기";

                saveStatus.append("저장했습니다. ", savedCourseLink);
                saveButton.hidden = true;
            } catch (exception) {
                saveStatus.textContent = exception.message;
            } finally {
                saveButton.disabled = false;
            }
        });
    }

    /** 폼 입력값을 기존 CourseGenerationRequest 구조로 변환한다. */
    function createRequestBody(formData, latitude, longitude, seed) {
        return {
            originLatitude: latitude ? Number(latitude) : null,
            originLongitude: longitude ? Number(longitude) : null,
            categories: formData.getAll("categories"),
            district: formData.get("district") || null,
            availableMinutes: numberOrNull(formData.get("availableMinutes")),
            budget: numberOrNull(formData.get("budget")),
            randomness: mode === "INSTANT"
                ? 100
                : Number(formData.get("randomness")),
            placeCount: Number(formData.get("placeCount")),
            randomSeed: seed,
            categoryMode: formData.get("categoryMode") || "PREFERRED",
            generationMode: mode,
            excludedPlaceIds: mode === "INSTANT" && seed === null
                ? recentPlaceIds(historyKey)
                : []
        };
    }

    /** Spring Security CSRF 토큰이 있는 경우 기존 요청 헤더에 함께 담는다. */
    function requestHeaders() {
        const headers = {"Content-Type": "application/json"};
        const csrfToken = form.querySelector("input[name='_csrf']")?.value;

        if (csrfToken) {
            headers["X-CSRF-TOKEN"] = csrfToken;
        }

        return headers;
    }

    function numberOrNull(value) {
        return value === null || value === "" ? null : Number(value);
    }

    /** 최근 추천 장소는 브라우저에 최대 30개까지만 보관한다. */
    function recentPlaceIds(key) {
        try {
            return JSON
                .parse(localStorage.getItem(key) || "[]")
                .filter(Number.isFinite)
                .slice(-30);
        } catch (_) {
            return [];
        }
    }

    function rememberPlaces(places, key) {
        const placeIds = places.map(place => place.placeId);
        const combined = [...recentPlaceIds(key), ...placeIds];
        const uniqueRecentIds = [...new Set(combined)].slice(-30);

        localStorage.setItem(key, JSON.stringify(uniqueRecentIds));
    }

    /** 새로운 요청 전에 이전 결과, 지도, 저장 토큰을 안전하게 초기화한다. */
    function resetOutput() {
        errorMessage.hidden = true;
        summary.hidden = true;
        rouletteStage.hidden = true;
        listSection.hidden = true;
        mapSection.hidden = true;
        results.replaceChildren();
        mapElement.classList.remove("map-empty");
        mapElement.textContent = "";
        latestSaveToken = null;

        if (savePanel) {
            savePanel.hidden = true;
        }
        if (loginPanel) {
            loginPanel.hidden = true;
        }
        if (saveButton) {
            saveButton.hidden = false;
        }
        if (saveStatus) {
            saveStatus.textContent = "";
        }
        if (map) {
            map.remove();
            map = null;
        }
    }

    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.hidden = false;
        errorMessage.tabIndex = -1;
        errorMessage.focus();
    }

    /**
     * 모션 감소 설정이 켜져 있으면 반복 애니메이션을 생략하고 결과만 표시한다.
     * 애니메이션이 끝나도 서버가 반환한 순서와 장소는 바뀌지 않는다.
     */
    async function animateSelection(places) {
        if (!places.length) {
            return;
        }

        rouletteStage.hidden = false;

        if (reducedMotion.matches) {
            rouletteCurrent.textContent = places
                .map(place => place.placeName)
                .join(" → ");
            return;
        }

        for (const selectedPlace of places) {
            rouletteStage.classList.add("is-spinning");

            for (let frame = 0; frame < 10; frame++) {
                rouletteCurrent.textContent = places[frame % places.length].placeName;
                await new Promise(resolve => {
                    window.setTimeout(resolve, 45 + frame * 7);
                });
            }

            rouletteCurrent.textContent =
                `${selectedPlace.sequence}번 · ${selectedPlace.placeName}`;
            rouletteStage.classList.remove("is-spinning");

            await new Promise(resolve => {
                window.setTimeout(resolve, 220);
            });
        }
    }

    /** 서버 응답을 요약, 장소 카드, 저장 안내, 지도의 순서로 렌더링한다. */
    function renderCourse(payload) {
        renderCourseSummary(payload);

        const hasSelectedOrigin = payload.origin?.selectedByUser === true;
        payload.places.forEach(place => {
            results.append(createCourseCard(place, hasSelectedOrigin));
        });

        listSection.hidden = payload.places.length === 0;
        latestSaveToken = payload.saveToken;

        if (savePanel && latestSaveToken) {
            savePanel.hidden = false;
        }
        if (loginPanel && payload.places.length) {
            loginPanel.hidden = false;
        }

        renderMap(payload);
    }

    /** 코스 제목과 방문 순서, 핵심 추천 기준을 한 영역에 구성한다. */
    function renderCourseSummary(payload) {
        const shortageMessage = payload.insufficientCandidates
            ? " 요청 수보다 후보가 적어 가능한 장소만 표시했습니다."
            : "";

        summary.hidden = false;
        summary.replaceChildren();

        const eyebrow = createTextElement(
            "p",
            "course-result-eyebrow",
            "TODAY'S CITY ROULETTE"
        );
        const heading = createTextElement("h2", "", "오늘의 추천 코스");
        const subtitle = createTextElement(
            "p",
            "course-result-subtitle",
            `현재 조건에서 추천할 수 있는 장소들을 방문 순서로 정리했어요.${shortageMessage}`
        );

        summary.append(
            eyebrow,
            heading,
            subtitle,
            createCourseSteps(payload.places),
            createRecommendationGuide(payload)
        );

        // 새 결과가 생성되면 키보드·스크린리더 사용자가 결과 시작점을 바로 찾게 한다.
        summary.tabIndex = -1;
        summary.focus();
    }

    function createRecommendationGuide(payload) {
        const guide = document.createElement("div");
        guide.className = "course-guide";

        const guideTitle = createTextElement(
            "strong",
            "",
            payload.message || "선호 조건과 장소 다양성을 반영했습니다."
        );
        const highlights = document.createElement("ul");
        highlights.className = "course-guide-highlights";

        [
            "선택한 조건과 카테고리 다양성을 반영해 코스를 구성했습니다.",
            "이동시간 정보가 없는 장소는 직선거리를 보조 기준으로 활용했습니다.",
            "장소별 자세한 정보는 상세 보기에서 확인할 수 있습니다."
        ].forEach(message => {
            highlights.append(createTextElement("li", "", message));
        });

        guide.append(guideTitle, highlights);

        if (payload.dataLimitations?.length) {
            guide.append(createLimitationDetails(payload.dataLimitations));
        }

        return guide;
    }

    /** 상단 방문 순서와 하단 카드가 같은 순번 badge를 사용하게 구성한다. */
    function createCourseSteps(places) {
        const list = document.createElement("ol");
        list.className = "course-steps";
        list.setAttribute("aria-label", "추천 코스 방문 순서");

        places.forEach(place => {
            const item = document.createElement("li");
            item.className = "course-step";

            const badge = createTextElement(
                "span",
                "course-sequence",
                place.sequence
            );
            const copy = document.createElement("span");
            copy.className = "course-step-copy";
            copy.append(
                createTextElement("strong", "", place.placeName),
                createTextElement("small", "", place.category || "미분류")
            );

            item.append(badge, copy);
            list.append(item);
        });

        return list;
    }

    /** 긴 기술적 제한사항은 기본 화면을 복잡하게 만들지 않도록 접어서 제공한다. */
    function createLimitationDetails(messages) {
        const details = document.createElement("details");
        details.className = "course-guide-details";

        const detailsSummary = createTextElement(
            "summary",
            "",
            "추천 기준 자세히 보기"
        );
        const list = document.createElement("ul");
        list.className = "course-limitations";

        messages.forEach(message => {
            list.append(createTextElement("li", "", friendlyLimitation(message)));
        });

        details.append(detailsSummary, list);
        return details;
    }

    function friendlyLimitation(message) {
        return String(message)
            .replaceAll("distanceMeters", "표시된 거리")
            .replaceAll("UNKNOWN", "아직 확인되지 않음")
            .replace(
                "주소와 상세 정보가 비교적 충실합니다.",
                "장소 정보가 충분히 제공되는 곳이에요."
            );
    }

    function friendlyReason(reason) {
        return String(reason)
            .replace(
                "주소와 상세 정보가 비교적 충실합니다.",
                "장소 정보가 충분히 제공되는 곳이에요."
            )
            .replace(
                "출발지에서 직선거리가 비교적 가깝습니다.",
                "출발지에서 비교적 가까운 장소예요."
            );
    }

    function formatDistance(meters) {
        const distance = Math.max(0, Math.round(Number(meters) || 0));

        return distance >= 1000
            ? `약 ${(distance / 1000).toFixed(1)}km`
            : `약 ${distance}m`;
    }

    /** 한 장소의 이미지, 핵심 정보, 추천 근거, 상세 링크를 카드로 만든다. */
    function createCourseCard(place, hasSelectedOrigin) {
        const item = document.createElement("li");
        item.className = "course-card";

        const badge = createTextElement(
            "span",
            "course-sequence",
            place.sequence
        );
        const imageFrame = createPlaceImage(place);
        const copy = document.createElement("div");
        copy.className = "course-card-copy";

        const cardHeader = document.createElement("div");
        cardHeader.className = "course-card-header";
        cardHeader.append(
            createTextElement("h3", "", place.placeName),
            createTextElement(
                "span",
                "course-category-badge",
                place.category || "미분류"
            )
        );

        const distance = place.sequence === 1 && !hasSelectedOrigin
            ? createTextElement(
                "p",
                "course-distance",
                "출발지를 선택하지 않아 이 장소부터 코스를 시작합니다."
            )
            : createTextElement(
                "p",
                "course-distance",
                `${place.sequence === 1 ? "출발지" : "이전 장소"}에서 직선거리 ${formatDistance(place.distanceFromPreviousMeters)}`
            );
        const address = createTextElement(
            "p",
            "course-address",
            place.address || place.district || "위치 상세 정보 없음"
        );
        const metrics = createCardMetrics(place);
        const reasons = createRecommendationReasons(place.recommendationReasons);

        const detailLink = createTextElement(
            "a",
            "secondary-button",
            "장소 상세 보기"
        );
        detailLink.href = `/places/${place.placeId}`;

        copy.append(
            cardHeader,
            distance,
            address,
            metrics,
            reasons,
            detailLink
        );
        item.append(badge, imageFrame, copy);

        return item;
    }

    /**
     * 원본 이미지가 없거나 로딩에 실패하면 기존 SVG와 안내 문구를 함께 표시한다.
     * 실패한 외부 URL을 반복해서 요청하지 않도록 error 처리는 한 번만 실행한다.
     */
    function createPlaceImage(place) {
        const placeholderUrl = "/images/place-placeholder.svg";
        const hasOriginalImage = Boolean(place.imageUrl);
        const frame = document.createElement("div");
        frame.className = "result-place-image course-place-image";

        const fallback = createTextElement(
            "span",
            "course-image-fallback",
            "이미지 준비 중"
        );
        fallback.hidden = hasOriginalImage;

        const image = document.createElement("img");
        image.src = place.imageUrl || placeholderUrl;
        image.alt = `${place.placeName} 대표 이미지`;
        image.loading = "lazy";

        if (!hasOriginalImage) {
            image.classList.add("is-placeholder");
        }

        image.addEventListener("error", () => {
            fallback.hidden = false;
            image.classList.add("is-placeholder");
            image.src = placeholderUrl;
        }, {once: true});

        frame.append(image, fallback);
        return frame;
    }

    function createCardMetrics(place) {
        const metrics = document.createElement("div");
        metrics.className = "course-card-metrics";

        const score = document.createElement("div");
        score.className = "course-score";

        const scoreNumber = Number(place.recommendationScore);
        const displayScore = Number.isFinite(scoreNumber)
            ? scoreNumber.toFixed(1)
            : "정보 없음";

        score.append(
            createTextElement("span", "", "추천 적합도"),
            createTextElement("strong", "", displayScore)
        );

        // 점수 의미를 왜곡하지 않도록 실제 0~100 범위일 때만 막대를 표시한다.
        if (Number.isFinite(scoreNumber) && scoreNumber >= 0 && scoreNumber <= 100) {
            const progress = document.createElement("progress");
            progress.max = 100;
            progress.value = scoreNumber;
            progress.setAttribute("aria-label", `추천 적합도 ${displayScore}점`);
            score.append(progress);
        }

        const transitStatus = createTextElement(
            "p",
            "course-transit-status",
            transitLabel(place.transitVerificationStatus)
        );

        metrics.append(score, transitStatus);
        return metrics;
    }

    function createRecommendationReasons(recommendationReasons) {
        const reasons = document.createElement("ul");
        reasons.className = "course-reasons";

        // 카드의 정보 밀도를 낮추기 위해 핵심 추천 이유 두 개까지만 노출한다.
        (recommendationReasons || []).slice(0, 2).forEach(reason => {
            reasons.append(createTextElement("li", "", friendlyReason(reason)));
        });

        return reasons;
    }

    function transitLabel(status) {
        const labels = {
            VERIFIED: "교통 연결이 확인된 장소예요",
            PARTIAL_DATA: "일부 교통 정보를 확인했어요",
            UNVERIFIED: "교통 정보 준비 중",
            NOT_EVALUATED: "교통 정보 준비 중"
        };

        return labels[status] || "교통 정보 준비 중";
    }

    /**
     * 사용자가 출발지를 선택한 경우에만 출발지 마커를 표시한다.
     * 즉흥 룰렛에서 출발지를 생략하면 내부 후보 계산 기준점은 지도에 노출하지 않는다.
     */
    function renderMap(payload) {
        if (!payload.places.length) {
            return;
        }

        mapSection.hidden = false;

        if (!window.L) {
            mapElement.classList.add("map-empty");
            mapElement.textContent =
                "지도를 불러오지 못했습니다. 아래 장소 카드에서 방문 순서를 확인해주세요.";
            return;
        }

        map = L.map(mapElement);
        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
            maxZoom: 19,
            attribution: "&copy; OpenStreetMap contributors"
        }).addTo(map);

        const points = [];
        const hasSelectedOrigin = payload.origin?.selectedByUser === true
            && Number.isFinite(payload.origin.latitude)
            && Number.isFinite(payload.origin.longitude);

        if (hasSelectedOrigin) {
            const originPoint = [payload.origin.latitude, payload.origin.longitude];
            points.push(originPoint);

            L.circleMarker(originPoint, {
                radius: 9,
                color: "#1565c0",
                fillOpacity: 1
            })
                .addTo(map)
                .bindPopup(payload.origin.name || "출발지");
        }

        payload.places.forEach(place => {
            const point = [place.latitude, place.longitude];
            points.push(point);

            const icon = L.divIcon({
                className: "course-number-marker",
                html: String(place.sequence),
                iconSize: [28, 28]
            });
            const marker = L.marker(point, {icon}).addTo(map);
            const popupLink = createTextElement("a", "", place.placeName);
            popupLink.href = `/places/${place.placeId}`;
            marker.bindPopup(popupLink);
        });

        if (points.length > 1) {
            L.polyline(points, {
                color: "#ef5350",
                weight: 3,
                opacity: .7,
                dashArray: "8 8"
            }).addTo(map);
        }

        map.fitBounds(points, {
            padding: [30, 30],
            maxZoom: 14
        });
    }

    /** 반복되는 텍스트 요소 생성을 한곳에 모아 DOM 구성 코드를 읽기 쉽게 한다. */
    function createTextElement(tagName, className, text) {
        const element = document.createElement(tagName);

        if (className) {
            element.className = className;
        }

        element.textContent = text;
        return element;
    }
}
