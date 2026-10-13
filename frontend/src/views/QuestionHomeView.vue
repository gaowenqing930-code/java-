<template>
  <div class="question-home">
    <div class="page-tabs">
      <div class="page-tab" :class="{ active: mainTab === 'practice' }" @click="mainTab = 'practice'">专项练习</div>
      <div class="page-tab" :class="{ active: mainTab === 'exam' && examSubTab === '笔试' }" @click="examSubTab = '笔试'; mainTab = 'exam'">笔试真题 <span class="badge">笔试</span></div>
      <div class="page-tab" :class="{ active: mainTab === 'exam' && examSubTab === '面试' }" @click="examSubTab = '面试'; mainTab = 'exam'">面试真题 <span class="badge">面试</span></div>
    </div>

    <!-- ============ 专项练习 ============ -->
    <div v-show="mainTab === 'practice'">
      <div class="section">
        <div class="section-title"><span class="st-no">01</span>推荐</div>
        <div class="card-grid">
          <div v-for="(card, i) in recommendCards" :key="card.id" class="practice-card" @click="goPractice(card)">
            <span class="card-index">{{ pad(i + 1) }}</span>
            <div class="card-head">
              <span class="card-name">{{ card.name }}</span>
              <el-tag v-if="card.newCount" size="small" type="danger" effect="plain" class="new-badge">新题+{{ card.newCount }}</el-tag>
              <el-tag v-if="card.matchGoal" size="small" effect="dark" class="goal-badge">为你推荐</el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 分模块 -->
      <div v-for="(g, gi) in moduleGroups" :key="g" class="section">
        <div class="section-title"><span class="st-no">{{ pad(gi + 2) }}</span>{{ g }}</div>
        <div class="card-grid">
          <div v-for="card in groupCards(g)" :key="card.id" class="practice-card" @click="goPractice(card)">
            <span class="card-index">·</span>
            <div class="card-head">
              <span class="card-name">{{ card.name }}</span>
              <el-tag v-if="card.newCount" size="small" type="danger" effect="plain" class="new-badge">新题+{{ card.newCount }}</el-tag>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ 真题（笔试 + 面试） ============ -->
    <div v-show="mainTab === 'exam'">
      <div class="exam-toolbar">
        <el-input v-model="examKeyword" placeholder="输入公司、职位搜索" clearable class="search-input" :prefix-icon="Search" />
        <div class="toolbar-right">
          <div class="sort-toggle">
            <span :class="{ active: examSort === '推荐' }" @click="examSort = '推荐'">推荐</span>
            <span :class="{ active: examSort === '最新' }" @click="examSort = '最新'">最新</span>
          </div>
        </div>
      </div>

      <div class="exam-section">
        <div class="section-title"><span class="st-no">A</span>热门公司</div>
        <div class="hot-companies">
          <span
            v-for="c in HOT_COMPANIES"
            :key="c"
            class="company-chip"
            :class="{ active: examCompany === c }"
            @click="toggleCompany(c)"
          >{{ c }}</span>
        </div>
      </div>

      <div class="section">
        <div class="section-title"><span class="st-no">B</span>{{ examSubTab }}套卷</div>
        <div class="paper-grid">
          <div v-for="paper in filteredPapers" :key="paper.id" class="paper-card" @click="goPaper(paper)">
            <div class="paper-logo">{{ paper.company.slice(0, 2) }}</div>
            <div class="paper-info">
              <div class="paper-title">{{ paper.year }}年{{ paper.season }}-{{ paper.company }}-{{ paper.round }}{{ paper.position }}</div>
              <div class="paper-meta">
                <span>{{ paper.questionCount }} 题</span>
                <span class="heat">🔥 {{ paper.heat }}</span>
              </div>
            </div>
          </div>
          <el-empty v-if="filteredPapers.length === 0" description="暂无匹配套卷" :image-size="80" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { PRACTICE_CARDS, GROUP_ORDER, type PracticeCard } from '@/mock/practice'
import { HOT_COMPANIES } from '@/mock/exam'
import { listExamPapersApi, type ExamPaperVO } from '@/api/examPaper'
import { readFilledGoal, type CareerGoal } from '@/utils/careerGoal'

const router = useRouter()
const authStore = useAuthStore()

const mainTab = ref<'practice' | 'exam'>('practice')
const examSubTab = ref<'笔试' | '面试'>('笔试')

// —— 专项练习：根据求职目标动态生成推荐卡片（优先数据库 user，回退本地缓存）——
const goal = ref<CareerGoal | null>(readFilledGoal(authStore.user))
const matchMajors = computed(() => new Set(goal.value?.majors || []))

// 监听求职目标变更（登录弹框 / 个人中心修改后实时刷新推荐）
onMounted(() => {
  window.addEventListener('career-goal-changed', onGoalChanged)
})
onUnmounted(() => {
  window.removeEventListener('career-goal-changed', onGoalChanged)
})

function onGoalChanged() {
  goal.value = readFilledGoal(authStore.user)
}

// 真题列表：后端预置卷子
onMounted(async () => {
  try {
    examPapers.value = await listExamPapersApi()
  } catch {
    examPapers.value = []
  }
})

function cardMatchesGoal(card: PracticeCard): boolean {
  if (!goal.value || matchMajors.value.size === 0) return false
  return card.relatedMajors.some((m) => matchMajors.value.has(m))
}

const recommendCards = computed(() => {
  const matched = PRACTICE_CARDS.filter(cardMatchesGoal).map((c) => ({
    ...c,
    matchGoal: true,
  }))
  if (matched.length > 0) {
    return matched
  }
  return PRACTICE_CARDS.filter((c) => c.group === '推荐').map((c) => ({
    ...c,
    matchGoal: false,
  }))
})

const moduleGroups = GROUP_ORDER.filter((g) => g !== '推荐')
function groupCards(g: string) {
  return PRACTICE_CARDS.filter((c) => c.group === g)
}

function goPractice(card: PracticeCard) {
  router.push({ path: '/questions/practice', query: { keyword: card.keyword, title: card.name } })
}

// —— 真题（后端预置卷子，含固定题目）——
const examKeyword = ref('')
const examSort = ref<'推荐' | '最新'>('推荐')
const examCompany = ref('')
const examPapers = ref<ExamPaperVO[]>([])

function toggleCompany(c: string) {
  examCompany.value = examCompany.value === c ? '' : c
}

const filteredPapers = computed(() => {
  let list = examPapers.value.filter((p) => p.type === examSubTab.value)
  if (examCompany.value) {
    list = list.filter((p) => p.company.includes(examCompany.value!))
  }
  const kw = examKeyword.value.trim()
  if (kw) {
    list = list.filter((p) => p.company.includes(kw) || p.position.includes(kw))
  }
  if (examSort.value === '最新') {
    list = [...list].sort((a, b) => b.year - a.year || b.heat - a.heat)
  } else {
    list = [...list].sort((a, b) => b.heat - a.heat)
  }
  return list
})

function goPaper(paper: ExamPaperVO) {
  router.push({ path: `/exam/${paper.id}`, query: { type: paper.type } })
}

function pad(n: number): string {
  return String(n).padStart(2, '0')
}
</script>

<style scoped>
.question-home {
  max-width: 1100px;
  margin: 0 auto;
  padding: 28px 20px;
}

/* —— Tab：黑色下划线 —— */
.page-tabs {
  display: flex;
  gap: 36px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 26px;
}
.page-tab {
  font-size: 17px;
  font-weight: 400;
  color: var(--gray-400);
  padding: 4px 2px 14px;
  cursor: pointer;
  position: relative;
  transition: color 0.2s ease;
}
.page-tab::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 3px;
  background: var(--ink);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.25s ease;
}
.page-tab:hover {
  color: var(--ink);
}
.page-tab.active {
  color: var(--ink);
  font-weight: 600;
}
.page-tab.active::after {
  transform: scaleX(1);
}
.badge {
  font-family: var(--mono);
  font-size: 11px;
  color: var(--ink);
  background: var(--paper);
  border: 1px solid var(--ink);
  padding: 1px 6px;
  margin-left: 2px;
  vertical-align: middle;
  font-weight: 400;
  letter-spacing: 0.05em;
}

/* —— 区块标题：等宽序号 + 黑色粗体 —— */
.section {
  margin-bottom: 30px;
}
.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 16px;
}
.st-no {
  font-family: var(--mono);
  font-size: 13px;
  font-weight: 400;
  color: var(--gray-400);
  letter-spacing: 0.1em;
  border: 1px solid var(--line-strong);
  padding: 1px 7px;
}

/* —— 专项卡片：白底黑边 + hover 上移 —— */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
}
.practice-card {
  position: relative;
  background: var(--paper);
  border: 1px solid var(--line);
  padding: 18px 18px 16px;
  cursor: pointer;
  transition: all 0.25s ease;
}
.card-index {
  position: absolute;
  top: 12px;
  right: 14px;
  font-family: var(--mono);
  font-size: 12px;
  color: var(--gray-400);
  letter-spacing: 0.08em;
}
.practice-card:hover {
  border-color: var(--ink);
  transform: translateY(-3px);
  box-shadow: 6px 6px 0 var(--ink);
}
.card-head {
  display: flex;
  align-items: center;
  gap: 6px;
}
.card-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
}
.new-badge {
  margin-left: 4px;
}
.goal-badge {
  margin-left: auto;
  background: var(--ink);
  border-color: var(--ink);
  color: var(--paper);
  font-weight: 500;
}

/* —— 真题工具栏 —— */
.exam-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}
.search-input {
  width: 320px;
}
.sort-toggle {
  display: flex;
  gap: 18px;
  font-size: 14px;
}
.sort-toggle span {
  cursor: pointer;
  color: var(--gray-500);
  padding-bottom: 2px;
  position: relative;
  transition: color 0.2s;
}
.sort-toggle span::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -2px;
  height: 2px;
  background: var(--ink);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.2s ease;
}
.sort-toggle span.active {
  color: var(--ink);
  font-weight: 600;
}
.sort-toggle span.active::after {
  transform: scaleX(1);
}

.exam-section {
  margin-bottom: 26px;
}
.hot-companies {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.company-chip {
  padding: 7px 18px;
  border: 1px solid var(--line);
  background: var(--paper);
  font-size: 14px;
  color: var(--gray-500);
  cursor: pointer;
  transition: all 0.2s ease;
}
.company-chip:hover {
  border-color: var(--ink);
  color: var(--ink);
}
.company-chip.active {
  border-color: var(--ink);
  background: var(--ink);
  color: var(--paper);
  font-weight: 500;
}

/* —— 套卷卡片：黑白 —— */
.paper-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}
.paper-card {
  display: flex;
  gap: 14px;
  background: var(--paper);
  border: 1px solid var(--line);
  padding: 16px;
  cursor: pointer;
  transition: all 0.25s ease;
}
.paper-card:hover {
  border-color: var(--ink);
  transform: translateY(-2px);
  box-shadow: 4px 4px 0 var(--ink);
}
.paper-logo {
  width: 48px;
  height: 48px;
  background: var(--ink);
  color: var(--paper);
  font-weight: 700;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.paper-info {
  flex: 1;
  min-width: 0;
}
.paper-title {
  font-size: 14px;
  color: var(--ink);
  line-height: 1.5;
  margin-bottom: 8px;
  font-weight: 500;
}
.paper-meta {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--gray-400);
}
</style>
