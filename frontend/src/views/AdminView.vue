<template>
  <div class="admin-page">
    <div class="page-head">
      <h2 class="page-title">后台管理</h2>
    </div>

    <el-tabs v-model="tab">
      <!-- 用户管理 -->
      <el-tab-pane label="用户管理" name="users">
        <div class="toolbar">
          <el-input v-model="userKeyword" placeholder="搜索用户名/邮箱" clearable style="width: 220px" @keyup.enter="loadUsers(1)" @clear="loadUsers(1)">
            <template #append><el-button @click="loadUsers(1)">搜索</el-button></template>
          </el-input>
        </div>
        <el-table :data="users" v-loading="userLoading" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="email" label="邮箱" min-width="180" />
          <el-table-column prop="role" label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'info'" size="small">{{ row.role }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130">
            <template #default="{ row }">
              <el-button v-if="row.role !== 'ADMIN'" size="small" :type="row.status === 1 ? 'danger' : 'success'" plain
                @click="toggleUser(row)">
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <span v-else class="admin-tag">管理员</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination v-model:current-page="userPage" :page-size="10" :total="userTotal" layout="prev, pager, next" @current-change="loadUsers" />
        </div>
      </el-tab-pane>

      <!-- 题目管理 -->
      <el-tab-pane label="题目管理" name="questions">
        <div class="toolbar">
          <el-select v-model="qType" placeholder="题型" clearable style="width: 130px" @change="loadQuestions(1)">
            <el-option label="单选题" value="SINGLE" />
            <el-option label="多选题" value="MULTIPLE" />
            <el-option label="判断题" value="JUDGE" />
            <el-option label="简答题" value="SHORT" />
          </el-select>
          <el-select v-model="qStatus" placeholder="状态" clearable style="width: 130px" @change="loadQuestions(1)">
            <el-option label="已上架" :value="1" />
            <el-option label="已下架" :value="0" />
          </el-select>
          <el-input v-model="qKeyword" placeholder="搜索题干" clearable style="width: 220px" @keyup.enter="loadQuestions(1)" @clear="loadQuestions(1)">
            <template #append><el-button @click="loadQuestions(1)">搜索</el-button></template>
          </el-input>
        </div>
        <el-table :data="questions" v-loading="qLoading" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column label="题型" width="90">
            <template #default="{ row }">{{ typeLabel(row.type) }}</template>
          </el-table-column>
          <el-table-column prop="stem" label="题干" min-width="300" show-overflow-tooltip />
          <el-table-column prop="difficulty" label="难度" width="80" />
          <el-table-column prop="source" label="来源" width="90" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '上架' : '下架' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110">
            <template #default="{ row }">
              <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" plain
                @click="toggleQuestion(row)">
                {{ row.status === 1 ? '下架' : '上架' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination v-model:current-page="qPage" :page-size="10" :total="qTotal" layout="prev, pager, next" @current-change="loadQuestions" />
        </div>
      </el-tab-pane>

      <!-- 操作日志 -->
      <el-tab-pane label="操作日志" name="logs">
        <div class="toolbar">
          <el-input v-model="logAction" placeholder="按动作筛选，如 ADMIN_USER_UPDATE / AI_FAIL" clearable style="width: 280px" @keyup.enter="loadLogs(1)" @clear="loadLogs(1)">
            <template #append><el-button @click="loadLogs(1)">搜索</el-button></template>
          </el-input>
        </div>
        <el-table :data="logs" v-loading="logLoading" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="action" label="动作" width="190">
            <template #default="{ row }">
              <el-tag :type="row.action.includes('FAIL') ? 'danger' : 'primary'" size="small">{{ row.action }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="detail" label="详情" min-width="300" show-overflow-tooltip />
          <el-table-column prop="userId" label="操作人ID" width="100" />
          <el-table-column prop="createTime" label="时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination v-model:current-page="logPage" :page-size="10" :total="logTotal" layout="prev, pager, next" @current-change="loadLogs" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminLogPageApi,
  adminQuestionPageApi,
  adminUpdateQuestionStatusApi,
  adminUpdateUserStatusApi,
  adminUserPageApi,
  type AdminUserVO,
  type OperationLogVO,
} from '@/api/admin'
import type { QuestionVO } from '@/api/question'
import { QUESTION_TYPE_LABEL, type QuestionType } from '@/api/question'

const tab = ref('users')

// 用户
const users = ref<AdminUserVO[]>([])
const userLoading = ref(false)
const userTotal = ref(0)
const userPage = ref(1)
const userKeyword = ref('')

// 题目
const questions = ref<QuestionVO[]>([])
const qLoading = ref(false)
const qTotal = ref(0)
const qPage = ref(1)
const qType = ref('')
const qStatus = ref<number>()
const qKeyword = ref('')

// 日志
const logs = ref<OperationLogVO[]>([])
const logLoading = ref(false)
const logTotal = ref(0)
const logPage = ref(1)
const logAction = ref('')

async function loadUsers(page = userPage.value) {
  userLoading.value = true
  try {
    const data = await adminUserPageApi({ keyword: userKeyword.value || undefined, pageNum: page, pageSize: 10 })
    users.value = data.records
    userTotal.value = data.total
  } finally {
    userLoading.value = false
  }
}

async function toggleUser(row: AdminUserVO) {
  try {
    await ElMessageBox.confirm(`确定${row.status === 1 ? '禁用' : '启用'}用户「${row.username}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await adminUpdateUserStatusApi(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('操作成功')
  loadUsers()
}

async function loadQuestions(page = qPage.value) {
  qLoading.value = true
  try {
    const data = await adminQuestionPageApi({
      type: qType.value || undefined,
      status: qStatus.value,
      keyword: qKeyword.value || undefined,
      pageNum: page,
      pageSize: 10,
    })
    questions.value = data.records
    qTotal.value = data.total
  } finally {
    qLoading.value = false
  }
}

async function toggleQuestion(row: QuestionVO) {
  await adminUpdateQuestionStatusApi(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已下架' : '已上架')
  loadQuestions()
}

async function loadLogs(page = logPage.value) {
  logLoading.value = true
  try {
    const data = await adminLogPageApi({ action: logAction.value || undefined, pageNum: page, pageSize: 10 })
    logs.value = data.records
    logTotal.value = data.total
  } finally {
    logLoading.value = false
  }
}

function typeLabel(type: string): string {
  return QUESTION_TYPE_LABEL[type as QuestionType] || type
}

function formatDateTime(s: string): string {
  const d = new Date(s)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

onMounted(() => {
  loadUsers()
  loadQuestions()
  loadLogs()
})
</script>

<style scoped>
.admin-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 20px;
}

.page-head {
  margin-bottom: 12px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  color: #1f2937;
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.admin-tag {
  font-size: 12px;
  color: #9ca3af;
}
</style>
