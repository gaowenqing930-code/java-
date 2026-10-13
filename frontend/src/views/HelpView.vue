<template>
  <div class="help-page">
    <el-card shadow="never" class="panel">
      <template #header><span class="card-title">常见问题</span></template>
      <el-collapse accordion>
        <el-collapse-item v-for="faq in faqs" :key="faq.q" :title="faq.q">
          <p class="faq-a">{{ faq.a }}</p>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <el-card shadow="never" class="panel">
      <template #header><span class="card-title">意见反馈</span></template>
      <el-form label-position="top">
        <el-form-item label="反馈类型">
          <el-select v-model="form.type" placeholder="请选择反馈类型" style="width: 100%">
            <el-option label="功能建议" value="建议" />
            <el-option label="内容纠错" value="纠错" />
            <el-option label="体验问题" value="体验" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="反馈内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="请描述你的建议或遇到的问题（必填）"
          />
        </el-form-item>
        <el-form-item label="联系方式（选填）">
          <el-input v-model="form.contact" placeholder="邮箱 / 微信，便于我们与你联系" />
        </el-form-item>
        <el-button type="primary" :loading="submitting" :disabled="!form.content.trim()" @click="handleSubmit">
          提交反馈
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

interface Faq {
  q: string
  a: string
}

const faqs: Faq[] = [
  {
    q: '如何开始刷题？',
    a: '登录后点击顶部「题库」，选择学习方向与目标职业，即可进入专项练习；也可以直接选择企业真题套卷进行练习。',
  },
  {
    q: 'AI 智能出题怎么用？',
    a: '进入「AI 出题」，选择知识点并设置题量与难度，AI 会生成专属题目；交卷后每题都有解析，答错的题目会自动进入错题本。',
  },
  {
    q: 'AI 模拟面试流程是怎样的？',
    a: '进入「AI 面试」粘贴简历，AI 解析你的技术栈与项目后开始面试：逐题提问、你口头/文字作答，共 10 题，全部答完后 AI 统一评分，给出总分、评价与提升建议。',
  },
  {
    q: '错题本和学习报告有什么作用？',
    a: '答错的题会自动收录进错题本，可反复练习；学习报告按知识领域与知识点统计正确率，定位薄弱环节并给出建议。',
  },
  {
    q: '忘记密码怎么办？',
    a: '当前版本暂未开放自助找回密码，请联系平台管理员处理。',
  },
]

const form = reactive({ type: '建议', content: '', contact: '' })
const submitting = ref(false)

function handleSubmit() {
  submitting.value = true
  // 演示环境：前端本地提交，后续版本将接入服务端
  setTimeout(() => {
    submitting.value = false
    ElMessage.success('感谢你的反馈，我们会尽快处理！')
    form.content = ''
    form.contact = ''
  }, 800)
}
</script>

<style scoped>
.help-page {
  max-width: 820px;
  margin: 0 auto;
  padding: 24px 20px 48px;
}

.panel {
  border-radius: 14px;
  margin-bottom: 20px;
}

.card-title {
  font-weight: 600;
}

.faq-a {
  color: #4b5563;
  line-height: 1.8;
  margin: 0;
  white-space: pre-wrap;
}
</style>
