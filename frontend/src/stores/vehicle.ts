import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { Vehicle, VehicleStatus } from '@/types'
import { getVehicleByIdApi, getVehiclesApi, recoverVehicleApi, triggerFaultApi } from '@/api/vehicle'
import { normalizeVehicle } from '@/api/types'
import { getErrorMessage, reportError } from '@/utils/errorHandler'
import { useToastStore } from './toast'

export const useVehicleStore = defineStore('vehicle', () => {
  const vehicles = ref<Vehicle[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const toastStore = useToastStore()

  const setVehicles = (nextVehicles: Vehicle[]) => {
    vehicles.value = nextVehicles
  }

  const vehicleCount = computed(() => vehicles.value.length)

  const vehiclesByStatus = computed(() => {
    return vehicles.value.reduce((acc, vehicle) => {
      if (!acc[vehicle.status]) {
        acc[vehicle.status] = []
      }
      acc[vehicle.status].push(vehicle)
      return acc
    }, {} as Record<VehicleStatus, Vehicle[]>)
  })

  const idleVehicles = computed(() => vehicles.value.filter((vehicle) => vehicle.status === 'IDLE'))
  const deliveringVehicles = computed(() => vehicles.value.filter((vehicle) => vehicle.status === 'DELIVERING'))
  const faultyVehicles = computed(() => vehicles.value.filter((vehicle) => vehicle.status === 'FAULTY'))

  const vehicleUtilization = computed(() => {
    if (vehicles.value.length === 0) {
      return 0
    }

    const activeVehicles = vehicles.value.filter((vehicle) => {
      return vehicle.status === 'DELIVERING' || vehicle.status === 'IDLE'
    }).length

    return (activeVehicles / vehicles.value.length) * 100
  })

  const fetchVehicles = async (status?: VehicleStatus) => {
    loading.value = true
    error.value = null

    try {
      const data = await getVehiclesApi(status)
      vehicles.value = data.map(normalizeVehicle)
      return vehicles.value
    } catch (err) {
      error.value = getErrorMessage(err, '加载车辆失败')
      reportError(err, '车辆加载失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const fetchVehicleById = async (id: number) => {
    loading.value = true
    error.value = null

    try {
      const data = await getVehicleByIdApi(id)
      const normalized = normalizeVehicle(data)
      updateVehicle(id, normalized)
      return normalized
    } catch (err) {
      error.value = getErrorMessage(err, '加载车辆详情失败')
      reportError(err, '车辆详情加载失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const addVehicle = (vehicle: Vehicle) => {
    vehicles.value.push(vehicle)
  }

  const updateVehicle = (id: number, updates: Partial<Vehicle>) => {
    const index = vehicles.value.findIndex((vehicle) => vehicle.id === id)

    if (index === -1) {
      if ('currentPosition' in updates && 'battery' in updates && 'speed' in updates && 'maxSpeed' in updates) {
        vehicles.value.unshift(updates as Vehicle)
      }
      return
    }

    vehicles.value[index] = { ...vehicles.value[index], ...updates }
  }

  const updateVehiclePosition = (id: number, x: number, y: number) => {
    const vehicle = vehicles.value.find((item) => item.id === id)
    if (vehicle) {
      vehicle.currentPosition = { x, y }
    }
  }

  const updateVehicleStatus = (id: number, status: VehicleStatus) => {
    const vehicle = vehicles.value.find((item) => item.id === id)
    if (vehicle) {
      vehicle.status = status
    }
  }

  const assignOrder = (vehicleId: number, orderId: number) => {
    const vehicle = vehicles.value.find((item) => item.id === vehicleId)
    if (vehicle) {
      vehicle.currentOrderId = orderId
      vehicle.status = 'DELIVERING'
    }
  }

  const completeOrder = (vehicleId: number) => {
    const vehicle = vehicles.value.find((item) => item.id === vehicleId)
    if (vehicle) {
      vehicle.currentOrderId = undefined
      vehicle.status = 'IDLE'
      vehicle.totalTasks += 1
    }
  }

  const setFaulty = (vehicleId: number) => {
    updateVehicleStatus(vehicleId, 'FAULTY')
  }

  const recover = (vehicleId: number) => {
    updateVehicleStatus(vehicleId, 'IDLE')
  }

  const triggerFault = async (vehicleId: number) => {
    loading.value = true
    error.value = null

    try {
      const data = await triggerFaultApi(vehicleId)
      const normalized = normalizeVehicle(data)
      updateVehicle(vehicleId, normalized)
      toastStore.warning('车辆故障已触发', `车辆 #${normalized.id} 已切换为故障状态`)
      return normalized
    } catch (err) {
      error.value = getErrorMessage(err, '触发故障失败')
      reportError(err, '触发故障失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const recoverVehicle = async (vehicleId: number) => {
    loading.value = true
    error.value = null

    try {
      const data = await recoverVehicleApi(vehicleId)
      const normalized = normalizeVehicle(data)
      updateVehicle(vehicleId, normalized)
      toastStore.success('车辆已恢复', `车辆 #${normalized.id} 已恢复可用`)
      return normalized
    } catch (err) {
      error.value = getErrorMessage(err, '恢复车辆失败')
      reportError(err, '恢复车辆失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const getVehicleById = (id: number) => {
    return vehicles.value.find((vehicle) => vehicle.id === id)
  }

  const clearVehicles = () => {
    vehicles.value = []
  }

  return {
    vehicles,
    loading,
    error,
    vehicleCount,
    vehiclesByStatus,
    idleVehicles,
    deliveringVehicles,
    faultyVehicles,
    vehicleUtilization,
    setVehicles,
    fetchVehicles,
    fetchVehicleById,
    addVehicle,
    updateVehicle,
    updateVehiclePosition,
    updateVehicleStatus,
    assignOrder,
    completeOrder,
    setFaulty,
    recover,
    triggerFault,
    recoverVehicle,
    getVehicleById,
    clearVehicles,
  }
})
