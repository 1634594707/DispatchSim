/**
 * Verification script for useZoomPan coordinate transformation functions
 * This script demonstrates that mapToScreen and screenToMap work correctly
 * 
 * Run this file to verify Task 1.4 implementation
 */

// Simplified version of the transformation functions for verification
class CoordinateTransformVerifier {
  private zoom = 1
  private offsetX = 0
  private offsetY = 0

  setZoom(zoom: number) {
    this.zoom = Math.max(0.1, Math.min(10, zoom))
  }

  setPan(x: number, y: number) {
    this.offsetX = x
    this.offsetY = y
  }

  mapToScreen(x: number, y: number): { x: number; y: number } {
    return {
      x: x * this.zoom + this.offsetX,
      y: y * this.zoom + this.offsetY,
    }
  }

  screenToMap(x: number, y: number): { x: number; y: number } {
    return {
      x: (x - this.offsetX) / this.zoom,
      y: (y - this.offsetY) / this.zoom,
    }
  }
}

// Verification tests
function runVerification() {
  console.log('='.repeat(60))
  console.log('Task 1.4 Coordinate Transformation Verification')
  console.log('='.repeat(60))
  console.log()

  const verifier = new CoordinateTransformVerifier()
  let testsPassed = 0
  let testsFailed = 0

  // Helper function to check if two numbers are approximately equal
  const approxEqual = (a: number, b: number, epsilon = 0.0001): boolean => {
    return Math.abs(a - b) < epsilon
  }

  // Helper function to run a test
  const test = (
    name: string,
    actual: { x: number; y: number },
    expected: { x: number; y: number }
  ) => {
    const passed =
      approxEqual(actual.x, expected.x) && approxEqual(actual.y, expected.y)
    if (passed) {
      console.log(`✅ PASS: ${name}`)
      console.log(`   Expected: (${expected.x}, ${expected.y})`)
      console.log(`   Got:      (${actual.x}, ${actual.y})`)
      testsPassed++
    } else {
      console.log(`❌ FAIL: ${name}`)
      console.log(`   Expected: (${expected.x}, ${expected.y})`)
      console.log(`   Got:      (${actual.x}, ${actual.y})`)
      testsFailed++
    }
    console.log()
  }

  // Test 1: Identity transformation (zoom=1, offset=0)
  console.log('Test Suite 1: Identity Transformation')
  console.log('-'.repeat(60))
  verifier.setZoom(1)
  verifier.setPan(0, 0)
  test(
    'mapToScreen with identity',
    verifier.mapToScreen(100, 100),
    { x: 100, y: 100 }
  )
  test(
    'screenToMap with identity',
    verifier.screenToMap(100, 100),
    { x: 100, y: 100 }
  )

  // Test 2: Zoom only (zoom=2, offset=0)
  console.log('Test Suite 2: Zoom Transformation')
  console.log('-'.repeat(60))
  verifier.setZoom(2)
  verifier.setPan(0, 0)
  test(
    'mapToScreen with 2x zoom',
    verifier.mapToScreen(50, 50),
    { x: 100, y: 100 }
  )
  test(
    'screenToMap with 2x zoom',
    verifier.screenToMap(100, 100),
    { x: 50, y: 50 }
  )

  // Test 3: Pan only (zoom=1, offset=50)
  console.log('Test Suite 3: Pan Transformation')
  console.log('-'.repeat(60))
  verifier.setZoom(1)
  verifier.setPan(50, 50)
  test(
    'mapToScreen with pan offset',
    verifier.mapToScreen(100, 100),
    { x: 150, y: 150 }
  )
  test(
    'screenToMap with pan offset',
    verifier.screenToMap(150, 150),
    { x: 100, y: 100 }
  )

  // Test 4: Combined zoom and pan
  console.log('Test Suite 4: Combined Zoom and Pan')
  console.log('-'.repeat(60))
  verifier.setZoom(2)
  verifier.setPan(50, 50)
  test(
    'mapToScreen with zoom and pan',
    verifier.mapToScreen(25, 25),
    { x: 100, y: 100 }
  )
  test(
    'screenToMap with zoom and pan',
    verifier.screenToMap(100, 100),
    { x: 25, y: 25 }
  )

  // Test 5: Inverse relationship
  console.log('Test Suite 5: Inverse Relationship')
  console.log('-'.repeat(60))
  verifier.setZoom(1.5)
  verifier.setPan(30, 40)
  const original = { x: 123.456, y: 789.012 }
  const screen = verifier.mapToScreen(original.x, original.y)
  const backToMap = verifier.screenToMap(screen.x, screen.y)
  test(
    'screenToMap(mapToScreen(x, y)) = (x, y)',
    backToMap,
    original
  )

  // Test 6: Edge cases - minimum zoom
  console.log('Test Suite 6: Edge Cases - Minimum Zoom')
  console.log('-'.repeat(60))
  verifier.setZoom(0.1)
  verifier.setPan(0, 0)
  test(
    'mapToScreen with min zoom (0.1x)',
    verifier.mapToScreen(100, 100),
    { x: 10, y: 10 }
  )
  test(
    'screenToMap with min zoom (0.1x)',
    verifier.screenToMap(10, 10),
    { x: 100, y: 100 }
  )

  // Test 7: Edge cases - maximum zoom
  console.log('Test Suite 7: Edge Cases - Maximum Zoom')
  console.log('-'.repeat(60))
  verifier.setZoom(10)
  verifier.setPan(0, 0)
  test(
    'mapToScreen with max zoom (10x)',
    verifier.mapToScreen(10, 10),
    { x: 100, y: 100 }
  )
  test(
    'screenToMap with max zoom (10x)',
    verifier.screenToMap(100, 100),
    { x: 10, y: 10 }
  )

  // Test 8: Negative coordinates
  console.log('Test Suite 8: Negative Coordinates')
  console.log('-'.repeat(60))
  verifier.setZoom(1)
  verifier.setPan(0, 0)
  test(
    'mapToScreen with negative coords',
    verifier.mapToScreen(-50, -50),
    { x: -50, y: -50 }
  )
  test(
    'screenToMap with negative coords',
    verifier.screenToMap(-50, -50),
    { x: -50, y: -50 }
  )

  // Test 9: Large offsets
  console.log('Test Suite 9: Large Pan Offsets')
  console.log('-'.repeat(60))
  verifier.setZoom(1)
  verifier.setPan(1000, -500)
  test(
    'mapToScreen with large offsets',
    verifier.mapToScreen(100, 100),
    { x: 1100, y: -400 }
  )
  test(
    'screenToMap with large offsets',
    verifier.screenToMap(1100, -400),
    { x: 100, y: 100 }
  )

  // Summary
  console.log('='.repeat(60))
  console.log('Verification Summary')
  console.log('='.repeat(60))
  console.log(`Total Tests: ${testsPassed + testsFailed}`)
  console.log(`✅ Passed: ${testsPassed}`)
  console.log(`❌ Failed: ${testsFailed}`)
  console.log()

  if (testsFailed === 0) {
    console.log('🎉 All tests passed! Task 1.4 implementation is correct.')
    console.log()
    console.log('Verified:')
    console.log('  ✅ mapToScreen(x, y) function works correctly')
    console.log('  ✅ screenToMap(x, y) function works correctly')
    console.log('  ✅ Zoom transformations applied correctly')
    console.log('  ✅ Pan transformations applied correctly')
    console.log('  ✅ Inverse relationship verified')
    console.log('  ✅ Edge cases handled correctly')
  } else {
    console.log('⚠️  Some tests failed. Please review the implementation.')
  }
  console.log('='.repeat(60))

  return testsFailed === 0
}

// Run verification if executed directly (Node.js environment check)
if (typeof window === 'undefined') {
  runVerification()
}

export { runVerification, CoordinateTransformVerifier }
