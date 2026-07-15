# Simulation Visualizer

## Overview
The Simulation Visualizer is a React-based frontend used to monitor vehicles in real time.

It:
- fetches vehicle data from the Query Service
- displays vehicles on a map (Leaflet)
- updates positions periodically
- allows selecting a vehicle

---

## Architecture

Browser → Nginx → Query Service → Kafka → Simulator

### Components
- React (Vite)
- Leaflet
- Nginx (reverse proxy)

---

## API

All backend calls go through:

/api

Endpoints:
- GET /api/vehicles
- GET /api/vehicles/{id}

---

## Development (Local)

Install:

```bash
npm install
```

Configure proxy (vite.config.ts):

```ts
server: {
  proxy: {
    "/api": {
      target: "http://localhost:8083",
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, "")
    }
  }
}
```

Run:

```bash
npm run dev
```

---

## Build

```bash
npm run build
```

Output: dist/

---

## Docker

Build:

```bash
docker build -t simulation-visualizer-service:latest .
```

---

## Nginx Configuration

```nginx
server {
    listen 80;

    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri /index.html;
    }

    location /api/ {
        proxy_pass http://query-service.acme-fleet.svc.cluster.local:8080/;
    }
}
```

---

## Kubernetes

Build:

```bash
eval $(minikube docker-env)
docker build -t simulation-visualizer-service:latest .
```

Deploy:

```bash
kubectl apply -f k8s/
```

Restart:

```bash
kubectl rollout restart deployment/simulation-visualizer-service -n acme-fleet
```

---

## Debugging

Test API via browser:

http://<visualizer-url>/api/vehicles

Check nginx config:

```bash
kubectl exec -it -n acme-fleet deploy/simulation-visualizer-service -- sh
cat /etc/nginx/conf.d/default.conf
```

Test backend:

```bash
apk add curl
curl http://query-service.acme-fleet.svc.cluster.local:8080/vehicles
```

---

## Notes

- Vite proxy works only in development
- Nginx handles API routing in production
- API base path must be /api
- Uses polling for updates