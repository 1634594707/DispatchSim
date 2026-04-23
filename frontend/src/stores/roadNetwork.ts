import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useUndoRedo, type UndoableOperation } from '@/composables/useUndoRedo'
import type { Position } from '@/types'

export interface RoadNode {
  id: number
  position: Position
  type: 'intersection' | 'depot' | 'poi'
  metadata?: Record<string, any>
  selected?: boolean
}

export interface RoadEdge {
  id: number
  fromNodeId: number
  toNodeId: number
  bidirectional: boolean
  weight: number
  metadata?: Record<string, any>
  selected?: boolean
}

export interface RoadNetwork {
  nodes: RoadNode[]
  edges: RoadEdge[]
  version: number
  updatedAt?: string
}

export interface GridGeneratorOptions {
  rows: number
  cols: number
  spacing: number
  offsetX: number
  offsetY: number
  bidirectional: boolean
}

const cloneNetwork = (network: RoadNetwork): RoadNetwork => ({
  ...network,
  nodes: network.nodes.map((node) => ({
    ...node,
    position: { ...node.position },
    metadata: node.metadata ? { ...node.metadata } : undefined,
  })),
  edges: network.edges.map((edge) => ({
    ...edge,
    metadata: edge.metadata ? { ...edge.metadata } : undefined,
  })),
})

const calculateDistance = (from: Position, to: Position) => {
  const dx = to.x - from.x
  const dy = to.y - from.y
  return Math.sqrt(dx * dx + dy * dy)
}

const defaultRoadNodes: RoadNode[] = [
  { id: 1, position: { x: 60, y: 400 }, type: 'poi', metadata: { label: 'West Gate' } },
  { id: 2, position: { x: 140, y: 400 }, type: 'intersection', metadata: { label: 'Gate Plaza' } },
  { id: 3, position: { x: 240, y: 400 }, type: 'intersection', metadata: { label: 'West Yard West' } },
  { id: 4, position: { x: 360, y: 400 }, type: 'intersection', metadata: { label: 'West Yard East' } },
  { id: 5, position: { x: 480, y: 400 }, type: 'intersection', metadata: { label: 'Campus East Junction' } },
  { id: 6, position: { x: 240, y: 560 }, type: 'depot', metadata: { label: 'Warehouse A' } },
  { id: 7, position: { x: 360, y: 560 }, type: 'depot', metadata: { label: 'Warehouse B' } },
  { id: 8, position: { x: 240, y: 240 }, type: 'poi', metadata: { label: 'Parking Court' } },
  { id: 9, position: { x: 360, y: 240 }, type: 'poi', metadata: { label: 'Admin / Workshop' } },
  { id: 10, position: { x: 480, y: 560 }, type: 'depot', metadata: { label: 'Cross Dock' } },
  { id: 11, position: { x: 620, y: 400 }, type: 'intersection', metadata: { label: 'Connector West' } },
  { id: 12, position: { x: 760, y: 400 }, type: 'intersection', metadata: { label: 'Connector Mid' } },
  { id: 13, position: { x: 900, y: 400 }, type: 'intersection', metadata: { label: 'Connector East' } },
  { id: 14, position: { x: 1040, y: 400 }, type: 'intersection', metadata: { label: 'Remote Hub Core' } },
  { id: 15, position: { x: 1040, y: 560 }, type: 'depot', metadata: { label: 'Remote Delivery Hub' } },
  { id: 16, position: { x: 1040, y: 240 }, type: 'poi', metadata: { label: 'Retail Cluster' } },
  { id: 17, position: { x: 900, y: 560 }, type: 'poi', metadata: { label: 'East Logistics Park' } },
  { id: 18, position: { x: 900, y: 240 }, type: 'poi', metadata: { label: 'Residential District' } },
  { id: 19, position: { x: 1120, y: 560 }, type: 'poi', metadata: { label: 'Terminal Court' } },
  { id: 20, position: { x: 1120, y: 240 }, type: 'poi', metadata: { label: 'Commercial Strip' } },
]

const nodeMap = new Map(defaultRoadNodes.map((node) => [node.id, node]))

const createDefaultEdge = (id: number, fromNodeId: number, toNodeId: number, bidirectional = true): RoadEdge => {
  const fromNode = nodeMap.get(fromNodeId)
  const toNode = nodeMap.get(toNodeId)
  if (!fromNode || !toNode) {
    throw new Error(`Invalid edge ${id}: missing nodes ${fromNodeId} or ${toNodeId}`)
  }

  return {
    id,
    fromNodeId,
    toNodeId,
    bidirectional,
    weight: calculateDistance(fromNode.position, toNode.position),
  }
}

const defaultRoadEdges: RoadEdge[] = [
  createDefaultEdge(1, 1, 2),
  createDefaultEdge(2, 2, 3),
  createDefaultEdge(3, 3, 4),
  createDefaultEdge(4, 4, 5),
  createDefaultEdge(5, 3, 6),
  createDefaultEdge(6, 4, 7),
  createDefaultEdge(7, 3, 8),
  createDefaultEdge(8, 4, 9),
  createDefaultEdge(9, 5, 10),
  createDefaultEdge(10, 6, 7),
  createDefaultEdge(11, 8, 9),
  createDefaultEdge(12, 6, 8),
  createDefaultEdge(13, 7, 9),
  createDefaultEdge(14, 5, 11),
  createDefaultEdge(15, 11, 12),
  createDefaultEdge(16, 12, 13),
  createDefaultEdge(17, 13, 14),
  createDefaultEdge(18, 13, 17),
  createDefaultEdge(19, 13, 18),
  createDefaultEdge(20, 14, 15),
  createDefaultEdge(21, 14, 16),
  createDefaultEdge(22, 15, 19),
  createDefaultEdge(23, 16, 20),
  createDefaultEdge(24, 17, 15),
  createDefaultEdge(25, 18, 16),
  createDefaultEdge(26, 17, 18),
  createDefaultEdge(27, 19, 20),
]

const createDefaultRoadNetwork = (): RoadNetwork => ({
  nodes: defaultRoadNodes.map((node) => ({
    ...node,
    position: { ...node.position },
    metadata: node.metadata ? { ...node.metadata } : undefined,
  })),
  edges: defaultRoadEdges.map((edge) => ({
    ...edge,
    metadata: edge.metadata ? { ...edge.metadata } : undefined,
  })),
  version: 1,
  updatedAt: new Date().toISOString(),
})

const getNextIds = (network: RoadNetwork) => ({
  nodeId: Math.max(0, ...network.nodes.map((node) => node.id)) + 1,
  edgeId: Math.max(0, ...network.edges.map((edge) => edge.id)) + 1,
})

export const useRoadNetworkStore = defineStore('roadNetwork', () => {
  const initialNetwork = createDefaultRoadNetwork()

  const network = ref<RoadNetwork>(cloneNetwork(initialNetwork))
  const initialIds = getNextIds(network.value)

  const nextNodeId = ref(initialIds.nodeId)
  const nextEdgeId = ref(initialIds.edgeId)
  const editorMode = ref<'view' | 'add-node' | 'add-edge' | 'select' | 'move' | 'delete' | 'add-vehicle' | 'move-vehicle' | 'delete-vehicle'>('view')
  const selectedNodeIds = ref<Set<number>>(new Set())
  const selectedEdgeIds = ref<Set<number>>(new Set())
  const tempEdgeStart = ref<number | null>(null)

  const undoRedo = useUndoRedo<RoadNetwork>(cloneNetwork(network.value), { maxStackSize: 50 })

  const syncFromUndoRedo = () => {
    network.value = cloneNetwork(undoRedo.state.value)
    const ids = getNextIds(network.value)
    nextNodeId.value = ids.nodeId
    nextEdgeId.value = ids.edgeId
  }

  const nodeCount = computed(() => network.value.nodes.length)
  const edgeCount = computed(() => network.value.edges.length)
  const selectedCount = computed(() => selectedNodeIds.value.size + selectedEdgeIds.value.size)
  const selectedNodes = computed(() => network.value.nodes.filter((node) => selectedNodeIds.value.has(node.id)))
  const selectedEdges = computed(() => network.value.edges.filter((edge) => selectedEdgeIds.value.has(edge.id)))

  const addNode = (position: Position, type: RoadNode['type'] = 'intersection') => {
    const newNode: RoadNode = {
      id: nextNodeId.value++,
      position: { ...position },
      type,
    }

    const operation: UndoableOperation<RoadNetwork> = {
      do: (state) => ({
        ...state,
        nodes: [...state.nodes, newNode],
        version: state.version + 1,
      }),
      undo: (state) => ({
        ...state,
        nodes: state.nodes.filter((node) => node.id !== newNode.id),
        version: state.version + 1,
      }),
      description: `Add node #${newNode.id}`,
    }

    undoRedo.execute(operation)
    syncFromUndoRedo()
    return newNode
  }

  const addEdge = (fromNodeId: number, toNodeId: number, bidirectional = true) => {
    const fromNode = network.value.nodes.find((node) => node.id === fromNodeId)
    const toNode = network.value.nodes.find((node) => node.id === toNodeId)

    if (!fromNode || !toNode) {
      console.error('Cannot create edge: one or both nodes do not exist')
      return null
    }

    const edgeExists = network.value.edges.some(
      (edge) =>
        (edge.fromNodeId === fromNodeId && edge.toNodeId === toNodeId) ||
        (edge.bidirectional && edge.fromNodeId === toNodeId && edge.toNodeId === fromNodeId)
    )
    if (edgeExists) {
      console.warn('Edge already exists between these nodes')
      return null
    }

    const newEdge: RoadEdge = {
      id: nextEdgeId.value++,
      fromNodeId,
      toNodeId,
      bidirectional,
      weight: calculateDistance(fromNode.position, toNode.position),
    }

    const operation: UndoableOperation<RoadNetwork> = {
      do: (state) => ({
        ...state,
        edges: [...state.edges, newEdge],
        version: state.version + 1,
      }),
      undo: (state) => ({
        ...state,
        edges: state.edges.filter((edge) => edge.id !== newEdge.id),
        version: state.version + 1,
      }),
      description: `Add edge #${newEdge.id}`,
    }

    undoRedo.execute(operation)
    syncFromUndoRedo()
    return newEdge
  }

  const deleteSelected = () => {
    if (selectedCount.value === 0) return

    const nodesToDelete = Array.from(selectedNodeIds.value)
    const edgesToDelete = Array.from(selectedEdgeIds.value)
    const connectedEdges = network.value.edges
      .filter((edge) => nodesToDelete.includes(edge.fromNodeId) || nodesToDelete.includes(edge.toNodeId))
      .map((edge) => edge.id)

    const allEdgesToDelete = new Set([...edgesToDelete, ...connectedEdges])
    const deletedNodes = network.value.nodes.filter((node) => nodesToDelete.includes(node.id))
    const deletedEdges = network.value.edges.filter((edge) => allEdgesToDelete.has(edge.id))

    const operation: UndoableOperation<RoadNetwork> = {
      do: (state) => ({
        ...state,
        nodes: state.nodes.filter((node) => !nodesToDelete.includes(node.id)),
        edges: state.edges.filter((edge) => !allEdgesToDelete.has(edge.id)),
        version: state.version + 1,
      }),
      undo: (state) => ({
        ...state,
        nodes: [...state.nodes, ...deletedNodes],
        edges: [...state.edges, ...deletedEdges],
        version: state.version + 1,
      }),
      description: `Delete ${nodesToDelete.length} nodes and ${allEdgesToDelete.size} edges`,
    }

    undoRedo.execute(operation)
    syncFromUndoRedo()
    clearSelection()
  }

  const moveNode = (nodeId: number, newPosition: Position) => {
    const node = network.value.nodes.find((item) => item.id === nodeId)
    if (!node) return

    const oldPosition = { ...node.position }

    const updateWeights = (state: RoadNetwork, movedPosition: Position) =>
      state.edges.map((edge) => {
        if (edge.fromNodeId !== nodeId && edge.toNodeId !== nodeId) return edge

        const fromNode = state.nodes.find((item) => item.id === edge.fromNodeId)
        const toNode = state.nodes.find((item) => item.id === edge.toNodeId)
        if (!fromNode || !toNode) return edge

        const from = edge.fromNodeId === nodeId ? movedPosition : fromNode.position
        const to = edge.toNodeId === nodeId ? movedPosition : toNode.position
        return { ...edge, weight: calculateDistance(from, to) }
      })

    const operation: UndoableOperation<RoadNetwork> = {
      do: (state) => ({
        ...state,
        nodes: state.nodes.map((item) => (item.id === nodeId ? { ...item, position: { ...newPosition } } : item)),
        edges: updateWeights(state, newPosition),
        version: state.version + 1,
      }),
      undo: (state) => ({
        ...state,
        nodes: state.nodes.map((item) => (item.id === nodeId ? { ...item, position: { ...oldPosition } } : item)),
        edges: updateWeights(state, oldPosition),
        version: state.version + 1,
      }),
      description: `Move node #${nodeId}`,
    }

    undoRedo.execute(operation)
    syncFromUndoRedo()
  }

  const generateGrid = (options: GridGeneratorOptions) => {
    const { rows, cols, spacing, offsetX, offsetY, bidirectional } = options
    const newNodes: RoadNode[] = []
    const newEdges: RoadEdge[] = []

    for (let row = 0; row < rows; row++) {
      for (let col = 0; col < cols; col++) {
        newNodes.push({
          id: nextNodeId.value++,
          position: {
            x: offsetX + col * spacing,
            y: offsetY + row * spacing,
          },
          type: 'intersection',
        })
      }
    }

    for (let row = 0; row < rows; row++) {
      for (let col = 0; col < cols; col++) {
        const currentIndex = row * cols + col
        const currentNode = newNodes[currentIndex]

        if (col < cols - 1) {
          const rightNode = newNodes[row * cols + col + 1]
          newEdges.push({
            id: nextEdgeId.value++,
            fromNodeId: currentNode.id,
            toNodeId: rightNode.id,
            bidirectional,
            weight: spacing,
          })
        }

        if (row < rows - 1) {
          const bottomNode = newNodes[(row + 1) * cols + col]
          newEdges.push({
            id: nextEdgeId.value++,
            fromNodeId: currentNode.id,
            toNodeId: bottomNode.id,
            bidirectional,
            weight: spacing,
          })
        }
      }
    }

    const oldNetwork = cloneNetwork(network.value)
    const operation: UndoableOperation<RoadNetwork> = {
      do: (state) => ({
        nodes: [...state.nodes, ...newNodes],
        edges: [...state.edges, ...newEdges],
        version: state.version + 1,
      }),
      undo: () => oldNetwork,
      description: `Generate ${rows}x${cols} grid`,
    }

    undoRedo.execute(operation)
    syncFromUndoRedo()
  }

  const clearNetwork = () => {
    const oldNetwork = cloneNetwork(network.value)
    const operation: UndoableOperation<RoadNetwork> = {
      do: () => ({
        nodes: [],
        edges: [],
        version: 1,
      }),
      undo: () => oldNetwork,
      description: 'Clear road network',
    }

    undoRedo.execute(operation)
    syncFromUndoRedo()
    clearSelection()
  }

  const replaceNetwork = (nextNetwork: RoadNetwork) => {
    const replacement = cloneNetwork(nextNetwork)
    const previous = cloneNetwork(network.value)
    const operation: UndoableOperation<RoadNetwork> = {
      do: () => cloneNetwork(replacement),
      undo: () => previous,
      description: `Replace network (${replacement.nodes.length} nodes, ${replacement.edges.length} edges)`,
    }

    undoRedo.execute(operation)
    syncFromUndoRedo()
    clearSelection()
    tempEdgeStart.value = null
  }

  const resetToDefaultNetwork = () => {
    const oldNetwork = cloneNetwork(network.value)
    const defaultNetwork = createDefaultRoadNetwork()

    const operation: UndoableOperation<RoadNetwork> = {
      do: () => cloneNetwork(defaultNetwork),
      undo: () => oldNetwork,
      description: 'Reset to default regional network',
    }

    undoRedo.execute(operation)
    syncFromUndoRedo()
    clearSelection()
  }

  const selectNode = (nodeId: number, addToSelection = false) => {
    if (!addToSelection) {
      selectedNodeIds.value.clear()
      selectedEdgeIds.value.clear()
    }
    selectedNodeIds.value.add(nodeId)
  }

  const selectEdge = (edgeId: number, addToSelection = false) => {
    if (!addToSelection) {
      selectedNodeIds.value.clear()
      selectedEdgeIds.value.clear()
    }
    selectedEdgeIds.value.add(edgeId)
  }

  const toggleNodeSelection = (nodeId: number) => {
    if (selectedNodeIds.value.has(nodeId)) selectedNodeIds.value.delete(nodeId)
    else selectedNodeIds.value.add(nodeId)
  }

  const toggleEdgeSelection = (edgeId: number) => {
    if (selectedEdgeIds.value.has(edgeId)) selectedEdgeIds.value.delete(edgeId)
    else selectedEdgeIds.value.add(edgeId)
  }

  const clearSelection = () => {
    selectedNodeIds.value.clear()
    selectedEdgeIds.value.clear()
  }

  const setEditorMode = (mode: typeof editorMode.value) => {
    editorMode.value = mode
    tempEdgeStart.value = null
  }

  const setTempEdgeStart = (nodeId: number | null) => {
    tempEdgeStart.value = nodeId
  }

  const bulkSelect = (nodeIds: number[], edgeIds: number[], addToSelection = false) => {
    if (!addToSelection) {
      selectedNodeIds.value.clear()
      selectedEdgeIds.value.clear()
    }
    nodeIds.forEach((id) => selectedNodeIds.value.add(id))
    edgeIds.forEach((id) => selectedEdgeIds.value.add(id))
  }

  const importFromGeoJSON = (geojson: any) => {
    if (!geojson || geojson.type !== 'FeatureCollection' || !Array.isArray(geojson.features)) {
      console.error('Invalid GeoJSON: expected FeatureCollection')
      return
    }

    const nodeIdMap = new Map<number, number>()
    const newNodes: RoadNode[] = []
    const newEdges: RoadEdge[] = []

    for (const feature of geojson.features) {
      if (feature.geometry?.type !== 'Point') continue

      const [x, y] = feature.geometry.coordinates
      const oldId = feature.properties?.id
      const newId = nextNodeId.value++
      if (oldId !== undefined) nodeIdMap.set(oldId, newId)

      newNodes.push({
        id: newId,
        position: { x, y },
        type: feature.properties?.type || 'intersection',
      })
    }

    for (const feature of geojson.features) {
      if (feature.geometry?.type !== 'LineString') continue

      const oldFromId = feature.properties?.fromNodeId
      const oldToId = feature.properties?.toNodeId
      const fromNodeId = nodeIdMap.get(oldFromId) ?? oldFromId
      const toNodeId = nodeIdMap.get(oldToId) ?? oldToId
      const fromNode = [...network.value.nodes, ...newNodes].find((node) => node.id === fromNodeId)
      const toNode = [...network.value.nodes, ...newNodes].find((node) => node.id === toNodeId)
      if (!fromNode || !toNode) continue

      let weight = feature.properties?.weight
      if (!weight) {
        weight = calculateDistance(fromNode.position, toNode.position)
      }

      newEdges.push({
        id: nextEdgeId.value++,
        fromNodeId,
        toNodeId,
        bidirectional: feature.properties?.bidirectional ?? true,
        weight,
      })
    }

    const oldNetwork = cloneNetwork(network.value)
    const operation: UndoableOperation<RoadNetwork> = {
      do: (state) => ({
        ...state,
        nodes: [...state.nodes, ...newNodes],
        edges: [...state.edges, ...newEdges],
        version: state.version + 1,
      }),
      undo: () => oldNetwork,
      description: `Import GeoJSON (${newNodes.length} nodes, ${newEdges.length} edges)`,
    }

    undoRedo.execute(operation)
    syncFromUndoRedo()
  }

  const exportToGeoJSON = () => ({
    type: 'FeatureCollection',
    features: [
      ...network.value.nodes.map((node) => ({
        type: 'Feature',
        geometry: {
          type: 'Point',
          coordinates: [node.position.x, node.position.y],
        },
        properties: {
          id: node.id,
          type: node.type,
          ...node.metadata,
        },
      })),
      ...network.value.edges
        .map((edge) => {
          const fromNode = network.value.nodes.find((node) => node.id === edge.fromNodeId)
          const toNode = network.value.nodes.find((node) => node.id === edge.toNodeId)
          if (!fromNode || !toNode) return null

          return {
            type: 'Feature',
            geometry: {
              type: 'LineString',
              coordinates: [
                [fromNode.position.x, fromNode.position.y],
                [toNode.position.x, toNode.position.y],
              ],
            },
            properties: {
              id: edge.id,
              fromNodeId: edge.fromNodeId,
              toNodeId: edge.toNodeId,
              bidirectional: edge.bidirectional,
              weight: edge.weight,
              ...edge.metadata,
            },
          }
        })
        .filter(Boolean),
    ],
  })

  const undo = () => {
    undoRedo.undo()
    syncFromUndoRedo()
  }

  const redo = () => {
    undoRedo.redo()
    syncFromUndoRedo()
  }

  return {
    network,
    editorMode,
    selectedNodeIds,
    selectedEdgeIds,
    tempEdgeStart,
    nodeCount,
    edgeCount,
    selectedCount,
    selectedNodes,
    selectedEdges,
    canUndo: undoRedo.canUndo,
    canRedo: undoRedo.canRedo,
    history: undoRedo.history,
    addNode,
    addEdge,
    deleteSelected,
    moveNode,
    generateGrid,
    clearNetwork,
    replaceNetwork,
    resetToDefaultNetwork,
    selectNode,
    selectEdge,
    toggleNodeSelection,
    toggleEdgeSelection,
    clearSelection,
    setEditorMode,
    setTempEdgeStart,
    bulkSelect,
    importFromGeoJSON,
    exportToGeoJSON,
    undo,
    redo,
  }
})
