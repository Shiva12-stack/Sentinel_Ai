# Validation of this delivery

## Executed successfully

- Frontend dependency installation and `npm run build`: strict TypeScript compilation and Vite production bundling.
- `npx playwright test --list`: nine real-backend browser workflows discovered and test/config syntax loaded successfully. Discovery is not browser execution.
- All 19 Java source/test files parsed successfully during development.
- Java similarity algorithm compiled and executed directly: Cyrillic homoglyph 100%, spacing 100%, numeric substitution 87.5%, added-word example 53.33%. Added words are explicitly considered by the scan risk rule.
- Maven backend compilation and isolated service/API tests: **15 passed**, **1 skipped**. Includes the 10 new observatory service/API workflows and five retained URL tests. Tests used H2 with PostgreSQL compatibility mode.
- Palette scan found no remaining saturated blue/cyan hex colors in the application source; legacy CSS, graph/chart colors and WebGL globe were recolored to warm hues.

## Verification limits

The host provides Java 17, so backend validation used `-Djava.version=17`. The delivered Maven project and Dockerfile target **Java 21**. No Java-21-specific APIs are used by these extensions, but a Java 21 build was not executed here.

PostgreSQL integration test `ObservatoryPostgresTest` was skipped because no PostgreSQL server was available. Enable `RUN_POSTGRES_TESTS=true` on your machine to run it against the configured PostgreSQL development database.

Browser tests were authored and discovered but not executed. The required managed browser-control capability was unavailable; no visual browser QA or browser screenshots were produced. Start the local stack and run `npm run test:browser` with Playwright Chromium installed to verify real interactions.

Docker Compose startup/image builds were not executed because Docker is unavailable on the host. The included Dockerfiles and Compose file provide the Java 21/PostgreSQL local run path and still need execution on your machine.

The frontend bundle retains the original 3D/graph/chart libraries and produces a large-chunk advisory. Build succeeds; optional lazy loading can reduce first-load size.

## What the results do not claim

No live social/app-feed connections, screenshot capture, image-content similarity, common-ownership attribution, adaptive ML, enterprise authentication or production deployment were verified or claimed. The application labels evidence provenance and unavailable source coverage.
