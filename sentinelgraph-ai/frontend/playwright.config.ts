import { defineConfig } from '@playwright/test'
export default defineConfig({testDir:'./tests',workers:1,fullyParallel:false,use:{baseURL:process.env.E2E_BASE_URL||'http://localhost:5173',headless:true},reporter:'list',webServer:{command:'npm run dev',url:'http://localhost:5173',reuseExistingServer:true}})
