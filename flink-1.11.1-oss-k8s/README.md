# Verify the OOMKill in Kubernetes

First, build the top-level project, then apply the K8s resources to start a K8s
session cluster and submit the job:

```
kubectl apply -f *.yaml

kubectl port-forward deployment/flink-jobmanager 8081:8081

flink run -m localhost:8081 -d -p 2 -c com.ververica.troubleshooting.RocksDBMemLeakNthLast ../build/libs/lab-rocksdb-memleak-0.1-SNAPSHOT-all.jar
```

Please note that the given setup will use an `emptyDir` volume for checkpoints
which will actually provide no fault tolerance guarantees and will fail to
restore the job if the pod dies! We want to find the reason for the OOMKill
though and the first instance of it is enough, so we don't need to retry the
job after that.
