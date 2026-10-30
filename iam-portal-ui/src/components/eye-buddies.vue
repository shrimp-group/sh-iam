<!-- 登录页动画小人（复刻 careercompass animated-characters，纯装饰）：
     四个互相重叠的几何色块——紫高矩形(后)/黑矩形(中)/橘半圆(前左)/黄矩形(右前，带嘴)，底边平齐；
     紫黑为白眼球+黑瞳（随机眨眼），橘黄为纯黑瞳。
     交互：眼珠/脸部/身体随鼠标偏移与倾斜（CSS transition 平滑）；聚焦账号框时小人对视 800ms、
     紫色长高凑近；密码隐藏输入时紫色保持凑近；密码明文时全体站直扭头看密码方向、紫色随机偷瞄。
     仅监听 window mousemove，pointer-events: none，对登录逻辑零影响 -->
<template>
  <div class="eye-buddies" aria-hidden="true">
    <!-- 紫色高矩形（后层）：输入/隐藏密码时凑近长高，明文时移到最左偷看 -->
    <div ref="purpleRef" class="buddy buddy--purple" :style="purpleBodyStyle">
      <div ref="purpleEyesRef" class="eyes eyes--slow" :style="purpleEyesStyle">
        <span v-for="i in 2" :key="i" class="eyeball" :class="{ 'eyeball--blink': purpleBlinking }">
          <span v-if="!purpleBlinking" class="pupil" :style="pupilStyle(purplePupil)"></span>
        </span>
      </div>
    </div>
    <!-- 黑色矩形（中层） -->
    <div ref="blackRef" class="buddy buddy--black" :style="blackBodyStyle">
      <div ref="blackEyesRef" class="eyes eyes--slow" :style="blackEyesStyle">
        <span v-for="i in 2" :key="i" class="eyeball" :class="{ 'eyeball--blink': blackBlinking }">
          <span v-if="!blackBlinking" class="pupil" :style="pupilStyle(blackPupil)"></span>
        </span>
      </div>
    </div>
    <!-- 橘色半圆（前左层，纯瞳孔） -->
    <div ref="orangeRef" class="buddy buddy--orange" :style="orangeBodyStyle">
      <div ref="orangeEyesRef" class="eyes eyes--fast" :style="orangeEyesStyle">
        <span v-for="i in 2" :key="i" class="pupil-dot" :style="pupilStyle(orangePupil)"></span>
      </div>
    </div>
    <!-- 黄色矩形（右前层，纯瞳孔 + 嘴） -->
    <div ref="yellowRef" class="buddy buddy--yellow" :style="yellowBodyStyle">
      <div ref="yellowEyesRef" class="eyes eyes--fast" :style="yellowEyesStyle">
        <span v-for="i in 2" :key="i" class="pupil-dot" :style="pupilStyle(yellowPupil)"></span>
      </div>
      <div class="mouth" :style="mouthStyle"></div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'

const props = defineProps({
  // 账号输入框聚焦中：小人们先对视一眼，紫色持续凑近
  isTyping: { type: Boolean, default: false },
  // 密码明文可见：全体站直扭头看密码方向，紫色随机偷瞄
  showPassword: { type: Boolean, default: false },
  // 密码已输入字符数：隐藏状态下输入时紫色保持凑近姿态
  passwordLength: { type: Number, default: 0 }
})

// 鼠标位置；moved 标记首次移动前眼珠/身体保持正中，避免初始看向屏幕外
const mouse = reactive({ x: 0, y: 0, moved: false })

// ---- 运行状态 ----
const purpleBlinking = ref(false)
const blackBlinking = ref(false)
const isLooking = ref(false)
const isPeeking = ref(false)

const isHiding = computed(() => props.passwordLength > 0 && !props.showPassword)
const isPeekShow = computed(() => props.passwordLength > 0 && props.showPassword)

// ---- 眨眼（紫/黑独立，随机 3~7s 一次，闭合 150ms）----
function blinkLoop(setter) {
  const timer = { id: null }
  const schedule = () => {
    timer.id = setTimeout(() => {
      setter(true)
      setTimeout(() => {
        setter(false)
        schedule()
      }, 150)
    }, Math.random() * 4000 + 3000)
  }
  schedule()
  return () => clearTimeout(timer.id)
}

let stopPurpleBlink = null
let stopBlackBlink = null
let lookTimer = null
let peekTimer = null
let peekResetTimer = null

// 聚焦账号框：小人们对视 800ms 后回归跟随
watch(() => props.isTyping, (typing) => {
  clearTimeout(lookTimer)
  if (typing) {
    isLooking.value = true
    lookTimer = setTimeout(() => {
      isLooking.value = false
    }, 800)
  } else {
    isLooking.value = false
  }
})

// 密码明文期间：紫色每 2~5s 偷瞄一眼（800ms），配合固定朝向的「扭头看密码」
function schedulePeek() {
  peekTimer = setTimeout(() => {
    isPeeking.value = true
    peekResetTimer = setTimeout(() => {
      isPeeking.value = false
      if (isPeekShow.value) {
        schedulePeek()
      }
    }, 800)
  }, Math.random() * 3000 + 2000)
}

watch(isPeekShow, (active) => {
  clearTimeout(peekTimer)
  clearTimeout(peekResetTimer)
  if (active) {
    schedulePeek()
  } else {
    isPeeking.value = false
  }
})

// ---- 体位计算（对照原版 calculatePosition）----
const purpleRef = ref(null)
const blackRef = ref(null)
const orangeRef = ref(null)
const yellowRef = ref(null)
const purpleEyesRef = ref(null)
const blackEyesRef = ref(null)
const orangeEyesRef = ref(null)
const yellowEyesRef = ref(null)

function bodyPose(el) {
  if (!el || !mouse.moved) return { faceX: 0, faceY: 0, skew: 0 }
  const rect = el.getBoundingClientRect()
  const dx = mouse.x - (rect.left + rect.width / 2)
  const dy = mouse.y - (rect.top + rect.height / 3)
  return {
    faceX: Math.max(-15, Math.min(15, dx / 20)),
    faceY: Math.max(-10, Math.min(10, dy / 30)),
    skew: Math.max(-6, Math.min(6, -dx / 120))
  }
}

const purplePose = computed(() => bodyPose(purpleRef.value))
const blackPose = computed(() => bodyPose(blackRef.value))
const orangePose = computed(() => bodyPose(orangeRef.value))
const yellowPose = computed(() => bodyPose(yellowRef.value))

// 眼珠朝向：眼球容器中心指向鼠标，偏移钳制在眼眶内（对照原版 maxDistance）
function eyeLook(el, maxDistance) {
  if (!el || !mouse.moved) return { x: 0, y: 0 }
  const rect = el.getBoundingClientRect()
  const dx = mouse.x - (rect.left + rect.width / 2)
  const dy = mouse.y - (rect.top + rect.height / 2)
  const dist = Math.min(Math.hypot(dx, dy), maxDistance)
  const angle = Math.atan2(dy, dx)
  return { x: Math.cos(angle) * dist, y: Math.sin(angle) * dist }
}

const purpleEye = computed(() => eyeLook(purpleEyesRef.value, 5))
const blackEye = computed(() => eyeLook(blackEyesRef.value, 4))
const orangeEye = computed(() => eyeLook(orangeEyesRef.value, 5))
const yellowEye = computed(() => eyeLook(yellowEyesRef.value, 5))

function pupilStyle(offset) {
  return { transform: `translate(${offset.x}px, ${offset.y}px)` }
}

// 各小人最终瞳孔朝向：状态朝向（偷看/扭头/对视）优先于鼠标跟随
const purplePupil = computed(() => {
  if (isPeekShow.value) {
    return isPeeking.value ? { x: 4, y: 5 } : { x: -4, y: -4 }
  }
  if (isLooking.value) return { x: 3, y: 4 }
  return purpleEye.value
})
const blackPupil = computed(() => {
  if (isPeekShow.value) return { x: -4, y: -4 }
  if (isLooking.value) return { x: 0, y: -4 }
  return blackEye.value
})
const orangePupil = computed(() => (isPeekShow.value ? { x: -5, y: -4 } : orangeEye.value))
const yellowPupil = computed(() => (isPeekShow.value ? { x: -5, y: -4 } : yellowEye.value))

// ---- 身体姿态 ----
const purpleBodyStyle = computed(() => {
  const pose = purplePose.value
  const lean = props.isTyping || isHiding.value
  let transform
  if (isPeekShow.value) {
    transform = 'skewX(0deg)'
  } else if (lean) {
    transform = `skewX(${pose.skew - 12}deg) translateX(40px)`
  } else {
    transform = `skewX(${pose.skew}deg)`
  }
  return {
    height: lean ? '440px' : '400px',
    transform,
    transformOrigin: 'bottom center'
  }
})

const blackBodyStyle = computed(() => {
  const skew = blackPose.value.skew * 1.5
  let transform
  if (isPeekShow.value) {
    transform = 'skewX(0deg)'
  } else if (isLooking.value) {
    transform = `skewX(${skew + 10}deg) translateX(20px)`
  } else if (props.isTyping || isHiding.value) {
    transform = `skewX(${skew}deg)`
  } else {
    transform = `skewX(${blackPose.value.skew}deg)`
  }
  return { transform, transformOrigin: 'bottom center' }
})

const orangeBodyStyle = computed(() => ({
  transform: isPeekShow.value ? 'skewX(0deg)' : `skewX(${orangePose.value.skew}deg)`,
  transformOrigin: 'bottom center'
}))

const yellowBodyStyle = computed(() => ({
  transform: isPeekShow.value ? 'skewX(0deg)' : `skewX(${yellowPose.value.skew}deg)`,
  transformOrigin: 'bottom center'
}))

// ---- 眼睛/嘴位置（状态优先：偷看 > 对视 > 鼠标跟随）----
const purpleEyesStyle = computed(() => {
  const pose = purplePose.value
  if (isPeekShow.value) return { left: '20px', top: '35px' }
  if (isLooking.value) return { left: '55px', top: '65px' }
  return { left: `${45 + pose.faceX}px`, top: `${40 + pose.faceY}px` }
})

const blackEyesStyle = computed(() => {
  const pose = blackPose.value
  if (isPeekShow.value) return { left: '10px', top: '28px' }
  if (isLooking.value) return { left: '32px', top: '12px' }
  return { left: `${26 + pose.faceX}px`, top: `${32 + pose.faceY}px` }
})

const orangeEyesStyle = computed(() => {
  const pose = orangePose.value
  if (isPeekShow.value) return { left: '50px', top: '85px' }
  return { left: `${82 + pose.faceX}px`, top: `${90 + pose.faceY}px` }
})

const yellowEyesStyle = computed(() => {
  const pose = yellowPose.value
  if (isPeekShow.value) return { left: '20px', top: '35px' }
  return { left: `${52 + pose.faceX}px`, top: `${40 + pose.faceY}px` }
})

const mouthStyle = computed(() => {
  const pose = yellowPose.value
  if (isPeekShow.value) return { left: '10px', top: '88px' }
  return { left: `${40 + pose.faceX}px`, top: `${88 + pose.faceY}px` }
})

onMounted(() => {
  window.addEventListener('mousemove', onMouseMove)
  stopPurpleBlink = blinkLoop((v) => { purpleBlinking.value = v })
  stopBlackBlink = blinkLoop((v) => { blackBlinking.value = v })
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onMouseMove)
  if (stopPurpleBlink) stopPurpleBlink()
  if (stopBlackBlink) stopBlackBlink()
  clearTimeout(lookTimer)
  clearTimeout(peekTimer)
  clearTimeout(peekResetTimer)
})

function onMouseMove(e) {
  mouse.x = e.clientX
  mouse.y = e.clientY
  mouse.moved = true
}
</script>

<style lang="scss" scoped>
.eye-buddies {
  position: absolute;
  width: 550px;
  height: 440px;
  pointer-events: none;
  z-index: 0;
  // 左侧垂直居中，整体放大小人（眼珠跟随基于实时 getBoundingClientRect，缩放不影响计算正确性）
  left: 8%;
  top: 50%;
  transform: translateY(-50%) scale(1.2);
  transform-origin: left center;

  // 中屏缩小避免压到右侧登录表单，小屏隐藏
  @media (max-width: 1280px) {
    left: 6%;
    transform: translateY(-50%) scale(0.7);
    transform-origin: left center;
  }

  @media (max-width: 992px) {
    display: none;
  }
}

.buddy {
  position: absolute;
  bottom: 0;
  transition: transform 0.7s cubic-bezier(0.4, 0, 0.2, 1), height 0.7s cubic-bezier(0.4, 0, 0.2, 1);

  // 紫色高矩形（后层）
  &--purple {
    left: 70px;
    width: 180px;
    height: 400px;
    border-radius: 10px 10px 0 0;
    background: #6c3ff5;
    z-index: 1;
  }

  // 黑色矩形（中层）
  &--black {
    left: 240px;
    width: 120px;
    height: 310px;
    border-radius: 8px 8px 0 0;
    background: #2d2d2d;
    z-index: 2;
  }

  // 橘色半圆（前左层）
  &--orange {
    left: 0;
    width: 240px;
    height: 200px;
    border-radius: 120px 120px 0 0;
    background: #ff9b6b;
    z-index: 3;
  }

  // 黄色矩形（右前层）
  &--yellow {
    left: 310px;
    width: 140px;
    height: 230px;
    border-radius: 70px 70px 0 0;
    background: #e8d754;
    z-index: 4;
  }
}

.eyes {
  position: absolute;
  display: flex;

  // 紫黑：脸部跟随较缓（对照原版 duration-700）
  &--slow {
    gap: 32px;
    transition: left 0.7s cubic-bezier(0.4, 0, 0.2, 1), top 0.7s cubic-bezier(0.4, 0, 0.2, 1);
  }

  // 橘黄：脸部跟随较快（对照原版 duration-200）
  &--fast {
    gap: 32px;
    transition: left 0.2s ease-out, top 0.2s ease-out;
  }
}

.buddy--black .eyes {
  gap: 24px;
}

.eyeball {
  --size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: var(--size);
  height: var(--size);
  border-radius: 50%;
  background: #fff;
  overflow: hidden;
  flex-shrink: 0;
  transition: height 0.15s ease;
}

// 眨眼：眼球压扁为一条缝，瞳孔隐藏
.eyeball.eyeball--blink {
  height: 2px;
}

.buddy--black .eyeball {
  --size: 16px;
}

.pupil {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2d2d2d;
  flex-shrink: 0;
  transition: transform 0.1s ease-out;
}

.buddy--black .pupil {
  width: 6px;
  height: 6px;
}

// 橘黄小人：纯黑瞳孔，无眼白
.pupil-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #2d2d2d;
  flex-shrink: 0;
  transition: transform 0.1s ease-out;
}

.mouth {
  position: absolute;
  width: 80px;
  height: 4px;
  border-radius: 999px;
  background: #2d2d2d;
  transition: left 0.2s ease-out, top 0.2s ease-out;
}
</style>
