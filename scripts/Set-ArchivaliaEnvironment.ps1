$profile = if ([string]::IsNullOrWhiteSpace($env:ARCHIVALIA_PROFILE)) {
    "demo"
}
else {
    $env:ARCHIVALIA_PROFILE.ToLowerInvariant()
}
if ($profile -notin @("demo", "postgres")) {
    throw "ARCHIVALIA_PROFILE must be 'demo' or 'postgres'. No credentials were changed."
}

$postgresUsername = if ([string]::IsNullOrWhiteSpace($env:POSTGRES_USER)) {
    "archivalia_user"
}
else {
    $env:POSTGRES_USER
}
$adminEmailDefault = if ([string]::IsNullOrWhiteSpace($env:ADMIN_EMAIL)) {
    "admin@archivalia.local"
}
else {
    $env:ADMIN_EMAIL
}

function Convert-SecureStringToPlainText {
    param([Parameter(Mandatory = $true)][Security.SecureString]$SecureValue)

    $pointer = [IntPtr]::Zero
    try {
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        if ($pointer -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        }
    }
}

if ($profile -eq "postgres") {
    $postgresPasswordSecure = Read-Host "PostgreSQL password for $postgresUsername" -AsSecureString
    $postgresPassword = Convert-SecureStringToPlainText $postgresPasswordSecure
    if ([string]::IsNullOrWhiteSpace($postgresPassword)) {
        throw "A PostgreSQL password is required for the postgres profile. No environment variables were changed."
    }
}

$jwtSecretSecure = Read-Host "JWT signing secret (at least 32 characters; press Enter to generate one)" -AsSecureString
$adminPasswordSecure = Read-Host "Initial administrator password (at least 12 characters)" -AsSecureString

$adminPassword = Convert-SecureStringToPlainText $adminPasswordSecure
$adminEmail = Read-Host "Administrator email [$adminEmailDefault]"
if ([string]::IsNullOrWhiteSpace($adminEmail)) { $adminEmail = $adminEmailDefault }
try {
    $null = [System.Net.Mail.MailAddress]::new($adminEmail)
}
catch {
    throw "ADMIN_EMAIL must be a valid email address. No environment variables were changed."
}

if ($jwtSecretSecure.Length -eq 0) {
    $jwtBytes = New-Object byte[] 32
    $randomGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $randomGenerator.GetBytes($jwtBytes)
        $jwtSecret = [Convert]::ToBase64String($jwtBytes)
    }
    finally {
        $randomGenerator.Dispose()
        [Array]::Clear($jwtBytes, 0, $jwtBytes.Length)
    }
}
else {
    $jwtSecret = Convert-SecureStringToPlainText $jwtSecretSecure
    if ([System.Text.Encoding]::UTF8.GetByteCount($jwtSecret) -lt 32) {
        throw "JWT_SECRET must be at least 32 UTF-8 bytes. No environment variables were changed."
    }
}
if ($adminPassword.Length -lt 12) {
    throw "ADMIN_PASSWORD must be at least 12 characters. No environment variables were changed."
}

if ([string]::IsNullOrWhiteSpace($env:POSTGRES_HOST)) { $env:POSTGRES_HOST = "localhost" }
if ([string]::IsNullOrWhiteSpace($env:POSTGRES_PORT)) { $env:POSTGRES_PORT = "5432" }
if ([string]::IsNullOrWhiteSpace($env:POSTGRES_DB)) { $env:POSTGRES_DB = "archivalia" }
if ([string]::IsNullOrWhiteSpace($env:POSTGRES_USER)) { $env:POSTGRES_USER = $postgresUsername }
if ([string]::IsNullOrWhiteSpace($env:POSTGRES_HOST_DOCKER)) { $env:POSTGRES_HOST_DOCKER = "host.docker.internal" }
if ([string]::IsNullOrWhiteSpace($env:LOAN_PERIOD_DAYS)) { $env:LOAN_PERIOD_DAYS = "14" }
if ([string]::IsNullOrWhiteSpace($env:FINE_PER_DAY)) { $env:FINE_PER_DAY = "1.00" }

$env:ARCHIVALIA_PROFILE = $profile
if ($profile -eq "postgres") {
    $env:POSTGRES_PASSWORD = $postgresPassword
}
$env:JWT_SECRET = $jwtSecret
$env:ADMIN_PASSWORD = $adminPassword
$env:ADMIN_EMAIL = $adminEmail

Write-Output "Archivalia credentials are set in this PowerShell process only."
Write-Output "Backend database profile: $profile"
if ($profile -eq "demo") {
    Write-Output "The demo profile uses separate in-memory H2 databases. Data is cleared when each service restarts."
}
Write-Output "No credential was saved to a file or printed. Start Maven or Docker Compose from this same terminal."
Write-Output "Keep this terminal open while using the credentials. If you close it, run this script again."
