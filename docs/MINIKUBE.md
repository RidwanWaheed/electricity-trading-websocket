# Minikube Commands Reference

Quick reference for running the trading platform on Minikube.

## Prerequisites

```bash
# Install minikube (macOS)
brew install minikube

# Start minikube
minikube start

# Verify installation
minikube status
```

## Building Images

Minikube runs its own Docker daemon. To build images that Minikube can use:

```bash
# Switch to Minikube's Docker environment (required before building)
eval $(minikube docker-env)

# Build JARs
./mvnw clean package -DskipTests -q

# Build Docker images
docker build -t trading-gateway:latest -f gateway-service/Dockerfile .
docker build -t trading-order-service:latest -f order-service/Dockerfile .
docker build -t trading-mock-m7:latest -f mock-m7-service/Dockerfile .
```

## Deploying to Minikube

```bash
# Create namespace and deploy all resources
kubectl apply -f k8s/

# Check deployment status
kubectl get pods -n trading

# Watch pods come up
kubectl get pods -n trading -w
```

## Accessing Services

```bash
# Get gateway URL (keeps terminal open for tunnel)
minikube service gateway -n trading --url

# Access via NodePort directly
curl http://$(minikube ip):30080/actuator/health
```

## Viewing Logs

```bash
# All pods in namespace
kubectl logs -n trading -l app=gateway --tail=50
kubectl logs -n trading -l app=order-service --tail=50
kubectl logs -n trading -l app=mock-m7 --tail=50

# Follow logs
kubectl logs -n trading -l app=gateway -f

# Specific pod
kubectl logs -n trading <pod-name>
```

## Debugging

```bash
# Describe pod (shows events, errors)
kubectl describe pod -n trading <pod-name>

# Get all resources in namespace
kubectl get all -n trading

# Check why pod is failing
kubectl logs -n trading <pod-name> --previous

# Shell into a running pod
kubectl exec -it -n trading <pod-name> -- /bin/sh
```

## Restarting Services

```bash
# Restart all deployments (picks up new images)
kubectl rollout restart deployment -n trading

# Restart specific deployment
kubectl rollout restart deployment gateway -n trading

# Delete and recreate all pods
kubectl delete pods -n trading --all
```

## Rebuilding After Code Changes

```bash
# Full rebuild cycle
eval $(minikube docker-env)
./mvnw clean package -DskipTests -q
docker build -t trading-gateway:latest -f gateway-service/Dockerfile .
docker build -t trading-order-service:latest -f order-service/Dockerfile .
docker build -t trading-mock-m7:latest -f mock-m7-service/Dockerfile .
kubectl rollout restart deployment -n trading
```

## Cleanup

```bash
# Delete all resources in namespace
kubectl delete -f k8s/

# Or delete entire namespace
kubectl delete namespace trading

# Stop minikube
minikube stop

# Delete minikube cluster entirely
minikube delete
```

## Useful Shortcuts

```bash
# Alias for kubectl
alias k=kubectl

# Quick status check
kubectl get pods,svc,deploy -n trading

# Port-forward to a specific service (alternative to NodePort)
kubectl port-forward -n trading svc/gateway 8080:8080
```

## Troubleshooting

### Pods stuck in CrashLoopBackOff
```bash
# Check logs for error
kubectl logs -n trading <pod-name>

# Check events
kubectl describe pod -n trading <pod-name>
```

### Images not found
```bash
# Ensure you're using Minikube's Docker
eval $(minikube docker-env)

# Verify images exist
docker images | grep trading
```

### Services not accessible
```bash
# Check service endpoints
kubectl get endpoints -n trading

# Verify NodePort is allocated
kubectl get svc -n trading
```
