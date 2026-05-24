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

    [pscustomobject]@{
        Name = $Name
        Code = $Response.code
        Success = $true
        Message = $Response.message
    }
}

function Merge-Hashtable {
    param(
        [hashtable]$Base,
        [hashtable]$Overrides
    )

    $merged = @{}
    foreach ($key in $Base.Keys) {
        $merged[$key] = $Base[$key]
    }
    foreach ($key in $Overrides.Keys) {
        $merged[$key] = $Overrides[$key]
    }
    return $merged
}

$stamp = Get-Date -Format 'yyyyMMddHHmmss'
$headers = $null
$results = New-Object System.Collections.Generic.List[object]
$created = New-Object System.Collections.Generic.List[object]

$resources = @(
    @{
        name = 'models'; idKey = 'modelId'; keyword = "smoke-model-$stamp"; matchKey = 'modelCode'; matchValue = "smoke-model-$stamp"
        create = @{ modelName = "Smoke Model $stamp"; modelCode = "smoke-model-$stamp"; provider = 'OpenAI Compatible'; modelType = 'CHAT'; endpoint = 'https://example.com/v1'; apiKeyRef = 'ENV:AI_KEY'; contextWindow = 8192; defaultModel = $false; status = 'ENABLED'; sortOrder = 99; remark = 'ai crud smoke' }
        update = @{ modelName = "Smoke Model $stamp Updated"; contextWindow = 16384; sortOrder = 98; remark = 'ai crud smoke updated' }
    },
    @{
        name = 'vector-configs'; idKey = 'vectorConfigId'; keyword = "Smoke Vector $stamp"; matchKey = 'configName'; matchValue = "Smoke Vector $stamp"
        create = @{ configName = "Smoke Vector $stamp"; provider = 'POSTGRESQL'; embeddingModel = 'text-embedding'; embeddingDimension = 1536; distanceMetric = 'COSINE'; pgvectorTable = 'ai_document_chunk'; indexType = 'IVFFLAT'; chunkSize = 800; chunkOverlap = 120; status = 'ENABLED'; remark = 'ai crud smoke' }
        update = @{ configName = "Smoke Vector $stamp Updated"; embeddingDimension = 1024; indexType = 'HNSW'; chunkSize = 600; chunkOverlap = 80; remark = 'ai crud smoke updated' }
    },
    @{
        name = 'prompts'; idKey = 'promptId'; keyword = "smoke-prompt-$stamp"; matchKey = 'promptCode'; matchValue = "smoke-prompt-$stamp"
        create = @{ promptCode = "smoke-prompt-$stamp"; promptName = "Smoke Prompt $stamp"; promptType = 'ASSISTANT'; systemPrompt = 'You are an enterprise assistant.'; userPrompt = '{{question}}'; variables = 'question'; version = '1.0.0'; status = 'ENABLED'; remark = 'ai crud smoke' }
        update = @{ promptName = "Smoke Prompt $stamp Updated"; systemPrompt = 'You are an enterprise AI assistant.'; variables = 'question,context'; version = '1.0.1'; remark = 'ai crud smoke updated' }
    },
    @{
        name = 'billing'; idKey = 'billingId'; keyword = "smoke-conv-$stamp"; matchKey = 'conversationId'; matchValue = "smoke-conv-$stamp"
        create = @{ conversationId = "smoke-conv-$stamp"; provider = 'OpenAI Compatible'; modelCode = 'general-chat'; bizType = 'AI_ASSISTANT'; promptTokens = 10; completionTokens = 20; totalTokens = 30; costAmount = 0.01; currency = 'CNY'; requestTime = '2026-05-12T22:00:00'; status = 'SUCCESS'; remark = 'ai crud smoke' }
        update = @{ promptTokens = 20; completionTokens = 30; totalTokens = 50; costAmount = 0.02; requestTime = '2026-05-12T22:01:00'; remark = 'ai crud smoke updated' }
    },
    @{
        name = 'knowledge-bases'; idKey = 'knowledgeBaseId'; keyword = "smoke-kb-$stamp"; matchKey = 'knowledgeBaseCode'; matchValue = "smoke-kb-$stamp"
        create = @{ knowledgeBaseCode = "smoke-kb-$stamp"; knowledgeBaseName = "Smoke Knowledge Base $stamp"; description = 'Smoke knowledge base'; vectorConfigId = 1; embeddingModel = 'text-embedding'; embeddingDimension = 1536; documentCount = 0; chunkCount = 0; status = 'ENABLED'; remark = 'ai crud smoke' }
        update = @{ knowledgeBaseName = "Smoke Knowledge Base $stamp Updated"; description = 'Smoke knowledge base updated'; documentCount = 1; chunkCount = 1; remark = 'ai crud smoke updated' }
    },
    @{
        name = 'documents'; idKey = 'documentId'; keyword = "smoke-doc-$stamp"; matchKey = 'documentName'; matchValue = "smoke-doc-$stamp.md"
        create = @{ knowledgeBaseId = 1; documentName = "smoke-doc-$stamp.md"; documentType = 'MARKDOWN'; sourceUri = 'local://smoke.md'; fileSize = 100; parseStatus = 'SUCCESS'; chunkStatus = 'SUCCESS'; metadata = '{"source":"smoke"}'; status = 'ENABLED'; remark = 'ai crud smoke' }
        update = @{ documentName = "smoke-doc-$stamp-updated.md"; fileSize = 120; metadata = '{"source":"smoke-updated"}'; remark = 'ai crud smoke updated' }
    },
    @{
        name = 'chunks'; idKey = 'chunkId'; keyword = "Smoke Chunk $stamp"; matchKey = 'title'; matchValue = "Smoke Chunk $stamp"
        create = @{ knowledgeBaseId = 1; documentId = 1; chunkIndex = 999; title = "Smoke Chunk $stamp"; content = 'Smoke chunk content'; tokenCount = 20; embeddingStatus = 'READY'; embeddingVector = ''; metadata = '{"source":"smoke"}'; status = 'ENABLED'; remark = 'ai crud smoke' }
        update = @{ chunkIndex = 1000; title = "Smoke Chunk $stamp Updated"; content = 'Smoke chunk content updated'; tokenCount = 30; metadata = '{"source":"smoke-updated"}'; remark = 'ai crud smoke updated' }
    },
    @{
        name = 'mcp-tools'; idKey = 'mcpToolId'; keyword = "smoke-tool-$stamp"; matchKey = 'toolCode'; matchValue = "smoke-tool-$stamp"
        create = @{ toolCode = "smoke-tool-$stamp"; toolName = "Smoke MCP Tool $stamp"; serverName = 'smoke-mcp'; transportType = 'HTTP'; endpoint = 'http://localhost:3000/mcp'; inputSchema = '{"query":"string"}'; outputSchema = '{"result":"string"}'; status = 'ENABLED'; remark = 'ai crud smoke' }
        update = @{ toolName = "Smoke MCP Tool $stamp Updated"; remark = 'ai crud smoke updated' }
    },
    @{
        name = 'api-skills'; idKey = 'apiSkillId'; keyword = "smoke-skill-$stamp"; matchKey = 'skillCode'; matchValue = "smoke-skill-$stamp"
        create = @{ skillCode = "smoke-skill-$stamp"; skillName = "Smoke API Skill $stamp"; skillType = 'REST'; endpoint = '/api/smoke'; httpMethod = 'GET'; authType = 'NONE'; requestSchema = '{}'; responseSchema = '{}'; status = 'ENABLED'; remark = 'ai crud smoke' }
        update = @{ skillName = "Smoke API Skill $stamp Updated"; httpMethod = 'POST'; remark = 'ai crud smoke updated' }
    },
    @{
        name = 'chatbi/datasources'; idKey = 'datasourceId'; keyword = "smoke-ds-$stamp"; matchKey = 'datasourceCode'; matchValue = "smoke-ds-$stamp"
        create = @{ datasourceCode = "smoke-ds-$stamp"; datasourceName = "Smoke DataSource $stamp"; datasourceType = 'POSTGRESQL'; jdbcUrl = 'jdbc:postgresql://localhost:5432/goat_cloud'; username = 'postgres'; credentialRef = 'ENV:POSTGRES_PASSWORD'; status = 'ENABLED'; remark = 'ai crud smoke' }
        update = @{ datasourceName = "Smoke DataSource $stamp Updated"; remark = 'ai crud smoke updated' }
    },
    @{
        name = 'chatbi/tables'; idKey = 'tableId'; keyword = "smoke_table_$stamp"; matchKey = 'tableName'; matchValue = "smoke_table_$stamp"
        create = @{ datasourceId = 1; schemaName = 'public'; tableName = "smoke_table_$stamp"; tableComment = "Smoke Table $stamp"; columnsJson = '[{"name":"id","type":"bigint"}]'; status = 'ENABLED'; remark = 'ai crud smoke' }
        update = @{ tableComment = "Smoke Table $stamp Updated"; columnsJson = '[{"name":"id","type":"bigint"},{"name":"name","type":"varchar"}]'; remark = 'ai crud smoke updated' }
    },
    @{
        name = 'chatbi/datasets'; idKey = 'datasetId'; keyword = "smoke-dataset-$stamp"; matchKey = 'datasetCode'; matchValue = "smoke-dataset-$stamp"
        create = @{ datasetCode = "smoke-dataset-$stamp"; datasetName = "Smoke Dataset $stamp"; datasourceId = 1; tableIds = '1'; semanticModel = '{"metrics":[]}'; defaultFilters = 'deleted = 0'; status = 'ENABLED'; remark = 'ai crud smoke' }
        update = @{ datasetName = "Smoke Dataset $stamp Updated"; tableIds = '1,2'; semanticModel = '{"metrics":[{"name":"count"}]}'; remark = 'ai crud smoke updated' }
    },
    @{
        name = 'chatbi/terms'; idKey = 'termId'; keyword = "smoke-term-$stamp"; matchKey = 'termCode'; matchValue = "smoke-term-$stamp"
        create = @{ termCode = "smoke-term-$stamp"; termName = "Smoke Term $stamp"; synonyms = 'smoke alias'; definition = 'smoke definition'; expression = 'count(1)'; datasetId = 1; status = 'ENABLED'; remark = 'ai crud smoke' }
        update = @{ termName = "Smoke Term $stamp Updated"; synonyms = 'smoke alias updated'; definition = 'smoke definition updated'; remark = 'ai crud smoke updated' }
    },
    @{
        name = 'agents'; idKey = 'agentId'; keyword = "smoke-agent-$stamp"; matchKey = 'agentCode'; matchValue = "smoke-agent-$stamp"
        create = @{ agentCode = "smoke-agent-$stamp"; agentName = "Smoke Agent $stamp"; description = 'Smoke agent'; modelId = 1; promptId = 1; toolIds = '1'; knowledgeBaseIds = '1'; memoryConfig = '{"type":"short-term"}'; status = 'ENABLED'; remark = 'ai crud smoke' }
        update = @{ agentName = "Smoke Agent $stamp Updated"; description = 'Smoke agent updated'; remark = 'ai crud smoke updated' }
    },
    @{
        name = 'workflows'; idKey = 'workflowId'; keyword = "smoke-workflow-$stamp"; matchKey = 'workflowCode'; matchValue = "smoke-workflow-$stamp"
        create = @{ workflowCode = "smoke-workflow-$stamp"; workflowName = "Smoke Workflow $stamp"; description = 'Smoke workflow'; triggerType = 'MANUAL'; graphJson = '{"nodes":[],"edges":[]}'; version = '1.0.0'; status = 'ENABLED'; remark = 'ai crud smoke' }
        update = @{ workflowName = "Smoke Workflow $stamp Updated"; description = 'Smoke workflow updated'; graphJson = '{"nodes":[{"id":"start"}],"edges":[]}'; version = '1.0.1'; remark = 'ai crud smoke updated' }
    }
)

try {
    $login = Invoke-Api -Method Post -Uri "$BaseUrl/api/auth/login" -Body @{ username = $Username; password = $Password }
    $results.Add((Assert-Ok 'auth.login' $login))
    $headers = @{ Authorization = $login.data.accessToken }

    foreach ($resource in $resources) {
        $name = $resource.name
        $idKey = $resource.idKey
        $createPayload = $resource.create

        $results.Add((Assert-Ok "$name.create" (Invoke-Api -Method Post -Uri "$BaseUrl/api/ai/$name/save" -Headers $headers -Body $createPayload)))

        $page = Invoke-Api -Method Post -Uri "$BaseUrl/api/ai/$name/page" -Headers $headers -Body @{
            pageNum = 1
            pageSize = 50
            keyword = $resource.keyword
        }
        $results.Add((Assert-Ok "$name.page" $page))
        $record = @($page.data.records | Where-Object { [string]($_.($resource.matchKey)) -eq [string]$resource.matchValue } | Select-Object -First 1)
        if (-not $record) {
            throw "$name.page did not return created record by $($resource.matchKey)=$($resource.matchValue)"
        }
        $id = $record.$idKey
        if (-not $id) {
            throw "$name.page did not return id field $idKey"
        }
        $created.Add([pscustomobject]@{ name = $name; id = $id })

        $results.Add((Assert-Ok "$name.detail" (Invoke-Api -Method Get -Uri "$BaseUrl/api/ai/$name/$id" -Headers $headers)))
        $updatePayload = Merge-Hashtable -Base $createPayload -Overrides $resource.update
        $updatePayload[$idKey] = $id
        $results.Add((Assert-Ok "$name.update" (Invoke-Api -Method Post -Uri "$BaseUrl/api/ai/$name/save" -Headers $headers -Body $updatePayload)))
    }
}
finally {
    if ($headers) {
        foreach ($item in @($created | Sort-Object id -Descending)) {
            $results.Add((Assert-Ok "$($item.name).delete" (Invoke-Api -Method Post -Uri "$BaseUrl/api/ai/$($item.name)/delete" -Headers $headers -Body @{ ids = @($item.id) })))
        }
    }
}

Write-Host ''
Write-Host 'AI CRUD Smoke Summary'
$results | Format-Table -AutoSize
Write-Host ''
Write-Host 'All AI CRUD smoke checks passed.'
