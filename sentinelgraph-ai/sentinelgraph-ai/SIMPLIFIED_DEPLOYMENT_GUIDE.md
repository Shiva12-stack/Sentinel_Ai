# SentinelGraph AI — focused risk-checking update

## What changed (Vercel React + Render Java + Render PostgreSQL)

- Observatory shows **Overview / Brand assets / Social & apps** only. Old observatory sub-page URLs redirect to Social & apps.
- Brand registry and official assets are not listed in the page after saving. The forms clear upon successful submission. Platform is entered as free text. The Observatory overview API returns counts and chart data rather than the full brand/asset registry.
- Social & apps accepts a source URL without requiring a brand selection. The backend compares it with saved brand references by official source URL and a cautious normalized brand-name match. Platform is inferred for supported hosts and can be manually specified.
- Current assessment only: big risk score, clear verdict, supporting matching signals. No raw JSON response panel and no historical findings table on this page.
- Observatory Overview shows aggregate risk and platform bar charts; demo seed entries are excluded from those charts. Main Command Overview has category graphs instead of recent-risk or recent-investigation lists.
- Previous analysis records are retained in PostgreSQL, including evidence for auditability. Removing a screen does not delete the underlying records or endpoints.

## Important limitations

- Analyses examine **submitted text and registered metadata**; they DO NOT fetch or verify Instagram, app stores, or arbitrary websites. A risk score is a heuristic priority score, not a fraud probability.
- A low score means **insufficient evidence of danger**, not verification that a URL/account is safe. The new screen labels this explicitly.
- Brand records are hidden from the page but NOT protected from direct API access. The prototype still lacks login, authorization, multi-tenant isolation, and rate limiting. Do not upload real customer or corporate records until access controls are added.
- Use only synthetic test data in your public hackathon environment. The `Synthetic demonstration data` checkbox is selected by default.
- JSON configuration files such as `package.json` and `vercel.json` remain necessary for build/deployment; **the raw JSON investigation result is removed from the UI**.

## Update your VS Code repository

1. Make a backup of your current project, including your `.env` secrets stored locally (never commit those).
2. Extract `SentinelGraph_AI_Focused_Replacement.zip` **into your Git repository root** (the folder containing `sentinelgraph-ai/`). When prompted, overwrite the listed code files.
3. If your project folder itself is the repository root, extract into its parent directory so you do not create `sentinelgraph-ai/sentinelgraph-ai/`.
4. Check `git status`, review `git diff`, then run:

```powershell
git add sentinelgraph-ai
git commit -m "Simplify observatory and add automatic brand reference risk checks"
git push origin main
```

5. For the existing Vercel frontend, the Root Directory stays `sentinelgraph-ai/frontend`. `frontend/vercel.json` forwards `/api/*` to `https://sentinel-ai-tool.onrender.com`.
6. In Vercel → Settings → Environment Variables, REMOVE old `VITE_API_BASE_URL` if set; the frontend now always uses same-origin `/api` requests through the rewrite.
7. For Render, Root Directory is `sentinelgraph-ai/backend`, Dockerfile `./Dockerfile`. Ensure the existing PostgreSQL variables `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` remain configured. Vercel and Render should rebuild from the latest Git commit if Auto Deploy is enabled.
8. Test `https://sentinel-ai-tool.onrender.com/api/v1/health` and your Vercel deployment `/api/v1/health` after both builds complete.
9. Register a synthetic brand, save an official asset, and test a fictional candidate such as `https://www.instagram.com/foodrush_free_coupon_demo/` with name `FoodRush Free Coupon Support` and description `Claim reward; verify account`. Confirm that the result is saved and scored, then refresh the page to see the current-only results disappear but aggregate dashboard totals remain.

## Changed files

- `frontend/src/pages/Observatory.tsx`
- `frontend/src/pages/Pages.tsx`
- `frontend/src/observatory.css`
- `frontend/src/styles.css`
- `frontend/src/App.tsx`
- `frontend/src/lib/backendUrl.ts`
- `frontend/vercel.json`
- `frontend/tests/observatory.spec.ts`
- `backend/src/main/java/com/sentinelgraph/observatory/ObservatoryService.java`
- `backend/src/main/java/com/sentinelgraph/observatory/ObservatoryController.java`
- `backend/src/test/java/com/sentinelgraph/observatory/ObservatoryWorkflowTest.java`

## Checks executed in this environment

- Java API source **compiled with local stubs** for Spring/Jackson interfaces, verifying Java 21 syntax/type consistency in the modified files. This is not an application build.
- Modified TypeScript/TSX files passed parser syntax checks.
- A full `npm` dependency installation and React build did not complete in this environment. Maven/Java integration tests and deployment tests still need to run on your machine/CI.
