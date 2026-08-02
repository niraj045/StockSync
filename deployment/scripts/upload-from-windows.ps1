param(
    [string]$Server = "66.116.253.40",
    [string]$User = "stocksync",
    [string]$RemoteDirectory = "/opt/stocksync",
    [switch]$PackageOnly
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$Timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$ArchiveName = "stocksync-deployment-$Timestamp.tar.gz"
$ArchivePath = Join-Path $env:TEMP $ArchiveName

foreach ($command in @("tar.exe", "scp.exe")) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
        throw "$command is required and was not found on PATH."
    }
}

$TarArguments = @(
    "-czf", $ArchivePath,
    "--exclude=.git",
    "--exclude=backend/target",
    "--exclude=backend/src/test",
    "--exclude=frontend/node_modules",
    "--exclude=frontend/dist",
    "--exclude=frontend/coverage",
    "--exclude=frontend/test-results",
    "--exclude=frontend/src/test",
    "--exclude=frontend/src/**/*.test.ts",
    "--exclude=frontend/src/**/*.test.tsx",
    "--exclude=deployment/.env",
    "--exclude=deployment/certbot/conf/*",
    "--exclude=deployment/*.tar.gz",
    "--exclude=deployment/storage/uploads/*",
    "--exclude=deployment/storage/documents/*",
    "--exclude=deployment/storage/reports/*",
    "--exclude=deployment/storage/backups/*",
    "-C", $ProjectRoot,
    "backend", "frontend", "deployment"
)

& tar.exe @TarArguments
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $ArchivePath)) {
    throw "Unable to create the deployment archive."
}

if (-not $PackageOnly) {
    Write-Host "Uploading $ArchiveName to $User@$Server`:$RemoteDirectory/"
    & scp.exe $ArchivePath "$User@$Server`:$RemoteDirectory/"
    if ($LASTEXITCODE -ne 0) {
        throw "SCP upload failed."
    }
} else {
    Write-Host "Package-only validation complete: $ArchivePath"
}

Write-Host ""
Write-Host "Upload complete. Run these commands exactly:"
Write-Host "ssh $User@$Server"
Write-Host "cd $RemoteDirectory"
Write-Host "tar -xzf $ArchiveName"
Write-Host "chmod +x deployment/scripts/*.sh backend/mvnw"
Write-Host "sudo ./deployment/scripts/prepare-server.sh"
Write-Host "cp deployment/.env.example deployment/.env"
Write-Host "nano deployment/.env"
Write-Host "./deployment/scripts/start.sh"
