import fs from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";
import { Presentation, PresentationFile } from "@oai/artifact-tool";

const workspaceDir = "C:/Projects/busan-city-roulette";
const buildDir = path.join(workspaceDir, ".build/presentation");
const outputDir = path.join(workspaceDir, "submission/presentation");
const skillDir = "C:/Users/kim05/.codex/plugins/cache/openai-primary-runtime/presentations/26.1105.12137/skills/presentations";
const finalPath = path.join(outputDir, "city-roulette-final-presentation-v2.pptx");
const pythonExecutable = "C:/Users/kim05/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe";
const font = "Malgun Gothic";

const screenshotFiles = [
  "docs/qa/screenshots/01-home-desktop.png",
  "docs/qa/screenshots/02-places-desktop.png",
  "docs/qa/screenshots/05-personalized-recommendation-desktop.png",
  "docs/qa/screenshots/06-course-roulette-desktop.png",
  "docs/qa/screenshots/08-saved-course-detail-desktop.png",
];
const screenshotBytes = new Map();
for (const file of screenshotFiles) {
  const bytes = await fs.readFile(path.join(workspaceDir, file));
  screenshotBytes.set(file, bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength));
}

const C = {
  navy: "17324D",
  navy2: "0D2238",
  teal: "67AAA4",
  teal2: "DDEDEA",
  coral: "E4775C",
  cream: "F4F6F0",
  white: "FFFFFF",
  ink: "152638",
  gray: "5D6B78",
  pale: "E8ECE7",
  gold: "E0B24D",
};

const presentation = Presentation.create({ slideSize: { width: 1280, height: 720 } });

function addShape(slide, geometry, left, top, width, height, fill, lineFill = "none", radius) {
  return slide.shapes.add({
    geometry,
    position: { left, top, width, height },
    fill,
    line: { style: "solid", fill: lineFill, width: lineFill === "none" ? 0 : 1 },
    ...(radius ? { borderRadius: radius } : {}),
  });
}

function addText(slide, text, left, top, width, height, size, color = C.ink, bold = false, align = "left") {
  const shape = addShape(slide, "textbox", left, top, width, height, "none");
  shape.text = text;
  shape.text.style = {
    typeface: font,
    fontSize: size,
    color,
    bold,
    alignment: align,
    verticalAlignment: "middle",
    lineSpacing: 1.05,
  };
  return shape;
}

function title(slide, number, heading, subheading) {
  addText(slide, String(number).padStart(2, "0"), 62, 38, 60, 28, 14, C.teal, true);
  addText(slide, heading, 62, 70, 1040, 54, 31, C.navy, true);
  if (subheading) addText(slide, subheading, 64, 124, 1050, 34, 15, C.gray, false);
  addText(slide, String(number), 1182, 664, 42, 22, 11, C.gray, false, "right");
}

function addImage(slide, file, left, top, width, height, alt, crop = undefined, fit = "cover") {
  return slide.images.add({
    blob: screenshotBytes.get(file),
    contentType: "image/png",
    alt,
    fit,
    position: { left, top, width, height },
    ...(crop ? { crop } : {}),
    geometry: "roundRect",
    borderRadius: 18,
  });
}

function note(slide, text) {
  slide.speakerNotes.text = text;
  slide.speakerNotes.setVisible(true);
}

function flowBox(slide, text, left, top, width, height, fill = C.white, color = C.navy) {
  const box = addShape(slide, "roundRect", left, top, width, height, fill, C.pale, 12);
  box.text = text;
  box.text.style = { typeface: font, fontSize: 16, color, bold: true, alignment: "center", verticalAlignment: "middle" };
  return box;
}

function line(slide, x, y, w, h, color = C.teal) {
  if (w < 0) { x += w; w = -w; }
  if (h < 0) { y += h; h = -h; }
  return slide.shapes.add({
    geometry: "line",
    position: { left: x, top: y, width: w, height: h },
    fill: "none",
    line: { style: "solid", fill: color, width: 2, endArrowType: "triangle" },
  });
}

// 1. Cover
{
  const s = presentation.slides.add();
  s.background.fill = C.navy2;
  addShape(s, "rect", 0, 0, 1280, 720, C.navy2);
  addShape(s, "rect", 0, 0, 14, 720, C.teal);
  addText(s, "BUSAN RANDOM TRIP", 72, 114, 420, 30, 15, C.teal, true);
  addText(s, "City Roulette", 70, 162, 560, 76, 50, C.white, true);
  addText(s, "부산 공공데이터 기반 즉흥 여행 추천 서비스", 72, 252, 520, 56, 23, "D7E2E8", false);
  addText(s, "웹프로그래밍 기말 프로젝트", 72, 328, 360, 34, 17, "AFC0CC", false);
  addShape(s, "roundRect", 72, 420, 420, 96, C.navy, "37566F", 18);
  addText(s, "관광 장소 395건\n추천 · 룰렛 · 회원별 저장", 98, 437, 365, 64, 22, C.white, true);
  addImage(s, "docs/qa/screenshots/01-home-desktop.png", 708, 42, 420, 636, "City Roulette main page", { left: 0, top: 0, right: 0, bottom: 0 });
  note(s, "발표 표지. 출처: README.md, docs/qa/screenshots/01-home-desktop.png");
}

// 2. Problem and service
{
  const s = presentation.slides.add();
  s.background.fill = C.cream;
  title(s, 2, "문제 정의와 서비스 흐름", "여러 서비스에 흩어진 탐색 과정을 한 번의 사용자 흐름으로 연결");
  addText(s, "여행 전 반복되는 일", 72, 200, 310, 36, 21, C.navy, true);
  addText(s, "장소 검색\n지도 확인\n대중교통 탐색\n후보 비교와 결정", 74, 252, 280, 190, 22, C.gray, false);
  line(s, 352, 340, 95, 0, C.coral);
  addShape(s, "roundRect", 452, 205, 356, 280, C.navy, C.navy, 24);
  addText(s, "City Roulette", 490, 235, 280, 48, 31, C.white, true, "center");
  addText(s, "공공데이터 통합\n조건 기반 추천\nseed 기반 룰렛 코스\n회원별 저장", 500, 306, 260, 148, 21, "DDEDEA", false, "center");
  line(s, 813, 340, 85, 0, C.coral);
  addText(s, "사용자가 얻는 결과", 920, 200, 290, 36, 21, C.navy, true);
  addText(s, "추천 점수와 이유\n장소 방문 순서\n지도 위치\n데이터 한계 표시", 920, 252, 280, 190, 22, C.gray, false);
  addText(s, "확인되지 않은 시간과 비용은 생성하지 않는다", 72, 582, 1050, 40, 24, C.coral, true);
  note(s, "출처: README.md, docs/final-project-report.md 1장과 2장");
}

// 3. Service overview with actual screen
{
  const s = presentation.slides.add();
  s.background.fill = C.cream;
  title(s, 3, "통합 관광지 탐색", "검색·필터·상세·지도·출처를 실제 DB 395건으로 제공");
  addImage(s, "docs/qa/screenshots/02-places-desktop.png", 60, 178, 720, 480, "Tourism place list with filters", { left: 0, top: 0.04, right: 0, bottom: 0.58 });
  addText(s, "서버 측 페이지네이션", 842, 212, 320, 36, 23, C.navy, true);
  addText(s, "전체 장소를 브라우저에 한 번에 보내지 않고 키워드·카테고리·구군 조건을 DB에서 적용", 842, 258, 330, 96, 17, C.gray, false);
  addText(s, "출처와 이미지 정책", 842, 390, 320, 36, 23, C.navy, true);
  addText(s, "Place와 PlaceSource를 분리하고 라이선스가 불명확한 이미지는 대체 이미지로 처리", 842, 436, 330, 100, 17, C.gray, false);
  addText(s, "395", 840, 566, 120, 56, 40, C.teal, true);
  addText(s, "Place / PlaceSource 395", 956, 577, 235, 32, 17, C.navy, true);
  note(s, "출처: docs/qa/screenshots/02-places-desktop.png, docs/phase4-c3-tourism-data-scale-up.md");
}

// 4. Architecture
{
  const s = presentation.slides.add();
  s.background.fill = C.cream;
  title(s, 4, "시스템 아키텍처", "웹 요청과 데이터 수집기를 분리한 Spring Boot 3계층 구조");
  flowBox(s, "Browser\nThymeleaf + JS", 54, 250, 180, 90, C.white);
  line(s, 238, 295, 62, 0);
  flowBox(s, "Controller", 306, 250, 150, 90, C.teal2);
  line(s, 460, 295, 54, 0);
  flowBox(s, "Service\n추천 · 코스 · 교통", 520, 232, 206, 126, C.navy, C.white);
  line(s, 730, 295, 54, 0);
  flowBox(s, "Repository", 790, 250, 154, 90, C.teal2);
  line(s, 948, 295, 62, 0);
  flowBox(s, "MySQL 8\nFlyway V12", 1016, 250, 190, 90, C.white);
  flowBox(s, "TourAPI · 부산 공공데이터\n부산버스 XML", 172, 492, 260, 92, C.white);
  line(s, 436, 538, 68, 0, C.coral);
  flowBox(s, "Manual Collector\nParse · Normalize · Validate", 510, 480, 280, 116, C.navy, C.white);
  line(s, 794, 538, 120, -190, C.coral);
  addText(s, "자동 수집 없음", 526, 614, 250, 28, 15, C.coral, true, "center");
  note(s, "출처: src/main/java/com/cityroulette의 controller/service/repository 구조, application.properties");
}

// 5. Data integration
{
  const s = presentation.slides.add();
  s.background.fill = C.cream;
  title(s, 5, "공공데이터 수집과 통합", "4개 공급자 395건, 동일 공급자·외부 ID 중복 0건");
  const providers = [
    ["TourAPI", 110, C.navy],
    ["부산명소", 100, C.teal],
    ["부산맛집", 100, C.coral],
    ["갈맷길", 85, C.gold],
  ];
  let y = 208;
  for (const [name, value, color] of providers) {
    addText(s, name, 74, y, 150, 34, 18, C.navy, true);
    addShape(s, "roundRect", 230, y + 4, value * 4.1, 26, color, color, 8);
    addText(s, String(value), 650, y, 70, 34, 18, C.navy, true, "right");
    y += 72;
  }
  addText(s, "API", 780, 208, 80, 30, 16, C.teal, true, "center");
  flowBox(s, "Raw DTO", 770, 252, 108, 64, C.white);
  line(s, 882, 284, 50, 0);
  flowBox(s, "정규화", 938, 252, 108, 64, C.teal2);
  line(s, 1050, 284, 50, 0);
  flowBox(s, "검증", 1106, 252, 108, 64, C.white);
  line(s, 1160, 320, 0, 58);
  flowBox(s, "원천 키 upsert", 1050, 384, 164, 68, C.navy, C.white);
  line(s, 1046, 418, -84, 0);
  flowBox(s, "Place + Source", 804, 384, 154, 68, C.teal2);
  addText(s, "부산푸디투어는 테마 콘텐츠로 확인되어 Place 저장에서 제외", 770, 510, 430, 66, 18, C.coral, true);
  addText(s, "재수집 후 395 / 395 유지", 770, 596, 430, 34, 21, C.navy, true);
  note(s, "출처: docs/phase4-c3-tourism-data-scale-up.md, docs/phase4-c2-real-api-import-verification.md");
}

// 6. Database design
{
  const s = presentation.slides.add();
  s.background.fill = C.cream;
  title(s, 6, "데이터베이스 설계", "대표 장소·원천·회원 데이터·교통 그래프를 분리");
  flowBox(s, "members", 58, 220, 150, 62, C.navy, C.white);
  flowBox(s, "saved_courses", 280, 184, 190, 62, C.white);
  flowBox(s, "saved_course_places", 530, 184, 224, 62, C.teal2);
  flowBox(s, "trip_records", 280, 276, 190, 62, C.white);
  flowBox(s, "place_favorites", 280, 368, 190, 62, C.white);
  flowBox(s, "places", 822, 220, 150, 62, C.navy, C.white);
  flowBox(s, "place_sources", 1040, 176, 176, 62, C.white);
  flowBox(s, "import_runs", 1040, 264, 176, 62, C.white);
  flowBox(s, "merge_candidates", 1040, 352, 176, 62, C.white);
  line(s, 212, 250, 64, -34);
  line(s, 474, 216, 52, 0);
  line(s, 212, 250, 64, 54);
  line(s, 212, 250, 64, 150);
  line(s, 758, 216, 60, 34);
  line(s, 976, 250, 60, -44);
  line(s, 976, 250, 60, 132);
  addShape(s, "line", 76, 506, 1128, 0, "none", C.pale);
  flowBox(s, "bus_routes", 180, 552, 176, 62, C.white);
  flowBox(s, "bus_route_stops\n정차 순서", 502, 540, 218, 86, C.teal2);
  flowBox(s, "bus_stops", 870, 552, 176, 62, C.white);
  line(s, 360, 583, 138, 0);
  line(s, 724, 583, 142, 0);
  addText(s, "Flyway V1~V12", 1046, 574, 170, 30, 17, C.coral, true, "right");
  note(s, "출처: src/main/resources/db/migration/V1__create_places_table.sql부터 V12__scope_legacy_place_external_id_by_source.sql");
}

// 7. Algorithms
{
  const s = presentation.slides.add();
  s.background.fill = C.cream;
  title(s, 7, "추천과 룰렛 알고리즘", "점수의 근거를 유지하면서 seed 재현성과 코스 다양성을 확보");
  addText(s, "추천 점수", 66, 194, 240, 40, 25, C.navy, true);
  addText(s, "카테고리 30\n직선거리 30\n다양성 15\n데이터 품질 15\n검증된 교통 10", 68, 250, 260, 208, 22, C.gray, false);
  addShape(s, "roundRect", 354, 196, 364, 252, C.navy, C.navy, 20);
  addText(s, "후보 제한 200건\n상위 10건만 교통 평가\n가중 비복원 추출\n동일 seed 결과 재현", 392, 226, 288, 188, 22, C.white, true, "center");
  addText(s, "코스 순서", 780, 194, 260, 40, 25, C.navy, true);
  addText(s, "역할 그룹 novelty bonus\n최대 5곳, 최대 120개 순열\n음식점 연속 최소화\n거리 상한 안에서 순서 결정", 780, 250, 408, 180, 20, C.gray, false);
  addShape(s, "rect", 66, 500, 1120, 4, C.teal);
  addText(s, "교통 데이터가 부족하면 장소를 제거하지 않고 PARTIAL_DATA 또는 UNVERIFIED로 표시", 66, 538, 1120, 46, 22, C.coral, true, "center");
  addText(s, "실제 이동시간 · 요금 · 도로 최적 경로는 계산하지 않음", 66, 598, 1120, 36, 19, C.navy, true, "center");
  note(s, "출처: docs/phase3-b-recommendation-engine.md, docs/phase3-c-course-roulette.md, docs/phase5-b2-course-diversity-and-images.md");
}

// 8. Screens
{
  const s = presentation.slides.add();
  s.background.fill = C.navy2;
  addText(s, "08", 62, 36, 50, 28, 14, C.teal, true);
  addText(s, "핵심 구현 화면", 62, 70, 900, 54, 31, C.white, true);
  addText(s, "실제 Chromium QA 캡처", 64, 124, 700, 30, 15, "AFC0CC", false);
  addImage(s, "docs/qa/screenshots/05-personalized-recommendation-desktop.png", 62, 182, 350, 468, "Personalized recommendation results", { left: 0, top: 0.18, right: 0, bottom: 0.34 });
  addImage(s, "docs/qa/screenshots/06-course-roulette-desktop.png", 466, 182, 350, 468, "Course roulette results", { left: 0, top: 0.44, right: 0, bottom: 0.08 });
  addImage(s, "docs/qa/screenshots/08-saved-course-detail-desktop.png", 870, 182, 350, 468, "Saved course detail", { left: 0, top: 0.03, right: 0, bottom: 0.36 });
  addText(s, "추천", 166, 650, 140, 28, 16, C.white, true, "center");
  addText(s, "룰렛 코스", 570, 650, 140, 28, 16, C.white, true, "center");
  addText(s, "회원별 저장", 974, 650, 140, 28, 16, C.white, true, "center");
  addText(s, "8", 1182, 674, 42, 20, 11, "AFC0CC", false, "right");
  note(s, "출처: docs/qa/screenshots/05-personalized-recommendation-desktop.png, 06-course-roulette-desktop.png, 08-saved-course-detail-desktop.png");
}

// 9. Validation
{
  const s = presentation.slides.add();
  s.background.fill = C.cream;
  title(s, 9, "검증 결과", "코드 테스트와 실제 MySQL·Chromium·재시작 검증을 분리해 확인");
  addText(s, "324", 74, 210, 220, 92, 60, C.navy, true);
  addText(s, "Java 테스트\n실패 0 · 오류 0", 78, 310, 250, 70, 21, C.gray, true);
  addText(s, "42", 382, 210, 190, 92, 60, C.teal, true);
  addText(s, "Chromium QA 항목\n최종 통과", 386, 310, 240, 70, 21, C.gray, true);
  addText(s, "395", 690, 210, 210, 92, 60, C.coral, true);
  addText(s, "Place와 Source\n중복 원천 0", 694, 310, 240, 70, 21, C.gray, true);
  addText(s, "5~7분", 1000, 210, 210, 92, 47, C.gold, true);
  addText(s, "로컬 발표 흐름\n원클릭 실행", 1004, 310, 220, 70, 21, C.gray, true);
  addShape(s, "rect", 74, 426, 1130, 3, C.pale);
  addText(s, "MySQL 8.0.46 · Flyway V12", 74, 468, 420, 40, 23, C.navy, true);
  addText(s, "Spring Boot와 MySQL 재시작 후 데이터 보존", 74, 520, 500, 36, 18, C.gray, false);
  addText(s, "PNG 업로드·다운로드·권한·SHA-256 일치", 74, 566, 500, 36, 18, C.gray, false);
  addText(s, "반응형 1440px부터 360px까지 확인", 686, 468, 500, 40, 23, C.navy, true);
  addText(s, "제출용 public data 덤프를 새 V12 DB에 복원", 686, 520, 500, 36, 18, C.gray, false);
  addText(s, "회원·코스·즐겨찾기·여행기록 0건 확인", 686, 566, 500, 36, 18, C.gray, false);
  note(s, "출처: docs/phase5-a-browser-integration-qa.md, docs/phase5-d-final-demo-rehearsal.md, build/test-results/test");
}

// 10. Limits and next steps
{
  const s = presentation.slides.add();
  s.background.fill = C.navy2;
  addText(s, "10", 62, 38, 50, 28, 14, C.teal, true);
  addText(s, "한계와 발전 방향", 62, 76, 900, 56, 33, C.white, true);
  addText(s, "현재 확인된 범위를 정확히 표시하고 다음 데이터 확보 순서를 제안", 64, 134, 900, 34, 16, "AFC0CC", false);
  addText(s, "현재 한계", 72, 222, 260, 40, 25, C.teal, true);
  addText(s, "버스 100번 일부 20개 정차\n실시간 도착정보 없음\n시간·가격·운영시간 없음\n지도 타일은 인터넷 의존\n로컬 시연만 검증", 74, 286, 420, 240, 22, C.white, false);
  addText(s, "발전 방향", 670, 222, 260, 40, 25, C.coral, true);
  addText(s, "전체 정적 노선망 확보\n검증된 도보 네트워크 연동\n운영시간·가격 데이터 추가\n실시간 정보의 짧은 TTL 캐시\n배포 환경별 보안 검증", 672, 286, 470, 240, 22, C.white, false);
  addShape(s, "roundRect", 72, 594, 1070, 58, C.navy, "37566F", 14);
  addText(s, "로컬 시연  http://127.0.0.1:18090/", 94, 604, 1026, 38, 21, C.white, true, "center");
  addText(s, "10", 1182, 674, 42, 20, 11, "AFC0CC", false, "right");
  note(s, "출처: docs/final-project-report.md 8장과 9장, docs/phase5-d-final-demo-rehearsal.md");
}

await fs.mkdir(buildDir, { recursive: true });
await fs.mkdir(outputDir, { recursive: true });

for (const [index, slide] of presentation.slides.items.entries()) {
  const png = await presentation.export({ slide, format: "png", scale: 1 });
  await fs.writeFile(path.join(buildDir, `slide-${String(index + 1).padStart(2, "0")}.png`), new Uint8Array(await png.arrayBuffer()));
}
const montage = await presentation.export({ format: "webp", montage: true });
await fs.writeFile(path.join(buildDir, "deck-montage.webp"), new Uint8Array(await montage.arrayBuffer()));

const { finalizePresentation } = await import(pathToFileURL(path.join(skillDir, "container_tools/artifact_tool_utils.mjs")).href);
const stagingDir = path.join(buildDir, ".finalizer");
await fs.mkdir(stagingDir, { recursive: true });
const candidatePath = path.join(stagingDir, "candidate.pptx");
await (await PresentationFile.exportPptx(presentation)).save(candidatePath);

const result = await finalizePresentation({
  explicitTotalSlideCount: 10,
  requiredNativeTableOwnerSlides: [],
  requiredNativeChartOwnerSlides: [],
  workspaceDir,
  candidatePath,
  finalPath,
  pythonExecutable,
  integrityValidatorPath: path.join(skillDir, "container_tools/inspect_presentation_package_integrity.py"),
  layoutValidatorPath: path.join(skillDir, "container_tools/inspect_presentation_layout_geometry.py"),
  layoutArgs: ["--expected-slide-size-emu", "12192000,6858000", "--validate-heading-fit"],
  fontPolicy: { basis: "design", families: [font] },
  verifyArtifactToolImport: true,
  receiptPath: path.join(stagingDir, "city-roulette-final-presentation-v2.validation.json"),
});

console.log(JSON.stringify({ finalPath, slideCount: presentation.slides.items.length, result }, null, 2));
