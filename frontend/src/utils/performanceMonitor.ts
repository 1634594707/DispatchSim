/**
 * Performance monitoring utilities
 */

export class PerformanceMonitor {
  private metrics: Map<string, number[]> = new Map()
  private startTimes: Map<string, number> = new Map()

  /**
   * Start measuring a metric
   */
  start(name: string): void {
    this.startTimes.set(name, performance.now())
  }

  /**
   * End measuring and record the metric
   */
  end(name: string): number {
    const startTime = this.startTimes.get(name)
    if (!startTime) {
      console.warn(`No start time found for metric: ${name}`)
      return 0
    }

    const duration = performance.now() - startTime
    this.startTimes.delete(name)

    // Store metric
    if (!this.metrics.has(name)) {
      this.metrics.set(name, [])
    }
    this.metrics.get(name)!.push(duration)

    return duration
  }

  /**
   * Get average duration for a metric
   */
  getAverage(name: string): number {
    const values = this.metrics.get(name)
    if (!values || values.length === 0) return 0

    const sum = values.reduce((a, b) => a + b, 0)
    return sum / values.length
  }

  /**
   * Get all metrics
   */
  getAllMetrics(): Record<string, { average: number; count: number; total: number }> {
    const result: Record<string, { average: number; count: number; total: number }> = {}

    this.metrics.forEach((values, name) => {
      const total = values.reduce((a, b) => a + b, 0)
      result[name] = {
        average: total / values.length,
        count: values.length,
        total
      }
    })

    return result
  }

  /**
   * Clear all metrics
   */
  clear(): void {
    this.metrics.clear()
    this.startTimes.clear()
  }

  /**
   * Log metrics to console
   */
  log(): void {
    const metrics = this.getAllMetrics()
    console.table(metrics)
  }
}

// Global instance
export const performanceMonitor = new PerformanceMonitor()

/**
 * Measure function execution time
 */
export function measureAsync<T>(
  name: string,
  fn: () => Promise<T>
): Promise<T> {
  performanceMonitor.start(name)
  return fn().finally(() => {
    performanceMonitor.end(name)
  })
}

/**
 * Measure sync function execution time
 */
export function measure<T>(name: string, fn: () => T): T {
  performanceMonitor.start(name)
  try {
    return fn()
  } finally {
    performanceMonitor.end(name)
  }
}

/**
 * Report Web Vitals
 */
export function reportWebVitals(): void {
  if ('PerformanceObserver' in window) {
    // Largest Contentful Paint (LCP)
    const lcpObserver = new PerformanceObserver((list) => {
      const entries = list.getEntries()
      const lastEntry = entries[entries.length - 1]
      console.log('LCP:', lastEntry.startTime)
    })
    lcpObserver.observe({ entryTypes: ['largest-contentful-paint'] })

    // First Input Delay (FID)
    const fidObserver = new PerformanceObserver((list) => {
      const entries = list.getEntries()
      entries.forEach((entry: any) => {
        console.log('FID:', entry.processingStart - entry.startTime)
      })
    })
    fidObserver.observe({ entryTypes: ['first-input'] })

    // Cumulative Layout Shift (CLS)
    let clsScore = 0
    const clsObserver = new PerformanceObserver((list) => {
      const entries = list.getEntries()
      entries.forEach((entry: any) => {
        if (!entry.hadRecentInput) {
          clsScore += entry.value
        }
      })
      console.log('CLS:', clsScore)
    })
    clsObserver.observe({ entryTypes: ['layout-shift'] })
  }
}

/**
 * Memory usage monitoring
 */
export function getMemoryUsage(): {
  usedJSHeapSize: number
  totalJSHeapSize: number
  jsHeapSizeLimit: number
} | null {
  if ('memory' in performance) {
    const memory = (performance as any).memory
    return {
      usedJSHeapSize: memory.usedJSHeapSize,
      totalJSHeapSize: memory.totalJSHeapSize,
      jsHeapSizeLimit: memory.jsHeapSizeLimit
    }
  }
  return null
}

/**
 * Log memory usage
 */
export function logMemoryUsage(): void {
  const memory = getMemoryUsage()
  if (memory) {
    console.log('Memory Usage:', {
      used: `${(memory.usedJSHeapSize / 1024 / 1024).toFixed(2)} MB`,
      total: `${(memory.totalJSHeapSize / 1024 / 1024).toFixed(2)} MB`,
      limit: `${(memory.jsHeapSizeLimit / 1024 / 1024).toFixed(2)} MB`
    })
  }
}
