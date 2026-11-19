/**
 * 추천 화면의 출발지 검색과 브라우저 현재 위치 선택을 관리한다.
 * 검색 결과 HTML은 서버가 만들고, 이 파일은 페이지 새로고침 없이 결과 영역만 교체한다.
 */
document.querySelectorAll("[data-recommend-form]").forEach((form) => {
    const departureName = form.querySelector("[data-departure-name]");
    const latitude = form.querySelector("[data-departure-latitude]");
    const longitude = form.querySelector("[data-departure-longitude]");
    const selectedLocation = form.querySelector("[data-selected-location]");
    const selectedLocationName = form.querySelector("[data-selected-location-name]");
    const currentLocationButton = form.querySelector("[data-current-location]");
    const currentLocationStatus = form.querySelector("[data-current-location-status]");
    const locationFeedback = form.querySelector("[data-location-feedback]");

    /** 선택한 장소를 추천·코스 API가 공통으로 사용하는 숨은 입력값에 기록한다. */
    const selectDeparture = (name, selectedLatitude, selectedLongitude) => {
        departureName.value = name;
        latitude.value = selectedLatitude;
        longitude.value = selectedLongitude;
        selectedLocationName.textContent = name;
        selectedLocation.classList.remove("is-empty");
    };

    /** 비동기 검색으로 새로 생성된 라디오 버튼에도 선택 이벤트를 다시 연결한다. */
    const bindLocationOptions = () => {
        form.querySelectorAll("[data-location-name]").forEach((option) => {
            option.addEventListener("change", () => {
                selectDeparture(
                    option.dataset.locationName,
                    option.dataset.latitude,
                    option.dataset.longitude
                );
                currentLocationStatus.textContent = `${option.dataset.locationName}을(를) 출발지로 설정했습니다.`;
            });
        });
    };

    bindLocationOptions();

    // 현재 위치는 브라우저 지원 여부와 사용자의 위치 권한에 따라 선택적으로 제공한다.
    if (currentLocationButton) {
        if (!navigator.geolocation) {
            currentLocationButton.disabled = true;
            currentLocationStatus.textContent = "이 브라우저에서는 현재 위치 기능을 지원하지 않습니다.";
        } else {
            currentLocationButton.addEventListener("click", () => {
                currentLocationButton.disabled = true;
                currentLocationButton.textContent = "위치 확인 중…";
                currentLocationStatus.textContent = "브라우저의 위치 권한 요청을 확인해주세요.";

                navigator.geolocation.getCurrentPosition(
                    (position) => {
                        selectDeparture(
                            "현재 위치",
                            position.coords.latitude,
                            position.coords.longitude
                        );
                        currentLocationStatus.textContent = "현재 위치를 출발지로 설정했습니다.";
                        currentLocationButton.disabled = false;
                        currentLocationButton.textContent = "현재 위치 다시 확인";
                    },
                    (error) => {
                        const messages = {
                            1: "위치 권한이 거부되었습니다. 브라우저 주소창의 위치 권한을 허용해주세요.",
                            2: "현재 위치 정보를 확인할 수 없습니다.",
                            3: "현재 위치 확인 시간이 초과되었습니다. 다시 시도해주세요."
                        };
                        currentLocationStatus.textContent = messages[error.code]
                            || "현재 위치를 불러오지 못했습니다.";
                        currentLocationButton.disabled = false;
                        currentLocationButton.textContent = "현재 위치";
                    },
                    {enableHighAccuracy: true, timeout: 10000, maximumAge: 60000}
                );
            });
        }
    }

    // 검색 폼의 전체 페이지 이동을 막고 검색 결과 영역만 갱신한다.
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const button = event.submitter;
        if (!button || !button.matches("[data-search-button]")) {
            return;
        }

        const originalLabel = button.textContent;
        button.disabled = true;
        button.textContent = "검색 중…";
        form.setAttribute("aria-busy", "true");
        currentLocationStatus.textContent = "출발지를 검색하고 있습니다.";

        try {
            const response = await fetch(form.action, {
                method: "POST",
                body: new FormData(form),
                headers: {"Accept": "text/html"}
            });
            if (!response.ok) {
                throw new Error(`Location search failed: ${response.status}`);
            }

            const documentFragment = new DOMParser().parseFromString(
                await response.text(),
                "text/html"
            );
            const nextFeedback = documentFragment.querySelector("[data-location-feedback]");
            if (!nextFeedback || !locationFeedback) {
                throw new Error("Location search response is incomplete");
            }

            locationFeedback.innerHTML = nextFeedback.innerHTML;
            bindLocationOptions();
            const resultCount = locationFeedback.querySelectorAll("[data-location-name]").length;
            currentLocationStatus.textContent = resultCount > 0
                ? `검색 결과 ${resultCount}곳을 불러왔습니다. 원하는 출발지를 선택해주세요.`
                : "검색 결과를 확인해주세요.";
        } catch (error) {
            if (locationFeedback) {
                locationFeedback.replaceChildren();
                const message = document.createElement("p");
                message.className = "form-error location-error";
                message.setAttribute("role", "alert");
                message.textContent = "출발지 검색 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
                locationFeedback.append(message);
            }
            currentLocationStatus.textContent = "출발지 검색을 완료하지 못했습니다.";
        } finally {
            button.disabled = false;
            button.textContent = originalLabel;
            form.removeAttribute("aria-busy");
        }
    });
});
