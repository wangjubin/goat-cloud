param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$Username = 'admin',
    [string]$Password = 'Admin@123456'
)

$ErrorActionPreference = 'Stop'

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers = @{},
        [object]$Body = $null
    )

    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers
    }

    return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Depth 30)
}

function Assert-Ok {
    param(
        [string]$Name,
        [object]$Response
    )

    if ($Response.code -ne 0) {
        throw "$Name failed: $($Response | ConvertTo-Json -Depth 30)"
    }
}

function Assert-True {
    param(
        [string]$Name,
        [bool]$Condition,
        [object]$Actual
    )

    if (-not $Condition) {
        throw "$Name assertion failed: $($Actual | ConvertTo-Json -Depth 30)"
    }
}

$login = Invoke-Api -Method Post -Uri "$BaseUrl/api/auth/login" -Body @{
    username = $Username
    password = $Password
}
Assert-Ok 'auth.login' $login
$headers = @{ Authorization = $login.data.accessToken }

$checks = New-Object System.Collections.Generic.List[object]

$rag = Invoke-Api -Method Post -Uri "$BaseUrl/api/ai/rag/search" -Headers $headers -Body @{
    query = 'PostgreSQL Redis health status'
    topK = 3
    includeContent = $true
}
Assert-Ok 'rag.search' $rag
Assert-True 'rag.search.hits' (@($rag.data.hits).Count -gt 0) $rag.data
$checks.Add([pscustomobject]@{ Name = 'rag.search'; Evidence = "hits=$(@($rag.data.hits).Count), firstChunkId=$($rag.data.hits[0].chunkId)" })

$chat = Invoke-Api -Method Post -Uri "$BaseUrl/api/ai/chat" -Headers $headers -Body @{
    modelCode = 'general-chat'
    message = 'When service is unavailable, which components should be checked first?'
    options = @{ topK = 3; bizType = 'CHAT_SMOKE' }
}
Assert-Ok 'chat.with-rag' $chat
Assert-True 'chat.with-rag.usage' ($chat.data.usage.totalTokens -gt 0) $chat.data
Assert-True 'chat.with-rag.rag-metadata' ($chat.data.metadata.rag.hitCount -ge 0) $chat.data.metadata
$checks.Add([pscustomobject]@{ Name = 'chat.with-rag'; Evidence = "mock=$($chat.data.mock), tokens=$($chat.data.usage.totalTokens), ragHits=$($chat.data.metadata.rag.hitCount)" })

$chatBi = Invoke-Api -Method Post -Uri "$BaseUrl/api/ai/chatbi/ask" -Headers $headers -Body @{
    question = 'count active users'
    datasetCode = 'system-user-analysis'
    limit = 50
}
Assert-Ok 'chatbi.ask' $chatBi
Assert-True 'chatbi.ask.sql' (-not [string]::IsNullOrWhiteSpace($chatBi.data.candidateSql)) $chatBi.data
Assert-True 'chatbi.ask.safe' ($chatBi.data.executable -eq $false) $chatBi.data
$checks.Add([pscustomobject]@{ Name = 'chatbi.ask'; Evidence = "sql=$($chatBi.data.candidateSql)" })

$agent = Invoke-Api -Method Post -Uri "$BaseUrl/api/ai/agents/1/run" -Headers $headers -Body @{
    message = 'count and 查询 login health status'
    options = @{ topK = 2; useChatBi = $true }
}
Assert-Ok 'agent.run' $agent
Assert-True 'agent.run.chat' (-not [string]::IsNullOrWhiteSpace($agent.data.chat.message.content)) $agent.data
Assert-True 'agent.run.plan' (@($agent.data.metadata.plan).Count -ge 4) $agent.data.metadata
Assert-True 'agent.run.toolResults' (@($agent.data.metadata.toolResults).Count -gt 0) $agent.data.metadata
Assert-True 'agent.run.rag-hits' ($null -ne $agent.data.metadata.rag.hits) $agent.data.metadata.rag
Assert-True 'agent.run.chatbi' (-not [string]::IsNullOrWhiteSpace($agent.data.metadata.chatBi.candidateSql)) $agent.data.metadata.chatBi
$checks.Add([pscustomobject]@{ Name = 'agent.run'; Evidence = "agent=$($agent.data.agentCode), plan=$(@($agent.data.metadata.plan).Count), toolResults=$(@($agent.data.metadata.toolResults).Count), ragHits=$(@($agent.data.metadata.rag.hits).Count), chatBiSql=$($agent.data.metadata.chatBi.candidateSql)" })

$workflow = Invoke-Api -Method Post -Uri "$BaseUrl/api/ai/workflows/1/run" -Headers $headers -Body @{
    message = 'run daily inspection and summarize'
    input = @{ source = 'ai-runtime-smoke' }
}
Assert-Ok 'workflow.run' $workflow
Assert-True 'workflow.run.traces' (@($workflow.data.traces).Count -gt 0) $workflow.data
Assert-True 'workflow.run.summary' (-not [string]::IsNullOrWhiteSpace($workflow.data.summary)) $workflow.data
$checks.Add([pscustomobject]@{ Name = 'workflow.run'; Evidence = "status=$($workflow.data.status), traces=$(@($workflow.data.traces).Count)" })

Write-Host ''
Write-Host 'AI Runtime Smoke Summary'
$checks | Format-Table -AutoSize
Write-Host ''
Write-Host 'All AI runtime smoke checks passed.'
