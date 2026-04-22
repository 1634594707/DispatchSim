# 园区无人车调度仿真平台 MVP设计文档

**项目名称**：DispatchSim  
**版本**：v2.0  
**更新日期**：2026-04-22  

---

## 目录

1. [项目概述](#一项目概述)
2. [功能分级与优先级](#二功能分级与优先级)
3. [技术架构设计](#三技术架构设计)
4. [P0 核心流程详细设计](#四p0-核心流程详细设计)
5. [P1 增强功能详细设计](#五p1-增强功能详细设计)
6. [P2 可选功能设计](#六p2-可选功能设计)
7. [数据模型设计](#七数据模型设计)
8. [API 接口设计](#八api-接口设计)
9. [项目目录结构](#九项目目录结构)
10. [非功能性需求](#十非功能性需求)
11. [风险分析与应对](#十一风险分析与应对)
12. [开发计划与里程碑](#十二开发计划与里程碑)
13. [面试展示要点](#十三面试展示要点)

---

## 一、项目概述

### 1.1 项目背景
DispatchSim 是一个园区无人车调度仿真平台，模拟真实场景下的无人车调度系统。通过该平台可以验证调度算法的有效性，演示订单从创建到完成的全流程，并支持故障模拟与重调度等高级功能。

### 1.2 核心目标
- 实现订单全生命周期管理（创建→分配→执行→完成）
- 支持多种调度策略，可动态切换
- 提供可视化界面实时展示调度过程
- 支持故障模拟与自动重调度

### 1.3 技术栈
- **后端**：Spring Boot 3.x + Java 17
- **数据库**：MySQL 8.0 + Redis 7.x
- **消息队列**：RabbitMQ / Kafka
- **前端**：Vue 3 + TypeScript + ECharts
- **通信**：WebSocket (STOMP)

---

## 二、功能分级与优先级

### 2.1 功能优先级矩阵

| 优先级 | 功能模块 | 预估工时 | 面试加分指数 | 核心价值 |
|--------|----------|----------|--------------|----------|
| **P0（必做）** | 订单全流程（创建→分配→移动→完成） | 1天 | ★★★★★ | 基础盘，验证核心技术栈和业务理解 |
| **P1（强烈推荐）** | 多策略选车算法 | 1天 | ★★★★☆ | 体现设计思想和对业务场景的思考 |
| **P1（强烈推荐）** | 故障模拟与重调度 | 2天 | ★★★★★ | 精准命中岗位明确提到的核心技术难点 |
| **P1（强烈推荐）** | 网格地图可视化 | 1.5天 | ★★★★★ | 面试演示效果提升10倍，直观展示调度过程 |
| **P2（可选）** | 批量订单生成 | 0.5天 | ★★★☆☆ | 演示高并发场景下的系统表现 |
| **P2（可选）** | 性能统计与监控 | 1天 | ★★★☆☆ | 展示工程思维和性能优化意识 |
| **P2（可选）** | 历史回放功能 | 1天 | ★★★☆☆ | 便于问题排查和算法调优 |

### 2.2 功能依赖关系

```
P0 核心流程
    │
    ├──→ P1 多策略选车（依赖调度引擎）
    │
    ├──→ P1 故障重调度（依赖核心流程+事件机制）
    │
    ├──→ P1 网格地图可视化（依赖WebSocket+位置数据）
    │
    └──→ P2 批量订单（依赖订单创建接口）
         └──→ P2 性能统计（依赖订单完成数据）
              └──→ P2 历史回放（依赖全量事件日志）
```

---

## 三、技术架构设计

### 3.1 系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         前端层 (Vue 3)                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │   地图可视化  │  │  订单管理面板  │  │   车辆状态监控面板    │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
└──────────────────────────┬────────────────────────────────────┘
                           │ WebSocket / HTTP
┌──────────────────────────▼────────────────────────────────────┐
│                      网关层 (Gateway)                          │
└──────────────────────────┬────────────────────────────────────┘
                           │
┌──────────────────────────▼────────────────────────────────────┐
│                      应用服务层                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │   订单服务    │  │   调度服务    │  │      车辆服务         │  │
│  │  OrderService│  │DispatchEngine│  │   VehicleService     │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │   仿真服务    │  │   故障服务    │  │      统计服务         │  │
│  │SimulationSvc │  │ FaultService │  │   StatisticsService  │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
└──────────────────────────┬────────────────────────────────────┘
                           │
┌──────────────────────────▼────────────────────────────────────┐
│                      领域层 (Domain)                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │   订单聚合    │  │   车辆聚合    │  │      调度策略         │  │
│  │  OrderAgg    │  │ VehicleAgg   │  │   Strategy Pattern   │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
│  ┌──────────────┐  ┌──────────────┐                            │
│  │   事件总线    │  │   状态机      │                            │
│  │  EventBus    │  │ StateMachine │                            │
│  └──────────────┘  └──────────────┘                            │
└──────────────────────────┬────────────────────────────────────┘
                           │
┌──────────────────────────▼────────────────────────────────────┐
│                      基础设施层                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │    MySQL     │  │    Redis     │  │      RabbitMQ        │  │
│  │  (主数据库)   │  │   (缓存/锁)   │  │      (消息队列)       │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 核心设计模式

| 模式 | 应用场景 | 解决的问题 |
|------|----------|------------|
| **策略模式** | 多策略选车 | 算法可插拔，运行时动态切换 |
| **状态模式** | 订单/车辆状态流转 | 状态行为封装，避免大量if-else |
| **发布订阅模式** | 事件驱动架构 | 服务解耦，支持异步处理 |
| **Outbox模式** | 事务与消息一致性 | 保证数据变更与事件发送的原子性 |
| **工作单元模式** | 复杂业务事务 | 统一管理事务边界 |

### 3.3 事件驱动架构

```
┌──────────────────────────────────────────────────────────────┐
│                        事件总线                               │
├──────────────────────────────────────────────────────────────┤
│  领域事件              │  应用事件              │  系统事件    │
├───────────────────────┼───────────────────────┼─────────────┤
│  OrderCreated         │  VehicleAssigned      │  SystemInit  │
│  OrderAssigned        │  VehicleFaulted       │  ConfigUpdate│
│  OrderCompleted       │  OrderReassigned      │  Heartbeat   │
│  VehicleStatusChanged │  SimulationTick       │              │
└───────────────────────┴───────────────────────┴─────────────┘
```

---

## 四、P0 核心流程详细设计

### 4.1 订单状态机

```
                    ┌─────────────┐
         ┌─────────→│   已创建    │←────────┐
         │          │  CREATED    │         │
         │          └──────┬──────┘         │
         │                 │ 分配车辆        │ 创建订单
         │                 ▼                │
         │          ┌─────────────┐         │
         │    ┌─────│   待分配    │─────┐   │
         │    │     │  PENDING    │     │   │
         │    │     └──────┬──────┘     │   │
    取消订单  │ 无可用车辆   │ 分配成功   重试失败
         │    │            ▼            │   │
         │    │     ┌─────────────┐     │   │
         │    └────→│   已分配    │←────┘   │
         │          │  ASSIGNED   │         │
         │          └──────┬──────┘         │
         │                 │ 开始配送        │
         │                 ▼                │
         │          ┌─────────────┐         │
         │          │   配送中    │         │
         │          │  DELIVERING │         │
         │          └──────┬──────┘         │
         │                 │ 送达目的地      │
         │                 ▼                │
         │          ┌─────────────┐         │
         └─────────→│   已完成    │─────────┘
                    │  COMPLETED  │
                    └─────────────┘
                           │
                    ┌──────┴──────┐
                    ▼             ▼
              ┌─────────┐   ┌─────────┐
              │  成功   │   │  失败   │
              │ SUCCESS │   │  FAILED │
              └─────────┘   └─────────┘
```

### 4.2 车辆状态机

```
                    ┌─────────────┐
              ┌────→│    空闲     │←────┐
              │     │   IDLE      │     │
              │     └──────┬──────┘     │
         任务完成/释放      │ 接收任务    │ 故障恢复
              │            ▼            │
              │     ┌─────────────┐     │
              │     │   配送中    │     │
              │     │  DELIVERING │─────┤
              │     └──────┬──────┘     │
              │            │            │
              │            ▼            │
              │     ┌─────────────┐     │
              └─────│    故障     │─────┘
                    │   FAULTY    │
                    └─────────────┘
```

### 4.3 核心流程时序图

#### 4.3.1 订单创建与分配流程

```
┌────────┐    ┌────────────┐    ┌─────────────┐    ┌────────────┐    ┌──────────┐
│  前端   │    │ OrderService│    │DispatchEngine│    │VehicleService│    │EventBus  │
└───┬────┘    └─────┬──────┘    └──────┬──────┘    └──────┬─────┘    └────┬─────┘
    │               │                  │                  │               │
    │ 1.创建订单     │                  │                  │               │
    │──────────────→│                  │                  │               │
    │               │                  │                  │               │
    │               │ 2.保存订单(CREATED)│                  │               │
    │               │────────────┐     │                  │               │
    │               │            │     │                  │               │
    │               │←───────────┘     │                  │               │
    │               │                  │                  │               │
    │               │ 3.发布OrderCreated│                  │               │
    │               │─────────────────────────────────────────────────────→│
    │               │                  │                  │               │
    │               │                  │ 4.监听并处理分配    │               │
    │               │                  │←──────────────────────────────────│
    │               │                  │                  │               │
    │               │                  │ 5.查询可用车辆      │               │
    │               │                  │──────────────────→│               │
    │               │                  │                  │               │
    │               │                  │ 6.返回可用车辆列表  │               │
    │               │                  │←──────────────────│               │
    │               │                  │                  │               │
    │               │                  │ 7.执行选车策略      │               │
    │               │                  │────┐             │               │
    │               │                  │    │ 计算得分     │               │
    │               │                  │←───┘             │               │
    │               │                  │                  │               │
    │               │ 8.更新订单(ASSIGNED)│                  │               │
    │               │←─────────────────│                  │               │
    │               │                  │                  │               │
    │               │ 9.更新车辆状态(DELIVERING)            │               │
    │               │──────────────────────────────────────→│               │
    │               │                  │                  │               │
    │               │                  │ 10.发布VehicleAssigned             │
    │               │                  │───────────────────────────────────→│
    │               │                  │                  │               │
    │ 11.返回结果    │                  │                  │               │
    │←──────────────│                  │                  │               │
    │               │                  │                  │               │
```

#### 4.3.2 车辆移动仿真流程

```
┌─────────────┐    ┌──────────────┐    ┌────────────┐    ┌──────────┐
│SimulationEngine│   │VehicleService│    │OrderService│    │WebSocket │
└──────┬──────┘    └──────┬───────┘    └─────┬──────┘    └────┬─────┘
       │                  │                  │                │
       │ 1.定时触发(1s)    │                  │                │
       │────┐             │                  │                │
       │    │             │                  │                │
       │←───┘             │                  │                │
       │                  │                  │                │
       │ 2.查询配送中车辆   │                  │                │
       │─────────────────→│                  │                │
       │                  │                  │                │
       │ 3.返回车辆列表    │                  │                │
       │←─────────────────│                  │                │
       │                  │                  │                │
       │ 4.计算新位置      │                  │                │
       │────┐             │                  │                │
       │    │ 4.1 路径规划  │                  │                │
       │    │ 4.2 速度计算  │                  │                │
       │    │ 4.3 位置插值  │                  │                │
       │←───┘             │                  │                │
       │                  │                  │                │
       │ 5.更新车辆位置    │                  │                │
       │─────────────────→│                  │                │
       │                  │                  │                │
       │ 6.检查是否到达    │                  │                │
       │────┐             │                  │                │
       │    │             │                  │                │
       │    ▼             │                  │                │
       │  ┌─────────────┐ │                  │                │
       │  │  未到达？    │─┘                  │                │
       │  │  继续移动    │────────────────────│                │
       │  └─────────────┘                    │                │
       │    │ yes           7.更新订单(COMPLETED)              │
       │    └───────────────────────────────→│                │
       │                                     │                │
       │                                     │ 8.释放车辆      │
       │                                     │───────────────→│
       │                                     │                │
       │ 9.推送位置更新(批量)                 │                │
       │──────────────────────────────────────────────────────→│
       │                                     │                │
```

### 4.4 核心接口定义

```java
// 订单服务接口
public interface OrderService {
    Order createOrder(CreateOrderRequest request);
    Order getOrder(Long orderId);
    void cancelOrder(Long orderId);
    List<Order> getPendingOrders();
}

// 调度引擎接口
public interface DispatchEngine {
    DispatchResult dispatch(Order order);
    void reDispatch(Long vehicleId);
}

// 车辆服务接口
public interface VehicleService {
    List<Vehicle> getAvailableVehicles();
    void updateVehicleStatus(Long vehicleId, VehicleStatus status);
    void updateVehiclePosition(Long vehicleId, Position position);
}

// 仿真引擎接口
public interface SimulationEngine {
    void start();
    void stop();
    void tick(); // 单步执行
}
```

---

## 五、P1 增强功能详细设计

### 5.1 多策略选车算法

#### 5.1.1 策略接口定义

```java
public interface VehicleSelectionStrategy {
    String getName();
    String getDescription();
    Vehicle select(Order order, List<Vehicle> candidates);
    double calculateScore(Order order, Vehicle vehicle);
}
```

#### 5.1.2 策略实现列表

| 策略名称 | 算法描述 | 适用场景 | 时间复杂度 |
|----------|----------|----------|------------|
| **NearestFirst** | 欧几里得距离最短 | 平峰期，追求响应速度 | O(n) |
| **LoadBalance** | 当前任务数最少 | 高峰期，避免单点过载 | O(n) |
| **CompositeScore** | 距离(60%) + 负载(30%) + 电量(10%) | 综合场景，默认策略 | O(n) |
| **FastestArrival** | 考虑当前位置和平均速度估算到达时间 | 时效敏感场景 | O(n) |

#### 5.1.3 综合评分算法详解

```java
public class CompositeScoreStrategy implements VehicleSelectionStrategy {
    
    private static final double W_DISTANCE = 0.6;
    private static final double W_LOAD = 0.3;
    private static final double W_BATTERY = 0.1;
    
    @Override
    public double calculateScore(Order order, Vehicle vehicle) {
        // 距离得分（越近越高，归一化到0-1）
        double distanceScore = 1.0 - normalizeDistance(calculateDistance(order, vehicle));
        
        // 负载得分（任务越少越高）
        double loadScore = 1.0 - normalizeLoad(vehicle.getCurrentTaskCount());
        
        // 电量得分（电量越高越高）
        double batteryScore = vehicle.getBatteryLevel() / 100.0;
        
        return W_DISTANCE * distanceScore 
             + W_LOAD * loadScore 
             + W_BATTERY * batteryScore;
    }
}
```

### 5.2 故障模拟与重调度

#### 5.2.1 故障类型定义

| 故障类型 | 触发方式 | 影响范围 | 恢复策略 |
|----------|----------|----------|----------|
| **随机故障** | 定时任务按概率触发 | 单个车辆 | 手动/自动恢复 |
| **手动故障** | 前端按钮触发 | 单个车辆 | 手动恢复 |
| **批量故障** | 模拟极端场景 | 多个车辆 | 分批恢复 |

#### 5.2.2 重调度流程

```
┌─────────────────────────────────────────────────────────────────┐
│                        故障重调度流程                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 故障触发                                                     │
│     ├── 随机故障：定时任务检测                                   │
│     └── 手动故障：API调用触发                                    │
│                      │                                          │
│                      ▼                                          │
│  2. 状态变更（事务内）                                            │
│     ├── 更新车辆状态：DELIVERING → FAULTY                        │
│     ├── 查询车辆未完成订单                                       │
│     └── 写入Outbox表：状态变更 + 重调度事件                        │
│                      │                                          │
│                      ▼                                          │
│  3. 事件发布                                                     │
│     ├── 事务提交后，异步读取Outbox                                │
│     └── 发布VehicleFaultedEvent + OrderReassignEvent             │
│                      │                                          │
│                      ▼                                          │
│  4. 重调度处理                                                   │
│     ├── 监听OrderReassignEvent                                   │
│     ├── 获取分布式锁（防止重复分配）                              │
│     ├── 订单状态回滚：→ PENDING                                  │
│     ├── 调用DispatchEngine重新分配                               │
│     └── 释放锁，发布新分配事件                                    │
│                      │                                          │
│                      ▼                                          │
│  5. 一致性保证                                                   │
│     ├── 幂等性：事件消费去重                                     │
│     ├── 最终一致性：失败重试3次                                  │
│     └── 死信队列：记录最终失败事件                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 5.2.3 Outbox表结构

```sql
CREATE TABLE outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_type VARCHAR(50) NOT NULL,    -- 聚合类型：ORDER/VEHICLE
    aggregate_id BIGINT NOT NULL,            -- 聚合ID
    event_type VARCHAR(100) NOT NULL,        -- 事件类型
    payload JSON NOT NULL,                   -- 事件内容
    status TINYINT DEFAULT 0,                -- 0:待发送 1:已发送 2:发送失败
    retry_count INT DEFAULT 0,               -- 重试次数
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL,
    INDEX idx_status_created (status, created_at)
);
```

### 5.3 网格地图可视化

#### 5.3.1 坐标系定义

```
                    Y (北)
                    ↑
                    │
    ┌───┬───┬───┬───┼───┬───┬───┬───┐
    │   │   │   │   │   │   │   │   │  100
    ├───┼───┼───┼───┼───┼───┼───┼───┤
    │   │   │   │   │   │   │   │   │  80
    ├───┼───┼───┼───┼───┼───┼───┼───┤
    │   │   │   │   │   │   │   │   │  60
    ├───┼───┼───┼───┼───┼───┼───┼───┤
    │   │   │   │   │   │   │   │   │  40
    ├───┼───┼───┼───┼───┼───┼───┼───┤
    │   │   │   │   │   │   │   │   │  20
    └───┴───┴───┴───┴───┴───┴───┴───┘
    0   20  40  60  80  100 120 140  → X (东)
    
    坐标单位：米
    网格大小：20m × 20m
    地图范围：0-140m × 0-100m
```

#### 5.3.2 可视化元素

| 元素 | 表示方式 | 颜色 | 交互 |
|------|----------|------|------|
| 空闲车辆 | 圆形图标 | 🟢 绿色 | 悬停显示详情 |
| 配送中车辆 | 圆形图标 + 方向箭头 | 🔵 蓝色 | 悬停显示当前订单 |
| 故障车辆 | 圆形图标 + 叉号 | 🔴 红色 | 点击可恢复 |
| 待分配订单 | 方形图标 | 🟡 黄色 | 悬停显示等待时间 |
| 配送中订单 | 方形图标 | 🟠 橙色 | 悬停显示配送车辆 |
| 行驶路线 | 虚线连接 | ⚪ 灰色 | 实时更新 |

#### 5.3.3 WebSocket消息格式

```json
{
  "type": "POSITION_UPDATE",
  "timestamp": "2026-04-22T10:30:00Z",
  "data": {
    "vehicles": [
      {
        "id": 1,
        "x": 45.5,
        "y": 32.0,
        "status": "DELIVERING",
        "heading": 45,
        "speed": 5.2
      }
    ],
    "orders": [
      {
        "id": 101,
        "pickupX": 20.0,
        "pickupY": 30.0,
        "deliveryX": 80.0,
        "deliveryY": 60.0,
        "status": "ASSIGNED"
      }
    ]
  }
}
```

---

## 六、P2 可选功能设计

### 6.1 批量订单生成

#### 6.1.1 配置参数

```java
public class BatchOrderConfig {
    private int totalCount;           // 总订单数
    private int batchSize;            // 每批数量
    private int intervalMs;           // 批次间隔(毫秒)
    private PositionRange pickupRange; // 起点范围
    private PositionRange deliveryRange; // 终点范围
    private double priorityRatio;     // 高优先级订单比例
}
```

#### 6.1.2 生成策略

- **均匀分布**：订单在时间和空间上均匀分布
- **高峰模拟**：模拟早晚高峰的订单集中现象
- **随机分布**：完全随机生成，用于压力测试

### 6.2 性能统计与监控

#### 6.2.1 核心指标

| 指标类别 | 指标名称 | 计算方式 | 更新频率 |
|----------|----------|----------|----------|
| **订单指标** | 总订单数 | 累计计数 | 实时 |
| | 完成率 | 完成数/总数 | 每10秒 |
| | 平均配送时间 | 总配送时间/完成数 | 每10秒 |
| | 平均等待时间 | 总等待时间/完成数 | 每10秒 |
| **车辆指标** | 平均利用率 | 配送中时间/总时间 | 每10秒 |
| | 平均任务数 | 总任务数/车辆数 | 实时 |
| **调度指标** | 平均分配时间 | 分配耗时累计/次数 | 每10秒 |
| | 重调度次数 | 故障导致的重分配次数 | 实时 |

#### 6.2.2 统计面板设计

```
┌─────────────────────────────────────────────────────────────┐
│                      实时监控面板                            │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐           │
│  │  总订单数    │ │   完成率    │ │ 平均配送时间 │           │
│  │   1,234     │ │   92.5%     │ │   8m 32s    │           │
│  └─────────────┘ └─────────────┘ └─────────────┘           │
│                                                            │
│  [订单完成趋势图 - 折线图]                                    │
│                                                            │
│  [车辆利用率热力图 - 时间轴]                                  │
│                                                            │
│  [策略对比雷达图 - 多维度评分]                                │
└─────────────────────────────────────────────────────────────┘
```

### 6.3 历史回放功能

#### 6.3.1 事件存储

```sql
CREATE TABLE simulation_event_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(50) NOT NULL,         -- 仿真会话ID
    event_type VARCHAR(50) NOT NULL,         -- 事件类型
    entity_type VARCHAR(20),                 -- 实体类型：VEHICLE/ORDER
    entity_id BIGINT,                        -- 实体ID
    old_state VARCHAR(20),                   -- 原状态
    new_state VARCHAR(20),                   -- 新状态
    position_x DECIMAL(10,2),                -- X坐标
    position_y DECIMAL(10,2),                -- Y坐标
    metadata JSON,                           -- 扩展信息
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_time (session_id, created_at)
);
```

#### 6.3.2 回放控制

- **播放/暂停**：控制回放状态
- **进度拖动**：跳转到任意时间点
- **速度调节**：0.5x / 1x / 2x / 5x
- **单步模式**：逐帧查看状态变化

---

## 七、数据模型设计

### 7.1 ER图

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│     Order       │       │  OrderAssignment│       │    Vehicle      │
├─────────────────┤       ├─────────────────┤       ├─────────────────┤
│ PK id           │◄──────┤ PK id           │──────►│ PK id           │
│    order_no     │       │    order_id     │       │    vehicle_no   │
│    status       │       │    vehicle_id   │       │    status       │
│    pickup_x     │       │    assigned_at  │       │    current_x    │
│    pickup_y     │       │    completed_at │       │    current_y    │
│    delivery_x   │       └─────────────────┘       │    heading      │
│    delivery_y   │                                 │    speed        │
│    priority     │       ┌─────────────────┐       │    battery      │
│    created_at   │       │  VehicleTask    │       │    capacity     │
│    completed_at │       ├─────────────────┤       │    current_load │
└─────────────────┘       │ PK id           │       └─────────────────┘
                          │    vehicle_id   │◄──────┘
                          │    order_id     │
                          │    task_type    │
                          │    status       │
                          │    created_at   │
                          └─────────────────┘
```

### 7.2 表结构定义

#### 7.2.1 订单表 (orders)

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) UNIQUE NOT NULL COMMENT '订单编号',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-已创建 1-待分配 2-已分配 3-配送中 4-已完成 5-已取消',
    pickup_x DECIMAL(10,2) NOT NULL COMMENT '取货点X坐标',
    pickup_y DECIMAL(10,2) NOT NULL COMMENT '取货点Y坐标',
    delivery_x DECIMAL(10,2) NOT NULL COMMENT '送货点X坐标',
    delivery_y DECIMAL(10,2) NOT NULL COMMENT '送货点Y坐标',
    priority TINYINT DEFAULT 5 COMMENT '优先级 1-10',
    strategy VARCHAR(20) DEFAULT 'COMPOSITE' COMMENT '使用的调度策略',
    assigned_vehicle_id BIGINT COMMENT '分配的车辆ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_at TIMESTAMP NULL COMMENT '分配时间',
    started_at TIMESTAMP NULL COMMENT '开始配送时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    cancel_reason VARCHAR(200) COMMENT '取消原因',
    INDEX idx_status_created (status, created_at),
    INDEX idx_assigned_vehicle (assigned_vehicle_id)
);
```

#### 7.2.2 车辆表 (vehicles)

```sql
CREATE TABLE vehicles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_no VARCHAR(20) UNIQUE NOT NULL COMMENT '车辆编号',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-空闲 1-配送中 2-故障 3-离线',
    current_x DECIMAL(10,2) DEFAULT 0 COMMENT '当前X坐标',
    current_y DECIMAL(10,2) DEFAULT 0 COMMENT '当前Y坐标',
    heading INT DEFAULT 0 COMMENT '朝向角度 0-360',
    speed DECIMAL(5,2) DEFAULT 5.0 COMMENT '当前速度 m/s',
    max_speed DECIMAL(5,2) DEFAULT 10.0 COMMENT '最大速度 m/s',
    battery TINYINT DEFAULT 100 COMMENT '电量百分比',
    capacity INT DEFAULT 10 COMMENT '最大载重 kg',
    current_load INT DEFAULT 0 COMMENT '当前载重 kg',
    total_tasks INT DEFAULT 0 COMMENT '累计完成任务数',
    total_distance DECIMAL(10,2) DEFAULT 0 COMMENT '累计行驶距离 m',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status)
);
```

#### 7.2.3 车辆任务表 (vehicle_tasks)

```sql
CREATE TABLE vehicle_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id BIGINT NOT NULL COMMENT '车辆ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    task_type TINYINT NOT NULL COMMENT '任务类型：1-取货 2-送货',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待执行 1-执行中 2-已完成',
    sequence_no INT DEFAULT 0 COMMENT '执行顺序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    INDEX idx_vehicle_status (vehicle_id, status),
    INDEX idx_order (order_id)
);
```

---

## 八、API 接口设计

### 8.1 RESTful API 列表

#### 订单管理接口

| 方法 | 路径 | 描述 | 请求参数 | 响应 |
|------|------|------|----------|------|
| POST | /api/orders | 创建订单 | CreateOrderRequest | OrderDTO |
| GET | /api/orders/{id} | 获取订单详情 | - | OrderDTO |
| GET | /api/orders | 查询订单列表 | status, page, size | PageResult<OrderDTO> |
| POST | /api/orders/{id}/cancel | 取消订单 | reason | OrderDTO |

#### 车辆管理接口

| 方法 | 路径 | 描述 | 请求参数 | 响应 |
|------|------|------|----------|------|
| GET | /api/vehicles | 获取车辆列表 | status | List<VehicleDTO> |
| GET | /api/vehicles/{id} | 获取车辆详情 | - | VehicleDTO |
| POST | /api/vehicles/{id}/fault | 触发故障 | faultType | VehicleDTO |
| POST | /api/vehicles/{id}/recover | 恢复故障 | - | VehicleDTO |

#### 仿真控制接口

| 方法 | 路径 | 描述 | 请求参数 | 响应 |
|------|------|------|----------|------|
| POST | /api/simulation/start | 启动仿真 | - | Result |
| POST | /api/simulation/stop | 停止仿真 | - | Result |
| POST | /api/simulation/pause | 暂停仿真 | - | Result |
| POST | /api/simulation/resume | 恢复仿真 | - | Result |
| POST | /api/simulation/tick | 单步执行 | - | Result |
| POST | /api/simulation/batch-orders | 批量生成订单 | BatchOrderConfig | Result |

#### 统计查询接口

| 方法 | 路径 | 描述 | 请求参数 | 响应 |
|------|------|------|----------|------|
| GET | /api/statistics/overview | 概览统计 | - | StatisticsOverview |
| GET | /api/statistics/orders | 订单统计 | startTime, endTime | OrderStatistics |
| GET | /api/statistics/vehicles | 车辆统计 | - | VehicleStatistics |
| GET | /api/statistics/strategies | 策略对比 | - | StrategyComparison |

### 8.2 WebSocket 接口

```
连接端点：ws://host/ws/simulation
订阅主题：
  - /topic/vehicle/position    # 车辆位置更新
  - /topic/order/status        # 订单状态变更
  - /topic/statistics/realtime # 实时统计数据
  - /topic/events              # 系统事件

发送目的地：
  - /app/simulation/control    # 仿真控制命令
```

---

## 九、项目目录结构

```
DispatchSim/
├── dispatch-sim-backend/           # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/dispatchsim/
│   │   │   │       ├── DispatchSimApplication.java
│   │   │   │       ├── config/           # 配置类
│   │   │   │       │   ├── WebSocketConfig.java
│   │   │   │       │   ├── RabbitMQConfig.java
│   │   │   │       │   └── RedisConfig.java
│   │   │   │       ├── controller/       # 控制器层
│   │   │   │       │   ├── OrderController.java
│   │   │   │       │   ├── VehicleController.java
│   │   │   │       │   └── SimulationController.java
│   │   │   │       ├── service/          # 服务层
│   │   │   │       │   ├── OrderService.java
│   │   │   │       │   ├── VehicleService.java
│   │   │   │       │   ├── DispatchEngine.java
│   │   │   │       │   ├── SimulationEngine.java
│   │   │   │       │   └── FaultSimulationService.java
│   │   │   │       ├── domain/           # 领域层
│   │   │   │       │   ├── model/        # 领域模型
│   │   │   │       │   │   ├── Order.java
│   │   │   │       │   │   ├── Vehicle.java
│   │   │   │       │   │   └── Position.java
│   │   │   │       │   ├── repository/   # 仓储接口
│   │   │   │       │   ├── event/        # 领域事件
│   │   │   │       │   └── strategy/     # 调度策略
│   │   │   │       ├── infrastructure/   # 基础设施层
│   │   │   │       │   ├── persistence/  # 数据持久化
│   │   │   │       │   ├── messaging/    # 消息队列
│   │   │   │       │   └── websocket/    # WebSocket
│   │   │   │       └── common/           # 公共组件
│   │   │   │           ├── exception/    # 异常处理
│   │   │   │           ├── utils/        # 工具类
│   │   │   │           └── constants/    # 常量定义
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-dev.yml
│   │   │       └── db/migration/         # 数据库迁移脚本
│   │   └── test/
│   └── pom.xml
│
├── dispatch-sim-frontend/          # 前端项目
│   ├── src/
│   │   ├── components/               # 公共组件
│   │   │   ├── MapVisualization.vue  # 地图可视化
│   │   │   ├── VehiclePanel.vue      # 车辆面板
│   │   │   ├── OrderPanel.vue        # 订单面板
│   │   │   └── StatisticsPanel.vue   # 统计面板
│   │   ├── views/                    # 页面视图
│   │   │   ├── Dashboard.vue         # 主控制台
│   │   │   ├── Simulation.vue        # 仿真页面
│   │   │   └── Settings.vue          # 设置页面
│   │   ├── api/                      # API接口
│   │   ├── stores/                   # Pinia状态管理
│   │   ├── utils/                    # 工具函数
│   │   ├── types/                    # TypeScript类型
│   │   ├── App.vue
│   │   └── main.ts
│   ├── package.json
│   └── vite.config.ts
│
├── docs/                           # 文档
│   ├── design.md                   # 设计文档
│   ├── api.md                      # API文档
│   └── deployment.md               # 部署文档
│
├── docker-compose.yml              # Docker编排
└── README.md                       # 项目说明
```

---

## 十、非功能性需求

### 10.1 性能需求

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 订单创建响应时间 | < 100ms | P99 |
| 车辆位置更新频率 | 1Hz | 每秒更新一次 |
| WebSocket延迟 | < 50ms | 服务端到客户端 |
| 支持并发车辆数 | ≥ 100 | 单实例 |
| 支持并发订单数 | ≥ 1000 | 单实例 |

### 10.2 可靠性需求

- **数据持久化**：所有订单和车辆状态变更必须持久化到数据库
- **故障恢复**：服务重启后能够恢复之前的仿真状态
- **消息可靠性**：重要事件至少投递一次，消费端幂等处理

### 10.3 可扩展性需求

- **水平扩展**：无状态服务设计，支持多实例部署
- **策略扩展**：新增调度策略无需修改核心代码
- **事件扩展**：新增事件类型不影响现有处理逻辑

### 10.4 安全需求

- **输入校验**：所有外部输入必须校验
- **SQL注入防护**：使用参数化查询
- **XSS防护**：前端输出转义

---

## 十一、风险分析与应对

### 11.1 技术风险

| 风险 | 可能性 | 影响 | 应对策略 |
|------|--------|------|----------|
| WebSocket连接不稳定 | 中 | 高 | 实现心跳检测和自动重连机制 |
| 数据库性能瓶颈 | 中 | 高 | 使用Redis缓存热点数据，读写分离 |
| 并发冲突 | 中 | 高 | 使用分布式锁，乐观锁机制 |
| 内存泄漏 | 低 | 高 | 定期压测，监控内存使用 |

### 11.2 进度风险

| 风险 | 可能性 | 影响 | 应对策略 |
|------|--------|------|----------|
| 故障重调度复杂度超预期 | 中 | 高 | 先实现简化版，保证核心流程可用 |
| 前端可视化耗时超预期 | 中 | 中 | 使用现成图表库，降低自定义开发 |
| 第三方组件兼容性问题 | 低 | 中 | 预留缓冲时间，准备备选方案 |

### 11.3 缓解措施

1. **MVP优先**：确保P0功能稳定后再进行P1开发
2. **持续集成**：每日构建，及时发现集成问题
3. **代码审查**：关键模块必须经过Review
4. **文档先行**：复杂功能先写设计文档再编码

---

## 十二、开发计划与里程碑

### 12.1 开发阶段

```
Week 1: P0 核心流程
├── Day 1-2: 项目搭建 + 数据模型 + 基础CRUD
├── Day 3-4: 订单状态机 + 调度引擎基础
├── Day 5-6: 车辆移动仿真 + WebSocket通信
└── Day 7: 集成测试 + Bug修复

Week 2: P1 增强功能
├── Day 8: 多策略选车算法
├── Day 9-10: 故障模拟 + Outbox模式
├── Day 11: 重调度流程实现
└── Day 12-13: 网格地图可视化

Week 3: P2 可选功能 + 优化
├── Day 14: 批量订单生成
├── Day 15: 性能统计面板
├── Day 16-17: 性能优化 + 压力测试
└── Day 18-20: 文档完善 + 面试准备
```

### 12.2 里程碑检查点

| 里程碑 | 验收标准 | 时间点 |
|--------|----------|--------|
| M1 - 基础框架 | 项目可运行，数据库连接正常 | Day 2 |
| M2 - 核心流程 | 订单创建到完成全流程跑通 | Day 7 |
| M3 - 增强功能 | 多策略+故障重调度+可视化完成 | Day 13 |
| M4 - 项目完成 | 所有功能稳定，文档完整 | Day 20 |

---

## 十三、面试展示要点

### 13.1 技术亮点梳理

| 技术点 | 解决的问题 | 展示方式 |
|--------|------------|----------|
| **事件驱动架构** | 服务解耦，支持异步处理 | 展示事件流转图 |
| **Outbox模式** | 事务与消息一致性 | 代码+流程图 |
| **状态机模式** | 状态流转清晰可控 | 状态图+代码 |
| **策略模式** | 算法可插拔 | 现场切换策略对比效果 |
| **分布式锁** | 防止并发冲突 | 压测展示 |

### 13.2 演示流程建议

1. **项目介绍**（2分钟）
   - 项目背景和目标
   - 技术栈选择理由
   - 核心功能概览

2. **架构讲解**（3分钟）
   - 分层架构图
   - 事件驱动设计
   - 关键技术决策

3. **功能演示**（5分钟）
   - 创建订单并查看自动分配
   - 切换不同调度策略对比
   - 触发车辆故障观察重调度
   - 查看实时统计面板

4. **代码亮点**（3分钟）
   - Outbox模式实现
   - 状态机设计
   - 策略模式应用

5. **Q&A准备**
   - 如何处理高并发？
   - 为什么选择这些技术？
   - 如果车辆数增加10倍怎么优化？

### 13.3 与 FSD-Core 的关联

- **技术栈一致**：Spring Boot、事件驱动、分布式锁
- **业务场景相似**：调度系统、状态管理、故障处理
- **设计思想相通**：领域驱动、可扩展架构

---

## 附录

### A. 术语表

| 术语 | 说明 |
|------|------|
| Outbox模式 | 解决分布式事务中数据变更与消息发送一致性的设计模式 |
| 事件溯源 | 通过记录状态变更事件来重建对象状态的模式 |
| 领域事件 | 领域中发生的具有业务含义的事件 |
| 聚合根 | 领域模型中的核心实体，维护业务一致性边界 |

### B. 参考资料

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [领域驱动设计](https://domainlanguage.com/ddd/reference/)
- [ECharts 文档](https://echarts.apache.org/)

---

**文档版本历史**

| 版本 | 日期 | 修改内容 | 作者 |
|------|------|----------|------|
| v1.0 | - | 初始版本 | - |
| v2.0 | 2026-04-22 | 重构优化：补充架构设计、详细流程、数据模型、API设计、风险分析等 | AI Assistant |
