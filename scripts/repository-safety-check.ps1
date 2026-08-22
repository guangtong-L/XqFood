param(
    [switch]$SelfTest
)

$ErrorActionPreference = 'Stop'
$repoRoot = (& git rev-parse --show-toplevel).Trim()
if (-not $repoRoot) { throw 'Cannot locate the Git repository root.' }

$selfPath = 'scripts/repository-safety-check.ps1'
$textExtensions = @(
    '.md', '.yml', '.yaml', '.json', '.java', '.ts', '.vue', '.js', '.mjs',
    '.ps1', '.xml', '.sql', '.properties', '.txt'
)
$excludedPath = '(^|/)(node_modules|target|dist|unpackage)(/|$)'
$violations = [System.Collections.Generic.List[object]]::new()

# Flyway migrations are immutable after release. V1 contains two historical wording defects,
# so only those exact rules are exempted for that exact file. V5 must contain the corrections.
$immutableMigrationExceptions = @{
    'xiaodudou-server/src/main/resources/db/migration/V1__init_schema.sql' = @(
        'false-phone-encryption-schema-comment',
        'unimplemented-fixed-log-retention-claim'
    )
}
$correctionMigration = 'xiaodudou-server/src/main/resources/db/migration/V5__correct_security_and_retention_comments.sql'

$rules = @(
    @{ Name = 'known-credential-signature'; Pattern = 'Xdd(?:App|Redis)@' },
    @{ Name = 'private-key-content'; Pattern = '-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----' },
    @{ Name = 'credentials-embedded-in-url'; Pattern = '(?i)://[^/\s:@]+:[^/\s@]+@' },
    @{ Name = 'password-on-command-line'; Pattern = '(?i)redis-cli\s+-a\s+|mysql\s+[^\r\n]*-p[''\"]\S+' },
    @{ Name = 'unimplemented-static-encryption-claim'; Pattern = 'AES-256' },
    @{ Name = 'unimplemented-image-deletion-claim'; Pattern = '24\s*\u5c0f\u65f6\u5185[^\r\n]*(\u81ea\u52a8)?\u5220\u9664' },
    @{ Name = 'unimplemented-recipe-count-claim'; Pattern = '300\+' },
    @{ Name = 'unimplemented-unlimited-ai-claim'; Pattern = '\u65e0\u9650\s*AI' },
    @{ Name = 'placeholder-support-contact'; Pattern = '400-XXX|xddai_service|contact@xiaodudou\.ai' },
    @{ Name = 'unimplemented-account-deletion-deadline'; Pattern = '30\s*\u5929\u5185[^\r\n]*\u5220\u9664' },
    @{ Name = 'unimplemented-feedback-sla'; Pattern = '\u5de5\u4f5c\u65e5\s*30\s*\u5206\u949f\u5185[^\r\n]*\u56de\u590d' },
    @{ Name = 'false-phone-encryption-schema-comment'; Pattern = '\u624b\u673a\u53f7\uff08\u52a0\u5bc6\u5b58\u50a8\uff09' },
    @{ Name = 'unimplemented-fixed-log-retention-claim'; Pattern = '\u5408\u89c4\u7559\u5b58[^\r\n]*180\s*\u5929|\u7559\u5b58\s*[\u2265>]\s*180\s*\u5929' },
    @{ Name = 'false-ai-audit-pass-default'; Pattern = '\u6682\u65e0\u5185\u5bb9\u5ba1\u6838[^\r\n]*\u6807\u8bb0\u901a\u8fc7' },
    @{ Name = 'example-secret-placeholder'; Pattern = 'your-[a-z0-9-]+' }
)

function Assert-ImmutableMigrationExceptionPolicy {
    $expectedFile = 'xiaodudou-server/src/main/resources/db/migration/V1__init_schema.sql'
    $expectedRules = @('false-phone-encryption-schema-comment', 'unimplemented-fixed-log-retention-claim')
    $exceptionFiles = @($immutableMigrationExceptions.Keys)
    if ($exceptionFiles.Count -ne 1 -or $exceptionFiles[0] -ne $expectedFile) {
        throw 'Immutable migration exception scope expanded beyond the approved V1 file.'
    }
    $actualRules = @($immutableMigrationExceptions[$expectedFile] | Sort-Object)
    $ruleDelta = @(Compare-Object ($expectedRules | Sort-Object) $actualRules)
    if ($ruleDelta.Count -ne 0) {
        throw 'Immutable migration exception rules differ from the two approved historical claims.'
    }

    $v1Path = Join-Path $repoRoot $expectedFile
    $currentV1Hash = (& git hash-object -- $v1Path).Trim()
    $headV1Hash = (& git rev-parse "HEAD:$expectedFile").Trim()
    if (-not $currentV1Hash -or $currentV1Hash -ne $headV1Hash) {
        throw 'V1 differs from HEAD. Released Flyway migrations must remain byte-for-byte immutable.'
    }

    $v5Path = Join-Path $repoRoot $correctionMigration
    if (-not (Test-Path -LiteralPath $v5Path -PathType Leaf)) {
        throw 'The mandatory V5 corrective migration is missing.'
    }
    $v5 = [IO.File]::ReadAllText($v5Path)
    $requiredCorrections = @(
        '\u654f\u611f\u4e2a\u4eba\u4fe1\u606f\uff0c\u5f53\u524d\u672a\u505a\u5b57\u6bb5\u7ea7\u9759\u6001\u52a0\u5bc6',
        'NULL\s+\u672a\u5ba1\u6838',
        '\u7559\u5b58\u7b56\u7565\u5f85\u6cd5\u52a1\u786e\u8ba4'
    )
    foreach ($required in $requiredCorrections) {
        if (-not [regex]::IsMatch($v5, $required)) {
            throw 'V5 does not contain all mandatory corrective comments.'
        }
    }
}

function Test-IsApprovedImmutableMigrationException([string]$file, [string]$ruleName) {
    return $immutableMigrationExceptions.ContainsKey($file) -and
        $immutableMigrationExceptions[$file] -contains $ruleName
}

Assert-ImmutableMigrationExceptionPolicy

if ($SelfTest) {
    $syntheticCases = @(
        @{ Rule = 'known-credential-signature'; Text = ('Xdd' + 'App@' + 'TEST_ONLY_NOT_A_SECRET') },
        @{ Rule = 'unimplemented-static-encryption-claim'; Text = ('AES' + '-256') }
    )
    foreach ($case in $syntheticCases) {
        $rule = $rules | Where-Object { $_.Name -eq $case.Rule }
        if (-not $rule -or -not [regex]::IsMatch($case.Text, $rule.Pattern)) {
            throw "Safety rule self-test failed: $($case.Rule)"
        }
    }
    $v1 = 'xiaodudou-server/src/main/resources/db/migration/V1__init_schema.sql'
    if (-not (Test-IsApprovedImmutableMigrationException $v1 'false-phone-encryption-schema-comment') -or
        (Test-IsApprovedImmutableMigrationException $v1 'unimplemented-static-encryption-claim') -or
        (Test-IsApprovedImmutableMigrationException 'README.md' 'false-phone-encryption-schema-comment')) {
        throw 'Immutable migration exception boundary self-test failed.'
    }
    Write-Host "Repository safety rule self-test passed: $($syntheticCases.Count) detection cases plus exception-boundary checks."
    exit 0
}

$trackedFiles = & git -c core.quotepath=false ls-files --cached --others --exclude-standard
foreach ($relativePath in $trackedFiles) {
    $normalized = $relativePath -replace '\\', '/'
    if ($normalized -eq $selfPath -or $normalized -match $excludedPath) { continue }

    $extension = [IO.Path]::GetExtension($normalized).ToLowerInvariant()
    if ($textExtensions -notcontains $extension -and -not [IO.Path]::GetFileName($normalized).StartsWith('.env')) {
        continue
    }

    $absolutePath = Join-Path $repoRoot $relativePath
    if (-not (Test-Path -LiteralPath $absolutePath -PathType Leaf)) { continue }
    $content = [IO.File]::ReadAllText($absolutePath)

    foreach ($rule in $rules) {
        if ([regex]::IsMatch($content, $rule.Pattern)) {
            if (-not (Test-IsApprovedImmutableMigrationException $normalized $rule.Name)) {
                $violations.Add([pscustomobject]@{ Rule = $rule.Name; File = $normalized })
            }
        }
    }

    foreach ($line in ($content -split "`r?`n")) {
        if ($line -match '^\s*(password|api-key|secret|secret-key)\s*:\s*([^#]*)') {
            $value = $Matches[2].Trim()
            $safePlaceholder = -not $value -or $value -match '^\$\{[A-Z0-9_]+:\}$' -or $value -in @("''", '""')
            if (-not $safePlaceholder) {
                $violations.Add([pscustomobject]@{ Rule = 'hardcoded-config-credential'; File = $normalized })
                break
            }
        }
    }
}

$violations = @($violations | Sort-Object Rule, File -Unique)
if ($violations.Count -gt 0) {
    Write-Host "Repository safety check failed with $($violations.Count) violation(s). Only rule and file names are shown."
    foreach ($violation in $violations) {
        Write-Host "[$($violation.Rule)] $($violation.File)"
    }
    exit 1
}

Write-Host 'Repository safety check passed.'
