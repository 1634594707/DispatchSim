/**
 * Throttle function - limits the rate at which a function can fire
 */
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  delay: number
): (...args: Parameters<T>) => void {
  let lastCall = 0
  let timeoutId: ReturnType<typeof setTimeout> | null = null

  return function (this: any, ...args: Parameters<T>) {
    const now = Date.now()
    const timeSinceLastCall = now - lastCall

    if (timeSinceLastCall >= delay) {
      lastCall = now
      func.apply(this, args)
    } else {
      if (timeoutId) {
        clearTimeout(timeoutId)
      }
      timeoutId = setTimeout(() => {
        lastCall = Date.now()
        func.apply(this, args)
      }, delay - timeSinceLastCall)
    }
  }
}

/**
 * Debounce function - delays execution until after wait time has elapsed
 */
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  delay: number
): (...args: Parameters<T>) => void {
  let timeoutId: ReturnType<typeof setTimeout> | null = null

  return function (this: any, ...args: Parameters<T>) {
    if (timeoutId) {
      clearTimeout(timeoutId)
    }
    timeoutId = setTimeout(() => {
      func.apply(this, args)
    }, delay)
  }
}

/**
 * Request animation frame with fallback
 */
export const requestAnimFrame = (() => {
  return (
    window.requestAnimationFrame ||
    ((callback: FrameRequestCallback) => window.setTimeout(callback, 1000 / 60))
  )
})()

/**
 * Cancel animation frame with fallback
 */
export const cancelAnimFrame = (() => {
  return (
    window.cancelAnimationFrame ||
    ((id: number) => window.clearTimeout(id))
  )
})()

/**
 * Check if element is in viewport
 */
export function isInViewport(element: HTMLElement): boolean {
  const rect = element.getBoundingClientRect()
  return (
    rect.top >= 0 &&
    rect.left >= 0 &&
    rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
    rect.right <= (window.innerWidth || document.documentElement.clientWidth)
  )
}

/**
 * Get device pixel ratio
 */
export function getDevicePixelRatio(): number {
  return window.devicePixelRatio || 1
}

/**
 * Measure performance
 */
export class PerformanceMonitor {
  private startTime: number = 0
  private marks: Map<string, number> = new Map()

  start() {
    this.startTime = performance.now()
  }

  mark(name: string) {
    this.marks.set(name, performance.now())
  }

  measure(name: string): number {
    const markTime = this.marks.get(name)
    if (!markTime) return 0
    return markTime - this.startTime
  }

  end(): number {
    return performance.now() - this.startTime
  }

  clear() {
    this.startTime = 0
    this.marks.clear()
  }
}

/**
 * FPS Counter
 */
export class FPSCounter {
  private frames: number[] = []
  private lastTime: number = performance.now()

  update(): number {
    const now = performance.now()
    const delta = now - this.lastTime
    this.lastTime = now

    this.frames.push(1000 / delta)
    if (this.frames.length > 60) {
      this.frames.shift()
    }

    return this.getAverage()
  }

  getAverage(): number {
    if (this.frames.length === 0) return 0
    const sum = this.frames.reduce((a, b) => a + b, 0)
    return Math.round(sum / this.frames.length)
  }

  reset() {
    this.frames = []
    this.lastTime = performance.now()
  }
}
