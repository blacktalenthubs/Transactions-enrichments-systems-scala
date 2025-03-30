### **High-Performance Computing (HPC) Fundamentals**  
**Objective**: Bridge HPC and Kubernetes to run compute-intensive workloads (AI/ML, simulations) efficiently on modern infrastructure.  

---

### **I. Core Concepts & Definitions**  
#### **1. HPC Cluster Architecture**  
- **Traditional HPC Design**:  
  - **Head Node**: Submits jobs, manages scheduling.  
  - **Compute Nodes**: Execute parallel tasks (CPU/GPU).  
  - **High-Speed Interconnects**: InfiniBand, RoCE (RDMA over Converged Ethernet) for low-latency communication.  
  - **Shared Storage**: Lustre, NFS for parallel file access.  
- **Kubernetes vs. HPC Schedulers**:  
  | **Aspect**          | **Slurm/PBS**                     | **Kubernetes**                          |  
  |----------------------|-----------------------------------|-----------------------------------------|  
  | **Workload Type**    | Batch jobs, MPI tasks             | Microservices, long-running services    |  
  | **Resource Mgmt**    | Static allocation                 | Dynamic scaling                         |  
  | **Networking**       | InfiniBand (MPI-aware)            | CNI plugins (Calico, Cilium)            |  

#### **2. Performance Tuning Essentials**  
- **GPU Optimization**:  
  - **CUDA**: Parallel computing framework for NVIDIA GPUs.  
  - **cuDNN/cuBLAS**: Accelerated libraries for deep learning/math.  
  - **MIG (Multi-Instance GPU)**: Partition GPUs for smaller workloads.  
- **CPU/Memory Affinity**:  
  - **NUMA (Non-Uniform Memory Access)**: Bind tasks to CPU/memory nodes to reduce latency.  
  - **CPU Pinning**: Assign processes to specific cores.  
- **Parallel Computing Models**:  
  - **MPI (Message Passing Interface)**: Distributed memory parallelism (e.g., multi-node simulations).  
  - **OpenMP**: Shared memory parallelism (e.g., multi-threaded CPU tasks).  

#### **3. HPC on Kubernetes**  
- **Why Kubernetes for HPC?**:  
  - Unified orchestration for hybrid workloads (HPC + microservices).  
  - Elastic scaling of GPU/CPU resources.  
- **Key Tools**:  
  - **NVIDIA GPU Operator**: Automates GPU driver/device plugin deployment.  
  - **Kubeflow**: ML workflows on Kubernetes.  
  - **MPI Operator**: Manages MPI jobs (e.g., Horovod).  

---

### **II. Design Considerations**  
#### **1. Network Optimization**  
- **Low-Latency Networking**:  
  - **SR-IOV**: Bypass the kernel for direct NIC access (e.g., Mellanox ConnectX).  
  - **Userspace Networking**: Use DPDK or FD.io for high packet throughput.  
- **InfiniBand on Kubernetes**:  
  - **Network Operator**: Deploy RDMA CNI plugins.  
  - **Pods with RDMA**: Mount `/dev/infiniband` to containers.  

#### **2. Storage for HPC Workloads**  
- **Lustre on Kubernetes**:  
  - **Rook**: Deploy Lustre as a Kubernetes storage class.  
  - **Access Mode**: `ReadWriteMany` for parallel access.  
- **Ephemeral Storage**: Use local SSDs for scratch space.  

#### **3. Multi-Node Scheduling**  
- **Bin Packing**: Schedule pods to minimize fragmentation.  
- **Topology Awareness**: Schedule interdependent pods close (e.g., same rack for low latency).  

---

### **III. Implementation & Labs**  
#### **Lab 1: Deploy MPI Job on Kubernetes**  
**Goal**: Run a multi-node MPI job (e.g., matrix multiplication) using the MPI Operator.  
1. **Install MPI Operator**:  
   ```bash
   kubectl apply -f https://raw.githubusercontent.com/kubeflow/mpi-operator/master/deploy/v2beta1/mpi-operator.yaml
   ```  
2. **Launch MPI Job**:  
   ```yaml
   # mpi-job.yaml
   apiVersion: kubeflow.org/v2beta1
   kind: MPIJob
   metadata:
     name: matrix-mult
   spec:
     slotsPerWorker: 4  # GPUs per worker
     runPolicy:
       cleanPodPolicy: Running
     mpiReplicaSpecs:
       Launcher:
         replicas: 1
         template:
           spec:
             containers:
             - name: launcher
               image: mpi-base:latest
               command: ["mpirun", "-np", "4", "python", "matrix_mult.py"]
       Worker:
         replicas: 2
         template:
           spec:
             containers:
             - name: worker
               image: mpi-base:latest
               resources:
                 limits:
                   nvidia.com/gpu: 2
   ```  
3. **Monitor**:  
   ```bash
   kubectl get mpijobs
   kubectl logs matrix-mult-launcher-0
   ```

#### **Lab 2: GPU Optimization with CUDA**  
**Goal**: Benchmark GPU performance with/without CUDA libraries.  
1. **Run CUDA Vector Add**:  
   ```bash
   # vector_add.cu
   __global__ void add(int *a, int *b, int *c) {
     int tid = blockIdx.x * blockDim.x + threadIdx.x;
     c[tid] = a[tid] + b[tid];
   }
   ```  
2. **Build & Run**:  
   ```bash
   nvcc vector_add.cu -o vector_add
   ./vector_add
   ```  
3. **Profile**:  
   ```bash
   nvprof ./vector_add
   ```

#### **Lab 3: NUMA-Aware Pod Scheduling**  
**Goal**: Bind a pod to specific NUMA nodes for memory-bound workloads.  
1. **Label Nodes with NUMA Info**:  
   ```bash
   kubectl label nodes node-1 numa=0
   kubectl label nodes node-2 numa=1
   ```  
2. **Pod Spec with Affinity**:  
   ```yaml
   affinity:
     nodeAffinity:
       requiredDuringSchedulingIgnoredDuringExecution:
         nodeSelectorTerms:
         - matchExpressions:
           - key: numa
             operator: In
             values: ["0"]
   ```  

---

### **IV. Use Cases**  
#### **1. Distributed Deep Learning**  
- **Tool**: Horovod + MPI.  
- **Workflow**:  
  1. Split training data across nodes.  
  2. Synchronize gradients via RDMA.  
  3. Use GPU Operator for NVIDIA driver management.  

#### **2. Computational Fluid Dynamics (CFD)**  
- **Tool**: OpenFOAM on Kubernetes.  
- **Setup**:  
  - **Storage**: Lustre for shared mesh data.  
  - **Networking**: InfiniBand for MPI communication.  

#### **3. Genomics (GATK)**  
- **Challenge**: Process large DNA datasets.  
- **Solution**:  
  - Use Kubernetes Jobs with GPU-accelerated GATK tools.  
  - Autoscale preemptible nodes for cost efficiency.  

---

### **V. Tools & Resources**  
- **NVIDIA GPU Operator**: Simplifies GPU setup in Kubernetes.  
- **KubeDirector**: Deploy complex HPC applications (e.g., Spark, Cassandra).  
- **Prometheus + DCGM**: Monitor GPU metrics (utilization, memory, temperature).  
- **SOSFlow**: Debugging tool for large-scale HPC jobs.  

---

### **VI. Best Practices**  
1. **Cluster Setup**:  
   - Use **Ubuntu/Debian** for HPC nodes (better low-latency kernel support).  
   - Tune kernel parameters:  
     ```bash
     sysctl -w net.core.rmem_max=16777216
     sysctl -w net.core.wmem_max=16777216
     ```  
2. **Job Scheduling**:  
   - Use **gang scheduling** (e.g., Volcano) to ensure all pods start simultaneously.  
3. **Monitoring**:  
   - Track `DCGM_FI_DEV_GPU_UTIL` (GPU usage) and `mpi_communication_time` (MPI efficiency).  

---

### **Practice**:  
1. **Lab 1**: Deploy a Lustre filesystem using Rook and run a parallel I/O test.  
2. **Lab 2**: Profile an MPI job with NVIDIA Nsight Systems.  
3. **Lab 3**: Optimize a CUDA kernel using shared memory.  


---

### **Step 1: Prerequisites**  
1. **AWS Account**: Ensure you have access to an AWS account with permissions for EC2, VPC, IAM, and S3.  
2. **AWS CLI**: Install and configure the AWS CLI with your credentials.  
   ```bash
   aws configure
   ```  
3. **AWS ParallelCluster**: Install the CLI tool:  
   ```bash
   pip install aws-parallelcluster
   ```  

---

### **Step 2: Configure Networking**  
#### **1. Create a VPC with Subnets**  
Use the AWS Management Console or CLI to create:  
- A **VPC** (e.g., `10.0.0.0/16`).  
- **Public/Private Subnets** in multiple Availability Zones (AZs).  

#### **2. Enable EFA**  
- **EFA Requirements**:  
  - Use EFA-supported instances (e.g., `c5n.18xlarge`, `p4d.24xlarge`).  
  - Launch instances in a **placement group** (cluster mode).  

---

### **Step 3: Create an HPC Cluster with AWS ParallelCluster**  
#### **1. Generate a Config File**  
```bash
pcluster configure --config cluster-config.yaml
```  
Follow prompts to select:  
- **Instance Types**: `c5n.18xlarge` (EFA-enabled).  
- **Operating System**: `alinux2` (Amazon Linux 2).  
- **Scheduler**: `slurm` (or `awsbatch`).  
- **Shared Storage**: `fsx_lustre` or `efs`.  

#### **2. Customize `cluster-config.yaml`**  
```yaml
Region: us-east-1
Image:
  Os: alinux2
HeadNode:
  InstanceType: t2.micro
  Ssh:
    KeyName: my-keypair
Scheduling:
  Scheduler: slurm
  SlurmQueues:
    - Name: compute
      ComputeResources:
        - Name: c5n-18xlarge
          InstanceType: c5n.18xlarge
          MinCount: 0
          MaxCount: 10
          Efa:
            Enabled: true
      Networking:
        SubnetIds: [subnet-12345]
        PlacementGroup:
          Enabled: true
      ComputeSettings:
        LocalStorage:
          RootVolume:
            Size: 50
SharedStorage:
  - MountDir: /shared
    Name: my-lustre
    StorageType: FsxLustre
    FsxLustreSettings:
      StorageCapacity: 1200
      DeploymentType: SCRATCH_2
      ImportPath: s3://my-bucket/data  # Optional: Import data from S3
```

#### **3. Create the Cluster**  
```bash
pcluster create-cluster --cluster-name my-hpc-cluster --cluster-configuration cluster-config.yaml
```  
Wait 10-15 minutes for the cluster to deploy.  

---

### **Step 4: Connect to the Cluster**  
#### **1. SSH into the Head Node**  
```bash
pcluster ssh --cluster-name my-hpc-cluster -i ~/.ssh/my-keypair.pem
```  

#### **2. Verify EFA and Slurm**  
```bash
# Check EFA status
[ec2-user@ip-10-0-0-1 ~]$ fi_info -p efa

# Check Slurm nodes
[ec2-user@ip-10-0-0-1 ~]$ sinfo
PARTITION AVAIL  TIMELIMIT  NODES  STATE NODELIST
compute*     up   infinite      0    idle
```  

---

### **Step 5: Submit an HPC Job**  
#### **1. Write a Sample MPI Job**  
Create `mpi_hello.c`:  
```c
#include <mpi.h>
#include <stdio.h>

int main(int argc, char** argv) {
    MPI_Init(&argc, &argv);
    int world_size, world_rank;
    MPI_Comm_size(MPI_COMM_WORLD, &world_size);
    MPI_Comm_rank(MPI_COMM_WORLD, &world_rank);
    printf("Hello from rank %d of %d\n", world_rank, world_size);
    MPI_Finalize();
    return 0;
}
```  

#### **2. Compile and Run**  
```bash
# Load MPI module
module load openmpi

# Compile
mpicc mpi_hello.c -o mpi_hello

# Submit job to Slurm
sbatch --nodes=2 --tasks-per-node=36 <<EOF
#!/bin/bash
srun ./mpi_hello
EOF
```  

#### **3. Check Output**  
```bash
cat slurm-*.out
Hello from rank 0 of 72
Hello from rank 1 of 72
...
```  

---

### **Step 6: Integrate with FSx for Lustre (Optional)**  
#### **1. Access Shared Storage**  
All nodes mount `/shared` (FSx Lustre). Copy data from S3:  
```bash
aws s3 cp s3://my-bucket/data /shared/data --recursive
```  

#### **2. Run a Parallel I/O Job**  
Use MPI-IO or HDF5 to read/write data from `/shared`.  

---

### **Step 7: Cost Optimization**  
1. **Use Spot Instances**: Edit `cluster-config.yaml` to add Spot pricing:  
   ```yaml
   ComputeResources:
     - Name: c5n-18xlarge-spot
       InstanceType: c5n.18xlarge
       MinCount: 0
       MaxCount: 10
       Efa:
         Enabled: true
       CapacityType: SPOT
   ```  
2. **Terminate When Idle**:  
   ```bash
   pcluster delete-cluster --cluster-name my-hpc-cluster
   ```  

---

### **Step 8: Advanced Features**  
#### **1. Hybrid Jobs (CPU + GPU)**  
Add a GPU queue to `cluster-config.yaml`:  
```yaml
SlurmQueues:
  - Name: gpu
    ComputeResources:
      - Name: p4d-24xlarge
        InstanceType: p4d.24xlarge
        MinCount: 0
        MaxCount: 4
```  

#### **2. AWS Batch Integration**  
For serverless HPC jobs:  
```bash
aws batch create-compute-environment --name hpc-batch \
  --type MANAGED --service-role AWSBatchServiceRole \
  --compute-resources type=EC2,minvCpus=0,maxvCpus=256,instanceTypes=c5n.18xlarge
```  

---

### **Final Architecture**  
```
[User] → [Head Node (Slurm)] → [Compute Nodes (c5n.18xlarge + EFA)]  
                               ↑  
[FSx Lustre/S3] ← Shared Storage
```  

---

### **Troubleshooting**  
- **EFA Issues**: Check `dmesg | grep EFA` and ensure instances are in a cluster placement group.  
- **Slurm Failures**: Use `scontrol show nodes` to debug node readiness.  
- **FSx Latency**: Validate Lustre client version and network bandwidth.  

---

### **Practice**  
1. **Benchmark**: Compare MPI performance with/without EFA.  
2. **Scale Up**: Run a 1,000-core CFD simulation.  
3. **Integrate ML**: Use SageMaker for hyperparameter tuning with HPC results.  


### **1. SLURM on AWS: Deep Dive**

#### **What is SLURM?**
**SLURM (Simple Linux Utility for Resource Management)** is an open-source job scheduler and resource manager for high-performance computing (HPC) clusters. It is widely used to manage workloads across thousands of nodes in supercomputers and cloud-based clusters. Key features include:
- **Job Scheduling**: Allocate resources (CPUs, GPUs, memory) to jobs based on priority, fairness, or quotas.
- **Resource Management**: Track node availability, enforce resource limits, and handle job queues.
- **Workload Orchestration**: Coordinate parallel or distributed jobs (e.g., MPI, multi-node machine learning).

**Why Use SLURM on AWS?**
- **Cluster Efficiency**: Automate resource allocation to avoid manual job scheduling.
- **Multi-User Support**: Prioritize jobs and prevent resource conflicts.
- **Integration with AWS HPC Tools**: AWS ParallelCluster uses SLURM as its default scheduler, simplifying cluster management.
- **Cost Control**: Terminate idle nodes to save costs (auto-scaling).

---

#### **How to Use SLURM on AWS (via AWS ParallelCluster)**

##### **Step 1: Configure SLURM in AWS ParallelCluster**
Define partitions (queues), compute nodes, and GPU/EFA settings in the `cluster-config.yaml`:
```yaml
Scheduling:
  Scheduler: slurm
  SlurmQueues:
    - Name: gpu_queue
      ComputeResources:
        - Name: p4d_24xlarge
          InstanceType: p4d.24xlarge  # NVIDIA A100 GPUs
          MinCount: 0
          MaxCount: 10
          Efa:
            Enabled: true  # For low-latency networking
          CustomSlurmSettings:
            Gres: gpu:8  # 8 GPUs per node
```

##### **Step 2: Submit Jobs to SLURM**
- **Basic Job Submission**:
  ```bash
  # Submit a job requesting 2 nodes, 4 GPUs each
  sbatch --job-name=my_gpu_job \
         --nodes=2 \
         --ntasks-per-node=4 \
         --gres=gpu:4 \
         --wrap="python train.py"
  ```
- **Job Script Example** (`train_job.sh`):
  ```bash
  #!/bin/bash
  #SBATCH --job-name=mnist
  #SBATCH --nodes=2
  #SBATCH --ntasks-per-node=8
  #SBATCH --gres=gpu:8
  #SBATCH --output=%x-%j.out

  module load cuda/11.7
  srun python train.py --batch-size=1024
  ```
  Submit with:
  ```bash
  sbatch train_job.sh
  ```

##### **Step 3: Monitor and Manage Jobs**
- **Check Job Status**:
  ```bash
  squeue -u $USER  # List your jobs
  sinfo -p gpu_queue  # Check node availability
  ```
- **Cancel a Job**:
  ```bash
  scancel <job_id>
  ```
- **View Job Output**:
  ```bash
  cat slurm-<job_id>.out
  ```

##### **Step 4: SLURM Accounting**
Track GPU/CPU usage for cost analysis:
```bash
sacct -j <job_id> --format=JobID,JobName,Elapsed,AllocCPUs,AllocGRES
```
Output:
```
JobID           JobName      Elapsed   AllocCPUS   AllocGRES
12345           mnist        00:30:00  64          gpu:32
```

---

### **2. Comparing GPU Training to Other Methods**

#### **Key Metrics for Comparison**
| **Metric**               | **GPU Training**                     | **CPU Training**                     | **TPU Training**                     |
|--------------------------|--------------------------------------|--------------------------------------|--------------------------------------|
| **Speed**                | ~10-100x faster (parallel compute)  | Slow (sequential processing)         | ~2-5x faster than GPU for TPU-optimized tasks |
| **Cost**                 | Higher upfront cost, but faster     | Lower cost for small datasets        | High cost, specialized use cases    |
| **Scalability**          | Scales well with data parallelism   | Limited by CPU cores                 | Optimized for TensorFlow/PyTorch-XLA |
| **Use Cases**            | Deep learning, simulations          | Small models, non-parallel tasks     | Large-scale TPU-optimized models    |

#### **How to Collect Metrics**
1. **Training Time**:
   - Use `time` command:
     ```bash
     time python train.py  # Real: 2h30m, User: 10h (multi-core)
     ```
   - SLURM’s `sacct` for job duration:
     ```bash
     sacct -j <job_id> --format=Elapsed
     ```

2. **GPU Utilization**:
   - **DCGM (NVIDIA Data Center GPU Manager)**:
     ```bash
     dcgmi dmon -e 203,204  # GPU utilization (%) and memory usage (MB)
     ```
   - **`nvidia-smi`**:
     ```bash
     nvidia-smi --query-gpu=utilization.gpu,memory.used --format=csv
     ```

3. **CPU/GPU Power Draw**:
   - AWS CloudWatch Metrics:
     - `GPUUtilization`, `GPUMemoryUtilization` for EC2 GPU instances.
     - `CPUUtilization` for CPU instances.

4. **Cost Efficiency**:
   - **AWS Cost Explorer**:
     - Compare cost per epoch/hour for GPU vs. CPU instances.
   - **EC2 Spot Pricing**:
     ```bash
     aws ec2 describe-spot-price-history --instance-types p4d.24xlarge --product-descriptions "Linux/UNIX"
     ```

5. **Distributed Training Metrics**:
   - **Horovod Timeline**:
     ```python
     import horovod.tensorflow as hvd
     hvd.init()
     # Enable profiling
     timeline = hvd.Timeline()
     options = tf.data.Options()
     options.experimental_threading.private_threadpool_size = 1
     ```
   - **PyTorch Profiler**:
     ```python
     with torch.profiler.profile(activities=[torch.profiler.ProfilerActivity.CUDA]) as prof:
         model.train()
     print(prof.key_averages().table(sort_by="cuda_time_total"))
     ```

---

### **3. Best Practices for Metric Collection**
- **Automate Logging**:
  ```python
  # PyTorch Lightning example
  trainer = Trainer(
      logger=CSVLogger(save_dir="logs/"),
      profiler="advanced"
  )
  ```
- **Centralized Monitoring**:
  - **Prometheus + Grafana**: Scrape metrics from DCGM, SLURM, and application logs.
  - **AWS CloudWatch**: Monitor EC2/EFA metrics and SageMaker training jobs.
- **Benchmarking**:
  - Use synthetic datasets to compare GPU/CPU performance under identical conditions.
  - Example benchmark script:
    ```bash
    #!/bin/bash
    for backend in gpu cpu; do
      echo "Testing $backend..."
      time python train.py --device $backend --epochs 10
    done
    ```

---

### **Summary**
- **SLURM** on AWS ParallelCluster enables efficient resource management for HPC/ML workloads. Use `sbatch`, `squeue`, and `sacct` to submit, monitor, and analyze jobs.
- **GPU Training** excels in parallel tasks but requires cost-benefit analysis. Compare metrics like time-to-accuracy, throughput, and cost-per-epoch.
- **Metric Collection** relies on tools like DCGM, CloudWatch, and application profiling to optimize performance and costs.