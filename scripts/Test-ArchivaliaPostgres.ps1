$hostName = if ([string]::IsNullOrWhiteSpace($env:POSTGRES_HOST)) { "localhost" } else { $env:POSTGRES_HOST }
$port = if ([string]::IsNullOrWhiteSpace($env:POSTGRES_PORT)) { "5432" } else { $env:POSTGRES_PORT }
$database = if ([string]::IsNullOrWhiteSpace($env:POSTGRES_DB)) { "archivalia" } else { $env:POSTGRES_DB }
$username = if ([string]::IsNullOrWhiteSpace($env:POSTGRES_USER)) { "archivalia_user" } else { $env:POSTGRES_USER }

$plainPassword = $env:POSTGRES_PASSWORD
$temporaryPassword = $false
if ([string]::IsNullOrWhiteSpace($plainPassword)) {
    $securePassword = Read-Host "PostgreSQL password for $username" -AsSecureString
    $pointer = [IntPtr]::Zero
    try {
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
        $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        if ($pointer -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        }
    }
    $temporaryPassword = $true
}

$previousPsqlPassword = $env:PGPASSWORD
$env:PGPASSWORD = $plainPassword
$plainPassword = $null

$psql = Get-Command "psql" -ErrorAction SilentlyContinue
if (-not $psql) {
    $knownClientPaths = @(
        "C:\Program Files\PostgreSQL\16\bin\psql.exe",
        "C:\Program Files\PostgreSQL\18\bin\psql.exe",
        "C:\Program Files (x86)\PostgreSQL\16\bin\psql.exe"
    )
    $clientPath = $knownClientPaths | Where-Object { Test-Path $_ } | Select-Object -First 1
    if ($clientPath) {
        $psql = Get-Item $clientPath
    }
}

if (-not $psql) {
    $env:PGPASSWORD = $previousPsqlPassword
    if ($temporaryPassword) {
        $securePassword.Dispose()
    }
    throw "psql was not found on PATH or in the usual PostgreSQL install folders. This does not mean your password is wrong. Verify the connection in pgAdmin Query Tool using: SELECT current_database(), current_user, version();"
}

Write-Output "Connecting to database '$database' on ${hostName}:$port as '$username'."
Write-Output "Checking authentication with the password provided in the hidden setup prompt."
$psqlExecutable = if ($psql.CommandType -eq "Application") { $psql.Source } else { $psql.FullName }
try {
    & $psqlExecutable -h $hostName -p $port -U $username -d $database -v ON_ERROR_STOP=1 -c "SELECT current_database() AS database, current_user AS role, version();"
    $psqlExitCode = $LASTEXITCODE
}
finally {
    $env:PGPASSWORD = $previousPsqlPassword
    if ($temporaryPassword) {
        $securePassword.Dispose()
    }
}
if ($psqlExitCode -ne 0) {
    throw "PostgreSQL connectivity check failed with psql exit code $psqlExitCode. The database and role were not modified."
}
