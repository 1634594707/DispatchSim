import { Client } from '@stomp/stompjs'
import { computed, readonly, ref } from 'vue'
import { normalizeOrder, normalizeVehicle } from '@/api/types'
import { reportWebsocketError } from '@/utils/errorHandler'
import { throttle } from '@/utils/performance'
import { useOrderStore } from '@/stores/order'
import { useStatisticsStore } from '@/stores/statistics'
import { useToastStore } from '@/stores/toast'
import { useVehicleStore } from '@/stores/vehicle'

type ConnectionState = 'disconnected' | 'connecting' | 'connected' | 'reconnecting' | 'error'

const connectionState = ref<ConnectionState>('disconnected')
const lastErrorMessage = ref<string | null>(null)

const resolveWebsocketUrl = () => {
  const envUrl = import.meta.env.VITE_WS_URL?.trim()
  if (envUrl) {
    return envUrl
  }

  if (typeof window !== 'undefined') {
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    return `${protocol}://${window.location.host}/ws/simulation`
  }

  return 'ws://localhost:8080/ws/simulation'
}

const unwrapPayload = (payload: unknown) => {
  if (payload && typeof payload === 'object' && 'data' in payload) {
    return (payload as { data: unknown }).data
  }

  return payload
}

const safeParse = (body: string) => {
  try {
    return JSON.parse(body)
  } catch (error) {
    throw reportWebsocketError(error)
  }
}

class DispatchSimWebsocketService {
  private client: Client | null = null
  private initialized = false
  private hasConnectedOnce = false
  private throttledVehiclePositionUpdate = throttle((payload: unknown) => {
    this.applyVehiclePositionUpdate(payload)
  }, 120)
  private throttledOrderStatusUpdate = throttle((payload: unknown) => {
    this.applyOrderStatusUpdate(payload)
  }, 120)
  private throttledVehicleStatusUpdate = throttle((payload: unknown) => {
    this.applyVehicleStatusUpdate(payload)
  }, 120)

  connect() {
    if (this.client?.active) {
      return
    }

    const toastStore = useToastStore()

    this.client = new Client({
      brokerURL: resolveWebsocketUrl(),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {},
    })

    connectionState.value = this.hasConnectedOnce ? 'reconnecting' : 'connecting'
    lastErrorMessage.value = null

    this.client.onConnect = () => {
      connectionState.value = 'connected'
      lastErrorMessage.value = null

      if (!this.initialized) {
        toastStore.success('实时连接已建立', '车辆与订单状态将自动刷新', 2000)
      }

      this.initialized = true
      this.hasConnectedOnce = true
      this.subscribeTopics()
    }

    this.client.onStompError = (frame) => {
      connectionState.value = 'error'
      lastErrorMessage.value = frame.headers.message || 'STOMP 服务异常'
      reportWebsocketError(new Error(lastErrorMessage.value))
    }

    this.client.onWebSocketClose = () => {
      if (this.client?.active) {
        connectionState.value = 'reconnecting'
      } else {
        connectionState.value = 'disconnected'
      }
    }

    this.client.onWebSocketError = () => {
      connectionState.value = 'error'
      lastErrorMessage.value = 'WebSocket 连接失败'
      reportWebsocketError(new Error(lastErrorMessage.value))
    }

    this.client.activate()
  }

  disconnect() {
    if (!this.client) {
      return
    }

    this.client.deactivate()
    this.client = null
    connectionState.value = 'disconnected'
  }

  private subscribeTopics() {
    if (!this.client || !this.client.connected) {
      return
    }

    this.client.subscribe('/topic/vehicle/position', (message) => {
      const payload = unwrapPayload(safeParse(message.body))
      this.throttledVehiclePositionUpdate(payload)
    })

    this.client.subscribe('/topic/order/status', (message) => {
      const payload = unwrapPayload(safeParse(message.body))
      this.throttledOrderStatusUpdate(payload)
    })

    this.client.subscribe('/topic/vehicle/status', (message) => {
      const payload = unwrapPayload(safeParse(message.body))
      this.throttledVehicleStatusUpdate(payload)
    })

    this.client.subscribe('/topic/statistics/realtime', (message) => {
      const payload = unwrapPayload(safeParse(message.body))
      this.handleRealtimeStatistics(payload)
    })
  }

  private applyVehiclePositionUpdate(payload: unknown) {
    const vehicleStore = useVehicleStore()
    const orderStore = useOrderStore()

    const data = payload as
      | {
          vehicles?: unknown[]
          orders?: unknown[]
        }
      | unknown[]

    const vehicles = Array.isArray(data) ? data : data?.vehicles ?? []
    vehicles.forEach((item) => {
      const vehicle = normalizeVehicle(item as Parameters<typeof normalizeVehicle>[0])
      vehicleStore.updateVehicle(vehicle.id, vehicle)
    })

    const orders = Array.isArray(data) ? [] : data?.orders ?? []
    orders.forEach((item) => {
      const order = normalizeOrder(item as Parameters<typeof normalizeOrder>[0])
      orderStore.updateOrder(order.id, order)
    })
  }

  private applyOrderStatusUpdate(payload: unknown) {
    const orderStore = useOrderStore()
    const updates = Array.isArray(payload) ? payload : [payload]

    updates.forEach((item) => {
      const order = normalizeOrder(item as Parameters<typeof normalizeOrder>[0])
      orderStore.updateOrder(order.id, order)
    })
  }

  private applyVehicleStatusUpdate(payload: unknown) {
    const vehicleStore = useVehicleStore()
    const updates = Array.isArray(payload) ? payload : [payload]

    updates.forEach((item) => {
      const vehicle = normalizeVehicle(item as Parameters<typeof normalizeVehicle>[0])
      vehicleStore.updateVehicle(vehicle.id, vehicle)
    })
  }

  private handleRealtimeStatistics(payload: unknown) {
    const statisticsStore = useStatisticsStore()
    const snapshot = payload as {
      totalOrders?: number
      completedOrders?: number
      completionRate?: number
      averageDeliveryTime?: number
      vehicleUtilization?: number
      avgWaitingTime?: number
      avgDispatchTime?: number
      reDispatchCount?: number
      pendingOrdersCount?: number
      deliveringOrdersCount?: number
      activeVehicles?: number
      totalDistance?: number
      averageDistance?: number
    }

    statisticsStore.applyOverviewSnapshot(snapshot)
  }
}

const websocketService = new DispatchSimWebsocketService()

export const websocketConnectionState = readonly(connectionState)
export const websocketLastErrorMessage = readonly(lastErrorMessage)
export const isWebsocketConnected = computed(() => connectionState.value === 'connected')

export const connectWebsocket = () => {
  websocketService.connect()
}

export const disconnectWebsocket = () => {
  websocketService.disconnect()
}

export const useWebsocketStatus = () => {
  return {
    connectionState: websocketConnectionState,
    lastErrorMessage: websocketLastErrorMessage,
  }
}
