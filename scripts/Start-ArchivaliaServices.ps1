$requiredVariables = @(
    "JWT_SECRET",
    "ADMIN_PASSWORD"
)

$profile = if ([string]::IsNullOrWhiteSpace($env:ARCHIVALIA_PROFILE)) {
    "demo"
}
else {
    $env:ARCHIVALIA_PROFILE.ToLowerInvariant()
}
if ($profile -notin @("demo", "postgres")) {
    throw "ARCHIVALIA_PROFILE must be 'demo' or 'postgres'. No services were started."
}
if ($profile -eq "postgres") {
    $requiredVariables += "POSTGRES_PASSWORD"
}

$missingVariables = @($requiredVariables | Where-Object {
    [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_, "Process"))
})

if ($missingVariables.Count -gt 0) {
    $environmentSetupScript = Join-Path $PSScriptRoot "Set-ArchivaliaEnvironment.ps1"
    . $environmentSetupScript
}

foreach ($variableName in $requiredVariables) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($variableName, "Process"))) {
        throw "The $variableName environment variable is still missing. No services were started."
    }
}

if ($profile -eq "postgres") {
    $postgresCheckScript = Join-Path $PSScriptRoot "Test-ArchivaliaPostgres.ps1"
    & $postgresCheckScript
    if ($LASTEXITCODE -ne 0) {
        throw "PostgreSQL login could not be verified. No application services were started. Check the existing database role and password; do not reset them automatically."
    }
}

function Find-ArchivaliaProjectRoot {
    $startPaths = @((Get-Location).Path, (Join-Path $PSScriptRoot ".."))
    foreach ($startPath in $startPaths) {
        $candidate = (Resolve-Path -LiteralPath $startPath).Path
        while ($candidate) {
            $hasRootPom = Test-Path -LiteralPath (Join-Path $candidate "pom.xml")
            $hasFrontend = Test-Path -LiteralPath (Join-Path $candidate "frontend\package.json")
            $hasServices = (Test-Path -LiteralPath (Join-Path $candidate "auth-service")) `
                -and (Test-Path -LiteralPath (Join-Path $candidate "api-gateway"))
            if ($hasRootPom -and $hasFrontend -and $hasServices) {
                return $candidate
            }

            $parent = Split-Path -Parent $candidate
            if ($parent -eq $candidate) {
                break
            }
            $candidate = $parent
        }
    }

    throw "Could not locate the Archivalia project root. Open PowerShell in the folder containing pom.xml and frontend\package.json, then run this script."
}

$projectRoot = Find-ArchivaliaProjectRoot
$powershellExecutable = (Get-Process -Id $PID).Path

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "Maven is not available on PATH. Install Maven 3.9 or later and reopen PowerShell."
}

if (-not (Test-Path (Join-Path $projectRoot "frontend\node_modules\vite\bin\vite.js"))) {
    throw "Frontend packages are not installed. From the frontend folder, run npm ci with approval, then try this script again."
}

Push-Location $projectRoot
try {
    Write-Output "Installing the shared project module into your local Maven cache."
    & mvn -B -pl common install
    if ($LASTEXITCODE -ne 0) {
        throw "The shared Maven module could not be installed. No application terminals were started."
    }
}
finally {
    Pop-Location
}

function Start-ArchivaliaTerminal {
    param(
        [Parameter(Mandatory = $true)][string]$Title,
        [Parameter(Mandatory = $true)][string]$Command,
        [Parameter(Mandatory = $true)][int]$Port
        )

        $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($listener) {
            $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($listener.OwningProcess)"
            Write-Warning "Port $Port is already in use by $($process.Name) (PID $($listener.OwningProcess)); not starting another $Title instance."
            return
        }

        $quotedRoot = $projectRoot.Replace("'", "''")
    $terminalCommand = "`$Host.UI.RawUI.WindowTitle = '$Title'; Set-Location -LiteralPath '$quotedRoot'; $Command"
    $encodedCommand = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($terminalCommand))
    Start-Process -FilePath $powershellExecutable -ArgumentList @("-NoExit", "-EncodedCommand", $encodedCommand) | Out-Null
    Write-Output "Opened terminal: $Title"
}

Start-ArchivaliaTerminal -Title "Archivalia - Eureka" -Command "mvn -pl discovery-server spring-boot:run" -Port 8761
Start-Sleep -Seconds 3
Start-ArchivaliaTerminal -Title "Archivalia - Auth Service" -Command "`$env:SPRING_PROFILES_ACTIVE = '$profile'; mvn -pl auth-service spring-boot:run" -Port 8081
Start-ArchivaliaTerminal -Title "Archivalia - Book Service" -Command "`$env:SPRING_PROFILES_ACTIVE = '$profile'; mvn -pl book-service spring-boot:run" -Port 8082
Start-ArchivaliaTerminal -Title "Archivalia - Fine Service" -Command "`$env:SPRING_PROFILES_ACTIVE = '$profile'; mvn -pl fine-service spring-boot:run" -Port 8083
Start-ArchivaliaTerminal -Title "Archivalia - Borrow Service" -Command "`$env:SPRING_PROFILES_ACTIVE = '$profile'; mvn -pl borrow-service spring-boot:run" -Port 8084
Start-ArchivaliaTerminal -Title "Archivalia - API Gateway" -Command "mvn -pl api-gateway spring-boot:run" -Port 8080
Start-ArchivaliaTerminal -Title "Archivalia - Frontend" -Command "Set-Location -LiteralPath '$projectRoot\frontend'; npm run dev" -Port 5173

Write-Output "Application windows are starting with the '$profile' database profile."
if ($profile -eq "demo") {
    Write-Output "Demo mode uses in-memory H2 databases. Data is cleared when a service restarts."
}
else {
    Write-Output "Read the Auth, Book, Borrow, and Fine terminal output to verify PostgreSQL connections."
}
Write-Output "Open the frontend at http://localhost:5173, the gateway at http://localhost:8080, and Eureka at http://localhost:8761."
