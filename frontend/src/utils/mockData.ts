import type { Vehicle, Order, Position, Obstacle, ObstacleType } from '@/types'

import { MAP_WIDTH, MAP_HEIGHT } from '@/constants/map'

// Generate random position within map bounds
export const generateRandomPosition = (): Position => {
  return {
    x: Math.random() * MAP_WIDTH,
    y: Math.random() * MAP_HEIGHT,
  }
}

// Generate random position with margin from edges
export const generateRandomPositionWithMargin = (margin: number = 10): Position => {
  return {
    x: margin + Math.random() * (MAP_WIDTH - 2 * margin),
    y: margin + Math.random() * (MAP_HEIGHT - 2 * margin),
  }
}

// Generate mock vehicles
export const generateMockVehicles = (count: number = 10): Vehicle[] => {
  const vehicles: Vehicle[] = []
  const statuses = ['IDLE', 'IDLE', 'IDLE', 'DELIVERING', 'DELIVERING', 'FAULTY', 'OFFLINE'] as const
  
  for (let i = 0; i < count; i++) {
    const status = statuses[Math.floor(Math.random() * statuses.length)]
    
    vehicles.push({
      id: i + 1,
      status,
      currentPosition: generateRandomPositionWithMargin(),
      battery: 50 + Math.floor(Math.random() * 50), // 50-100%
      speed: 5,
      maxSpeed: 10,
      currentLoad: 0,
      capacity: 10,
      totalTasks: Math.floor(Math.random() * 20),
      totalDistance: Math.floor(Math.random() * 500),
      heading: status === 'DELIVERING' ? Math.random() * 360 : undefined,
    })
  }
  
  return vehicles
}

// Generate mock orders
export const generateMockOrders = (count: number = 5): Order[] => {
  const orders: Order[] = []
  const statuses = ['PENDING', 'ASSIGNED', 'DELIVERING', 'COMPLETED'] as const
  
  for (let i = 0; i < count; i++) {
    const status = statuses[Math.floor(Math.random() * statuses.length)]
    const createdAt = new Date(Date.now() - Math.random() * 3600000) // Within last hour
    
    orders.push({
      id: i + 1,
      status,
      pickup: generateRandomPositionWithMargin(),
      delivery: generateRandomPositionWithMargin(),
      priority: Math.floor(Math.random() * 10) + 1, // 1-10
      createdAt,
      completedAt: status === 'COMPLETED' ? new Date() : undefined,
    })
  }
  
  return orders
}

// Calculate distance between two positions
export const calculateDistance = (pos1: Position, pos2: Position): number => {
  const dx = pos2.x - pos1.x
  const dy = pos2.y - pos1.y
  return Math.sqrt(dx * dx + dy * dy)
}

// Calculate angle between two positions (in degrees)
export const calculateAngle = (from: Position, to: Position): number => {
  const dx = to.x - from.x
  const dy = to.y - from.y
  return (Math.atan2(dy, dx) * 180) / Math.PI
}

// Move position towards target by distance
export const moveTowards = (
  current: Position,
  target: Position,
  distance: number
): Position => {
  const totalDistance = calculateDistance(current, target)
  
  if (totalDistance <= distance) {
    return { ...target }
  }
  
  const ratio = distance / totalDistance
  return {
    x: current.x + (target.x - current.x) * ratio,
    y: current.y + (target.y - current.y) * ratio,
  }
}

// Check if position is within bounds
export const isWithinBounds = (position: Position): boolean => {
  return (
    position.x >= 0 &&
    position.x <= MAP_WIDTH &&
    position.y >= 0 &&
    position.y <= MAP_HEIGHT
  )
}

// Clamp position to bounds
export const clampToBounds = (position: Position): Position => {
  return {
    x: Math.max(0, Math.min(MAP_WIDTH, position.x)),
    y: Math.max(0, Math.min(MAP_HEIGHT, position.y)),
  }
}

// Generate random priority
export const generateRandomPriority = (): number => {
  return Math.floor(Math.random() * 10) + 1
}

// Format time duration
export const formatDuration = (seconds: number): string => {
  if (seconds < 60) {
    return `${Math.floor(seconds)}秒`
  }
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = Math.floor(seconds % 60)
  return `${minutes}分${remainingSeconds}秒`
}

// Format distance
export const formatDistance = (meters: number): string => {
  if (meters < 1000) {
    return `${Math.floor(meters)}m`
  }
  return `${(meters / 1000).toFixed(2)}km`
}

// Generate campus obstacles (buildings, roads, parking, green spaces)
export const generateCampusObstacles = (): Obstacle[] => {
  const obstacles: Obstacle[] = []
  
  // Main buildings
  obstacles.push(
    {
      id: 'building-1',
      type: 'BUILDING' as ObstacleType,
      x: 20,
      y: 20,
      width: 25,
      height: 20,
      label: '教学楼A'
    },
    {
      id: 'building-2',
      type: 'BUILDING' as ObstacleType,
      x: 55,
      y: 15,
      width: 30,
      height: 25,
      label: '图书馆'
    },
    {
      id: 'building-3',
      type: 'BUILDING' as ObstacleType,
      x: 95,
      y: 20,
      width: 25,
      height: 20,
      label: '实验楼'
    },
    {
      id: 'building-4',
      type: 'BUILDING' as ObstacleType,
      x: 15,
      y: 55,
      width: 20,
      height: 25,
      label: '食堂'
    },
    {
      id: 'building-5',
      type: 'BUILDING' as ObstacleType,
      x: 100,
      y: 60,
      width: 25,
      height: 20,
      label: '宿舍楼'
    }
  )
  
  // Parking lots
  obstacles.push(
    {
      id: 'parking-1',
      type: 'PARKING' as ObstacleType,
      x: 50,
      y: 50,
      width: 15,
      height: 10,
      label: '停车场1'
    },
    {
      id: 'parking-2',
      type: 'PARKING' as ObstacleType,
      x: 75,
      y: 70,
      width: 15,
      height: 10,
      label: '停车场2'
    }
  )
  
  // Green spaces
  obstacles.push(
    {
      id: 'green-1',
      type: 'GREEN_SPACE' as ObstacleType,
      x: 45,
      y: 70,
      width: 20,
      height: 15,
      label: '中心花园'
    },
    {
      id: 'green-2',
      type: 'GREEN_SPACE' as ObstacleType,
      x: 70,
      y: 45,
      width: 15,
      height: 15,
      label: '绿地'
    }
  )
  
  return obstacles
}

// Check if position collides with obstacle
export const checkObstacleCollision = (position: Position, obstacles: Obstacle[]): boolean => {
  return obstacles.some(obstacle => {
    return (
      position.x >= obstacle.x &&
      position.x <= obstacle.x + obstacle.width &&
      position.y >= obstacle.y &&
      position.y <= obstacle.y + obstacle.height
    )
  })
}
