# Lecture seule. Aucune interaction ADB ou installation TV.
# powershell -NoProfile -ExecutionPolicy Bypass -File .\Preflight-Anime-Sama-TV.ps1
# powershell -NoProfile -ExecutionPolicy Bypass -File .\Preflight-Anime-Sama-TV.ps1 -Online
[CmdletBinding()]
param([switch]$Online)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
# Works both in AndroidTooling/ and as a Git-tracked project script.
if (Test-Path (Join-Path $root 'app') -PathType Container) {
    $root = Split-Path -Parent $root
}
$client = Join-Path $root 'Nuvio-AnimeSama-Client'
$provider = Join-Path $root 'AnimeSama-Published'
$apk = Join-Path $root 'Builds\AnimeSama-LocalStable\Anime-Sama-TV-Qilive-ARM.apk'
$apksigner = Join-Path $env:LOCALAPPDATA 'Android\Sdk\build-tools\35.0.0\apksigner.bat'
$expectedLocalCert = 'f3177306ef22d9686885e46d7fd224a1efceb328f775e1b066fba787e1fcf582'
# Historic fingerprint recorded when the TV app was installed. This script
# DOES NOT assume it still reflects the installed APK or access the TV.
$historicalTvCert = '351ca392afcc4098c6ac74a801c69ffe67ff246b790c647f5f11755ad05ba7f4'
foreach($file in @($apk, $apksigner)) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { throw "Missing required file: $file" }
}
$ErrorActionPreference = 'Continue' # Java warning on stderr is not a failed signature.
$verified = @(& $apksigner verify --print-certs $apk 2>&1)
$verifyExit = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
if($verifyExit -ne 0) { throw 'APK signature verification failed.' }
$line = $verified | Where-Object { $_ -match 'Signer #1 certificate SHA-256 digest:\s*([a-fA-F0-9]{64})' } | Select-Object -First 1
if(-not $line) { throw 'Unable to parse APK signer fingerprint.' }
$cert = [regex]::Match([string]$line,'([a-fA-F0-9]{64})').Groups[1].Value.ToLowerInvariant()
$apkInfo = Get-Item -LiteralPath $apk
$sha = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
$clientHead = (& git -C $client rev-parse HEAD).Trim()
$providerHead = (& git -C $provider rev-parse HEAD).Trim()
if($LASTEXITCODE -ne 0) { throw 'Unable to read local Git revisions.' }
$tracked = @(& git -C $client ls-files)
# Three public/upstream test fixtures are already tracked in Nuvio.
# They are not the private Android APK signing keystore.
$knownUpstreamFixtures = @(
    'app/src/main/res/raw/isrg_root_x2.pem',
    'app/src/main/res/raw/isrgrootx1.pem',
    'app/src/test/resources/subtitle-redirect-test.p12'
)
$trackedSecrets = @($tracked | Where-Object {
    $_ -match '(?i)(^|/)([^/]*\.(jks|keystore|p12|pfx|pem|apk))$' -and
    $_ -notin $knownUpstreamFixtures
})
$localCatalog = Get-Content -LiteralPath (Join-Path $provider 'docs\catalog\series\as_search.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$localManifest = Get-Content -LiteralPath (Join-Path $provider 'manifest.json') -Raw -Encoding UTF8 | ConvertFrom-Json
Write-Host '=== Anime-Sama TV: verification PRE-INSTALLATION (read only) ==='
Write-Host "APK: $($apkInfo.FullName)"
Write-Host "APK bytes: $($apkInfo.Length)"
Write-Host "APK SHA-256: $sha"
Write-Host "APK signature SHA-256: $cert"
Write-Host "Local signing key matches expected: $($cert -eq $expectedLocalCert)"
Write-Host "Known OLD installation signature differs: $($cert -ne $historicalTvCert) (historic, not checked on TV)"
Write-Host "Android Git HEAD: $clientHead"
Write-Host "Provider Git HEAD: $providerHead"
Write-Host "Local provider version: $($localManifest.version)"
Write-Host "Verified search entries: $(@($localCatalog.metas).Count)"
Write-Host "Tracked sensitive binaries: $($trackedSecrets.Count)"
if($trackedSecrets.Count -gt 0) { Write-Warning "Audit files in repository before pushing: $($trackedSecrets -join ', ')" }
if($Online) {
    try {
        $remote = @(& git -C $client ls-remote origin refs/heads/anime-sama-tv)
        if(-not $remote -or $LASTEXITCODE -ne 0) { throw 'Remote revision unavailable' }
        $remoteHead = ($remote[0] -split '\s+')[0]
        Write-Host "GitHub Android HEAD: $remoteHead"
        Write-Host "Android matches GitHub: $($remoteHead -eq $clientHead)"
        $manifest = Invoke-RestMethod 'https://raw.githubusercontent.com/JoeSm0ke/Nuvio-AnimeSama-Qilive/main/manifest.json' -TimeoutSec 15
        Write-Host "GitHub provider version: $($manifest.version)"
        $site = Invoke-RestMethod ('https://joesm0ke.github.io/Nuvio-AnimeSama-Qilive/catalog/series/as_search.json?preflight=' + [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()) -TimeoutSec 15
        Write-Host "Public search entries: $(@($site.metas).Count)"
    } catch {
        Write-Warning ('Public online verification incomplete: ' + $_.Exception.Message)
    }
}
if($cert -ne $expectedLocalCert) {
    throw 'STOP: APK signed by an unexpected certificate.'
}
if($cert -ne $historicalTvCert) {
    Write-Warning 'Migration risk: the older GitHub-signed TV APK cannot be updated in place using this APK. DO NOT uninstall; backup/migration must be approved and planned.'
    Write-Host 'Migration readiness: BLOCKED until authorized on-TV backup and compatibility verification.'
}
Write-Host 'DONE: no TV access, no installation, no secret transfer.'
