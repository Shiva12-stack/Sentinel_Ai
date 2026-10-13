import { Suspense, useRef, useMemo } from 'react'
import { Canvas, useFrame } from '@react-three/fiber'
import { OrbitControls, Float, Stars } from '@react-three/drei'
import * as THREE from 'three'
import type { Group } from 'three'
function Orb(){
  const group=useRef<Group>(null)
  const dots=useMemo(()=>Array.from({length:65},(_,i)=>{
    const y=1-2*(i+0.5)/65;const radius=Math.sqrt(1-y*y);const theta=i*Math.PI*(3-Math.sqrt(5));
    return [radius*Math.cos(theta)*2.05,y*2.05,radius*Math.sin(theta)*2.05] as [number,number,number]
  }),[])
  useFrame((_,delta)=>{if(group.current){group.current.rotation.y+=delta*0.09;group.current.rotation.z=Math.sin(Date.now()/5500)*0.045}})
  return <group ref={group}>
    <mesh><sphereGeometry args={[1.98,38,26]}/><meshPhongMaterial color="#a37837" emissive="#513b1b" emissiveIntensity={0.3} transparent opacity={0.08} depthWrite={false}/></mesh>
    <mesh><sphereGeometry args={[2,32,24]}/><meshBasicMaterial color="#ab7d39" wireframe transparent opacity={0.18}/></mesh>
    <mesh rotation={[Math.PI/2.4,0,0]}><torusGeometry args={[2.25,.007,6,170]}/><meshBasicMaterial color="#c3934c" transparent opacity={0.65}/></mesh>
    <mesh rotation={[-Math.PI/3,Math.PI/5,0]}><torusGeometry args={[2.18,.006,6,170]}/><meshBasicMaterial color="#c79a57" transparent opacity={0.42}/></mesh>
    <mesh rotation={[Math.PI/4,0,Math.PI/3]}><torusGeometry args={[2.37,.004,6,170]}/><meshBasicMaterial color="#d8b98b" transparent opacity={0.2}/></mesh>
    {dots.map((p,i)=><mesh position={p} key={i}><sphereGeometry args={[i%11===0?.047:.018,8,8]}/><meshBasicMaterial color={i%11===0?'#d3af7a':'#c4944d'} transparent opacity={i%11===0?1:.75}/></mesh>)}
    <mesh position={[.76,.68,1.59]}><sphereGeometry args={[.13,16,16]}/><meshBasicMaterial color="#ac7e39"/></mesh>
    <pointLight position={[.76,.68,1.59]} color="#c59752" intensity={6} distance={1.8}/>
  </group>
}
function World(){return <><ambientLight intensity={0.7}/><directionalLight color="#d6b584" intensity={1.8} position={[2,3,5]}/><Stars radius={18} depth={12} count={850} factor={2.4} saturation={0} fade speed={0.28}/><Float speed={1.15} floatIntensity={0.28} rotationIntensity={0.06}><Orb/></Float><OrbitControls enablePan={false} enableZoom={false} autoRotate autoRotateSpeed={0.23} minPolarAngle={.6} maxPolarAngle={2.5}/></>}
export default function Globe(){return <div className="globe-canvas" aria-hidden="true"><Suspense fallback={<div className="globe-fallback"/>}><Canvas camera={{position:[0,.05,6.4],fov:47}} dpr={[1,1.8]} gl={{antialias:true,alpha:true,powerPreference:'high-performance'}}><World/></Canvas></Suspense></div>}
