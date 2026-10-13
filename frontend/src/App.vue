<template>
  <router-view />
  <CareerGoalDialog ref="goalDialog" />
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import CareerGoalDialog from '@/components/CareerGoalDialog.vue'

const authStore = useAuthStore()
const goalDialog = ref<InstanceType<typeof CareerGoalDialog>>()

// 已登录但未在数据库填写求职目标 → 弹框引导（填过的不再打扰）
onMounted(() => {
  if (authStore.isLoggedIn) {
    goalDialog.value?.maybeAsk(false)
  }
})

watch(
  () => authStore.isLoggedIn,
  (logged) => {
    if (logged) {
      // 登录成功：仅在未填写时弹框引导
      goalDialog.value?.maybeAsk(false)
    }
  },
)
</script>
