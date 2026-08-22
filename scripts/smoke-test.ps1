param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^https?://')]
    [string]$BaseUrl,
    [string]$Token
)

$ErrorActionPreference = 'Stop'
$base = $BaseUrl.TrimEnd('/')

function Invoke-Check([string]$path, [hashtable]$headers = @{}, [int]$expectedStatus = 200) {
    try {
        $response = Invoke-WebRequest -Uri "$base$path" -Method Get -Headers $headers -TimeoutSec 10 -UseBasicParsing
        if ($response.StatusCode -ne $expectedStatus) { throw "unexpected status" }
        if ([string]::IsNullOrWhiteSpace($response.Headers['X-Request-Id'])) { throw 'missing request id' }
    } catch {
        throw "烟测失败：$path（不输出响应正文，避免泄露内部信息）"
    }
}

Invoke-Check '/api/v1/health'
Invoke-Check '/api/v1/readiness'
if (-not [string]::IsNullOrWhiteSpace($Token)) {
    Invoke-Check '/api/v1/recipes?page=1&size=1' @{ 'x-token' = $Token }
    Write-Host '只读烟测通过：liveness、readiness、已鉴权菜谱浏览与request-id正常。'
} else {
    Write-Host '基础烟测通过：liveness、readiness与request-id正常；未提供Token，未宣称菜谱链路已验证。'
}
