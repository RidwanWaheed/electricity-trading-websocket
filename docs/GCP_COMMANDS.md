# GCP Deployment Commands

Quick reference for deploying to GKE Autopilot.

## Prerequisites

```bash
gcloud auth login
gcloud config set project trading-platform-ridwan
gcloud config set compute/region europe-west3
```

## Create Infrastructure

```bash
# 1. Artifact Registry
gcloud artifacts repositories create trading-images \
  --repository-format=docker \
  --location=europe-west3

# 2. GKE Autopilot cluster
gcloud container clusters create-auto trading-cluster \
  --region=europe-west3

# 3. Cloud SQL (PostgreSQL)
gcloud sql instances create trading-postgres \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region=europe-west3 \
  --no-assign-ip \
  --network=default

# 4. VPC peering (required for private IP)
gcloud services vpc-peerings connect \
  --service=servicenetworking.googleapis.com \
  --ranges=google-managed-services-default \
  --network=default

# 5. Create database and user
gcloud sql databases create trading --instance=trading-postgres
gcloud sql users create tradingadmin \
  --instance=trading-postgres \
  --password=TradingDB2024!

# 6. Get Cloud SQL private IP (update in k8s/overlays/gcp/order-service.yaml)
gcloud sql instances describe trading-postgres --format="value(ipAddresses[0].ipAddress)"
```

## Build & Push Images

```bash
# Configure Docker for Artifact Registry
gcloud auth configure-docker europe-west3-docker.pkg.dev

# Build for AMD64 (required for GKE)
docker buildx build --platform linux/amd64 \
  -t europe-west3-docker.pkg.dev/trading-platform-ridwan/trading-images/gateway:latest \
  --push ./gateway

docker buildx build --platform linux/amd64 \
  -t europe-west3-docker.pkg.dev/trading-platform-ridwan/trading-images/order-service:latest \
  --push ./order-service

docker buildx build --platform linux/amd64 \
  -t europe-west3-docker.pkg.dev/trading-platform-ridwan/trading-images/mock-m7:latest \
  --push ./mock-m7
```

## Deploy to GKE

```bash
# Get cluster credentials
gcloud container clusters get-credentials trading-cluster --region=europe-west3

# Deploy
kubectl apply -k k8s/overlays/gcp/

# Watch pods
kubectl get pods -n trading -w

# Get Gateway public IP
kubectl get svc gateway -n trading
```

## Verify

```bash
# Health check (replace IP)
curl http://<GATEWAY_IP>:8080/actuator/health

# View logs
kubectl logs -n trading -l app=gateway --tail=100
kubectl logs -n trading -l app=order-service --tail=100
kubectl logs -n trading -l app=mock-m7 --tail=100
```

## Cleanup

```bash
# Delete cluster
gcloud container clusters delete trading-cluster --region=europe-west3 --quiet

# Delete Cloud SQL
gcloud sql instances delete trading-postgres --quiet

# Delete Artifact Registry
gcloud artifacts repositories delete trading-images --location=europe-west3 --quiet
```
