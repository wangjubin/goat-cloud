<template>
  <div class="workflow-container">
    <!-- 顶部工具栏 -->
    <div class="workflow-toolbar">
      <div class="toolbar-left">
        <el-button @click="loadWorkflows" :icon="Refresh" size="small">刷新</el-button>
        <el-button type="primary" @click="createWorkflow" :icon="Plus" size="small">新建工作流</el-button>
      </div>
      <div class="toolbar-right">
        <el-select v-model="filterStatus" placeholder="状态" clearable size="small" style="width: 120px" @change="loadWorkflows">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="ACTIVE" />
        </el-select>
        <el-select v-model="filterType" placeholder="类型" clearable size="small" style="width: 120px" @change="loadWorkflows">
          <el-option label="顺序" value="SEQUENTIAL" />
          <el-option label="DAG" value="DAG" />
          <el-option label="并行" value="PARALLEL" />
        </el-select>
      </div>
    </div>

    <!-- 工作流列表 -->
    <el-table :data="workflows" border stripe size="small" v-loading="loading">
      <el-table-column prop="graphCode" label="编码" width="180" />
      <el-table-column prop="graphName" label="名称" min-width="160" />
      <el-table-column prop="graphType" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="typeTagMap[row.graphType] || 'info'" size="small">{{ typeLabelMap[row.graphType] || row.graphType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'warning'" size="small">{{ row.status === 'ACTIVE' ? '已发布' : '草稿' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="editWorkflow(row)">编排</el-button>
          <el-button v-if="row.status === 'DRAFT'" size="small" text type="success" @click="publishWorkflow(row)">发布</el-button>
          <el-button v-if="row.status === 'ACTIVE'" size="small" text type="warning" @click="unpublishWorkflow(row)">下线</el-button>
          <el-button size="small" text @click="validateWorkflow(row)">验证</el-button>
          <el-button size="small" text type="info" @click="viewNodes(row)">节点</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建/编辑工作流对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingGraph?.graphId ? '编辑工作流' : '新建工作流'" width="600px">
      <el-form :model="editingGraph" label-width="80px">
        <el-form-item label="编码" required>
          <el-input v-model="editingGraph.graphCode" placeholder="如 chatbi_default" :disabled="!!editingGraph.graphId" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="editingGraph.graphName" placeholder="如 ChatBI 默认问数流程" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="editingGraph.graphType" style="width: 100%">
            <el-option label="顺序执行" value="SEQUENTIAL" />
            <el-option label="DAG 工作流" value="DAG" />
            <el-option label="并行执行" value="PARALLEL" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="editingGraph.version" placeholder="1.0.0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editingGraph.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveWorkflow">保存</el-button>
      </template>
    </el-dialog>

    <!-- 节点编排对话框 -->
    <el-dialog v-model="nodeDialogVisible" :title="'节点编排 - ' + (currentGraph?.graphName || '')" width="95%" top="3vh">
      <div class="node-editor">
        <!-- 可视化流程图 -->
        <div class="visual-graph-container">
          <div class="graph-header">
            <span class="graph-title">流程可视化</span>
            <el-button size="small" @click="refreshGraphLayout">刷新布局</el-button>
          </div>
          <div class="visual-graph" ref="graphContainer">
            <svg :width="graphWidth" :height="graphHeight" class="graph-svg">
              <!-- 连线 -->
              <defs>
                <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
                  <polygon points="0 0, 10 3.5, 0 7" fill="#909399" />
                </marker>
              </defs>
              <g v-for="(edge, edgeIndex) in graphEdges" :key="'edge-' + edgeIndex">
                <path
                  :d="edge.path"
                  fill="none"
                  stroke="#909399"
                  stroke-width="2"
                  marker-end="url(#arrowhead)"
                />
                <text v-if="edge.condition" :x="edge.labelX" :y="edge.labelY" class="edge-label">
                  {{ edge.condition }}
                </text>
              </g>
              <!-- 节点 -->
              <g v-for="(node, nodeIndex) in graphNodes" :key="'node-' + nodeIndex">
                <rect
                  :x="node.x"
                  :y="node.y"
                  :width="nodeWidth"
                  :height="nodeHeight"
                  :rx="node.nodeType === 'START' || node.nodeType === 'END' ? nodeHeight / 2 : 6"
                  :fill="getNodeColor(node.nodeType)"
                  :stroke="getNodeStroke(node.nodeType)"
                  stroke-width="2"
                  class="node-rect"
                  @click="editNode(node, nodeIndex)"
                />
                <text :x="node.x + nodeWidth / 2" :y="node.y + nodeHeight / 2 - 6" text-anchor="middle" class="node-label">
                  {{ node.nodeName || node.nodeCode }}
                </text>
                <text :x="node.x + nodeWidth / 2" :y="node.y + nodeHeight / 2 + 10" text-anchor="middle" class="node-type-label">
                  {{ nodeTypeLabel[node.nodeType] || node.nodeType }}
                </text>
              </g>
            </svg>
          </div>
        </div>

        <!-- 节点列表 -->
        <div class="node-toolbar">
          <el-button type="primary" size="small" :icon="Plus" @click="addNode">添加节点</el-button>
          <el-button size="small" :icon="Connection" @click="addEdge">添加连线</el-button>
          <span class="node-count">共 {{ nodes.length }} 个节点，{{ edges.length }} 条连线</span>
        </div>
        <el-table :data="nodes" border size="small" max-height="300">
          <el-table-column prop="nodeCode" label="编码" width="160" />
          <el-table-column prop="nodeName" label="名称" width="140" />
          <el-table-column prop="nodeType" label="类型" width="160">
            <template #default="{ row }">
              <el-tag :type="nodeTypeTag[row.nodeType] || 'info'" size="small">{{ nodeTypeLabel[row.nodeType] || row.nodeType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="60" />
          <el-table-column label="出边" min-width="200">
            <template #default="{ row }">
              <span v-if="getOutgoingEdges(row.nodeCode).length">
                <el-tag v-for="edge in getOutgoingEdges(row.nodeCode)" :key="edge.to" size="small" class="edge-tag">
                  → {{ edge.to }}{{ edge.condition ? ' [' + edge.condition + ']' : '' }}
                </el-tag>
              </span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="超时(ms)" width="90">
            <template #default="{ row }">{{ row.timeoutMs || 30000 }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="{ row, $index }">
              <el-button size="small" text @click="editNode(row, $index)">编辑</el-button>
              <el-button size="small" text type="danger" @click="removeNode($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- 节点编辑对话框 -->
    <el-dialog v-model="nodeEditVisible" :title="editingNode?.nodeId ? '编辑节点' : '添加节点'" width="600px">
      <el-form :model="editingNode" label-width="80px">
        <el-form-item label="编码" required>
          <el-input v-model="editingNode.nodeCode" placeholder="如 intent_recognition" :disabled="!!editingNode.nodeId" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="editingNode.nodeName" placeholder="如 意图识别" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="editingNode.nodeType" style="width: 100%">
            <el-option label="起始" value="START" />
            <el-option label="终止" value="END" />
            <el-option label="网关" value="GATEWAY" />
            <el-option label="意图识别" value="INTENT_RECOGNITION" />
            <el-option label="Schema召回" value="SCHEMA_RECALL" />
            <el-option label="NL2SQL" value="NL2SQL" />
            <el-option label="SQL执行" value="SQL_EXECUTION" />
            <el-option label="人工反馈" value="HUMAN_FEEDBACK" />
            <el-option label="Python执行" value="PYTHON_EXECUTION" />
            <el-option label="报告生成" value="REPORT_GENERATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="editingNode.sortOrder" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="超时(ms)">
          <el-input-number v-model="editingNode.timeoutMs" :min="1000" :max="300000" :step="5000" />
        </el-form-item>
        <el-form-item label="配置JSON">
          <el-input v-model="editingNode.configJson" type="textarea" :rows="3" placeholder='{"key":"value"}' />
        </el-form-item>
        <el-form-item label="出边">
          <el-input v-model="editingNode._outgoing" placeholder="目标节点编码，逗号分隔，如 schema_recall,nl2sql" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nodeEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNode">保存</el-button>
      </template>
    </el-dialog>

    <!-- 连线对话框 -->
    <el-dialog v-model="edgeDialogVisible" title="添加连线" width="500px">
      <el-form label-width="80px">
        <el-form-item label="源节点">
          <el-select v-model="edgeFrom" style="width: 100%">
            <el-option v-for="n in nodes" :key="n.nodeCode" :label="n.nodeName + '(' + n.nodeCode + ')'" :value="n.nodeCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标节点">
          <el-select v-model="edgeTo" style="width: 100%">
            <el-option v-for="n in nodes" :key="n.nodeCode" :label="n.nodeName + '(' + n.nodeCode + ')'" :value="n.nodeCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="条件">
          <el-input v-model="edgeCondition" placeholder="可选，如 intent==TREND_ANALYSIS" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="edgeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdge">添加</el-button>
      </template>
    </el-dialog>

    <!-- 验证结果对话框 -->
    <el-dialog v-model="validateDialogVisible" title="工作流验证结果" width="500px">
      <el-result v-if="validateResult?.valid" icon="success" title="验证通过" />
      <el-result v-else icon="error" title="验证失败" />
      <div v-if="validateResult">
        <div v-if="validateResult.errors?.length">
          <h4>错误</h4>
          <ul>
            <li v-for="e in validateResult.errors" :key="e" class="error-text">{{ e }}</li>
          </ul>
        </div>
        <div v-if="validateResult.warnings?.length">
          <h4>警告</h4>
          <ul>
            <li v-for="w in validateResult.warnings" :key="w" class="warning-text">{{ w }}</li>
          </ul>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Refresh, Plus, Connection } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/api/client'

const loading = ref(false)
const workflows = ref<any[]>([])
const filterStatus = ref('')
const filterType = ref('')

const dialogVisible = ref(false)
const editingGraph = ref<any>({})

const nodeDialogVisible = ref(false)
const currentGraph = ref<any>(null)
const nodes = ref<any[]>([])
const edges = ref<Array<{ from: string; to: string; condition?: string }>>([])

const nodeEditVisible = ref(false)
const editingNode = ref<any>({})
const editingNodeIndex = ref(-1)

const edgeDialogVisible = ref(false)
const edgeFrom = ref('')
const edgeTo = ref('')
const edgeCondition = ref('')

const validateDialogVisible = ref(false)
const validateResult = ref<any>(null)

const graphContainer = ref<HTMLElement>()
const nodeWidth = 140
const nodeHeight = 50
const nodeGap = 40

const typeLabelMap: Record<string, string> = { SEQUENTIAL: '顺序', DAG: 'DAG', PARALLEL: '并行' }
const typeTagMap: Record<string, string> = { SEQUENTIAL: '', DAG: 'success', PARALLEL: 'warning' }
const nodeTypeLabel: Record<string, string> = {
  START: '起始', END: '终止', GATEWAY: '网关',
  INTENT_RECOGNITION: '意图识别', SCHEMA_RECALL: 'Schema召回',
  NL2SQL: 'NL2SQL', SQL_EXECUTION: 'SQL执行',
  HUMAN_FEEDBACK: '人工反馈', PYTHON_EXECUTION: 'Python执行',
  REPORT_GENERATION: '报告生成'
}
const nodeTypeTag: Record<string, string> = {
  START: 'success', END: 'danger', GATEWAY: 'warning',
  INTENT_RECOGNITION: '', SCHEMA_RECALL: '',
  NL2SQL: 'primary', SQL_EXECUTION: 'primary',
  HUMAN_FEEDBACK: 'warning', PYTHON_EXECUTION: '',
  REPORT_GENERATION: 'success'
}

// Graph layout computed properties
const graphWidth = computed(() => {
  if (nodes.value.length === 0) return 400
  const maxX = Math.max(...graphNodes.value.map(n => n.x)) + nodeWidth + 60
  return Math.max(400, maxX)
})

const graphHeight = computed(() => {
  if (nodes.value.length === 0) return 200
  const maxY = Math.max(...graphNodes.value.map(n => n.y)) + nodeHeight + 60
  return Math.max(200, maxY)
})

const graphNodes = computed(() => {
  const result: Array<{ x: number; y: number; nodeCode: string; nodeName: string; nodeType: string; sortOrder: number }> = []
  const sorted = [...nodes.value].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))

  sorted.forEach((node, index) => {
    const col = index % 4
    const row = Math.floor(index / 4)
    result.push({
      x: 40 + col * (nodeWidth + nodeGap),
      y: 40 + row * (nodeHeight + nodeGap),
      nodeCode: node.nodeCode,
      nodeName: node.nodeName,
      nodeType: node.nodeType,
      sortOrder: node.sortOrder || 0,
    })
  })
  return result
})

const graphEdges = computed(() => {
  const result: Array<{ path: string; condition?: string; labelX: number; labelY: number }> = []
  const nodeMap = new Map(graphNodes.value.map(n => [n.nodeCode, n]))

  edges.value.forEach(edge => {
    const fromNode = nodeMap.get(edge.from)
    const toNode = nodeMap.get(edge.to)
    if (!fromNode || !toNode) return

    const fromX = fromNode.x + nodeWidth
    const fromY = fromNode.y + nodeHeight / 2
    const toX = toNode.x
    const toY = toNode.y + nodeHeight / 2

    const midX = (fromX + toX) / 2
    const path = `M ${fromX} ${fromY} C ${midX} ${fromY}, ${midX} ${toY}, ${toX} ${toY}`

    result.push({
      path,
      condition: edge.condition,
      labelX: midX,
      labelY: (fromY + toY) / 2 - 8,
    })
  })
  return result
})

function getNodeColor(nodeType: string): string {
  const colors: Record<string, string> = {
    START: '#67c23a',
    END: '#f56c6c',
    GATEWAY: '#e6a23c',
    NL2SQL: '#409eff',
    SQL_EXECUTION: '#409eff',
    INTENT_RECOGNITION: '#909399',
    SCHEMA_RECALL: '#909399',
    HUMAN_FEEDBACK: '#e6a23c',
    PYTHON_EXECUTION: '#909399',
    REPORT_GENERATION: '#67c23a',
  }
  return colors[nodeType] || '#dcdfe6'
}

function getNodeStroke(nodeType: string): string {
  const strokes: Record<string, string> = {
    START: '#529b2e',
    END: '#c45656',
    GATEWAY: '#cf9236',
    NL2SQL: '#337ecc',
    SQL_EXECUTION: '#337ecc',
  }
  return strokes[nodeType] || '#c0c4cc'
}

function getOutgoingEdges(nodeCode: string) {
  return edges.value.filter(e => e.from === nodeCode)
}

function refreshGraphLayout() {
  // Force reactivity update
  edges.value = [...edges.value]
}

onMounted(() => loadWorkflows())

async function loadWorkflows() {
  loading.value = true
  try {
    const params: any = {}
    if (filterStatus.value) params.status = filterStatus.value
    if (filterType.value) params.graphType = filterType.value
    const res = await http.get('/ai/chatbi/workflow/list', { params })
    workflows.value = res || []
  } catch (e: any) {
    ElMessage.error('加载失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

function createWorkflow() {
  editingGraph.value = { graphType: 'DAG', version: '1.0.0' }
  dialogVisible.value = true
}

async function editWorkflow(row: any) {
  editingGraph.value = { ...row }
  dialogVisible.value = true
}

async function saveWorkflow() {
  try {
    if (editingGraph.value.graphId) {
      await http.put(`/ai/chatbi/stategraphs`, editingGraph.value)
    } else {
      await http.post(`/ai/chatbi/stategraphs`, editingGraph.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadWorkflows()
  } catch (e: any) {
    ElMessage.error('保存失败: ' + e.message)
  }
}

async function publishWorkflow(row: any) {
  try {
    await http.post(`/ai/chatbi/workflow/${row.graphId}/publish`)
    ElMessage.success('发布成功')
    loadWorkflows()
  } catch (e: any) {
    ElMessage.error('发布失败: ' + e.message)
  }
}

async function unpublishWorkflow(row: any) {
  try {
    await http.post(`/ai/chatbi/workflow/${row.graphId}/unpublish`)
    ElMessage.success('已下线')
    loadWorkflows()
  } catch (e: any) {
    ElMessage.error('下线失败: ' + e.message)
  }
}

async function validateWorkflow(row: any) {
  try {
    const res = await http.post(`/ai/chatbi/workflow/${row.graphId}/validate`)
    validateResult.value = res
    validateDialogVisible.value = true
  } catch (e: any) {
    ElMessage.error('验证失败: ' + e.message)
  }
}

async function viewNodes(row: any) {
  currentGraph.value = row
  try {
    const res = await http.get(`/ai/chatbi/workflow/${row.graphId}/definition`)
    nodes.value = (res?.nodes || []).map((n: any) => ({
      ...n,
      _outgoing: parseOutgoing(n.edgesJson)
    }))
    // Parse edges from nodes
    edges.value = []
    nodes.value.forEach((node: any) => {
      if (node.edgesJson) {
        try {
          const edgeData = JSON.parse(node.edgesJson)
          const outgoing = edgeData.outgoing || []
          outgoing.forEach((e: any) => {
            const to = typeof e === 'string' ? e : e.to
            const condition = typeof e === 'object' ? e.condition : undefined
            if (to) {
              edges.value.push({ from: node.nodeCode, to, condition })
            }
          })
        } catch {}
      }
    })
    nodeDialogVisible.value = true
  } catch (e: any) {
    ElMessage.error('加载节点失败: ' + e.message)
  }
}

function parseOutgoing(edgesJson: string): string {
  if (!edgesJson) return ''
  try {
    const edges = JSON.parse(edgesJson)
    const outgoing = edges.outgoing || []
    return outgoing.map((e: any) => typeof e === 'string' ? e : `${e.to}${e.condition ? '[' + e.condition + ']' : ''}`).join(', ')
  } catch {
    return ''
  }
}

function addNode() {
  editingNode.value = { sortOrder: nodes.value.length + 1, timeoutMs: 30000 }
  editingNodeIndex.value = -1
  nodeEditVisible.value = true
}

function editNode(row: any, index: number) {
  editingNode.value = { ...row }
  editingNodeIndex.value = index
  nodeEditVisible.value = true
}

function removeNode(index: number) {
  nodes.value.splice(index, 1)
}

function saveNode() {
  if (editingNodeIndex.value >= 0) {
    nodes.value[editingNodeIndex.value] = { ...editingNode.value }
  } else {
    nodes.value.push({ ...editingNode.value })
  }
  nodeEditVisible.value = false
}

function addEdge() {
  edgeFrom.value = ''
  edgeTo.value = ''
  edgeCondition.value = ''
  edgeDialogVisible.value = true
}

function saveEdge() {
  if (!edgeFrom.value || !edgeTo.value) {
    ElMessage.warning('请选择源节点和目标节点')
    return
  }
  const sourceNode = nodes.value.find(n => n.nodeCode === edgeFrom.value)
  if (sourceNode) {
    const existing = sourceNode._outgoing ? sourceNode._outgoing.split(', ') : []
    const newEdge = edgeCondition.value ? `${edgeTo.value}[${edgeCondition.value}]` : edgeTo.value
    existing.push(newEdge)
    sourceNode._outgoing = existing.join(', ')
  }
  edgeDialogVisible.value = false
  ElMessage.success('连线已添加')
}
</script>

<style scoped>
.workflow-container {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
}

.workflow-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-left, .toolbar-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

.node-editor {
  min-height: 300px;
}

.node-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.node-count {
  font-size: 12px;
  color: #909399;
}

.text-muted {
  color: #c0c4cc;
}

.error-text {
  color: #f56c6c;
}

.warning-text {
  color: #e6a23c;
}

/* Visual Graph Styles */
.visual-graph-container {
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.graph-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.graph-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}

.visual-graph {
  padding: 20px;
  overflow: auto;
  background: #fafafa;
  min-height: 200px;
}

.graph-svg {
  display: block;
}

.node-rect {
  cursor: pointer;
  transition: filter 0.2s;
}

.node-rect:hover {
  filter: brightness(0.9);
}

.node-label {
  font-size: 12px;
  font-weight: 600;
  fill: #303133;
  pointer-events: none;
}

.node-type-label {
  font-size: 10px;
  fill: #606266;
  pointer-events: none;
}

.edge-label {
  font-size: 10px;
  fill: #909399;
  text-anchor: middle;
}

.edge-tag {
  margin: 2px 4px 2px 0;
}
</style>
