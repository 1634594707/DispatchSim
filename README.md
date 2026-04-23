# DispatchSim

园区无人车调度仿真平台，包含 `Vue 3 + Vite` 前端和 `Spring Boot` 后端。当前版本已经完成地图交互增强、园区路网编辑、固定配送中心/仓库建模、车辆摆放、订单生命周期扩展、仿真控制和性能观测。

## 主要能力

- 大地图缩放、平移、右键创建订单、最小地图联动
- 园区级路网编辑：节点、边、框选、移动、删除、撤销重做、GeoJSON 导入导出
- 出货点/配送中心管理：创建、编辑、删除、CSV 导入导出
- 车辆增强：电量、装卸时间、多订单队列、手动指派、地图吸附摆放
- 订单生命周期：取消、归档、恢复、批量创建
- 仿真控制：速度调节、单步执行、暂停编辑、重置
- 性能观测：FPS、渲染耗时、LOD、视口剔除率、前端 10 秒基准采样

## 仓库结构

```text
DispatchSim/
├─ frontend/                 Vue 3 + Vite 前端
├─ dispatch-sim-backend/     Spring Boot 后端
├─ docker-compose.yml        本地容器联调
├─ helm/                     Helm Chart
├─ k8s/                      Kubernetes 清单
├─ .github/workflows/        GitHub Actions
└─ Jenkinsfile               Jenkins 流水线
```

## 技术栈

- 前端：Vue 3、TypeScript、Pinia、Vite、ECharts、STOMP
- 后端：Spring Boot 3、Spring Data JPA、Flyway、WebSocket
- 数据库：MySQL（运行态），H2（测试态）
- 部署：Docker Compose、Kubernetes、Helm

## 快速开始

### 1. 启动后端

```powershell
cd .\dispatch-sim-backend
.\mvnw.cmd spring-boot:run
```

默认地址：

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

说明：

- `mvnw.cmd` 优先使用仓库内 `.tools/apache-maven-3.9.11`
- 后端测试默认使用 `H2`，不依赖本机 MySQL
- 本地运行配置可参考 [dispatch-sim-backend/README.md](/D:/Administrator/Desktop/Project/DispatchSim/dispatch-sim-backend/README.md)

### 2. 启动前端

```powershell
cd .\frontend
npm ci
npm run dev
```

默认地址：

- 前端：`http://localhost:5173`

开发代理已配置：

- `/api` -> `http://localhost:8080`
- `/ws` -> `ws://localhost:8080`

更多前端说明见 [frontend/README.md](/D:/Administrator/Desktop/Project/DispatchSim/frontend/README.md)。

## 当前地图与性能增强

本轮增强已经把默认示例改成更接近真实园区的布局，并补齐了前后端联动：

- 默认园区路网、配送中心、仓库、远端配送区已重构
- 默认车辆会出生在真实 depot / road node 上，而不是任意坐标
- 新增 `Vehicle Placement` 模式，可在地图上新增、拖动、删除无人车，并吸附到路网节点
- 地图支持更复杂的主干道 + 多片区路网，不再局限于小范围示意图

性能验证入口位于 Dashboard 左上角 `性能监控` 面板：

- `加载压力场景`：载入 `100` 辆车、`500` 个订单、`1000` 个路网节点
- `运行 10 秒基准采样`：记录 FPS、渲染耗时、剔除率、LOD
- `复制结果`：复制最近一次前端基准摘要

## 已完成验证

### 前端

- `cd .\frontend && npm run lint`
- `cd .\frontend && npm run build`

### 后端

- `cd .\dispatch-sim-backend && .\mvnw.cmd test`

### 性能验证

后端大数据量验证已完成，覆盖：

- `100` vehicles
- `500` orders
- `1000` road nodes

前端浏览器侧基准结果之一：

```text
DispatchSim 前端基准结果 (2026/4/23 20:22:52)
平均 FPS: 60.0
最低 FPS: 60
平均渲染: 117.69ms
峰值渲染: 120.13ms
平均剔除率: 67.9%
主 LOD: L2
最大已渲染实体: 1235
样本数: 11
```

这组结果已用于完成地图增强任务里的性能测试检查项。

## 常用命令

### 前端

```powershell
cd .\frontend
npm run dev
npm run lint
npm run build
```

### 后端

```powershell
cd .\dispatch-sim-backend
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```

## Docker 与部署

### Docker Compose

```powershell
docker compose up --build
docker compose down
```

### Kubernetes

```powershell
kubectl apply -k .\k8s
```

Overlay:

```powershell
kubectl apply -k .\k8s\overlays\dev
kubectl apply -k .\k8s\overlays\prod
```

### Helm

```powershell
helm upgrade --install dispatchsim .\helm\dispatchsim -n dispatchsim --create-namespace
```

## 说明

- 本仓库当前工作流和部署模板已经存在，但镜像仓库地址、域名、Secret 和中间件连接信息仍需按你的环境调整
- 运行态后端默认依赖 MySQL / Redis / RabbitMQ；测试态不依赖本机 MySQL
- 若要查看任务拆解与阶段记录，可参考 [.kiro/specs/map-and-order-enhancements/tasks.md](/D:/Administrator/Desktop/Project/DispatchSim/.kiro/specs/map-and-order-enhancements/tasks.md)
