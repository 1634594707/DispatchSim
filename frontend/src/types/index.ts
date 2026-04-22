// Vehicle Types
export const VehicleStatus = {
  IDLE: 'IDLE',
  DELIVERING: 'DELIVERING',
  FAULTY: 'FAULTY',
  OFFLINE: 'OFFLINE',
} as const

export type VehicleStatus = (typeof VehicleStatus)[keyof typeof VehicleStatus]

export interface Position {
  x: number
  y: number
}

export interface Vehicle {
  id: number
  status: VehicleStatus
  currentPosition: Position
  battery: number
  speed: number
  maxSpeed: number
  currentLoad: number
  capacity: number
  heading?: number // angle in degrees
  totalTasks: number
  totalDistance: number
  currentOrderId?: number
}

// Order Types
export const OrderStatus = {
  PENDING: 'PENDING',
  ASSIGNED: 'ASSIGNED',
  DELIVERING: 'DELIVERING',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED',
} as const

export type OrderStatus = (typeof OrderStatus)[keyof typeof OrderStatus]

export interface Order {
  id: number
  status: OrderStatus
  pickup: Position
  delivery: Position
  priority: number
  assignedVehicleId?: number
  createdAt: Date
  completedAt?: Date
  cancellationReason?: string
}

// Simulation Types
export const SimulationStatus = {
  STOPPED: 'STOPPED',
  RUNNING: 'RUNNING',
  PAUSED: 'PAUSED',
} as const

export type SimulationStatus = (typeof SimulationStatus)[keyof typeof SimulationStatus]

export const DispatchStrategy = {
  NEAREST_FIRST: 'NEAREST_FIRST',
  LOAD_BALANCE: 'LOAD_BALANCE',
  COMPOSITE_SCORE: 'COMPOSITE_SCORE',
  FASTEST_ARRIVAL: 'FASTEST_ARRIVAL',
} as const

export type DispatchStrategy = (typeof DispatchStrategy)[keyof typeof DispatchStrategy]

export interface SimulationState {
  status: SimulationStatus
  strategy: DispatchStrategy
  elapsedTime: number
}

// Statistics Types
export interface Statistics {
  totalOrders: number
  completedOrders: number
  completionRate: number
  averageDeliveryTime: number
  vehicleUtilization: number
}

// Obstacle Types
export const ObstacleType = {
  BUILDING: 'BUILDING',
  ROAD: 'ROAD',
  PARKING: 'PARKING',
  GREEN_SPACE: 'GREEN_SPACE',
} as const

export type ObstacleType = (typeof ObstacleType)[keyof typeof ObstacleType]

export interface Obstacle {
  id: string
  type: ObstacleType
  x: number
  y: number
  width: number
  height: number
  label?: string
}
