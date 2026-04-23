import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { createDepotApi, deleteDepotApi, getDepotsApi, updateDepotApi } from '@/api/depot'
import type { DepotDto } from '@/api/types'
import type { Depot, Position } from '@/types'
import { getErrorMessage, reportError } from '@/utils/errorHandler'

const normalizeDepot = (dto: DepotDto): Depot => ({
  id: Number(dto.id),
  name: dto.name,
  position: { x: dto.position.x, y: dto.position.y },
  icon: dto.icon ?? undefined,
  createdAt: dto.createdAt ? new Date(dto.createdAt) : undefined,
})

const calculateDistance = (pos1: Position, pos2: Position): number => {
  const dx = pos1.x - pos2.x
  const dy = pos1.y - pos2.y
  return Math.sqrt(dx * dx + dy * dy)
}

export const useDepotStore = defineStore('depot', () => {
  const depots = ref<Depot[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const depotCount = computed(() => depots.value.length)

  const fetchDepots = async () => {
    loading.value = true
    error.value = null

    try {
      depots.value = (await getDepotsApi()).map(normalizeDepot)
      return depots.value
    } catch (err) {
      error.value = getErrorMessage(err, '加载出货点失败')
      reportError(err, '加载出货点失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const createDepot = async (depot: Omit<Depot, 'id' | 'createdAt'>) => {
    loading.value = true
    error.value = null

    try {
      const created = normalizeDepot(
        await createDepotApi({
          name: depot.name,
          position: depot.position,
          icon: depot.icon,
        }),
      )
      depots.value.push(created)
      return created
    } catch (err) {
      error.value = getErrorMessage(err, '创建出货点失败')
      reportError(err, '创建出货点失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateDepot = async (id: number, updates: Partial<Depot>) => {
    loading.value = true
    error.value = null

    try {
      const current = depots.value.find((item) => item.id === id)
      if (!current) {
        return
      }
      const updated = normalizeDepot(
        await updateDepotApi(id, {
          name: updates.name ?? current.name,
          position: updates.position ?? current.position,
          icon: updates.icon ?? current.icon,
        }),
      )
      const index = depots.value.findIndex((item) => item.id === id)
      if (index !== -1) {
        depots.value[index] = { ...current, ...updated }
      }
    } catch (err) {
      error.value = getErrorMessage(err, '更新出货点失败')
      reportError(err, '更新出货点失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const deleteDepot = async (id: number) => {
    loading.value = true
    error.value = null

    try {
      await deleteDepotApi(id)
      const index = depots.value.findIndex((item) => item.id === id)
      if (index !== -1) {
        depots.value.splice(index, 1)
      }
    } catch (err) {
      error.value = getErrorMessage(err, '删除出货点失败')
      reportError(err, '删除出货点失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const findNearestDepot = (position: Position): Depot | null => {
    if (depots.value.length === 0) return null

    let nearest = depots.value[0]
    let minDistance = calculateDistance(position, nearest.position)

    for (const depot of depots.value) {
      const distance = calculateDistance(position, depot.position)
      if (distance < minDistance) {
        minDistance = distance
        nearest = depot
      }
    }

    return nearest
  }

  return {
    depots,
    loading,
    error,
    depotCount,
    fetchDepots,
    createDepot,
    updateDepot,
    deleteDepot,
    findNearestDepot,
  }
})
