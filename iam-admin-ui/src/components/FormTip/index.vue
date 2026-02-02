<template>
  <div class="form-tips">
    <div v-if="html" v-html="html"/>
    <div v-if="text">{{text}}</div>
  </div>
</template>
<script setup>
const props = defineProps({
  html: {
    type: String,
    default: undefined,
  },
  text: {
    type: String,
    default: undefined,
  },
});
</script>

<style lang="scss" scoped>

.form-tips {
  color: silver;
  :deep(p) {
    line-height: 1;
    margin: 1px 0 4px 0;
  }
  :deep(div) {
    line-height: 1;
    margin: 1px 0 4px 0;
  }
}
</style>
