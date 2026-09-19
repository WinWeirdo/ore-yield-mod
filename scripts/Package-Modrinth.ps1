<#
.SYNOPSIS
Collects every built Ore Yield release jar for a Modrinth upload.

.DESCRIPTION
Copies the ten supported Minecraft/loader jars into build\modrinth with
stable, upload-ready names such as ore-yield-1.20.1-forge-1.2.1.jar.

.EXAMPLE
.\old\Package-Modrinth.ps1

.EXAMPLE
.\old\Package-Modrinth.ps1 -WhatIf
#>
[CmdletBinding(SupportsShouldProcess)]
param(
    [string]$DestinationDirectory
)

$projectRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($DestinationDirectory)) {
    $DestinationDirectory = Join-Path $projectRoot 'build\modrinth'
}

$propertiesPath = Join-Path $projectRoot 'gradle.properties'
if (-not (Test-Path -LiteralPath $propertiesPath)) {
    throw "Cannot find gradle.properties at $propertiesPath"
}

$versionMatch = Select-String -LiteralPath $propertiesPath -Pattern '^mod_version\s*=\s*(?<version>\S+)\s*$' |
    Select-Object -First 1
if ($null -eq $versionMatch) {
    throw "mod_version is missing from $propertiesPath"
}
$modVersion = $versionMatch.Matches[0].Groups['version'].Value

$targets = @(
    @{ Minecraft = '1.20.1'; Loader = 'forge' },
    @{ Minecraft = '1.20.1'; Loader = 'fabric' },
    @{ Minecraft = '1.21.1'; Loader = 'fabric' },
    @{ Minecraft = '1.21.1'; Loader = 'neoforge' },
    @{ Minecraft = '1.21.11'; Loader = 'fabric' },
    @{ Minecraft = '1.21.11'; Loader = 'neoforge' },
    @{ Minecraft = '26.1.2'; Loader = 'fabric' },
    @{ Minecraft = '26.1.2'; Loader = 'neoforge' },
    @{ Minecraft = '26.2'; Loader = 'fabric' },
    @{ Minecraft = '26.2'; Loader = 'neoforge' },
    @{ Minecraft = '26.3'; Loader = 'fabric' },
    @{ Minecraft = '26.3'; Loader = 'neoforge' }
)

$plans = foreach ($target in $targets) {
    $source = Join-Path $projectRoot "build\generated\$($target.Minecraft)\$($target.Loader)\build\libs\ore-yield-$($target.Loader)-$modVersion.jar"
    [pscustomobject]@{
        Minecraft = $target.Minecraft
        Loader = $target.Loader
        Source = $source
        Destination = Join-Path $DestinationDirectory "ore-yield-$($target.Minecraft)-$($target.Loader)-$modVersion.jar"
    }
}

$missingSources = $plans | Where-Object { -not (Test-Path -LiteralPath $_.Source) }
if ($missingSources) {
    $missingList = ($missingSources.Source -join [Environment]::NewLine)
    throw "Build every target before packaging. Missing release jars:`n$missingList"
}

if (($plans.Destination | Select-Object -Unique).Count -ne $plans.Count) {
    throw 'Package destination names must be unique.'
}

if ($PSCmdlet.ShouldProcess($DestinationDirectory, 'Create Modrinth release directory')) {
    New-Item -ItemType Directory -Path $DestinationDirectory -Force | Out-Null
}

foreach ($plan in $plans) {
    if ($PSCmdlet.ShouldProcess($plan.Destination, "Copy $($plan.Minecraft) $($plan.Loader) release jar")) {
        Copy-Item -LiteralPath $plan.Source -Destination $plan.Destination -Force
    }
}

if ($WhatIfPreference) {
    Write-Output "Preflight passed: Ore Yield $modVersion has $($plans.Count) release jars ready for packaging. WhatIf made no changes."
} else {
    Write-Output "Packaged Ore Yield $modVersion into $DestinationDirectory ($($plans.Count) jars)."
}
