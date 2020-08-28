# Verify the OOMKill locally via Docker Compose

First, build the top-level project, then apply the K8s resources to start a K8s
session cluster and submit the job:

```
make image
make run

make submit

# after your test:
make stop
```

So far, I could not reproduce the problem locally, but it may actually take
longer or even depend on the local docker setup.
