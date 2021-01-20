# OMAR (Lite) WMS

## Feature kubernetes documentation

- [Micronaut Kubernetes Support documentation](https://micronaut-projects.github.io/micronaut-kubernetes/latest/guide/index.html)

- [https://kubernetes.io/docs/home/](https://kubernetes.io/docs/home/)

## Feature management documentation

- [Micronaut Micronaut Management documentation](https://docs.micronaut.io/latest/guide/index.html#management)

## Feature http-client documentation

- [Micronaut Micronaut HTTP Client documentation](https://docs.micronaut.io/latest/guide/index.html#httpClient)

## Special Branches

Per the `Jenkinsfile`, certain branches are treated as special, and commits to these branches have implications listed 
below:

### Master

Each commit to master will result in a newly published docker image of the version specified in `Chart.yaml`:`appVersion`. 
Docker images cannot be overriden in our docker registry (that is, nexus), so each commit will need to be accompanied 
with a bumped version, or else a docker image will not be pushed.
