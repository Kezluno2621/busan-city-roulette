import http from "node:http";

const host = "127.0.0.1";
const port = Number(process.env.QA_KAKAO_STUB_PORT || 18082);

const response = JSON.stringify({
  documents: [
    {
      id: "qa-busan-station",
      place_name: "부산역",
      category_name: "교통,수송 > 기차역",
      address_name: "부산광역시 동구 초량동 1187-1",
      road_address_name: "부산광역시 동구 중앙대로 206",
      x: "129.040276",
      y: "35.115225"
    }
  ],
  meta: { is_end: true, pageable_count: 1, same_name: null, total_count: 1 }
});

const server = http.createServer((request, result) => {
  const url = new URL(request.url, `http://${request.headers.host}`);
  if (request.method === "GET" && url.pathname === "/v2/local/search/keyword.json") {
    result.writeHead(200, { "Content-Type": "application/json; charset=utf-8" });
    result.end(response);
    return;
  }
  result.writeHead(404, { "Content-Type": "application/json; charset=utf-8" });
  result.end(JSON.stringify({ message: "QA stub route not found" }));
});

server.listen(port, host, () => {
  console.log(`Kakao Local QA stub listening on http://${host}:${port}`);
});

for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, () => server.close(() => process.exit(0)));
}
