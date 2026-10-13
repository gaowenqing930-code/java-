<template>
  <el-dialog
    v-model="visible"
    title="选择你的求职目标"
    width="520px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <div class="goal-dialog">
      <p class="goal-tip">填写后将为你推荐更匹配的题库与真题；也可以先跳过，后续在个人中心完善。</p>
      <p v-if="hasExisting" class="goal-exist-tip">已保存的目标已为你预填，可直接确认或修改。</p>

      <div class="goal-block">
        <div class="goal-label">我的专业 / 方向（可多选）</div>
        <el-checkbox-group v-model="selectedMajors">
          <el-checkbox v-for="m in MAJORS" :key="m" :value="m" class="goal-tag">{{ m }}</el-checkbox>
        </el-checkbox-group>
      </div>

      <div class="goal-block">
        <div class="goal-label">目标职业（单选）</div>
        <el-radio-group v-model="selectedPosition">
          <el-radio v-for="p in POSITIONS" :key="p" :value="p" class="goal-tag">{{ p }}</el-radio>
        </el-radio-group>
      </div>
    </div>

    <template #footer>
      <el-button :loading="saving" @click="handleSkip">{{ hasExisting ? '暂不修改' : '暂不设置' }}</el-button>
      <el-button type="primary" :loading="saving" :disabled="!selectedPosition" @click="handleSave">保存目标</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { CAREER_GOAL_KEY, readCareerGoal, readFilledGoal, type CareerGoal } from '@/utils/careerGoal'

const MAJORS = ['Java', '前端', 'Python', 'C++', '算法', '数据', 'AI/大模型', '测试', '运维', '嵌入式', '产品']
const POSITIONS = ['软件开发', '前端开发', '算法工程师', '数据研发', '测试开发', '产品经理', '考公', '国央企']

const visible = ref(false)
const selectedMajors = ref<string[]>([])
const selectedPosition = ref('')
const hasExisting = ref(false)
const saving = ref(false)

const authStore = useAuthStore()

const emit = defineEmits<{ (e: 'saved', goal: CareerGoal): void }>()

/** 目标变更通知（推荐栏等监听刷新） */
function notifyChanged() {
  window.dispatchEvent(new CustomEvent('career-goal-changed'))
}

/**
 * 弹出求职目标选择框。
 * force=false（登录后引导）：仅在未填写（数据库与本地缓存均无）时弹出，填过就不打扰；
 * force=true（个人中心"修改"）：总是弹出并预填当前目标。
 */
function maybeAsk(force = false) {
  const filled = readFilledGoal(authStore.user)
  if (!force && filled) {
    return
  }
  hasExisting.value = !!filled
  if (filled) {
    selectedMajors.value = [...(filled.majors || [])]
    selectedPosition.value = filled.position
  } else {
    selectedMajors.value = []
    selectedPosition.value = ''
  }
  visible.value = true
}

function handleSkip() {
  // 已有目标时是"暂不修改"：保留原目标；首次弹出才是"暂不设置"
  if (!hasExisting.value) {
    localStorage.setItem(CAREER_GOAL_KEY, JSON.stringify({ majors: [], position: 'skip' }))
  }
  visible.value = false
  notifyChanged()
}

async function handleSave() {
  const goal: CareerGoal = { majors: selectedMajors.value, position: selectedPosition.value }
  // 写入数据库（user.major / user.target_job），本地缓存兜底
  localStorage.setItem(CAREER_GOAL_KEY, JSON.stringify(goal))
  saving.value = true
  try {
    await authStore.updateProfile({ major: goal.majors.join(','), targetJob: goal.position })
  } catch {
    ElMessage.warning('云端保存失败，目标已保存在本机，下次登录将再次引导完善')
  } finally {
    saving.value = false
  }
  visible.value = false
  emit('saved', goal)
  notifyChanged()
}

defineExpose({ maybeAsk })
</script>

<style scoped>
.goal-dialog {
  padding: 4px 0;
}

.goal-tip {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
  margin: 0 0 8px;
}

.goal-exist-tip {
  color: #2563eb;
  font-size: 13px;
  margin: 0 0 12px;
}

.goal-block {
  margin-bottom: 16px;
}

.goal-label {
  font-weight: 600;
  color: #374151;
  margin-bottom: 10px;
  font-size: 14px;
}

.goal-tag {
  margin: 0 8px 8px 0;
}
</style>
