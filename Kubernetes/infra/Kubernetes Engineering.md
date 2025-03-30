


###  **Kubernetes engineering**

- Covers practical applications of key Kubernetes concepts 

### **Core Architecture & Components**
- **Key Topics**: Master plane (API Server, Scheduler, Controller Manager), Node plane (Kubelet, Kube Proxy), etcd, networking model.namespace etc
- **Design Considerations**: 
  - Control plane high availability, etcd backup strategies.  
  - Node scalability (horizontal scaling).  
- **Implementation**: 
  - **Minikube** for quick local setup.  
  - Inspect logs/components (`kubectl describe`, `kubectl get events`).  
- **Use Cases**:  
  - Simple microservice deployment to observe how each component handles pods/services.

---

### **Cluster Setup & Day-to-Day Operations**
- **Key Topics**: 
  - Installing & configuring clusters (Minikube → managed solutions like EKS).  
  - RBAC controls, resource quotas, horizontal pod autoscaling.  
  - Taints & tolerations for specialized workloads.  
- **Design Considerations**: 
  - Multi-zone vs. single-zone, storage provisioning, load balancing.  
  - Upgrade strategies (rolling vs. blue/green).  
- **Implementation**: 
  - Deploy a multi-node Minikube or an EKS cluster.  
  - Practice scaling pods, upgrading cluster versions, setting up node taints.  
- **Use Cases**:
  - Staging environment vs. production environment, custom RBAC roles.

---

### **Debugging & Observability**
- **Key Topics**: 
  - Debugging commands: `kubectl logs`, `kubectl exec`, `kubectl describe`, events, CrashLoopBackOff scenarios.  
  - Observability stack: Prometheus + Grafana, metrics-server.  
- **Design Considerations**: 
  - Instrumentation approach (application-level metrics, K8s cluster metrics).  
  - Alerting & SLA tracking.  
- **Implementation**: 
  - Integrate Prometheus operator on your cluster, configure Grafana dashboards.  
  - Use liveness/readiness probes, interpret logs/events to troubleshoot.  
- **Use Cases**: 
  - Stress test an application, analyze resource usage, apply autoscaling.
---

### **Advanced Workload Patterns & GPU Integration**
- **Key Topics**: 
  - DaemonSets, StatefulSets, Jobs, CronJobs for specialized workloads.  
  - GPU scheduling (NVIDIA device plugin), ephemeral vs. persistent storage.  
- **Design Considerations**: 
  - Node affinity/anti-affinity, advanced scheduling constraints.  
  - GPU resource fragmentation and multi-tenant clusters.  
- **Implementation**: 
  - Deploy a GPU workload (e.g., PyTorch container) on a GPU-enabled Minikube/EKS node.  
  - Utilize node labels, resource limits, GPU device plugin configurations.  
- **Use Cases**: 
  - ML training job on K8s, HPC-style scheduling with data locality considerations.

---

### **Security & Best Practices**
- **Key Topics**: 
  - Network policies (Calico/Cilium), pod security context, secrets management.  
  - Admission controllers (PodSecurityPolicy, OPA/Gatekeeper).  
- **Design Considerations**: 
  - Zero-trust networking, encryption at rest/in transit.  
  - Multi-tenant clusters, restricted admin privileges.  
- **Implementation**: 
  - Set up network policies to restrict inter-pod traffic.  
  - Harden cluster with minimal privilege, custom admission controllers.  
- **Use Cases**: 
  - Compliance-focused workloads (PCI/HIPAA).  
  - Auditing cluster security posture (CIS benchmarks, built-in K8s security tools).

---



Core Architecture & Components
------------------------------

Master Plane
------------
**API Server**  
Acts as the single entry point for all cluster operations. It exposes the Kubernetes API over HTTPS, processes and validates requests, and updates the desired state of the cluster in etcd. In a high-availability setup, multiple API Servers run behind a load balancer. The API Server enforces authentication (e.g., token, TLS certs) and authorization (e.g., RBAC). It also provides admission control, where specific controllers or policies can inspect and modify API requests before they’re persisted.

**Controller Manager**  
A collection of independent control loops (controllers) that continually compare the desired state of the cluster (as specified in the API Server) with the actual state. Examples include the Deployment Controller (ensuring the correct number of pod replicas), Node Controller (monitoring node health), and Endpoint Controller (linking services to their backing pods). Each controller observes changes via the API Server’s watch mechanism and issues updates when discrepancies arise.

**Scheduler**  
Responsible for placing unassigned pods onto appropriate nodes. The scheduler uses filtering (rejecting nodes that don’t meet resource or policy requirements) and scoring (ranking remaining nodes) to find the best candidate. Advanced features include taints and tolerations for dedicated hardware, node affinity/anti-affinity for data locality, and custom schedulers for specialized workloads like GPU or HPC tasks.

**etcd**  
A strongly consistent, distributed key-value store that serves as the central database for cluster state. All Kubernetes objects (pods, services, deployments, etc.) are ultimately persisted in etcd. It uses the Raft consensus algorithm to maintain reliability in a multi-node setup. Regular backups of etcd are crucial, and it’s best practice to run an odd number of etcd nodes (three or five) to maintain quorum. Proper isolation (e.g., separate instances, dedicated SSDs) is recommended in production to avoid performance bottlenecks.

Node Plane
----------
**Kubelet**  
Runs on every node in the cluster and communicates with the API Server to receive pod specifications. It manages container lifecycles (create, start, stop) and reports the node and pod statuses back to the control plane. Kubelet enforces resource limits on CPU, memory, or GPU through underlying container runtimes (Docker, containerd, CRI-O). It also ensures containers remain healthy through configured probes (liveness, readiness, startup).

**Kube Proxy**  
Implements networking rules on each node to handle traffic directed at services. Depending on the configured mode (iptables, IPVS, or user space), it routes external or inter-pod traffic to the appropriate backend pods. IPVS mode is generally more performant at large scale, while iptables mode is more common by default. Kube Proxy effectively creates a “virtual IP” for services and ensures requests are balanced across multiple pod replicas.

Networking Model
----------------
Kubernetes follows a “flat” networking model where each pod receives its own unique IP address, and pods can communicate with each other without NAT. The Container Network Interface (CNI) layer—using plugins like Calico, Flannel, Weave Net, or Cilium—implements this flat network. Services add a stable virtual IP front end to a group of pods, with types such as ClusterIP for internal traffic, NodePort for external access via node IP, and LoadBalancer for integrating with cloud load balancers. Network policies (e.g., using Calico or Cilium) can enforce fine-grained traffic rules, which is critical in multi-tenant or security-focused environments.

Design Considerations for Phase 1
---------------------------------
Control Plane High Availability  
- Deploy multiple API Server replicas behind a load balancer.  
- Run etcd as a cluster of three or five nodes to ensure quorum.  
- Distribute etcd nodes and control plane components across different failure domains (e.g., multiple availability zones).

etcd Backup Strategies  
- Schedule regular snapshots, especially for critical production clusters.  
- Test restore procedures regularly to validate backup integrity.  
- Keep backups on secure and durable storage (e.g., AWS S3 with versioning).

Node Scalability (Horizontal Scaling)  
- Utilize the Kubernetes Cluster Autoscaler to add or remove nodes based on resource demands.  
- Use the Horizontal Pod Autoscaler (HPA) to scale the number of pod replicas automatically based on CPU/memory usage or custom metrics.  
- Ensure that requests/limits in pod specs are set accurately to avoid node overcommitment or resource starvation.

Hands-On Implementation with Minikube
-------------------------------------
Prerequisites  
- Install Minikube and kubectl.  
- Ensure you have a local hypervisor or Docker environment available.

Start Minikube  
```bash
minikube start --driver=docker
```
This launches a single-node Kubernetes cluster in a local VM or container. All control plane components (API Server, Scheduler, Controller Manager, etcd) and the single worker node run on this VM/container.

Inspect the Cluster  
```bash
kubectl cluster-info
kubectl get nodes
kubectl get pods -n kube-system
```
You will see system pods like coredns (for DNS), kube-proxy, and possibly a metrics-server if installed. These confirm that the control plane and node plane are functional.

Deploy a Simple Microservice  
1. Create a Deployment (nginx) that runs two replicas.  
2. Expose it as a NodePort service on port 30080.  

Deployment (nginx-deployment.yaml):
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
spec:
  replicas: 2
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
        image: nginx:stable
        ports:
        - containerPort: 80
```
```bash
kubectl apply -f nginx-job.yaml
```

Service (nginx-service.yaml):
```yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-service
spec:
  type: NodePort
  selector:
    app: nginx
  ports:
    - port: 80
      targetPort: 80
      nodePort: 30080
```
```bash
kubectl apply -f nginx-service.yaml
kubectl get pods
kubectl get svc
```
You should see two nginx pods running and a NodePort service on port 30080.

Access the Service  
```bash
minikube service nginx-service
```
Minikube opens a browser to the NodePort URL, showing the default Nginx page.

Inspect Logs & Events  
- Use `kubectl logs <pod-name>` to view container output.  
- Use `kubectl describe pod <pod-name>` to see details, including scheduling decisions, image pulls, and events.

Use Cases & Observations
------------------------
- The API Server stores the desired Deployment (replicas = 2) in etcd.  
- The Scheduler binds each pod to the Minikube node based on resource availability.  
- The Controller Manager ensures the replica count remains two. If a pod is deleted, another is spun up.  
- The Kubelet on the node receives the pod spec and starts the nginx container.  
- Kube Proxy sets up iptables or IPVS rules to route requests from the NodePort to the nginx pods.


---

### **Cluster Setup & Day-to-Day Operations**  
**Objective**: Build expertise in cluster lifecycle management, operational security, and workload orchestration for enterprise-grade Kubernetes environments.
---

### **I. Core Concepts & Definitions**  
#### **1. Cluster Installation & Configuration**  
- **Local vs. Managed Clusters**:  
  - **Minikube**: A local Kubernetes cluster for development/testing. Simulates core components but lacks cloud-native integrations (e.g., auto-scaling).  
  - **Managed Clusters (EKS, GKE, AKS)**: Cloud-hosted control planes with automated scaling, upgrades, and integrations (e.g., AWS VPC CNI, CSI drivers).  
  - **Key Differences**:  
    | **Aspect**          | **Minikube**                          | **Managed (EKS)**                     |  
    |----------------------|---------------------------------------|---------------------------------------|  
    | Control Plane        | Single-node, local                   | HA, cloud-managed                     |  
    | Node Management      | Manual (`minikube node add`)         | Auto-scaling groups                  |  
    | Networking           | Simplified (Docker bridge)           | Cloud VPC integration                |  

- **Why It Matters**:  
  - **Local Clusters**: Rapid prototyping, cost-free experimentation.  
  - **Managed Clusters**: Production-grade reliability, compliance, and scalability.  

#### **2. RBAC (Role-Based Access Control)**  
- **Definitions**:  
  - **Role**: Permissions within a namespace (e.g., read pods).  
  - **ClusterRole**: Permissions cluster-wide (e.g., create nodes).  
  - **RoleBinding**: Assigns a Role to a user/group in a namespace.  
  - **ClusterRoleBinding**: Assigns a ClusterRole cluster-wide.  
- **Purpose**: Enforce least-privilege access (e.g., restrict developers to `staging` namespace).  
- **Real-World Context**: At NVIDIA, RBAC ensures engineers only access GPU-specific resources they’re authorized for.  

#### **3. Resource Quotas**  
- **What They Do**: Enforce limits on namespace resources (CPU, memory, storage, GPU).  
  ```yaml
  spec:
    hard:
      requests.nvidia.com/gpu: 4  # Limit GPUs per namespace
  ```  
- **Why It Matters**: Prevents resource hogging in shared clusters (e.g., a team overusing GPUs).  

#### **4. Horizontal Pod Autoscaling (HPA)**  
- **Definition**: Automatically scales pods based on metrics (CPU, memory, custom).  
- **How It Works**:  
  1. Metrics Server collects pod CPU/memory.  
  2. HPA adjusts replica count to meet target (e.g., CPU < 50%).  
- **Use Case**: Bursty AI/ML workloads requiring dynamic GPU allocation.  

#### **5. Taints & Tolerations**  
- **Definitions**:  
  - **Taint**: A node property that repels pods unless they "tolerate" it.  
  - **Toleration**: A pod property that allows it to schedule on tainted nodes.  
- **Purpose**: Reserve nodes for specialized workloads (e.g., GPU nodes for AI jobs).  
- **NVIDIA Context**: Taint GPU nodes to ensure only CUDA workloads use them.  

---

### **II. Design Considerations Explained**  
#### **1. Multi-Zone vs. Single-Zone Clusters**  
- **Single-Zone**:  
  - **Pros**: Simpler networking, lower latency.  
  - **Cons**: No fault tolerance (zone outage = cluster downtime).  
- **Multi-Zone**:  
  - **Pros**: High availability, fault tolerance.  
  - **Cons**: Complex networking, higher cost.  
- **NVIDIA Scenario**: Multi-zone clusters for distributed GPU training jobs.  

#### **2. Storage Provisioning**  
- **Persistent Volume (PV)**: Cluster-wide storage resource (e.g., AWS EBS).  
- **Persistent Volume Claim (PVC)**: A user’s request for storage.  
- **StorageClass**: Defines provisioner and parameters (e.g., `gp2` on AWS).  
- **Why It Matters**: Stateful AI workloads (e.g., training datasets) require persistent storage.  

#### **3. Upgrade Strategies**  
- **Rolling Upgrades**:  
  - Gradually replace nodes/pods with minimal downtime.  
  - **Best For**: Stateless apps.  
- **Blue/Green**:  
  - Deploy new cluster version alongside old, then switch traffic.  
  - **Best For**: Stateful apps needing zero downtime.  
- **NVIDIA Context**: Rolling upgrades for GPU driver updates.  

---

### **III. Real-World Use Cases**  
#### **1. Staging vs. Production Environments**  
- **Staging**:  
  - **Resource Quotas**: Lenient limits for testing.  
  - **RBAC**: Developers have write access.  
- **Production**:  
  - **Quotas**: Strict CPU/GPU limits.  
  - **RBAC**: Read-only access for auditors.  

#### **2. Custom RBAC for GPU Engineers**  
- **Scenario**: Allow NVIDIA engineers to schedule pods on GPU nodes but not delete nodes.  
  ```yaml
  # gpu-engineer-role.yaml
  rules:
  - apiGroups: [""]
    resources: ["pods"]
    verbs: ["create", "get"]
  - apiGroups: ["apps"]
    resources: ["daemonsets"]  # For GPU device plugins
    verbs: ["list"]
  ```  

#### **3. Node Taints for GPU Pools**  
- **Step 1**: Label GPU nodes.  
  ```bash
  kubectl label nodes node-1 accelerator=nvidia-gpu
  ```  
- **Step 2**: Taint nodes to repel non-GPU workloads.  
  ```bash
  kubectl taint nodes node-1 nvidia.com/gpu=true:NoSchedule
  ```  

---

### **IV. Pre-Lab Preparation**  
#### **1. Minikube Multi-Node Setup**  
```bash
minikube start --nodes=3 --driver=docker  # 1 control-plane, 2 workers
minikube node list  # Verify nodes (minikube, minikube-m02, minikube-m03)
```  

#### **2. Simulate Cloud Features in Minikube**  
- **AWS Load Balancer Controller (Mock)**:  
  ```bash
  kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/aws/deploy.yaml
  ```  
- **Persistent Storage**:  
  ```bash
  kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/master/deploy/local-path-storage.yaml
  ```  

---

### **V. Key Principles for NVIDIA Engineers**  
1. **GPU Resource Isolation**: Use taints, labels, and quotas to prevent non-GPU workloads from consuming GPU nodes.  
2. **Disaster Recovery**: Regularly back up etcd and test GPU workload rescheduling.  
3. **Upgrade Safety**: Validate GPU driver compatibility before cluster upgrades.  

---

### **Next Steps**:  
1. **Lab 1**: Configure RBAC to allow a "staging-viewer" service account to list pods but not modify them.  
2. **Lab 2**: Create a GPU-tolerating pod and schedule it on a tainted node in Minikube.  
3. **Lab 3**: Simulate a multi-zone deployment using node labels and pod anti-affinity.  

---

### **Project Plan: Multi-Environment NGINX Deployment**  
**Objective**: Deploy NGINX across "staging" and "production" namespaces with RBAC, resource quotas, GPU taints, and HPA.  

---

### **Step 1: Cluster Setup (Minikube)**  
**Goal**: Create a multi-node cluster with 1 control plane and 2 worker nodes.  
```bash
minikube start --nodes=3 --driver=docker  # 3-node cluster
minikube node list  # Verify nodes: minikube (control-plane), minikube-m02, minikube-m03
```

---

### **Step 2: Namespace & RBAC Setup**  
**Goal**: Define `staging` and `production` namespaces with custom RBAC roles.  

#### **1. Namespaces**  
```yaml
# namespaces.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: staging
---
apiVersion: v1
kind: Namespace
metadata:
  name: production
```
```bash
kubectl apply -f namespaces.yaml
```

#### **2. RBAC for Staging**  
**Scenario**: Allow `staging-viewer` to view pods but not modify them.  
```yaml
# staging-rbac.yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: staging-pod-viewer
  namespace: staging
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list", "watch"]
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: staging-viewer
  namespace: staging
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: staging-pod-viewer-binding
  namespace: staging
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: staging-pod-viewer
subjects:
- kind: ServiceAccount
  name: staging-viewer
  namespace: staging
```

#### **3. RBAC for Production**  
**Scenario**: Restrict `production-admin` to manage deployments only.  
```yaml
# production-rbac.yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: production-deployer
  namespace: production
rules:
- apiGroups: ["apps"]
  resources: ["deployments"]
  verbs: ["create", "delete", "patch"]
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: production-admin
  namespace: production
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: production-deployer-binding
  namespace: production
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: production-deployer
subjects:
- kind: ServiceAccount
  name: production-admin
  namespace: production
```

---

### **Step 3: Resource Quotas & Limits**  
**Goal**: Enforce resource constraints per namespace.  

#### **1. Staging Quota (Lenient)**  
```yaml
# staging-quota.yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: staging-quota
  namespace: staging
spec:
  hard:
    requests.cpu: "4"
    requests.memory: 8Gi
    limits.cpu: "8"
    limits.memory: 16Gi
```

#### **2. Production Quota (Strict)**  
```yaml
# production-quota.yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: production-quota
  namespace: production
spec:
  hard:
    requests.cpu: "2"
    requests.memory: 4Gi
    limits.cpu: "4"
    limits.memory: 8Gi
    pods: "10"  # Max 10 pods in production
```

---

### **Step 4: Taints & Tolerations (GPU Simulation)**  
**Goal**: Reserve `minikube-m02` for "GPU workloads" and schedule NGINX there.  

#### **1. Taint a Node**  
```bash
kubectl taint nodes minikube-m02 accelerator=gpu:NoSchedule
```

#### **2. Deploy Tolerating NGINX**  
```yaml
# gpu-nginx.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gpu-nginx
  namespace: production
spec:
  replicas: 1
  selector:
    matchLabels:
      app: gpu-nginx
  template:
    metadata:
      labels:
        app: gpu-nginx
    spec:
      containers:
      - name: nginx
        image: nginx:latest
        resources:
          requests:
            cpu: "0.5"
            memory: "256Mi"
          limits:
            cpu: "1"
            memory: "512Mi"
      tolerations:
      - key: "accelerator"
        operator: "Equal"
        value: "gpu"
        effect: "NoSchedule"
      nodeSelector:
        kubernetes.io/hostname: minikube-m02  # Target the tainted node
```

---

### **Step 5: Horizontal Pod Autoscaling (HPA)**  
**Goal**: Auto-scale NGINX in production based on CPU.  

#### **1. Deploy HPA**  
```bash
kubectl autoscale deployment gpu-nginx -n production --cpu-percent=50 --min=1 --max=3
```

#### **2. Generate Load**  
```bash
kubectl run load-test -n production --image=busybox -- /bin/sh -c "while true; do wget -q -O- http://gpu-nginx; done"
```

#### **3. Monitor HPA**  
```bash
watch kubectl get hpa -n production
```

---

### **Step 6: Validate RBAC**  
**Goal**: Test if `staging-viewer` can’t modify resources.  

#### **1. Impersonate Service Account**  
```bash
# Test pod listing (should work)
kubectl get pods -n staging --as=system:serviceaccount:staging:staging-viewer

# Test deployment creation (should fail)
kubectl create deployment test -n staging --image=nginx --as=system:serviceaccount:staging:staging-viewer
```

---

### **Step 7: Upgrade Simulation**  
**Goal**: Perform a zero-downtime cluster upgrade.  

#### **1. Drain a Node**  
```bash
kubectl drain minikube-m03 --ignore-daemonsets --delete-emptydir-data
```

#### **2. Upgrade Minikube**  
```bash
minikube stop
minikube start --kubernetes-version=v1.25.0 --nodes=3 --driver=docker
```

---

### **Step 8: Disaster Recovery Test**  
**Goal**: Delete a pod and verify self-healing.  
```bash
kubectl delete pod -n production -l app=gpu-nginx --force
kubectl get pods -n production -w  # Watch pod restart
```

---

### **Step 9: Cleanup**  
```bash
minikube delete --all
```

---

### **Key Learnings Validated**  
1. **RBAC**: Namespace-scoped roles vs. cluster roles.  
2. **Taints/Tolerations**: Isolate workloads to specific nodes.  
3. **Quotas**: Enforce resource limits per environment.  
4. **HPA**: Auto-scale stateless apps under load.  
5. **Upgrades**: Graceful node draining.  

---

### **NVIDIA-Specific Extensions**  
1. Replace NGINX with a GPU-based workload (e.g., CUDA app).  
2. Use `nvidia.com/gpu` resource limits in the pod spec.  
3. Label nodes with `accelerator: nvidia-tesla-v100`.  

---

### **Next Steps**  
1. Add persistent storage (PVC) for NGINX logs.  
2. Implement network policies to restrict traffic between namespaces.  
3. Integrate Prometheus monitoring for HPA metrics.  

---

### **1. What is a Cluster?**  
A Kubernetes cluster is a **group of machines (physical/virtual)** working together to run containerized applications. It has two key parts:  
- **Control Plane (Master)**: The "brain" managing the cluster’s desired state.  
- **Worker Nodes**: The "muscle" executing workloads (pods).  

![Cluster Architecture](https://miro.medium.com/v2/resize:fit:720/format:webp/1*NlVJg7yzL-1hG2tEAfGXJQ.png)

---

### **2. What is a Node?**  
A node is a **single machine** (VM or physical server) in the cluster. There are two types:  
- **Control Plane Node**: Hosts components like the API Server, Scheduler, etc.  
- **Worker Node**: Runs pods (your apps) via the Kubelet and Kube Proxy.  

---

### **3. How Components Tie Together**  
#### **Scenario**: Deploying a Pod  
Let’s see how the API Server, etcd, Scheduler, and Kubelet collaborate when you run `kubectl apply -f pod.yaml`:

1. **You (User)**:  
   ```bash
   kubectl apply -f pod.yaml  # Sends a request to the API Server
   ```

2. **API Server**:  
   - **Role**: The **front door** to the cluster.  
   - **Action**:  
     - Validates your request.  
     - Stores the pod’s desired state in **etcd**.  
     - Notifies other components (e.g., Scheduler).  

3. **etcd**:  
   - **Role**: The cluster’s **source of truth** (key-value database).  
   - **Action**: Persistently stores the pod’s configuration.  

4. **Scheduler**:  
   - **Role**: The **matchmaker** for pods and nodes.  
   - **Action**:  
     - Watches the API Server for unassigned pods.  
     - Selects a node based on resources, labels, taints, etc.  
     - Tells the API Server: “Schedule this pod on Node X.”  

5. **Kubelet (on Node X)**:  
   - **Role**: The **node agent**.  
   - **Action**:  
     - Watches the API Server for pods assigned to its node.  
     - Creates the pod (via Docker/containerd).  
     - Reports pod status back to the API Server → etcd.  

6. **Kube Proxy**:  
   - **Role**: The **network traffic cop**.  
   - **Action**: Updates iptables/IPVS rules to route traffic to pods.  

---

### **4. Key Interactions**  
#### **Control Plane Components**  
| Component         | Talks to...               | Purpose                                                                 |  
|--------------------|---------------------------|-------------------------------------------------------------------------|  
| **API Server**     | Everyone                  | All cluster operations go through it.                                  |  
| **etcd**           | API Server                | Stores cluster state (pods, nodes, secrets, etc.).                     |  
| **Scheduler**      | API Server                | Watches for unscheduled pods → assigns nodes.                          |  
| **Controller Manager** | API Server           | Ensures actual state matches desired state (e.g., restarts failed pods). |  

#### **Worker Node Components**  
| Component         | Talks to...               | Purpose                                                                 |  
|--------------------|---------------------------|-------------------------------------------------------------------------|  
| **Kubelet**        | API Server                | “My node has these resources.” / “This pod is running.”                 |  
| **Kube Proxy**     | API Server                | “Services changed? Update networking rules.”                           |  

---

### **5. Real-World Analogy**  
Think of Kubernetes as a **restaurant**:  
- **API Server** = Host/Hostess: Takes orders (requests) and coordinates staff.  
- **etcd** = Kitchen Logbook: Records every order and its status.  
- **Scheduler** = Head Chef: Assigns orders (pods) to cooks (nodes).  
- **Kubelet** = Line Cook: Prepares dishes (pods) as instructed.  
- **Kube Proxy** = Waitstaff: Ensures dishes (traffic) go to the right tables (pods).  

---

### **6. Why This Matters for NVIDIA**  
- **GPU Workloads**: The Scheduler ensures GPU-intensive pods land on nodes with NVIDIA GPUs (using taints/tolerations).  
- **Scalability**: The API Server and etcd handle thousands of nodes/pods in AI/ML clusters.  
- **High Availability**: Multi-master setups (multiple API Servers/etcd nodes) prevent downtime during failures.  

---

### **7. Example Flow with Taints/Tolerations**  
**Scenario**: Schedule a pod on a GPU node.  

1. **Taint the GPU Node**:  
   ```bash
   kubectl taint nodes node-1 nvidia.com/gpu=true:NoSchedule
   ```  
2. **Pod Spec with Toleration**:  
   ```yaml
   tolerations:
   - key: "nvidia.com/gpu"
     operator: "Exists"
     effect: "NoSchedule"
   ```  
3. **Scheduler’s Decision**:  
   - Ignores non-GPU nodes.  
   - Assigns the pod to `node-1` (if resources are available).  

---

### **8. Summary Diagram**  
```
You (kubectl)  
   │  
   ▼  
API Server ←→ etcd (cluster state)  
   │  ▲  
   │  └── Scheduler (assigns pods)  
   │  
   ▼  
Kubelet (on nodes) ←→ Kube Proxy (network rules)
```

--- 

### **Key Takeaway**  
The **API Server** is the central nervous system. Every component either:  
- Watches it for changes (e.g., Scheduler, Kubelet).  
- Updates it with new state (e.g., etcd, Kubelet).  

**Without the API Server, the cluster is brain-dead.**
---

### **I. Core Concepts & Architecture**  
#### **1. Kubernetes Debugging Fundamentals**  
**Why Observability Matters**:  
- **Cluster Health**: Detect node failures, resource starvation, or misconfigurations.  
- **Application Health**: Identify crashing pods, slow APIs, or memory leaks.  
- **Performance**: Optimize resource usage (CPU/GPU, memory, network).  

**Key Components**:  
- **Metrics**: Quantitative data (e.g., CPU usage, request latency).  
- **Logs**: Time-ordered events from apps/system components.  
- **Traces**: Distributed transaction flow (e.g., Jaeger).  

---

### **II. Debugging Tools Deep Dive**  
#### **1. `kubectl` Debugging Commands**  
- **`kubectl logs`**:  
  - **Purpose**: Fetch logs from a pod’s container.  
  - **Pro Tip**: Use `-f` to follow logs in real-time.  
  - **Example**:  
    ```bash
    kubectl logs -n production gpu-nginx-xyz --tail=100  # Last 100 lines
    ```

- **`kubectl describe`**:  
  - **Purpose**: Get detailed state/events of a resource (pod, node, service).  
  - **Key Fields**:  
    - `Events`: Scheduling failures, image pull errors, etc.  
    - `Conditions`: Pod readiness, memory pressure.  
  - **Example**:  
    ```bash
    kubectl describe pod gpu-nginx-xyz -n production | grep -A 20 Events
    ```

- **`kubectl exec`**:  
  - **Purpose**: Execute commands in a running container.  
  - **Use Case**: Debug networking (e.g., `curl`, `dig`) or inspect files.  
  - **Example**:  
    ```bash
    kubectl exec -it gpu-nginx-xyz -n production -- nvidia-smi  # Check GPU status
    ```

- **`kubectl get events`**:  
  - **Purpose**: Cluster-wide event stream (e.g., node failures, OOM kills).  
  - **Filtering**:  
    ```bash
    kubectl get events --field-selector=involvedObject.kind=Pod --sort-by=.lastTimestamp
    ```

#### **2. Understanding `CrashLoopBackOff`**  
**Causes**:  
1. **Application Error**: Unhandled exception in code.  
2. **Misconfigured Probes**: Liveness/readiness checks failing.  
3. **Resource Limits**: OOM (Out-of-Memory) kills.  

**Debugging Workflow**:  
1. Check logs: `kubectl logs <pod> --previous` (for crashed containers).  
2. Inspect events: `kubectl describe pod <pod>`.  
3. Test with `kubectl exec` to manually trigger the app.  

---

### **III. Observability Stack**  
#### **1. Metrics Pipeline**  
![Observability Stack](https://matthewpalmer.net/kubernetes-app-developer/articles/prometheus-kubernetes-architecture.png)  

- **Metrics Server**:  
  - **Role**: Collects node/pod CPU/memory metrics for `kubectl top` and HPA.  
  - **Installation**:  
    ```bash
    minikube addons enable metrics-server
    kubectl top nodes  # Verify
    ```

- **Prometheus**:  
  - **Role**: Time-series database for custom metrics (e.g., HTTP requests/sec, GPU utilization).  
  - **Components**:  
    - **Prometheus Operator**: Manages Prometheus instances.  
    - **Exporters**: Node Exporter, NVIDIA GPU Exporter.  

- **Grafana**: Visualization layer for Prometheus metrics.  

#### **2. Instrumentation Strategies**  
- **Application-Level Metrics**:  
  - Expose `/metrics` endpoint using Prometheus client libraries (Python, Go).  
  - **Example (Python)**:  
    ```python
    from prometheus_client import start_http_server, Counter
    REQUEST_COUNT = Counter('http_requests_total', 'Total HTTP Requests')
    @app.route('/')
    def home():
        REQUEST_COUNT.inc()
        return "Hello, GPU!"
    ```

- **Kubernetes Cluster Metrics**:  
  - Monitor etcd latency, API Server request rate, node disk pressure.  

---

### **IV. Hands-On Labs**  
#### **Lab 1: Simulate & Debug `CrashLoopBackOff`**  
**Scenario**: A GPU pod crashes due to missing CUDA drivers.  

1. **Deploy a Broken Pod**:  
   ```yaml
   # broken-gpu-pod.yaml
   apiVersion: v1
   kind: Pod
   metadata:
     name: cuda-pod
     namespace: production
   spec:
     containers:
     - name: cuda
       image: nvidia/cuda:12.0-base
       command: ["/bin/sh", "-c", "sleep 60; exit 1"]  # Intentional crash
     tolerations:
     - key: "accelerator"
       operator: "Equal"
       value: "gpu"
   ```

2. **Debug**:  
   ```bash
   kubectl logs cuda-pod -n production --previous  # Check why the last container died
   kubectl describe pod cuda-pod -n production     # Look for "Last State"
   ```

#### **Lab 2: Liveness/Readiness Probes**  
**Goal**: Ensure a pod is restarted if its GPU becomes unresponsive.  

1. **Deploy with Probes**:  
   ```yaml
   # gpu-probes.yaml
   livenessProbe:
     exec:
       command: ["nvidia-smi"]  # Fails if GPU is unavailable
     initialDelaySeconds: 30
   readinessProbe:
     httpGet:
       path: /healthz
       port: 8080
   ```

2. **Trigger Failure**:  
   ```bash
   kubectl exec -it gpu-pod -- pkill nvidia-smi  # Simulate GPU hang
   kubectl get pods -w  # Watch restart count
   ```

#### **Lab 3: Prometheus + Grafana Setup**  
1. **Install Prometheus Operator**:  
   ```bash
   helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
   helm install prometheus prometheus-community/kube-prometheus-stack
   ```

2. **Access Grafana**:  
   ```bash
   kubectl port-forward svc/prometheus-grafana 3000:80
   ```
   - Login: `admin/prom-operator`  
   - Import dashboard `3119` for Kubernetes cluster metrics.  

3. **Monitor GPU Metrics**:  
   - Deploy NVIDIA GPU Exporter:  
     ```bash
     kubectl apply -f https://raw.githubusercontent.com/NVIDIA/gpu-monitoring-tools/master/exporters/prometheus-dcgm/k8s/prometheus-dcgm.yaml
     ```
   - Grafana Query: `DCGM_FI_DEV_GPU_UTIL` (GPU utilization %).  

---

### **V. Design Considerations**  
#### **1. Alerting Strategies**  
- **Critical Alerts**:  
  - `Cluster CPU/Memory Overcommit` → Risk of pod evictions.  
  - `GPU Utilization > 90%` → Potential training job slowdowns.  
- **Tools**:  
  - **Prometheus Alertmanager**: Send alerts to Slack/Email.  
  - **Custom Rules**:  
    ```yaml
    # prometheus-rules.yaml
    - alert: HighGPUUsage
      expr: DCGM_FI_DEV_GPU_UTIL > 90
      for: 5m
      labels:
        severity: critical
    ```

#### **2. SLA Tracking**  
- **Key Metrics**:  
  - Uptime: `sum(up{job="my-gpu-service"} == 1) / count(up{job="my-gpu-service"})`  
  - Request Latency: `histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m]))`  

---

### **VI. Use Cases**  
#### **1. Stress Testing & Autoscaling**  
**Scenario**: Auto-scale a GPU inference service under load.  

1. **Deploy HPA with Custom Metrics**:  
   ```bash
   kubectl autoscale deployment gpu-inference --min=1 --max=5 --cpu-percent=50
   ```

2. **Generate Load**:  
   ```bash
   kubectl run -n production load-test --image=busybox -- \
     /bin/sh -c "while true; do wget -q -O- http://gpu-inference; done"
   ```

3. **Monitor**:  
   - Grafana: Track GPU utilization and pod scaling.  
   - `kubectl get hpa -w`  

#### **2. Node Failure Simulation**  
**Steps**:  
1. **Cordon a Node**:  
   ```bash
   kubectl cordon minikube-m02  # Prevent new pods
   ```
2. **Drain the Node**:  
   ```bash
   kubectl drain minikube-m02 --ignore-daemonsets --delete-emptydir-data
   ```
3. **Observe**:  
   - Prometheus alerts for node downtime.  
   - Pods rescheduled to healthy nodes.  

---

### **VII. NVIDIA-Specific Scenarios**  
#### **1. GPU-Specific Metrics**  
- **Key Metrics**:  
  - `DCGM_FI_DEV_MEM_COPY_UTIL`: GPU memory bandwidth usage.  
  - `DCGM_FI_DEV_GPU_TEMP`: GPU temperature (avoid thermal throttling).  

#### **2. Debugging GPU OOM**  
**Symptoms**:  
- `nvidia-smi` shows high memory usage.  
- Pods evicted with `OOMKilled`.  

**Fix**:  
- Adjust pod memory limits:  
  ```yaml
  resources:
    limits:
      nvidia.com/gpu: 2
      memory: 16Gi
  ```

---

### **VIII. Tools & Resources**  
- **Log Aggregation**: Loki + Grafana (alternative to ELK).  
- **Advanced Tracing**: Jaeger for distributed tracing in CUDA microservices.  
- **Cheat Sheet**: [Kubernetes Debugging Commands](https://learnk8s.io/troubleshooting-deployments).  

---

### **Next Steps**:  
1. **Lab 1**: Deploy a GPU app with intentional OOM errors and debug using Prometheus.  
2. **Lab 2**: Configure Alertmanager to notify you when GPU temps exceed 85°C.  
3. **Phase 4**: Explore **Security & Governance** (Pod Security Policies, OPA Gatekeeper).  


---

### **Advanced Workload Patterns & Resource Optimization**  
**Objective**: Understand and apply Kubernetes workload types, scheduling strategies, and storage patterns for stateful, batch, and specialized applications.  

---

### **I. Core Concepts & Definitions**  
#### **1. Workload Types**  
##### **DaemonSet**  
- **What**: A controller that ensures a copy of a pod runs on **all** (or a subset of) nodes.  
- **Why**:  
  - Deploy cluster-wide services (e.g., logging, monitoring).  
  - Install node-specific software (e.g., GPU drivers).  
- **Key Features**:  
  - Automatically adds/removes pods as nodes join/leave the cluster.  
  - Uses node selectors/affinity to target specific nodes.  

##### **StatefulSet**  
- **What**: Manages stateful applications with **stable identities** and **persistent storage**.  
- **Why**:  
  - Databases (MySQL, Cassandra), distributed systems (ZooKeeper).  
  - Requires predictable hostnames, ordered scaling, and durable storage.  
- **Key Features**:  
  - Pods are named sequentially (`web-0`, `web-1`).  
  - PersistentVolumeClaims (PVCs) are retained even if pods are deleted.  

##### **Job**  
- **What**: Runs a pod to **completion** (e.g., batch processing, one-off tasks).  
- **Why**:  
  - Data processing, report generation, machine learning inference.  
  - Ensures a task finishes successfully (retries on failure).  

##### **CronJob**  
- **What**: Runs Jobs on a **schedule** (e.g., hourly, daily).  
- **Why**:  
  - Periodic backups, nightly model training, cleanup tasks.  

---

#### **2. Advanced Scheduling**  
##### **Node Affinity/Anti-Affinity**  
- **What**: Rules to **attract** or **repel** pods from nodes based on labels.  
- **Why**:  
  - Colocate pods for performance (e.g., cache + app).  
  - Spread pods across zones for high availability.  
- **Key Terms**:  
  - `requiredDuringScheduling`: Hard rule (pod won’t schedule if not met).  
  - `preferredDuringScheduling`: Soft preference.  

##### **Taints & Tolerations**  
- **What**:  
  - **Taint**: A node property that **repels** pods unless they tolerate it.  
  - **Toleration**: A pod property that allows scheduling on tainted nodes.  
- **Why**:  
  - Reserve nodes for specific workloads (e.g., GPU nodes, memory-heavy tasks).  

##### **Resource Fragmentation**  
- **What**: Cluster resources (CPU, memory) become **underutilized** due to inefficient scheduling.  
- **Why It Matters**: Wastes resources, increases costs.  
- **Solution**:  
  - **Bin packing**: Schedule pods to fill nodes as tightly as possible.  
  - **Overcommitment**: Allow pods to exceed node capacity (risky!).  

---

#### **3. Storage Strategies**  
##### **Ephemeral Storage**  
- **What**: Short-lived storage tied to a pod’s lifecycle (e.g., `emptyDir`).  
- **Why**:  
  - Scratch space for temporary data (caches, build artifacts).  
  - No persistence needed beyond the pod’s lifetime.  

##### **Persistent Storage**  
- **What**: Long-term storage that survives pod restarts/deletions (e.g., PVCs).  
- **Why**:  
  - Databases, user uploads, training datasets.  
- **Key Components**:  
  - **PersistentVolume (PV)**: Cluster-wide storage resource (e.g., AWS EBS).  
  - **PersistentVolumeClaim (PVC)**: A user’s request for storage.  

---

### **II. Use Cases & Real-World Scenarios**  
#### **1. DaemonSet**  
- **Non-GPU**: Deploy `fluentd` logging agents on all nodes.  
- **GPU**: Install the NVIDIA GPU device plugin on GPU-enabled nodes.  

#### **2. StatefulSet**  
- **Non-GPU**: Deploy a Kafka cluster with persistent storage for message durability.  
- **GPU**: Distributed deep learning training with checkpointing (e.g., PyTorch Elastic).  

#### **3. Job**  
- **Non-GPU**: Process a daily batch of customer transactions.  
- **GPU**: Run a one-off machine learning inference task on a GPU node.  

#### **4. CronJob**  
- **Non-GPU**: Delete temporary files every night at 2 AM.  
- **GPU**: Retrain a recommendation model weekly using GPU resources.  

---

### **III. Implementation Examples**  
#### **1. DaemonSet for Logging (Non-GPU)**  
```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: fluentd
spec:
  selector:
    matchLabels:
      name: fluentd
  template:
    metadata:
      labels:
        name: fluentd
    spec:
      containers:
      - name: fluentd
        image: fluent/fluentd
        volumeMounts:
        - name: varlog
          mountPath: /var/log
      volumes:
      - name: varlog
        hostPath:
          path: /var/log
```

#### **2. StatefulSet for Database (Non-GPU)**  
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: mysql
spec:
  serviceName: mysql
  replicas: 3
  selector:
    matchLabels:
      app: mysql
  template:
    metadata:
      labels:
        app: mysql
    spec:
      containers:
      - name: mysql
        image: mysql:8.0
        volumeMounts:
        - name: data
          mountPath: /var/lib/mysql
  volumeClaimTemplates:
  - metadata:
      name: data
    spec:
      accessModes: [ "ReadWriteOnce" ]
      storageClassName: fast-ssd
      resources:
        requests:
          storage: 100Gi
```

#### **3. GPU Job (Optional Extension)**  
```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: gpu-inference
spec:
  template:
    spec:
      containers:
      - name: inference
        image: nvcr.io/nvidia/tensorrt:22.12-py3
        command: ["python", "inference.py"]
        resources:
          limits:
            nvidia.com/gpu: 1  # Requires NVIDIA device plugin
      tolerations:
      - key: "nvidia.com/gpu"
        operator: "Exists"
        effect: "NoSchedule"
      restartPolicy: Never
```

---

### **IV. Design Considerations**  
#### **1. Choosing Workload Types**  
| **Scenario**                | **Workload Type** | **Reason**                              |  
|------------------------------|-------------------|-----------------------------------------|  
| Node-level monitoring agent  | DaemonSet         | Run on every node.                      |  
| Database with replication    | StatefulSet       | Stable hostnames, persistent storage.  |  
| Monthly report generation    | CronJob           | Scheduled task.                         |  

#### **2. Storage Best Practices**  
- **Ephemeral**: Use for temporary data (no backups needed).  
- **Persistent**:  
  - Choose `ReadWriteOnce` for databases, `ReadWriteMany` for shared datasets.  
  - Use `storageClassName` to match performance tiers (e.g., `fast-ssd`).  

#### **3. Avoiding Resource Fragmentation**  
- **Strategy**:  
  - Use `kubectl top nodes` to monitor utilization.  
  - Set resource `requests` close to `limits` for predictable scheduling.  

---

### **V. Troubleshooting Guide**  
#### **1. Pods Not Scheduling**  
- **Check**:  
  ```bash
  kubectl describe pod <pod> | grep Events  # Look for taint/toleration mismatches
  kubectl get nodes -o json | jq '.items[].status.allocatable'  # Resource availability
  ```

#### **2. Persistent Volume Claims Pending**  
- **Fix**:  
  - Ensure `storageClassName` matches an existing StorageClass.  
  - Verify the storage backend has enough capacity.  

#### **3. Job Failing Repeatedly**  
- **Diagnose**:  
  ```bash
  kubectl logs job/<job-name> --previous  # Logs from the last failed attempt
  kubectl describe job/<job-name>         # Check completion/backoff limits
  ```

---

### **VI. Summary**  
- **DaemonSets** → Cluster-wide services.  
- **StatefulSets** → Stateful apps with stable identities.  
- **Jobs/CronJobs** → Finite or scheduled tasks.  
- **Advanced Scheduling** → Optimize placement and resource usage.  
- **Storage** → Match persistence needs to workload requirements.  

---

### **Next Steps**:  
1. **Lab 1**: Deploy a StatefulSet for Redis with persistent storage.  
2. **Lab 2**: Create a CronJob to clean up temporary files daily.  
3. **Lab 3**: Simulate resource fragmentation and apply bin packing.  



### **Security & Best Practices**  
**Objective**: Harden Kubernetes clusters against threats, enforce compliance, and adopt a zero-trust security model for enterprise-grade workloads.  

---

### **I. Core Concepts & Definitions**  
#### **1. Network Policies**  
- **What**: Rules governing communication between pods/namespaces.  
- **Why**: Prevent lateral movement in breaches (e.g., restrict frontend pods to only talk to backend).  
- **Key Tools**:  
  - **Calico**: Policy engine for granular control (L3-L4).  
  - **Cilium**: L7-aware policies (HTTP/gRPC).  

#### **2. Pod Security Context**  
- **What**: Security settings applied at the pod/container level.  
- **Why**: Mitigate privilege escalation (e.g., disallow root access, drop capabilities).  
- **Key Settings**:  
  - `runAsNonRoot: true`  
  - `readOnlyRootFilesystem: true`  
  - `capabilities.drop: ["ALL"]`  

#### **3. Secrets Management**  
- **What**: Securely store sensitive data (passwords, tokens, keys).  
- **Why**: Avoid exposing secrets in plaintext (e.g., in YAML files).  
- **Tools**:  
  - **Kubernetes Secrets** (base64-encoded, encrypted with etcd KMS).  
  - **HashiCorp Vault**: External secrets management with dynamic credentials.  

#### **4. Admission Controllers**  
- **What**: Intercept API requests to enforce policies before resource creation.  
- **Key Types**:  
  - **PodSecurityPolicy (Deprecated)**: Replaced by **Pod Security Admission (PSA)**.  
  - **OPA/Gatekeeper**: Custom policies (e.g., "No privileged containers").  

---

### **II. Design Considerations**  
#### **1. Zero-Trust Networking**  
- **Principle**: "Never trust, always verify."  
- **Implementation**:  
  - **Default-Deny**: Block all traffic unless explicitly allowed.  
  - **Microsegmentation**: Restrict pods to required ports/peers.  

#### **2. Encryption**  
- **In Transit**: TLS for API Server, service mesh (Istio/Linkerd).  
- **At Rest**: Encrypt etcd, PersistentVolumes (e.g., AWS EBS encryption).  

#### **3. Multi-Tenant Clusters**  
- **Isolation Strategies**:  
  - **Namespaces + RBAC**: Limit team access to their namespace.  
  - **Network Policies**: Block cross-tenant communication.  
  - **Resource Quotas**: Prevent resource hogging.  

#### **4. Audit Logging**  
- **What**: Track API Server requests (who did what, when).  
- **Why**: Detect unauthorized access, meet compliance (e.g., HIPAA).  
- **Configuration**:  
  ```yaml
  apiVersion: audit.k8s.io/v1
  kind: Policy
  rules:
  - level: Metadata  # Log all requests
  ```

---

### **III. Implementation & Labs**  
#### **1. Network Policies (Calico)**  
**Lab**: Allow frontend pods to talk only to the backend on port 8080.  
```yaml
# frontend-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: frontend-allow-backend
spec:
  podSelector:
    matchLabels:
      app: frontend
  policyTypes:
  - Egress
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: backend
    ports:
    - protocol: TCP
      port: 8080
```

#### **2. Pod Security Context**  
**Lab**: Run NGINX as non-root with read-only filesystem.  
```yaml
# secure-nginx.yaml
apiVersion: v1
kind: Pod
metadata:
  name: secure-nginx
spec:
  securityContext:
    runAsUser: 1000
    runAsNonRoot: true
  containers:
  - name: nginx
    image: nginx:alpine
    securityContext:
      readOnlyRootFilesystem: true
      capabilities:
        drop: ["ALL"]
```

#### **3. OPA/Gatekeeper Policies**  
**Lab**: Block privileged containers.  
1. **Install Gatekeeper**:  
   ```bash
   helm install gatekeeper gatekeeper/gatekeeper
   ```
2. **Create Constraint Template**:  
   ```yaml
   # block-privileged.yaml
   apiVersion: templates.gatekeeper.sh/v1
   kind: ConstraintTemplate
   metadata:
     name: k8sblockprivileged
   spec:
     crd:
       spec:
         names:
           kind: K8sBlockPrivileged
     targets:
     - target: admission.k8s.gatekeeper.sh
       rego: |
         package k8sblockprivileged
         violation[{"msg": msg}] {
           containers = input.review.object.spec.template.spec.containers
           c_name := containers[_].name
           containers[_].securityContext.privileged == true
           msg := sprintf("Privileged container %v is not allowed", [c_name])
         }
   ```
3. **Apply Constraint**:  
   ```yaml
   apiVersion: constraints.gatekeeper.sh/v1beta1
   kind: K8sBlockPrivileged
   metadata:
     name: block-privileged
   ```

---

### **IV. Use Cases**  
#### **1. PCI-DSS Compliance**  
- **Requirements**:  
  - Encrypt cardholder data (TLS, encrypted PVs).  
  - Restrict access to payment processing pods.  
- **Implementation**:  
  - Network Policies: Isolate payment pods.  
  - RBAC: Only finance team can access payment namespace.  

#### **2. Auditing with CIS Benchmarks**  
**Lab**: Check cluster against CIS Kubernetes Benchmark.  
```bash
# Run kube-bench (CIS compliance scanner)
docker run --rm --pid=host -v /etc:/etc:ro -v /var:/var:ro aquasec/kube-bench:latest
```

#### **3. Runtime Security with Falco**  
**Lab**: Detect suspicious container activity (e.g., shell in a pod).  
1. **Install Falco**:  
   ```bash
   helm install falco falcosecurity/falco
   ```
2. **Trigger Alert**:  
   ```bash
   kubectl exec -it <pod> -- /bin/sh  # Falco logs "Shell spawned in container"
   ```

---

### **V. Tools & Resources**  
- **Secrets Management**:  
  - **Vault Helm Chart**: `helm install vault hashicorp/vault`  
  - **Sealed Secrets**: Encrypt secrets for GitOps workflows.  
- **Policy Enforcement**:  
  - **Kyverno**: Kubernetes-native policy engine.  
  - **kubescape**: E2E security scanner.  
- **Monitoring**:  
  - **Prometheus + Alertmanager**: Track security metrics (e.g., failed login attempts).  

---

### **VI. Best Practices Checklist**  
1. **Cluster Hardening**:  
   - Disable anonymous access to API Server (`--anonymous-auth=false`).  
   - Rotate certificates (e.g., `kubeadm certs renew`).  
2. **Pod Security**:  
   - Use PSA in enforce mode (e.g., `restricted` profile).  
   - Scan images for vulnerabilities (Trivy/Clair).  
3. **RBAC**:  
   - Avoid wildcard permissions (`verbs: ["*"]`).  
   - Use `Role` over `ClusterRole` where possible.  
4. **Network Security**:  
   - Apply default-deny policies to all namespaces.  
   - Encrypt etcd with KMS (e.g., AWS KMS, GCP CloudHSM).  

---

### **VII. Summary**  
- **Zero-Trust**: Default-deny networking, least-privilege RBAC.  
- **Defense-in-Depth**: Layer security controls (network policies, PSA, encryption).  
- **Compliance**: Audit logs, CIS benchmarks, runtime monitoring.  

---

### **Next Steps**:  
1. **Lab 1**: Deploy a default-deny network policy and allow only necessary traffic.  
2. **Lab 2**: Enforce OPA policies to block containers with `privileged: true`.  
3. **Lab 3**: Configure Vault to inject secrets into a pod dynamically.
Let's break down Kubernetes networking concepts using your `kubectl get svc` output as a reference. We'll focus on **Service types**, **ClusterIP**, **NodePort**, and how traffic flows in a cluster.

---

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