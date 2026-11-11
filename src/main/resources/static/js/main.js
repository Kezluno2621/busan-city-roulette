document.querySelectorAll("[data-recommend-form]").forEach((form) => {
    const departureName = form.querySelector("[data-departure-name]");
    const latitude = form.querySelector("[data-departure-latitude]");
    const longitude = form.querySelector("[data-departure-longitude]");
    const selectedLocation = form.querySelector("[data-selected-location]");
    const selectedLocationName = form.querySelector("[data-selected-location-name]");
    const currentLocationButton = form.querySelector("[data-current-location]");
    const currentLocationStatus = form.querySelector("[data-current-location-status]");

    const selectDeparture = (name, selectedLatitude, selectedLongitude) => {
        departureName.value = name;
        latitude.value = selectedLatitude;
        longitude.value = selectedLongitude;
        selectedLocationName.textContent = name;
        selectedLocation.classList.remove("is-empty");
    };

    form.querySelectorAll("[data-location-name]").forEach((option) => {
        option.addEventListener("change", () => {
            selectDeparture(
                option.dataset.locationName,
                option.dataset.latitude,
                option.dataset.longitude
            );
        });
    });

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

    form.addEventListener("submit", (event) => {
        const button = event.submitter;
        if (button) {
            button.disabled = true;
            button.textContent = button.matches("[data-search-button]")
                ? "검색 중…"
                : "추천 장소 찾는 중…";
            form.setAttribute("aria-busy", "true");
        }
    });
});
