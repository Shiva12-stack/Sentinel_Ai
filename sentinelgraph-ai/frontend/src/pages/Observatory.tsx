import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Activity, BarChart3, CheckCircle2, Search, ShieldCheck, ShieldAlert } from 'lucide-react'
import { ConsoleShell } from '../components/UI'
import RiskGauge from '../components/RiskGauge'
import { obs, type RecordData } from '../lib/observatory'

type AnalysisResult = {
  id: string
  name: string
  sourceUrl: string
  platform: string
  matchedBrand: string
  coverage: string
  risk: { score: number; verdict: string; riskLevel: string; signals: Array<{ signal: string; points: number }> }
}
const tabs = [
  { url: '/observatory', label: 'Overview', icon: BarChart3 },
  { url: '/observatory/brand', label: 'Brand assets', icon: ShieldCheck },
  { url: '/observatory/monitor', label: 'Social & apps', icon: Search },
]
function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return <section className="obs-card"><h2>{title}</h2>{children}</section>
}
function Field({ label, value, onChange, required = false, placeholder = '' }: {
  label: string; value: string; onChange: (value: string) => void; required?: boolean; placeholder?: string
}) {
  return <label>{label}<input value={value} onChange={e => onChange(e.target.value)} required={required} placeholder={placeholder} /></label>
}
function ScorePanel({ result }: { result: AnalysisResult }) {
  const risk = result.risk
  const score = risk.score
  const concern = score >= 35 || ['SUSPICIOUS', 'LIKELY_IMPERSONATION'].includes(risk.verdict)
  return <div className={`obs-score-panel ${concern ? 'obs-high' : 'obs-uncertain'}`} role="status">
    <div className="obs-score-head"><ShieldAlert size={24}/><span>Current risk analysis</span></div>
    <RiskGauge score={score} verdict={risk.verdict}/>
    <p className="obs-score-identity"><strong>{result.name}</strong> · {result.platform}</p>
    <p><strong>Reference comparison:</strong> {result.matchedBrand}</p>
    <p><strong>Submitted URL:</strong> {result.sourceUrl}</p>
    {risk.signals?.length > 0 ? <div className="obs-signal-list"><h4>Why this score?</h4>{risk.signals.map((signal, i) => <div key={i}><span>{signal.signal}</span><b>+{signal.points}</b></div>)}</div> : <p>No definite warning indicators were detected from the supplied information.</p>}
    <p className="obs-muted"><strong>Coverage:</strong> {result.coverage} A low score is not a safety guarantee.</p>
  </div>
}
export default function Observatory() {
  const path = useLocation().pathname
  const [overview, setOverview] = useState<RecordData | null>(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const [result, setResult] = useState<AnalysisResult | null>(null)
  const [brandName, setBrandName] = useState('')
  const [assetBrand, setAssetBrand] = useState('')
  const [assetPlatform, setAssetPlatform] = useState('')
  const [assetName, setAssetName] = useState('')
  const [assetIdentifier, setAssetIdentifier] = useState('')
  const [assetUrl, setAssetUrl] = useState('')
  const [assetUsername, setAssetUsername] = useState('')
  const [assetPublisher, setAssetPublisher] = useState('')
  const [assetDescription, setAssetDescription] = useState('')
  const [assetLogo, setAssetLogo] = useState('')
  const [sourceUrl, setSourceUrl] = useState('')
  const [platform, setPlatform] = useState('')
  const [name, setName] = useState('')
  const [username, setUsername] = useState('')
  const [publisher, setPublisher] = useState('')
  const [description, setDescription] = useState('')
  const [outboundUrl, setOutboundUrl] = useState('')
  const [synthetic, setSynthetic] = useState(true)

  useEffect(() => { setResult(null); setMessage(''); setError('') }, [path])

  useEffect(() => {
    if (path !== '/observatory') return
    obs().then(setOverview).catch(e => setError(e instanceof Error ? e.message : String(e)))
  }, [path])

  async function submit(task: () => Promise<unknown>, success: string): Promise<boolean> {
    setBusy(true); setError(''); setMessage('')
    try { await task(); setMessage(success); return true }
    catch (e) { setError(e instanceof Error ? e.message : String(e)); return false }
    finally { setBusy(false) }
  }
  const total = Number(overview?.total || 0)
  const high = Number(overview?.high || 0)
  const medium = Number(overview?.medium || 0)
  const low = Number(overview?.low || 0)
  const riskGroups = [{ label: 'High risk', count: high, className: 'high' }, { label: 'Medium risk', count: medium, className: 'medium' }, { label: 'Low / inconclusive', count: low, className: 'low' }]
  const selected = tabs.find(t => t.url === path) || tabs[0]
  return <ConsoleShell title={selected.label === 'Overview' ? 'Threat Observatory' : selected.label} subtitle="Digital Threat Observatory · brand identity and risk signals">
    <div className="obs">
      <nav className="obs-tabs">{tabs.map(({url, label, icon: Icon}) => <Link key={url} to={url} className={selected.url === url ? 'selected' : ''}><Icon size={15}/>{label}</Link>)}</nav>
      {error && <div role="alert" className="obs-error">{error}</div>}
      {message && <div role="status" className="obs-notice"><CheckCircle2 size={17}/>{message}</div>}
      {path === '/observatory' && <>
        <div className="obs-hero"><div><div className="obs-kicker">SENTINELGRAPH · RISK OVERVIEW</div><h2>See the signals.<br/><em>Understand the risks.</em></h2><p>Aggregate analysis of submitted observations, with transparent risk levels.</p><Link className="obs-button" to="/observatory/monitor">Analyze a URL or account</Link></div><div className="obs-orbit"><Activity size={118} strokeWidth={1}/><span>RISK INTELLIGENCE</span></div></div>
        <div className="obs-stats"><div><span>All submitted observations</span><strong>{total}</strong></div><div><span>High-risk findings</span><strong className="obs-critical">{high}</strong></div><div><span>Medium-risk findings</span><strong>{medium}</strong></div><div><span>Needs more evidence</span><strong>{low}</strong></div></div>
        <Card title="Risk distribution dashboard"><div className="obs-chart-description">Counts are based on saved observations. Repeated submissions of the same URL update its latest candidate record.</div><div className="obs-bars">{riskGroups.map(g => <div className="obs-bar-row" key={g.label}><span>{g.label}</span><div className="obs-bar-track"><div className={`obs-bar-fill ${g.className}`} style={{width:`${total ? 100*g.count/total : 0}%`}}/></div><strong>{g.count}</strong></div>)}</div></Card>
        <Card title="Platform distribution"><div className="obs-bars">{Object.entries((overview?.platformCounts || {}) as Record<string, number>).sort((a,b)=>b[1]-a[1]).map(([label,count])=><div className="obs-bar-row" key={label}><span>{label}</span><div className="obs-bar-track"><div className="obs-bar-fill platform" style={{width:`${total?count/total*100:0}%`}}/></div><strong>{count}</strong></div>)}{total===0&&<p className="obs-muted">No observations analyzed yet.</p>}</div></Card>
      </>}
      {path === '/observatory/brand' && <>
        <div className="obs-columns"><Card title="Register a brand"><form onSubmit={e => {e.preventDefault();void submit(() => obs('/brands', {name:brandName}), 'Brand registered in the database. Its details are not listed here.').then(ok=>{if(ok){setAssetBrand(brandName);setBrandName('')}})}}>
          <Field label="Brand name" value={brandName} onChange={setBrandName} required placeholder="Example: FoodRush"/>
          <button className="obs-button" disabled={busy}>Create brand</button></form></Card>
          <Card title="Add an official asset"><form className="obs-form" onSubmit={e=>{e.preventDefault();void submit(()=>obs('/assets',{brandName:assetBrand,platform:assetPlatform,name:assetName,identifier:assetIdentifier,sourceUrl:assetUrl,username:assetUsername,publisher:assetPublisher,description:assetDescription,logo:assetLogo}),'Official asset saved. It will be used automatically during analysis.').then(ok=>{if(ok){setAssetBrand('');setAssetPlatform('');setAssetName('');setAssetIdentifier('');setAssetUrl('');setAssetUsername('');setAssetPublisher('');setAssetDescription('');setAssetLogo('')}})}}>
            <Field label="Brand name" value={assetBrand} onChange={setAssetBrand} required placeholder="Registered brand name"/>
            <Field label="Platform (type any platform)" value={assetPlatform} onChange={setAssetPlatform} required placeholder="Instagram, Facebook, Google Play…"/>
            <Field label="Asset name" value={assetName} onChange={setAssetName} required/>
            <Field label="Stable identifier" value={assetIdentifier} onChange={setAssetIdentifier} required/>
            <Field label="Official URL" value={assetUrl} onChange={setAssetUrl} required placeholder="https://…"/>
            <Field label="Username (optional)" value={assetUsername} onChange={setAssetUsername}/>
            <Field label="Publisher (optional)" value={assetPublisher} onChange={setAssetPublisher}/>
            <Field label="Description (optional)" value={assetDescription} onChange={setAssetDescription}/>
            <Field label="Logo URL (optional)" value={assetLogo} onChange={setAssetLogo}/>
            <button className="obs-button" disabled={busy}>Save official asset</button>
          </form></Card></div>
        <p className="obs-muted">Registered reference data is stored in PostgreSQL and used for internal comparisons. It is intentionally not listed here. The current prototype still requires authentication before real brand data should be stored.</p>
      </>}
      {path === '/observatory/monitor' && <>
        <Card title="Analyze a social account, app, or URL">
          <p className="obs-muted">Paste a URL to check it against internally registered brands. You do not need to select an official page. Additional information is optional and improves the assessment; the app does not fetch live account data.</p>
          <form className="obs-form" onSubmit={e=>{e.preventDefault();setResult(null);submit(async()=>{const r=await obs<AnalysisResult>('/analyze',{sourceUrl,platform,name,username,publisher,description,outboundUrl,synthetic});setResult(r)},'Current analysis completed and saved to PostgreSQL.')}}>
            <Field label="URL to analyze" value={sourceUrl} onChange={setSourceUrl} required placeholder="https://www.instagram.com/suspicious_account/"/>
            <Field label="Platform (optional — detected from URL)" value={platform} onChange={setPlatform} placeholder="Automatically detected"/>
            <Field label="Account or app name (optional)" value={name} onChange={setName}/>
            <Field label="Username (optional)" value={username} onChange={setUsername}/>
            <Field label="Publisher (optional)" value={publisher} onChange={setPublisher}/>
            <Field label="Description or reported message (optional)" value={description} onChange={setDescription}/>
            <Field label="Outbound link (optional)" value={outboundUrl} onChange={setOutboundUrl}/>
            <label className="obs-check"><input type="checkbox" checked={synthetic} onChange={e=>setSynthetic(e.target.checked)}/> This is synthetic demonstration data</label>
            <button className="obs-button" disabled={busy}>{busy?'Analyzing…':'Analyze risk'}</button>
          </form>
        </Card>
        {result&&<Card title="Risk analysis result"><ScorePanel result={result}/></Card>}
      </>}
    </div>
  </ConsoleShell>
}
