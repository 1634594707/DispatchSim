import type { Order, Position, Vehicle } from '@/types'
import type { RoadEdge, RoadNetwork, RoadNode } from '@/stores/roadNetwork'

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value))

const distance = (from: Position, to: Position) => {
  const dx = to.x - from.x
  const dy = to.y - from.y
  return Math.hypot(dx, dy)
}

export interface PerformanceScenario {
  network: RoadNetwork
  vehicles: Vehicle[]
  orders: Order[]
}

export interface PerformanceScenarioOptions {
  width: number
  height: number
  nodeCount?: number
  vehicleCount?: number
  orderCount?: number
}

export const createPerformanceScenario = ({
  width,
  height,
  nodeCount = 1000,
  vehicleCount = 100,
  orderCount = 500,
}: PerformanceScenarioOptions): PerformanceScenario => {
  const columns = Math.ceil(Math.sqrt(nodeCount))
  const rows = Math.ceil(nodeCount / columns)
  const marginX = 56
  const marginY = 56
  const spacingX = Math.max(18, (width - marginX * 2) / Math.max(columns - 1, 1))
  const spacingY = Math.max(18, (height - marginY * 2) / Math.max(rows - 1, 1))

  const nodes: RoadNode[] = []
  for (let index = 0; index < nodeCount; index++) {
    const row = Math.floor(index / columns)
    const column = index % columns
    const stagger = row % 2 === 0 ? 0 : spacingX * 0.18
    const x = clamp(marginX + column * spacingX + stagger, 24, width - 24)
    const y = clamp(height - marginY - row * spacingY, 24, height - 24)

    nodes.push({
      id: index + 1,
      position: { x, y },
      type: index % 97 === 0 ? 'depot' : index % 29 === 0 ? 'poi' : 'intersection',
      metadata: {
        label: `N-${index + 1}`,
      },
    })
  }

  const edges: RoadEdge[] = []
  let edgeId = 1
  const pushEdge = (fromNodeId: number, toNodeId: number) => {
    const fromNode = nodes[fromNodeId - 1]
    const toNode = nodes[toNodeId - 1]
    if (!fromNode || !toNode) {
      return
    }

    edges.push({
      id: edgeId++,
      fromNodeId,
      toNodeId,
      bidirectional: true,
      weight: distance(fromNode.position, toNode.position),
    })
  }

  for (let row = 0; row < rows; row++) {
    for (let column = 0; column < columns; column++) {
      const index = row * columns + column
      if (index >= nodeCount) {
        continue
      }

      const currentNodeId = index + 1
      const rightIndex = index + 1
      const bottomIndex = index + columns
      const diagonalIndex = index + columns + 1

      if (column < columns - 1 && rightIndex < nodeCount) {
        pushEdge(currentNodeId, rightIndex + 1)
      }

      if (bottomIndex < nodeCount) {
        pushEdge(currentNodeId, bottomIndex + 1)
      }

      if (column < columns - 1 && row < rows - 1 && diagonalIndex < nodeCount && (row + column) % 3 === 0) {
        pushEdge(currentNodeId, diagonalIndex + 1)
      }
    }
  }

  const vehicles: Vehicle[] = Array.from({ length: vehicleCount }, (_, index) => {
    const node = nodes[(index * 7) % nodes.length]
    return {
      id: index + 1,
      status: index % 11 === 0 ? 'FAULTY' : index % 5 === 0 ? 'DELIVERING' : 'IDLE',
      currentPosition: { ...node.position },
      battery: 35 + (index * 13) % 65,
      speed: 8,
      maxSpeed: 12,
      currentLoad: index % 5 === 0 ? 1 : 0,
      capacity: 80,
      heading: (index * 17) % 360,
      totalTasks: (index * 3) % 28,
      totalDistance: 1200 + index * 35,
      currentOrderId: index % 5 === 0 ? index + 1 : undefined,
      orderQueue: index % 7 === 0 ? [index + 1, ((index + 37) % orderCount) + 1] : [],
      loadingTimeRemaining: index % 9 === 0 ? 3 : 0,
    }
  })

  const orders: Order[] = Array.from({ length: orderCount }, (_, index) => {
    const pickupNode = nodes[(index * 5) % nodes.length]
    const deliveryNode = nodes[(index * 11 + 41) % nodes.length]
    const cycle = index % 10
    const status =
      cycle < 4 ? 'PENDING' :
      cycle < 6 ? 'ASSIGNED' :
      cycle < 8 ? 'DELIVERING' :
      cycle === 8 ? 'COMPLETED' :
      'CANCELLED'

    const createdAt = new Date(Date.now() - (index % 120) * 60_000)
    const completedAt = status === 'COMPLETED' ? new Date(createdAt.getTime() + 18 * 60_000) : undefined

    return {
      id: index + 1,
      status,
      pickup: { ...pickupNode.position },
      delivery: { ...deliveryNode.position },
      priority: (index % 10) + 1,
      assignedVehicleId: status === 'ASSIGNED' || status === 'DELIVERING' ? ((index % vehicleCount) + 1) : undefined,
      createdAt,
      completedAt,
      archived: false,
    }
  })

  return {
    network: {
      nodes,
      edges,
      version: 1,
      updatedAt: new Date().toISOString(),
    },
    vehicles,
    orders,
  }
}
