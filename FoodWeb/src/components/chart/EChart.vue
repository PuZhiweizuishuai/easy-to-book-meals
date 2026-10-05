<template>
  <!--
    通用 ECharts 容器组件
    - option 变化时增量更新图表（notMerge=false 保留动画过渡）
    - 容器尺寸变化（窗口 resize / 侧边栏折叠）自动 resize
    - 组件卸载时销毁实例，避免内存泄漏
    - echarts 按模块按需引入，避免把整包打进产物
  -->
  <div ref="chartEl" :style="{ width: '100%', height: height + 'px' }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
// 按需引入：只注册报表实际用到的图表与组件，避免把 echarts 整包打进产物
import * as echarts from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart,
  LineChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  CanvasRenderer
])

const props = defineProps({
  /** ECharts 配置项 */
  option: { type: Object, default: () => ({}) },
  /** 图表高度（像素） */
  height: { type: Number, default: 320 },
  /** 数据为空时是否展示占位文案 */
  loading: { type: Boolean, default: false }
})

const chartEl = ref(null)
let chart = null
let observer = null

const render = () => {
  if (!chart) return
  chart.setOption(props.option, { notMerge: true })
}

onMounted(async () => {
  await nextTick()
  if (!chartEl.value) return
  chart = echarts.init(chartEl.value)
  render()

  // 监听容器尺寸变化（比 window.resize 更准确，覆盖布局变化场景）
  if (typeof ResizeObserver !== 'undefined') {
    observer = new ResizeObserver(() => chart?.resize())
    observer.observe(chartEl.value)
  } else {
    window.addEventListener('resize', handleResize)
  }
})

const handleResize = () => chart?.resize()

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  observer?.disconnect()
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})
</script>
