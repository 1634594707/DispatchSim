# DispatchSim

园区无人车调度仿真平台，包含 Vue 3 前端和 Spring Boot 后端，支持订单管理、车辆调度、仿真控制、统计看板、Outbox 事件流和 WebSocket 实时推送。

## 项目结构

```text
DispatchSim/
├─ frontend/                 Vue 3 + Vite 前端
├─ dispatch-sim-backend/     Spring Boot 后端
├─ docker-compose.yml        本地容器联调
├─ k8s/                      Kubernetes 部署清单
├─ helm/                     Helm Chart
├─ .github/workflows/        GitHub Actions 流水线
└─ Jenkinsfile               Jenkins 流水线
```

## 技术栈

- 前端：Vue 3、TypeScript、Pinia、Vite、STOMP
- 后端：Spring Boot、JPA、Flyway、Redis、RabbitMQ、WebSocket
- 部署：Docker、Kubernetes、Nginx Ingress

## 本地开发

### 前端

```powershell
cd .\frontend
npm ci
npm run dev
```

开发端口：`http://localhost:5173`

前端开发服务器已配置：

- `/api` 代理到 `http://localhost:8080`
- `/ws` 代理到 `ws://localhost:8080`

### 后端

```powershell
cd .\dispatch-sim-backend
.\mvnw.cmd spring-boot:run
```

测试：

```powershell
cd .\dispatch-sim-backend
.\mvnw.cmd test
```

SonarQube 扫描：

```powershell
cd .\dispatch-sim-backend
.\mvnw.cmd -B test sonar:sonar `
  -Dsonar.host.url=http://your-sonarqube-host:9000 `
  -Dsonar.token=your-sonar-token
```

说明：

- `mvnw.cmd` / `mvnw` 优先使用仓库内 `.tools/apache-maven-3.9.11`
- Maven 本地仓库存放在 `dispatch-sim-backend/.m2/repository`
- 测试环境使用 `H2`，不依赖本机 MySQL
- OpenAPI 文档：`http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`
- SonarQube 项目配置位于 [dispatch-sim-backend/sonar-project.properties](D:\Administrator\Desktop\Project\DispatchSim\dispatch-sim-backend\sonar-project.properties)

## Docker

### 构建前端镜像

```powershell
docker build -t your-registry/dispatchsim-frontend:latest .\frontend
```

### 构建后端镜像

```powershell
docker build -t your-registry/dispatchsim-backend:latest .\dispatch-sim-backend
```

### 本地容器联调

```powershell
docker compose up --build
```

后台启动：

```powershell
docker compose up -d --build
```

停止：

```powershell
docker compose down
```

## Kubernetes 部署

K8s 清单位于 [k8s](D:\Administrator\Desktop\Project\DispatchSim\k8s)，按“应用部署”和“中间件独立提供”拆分。

已包含：

- Namespace
- Backend ConfigMap / Secret
- Backend Deployment / Service
- Frontend Deployment / Service
- Nginx Ingress
- `kustomization.yaml`

### 1. 构建并推送镜像

先把两个镜像推到你的镜像仓库：

```powershell
docker build -t your-registry/dispatchsim-frontend:latest .\frontend
docker push your-registry/dispatchsim-frontend:latest

docker build -t your-registry/dispatchsim-backend:latest .\dispatch-sim-backend
docker push your-registry/dispatchsim-backend:latest
```

然后把下面两个文件里的镜像地址改成你的实际仓库：

- [frontend-deployment.yaml](D:\Administrator\Desktop\Project\DispatchSim\k8s\frontend-deployment.yaml)
- [backend-deployment.yaml](D:\Administrator\Desktop\Project\DispatchSim\k8s\backend-deployment.yaml)

### 2. 修改集群连接配置

按你的集群环境修改：

- [backend-configmap.yaml](D:\Administrator\Desktop\Project\DispatchSim\k8s\backend-configmap.yaml)
  - MySQL 地址
  - Redis 地址
  - RabbitMQ 地址
  - 用户名
- [backend-secret.yaml](D:\Administrator\Desktop\Project\DispatchSim\k8s\backend-secret.yaml)
  - 数据库密码
  - RabbitMQ 密码
- [ingress.yaml](D:\Administrator\Desktop\Project\DispatchSim\k8s\ingress.yaml)
  - 域名 `dispatchsim.example.com`

注意：

- 当前清单假设 MySQL、Redis、RabbitMQ 已经由集群内独立服务或外部托管服务提供
- 不建议把这三个中间件和业务应用塞进同一套简单 Deployment 里

### 3. 应用部署

```powershell
kubectl apply -k .\k8s
```

查看状态：

```powershell
kubectl get all -n dispatchsim
kubectl get ingress -n dispatchsim
```

查看后端日志：

```powershell
kubectl logs -n dispatchsim deploy/dispatchsim-backend -f
```

查看前端日志：

```powershell
kubectl logs -n dispatchsim deploy/dispatchsim-frontend -f
```

### 4. Ingress 路由设计

推荐同域部署：

- `/` -> `dispatchsim-frontend`
- `/api` -> `dispatchsim-backend`
- `/ws` -> `dispatchsim-backend`

这样前端不需要写死后端域名，当前代码也已经兼容这种部署方式。

## K8s Overlays

已新增：

- [k8s/overlays/dev](D:\Administrator\Desktop\Project\DispatchSim\k8s\overlays\dev)
- [k8s/overlays/prod](D:\Administrator\Desktop\Project\DispatchSim\k8s\overlays\prod)

用途：

- `dev`：开发环境，较小资源、`dispatchsim-dev.example.com`
- `prod`：生产环境，多副本、TLS、`dispatchsim.example.com`

使用方式：

```powershell
kubectl apply -k .\k8s\overlays\dev
kubectl apply -k .\k8s\overlays\prod
```

部署前需要先替换：

- overlays 里的镜像地址和 tag
- overlays 里的数据库、Redis、RabbitMQ 地址
- overlays 里的 Secret 占位值

验证渲染：

```powershell
kubectl kustomize .\k8s\overlays\dev
kubectl kustomize .\k8s\overlays\prod
```

## Helm Chart

Helm Chart 位于 [helm/dispatchsim](D:\Administrator\Desktop\Project\DispatchSim\helm\dispatchsim)。

核心文件：

- [Chart.yaml](D:\Administrator\Desktop\Project\DispatchSim\helm\dispatchsim\Chart.yaml)
- [values.yaml](D:\Administrator\Desktop\Project\DispatchSim\helm\dispatchsim\values.yaml)

安装：

```powershell
helm upgrade --install dispatchsim .\helm\dispatchsim -n dispatchsim --create-namespace
```

覆盖镜像和域名示例：

```powershell
helm upgrade --install dispatchsim .\helm\dispatchsim `
  -n dispatchsim --create-namespace `
  --set frontend.image.repository=your-registry/dispatchsim-frontend `
  --set frontend.image.tag=1.0.0 `
  --set backend.image.repository=your-registry/dispatchsim-backend `
  --set backend.image.tag=1.0.0 `
  --set ingress.host=dispatchsim.example.com
```

模板渲染：

```powershell
helm template dispatchsim .\helm\dispatchsim
```

## CI/CD

已新增两套流水线模板：

- GitHub Actions: [.github/workflows/docker-k8s.yml](D:\Administrator\Desktop\Project\DispatchSim\.github\workflows\docker-k8s.yml)
- Jenkins: [Jenkinsfile](D:\Administrator\Desktop\Project\DispatchSim\Jenkinsfile)

### GitHub Actions

流程：

- 推送到 `main/master` 时构建并推送前后端镜像
- 推送 tag `v*` 时构建镜像并走生产部署
- `main/master` 默认走 `dev` overlay

需要配置的 Secret：

- `KUBE_CONFIG_DEV`
- `KUBE_CONFIG_PROD`
- `SONAR_HOST_URL`
- `SONAR_TOKEN`

镜像仓库默认使用 `ghcr.io`，如果你不是用 GHCR，需要改 [docker-k8s.yml](D:\Administrator\Desktop\Project\DispatchSim\.github\workflows\docker-k8s.yml) 的 `REGISTRY` 和镜像命名。

### Jenkins

`Jenkinsfile` 默认包含：

- 后端测试与打包
- 前端构建
- Docker 构建与推送
- 按参数选择 `dev/prod` 环境部署

你需要在 Jenkins 里准备：

- Docker 登录能力
- `kubectl`
- 可用的 `KUBECONFIG`
- 镜像仓库权限
- `sonar-host-url` 凭据
- `sonar-token` 凭据

说明：

- `RUN_SONAR=true` 时会在后端构建后执行 SonarQube 扫描
- 如果 Jenkins 环境暂时没有 SonarQube，可把 `RUN_SONAR` 设为 `false`

## Spring Boot 运行报错说明

你贴的错误：

```text
[ERROR] Failed to execute goal org.springframework.boot:spring-boot-maven-plugin:3.5.4:run
```

这只是 Maven 外层错误，不是真正的根因。要继续定位，需要至少拿到下面任意一种输出：

```powershell
cd .\dispatch-sim-backend
.\mvnw.cmd spring-boot:run -e
.\mvnw.cmd spring-boot:run -X
```

或者直接贴应用启动日志里 `Caused by:` 后面的异常。

## K8s 清单说明

- [namespace.yaml](D:\Administrator\Desktop\Project\DispatchSim\k8s\namespace.yaml)
- [kustomization.yaml](D:\Administrator\Desktop\Project\DispatchSim\k8s\kustomization.yaml)
- [backend-configmap.yaml](D:\Administrator\Desktop\Project\DispatchSim\k8s\backend-configmap.yaml)
- [backend-secret.yaml](D:\Administrator\Desktop\Project\DispatchSim\k8s\backend-secret.yaml)
- [backend-deployment.yaml](D:\Administrator\Desktop\Project\DispatchSim\k8s\backend-deployment.yaml)
- [backend-service.yaml](D:\Administrator\Desktop\Project\DispatchSim\k8s\backend-service.yaml)
- [frontend-deployment.yaml](D:\Administrator\Desktop\Project\DispatchSim\k8s\frontend-deployment.yaml)
- [frontend-service.yaml](D:\Administrator\Desktop\Project\DispatchSim\k8s\frontend-service.yaml)
- [ingress.yaml](D:\Administrator\Desktop\Project\DispatchSim\k8s\ingress.yaml)
- [k8s/overlays/dev/kustomization.yaml](D:\Administrator\Desktop\Project\DispatchSim\k8s\overlays\dev\kustomization.yaml)
- [k8s/overlays/prod/kustomization.yaml](D:\Administrator\Desktop\Project\DispatchSim\k8s\overlays\prod\kustomization.yaml)
- [helm/dispatchsim/Chart.yaml](D:\Administrator\Desktop\Project\DispatchSim\helm\dispatchsim\Chart.yaml)
- [.github/workflows/docker-k8s.yml](D:\Administrator\Desktop\Project\DispatchSim\.github\workflows\docker-k8s.yml)
- [Jenkinsfile](D:\Administrator\Desktop\Project\DispatchSim\Jenkinsfile)

## 已验证

- 后端测试通过：`.\dispatch-sim-backend\mvnw.cmd test -q`
- 前端构建通过：`cd .\frontend && npm run build`
- `docker compose config` 通过
- `kubectl kustomize .\k8s` 通过

## 当前限制

- K8s 清单里的中间件地址仍是模板值，需要按你的集群实际环境修改
- `Secret` 当前是占位值，部署前必须替换
- 后端健康检查暂时使用 `tcpSocket`，如果后续加了 `actuator/health`，建议改成 HTTP 探针
- GitHub Actions 和 Jenkins 里的仓库地址、凭据 ID、KubeConfig 仍需按你的环境改

## 下一步建议

- 增加 Helm Chart 或 K8s `overlays`
- 补 CI/CD：镜像构建、推送、自动部署
- 补第 15 阶段前后端联调和端到端验证
- 如果要定位 `spring-boot:run` 失败，需要补充完整异常栈
