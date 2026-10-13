import type { Analysis, DashboardStats, CaseItem } from './types'
import { backendApiUrl } from './backendUrl'
import { getDemoAnalyses, getDemoAnalysis, saveDemoAnalysis, getDemoStats, getDemoCases, saveDemoCase } from './demo'
const MODE_KEY='sentinelgraph_use_backend'
export const useBackend = () => true
export const setBackendMode = (enabled:boolean) => {localStorage.setItem(MODE_KEY,String(enabled));window.dispatchEvent(new Event('sentinelgraph-mode'))}
async function request<T>(path:string,init?:RequestInit):Promise<T>{
  const response=await fetch(backendApiUrl(`/api/v1${path}`),{...init,headers:{'Content-Type':'application/json',...(init?.headers||{})}})
  if(!response.ok){let detail='The server could not process this request.';try{const body=await response.json();detail=body.message||detail}catch{};throw new Error(detail)}
  return response.json() as Promise<T>
}
export const api={
  async analyze(url:string):Promise<Analysis>{return useBackend()?request('/analyze/url',{method:'POST',body:JSON.stringify({url})}):saveDemoAnalysis(url)},
  async list():Promise<Analysis[]>{return useBackend()?request('/findings'):getDemoAnalyses()},
  async one(id:string):Promise<Analysis>{if(useBackend())return request(`/analyses/${encodeURIComponent(id)}`);const result=getDemoAnalysis(id);if(!result)throw new Error('Analysis not found.');return result},
  async stats():Promise<DashboardStats>{return useBackend()?request('/dashboard/stats'):getDemoStats()},
  async cases():Promise<CaseItem[]>{return useBackend()?request('/cases'):getDemoCases()},
  async newCase(id:string,title:string,priority:CaseItem['priority']='MEDIUM'):Promise<CaseItem>{return useBackend()?request('/cases',{method:'POST',body:JSON.stringify({analysisId:id,title,priority})}):saveDemoCase(id,title,priority)},
  async caseStatus(id:string,status:string):Promise<CaseItem>{return request(`/cases/${encodeURIComponent(id)}/status`,{method:'POST',body:JSON.stringify({status})})},
  async health():Promise<boolean>{try{await request('/health');return true}catch{return false}}
}
