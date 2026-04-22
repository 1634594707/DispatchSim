import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { DispatchStrategy, SimulationStatus } from '@/types'
import { useOrderStore } from './order'
import { useStatisticsStore } from './statistics'
import { useVehicleStore } from './vehicle'
import {
  createBatchOrdersApi,
  getSimulationStatusApi,
  pauseSimulationApi,
  resumeSimulationApi,
  startSimulationApi,
  stopSimulationApi,
  tickSimulationApi,
  updateSimulationStrategyApi,
} from '@/api/simulation'
import { getReplaySessionsApi, replayControlApi } from '@/api/replay'
import type { BatchOrderRequestDto, ReplayFrameDto, ReplaySessionDto } from '@/api/types'
import { getErrorMessage, reportError } from '@/utils/errorHandler'
import { useToastStore } from './toast'
import { normalizeOrder, normalizeVehicle } from '@/api/types'

export const useSimulationStore = defineStore('simulation', () => {
  const status = ref<SimulationStatus>('STOPPED')
  const strategy = ref<DispatchStrategy>('NEAREST_FIRST')
  const sessionId = ref<string | null>(null)
  const elapsedTime = ref(0)
  const speed = ref(1)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const replaySessions = ref<ReplaySessionDto[]>([])
  const replaySessionId = ref<string | null>(null)
  const replayProgress = ref(1)
  const replayPlaying = ref(false)
  const replayFrame = ref<ReplayFrameDto | null>(null)
  const toastStore = useToastStore()
  const orderStore = useOrderStore()
  const vehicleStore = useVehicleStore()
  const statisticsStore = useStatisticsStore()

  const isRunning = computed(() => status.value === 'RUNNING')
  const isPaused = computed(() => status.value === 'PAUSED')
  const isStopped = computed(() => status.value === 'STOPPED')

  const formattedTime = computed(() => {
    const hours = Math.floor(elapsedTime.value / 3600)
    const minutes = Math.floor((elapsedTime.value % 3600) / 60)
    const seconds = elapsedTime.value % 60

    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  })

  const syncState = (nextState: { status: SimulationStatus; strategy: DispatchStrategy; sessionId?: string; elapsedTime: number }) => {
    status.value = nextState.status
    strategy.value = nextState.strategy
    sessionId.value = nextState.sessionId ?? null
    elapsedTime.value = nextState.elapsedTime
  }

  const refreshRelatedData = async () => {
    await Promise.allSettled([
      orderStore.fetchOrders(),
      vehicleStore.fetchVehicles(),
      statisticsStore.fetchStatistics(),
    ])
  }

  const fetchStatus = async () => {
    loading.value = true
    error.value = null

    try {
      const nextState = await getSimulationStatusApi()
      syncState(nextState)
      return nextState
    } catch (err) {
      error.value = getErrorMessage(err, '加载仿真状态失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const start = async () => {
    loading.value = true
    error.value = null

    try {
      await startSimulationApi()
      await fetchStatus()
      await refreshRelatedData()
      toastStore.success('仿真已启动')
    } catch (err) {
      error.value = getErrorMessage(err, '启动仿真失败')
      reportError(err, '启动仿真失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const pause = async () => {
    loading.value = true
    error.value = null

    try {
      await pauseSimulationApi()
      await fetchStatus()
      await refreshRelatedData()
      toastStore.info('仿真已暂停')
    } catch (err) {
      error.value = getErrorMessage(err, '暂停仿真失败')
      reportError(err, '暂停仿真失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const resume = async () => {
    loading.value = true
    error.value = null

    try {
      await resumeSimulationApi()
      await fetchStatus()
      await refreshRelatedData()
      toastStore.success('仿真已恢复')
    } catch (err) {
      error.value = getErrorMessage(err, '恢复仿真失败')
      reportError(err, '恢复仿真失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const stop = async () => {
    loading.value = true
    error.value = null

    try {
      await stopSimulationApi()
      await fetchStatus()
      await refreshRelatedData()
      toastStore.info('仿真已停止')
    } catch (err) {
      error.value = getErrorMessage(err, '停止仿真失败')
      reportError(err, '停止仿真失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const step = async () => {
    loading.value = true
    error.value = null

    try {
      await tickSimulationApi()
      await fetchStatus()
      await refreshRelatedData()
      toastStore.info('已执行单步仿真')
    } catch (err) {
      error.value = getErrorMessage(err, '单步执行失败')
      reportError(err, '单步执行失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const setStrategy = async (newStrategy: DispatchStrategy) => {
    strategy.value = newStrategy
    loading.value = true
    error.value = null

    try {
      await updateSimulationStrategyApi(newStrategy)
      await fetchStatus()
      await refreshRelatedData()
      toastStore.info('调度策略已切换', `当前策略：${newStrategy}`)
    } catch (err) {
      error.value = getErrorMessage(err, '切换策略失败')
      reportError(err, '切换策略失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const setSpeed = (newSpeed: number) => {
    speed.value = newSpeed
  }

  const fetchReplaySessions = async () => {
    try {
      const sessions = await getReplaySessionsApi()
      replaySessions.value = sessions
      if (!replaySessionId.value && sessions.length > 0) {
        replaySessionId.value = sessions[0].sessionId
      }
      return sessions
    } catch (err) {
      error.value = getErrorMessage(err, '加载历史会话失败')
      throw err
    }
  }

  const applyReplayFrame = (frame: ReplayFrameDto) => {
    replayFrame.value = frame
    replayProgress.value = frame.progress
    orderStore.setOrders(frame.orders.map(normalizeOrder))
    vehicleStore.setVehicles(frame.vehicles.map(normalizeVehicle))
  }

  const controlReplay = async (
    action: 'PLAY' | 'PAUSE' | 'SEEK' | 'SPEED' | 'STEP',
    overrides?: Partial<{ progress: number; speed: number; step: number; sessionId: string }>,
  ) => {
    const targetSessionId = overrides?.sessionId ?? replaySessionId.value
    if (!targetSessionId) {
      return null
    }
    loading.value = true
    error.value = null
    try {
      const frame = await replayControlApi({
        sessionId: targetSessionId,
        action,
        progress: overrides?.progress ?? replayProgress.value,
        speed: overrides?.speed ?? speed.value,
        step: overrides?.step ?? 1,
      })
      replaySessionId.value = targetSessionId
      applyReplayFrame(frame)
      return frame
    } catch (err) {
      error.value = getErrorMessage(err, '加载回放帧失败')
      reportError(err, '加载回放帧失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const selectReplaySession = async (nextSessionId: string) => {
    replaySessionId.value = nextSessionId
    replayPlaying.value = false
    replayProgress.value = 1
    return controlReplay('SEEK', { sessionId: nextSessionId, progress: 1 })
  }

  const setReplayProgress = async (nextProgress: number) => {
    replayPlaying.value = false
    replayProgress.value = nextProgress
    return controlReplay('SEEK', { progress: nextProgress })
  }

  const toggleReplay = async () => {
    replayPlaying.value = !replayPlaying.value
    return controlReplay(replayPlaying.value ? 'PLAY' : 'PAUSE')
  }

  const stopReplayPlayback = () => {
    replayPlaying.value = false
  }

  const stepReplay = async () => {
    replayPlaying.value = false
    const totalEvents = replayFrame.value?.totalEvents ?? 1
    const nextProgress = Math.min(1, replayProgress.value + 1 / totalEvents)
    replayProgress.value = nextProgress
    return controlReplay('STEP', { progress: nextProgress, step: 1 })
  }

  const createBatchOrders = async (payload: BatchOrderRequestDto) => {
    loading.value = true
    error.value = null

    try {
      const result = await createBatchOrdersApi(payload)
      await fetchStatus()
      await refreshRelatedData()
      toastStore.success('批量订单已生成', `共创建 ${result.totalOrders} 个订单`)
      return result
    } catch (err) {
      error.value = getErrorMessage(err, '批量订单生成失败')
      reportError(err, '批量订单生成失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    status,
    strategy,
    sessionId,
    elapsedTime,
    speed,
    loading,
    error,
    replaySessions,
    replaySessionId,
    replayProgress,
    replayPlaying,
    replayFrame,
    isRunning,
    isPaused,
    isStopped,
    formattedTime,
    syncState,
    fetchStatus,
    start,
    pause,
    resume,
    stop,
    step,
    setStrategy,
    setSpeed,
    createBatchOrders,
    fetchReplaySessions,
    selectReplaySession,
    setReplayProgress,
    toggleReplay,
    stopReplayPlayback,
    stepReplay,
    controlReplay,
  }
})
