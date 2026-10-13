import type { Analysis, CaseItem, AlertItem, DashboardStats, Evidence, Verdict } from './types'

const STORAGE = 'sentinelgraph_demo_analyses_v1'
const CASES = 'sentinelgraph_demo_cases_v1'
const ALERTS = 'sentinelgraph_demo_alerts_v1'
const now = () => new Date().toISOString()
export const demoDomains = [
  { brand: 'ShopKart', domain: 'shopkart.example' },
  { brand: 'NovaBank', domain: 'novabank.example' },
  { brand: 'CloudCart', domain: 'cloudcart.example' },
  { brand: 'QuickPay', domain: 'quickpay.example' },
]
const seeded = [
  'shopkart.example','shopkart-secure-login.example','novabank-auth.example',
  'cloudcart.example','unknown-service.example','quickpay-bonus.example',
].map((url,i) => demoAnalysis(url,`DEMO-${String(i+1).padStart(3,'0')}`,new Date(Date.now()-(i*7200000)).toISOString()))

export function demoAnalysis(input: string, id = `DEMO-${Date.now()}`, createdAt = now()): Analysis {
  const clean = input.trim()
  const full = /^[a-z][a-z\d+.-]*:\/\//i.test(clean) ? clean : `https://${clean}`
  let parsed: URL
  try { parsed = new URL(full) } catch { throw new Error('Enter a valid website address, such as shopkart.example.') }
  if (!['https:', 'http:'].includes(parsed.protocol) || !parsed.hostname || parsed.username || parsed.password) throw new Error('Only HTTP(S) website URLs without embedded credentials are supported.')
  const hostname = parsed.hostname.replace(/\.$/, '').toLowerCase()
  if (!/^[a-z\d.-]+$/i.test(hostname) || !hostname.includes('.')) throw new Error('Enter a valid domain name.')
  const registrableDomain = hostname.endsWith('.example') ? hostname.split('.').slice(-2).join('.') : hostname.split('.').slice(-2).join('.')
  // Browser-only demonstration intentionally does NOT claim full public suffix accuracy.
  const match = demoDomains.find(ref => hostname.includes(ref.brand.toLowerCase()) || hostname.replace(/[-_]/g,'').includes(ref.brand.toLowerCase()))
  const claimedBrand = match?.brand || null
  const referenceDomain = match?.domain || null
  const exact = !!match && (hostname === match.domain || hostname.endsWith(`.${match.domain}`))
  const mimic = !!match && !exact
  let verdict: Verdict = 'INCONCLUSIVE'
  if (exact) verdict = 'VERIFIED_REFERENCE_MATCH'
  else if (mimic) verdict = 'SUSPICIOUS'
  const evidence: Evidence[] = [
    {ruleId:'DOMAIN_PARSED',title:'Hostname identified',description:`Submitted hostname: ${hostname}`,source:'Local URL parser (demo)',kind:'neutral',observedAt:createdAt},
  ]
  if (exact) evidence.push({ruleId:'DEMO_REFERENCE_MATCH',title:'Fictional reference match',description:`Hostname matches the fictional ${claimedBrand} domain reference. This is not a real-world ownership verification.`,source:'Seeded demonstration catalog',kind:'positive',observedAt:createdAt})
  if (mimic) evidence.push({ruleId:'DEMO_BRAND_RESEMBLANCE',title:'Brand-name resemblance',description:`The domain contains a name resembling ${claimedBrand}, but differs from ${referenceDomain}. A different domain alone does not prove impersonation.`,source:'Deterministic demo rule',kind:'warning',observedAt:createdAt})
  if (/login|secure|auth|bonus|reward|verify|update/i.test(hostname)) evidence.push({ruleId:'SENSITIVE_WORDS',title:'Attention-worthy domain wording',description:'The domain contains sign-in, verification, or promotional wording that can warrant investigation when combined with other evidence.',source:'Deterministic demo rule',kind:'warning',observedAt:createdAt})
  if (!match) evidence.push({ruleId:'NO_REFERENCE',title:'No reference available',description:'No matching brand was found in this small fictional demonstration catalog.',source:'Seeded demonstration catalog',kind:'neutral',observedAt:createdAt})
  // Local demo uses a deliberately simple heuristic, not live reputation data.
  let score = mimic ? 55 : 0
  if (/login|secure|auth|bonus|reward|verify|update/i.test(hostname)) score += 15
  if (/^(com|net|org|co|io)\./i.test(hostname)) score += 35
  if (parsed.protocol === 'http:') score += 10
  if (hostname.includes('xn--')) score += 15
  if (/verify-password|send-otp|password-reset|claim-reward/i.test(parsed.pathname)) score += 15
  score = exact ? 0 : Math.min(100, score)
  if (mimic && score >= 90) verdict = 'LIKELY_IMPERSONATION'
  else if (mimic || score >= 35) verdict = 'SUSPICIOUS'
  evidence.push({ruleId:'DEMO_RISK_SCORE',title:`Heuristic URL risk score: ${score}/100`,description:'Calculated only from demo URL text rules. Not a fraud probability or safety verification.',source:'Local demo rules',kind:score>=35?'warning':'neutral',observedAt:createdAt})
  return {id,submittedUrl:`${parsed.protocol}//${hostname}${parsed.pathname}`,normalizedUrl:`${parsed.protocol}//${hostname}${parsed.pathname}`,hostname,registrableDomain,claimedBrand,referenceDomain,verdict,threatStatus:'NOT_CHECKED',score,analysisMode:'DEMO_LOCAL_ONLY',evidence,limitations:['Fictional demonstration catalog; no real-world official ownership verification.','No external threat intelligence or DNS/RDAP lookup was performed.','Website content and redirects were not inspected.','Browser demo domain parsing does not cover all public suffixes.'],createdAt}
}
function read<T>(key:string,fallback:T):T { try { const raw=localStorage.getItem(key);return raw?JSON.parse(raw) as T:fallback } catch {return fallback} }
function write<T>(key:string,data:T){localStorage.setItem(key,JSON.stringify(data))}
export function getDemoAnalyses():Analysis[]{ return read(STORAGE,seeded) }
export function getDemoAnalysis(id:string):Analysis|undefined { return getDemoAnalyses().find(a=>a.id===id) }
export function saveDemoAnalysis(input:string):Analysis { const item=demoAnalysis(input);write(STORAGE,[item,...getDemoAnalyses()]);return item }
export function getDemoCases():CaseItem[]{return read<CaseItem[]>(CASES,[{id:'CASE-001',title:'Review possible ShopKart lookalike',priority:'HIGH',status:'INVESTIGATING',analysisId:'DEMO-002',notes:'Training case using a fictional .example domain.',createdAt:new Date(Date.now()-36000000).toISOString()}])}
export function saveDemoCase(analysisId:string,title:string,priority:CaseItem['priority']='MEDIUM'):CaseItem {const item={id:`CASE-${Date.now()}`,title,priority,status:'NEW',analysisId,notes:'Created in local demo mode.',createdAt:now()};write(CASES,[item,...getDemoCases()]);return item}
export function updateDemoCase(id:string,status:string) { write(CASES,getDemoCases().map(c=>c.id===id?{...c,status}:c)) }
export function getDemoAlerts():AlertItem[]{return read<AlertItem[]>(ALERTS,[{id:'ALERT-001',title:'Lookalike candidate flagged',description:'Fictional ShopKart-like domain requires investigation.',severity:'HIGH',createdAt:new Date(Date.now()-3600000).toISOString(),acknowledged:false,analysisId:'DEMO-002'},{id:'ALERT-002',title:'Demo discovery batch ingested',description:'Three synthetic domains added to demonstration data.',severity:'INFO',createdAt:new Date(Date.now()-15000000).toISOString(),acknowledged:false}])}
export function acknowledgeDemoAlert(id:string){write(ALERTS,getDemoAlerts().map(a=>a.id===id?{...a,acknowledged:true}:a))}
export function getDemoStats():DashboardStats {const all=getDemoAnalyses();return {total:all.length,suspicious:all.filter(a=>a.verdict==='SUSPICIOUS').length,impersonation:all.filter(a=>a.verdict==='LIKELY_IMPERSONATION').length,verified:all.filter(a=>a.verdict==='VERIFIED_REFERENCE_MATCH').length,inconclusive:all.filter(a=>a.verdict==='INCONCLUSIVE').length,openCases:getDemoCases().filter(c=>!['RESOLVED','FALSE_POSITIVE'].includes(c.status)).length,recent:all.slice(0,6)}}
export function runDemoDiscovery():number {const names=['shopkart-rewards.example','novabank-secure.example','quickpay-verify.example'];const current=getDemoAnalyses();const seen=new Set(current.map(a=>a.hostname));const additions=names.filter(n=>!seen.has(n)).map(n=>demoAnalysis(n));write(STORAGE,[...additions,...current]);return additions.length}
