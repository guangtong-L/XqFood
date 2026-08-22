param()

$ErrorActionPreference = 'Stop'
$errors = [System.Collections.Generic.List[string]]::new()

function Require-Value([string]$name) {
    $value = [Environment]::GetEnvironmentVariable($name)
    if ([string]::IsNullOrWhiteSpace($value)) { $errors.Add("缺少环境变量：$name") }
}

function Require-Boolean([string]$name, [bool]$expected) {
    $value = [Environment]::GetEnvironmentVariable($name)
    if ([string]::IsNullOrWhiteSpace($value) -or $value.ToLowerInvariant() -ne $expected.ToString().ToLowerInvariant()) {
        $errors.Add("$name 必须为 $($expected.ToString().ToLowerInvariant())")
    }
}

function Require-Range([string]$name, [int]$minimum, [int]$maximum) {
    $value = 0
    $raw = [Environment]::GetEnvironmentVariable($name)
    if (-not [int]::TryParse($raw, [ref]$value) -or $value -lt $minimum -or $value -gt $maximum) {
        $errors.Add("$name 必须在 $minimum..$maximum")
    }
}

$profiles = [Environment]::GetEnvironmentVariable('SPRING_PROFILES_ACTIVE')
if ($profiles -ne 'prod') { $errors.Add('SPRING_PROFILES_ACTIVE 必须且只能为 prod') }

@('DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'REDIS_HOST', 'REDIS_PASSWORD',
  'XDD_DATA_ENCRYPTION_KEY', 'WX_APPID', 'WX_SECRET', 'CORS_ALLOWED_ORIGINS') | ForEach-Object { Require-Value $_ }
Require-Boolean 'WX_LOGIN_ENABLED' $true
@('XDD_MOCK_LOGIN_ENABLED', 'XDD_MOCK_AI_ENABLED', 'XDD_AI_FEATURE_ENABLED',
  'ZHIPU_ENABLED', 'OPENAPI_ENABLED') | ForEach-Object { Require-Boolean $_ $false }
Require-Range 'AI_LOG_RETENTION_DAYS' 1 365
Require-Range 'SA_TOKEN_TIMEOUT_SECONDS' 3600 604800
Require-Range 'SA_TOKEN_ACTIVE_TIMEOUT_SECONDS' 300 43200
Require-Range 'LOGIN_RATE_LIMIT_ATTEMPTS' 1 1000
Require-Range 'LOGIN_RATE_LIMIT_WINDOW_SECONDS' 1 3600

$key = [Environment]::GetEnvironmentVariable('XDD_DATA_ENCRYPTION_KEY')
if (-not [string]::IsNullOrWhiteSpace($key)) {
    try {
        if ([Convert]::FromBase64String($key).Length -ne 32) { $errors.Add('画像加密密钥必须是32字节Base64') }
    } catch { $errors.Add('画像加密密钥必须是合法Base64') }
}

$origins = [Environment]::GetEnvironmentVariable('CORS_ALLOWED_ORIGINS')
if (-not [string]::IsNullOrWhiteSpace($origins)) {
    foreach ($origin in $origins.Split(',')) {
        $uri = $null
        if (-not [Uri]::TryCreate($origin.Trim(), [UriKind]::Absolute, [ref]$uri) -or
            $uri.Scheme -ne 'https' -or $uri.Host -in @('localhost', '127.0.0.1') -or $origin.Contains('*')) {
            $errors.Add('CORS_ALLOWED_ORIGINS 仅允许逗号分隔的精确 HTTPS 来源')
            break
        }
    }
}

if ($errors.Count -gt 0) {
    Write-Host "生产预检未通过（仅显示配置项，不显示任何值）："
    $errors | ForEach-Object { Write-Host " - $_" }
    exit 1
}
Write-Host '生产预检通过；仍需完成备份恢复演练、灰度和外部微信配置验收。'
