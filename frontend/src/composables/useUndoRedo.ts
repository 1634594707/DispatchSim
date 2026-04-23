import { ref, computed, type Ref } from 'vue'

/**
 * Represents a reversible operation that can be undone and redone
 */
export interface UndoableOperation<T> {
  /**
   * Execute the operation and return the new state
   */
  do: (state: T) => T
  
  /**
   * Undo the operation and return the previous state
   */
  undo: (state: T) => T
  
  /**
   * Human-readable description of the operation
   */
  description: string
}

/**
 * Options for configuring the undo/redo manager
 */
export interface UndoRedoOptions {
  /**
   * Maximum number of operations to keep in history
   * @default 50
   */
  maxStackSize?: number
}

/**
 * Undo/Redo manager for managing reversible operations
 */
export interface UndoRedoManager<T> {
  /**
   * Execute an operation and add it to the undo stack
   */
  execute: (operation: UndoableOperation<T>) => void
  
  /**
   * Undo the last operation
   */
  undo: () => void
  
  /**
   * Redo the last undone operation
   */
  redo: () => void
  
  /**
   * Whether undo is available
   */
  canUndo: Readonly<Ref<boolean>>
  
  /**
   * Whether redo is available
   */
  canRedo: Readonly<Ref<boolean>>
  
  /**
   * Clear all history
   */
  clear: () => void
  
  /**
   * Get the operation history (descriptions)
   */
  history: Readonly<Ref<string[]>>
  
  /**
   * Current state
   */
  state: Readonly<Ref<T>>
}

/**
 * Create an undo/redo manager for managing reversible operations
 * 
 * @param initialState - The initial state
 * @param options - Configuration options
 * @returns UndoRedoManager instance
 * 
 * @example
 * ```ts
 * interface RoadNetwork {
 *   nodes: Node[]
 *   edges: Edge[]
 * }
 * 
 * const undoRedo = useUndoRedo<RoadNetwork>({ nodes: [], edges: [] })
 * 
 * // Execute an operation
 * undoRedo.execute({
 *   do: (state) => ({
 *     ...state,
 *     nodes: [...state.nodes, newNode]
 *   }),
 *   undo: (state) => ({
 *     ...state,
 *     nodes: state.nodes.filter(n => n.id !== newNode.id)
 *   }),
 *   description: '添加节点'
 * })
 * 
 * // Undo the operation
 * undoRedo.undo()
 * 
 * // Redo the operation
 * undoRedo.redo()
 * ```
 */
export function useUndoRedo<T>(
  initialState: T,
  options: UndoRedoOptions = {}
): UndoRedoManager<T> {
  const { maxStackSize = 50 } = options
  
  // Current state
  const state = ref<T>(initialState) as Ref<T>
  
  // Undo stack (stores operations that can be undone)
  const undoStack = ref<UndoableOperation<T>[]>([])
  
  // Redo stack (stores operations that can be redone)
  const redoStack = ref<UndoableOperation<T>[]>([])
  
  // Computed properties
  const canUndo = computed(() => undoStack.value.length > 0)
  const canRedo = computed(() => redoStack.value.length > 0)
  
  const history = computed(() => {
    return undoStack.value.map(op => op.description)
  })
  
  /**
   * Execute an operation and add it to the undo stack
   */
  const execute = (operation: UndoableOperation<T>) => {
    // Execute the operation
    state.value = operation.do(state.value)
    
    // Add to undo stack
    undoStack.value.push(operation)
    
    // Limit stack size
    if (undoStack.value.length > maxStackSize) {
      undoStack.value.shift()
    }
    
    // Clear redo stack (new operation invalidates redo history)
    redoStack.value = []
  }
  
  /**
   * Undo the last operation
   */
  const undo = () => {
    if (!canUndo.value) {
      console.warn('No operations to undo')
      return
    }
    
    // Pop from undo stack
    const operation = undoStack.value.pop()!
    
    // Undo the operation
    state.value = operation.undo(state.value)
    
    // Add to redo stack
    redoStack.value.push(operation)
    
    // Limit redo stack size
    if (redoStack.value.length > maxStackSize) {
      redoStack.value.shift()
    }
  }
  
  /**
   * Redo the last undone operation
   */
  const redo = () => {
    if (!canRedo.value) {
      console.warn('No operations to redo')
      return
    }
    
    // Pop from redo stack
    const operation = redoStack.value.pop()!
    
    // Redo the operation
    state.value = operation.do(state.value)
    
    // Add back to undo stack
    undoStack.value.push(operation)
    
    // Limit undo stack size
    if (undoStack.value.length > maxStackSize) {
      undoStack.value.shift()
    }
  }
  
  /**
   * Clear all history
   */
  const clear = () => {
    undoStack.value = []
    redoStack.value = []
  }
  
  return {
    execute,
    undo,
    redo,
    canUndo,
    canRedo,
    clear,
    history,
    state: computed(() => state.value),
  }
}
