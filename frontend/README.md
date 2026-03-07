# Frontend

## Start

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server runs on `http://localhost:5173` and proxies `/v1/*` to `http://127.0.0.1:8888`.

## Notes

- Make sure the Spring Boot backend is running on port `8888`.
- The chat page posts to `/v1/medibuddy/chat`.
- Click `新建问诊` to create a fresh `memoryId` conversation.