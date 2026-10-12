# Vibe coding in Google Antigravity

Open the extracted `sentinelgraph-ai` folder. This is an actual source project; pasting the prompt below into an agent on its own does not start services on your computer.

Paste this focused continuation prompt:

> Inspect README.md, ARCHITECTURE.md, HACKATHON_DEMO_GUIDE.md and VALIDATION.md. Work on this existing SentinelGraph AI project. Preserve React/Vite, Java 21 Spring Boot and PostgreSQL. Keep the charcoal/copper/amber palette; no blue colors. First start the local stack and run backend tests and real-backend Playwright workflows. Correct observed failures without replacing API data with mock results. Preserve the eight observatory workflows and the existing URL functionality. Do not label imports as live, invent screenshots, infer ownership from name similarity, or delete user records on reset. Keep designated demo seed records isolated from user imports. Update documentation and report the exact checks that passed. Complete one verified change at a time; do not deploy until local tests pass.

Then run `docker compose up --build` from the folder containing Compose. Open `http://localhost:5173/observatory`. Use the demo guide to verify behavior yourself. Provide the coding agent the actual terminal error if startup fails.
