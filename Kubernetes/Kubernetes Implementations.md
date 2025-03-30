
### **1. Service Types in Kubernetes**
#### **ClusterIP (Default)**
- **Purpose**: Internal communication within the cluster.
- **Example**: 
  ```bash
  NAME         TYPE        CLUSTER-IP    PORT(S)   AGE
  kubernetes   ClusterIP   10.96.0.1     443/TCP   8d
  ```
  - The `kubernetes` service is a built-in ClusterIP service for internal API server access.
  - Pods/services inside the cluster can talk to the API server via `https://10.96.0.1:443`.

#### **NodePort**
- **Purpose**: Expose a service externally on a static port across all nodes.
- **Example**:
  ```bash
  NAME            TYPE       CLUSTER-IP     PORT(S)        AGE
  nginx-service   NodePort   10.99.77.189   80:30080/TCP   20s
  ```
  - Exposes the service on port `30080` on **every node's IP**.
  - External traffic can reach the service via `<NodeIP>:30080`.

---

### **2. Service Ports Explained**
For `nginx-service`:
- **Port 80**: The port the service listens to internally (mapped to Pods).
- **NodePort 30080**: The port exposed externally on all nodes.
- **ClusterIP 10.99.77.189**: The virtual IP assigned to the service for internal cluster communication.

---

### **3. How Traffic Flows for NodePort**
1. **External Request**: A user sends a request to `<NodeIP>:30080`.
2. **NodePort Handling**: The node’s `kube-proxy` forwards the request to the ClusterIP (`10.99.77.189:80`).
3. **Service to Pod**: The service routes the request to one of the Pods backing it (using labels/selectors).
4. **Pod Response**: The Pod processes the request and sends a response back.

---

### **4. Key Networking Components**
#### **kube-proxy**
- Runs on every node.
- Manages network rules (iptables/IPVS) to route traffic to services.
- For NodePort, it ensures `<NodeIP>:30080` → `ClusterIP:80` → Pods.

#### **Pod Networking**
- Each Pod gets a unique IP (e.g., `10.244.1.3`).
- Pods communicate directly across nodes via **CNI plugins** (e.g., Calico, Flannel).

#### **Cluster DNS (CoreDNS)**
- Resolves service names to ClusterIPs (e.g., `nginx-service.default.svc.cluster.local` → `10.99.77.189`).

---

### **5. Service Configuration Deep Dive**
#### **Service Definition for `nginx-service`**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-service
spec:
  type: NodePort
  selector:
    app: nginx  # Matches Pods with label `app: nginx`
  ports:
    - protocol: TCP
      port: 80     # Service port (ClusterIP:80)
      targetPort: 80  # Pod port (e.g., 80 on the NGINX container)
      nodePort: 30080 # External port on nodes (optional; Kubernetes auto-allocates if omitted)
```

#### **View Service Details**
```bash
kubectl describe svc nginx-service
```
Output:
```
Name:                     nginx-service
Namespace:                default
Labels:                   <none>
Annotations:              <none>
Selector:                 app=nginx
Type:                     NodePort
IP:                       10.99.77.189
Port:                     <unset>  80/TCP
TargetPort:               80/TCP
NodePort:                 <unset>  30080/TCP
Endpoints:                10.244.1.3:80,10.244.2.5:80  # Pod IPs
```

---

### **6. Use Cases**
#### **When to Use NodePort**
- Development/testing (quick external access).
- On-prem clusters without a cloud LoadBalancer.
- Debugging traffic flow.

#### **When to Use ClusterIP**
- Internal communication (e.g., microservices talking to each other).
- APIs/databases that shouldn’t be exposed externally.

---

### **7. Advanced: Beyond NodePort**
#### **LoadBalancer**
- Cloud-managed external IP (e.g., AWS ELB, GCP Load Balancer).
- Automatically routes traffic to NodePorts or Pods.

#### **Ingress**
- HTTP/HTTPS routing (e.g., host/path-based rules).
- Requires an Ingress Controller (e.g., NGINX Ingress, Traefik).

---

### **8. Security Considerations**
- **NodePorts are open on all nodes**. Use firewall rules to restrict access.
- Prefer **Ingress** + **LoadBalancer** for production over NodePort.

---

### **9. Summary**
| **Concept**   | **Description**                                                                 |
|---------------|---------------------------------------------------------------------------------|
| **ClusterIP** | Internal IP for services. Only accessible within the cluster.                   |
| **NodePort**  | Exposes a service on a static port on all nodes. Accessible via `<NodeIP>:<Port>`. |
| **kube-proxy**| Manages network rules to route traffic to services.                             |
| **Pod IP**    | Unique IP assigned to each Pod for direct communication.                        |

---

### **10. Commands to Explore**
1. List endpoints (Pods) for a service:
   ```bash
   kubectl get endpoints nginx-service
   ```
2. View iptables rules managed by kube-proxy:
   ```bash
   iptables-save | grep nginx-service
   ```
3. Access the service:
   ```bash
   curl http://<ANY_NODE_IP>:30080
   ```

Taint and Tolerations 

kubectl taint nodes <node-name> key=value:effect


### 1. First, delete existing cluster:
```bash
minikube delete
```

### 2. Create new cluster with 3 nodes:
```bash
minikube start --nodes 3 --driver=docker
```

### 3. If you still get the node count error:
```bash
# Create single-node cluster first
minikube start --driver=docker

# Then add nodes one by one
minikube node add node2
minikube node add node3
```

### 4. Verify your nodes:
```bash
minikube node list
```

### 5. Check cluster status:
```bash
kubectl get nodes -o wide
```

### Key Notes:
1. **Docker Driver Limitations**:
   - Multi-node clusters require proper container networking setup
   - Each node runs as separate Docker container
   - ARM Macs might need extra configuration for networking

2. **Alternative Drivers** (if Docker gives issues):
   ```bash
   minikube start --nodes 3 --driver=hyperkit  # For Intel Macs
   minikube start --nodes 3 --driver=qemu2     # For ARM Macs
   ```

3. **Node Roles**:
   ```bash
   kubectl get nodes
   ```
   You'll see:
   - 1 control-plane node (master)
   - 2 worker nodes

4. **Access Dashboard**:
   ```bash
   minikube dashboard
   ```

### Step 1: Verify Your Cluster
```bash
kubectl get nodes -o wide
# Example output:
# NAME           STATUS   ROLES           AGE   VERSION
# minikube       Ready    control-plane   10m   v1.30.0
# minikube-m02   Ready    <none>          9m    v1.30.0
# minikube-m03   Ready    <none>          9m    v1.30.0
```

### Step 2: Add Taint to a Node
```bash
# Taint node minikube-m02 to repel pods without special permission
kubectl taint nodes minikube-m02 app=nginx:NoSchedule
```

### Step 3: Create Basic Nginx Deployment (Without Toleration)
```bash
kubectl create deployment nginx --image=nginx:alpine
kubectl get pods -o wide
```

**Observation**: 
- Pods will only run on minikube (control-plane) and minikube-m03
- No pods will schedule on minikube-m02

### Step 4: Create Tolerated Deployment
```yaml
# nginx-tolerated.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-tolerated
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
      tolerations:
      - key: "app"
        operator: "Equal"
        value: "nginx"
        effect: "NoSchedule"
      containers:
      - name: nginx
        image: nginx:alpine
```

Apply the deployment:
```bash
kubectl apply -f nginx-tolerated.yaml
kubectl get pods -o wide
```

**Observation**:
- Pods will now schedule on all nodes, including tainted minikube-m02
- Use `kubectl describe node minikube-m02` to see taint details

### Step 5: Verify Taint Enforcement
```bash
# Scale up the original deployment
kubectl scale deployment nginx --replicas=5
kubectl get pods -o wide
```

**Observation**:
- Original nginx pods still avoid minikube-m02
- Tolerated pods use all nodes

### Step 6: Clean Up
```bash
# Remove taint
kubectl taint nodes minikube-m02 app:NoSchedule-

# Delete deployments
kubectl delete deployment nginx nginx-tolerated
```

### Key Concepts:
1. **Taint Components**:
   - **Key**: Identifier (e.g., `app`)
   - **Value**: Optional constraint (e.g., `nginx`)
   - **Effect**: 
     - `NoSchedule` (prevent new pods)
     - `PreferNoSchedule` (soft restriction)
     - `NoExecute` (evict existing pods)

2. **Toleration Matching**:
   ```yaml
   tolerations:
   - key: "app"
     operator: "Equal"  # or "Exists"
     value: "nginx"
     effect: "NoSchedule"
   ```

3. **Common Use Cases**:
   - Reserve nodes for GPU workloads
   - Isolate production/test environments
   - Handle special hardware nodes

### Advanced Practice:
1. Try combining with node affinity:
   ```yaml
   affinity:
     nodeAffinity:
       requiredDuringSchedulingIgnoredDuringExecution:
         nodeSelectorTerms:
         - matchExpressions:
           - key: app
             operator: In
             values: [nginx]
   ```

2. Experiment with different effects:
   ```bash
   kubectl taint nodes minikube-m02 app=nginx:NoExecute
   ```

3. Check pod eviction behavior:
   ```bash
   watch kubectl get pods -o wide
   ```

# Remove taint
kubectl taint nodes minikube-m02 app:NoSchedule-

# Delete deployments
kubectl delete deployment nginx nginx-tolerated