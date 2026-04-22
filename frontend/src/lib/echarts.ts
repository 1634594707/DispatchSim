import { use, init, type EChartsType, type EChartsCoreOption } from 'echarts/core'
import { LineChart, BarChart, HeatmapChart, RadarChart, GaugeChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  VisualMapComponent,
  RadarComponent,
  GraphicComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([
  LineChart,
  BarChart,
  HeatmapChart,
  RadarChart,
  GaugeChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  VisualMapComponent,
  RadarComponent,
  GraphicComponent,
  CanvasRenderer,
])

export { init, type EChartsType }
export type EChartsOption = EChartsCoreOption
