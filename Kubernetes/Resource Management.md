To enforce **Resource Quotas** at the namespace level in Kubernetes (e.g., limiting CPU/memory for the `web` and `database` namespaces), follow these steps. 
This ensures no single namespace overconsumes cluster resources, a critical skill for production environments.

---

### **1. Create a Namespace**
First, create the namespaces (e.g., `web` and `database`):
```bash
kubectl create namespace web
kubectl create namespace database
```

---

### **2. Define Resource Quotas**
Create a YAML file (e.g., `resource-quota.yaml`) to enforce limits for each namespace:

#### **Example: `web` Namespace Quota (10 CPUs, 20Gi Memory)**
```yaml
# web-quota.yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: web-resource-quota
  namespace: web
spec:
  hard:
    requests.cpu: "10"      # Max 10 CPUs requested
    requests.memory: "20Gi" # Max 20Gi memory requested
    limits.cpu: "15"        # Max 15 CPUs allowed (burst capacity)
    limits.memory: "30Gi"   # Max 30Gi memory allowed
```

#### **Example: `database` Namespace Quota (20 CPUs, 40Gi Memory)**
```yaml
# database-quota.yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: database-resource-quota
  namespace: database
spec:
  hard:
    requests.cpu: "20"
    requests.memory: "40Gi"
    limits.cpu: "30"
    limits.memory: "60Gi"
```

---

### **3. Apply the Quotas**
```bash
kubectl apply -f web-quota.yaml
kubectl apply -f database-quota.yaml
```

---

### **4. Verify Quotas**
Check the applied quotas:
```bash
kubectl get resourcequota -n web
kubectl get resourcequota -n database
```

Example output:
```
NAME                 AGE   REQUEST                                      LIMIT
web-resource-quota   10s   requests.cpu: 0/10, requests.memory: 0/20Gi   limits.cpu: 0/15, limits.memory: 0/30Gi
```

---

### **5. Deploy Pods with Resource Requests/Limits**
For the quota to work, **Pods must specify resource requests/limits** in their specs.  
**Example Nginx Deployment in `web` namespace**:
```yaml
# nginx-job.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx
  namespace: web
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:alpine
        resources:
          requests:
            cpu: "1"     # Requests 1 CPU per Pod
            memory: "2Gi"
          limits:
            cpu: "2"     # Limits to 2 CPUs per Pod (burst)
            memory: "4Gi"
```

Apply the deployment:
```bash
kubectl apply -f nginx-job.yaml
```

---

### **6. How It Works**
- **Requests**: Reserved resources (e.g., `requests.cpu: "1"` means 1 CPU is guaranteed).
- **Limits**: Maximum allowed resources (prevents a Pod from starving others).
- **Quota Enforcement**:
  - If a Pod’s `requests` exceed the quota, it **won’t schedule**.
  - If a Pod’s `limits` exceed the quota, it **won’t schedule**.

---

### **7. Key Commands for Monitoring**
#### **Check Quota Usage**
```bash
kubectl describe resourcequota -n web
```
Output includes current usage:
```
Name:            web-resource-quota
Namespace:       web
Resource         Used  Hard
--------         ----  ----
limits.cpu       6     15
limits.memory    12Gi  30Gi
requests.cpu     3     10
requests.memory  6Gi   20Gi
```

#### **Check Pod Resource Usage**
```bash
kubectl top pods -n web
```

---

### **8. Troubleshooting**
#### **Error: Exceeded Quota**
If a Pod fails to schedule with an error like:
```
Error: pods "nginx" is forbidden: exceeded quota: web-resource-quota
```
**Solution**:
1. Scale down existing deployments.
2. Increase the quota (if justified).
3. Optimize Pod resource requests/limits.

---

### **9. Advanced Quota Types**
You can also limit other resources:
```yaml
spec:
  hard:
    pods: "50"              # Max Pods in the namespace
    services: "10"          # Max Services
    persistentvolumeclaims: "5" # Max PVCs
    secrets: "20"           # Max Secrets
```

---

### **10. Best Practices**
1. **Start Conservative**: Begin with tight quotas and adjust as needed.
2. **Monitor**: Use `kubectl top` and Prometheus/Grafana for trends.
3. **Combine with LimitRanges**: Set default requests/limits for Pods in a namespace:
   ```yaml
   # limit-range.yaml
   apiVersion: v1
   kind: LimitRange
   metadata:
     name: default-limits
   spec:
     limits:
     - default:
         cpu: "1"
         memory: "2Gi"
       defaultRequest:
         cpu: "0.5"
         memory: "1Gi"
       type: Container
   ```

---

### **Summary**
By enforcing **Resource Quotas**, you:
- Prevent resource starvation between teams/namespaces.
- Ensure fair allocation of cluster resources.
- Maintain cluster stability and performance.
