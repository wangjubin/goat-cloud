param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$Username = 'admin',
    [string]$Password = 'Admin@123456'
)

$ErrorActionPreference = 'Stop'

function Write-Step {
    param(
        [string]$Message
    )
    Write-Host "==> $Message"
}

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

    return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Depth 10)
}

Write-Step "Checking health at $BaseUrl"
$health = Invoke-RestMethod -Method Get -Uri "$BaseUrl/actuator/health"
if ($health.status -ne 'UP') {
    throw "Backend health check failed: $($health | ConvertTo-Json -Depth 5)"
}

Write-Step "Logging in as $Username"
$login = Invoke-Api -Method Post -Uri "$BaseUrl/api/auth/login" -Body @{
    username = $Username
    password = $Password
}

if ($login.code -ne 0 -or [string]::IsNullOrWhiteSpace($login.data.accessToken)) {
    throw "Login failed: $($login | ConvertTo-Json -Depth 10)"
}

$headers = @{ Authorization = $login.data.accessToken }

$checks = @(
    @{ name = 'auth.profile'; method = 'GET'; path = '/api/auth/profile' },
    @{ name = 'system.user.page'; method = 'POST'; path = '/api/system/user/page'; body = @{} },
    @{ name = 'system.role.page'; method = 'POST'; path = '/api/system/role/page'; body = @{} },
    @{ name = 'system.dept.page'; method = 'POST'; path = '/api/system/dept/page'; body = @{} },
    @{ name = 'system.dept.tree.get'; method = 'GET'; path = '/api/system/dept/tree' },
    @{ name = 'system.dept.tree.post'; method = 'POST'; path = '/api/system/dept/tree'; body = @{} },
    @{ name = 'system.menu.tree'; method = 'GET'; path = '/api/system/menu/tree' },
    @{ name = 'ai.overview'; method = 'GET'; path = '/api/ai/overview' },
    @{ name = 'ai.chat'; method = 'POST'; path = '/api/ai/chat'; body = @{ message = 'api smoke'; modelCode = 'general-chat' } },
    @{ name = 'ai.models.list'; method = 'GET'; path = '/api/ai/models/list' },
    @{ name = 'ai.vector-configs.list'; method = 'GET'; path = '/api/ai/vector-configs/list' },
    @{ name = 'ai.prompts.list'; method = 'GET'; path = '/api/ai/prompts/list' },
    @{ name = 'ai.billing.list'; method = 'GET'; path = '/api/ai/billing/list' },
    @{ name = 'ai.knowledge-bases.list'; method = 'GET'; path = '/api/ai/knowledge-bases/list' },
    @{ name = 'ai.documents.list'; method = 'GET'; path = '/api/ai/documents/list' },
    @{ name = 'ai.chunks.list'; method = 'GET'; path = '/api/ai/chunks/list' },
    @{ name = 'ai.mcp-tools.list'; method = 'GET'; path = '/api/ai/mcp-tools/list' },
    @{ name = 'ai.api-skills.list'; method = 'GET'; path = '/api/ai/api-skills/list' },
    @{ name = 'ai.chatbi.datasources.list'; method = 'GET'; path = '/api/ai/chatbi/datasources/list' },
    @{ name = 'ai.chatbi.tables.list'; method = 'GET'; path = '/api/ai/chatbi/tables/list' },
    @{ name = 'ai.chatbi.datasets.list'; method = 'GET'; path = '/api/ai/chatbi/datasets/list' },
    @{ name = 'ai.chatbi.terms.list'; method = 'GET'; path = '/api/ai/chatbi/terms/list' },
    @{ name = 'ai.agents.list'; method = 'GET'; path = '/api/ai/agents/list' },
    @{ name = 'ai.workflows.list'; method = 'GET'; path = '/api/ai/workflows/list' }
)

$results = foreach ($check in $checks) {
    try {
        $response = Invoke-Api -Method $check.method -Uri "$BaseUrl$($check.path)" -Headers $headers -Body $check.body
        [pscustomobject]@{
            Name    = $check.name
            Code    = $response.code
            Success = ($response.code -eq 0)
            Message = $response.message
        }
    }
    catch {
        [pscustomobject]@{
            Name    = $check.name
            Code    = 'ERR'
            Success = $false
            Message = $_.Exception.Message
        }
    }
}

$failed = $results | Where-Object { -not $_.Success }

Write-Host ''
Write-Host 'Smoke Test Summary'
$results | Format-Table -AutoSize

if ($failed) {
    throw "API smoke failed for: $($failed.Name -join ', ')"
}

Write-Host ''
Write-Host 'All API smoke checks passed.'
