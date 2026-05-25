<template>
  <div class="column-editor">
    <div class="column-editor-header">
      <el-button size="small" @click="addRow">添加字段</el-button>
    </div>
    <el-table :data="rows" border size="small" style="width: 100%">
      <el-table-column label="字段名" width="180">
        <template #default="{ row }">
          <el-input v-model="row.columnName" size="small" placeholder="字段名" />
        </template>
      </el-table-column>
      <el-table-column label="类型" width="140">
        <template #default="{ row }">
          <el-input v-model="row.dataType" size="small" placeholder="类型" />
        </template>
      </el-table-column>
      <el-table-column label="注释" min-width="180">
        <template #default="{ row }">
          <el-input v-model="row.columnComment" size="small" placeholder="注释" />
        </template>
      </el-table-column>
      <el-table-column label="可空" width="70">
        <template #default="{ row }">
          <el-switch v-model="row.nullable" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="60" fixed="right">
        <template #default="{ $index }">
          <el-button text type="danger" size="small" @click="removeRow($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

interface ColumnRow {
  columnName: string
  dataType: string
  columnComment: string
  nullable: boolean
  columnSize?: number
  ordinalPosition?: number
}

const rows = ref<ColumnRow[]>([])

function parseJson(json: string): ColumnRow[] {
  if (!json || json.trim() === '') return []
  try {
    const parsed = JSON.parse(json)
    if (Array.isArray(parsed)) {
      return parsed.map((col: any) => ({
        columnName: col.columnName || '',
        dataType: col.dataType || '',
        columnComment: col.columnComment || '',
        nullable: col.nullable ?? true,
        columnSize: col.columnSize,
        ordinalPosition: col.ordinalPosition,
      }))
    }
    return []
  } catch {
    return []
  }
}

function toJson(rows: ColumnRow[]): string {
  return JSON.stringify(rows.map((row, index) => ({
    columnName: row.columnName,
    dataType: row.dataType,
    columnComment: row.columnComment,
    nullable: row.nullable,
    columnSize: row.columnSize,
    ordinalPosition: row.ordinalPosition ?? index + 1,
  })))
}

function addRow() {
  rows.value.push({ columnName: '', dataType: '', columnComment: '', nullable: true })
}

function removeRow(index: number) {
  rows.value.splice(index, 1)
}

onMounted(() => {
  rows.value = parseJson(props.modelValue)
})

watch(() => props.modelValue, (newVal) => {
  rows.value = parseJson(newVal)
})

watch(rows, () => {
  emit('update:modelValue', toJson(rows.value))
}, { deep: true })
</script>

<style scoped>
.column-editor-header {
  margin-bottom: 8px;
}
</style>