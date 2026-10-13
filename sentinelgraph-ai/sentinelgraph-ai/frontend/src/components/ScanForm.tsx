import { useState } from 'react'
import { ArrowRight, Link2, LoaderCircle, ScanSearch } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { api } from '../lib/api'
export default function ScanForm({hero=false}:{hero?:boolean}){
  const [value,setValue]=useState(''),[loading,setLoading]=useState(false),[error,setError]=useState('');const navigate=useNavigate()
  async function submit(e:React.FormEvent){e.preventDefault();if(!value.trim()){setError('Enter a website URL to continue.');return}setLoading(true);setError('');try{const result=await api.analyze(value);navigate(`/analysis/${encodeURIComponent(result.id)}`)}catch(err){setError(err instanceof Error?err.message:'Analysis could not be completed.')}finally{setLoading(false)}}
  return <form className={hero?'scan-form scan-hero':'scan-form'} onSubmit={submit}><div className="scan-main"><Link2 size={21} className="scan-link-icon"/><input value={value} onChange={e=>{setValue(e.target.value);setError('')}} placeholder="Enter a website URL to investigate..." aria-label="Website URL" autoComplete="url"/><button type="submit" disabled={loading} className="button button-primary scan-submit">{loading?<LoaderCircle className="spin" size={19}/>:<ScanSearch size={19}/>} <span>{loading?'Analyzing':'Analyze Website'}</span><ArrowRight className="scan-arr" size={17}/></button></div>{error&&<div className="form-error" role="alert">{error}</div>}<div className="scan-bottom"><span><span className="tiny-success"/> URL structure · brand resemblance · reference comparison</span><span>{hero?'No sign-up required':'Offline inspection unless configured'}</span></div></form>
}
