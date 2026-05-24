/param(
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

    return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Depth 20)
}

function Assert-Ok {
    param(
        [string]$Name,
        [object]$Response
    )

    if ($Response.code -ne 0) {
        throw "$Name failed: $($Response | ConvertTo-Json -Depth 20)"
    }

    [pscustomobject]@{
        Name = $Name
        Code = $Response.code
        Success = $true
        Message = $Response.message
    }
}

$stamp = Get-Date -Format 'yyyyMMddHHmmss'
$createdUserId = $null
$createdRoleId = $null
$createdMenuId = $null
$headers = $null
$results = New-Object System.Collections.Generic.List[object]

try {
    $login = Invoke-Api -Method Post -Uri "$BaseUrl/api/auth/login" -Body @{
        username = $Username
        password = $Password
    }
    $results.Add((Assert-Ok 'auth.login' $login))
    $headers = @{ Authorization = $login.data.accessToken }

    $roleCode = "SMOKE_ROLE_$stamp"
    $roleName = "Smoke Role $stamp"
    $results.Add((Assert-Ok 'role.create' (Invoke-Api -Method Post -Uri "$BaseUrl/api/system/role/save" -Headers $headers -Body @{
        roleCode = $roleCode
        roleName = $roleName
        dataScope = 'ALL'
        status = 'ENABLED'
        remark = 'system crud smoke'
    })))

    $rolePage = Invoke-Api -Method Post -Uri "$BaseUrl/api/system/role/page" -Headers $headers -Body @{
        roleCode = $roleCode
        pageNum = 1
        pageSize = 10
    }
    $results.Add((Assert-Ok 'role.query' $rolePage))
    $createdRoleId = @($rolePage.data.records | Where-Object { $_.roleCode -eq $roleCode } | Select-Object -First 1).roleId
    if (-not $createdRoleId) {
        throw 'role.query did not return created role'
    }

    $results.Add((Assert-Ok 'role.update' (Invoke-Api -Method Post -Uri "$BaseUrl/api/system/role/save" -Headers $headers -Body @{
        roleId = $createdRoleId
        roleCode = $roleCode
        roleName = "$roleName Updated"
        dataScope = 'ALL'
        status = 'ENABLED'
        remark = 'system crud smoke updated'
    })))

    $results.Add((Assert-Ok 'role.detail' (Invoke-Api -Method Get -Uri "$BaseUrl/api/system/role/$createdRoleId" -Headers $headers)))
    $results.Add((Assert-Ok 'role.permissions.detail' (Invoke-Api -Method Get -Uri "$BaseUrl/api/system/role/$createdRoleId/permissions" -Headers $headers)))
    $results.Add((Assert-Ok 'role.assign.permissions' (Invoke-Api -Method Post -Uri "$BaseUrl/api/system/role/assign-permissions" -Headers $headers -Body @{
        roleId = $createdRoleId
        menuIds = @(1, 10, 11)
        dataScope = 'ALL'
        deptIds = @()
    })))

    $menuName = "Smoke Menu $stamp"
    $permissionCode = "smoke:menu:$stamp"
    $results.Add((Assert-Ok 'menu.create' (Invoke-Api -Method Post -Uri "$BaseUrl/api/system/menu/save" -Headers $headers -Body @{
        parentId = 10
        menuName = $menuName
        menuType = 'MENU'
        routePath = "/system/smoke-$stamp"
        componentPath = 'system/menu/index'
        permissionCode = $permissionCode
        icon = 'Menu'
        sortNo = 999
        visible = $false
        keepAlive = $false
        externalLink = $false
        status = 'ENABLED'
        remark = 'system crud smoke'
    })))

    $menuTree = Invoke-Api -Method Get -Uri "$BaseUrl/api/system/menu/manage-tree" -Headers $headers
    $results.Add((Assert-Ok 'menu.query' $menuTree))
    $flatMenus = New-Object System.Collections.Generic.List[object]
    function Add-Menus {
        param([object[]]$Items)
        foreach ($item in $Items) {
            $flatMenus.Add($item)
            if ($item.children) {
                Add-Menus -Items @($item.children)
            }
        }
    }
    Add-Menus -Items @($menuTree.data)
    $createdMenuId = @($flatMenus | Where-Object { $_.permissionCode -eq $permissionCode } | Select-Object -First 1).menuId
    if (-not $createdMenuId) {
        throw 'menu.query did not return created menu'
    }

    $results.Add((Assert-Ok 'menu.update' (Invoke-Api -Method Post -Uri "$BaseUrl/api/system/menu/save" -Headers $headers -Body @{
        menuId = $createdMenuId
        parentId = 10
        menuName = "$menuName Updated"
        menuType = 'MENU'
        routePath = "/system/smoke-$stamp"
        componentPath = 'system/menu/index'
        permissionCode = $permissionCode
        icon = 'Menu'
        sortNo = 998
        visible = $false
        keepAlive = $false
        externalLink = $false
        status = 'ENABLED'
        remark = 'system crud smoke updated'
    })))
    $results.Add((Assert-Ok 'menu.detail' (Invoke-Api -Method Get -Uri "$BaseUrl/api/system/menu/$createdMenuId" -Headers $headers)))

    $usernameSmoke = "smoke_$stamp"
    $results.Add((Assert-Ok 'user.create' (Invoke-Api -Method Post -Uri "$BaseUrl/api/system/user/create" -Headers $headers -Body @{
        username = $usernameSmoke
        nickname = "Smoke User $stamp"
        deptId = 1
        phone = '13800000000'
        email = "$usernameSmoke@example.com"
        status = 'ENABLED'
        remark = 'system crud smoke'
    })))

    $userPage = Invoke-Api -Method Post -Uri "$BaseUrl/api/system/user/page" -Headers $headers -Body @{
        username = $usernameSmoke
        pageNum = 1
        pageSize = 10
    }
    $results.Add((Assert-Ok 'user.query' $userPage))
    $createdUserId = @($userPage.data.records | Where-Object { $_.username -eq $usernameSmoke } | Select-Object -First 1).userId
    if (-not $createdUserId) {
        throw 'user.query did not return created user'
    }

    $results.Add((Assert-Ok 'user.update' (Invoke-Api -Method Post -Uri "$BaseUrl/api/system/user/update" -Headers $headers -Body @{
        userId = $createdUserId
        nickname = "Smoke User $stamp Updated"
        deptId = 1
        phone = '13900000000'
        email = "$usernameSmoke-updated@example.com"
        status = 'ENABLED'
        remark = 'system crud smoke updated'
    })))
    $results.Add((Assert-Ok 'user.detail' (Invoke-Api -Method Get -Uri "$BaseUrl/api/system/user/$createdUserId" -Headers $headers)))
    $results.Add((Assert-Ok 'user.assign.roles' (Invoke-Api -Method Post -Uri "$BaseUrl/api/system/user/assign-roles" -Headers $headers -Body @{
        userId = $createdUserId
        roleIds = @($createdRoleId)
    })))
    $results.Add((Assert-Ok 'user.status.disable' (Invoke-Api -Method Post -Uri "$BaseUrl/api/system/user/status" -Headers $headers -Body @{
        id = $createdUserId
        status = 'DISABLED'
    })))
    $results.Add((Assert-Ok 'user.reset.password' (Invoke-Api -Method Post -Uri "$BaseUrl/api/system/user/reset-password" -Headers $headers -Body @{
        userId = $createdUserId
    })))
}
finally {
    if ($headers) {
        if ($createdUserId) {
            $results.Add((Assert-Ok 'cleanup.user.delete' (Invoke-Api -Method Post -Uri "$BaseUrl/api/system/user/delete" -Headers $headers -Body @{ ids = @($createdUserId) })))
        }
        if ($createdMenuId) {
            $results.Add((Assert-Ok 'cleanup.menu.delete' (Invoke-Api -Method Post -Uri "$BaseUrl/api/system/menu/delete/$createdMenuId" -Headers $headers)))
        }
        if ($createdRoleId) {
            $results.Add((Assert-Ok 'cleanup.role.delete' (Invoke-Api -Method Post -Uri "$BaseUrl/api/system/role/delete" -Headers $headers -Body @{ ids = @($createdRoleId) })))
        }
    }
}

Write-Host ''
Write-Host 'System CRUD Smoke Summary'
$results | Format-Table -AutoSize
Write-Host ''
Write-Host 'All system CRUD smoke checks passed.'
