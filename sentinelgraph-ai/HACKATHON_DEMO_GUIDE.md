# Five-minute hackathon demo

Start with `docker compose up --build`, then open `http://localhost:5173/observatory`.

1. **30 seconds — coverage and brand identity.** Show the synthetic/imported badges, zero connected live sources and UNAVAILABLE live exposure. Say: “We monitor supplied observations in this prototype; production connectors are not configured.” Open Brand assets to show NovaBank's fictional official social and app identifiers.
2. **45 seconds — social/app observations.** Show prioritized candidates. Explain that the fictional NovaBank account and rewards app have evidence-based risk signals. To demonstrate real persistence, import a MANUAL_IMPORT observation: name `NovaBank Support`, identifier `demo-user-support`, source `https://candidate.example/support`, description `Claim reward; send OTP`. Do not enable complete snapshot for a partial import.
3. **40 seconds — comparison.** Compare a synthetic candidate with its official brand asset. Show names, logo URLs, publisher, identifiers and missing fields. Explain each risk component; a logo URL match is not an image-analysis result.
4. **35 seconds — accuracy lab.** Test `NovaBank` against `NоvaBank` (the second character is Cyrillic), `Nova Bank`, `N0vaBank` and `NovaBank Support`. Show the Unicode differences and deterministic score. For official exclusion choose Instagram and stable identifier `novabank-official`. State that manual tests do not become findings.
5. **35 seconds — evidence and change history.** Open a stored snapshot. Show source, ingestion timestamp, provenance, metadata and signals. Re-import `demo-user-support` with a changed description and show CHANGED metadata in history. Explain that a partial scan never resolves unseen findings.
6. **30 seconds — clusters and exposure.** Show the synthetic account/app group. Explain the shared contact and outbound host; do not claim shared ownership. Show the exposure formula and separated provenance scores.
7. **45 seconds — analyst workflow and report.** Record a confirmed or allowlisted decision with a reason, create an investigation and export its JSON evidence report. A later scan reuses the explicit decision; this is not adaptive machine learning.
8. **20 seconds — safe reset.** Open Demo reset, type `RESET SYNTHETIC DEMO`, and reset. Show that the manually imported observation, analyst audit and case still exist. Only designated synthetic seeds were restored.

## Questions judges may ask

- **Is it live monitoring?** No live connectors are configured. This implementation stores and analyzes manual source imports and isolated synthetic demonstrations.
- **Are screenshots collected?** No. The evidence schema labels capture unavailable. We will add platform-permitted capture only with a functioning connector.
- **Why is an account suspicious?** Show its individual signal points and metadata. Name resemblance is a screening signal, not proof of maliciousness.
- **Does clustering prove ownership?** No. Every edge identifies the actual shared supplied indicator and states the attribution limit.
- **Is score zero safe?** No. Missing source data and unavailable integrations remain unknown.
- **Does analyst feedback train a model?** No. It stores an explicit, audited decision applied to the stable entity on future observations.
- **What is connected?** Every new workflow calls the Spring Boot API, which persists to PostgreSQL. The browser does not substitute invented results if the API is down.

Use fictional `.example` URLs for demonstration. Do not present fictional assets, synthetic observations or unverified imported metadata as independently verified live threats.
