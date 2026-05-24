import {http} from './client'
import type {PageResponse} from '@/types/auth'

export interface AiPageQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  status?: string
  [key: string]: unknown
}

export interface AiChatMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
}

export interface AiChatPayload {
  modelId?: string | number
  knowledgeId?: string | number
  messages: AiChatMessage[]
}

export interface AiChatResponse {
  conversationId: string
  provider: string
  modelCode: string
  message: AiChatMessage
  finishReason: string
  mock: boolean
  usage?: {
    promptTokens: number
    completionTokens: number
    totalTokens: number
  }
  metadata?: Record<string, unknown>
  createdAt?: string
}

export interface AiAgentRunPayload {
  message: string
  options?: {
    useRag?: boolean
    useChatBi?: boolean
    topK?: number
    [key: string]: unknown
  }
}

export interface AiAgentRunResponse {
  agentCode?: string
  agentName?: string
  answer?: string
  summary?: string
  chat?: AiChatResponse
  model?: string
  modelCode?: string
  finishReason?: string
  usage?: AiChatResponse['usage']
  ragReferences?: unknown[]
  references?: unknown[]
  citations?: unknown[]
  toolResults?: unknown[]
  plan?: unknown
  traces?: unknown[]
  metadata?: Record<string, unknown>
  [key: string]: unknown
}

export function fetchAiPage<T = Record<string, unknown>>(resource: string, payload: AiPageQuery = {}) {
  return http.post<any, PageResponse<T>>(`/ai/${resource}/page`, payload)
}

export function fetchAiList<T = Record<string, unknown>>(resource: string, payload: Record<string, unknown> = {}) {
  return http.get<any, T[]>(`/ai/${resource}/list`, {params: payload})
}

export function fetchAiDetail<T = Record<string, unknown>>(resource: string, id: string | number) {
  return http.get<any, T>(`/ai/${resource}/${id}`)
}

export function saveAiResource<T = Record<string, unknown>>(resource: string, payload: Record<string, unknown>) {
  return http.post<any, T>(`/ai/${resource}/save`, payload)
}

export function deleteAiResource(resource: string, ids: Array<string | number>) {
  return http.post<any, void>(`/ai/${resource}/delete`, {ids})
}

export function sendAiChat(payload: AiChatPayload) {
  return http.post<any, AiChatResponse>('/ai/chat', payload)
}

export function runAiAgent(agentId: string | number, payload: AiAgentRunPayload) {
  return http.post<any, AiAgentRunResponse>(`/ai/agents/${agentId}/run`, payload)
}

export function fetchAiOverview() {
  return http.get<any, Record<string, unknown>>('/ai/overview')
}

export function uploadAiDocument(formData: FormData) {
  return http.post<any, { documentId: number; documentName: string; parseStatus: string; chunkCount: number; message: string }>('/ai/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function fetchKnowledgeBases() {
  return http.post<any, { records: Array<{ knowledgeBaseId: number; knowledgeBaseName: string }> }>('/ai/knowledge-bases/page', { pageNum: 1, pageSize: 100 })
}

// ========== ChatBI 智能问数 ==========

export interface ChatBiStreamEvent {
  event: string
  data: Record<string, unknown>
}

export interface ChatBiRequest {
  question: string
  datasourceId?: number
  graphCode?: string
}

export function chatBiStream(request: ChatBiRequest): EventSource | null {
  // ChatBI 使用 POST SSE，需要用 fetch + ReadableStream 方式
  return null
}

export async function chatBiStreamFetch(
  request: ChatBiRequest,
  onEvent: (event: string, data: Record<string, unknown>) => void,
  onError?: (error: string) => void,
  signal?: AbortSignal
) {
  const token = localStorage.getItem('access_token') || ''
  const response = await fetch('/api/ai/chatbi/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify(request),
    signal,
  })

  if (!response.ok) {
    onError?.(`HTTP ${response.status}`)
    return
  }

  const reader = response.body?.getReader()
  if (!reader) return

  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    let currentEvent = ''
    for (const line of lines) {
      if (line.startsWith('event: ')) {
        currentEvent = line.substring(7).trim()
      } else if (line.startsWith('data: ') && currentEvent) {
        try {
          const data = JSON.parse(line.substring(6))
          onEvent(currentEvent, data)
        } catch {
          // ignore parse errors
        }
        currentEvent = ''
      }
    }
  }
}

export function resumeChatBiSession(runId: string, feedback: Record<string, unknown>) {
  return http.post<any, Record<string, unknown>>('/ai/chatbi/chat/resume', { runId, feedback })
}

export function getChatBiSession(runId: string) {
  return http.get<any, Record<string, unknown>>(`/ai/chatbi/chat/session/${runId}`)
}
