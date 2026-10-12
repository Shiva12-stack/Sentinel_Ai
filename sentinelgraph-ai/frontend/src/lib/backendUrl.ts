/**
 * All frontend API calls remain same-origin. Vercel rewrites /api/* to Render;
 * Vite proxies /api/* locally. No browser-exposed backend URL or CORS required.
 */
export function backendApiUrl(path: string): string {
  if (!path.startsWith('/api/')) throw new Error('API path must start with /api/')
  return path
}
