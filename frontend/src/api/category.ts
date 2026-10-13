import { get } from './request'

/** 分类树节点 */
export interface CategoryNode {
  id: number
  parentId: number | null
  name: string
  level: number
  sort: number
  children: CategoryNode[]
}

/** 获取三级分类树（领域—方向—知识点） */
export function getCategoryTreeApi() {
  return get<CategoryNode[]>('/categories/tree')
}
