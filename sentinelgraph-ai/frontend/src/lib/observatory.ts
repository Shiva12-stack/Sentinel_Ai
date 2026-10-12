export type RecordData = Record<string, any>
export async function obs<T=RecordData>(path='',body?:unknown):Promise<T>{
 const response=await fetch('/api/v1/observatory'+path,{method:body===undefined?'GET':'POST',headers:{'Content-Type':'application/json'},...(body===undefined?{}:{body:JSON.stringify(body)})})
 if(!response.ok){let message=`API returned ${response.status}`;try{message=(await response.json()).message||message}catch{}throw new Error(message)}
 return response.json()
}
export function download(data:unknown,name:string){const url=URL.createObjectURL(new Blob([JSON.stringify(data,null,2)],{type:'application/json'}));const a=document.createElement('a');a.href=url;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000)}
