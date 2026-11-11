# Phase 5-A local QA helper

`kakao-local-stub.mjs` is a localhost-only Kakao Local fixture used to test the
recommendation and course UI without copying a real API key into the terminal.
It does not replace an actual Kakao API connectivity test.

```powershell
node e2e/kakao-local-stub.mjs
```

Start the application with the QA-only overrides below in addition to the
isolated MySQL environment variables:

```text
--kakao.local.base-url=http://127.0.0.1:18082
--kakao.local.rest-api-key=qa-local-stub
```

The stub binds to `127.0.0.1:18082`, returns one Busan Station fixture, and
must never be enabled in a production run configuration.
