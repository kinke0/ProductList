<template>
  <div class="viewport">
    <router-view />
  </div>
</template>

<script setup>
</script>

<style>
.viewport {
  will-change: transform;
  transform: translateZ(0);
  backface-visibility: hidden;
}
body {
  margin: 0;
  padding: 0;
  background: #F8FAFC;
}
</style>
