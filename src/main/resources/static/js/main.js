document.querySelectorAll("[data-recommend-form]").forEach((form) => {
    const departureName = form.querySelector("[data-departure-name]");
    const latitude = form.querySelector("[data-departure-latitude]");
    const longitude = form.querySelector("[data-departure-longitude]");
    const selectedLocation = form.querySelector("[data-selected-location]");
    const selectedLocationName = form.querySelector("[data-selected-location-name]");

    form.querySelectorAll("[data-location-name]").forEach((option) => {
        option.addEventListener("change", () => {
            departureName.value = option.dataset.locationName;
            latitude.value = option.dataset.latitude;
            longitude.value = option.dataset.longitude;
            selectedLocationName.textContent = option.dataset.locationName;
            selectedLocation.classList.remove("is-empty");
        });
    });

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
