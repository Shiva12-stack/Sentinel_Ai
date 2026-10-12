export type Verdict = 'VERIFIED_REFERENCE_MATCH' | 'LIKELY_IMPERSONATION' | 'SUSPICIOUS' | 'INCONCLUSIVE'
export type Evidence = { id?: number; ruleId: string; title: string; description: string; source: string; kind: 'positive' | 'warning' | 'neutral'; observedAt?: string }
export type Analysis = { id: string; submittedUrl: string; normalizedUrl: string; hostname: string; registrableDomain: string; claimedBrand: string | null; referenceDomain: string | null; verdict: Verdict; threatStatus: string; score: number | null; analysisMode: string; evidence: Evidence[]; limitations: string[]; createdAt: string }
export type DashboardStats = { total: number; suspicious: number; impersonation: number; verified: number; inconclusive: number; openCases: number; recent: Analysis[] }
export type CaseItem = { id: string; title: string; priority: 'LOW'|'MEDIUM'|'HIGH'; status: string; analysisId: string; notes: string; createdAt: string }
export type AlertItem = { id: string; title: string; description: string; severity: string; createdAt: string; acknowledged: boolean; analysisId?: string }
