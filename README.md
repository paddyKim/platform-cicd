# platform-cicd

Backend-only CI/CD execution service for the platform project.

## Scope

`platform-cicd` receives CI/CD execution requests from `platform-portal` and owns the actual execution work:

- image build requests
- image deployment requests
- later replica operations, rollback, and recovery actions
- later GitOps commit/push, image pushes, ArgoCD sync, and execution callbacks

The initial Day 18 implementation is a synchronous HTTP skeleton. Message queue based asynchronous dispatch is deferred.

Day 19 adds the first real execution path: `DEPLOY_IMAGE` updates the local GitOps values file for the dev environment.

Day 20 keeps `DEPLOY_IMAGE` as the priority and defers `CHANGE_REPLICAS` until the deployment flow is complete. Replica changes belong to later operations or recovery work.

## Local Run

```bash
./gradlew bootRun
```

Default port:

```text
8082
```

## APIs

```text
GET  /api/health
POST /api/cicd/executions
GET  /api/cicd/executions
GET  /api/cicd/executions/{id}
```

Create execution request:

```bash
curl -X POST http://localhost:8082/api/cicd/executions \
  -H 'Content-Type: application/json' \
  -d '{
    "portalRequestId": 1,
    "applicationName": "platform-app",
    "environment": "dev",
    "componentName": "platform-api",
    "requestType": "DEPLOY_IMAGE",
    "requestedValue": "1fd847c",
    "requestedBy": "platform-operator"
  }'
```

By default, successful `DEPLOY_IMAGE` requests update:

```text
../platform-deploy/environments/dev/values.yaml
```

Override the values file path:

```bash
PLATFORM_GITOPS_VALUES_PATH=/path/to/values.yaml ./gradlew bootRun
```

Initial status flow:

```text
platform-portal: DISPATCHED
platform-cicd:   REQUESTED -> RUNNING -> SUCCEEDED / FAILED
```

`QUEUED` will be added later when a message broker is introduced.

Current support matrix:

| Request Type | Environment | Component | Result |
|---|---|---|---|
| `DEPLOY_IMAGE` | `dev` | `platform-api` | updates `api.image.tag` |
| `DEPLOY_IMAGE` | `dev` | `platform-web` | updates `web.image.tag` |
| `DEPLOY_IMAGE` | `dev` | `platform-mariadb` | `FAILED` |
| `BUILD_IMAGE` | any | any | `FAILED` |
| `CHANGE_REPLICAS` | any | any | deferred; currently `FAILED` |

Next deployment steps:

```text
DEPLOY_IMAGE request
-> GitOps values image tag update
-> platform-deploy commit/push or approved deploy repo update
-> ArgoCD automated/manual sync
-> portal status tracking
```

ArgoCD sync mode, self-heal, prune, and recovery choices should be selected from `platform-portal` in a later milestone.

## Verification

```bash
./gradlew test --no-daemon
```
