[CmdletBinding()]
param(
    [switch]$SkipGenerate,
    [string]$Java17Home,
    [string]$Java21Home,
    [string]$Java25Home
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
$generator = Join-Path $repoRoot 'scripts\generate.py'

function Resolve-Python {
    $python = Get-Command python -ErrorAction SilentlyContinue
    if ($python) {
        return @($python.Source, @())
    }

    $pyLauncher = Get-Command py -ErrorAction SilentlyContinue
    if ($pyLauncher) {
        return @($pyLauncher.Source, @('-3'))
    }

    $bundledPython = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
    if (Test-Path -LiteralPath $bundledPython) {
        return @($bundledPython, @())
    }

    throw 'Python was not found. Install Python 3 or add python.exe/py.exe to PATH.'
}

function Resolve-JavaHome {
    param(
        [Parameter(Mandatory)]
        [int]$Major,
        [string]$Override
    )

    $candidates = @()
    if ($Override) {
        $candidates += $Override
    }

    switch ($Major) {
        17 {
            $candidates += 'C:\Program Files\Zulu\zulu-17'
            $candidates += Get-ChildItem 'C:\Program Files\Java' -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
        }
        21 {
            $candidates += Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
            $candidates += 'C:\Program Files\Java\jdk-21'
        }
        25 {
            $candidates += Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-25*' -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
            $candidates += 'C:\Program Files\Java\jdk-25'
        }
    }

    foreach ($candidate in ($candidates | Where-Object { $_ } | Select-Object -Unique)) {
        if (Test-Path -LiteralPath (Join-Path $candidate 'bin\java.exe')) {
            return $candidate
        }
    }

    throw "JDK $Major was not found. Pass -Java${Major}Home 'C:\path\to\jdk'."
}

function Use-Java {
    param([Parameter(Mandatory)][string]$JavaHome)

    $env:JAVA_HOME = $JavaHome
    $javaBin = Join-Path $JavaHome 'bin'
    $pathParts = @($env:Path -split ';' | Where-Object { $_ -and $_ -ne $javaBin })
    $env:Path = $javaBin + ';' + ($pathParts -join ';')
}

function Invoke-Build {
    param(
        [Parameter(Mandatory)][string]$Minecraft,
        [Parameter(Mandatory)][int]$JavaMajor,
        [Parameter(Mandatory)][string]$ProjectDirectory,
        [Parameter(Mandatory)][string]$JavaHome,
        [switch]$Standalone
    )

    Use-Java $JavaHome
    $env:MC_VERSION = $Minecraft
    Push-Location $ProjectDirectory
    try {
        if ($Standalone) {
            Write-Host "`n=== Building Minecraft $Minecraft (standalone, JDK $JavaMajor) ===" -ForegroundColor Cyan
        } else {
            Write-Host "`n=== Building Minecraft $Minecraft (root, JDK $JavaMajor) ===" -ForegroundColor Cyan
        }

        & .\gradlew.bat build --no-daemon --console=plain
        if ($LASTEXITCODE -ne 0) {
            throw "Build failed for Minecraft $Minecraft with exit code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

$pythonInfo = Resolve-Python
$python = $pythonInfo[0]
$pythonArgs = $pythonInfo[1]

$jdk17 = Resolve-JavaHome 17 $Java17Home
$jdk21 = Resolve-JavaHome 21 $Java21Home
$jdk25 = Resolve-JavaHome 25 $Java25Home

Push-Location $repoRoot
try {
    if (-not $SkipGenerate) {
        Write-Host '=== Regenerating all Minecraft projects ===' -ForegroundColor Cyan
        & $python @pythonArgs $generator --all
        if ($LASTEXITCODE -ne 0) {
            throw "Generation failed with exit code $LASTEXITCODE."
        }
    }

    Invoke-Build '1.20.1' 17 $repoRoot $jdk17
    Invoke-Build '1.21.1' 21 $repoRoot $jdk21
    Invoke-Build '1.21.11' 21 $repoRoot $jdk21
    Invoke-Build '26.1.2' 25 (Join-Path $repoRoot 'build\generated\26.1.2') $jdk25 -Standalone
    Invoke-Build '26.2' 25 (Join-Path $repoRoot 'build\generated\26.2') $jdk25 -Standalone

    Write-Host "`nAll Minecraft builds completed successfully." -ForegroundColor Green
}
finally {
    Pop-Location
}
