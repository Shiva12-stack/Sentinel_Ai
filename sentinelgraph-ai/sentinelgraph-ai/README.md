# SentinelGraph AI · Digital Threat Observatory

A local hackathon application for social-profile, company-page, scam-account and mobile-app impersonation investigations. Existing URL analysis, findings, cases and reports are retained. The new observatory uses React/Vite + Java 21 Spring Boot + PostgreSQL. The retained frontend is Vite, not Next.js; no framework migration is necessary to extend this archive.

## Run on Windows — easiest route

1. Install and start Docker Desktop with Linux containers.
2. Extract this ZIP. Open a terminal inside `sentinelgraph-ai` (the folder containing `docker-compose.yml`).
3. Run:

```powershell
docker compose up --build
```

4. Wait until PostgreSQL is healthy and Spring Boot reports `Started SentinelGraphApplication`.
5. Open **http://localhost:5173/observatory**. Java API is at **http://localhost:8080/api/v1/observatory**.

`start-all.bat` runs the same command. First startup downloads images and dependencies, so an internet connection is required. If a port is occupied, stop the previous SentinelGraph instance. PostgreSQL is created automatically inside the `postgres` container; its named volume preserves data after stopping.

Stop with Ctrl+C, then `docker compose down`. Do not use `down -v` to reset demos: that removes the entire database volume. Use the guided reset screen instead.

## Run without containerizing the app

Requirements: Java **21 JDK**, Maven 3.9+, Node 22+ and PostgreSQL 16 (or Docker Desktop for the database).

```powershell
docker compose up -d postgres
cd backend
mvn spring-boot:run
```

In a second terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Default database: `jdbc:postgresql://localhost:5432/sentinelgraph`; username `sentinelgraph`; development password `sentinelgraph_dev_only`. Override `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` as needed. Defaults bind via Docker only to localhost. Do not deploy with these development credentials.

## What is implemented

| Workflow | Backend route suffix under `/api/v1/observatory` | Behavior |
|---|---|---|
| Brand profile | `POST /brands`, `POST /assets` | Brand names and official social/app identifiers, logo URL, publisher, descriptions and source URLs |
| Social/app detection | `POST /scans` | Processes imported source metadata with the actual Java detector; stable IDs deduplicate user observations within brand/platform/provenance |
| Evidence locker | `GET /evidence` | Persisted observation snapshots with source, ingestion timestamp, metadata, matching signals and provenance |
| Comparison lab | `GET /compare?candidateId=…&assetId=…` | Official/suspected fields, unavailable metadata, signal points and linked cases |
| Accuracy lab | `POST /test` | NFKC normalization, limited homoglyph mapping, Levenshtein, raw Unicode differences and official identifier exclusion; no findings are created |
| Threat clusters | `GET /clusters` | Connected groups across platforms from a shared supplied contact or non-generic outbound host; every relationship is explained |
| History | `GET /history` | NEW, UNCHANGED, CHANGED and RESOLVED snapshots with field-level before/after differences |
| Exposure | `GET /brands/{id}/exposure` | Separate synthetic/imported aggregate scores with explicit observed coverage |
| Analyst decisions | `POST /candidates/{id}/feedback`, `GET /decisions` | Confirmed, dismissed, allowlisted and unreviewed decisions stored as append-only audit entries |
| Cases/reports | `POST /candidates/{id}/cases`, `GET /cases/{id}/report` | Reports include evidence snapshots and analyst investigation history; JSON download |
| Guided reset | `POST /demo/reset` | Confirmation phrase required; deletes only designated seed rows and restores fictional fixtures |

All new screens use real API calls. There is no frontend fallback to fabricated observations when the API is unavailable. Legacy URL pages retain a clearly labeled browser demo mode; Java API mode is now the default.

## Evidence, detection and scoring

Provenance accepted by ingestion is `MANUAL_IMPORT` or `SYNTHETIC`. Manually imported metadata has **not** been independently verified and is not labeled LIVE. No live connectors or screenshot capture are configured. Source URLs and logo URLs are stored as metadata, not fetched; exact logo-URL equality is explicitly distinguished from image-content analysis.

Character similarity: NFKC → lowercase → limited Cyrillic/Greek/Latin confusable mapping → remove punctuation/spacing → Levenshtein. Similarity is `100 × (1 − editDistance / max(normalized lengths, 1))`. Raw positional character differences include Unicode code points; these are not an edit-alignment visualization. Added trailing words are a separate name signal. Confusable coverage is limited, not universal.

Candidate risk: name resemblance up to 40 points, exact supplied official logo-URL match 25, explicit credential/reward/payment phrases 25, conflicting supplied app publisher 10. Names shorter than four normalized characters receive no name points. Official exclusion requires the same brand, platform and exact stable identifier. Score zero is not a safety guarantee. Confirmed decisions do not artificially inflate the evidence-based score.

Exposure: `round(100 × (1 − exp(−Σ active candidate risk / 200)))`. Resolved, dismissed and allowlisted candidates are excluded. Synthetic and manually imported scores are separate. Live score is UNAVAILABLE, connected live sources are zero, observed platforms are listed, and missing metadata remains unknown. This score does not measure the entire internet.

Resolution: partial scans never mark unseen candidates resolved. A complete source snapshot explicitly declared by the analyst may mark missing user observations resolved within the same brand/platform/provenance. The single-observation UI explains that enabling this checkbox declares that **one record** to be the complete snapshot. Designated demo seeds are protected from user imports and absence resolution.

Relationships: same supplied contact or outbound domain can support a group; neither proves common ownership. Generic platform/link hosts are ignored. Synthetic and imported records never cluster together.

Feedback: decisions match stable candidate identity across scans. Most recent analyst decision wins; `UNREVIEWED` revokes an earlier suppression. No adaptive ML is implemented. The local actor is labeled `LOCAL_ANALYST`, not an authenticated identity.

Reset: only rows marked `demoSeed=true` are reset. User-created synthetic records, production/user brands, assets, observations, evidence, cases, decisions and reset audit history remain. Seed IDs are stable so reports and decisions linked to seeded candidates survive. The original H2 database is not migrated or erased; PostgreSQL starts independently.

## Architecture and persistence

See [ARCHITECTURE.md](ARCHITECTURE.md). `observatory_records` stores typed, versioned documents with kind indexes and SQL text payloads. Legacy URL analyses and investigation tables remain. Service mutations are transactional; evidence and decisions have no update/delete API. Hibernate `ddl-auto=update` is a development convenience, not a production migration strategy.

## Tests

```powershell
cd backend
mvn test
```

The workflow suite uses H2 in PostgreSQL compatibility mode for isolated tests. It covers manual-test separation, confusables, official exclusions, evidence/report/comparison, historical resolution, evidence-only clusters, analyst reuse, exposure and safe reset. H2 is test infrastructure; the application defaults to PostgreSQL.

With a running local PostgreSQL:

```powershell
$env:RUN_POSTGRES_TESTS="true"
mvn test
```

Run actual browser workflows with PostgreSQL and Java API already running:

```powershell
cd frontend
npm ci
npx playwright install chromium
npm run test:browser
```

Browser tests use the real backend; no mocked responses. Use a dedicated development/test database because browser tests intentionally create records and reset synthetic seeds. The reset preservation test verifies a user brand remains. See [VALIDATION.md](VALIDATION.md) for checks actually executed in the build environment.

## Scope and deployment limits

This is a local hackathon build. API endpoints do not have authentication, organization isolation or enterprise authorization. Keep it on localhost. Live social/app discovery, screenshot permission/capture, image-content similarity, full homoglyph coverage, production migrations, scheduler integrations and authenticated analyst identities require additional implementation. No autonomous takedown or platform reporting is claimed.

The archive retains prior URL analysis workflows; their offline intelligence is distinct from the new social/app observation workflow. Native hosted Sites use a different server runtime, so this Java/PostgreSQL project is delivered for localhost as requested in the existing project context.

## Vibe coding handoff

The source is editable in Google Antigravity or another coding agent. Use [VIBE_CODING.md](VIBE_CODING.md) for a focused validation and iteration prompt. Review the generated changes, run the actual tests, and use the documented evidence limits in your presentation. Do not claim that automated coding makes unverified integrations live.
