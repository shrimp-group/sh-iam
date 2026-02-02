<template>
  <div class="layout-container">
    <div class="left-panel" :style="{ width: leftWidth + 'px' }">
      <slot name="left"/>
    </div>
    <div class="resizer" @mousedown="startResize">
      <div class="resizer-icon">
        <el-icon class="resize-icon"><DCaret /></el-icon>
      </div>
    </div>
    <div class="right-panel" :style="{ flex: 1 }">
      <slot name="right"/>
    </div>
  </div>
</template>

<script setup name="LayoutSplit">
import { ref } from 'vue';

const leftWidth = ref(200);
const isResizing = ref(false);

/** 开始拖动 */
function startResize(event) {
  isResizing.value = true;
  document.addEventListener('mousemove', resize);
  document.addEventListener('mouseup', stopResize);
}

/** 拖动中 */
function resize(event) {
  if (!isResizing.value) return;

  // 计算新的宽度，限制最小和最大值
  const container = document.querySelector('.layout-container');
  const containerRect = container.getBoundingClientRect();
  let newWidth = event.clientX - containerRect.left;

  // 限制宽度范围
  newWidth = Math.max(150, Math.min(newWidth, containerRect.width - 300));
  leftWidth.value = newWidth;

  // 强制触发重绘，确保右侧内容及时适应宽度变化
  const rightPanel = container.querySelector('.right-panel');
  if (rightPanel) {
    rightPanel.style.width = `${containerRect.width - newWidth - 12}px`;
    rightPanel.offsetHeight; // 触发重排
  }
}

/** 停止拖动 */
function stopResize() {
  isResizing.value = false;
  document.removeEventListener('mousemove', resize);
  document.removeEventListener('mouseup', stopResize);

  // 拖动结束后，移除右侧面板的内联宽度样式，回到正常的 flex 布局
  const container = document.querySelector('.layout-container');
  const rightPanel = container.querySelector('.right-panel');
  if (rightPanel) {
    rightPanel.style.width = '';
  }
}
</script>

<style scoped>
.layout-container {
  display: flex;
  min-height: 100%;
  position: relative;
}

.left-panel {
  background-color: #f5f7fa;
  border-right: 1px solid #e4e7ed;
  overflow: hidden;
  transition: width 0.3s ease;
  flex-shrink: 0;
}

.resizer {
  width: 12px;
  cursor: col-resize;
  background-color: transparent;
  transition: background-color 0.2s ease;
  align-self: stretch;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  margin: 0;
}

.resizer::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 0;
  bottom: 0;
  background-color: #e4e7ed;
}

.resizer:hover::before {
  background-color: #409eff;
}

.resizer:active::before {
  background-color: #409eff;
}

.resizer:hover {
  background-color: rgba(64, 158, 255, 0.1);
}

.resizer:active {
  background-color: rgba(64, 158, 255, 0.2);
}

.resizer-icon {
  width: 100%;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 12px;
  transition: color 0.2s ease;
  position: relative;
  z-index: 101;
}

.resize-icon {
  transform: rotate(90deg);
}

.resizer:hover .resizer-icon {
  color: #409eff;
}

.right-panel {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  transition: width 0.1s ease;
}

.right-panel > * {
  width: 100%;
  min-width: 0;
}
</style>
