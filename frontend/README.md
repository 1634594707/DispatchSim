# DispatchSim Frontend

Vue 3 frontend for DispatchSim.

## Run

```powershell
npm ci
npm run dev
```

Default URL: `http://localhost:5173`

## Build

```powershell
npm run build
```

## Environment variables

- `VITE_API_BASE_URL`
- `VITE_WS_URL`

See `.env.example` for the recommended local development values.

## Development behavior

- Vite dev server listens on `0.0.0.0:5173`
- `/api` is proxied to `http://localhost:8080`
- `/ws` is proxied to `ws://localhost:8080`

If no environment variables are provided, the app prefers same-origin paths for API and WebSocket access.

## Related docs

- Root guide: `../README.md`
