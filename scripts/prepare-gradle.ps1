$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$jarPath = Join-Path $projectRoot 'gradle\wrapper\gradle-wrapper.jar'
$url = 'https://services.gradle.org/distributions/gradle-8.11.1-wrapper.jar'
if (-not (Test-Path $jarPath)) {
    Write-Host 'Baixando o Gradle Wrapper oficial (primeira compilacao requer internet)...'
    Invoke-WebRequest -Uri $url -OutFile $jarPath
}
$hashBody = (Invoke-WebRequest -Uri "$url.sha256").Content
if ($hashBody -is [byte[]]) {
    $expected = [System.Text.Encoding]::ASCII.GetString($hashBody).Trim().ToLowerInvariant()
} else {
    $expected = ([string]$hashBody).Trim().ToLowerInvariant()
}
$actual = (Get-FileHash -Path $jarPath -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actual -ne $expected) {
    Remove-Item $jarPath -ErrorAction SilentlyContinue
    throw 'Checksum SHA-256 do Gradle Wrapper nao corresponde ao oficial.'
}
Write-Host 'Gradle Wrapper verificado.'
