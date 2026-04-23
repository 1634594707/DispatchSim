import type {
  DispatchStrategy,
  Order,
  OrderStatus,
  Position,
  SimulationStatus,
  Vehicle,
  VehicleStatus,
} from '@/types'

export interface ApiEnvelope<T> {
  code?: number
  message?: string
  data?: T
}

export interface PageResult<T> {
  content?: T[]
  records?: T[]
  items?: T[]
  total?: number
  totalElements?: number
  page?: number
  size?: number
  totalPages?: number
}

export interface PositionDto {
  x: number
  y: number
}

export interface OrderDto {
  id: number | string
  status: OrderStatus
  pickup?: PositionDto
  delivery?: PositionDto
  pickupX?: number
  pickupY?: number
  deliveryX?: number
  deliveryY?: number
  priority: number
  assignedVehicleId?: number | string | null
  createdAt?: string
  completedAt?: string | null
  cancellationReason?: string | null
  archived?: boolean
  archivedAt?: string | null
  archivalReason?: string | null
  depotId?: number | string | null
}

export interface CreateOrderRequestDto {
  pickup: PositionDto
  delivery: PositionDto
  priority: number
}

export interface CancelOrderRequestDto {
  reason?: string
}

export interface ArchiveOrderRequestDto {
  reason?: string
}

export interface VehicleDto {
  id: number | string
  status: VehicleStatus
  currentPosition?: PositionDto
  position?: PositionDto
  x?: number
  y?: number
  battery: number
  speed: number
  maxSpeed: number
  currentLoad: number
  capacity: number
  heading?: number | null
  totalTasks?: number
  totalDistance?: number
  currentOrderId?: number | string | null
  orderQueue?: Array<number | string>
  loadingTimeRemaining?: number
}

export interface TriggerFaultRequestDto {
  faultType?: string
}

export interface DepotDto {
  id: number | string
  name: string
  position: PositionDto
  icon?: string | null
  metadata?: string | null
  createdAt?: string
}

export interface DepotUpsertRequestDto {
  name: string
  position: PositionDto
  icon?: string
  metadata?: string
}

export interface SimulationStatusDto {
  status: SimulationStatus
  strategy: DispatchStrategy
  sessionId?: string
  elapsedTime: number
  speed?: number
  stepMode?: boolean
  pauseEditingEnabled?: boolean
}

export interface UpdateSimulationSpeedRequestDto {
  speed: number
}

export interface UpdateStrategyRequestDto {
  strategy: DispatchStrategy
}

export type BatchOrderGenerationStrategyDto = 'UNIFORM' | 'PEAK' | 'RANDOM'

export interface BatchOrderRangeDto {
  minX: number
  maxX: number
  minY: number
  maxY: number
}

export interface BatchOrderRequestDto {
  totalOrders: number
  batchSize: number
  batchIntervalMs: number
  strategy: BatchOrderGenerationStrategyDto
  pickupRange: BatchOrderRangeDto
  deliveryRange: BatchOrderRangeDto
  priority: number
}

export interface BatchOrderResponseDto {
  totalOrders: number
  batchesCreated: number
  strategy: BatchOrderGenerationStrategyDto
  batchIntervalMs: number
  orderIds: number[]
}

export type ReplayActionDto = 'PLAY' | 'PAUSE' | 'SEEK' | 'SPEED' | 'STEP'
export type ReplayEventTypeDto = 'ORDER_STATUS_CHANGED' | 'VEHICLE_STATUS_CHANGED' | 'VEHICLE_POSITION_UPDATED'

export interface ReplaySessionDto {
  sessionId: string
  startedAt?: string
  endedAt?: string
  eventCount: number
}

export interface ReplayEventDto {
  id: number
  sessionId: string
  eventType: ReplayEventTypeDto
  aggregateType: string
  aggregateId: number
  eventTime: string
  payload: unknown
}

export interface ReplayControlRequestDto {
  sessionId: string
  action: ReplayActionDto
  progress?: number
  speed?: number
  step?: number
}

export interface ReplayFrameDto {
  sessionId: string
  action: ReplayActionDto
  cursorTime?: string | null
  speed: number
  progress: number
  appliedEvents: number
  totalEvents: number
  orders: OrderDto[]
  vehicles: VehicleDto[]
}

export interface ResultDto {
  success?: boolean
  message?: string
}

export interface StatisticsOverviewDto {
  totalOrders: number
  completedOrders: number
  completionRate: number
  averageDeliveryTime?: number
  avgDeliveryTime?: number
  vehicleUtilization?: number
  avgUtilization?: number
  avgWaitingTime?: number
  avgDispatchTime?: number
  reDispatchCount?: number
  pendingOrdersCount?: number
  deliveringOrdersCount?: number
  activeVehicles?: number
  totalDistance?: number
  averageDistance?: number
}

export interface OrderStatisticsDto {
  pendingOrders?: number
  assignedOrders?: number
  deliveringOrders?: number
  completedOrders?: number
  cancelledOrders?: number
}

export interface VehicleStatisticsDto {
  totalVehicles?: number
  idleVehicles?: number
  deliveringVehicles?: number
  faultyVehicles?: number
  offlineVehicles?: number
}

export interface StrategyStatisticsItemDto {
  strategy: DispatchStrategy
  count: number
  percentage: number
}

export interface StrategyStatisticsDto {
  items?: StrategyStatisticsItemDto[]
  strategies?: StrategyStatisticsItemDto[]
}

export interface PerformanceMetricPointDto {
  name: string
  value: number
  label: string
}

export interface PerformanceMetricsDto {
  latestDispatchDurationMs: number
  averageDispatchDurationMs: number
  websocketMessagesLastMinute: number
  slowQueryCount: number
  slowQueries?: PerformanceMetricPointDto[]
  dispatchMetrics?: PerformanceMetricPointDto[]
  websocketMetrics?: PerformanceMetricPointDto[]
}

const toNumber = (value: number | string | null | undefined, fallback = 0) => {
  if (value === null || value === undefined || value === '') {
    return fallback
  }

  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

const toDate = (value?: string | null) => {
  return value ? new Date(value) : undefined
}

export const normalizePosition = (input?: PositionDto | null, x?: number, y?: number): Position => ({
  x: input?.x ?? x ?? 0,
  y: input?.y ?? y ?? 0,
})

export const normalizeOrder = (dto: OrderDto): Order => ({
  id: toNumber(dto.id),
  status: dto.status,
  pickup: normalizePosition(dto.pickup, dto.pickupX, dto.pickupY),
  delivery: normalizePosition(dto.delivery, dto.deliveryX, dto.deliveryY),
  priority: dto.priority,
  assignedVehicleId:
    dto.assignedVehicleId === null || dto.assignedVehicleId === undefined
      ? undefined
      : toNumber(dto.assignedVehicleId),
  createdAt: toDate(dto.createdAt) ?? new Date(),
  completedAt: toDate(dto.completedAt),
  cancellationReason: dto.cancellationReason ?? undefined,
  archived: dto.archived ?? false,
  archivedAt: toDate(dto.archivedAt),
  archivalReason: dto.archivalReason ?? undefined,
  depotId:
    dto.depotId === null || dto.depotId === undefined
      ? undefined
      : toNumber(dto.depotId),
})

export const normalizeVehicle = (dto: VehicleDto): Vehicle => ({
  id: toNumber(dto.id),
  status: dto.status,
  currentPosition: normalizePosition(dto.currentPosition ?? dto.position, dto.x, dto.y),
  battery: dto.battery,
  speed: dto.speed,
  maxSpeed: dto.maxSpeed,
  currentLoad: dto.currentLoad,
  capacity: dto.capacity,
  heading: dto.heading ?? undefined,
  totalTasks: dto.totalTasks ?? 0,
  totalDistance: dto.totalDistance ?? 0,
  currentOrderId:
    dto.currentOrderId === null || dto.currentOrderId === undefined
      ? undefined
      : toNumber(dto.currentOrderId),
  orderQueue: (dto.orderQueue ?? []).map((id) => toNumber(id)),
  loadingTimeRemaining: dto.loadingTimeRemaining ?? 0,
})
