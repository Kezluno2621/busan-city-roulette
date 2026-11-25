const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");
const vm = require("node:vm");

const script = fs.readFileSync(path.join(
    __dirname, "../src/main/resources/static/js/saved-course-map.js"
), "utf8");

function renderMap(origin, places, leafletAvailable = true) {
    const state = {created: 0, labels: [], icons: [], bounds: [], classes: []};
    const mapElement = {
        classList: {add: name => state.classes.push(name)},
        textContent: ""
    };
    let onReady;
    const leaf = {addTo() { return this; }, bindPopup(label) { state.labels.push(label); }};
    const map = {
        fitBounds(points) {
            state.bounds = Array.from(points, point => Array.from(point));
        }
    };
    const L = {
        map() { state.created++; return map; },
        tileLayer: () => leaf,
        marker: () => leaf,
        circleMarker: () => leaf,
        polyline: () => leaf,
        divIcon(options) { state.icons.push(options.html); return options; }
    };
    const document = {
        addEventListener(_event, handler) { onReady = handler; },
        querySelector: selector => selector === ".course-detail-map" ? {
            dataset: origin,
            querySelectorAll: () => places.map(dataset => ({dataset}))
        } : null,
        getElementById: () => mapElement,
        createElement: tagName => ({tagName, textContent: ""})
    };
    vm.runInNewContext(script, { document, L, window: { L: leafletAvailable ? L : undefined } });
    onReady();
    return {state, mapElement};
}

test("출발 좌표 없이도 유효한 방문 장소를 텍스트 팝업으로 표시한다", () => {
    const {state} = renderMap({}, [
        {lat: "", lon: "129", name: "좌표 없음", sequence: "1"},
        {lat: "35.17", lon: "129.06", name: "바다 & 산", sequence: "2"}
    ]);

    assert.equal(state.created, 1);
    assert.deepEqual(state.bounds, [[35.17, 129.06]]);
    assert.deepEqual(state.labels, [{tagName: "span", textContent: "바다 & 산"}]);
    assert.deepEqual(state.icons, [{tagName: "span", textContent: "2"}]);
});

test("표시할 좌표가 없으면 지도 대신 안내를 표시한다", () => {
    const {state, mapElement} = renderMap({originName: "출발지 미지정"}, [
        {lat: "120", lon: "129", name: "범위 밖", sequence: "1"}
    ]);

    assert.equal(state.created, 0);
    assert.ok(state.classes.includes("map-empty"));
    assert.match(mapElement.textContent, /표시할 위치 정보가 없습니다/);
});

test("Leaflet을 불러오지 못하면 기존 안내를 표시한다", () => {
    const {state, mapElement} = renderMap({}, [], false);

    assert.equal(state.created, 0);
    assert.match(mapElement.textContent, /지도를 불러오지 못했습니다/);
});
