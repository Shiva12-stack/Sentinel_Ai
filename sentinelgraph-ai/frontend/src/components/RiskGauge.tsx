import type { CSSProperties } from 'react'

type Props = {
  score: number | null | undefined
  verdict?: string
  compact?: boolean
  className?: string
}

/** A visual indicator of reported *heuristic* risk, not a fraud probability or safety certificate. */
export function getRiskPresentation(score: number | null | undefined, verdict = '') {
  if (score === null || score === undefined || !Number.isFinite(score)) {
    return { value: null, label: 'NOT SCORED', tone: 'unknown', detail: 'No score is available for this record.' }
  }
  const value = Math.max(0, Math.min(100, Math.round(score)))
  if (verdict === 'VERIFIED_REFERENCE_MATCH' || verdict === 'SUPPLIED_OFFICIAL_ASSET_MATCH') {
    return { value, label: 'REFERENCE MATCH', tone: 'reference', detail: 'Matched a supplied reference; safety is not verified.' }
  }
  if (value >= 70 || verdict === 'LIKELY_IMPERSONATION') {
    return { value, label: 'HIGH RISK', tone: 'danger', detail: 'Strong suspicious indicators from available evidence.' }
  }
  if (value >= 35 || verdict === 'SUSPICIOUS') {
    return { value, label: 'RISK DETECTED', tone: 'danger', detail: 'Suspicious indicators need further investigation.' }
  }
  return { value, label: 'LOW INDICATED RISK', tone: 'low', detail: 'Insufficient evidence to declare this source safe.' }
}

const segmentColors = [
  '#54b865', '#79bb59', '#a5bf53', '#c1bd4c',
  '#d7b84e', '#e5ac49', '#eda149', '#ef9147',
  '#ed7b3d', '#e96339', '#e5483b', '#ce3439',
]
const polar = (angle: number, radius: number) => {
  const rad = (180 - angle * 1.8) * Math.PI / 180
  return { x: 180 + radius * Math.cos(rad), y: 181 - radius * Math.sin(rad) }
}
const arc = (start: number, end: number, radius = 125) => {
  const a = polar(start, radius)
  const b = polar(end, radius)
  return `M ${a.x.toFixed(2)} ${a.y.toFixed(2)} A ${radius} ${radius} 0 0 1 ${b.x.toFixed(2)} ${b.y.toFixed(2)}`
}

/** Shared between URL Analysis, Findings and Social & Apps. */
export default function RiskGauge({ score, verdict, compact = false, className = '' }: Props) {
  const result = getRiskPresentation(score, verdict)
  const marker = result.value === null ? null : polar(result.value, 125)
  const accessibilityLabel = result.value === null
    ? 'Risk score not available'
    : `Heuristic risk score ${result.value} out of 100. ${result.label}. Not a safety guarantee.`
  return <div className={`sg-risk-gauge ${compact ? 'sg-risk-gauge--compact' : ''} sg-risk-gauge--${result.tone} ${className}`} style={{ '--sg-score-color': result.tone === 'danger' ? '#c92e35' : result.tone === 'low' ? '#27894e' : result.tone === 'reference' ? '#287c55' : '#746d62' } as CSSProperties}>
    <div className="sg-risk-gauge__visual" role="img" aria-label={accessibilityLabel}>
      <svg viewBox="0 0 360 240" aria-hidden="true" focusable="false">
        {segmentColors.map((color, i) => <path key={i} d={arc(i * 100 / 12 + 0.5, (i + 1) * 100 / 12 - 0.5)} fill="none" stroke={color} strokeWidth="27" strokeLinecap="butt" />)}
        {[0, 20, 40, 60, 80, 100].map(n => {
          const p = polar(n, 159)
          return <text key={n} x={p.x} y={p.y+4} textAnchor="middle" className="sg-risk-gauge__tick">{n}</text>
        })}
        {marker && <>
          <circle cx={marker.x} cy={marker.y} r="9" fill="#fff" stroke="#28241e" strokeWidth="2.5" />
          <circle cx={marker.x} cy={marker.y} r="3.5" fill="#28241e" />
        </>}
        <text x="180" y="168" textAnchor="middle" className="sg-risk-gauge__number">{result.value ?? '—'}</text>
        <text x="180" y="191" textAnchor="middle" className="sg-risk-gauge__denominator">{result.value === null ? 'NOT SCORED' : 'OUT OF 100'}</text>
      </svg>
    </div>
    <div className={`sg-risk-gauge__badge sg-risk-gauge__badge--${result.tone}`}>{result.label}</div>
    {!compact && <p className="sg-risk-gauge__detail">{result.detail} <span>Offline heuristic only — not a fraud probability.</span></p>}
  </div>
}
