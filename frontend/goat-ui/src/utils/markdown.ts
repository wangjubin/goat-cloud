import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

// 创建 markdown-it 实例
const md = new MarkdownIt({
  html: false, // 禁用 HTML 标签以防止 XSS
  breaks: true, // 转换换行符为 <br>
  linkify: true, // 自动识别链接
  typographer: true, // 启用智能标点
  highlight: function (str, lang) {
    // 代码高亮
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang }).value}</code></pre>`
      } catch (__) {
        // 忽略错误
      }
    }
    // 使用默认的转义
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  }
})

/**
 * 渲染 Markdown 文本为 HTML
 */
export function renderMarkdown(content: string): string {
  if (!content) return ''
  return md.render(content)
}

/**
 * 清理 Markdown 文本（移除 Markdown 标记）
 */
export function stripMarkdown(content: string): string {
  if (!content) return ''
  return content
    .replace(/[*_~`]/g, '') // 移除强调符号
    .replace(/#{1,6}\s/g, '') // 移除标题标记
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1') // 移除链接，保留文本
    .replace(/!\[([^\]]*)\]\([^)]+\)/g, '$1') // 移除图片，保留 alt 文本
}
