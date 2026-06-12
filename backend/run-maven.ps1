# run-maven.ps1
# PowerShell runner script to detect, download, and execute Maven inside the backend directory.

param (
    [string]$Command = "spring-boot:run"
)

$MavenVersion = "3.9.9"
$LocalMavenDir = Join-Path $PSScriptRoot ".maven"
$MavenHome = Join-Path $LocalMavenDir "apache-maven-$MavenVersion"
$MavenBin = Join-Path $MavenHome "bin\mvn.cmd"

# Check if global mvn command is available in PATH
$MvnPath = Get-Command mvn -ErrorAction SilentlyContinue
if ($MvnPath) {
    Write-Host "Detected global Maven installation at $($MvnPath.Source)"
    Invoke-Expression "mvn $Command"
    exit $LASTEXITCODE
}

# If not in path, check if local maven is downloaded
if (-not (Test-Path $MavenBin)) {
    Write-Host "Global Maven not found. Installing standalone Maven version $MavenVersion locally..."
    
    if (-not (Test-Path $LocalMavenDir)) {
        New-Item -ItemType Directory -Path $LocalMavenDir -Force | Out-Null
    }
    
    $DownloadUrl = "https://archive.apache.org/dist/maven/maven-3/$MavenVersion/binaries/apache-maven-$MavenVersion-bin.zip"
    $ZipPath = Join-Path $LocalMavenDir "maven.zip"
    
    Write-Host "Downloading Maven from $DownloadUrl..."
    Invoke-WebRequest -Uri $DownloadUrl -OutFile $ZipPath
    
    Write-Host "Extracting Maven..."
    Expand-Archive -Path $ZipPath -DestinationPath $LocalMavenDir -Force
    
    Write-Host "Cleaning zip file..."
    Remove-Item $ZipPath -Force
}

if (Test-Path $MavenBin) {
    Write-Host "Running local Maven command: '$MavenBin $Command'"
    
    # Set JAVA_HOME environment variable if not already set
    if (-not $env:JAVA_HOME) {
        # Check local JDK first
        $LocalJdk = Join-Path $PSScriptRoot ".jdk\jdk-21"
        if (Test-Path "$LocalJdk\bin\java.exe") {
            $env:JAVA_HOME = $LocalJdk
            Write-Host "Set JAVA_HOME to local JDK: $LocalJdk"
        } else {
            # Try known JDK installation paths first
            $KnownPaths = @(
                "C:\Program Files\Java\jdk-24",
                "C:\Program Files\Java\jdk-21",
                "C:\Program Files\Eclipse Adoptium\jdk-21",
                "C:\Program Files\Microsoft\jdk-21"
            )
            foreach ($p in $KnownPaths) {
                if (Test-Path "$p\bin\java.exe") {
                    $env:JAVA_HOME = $p
                    Write-Host "Set JAVA_HOME to $p"
                    break
                }
            }
            # Fall back to auto-detect from PATH
            if (-not $env:JAVA_HOME) {
                $JavaCmd = Get-Command java -ErrorAction SilentlyContinue
                if ($JavaCmd) {
                    $ResolvedPath = (Get-Item $JavaCmd.Source -ErrorAction SilentlyContinue)
                    if ($ResolvedPath -and $ResolvedPath.Target) {
                        $env:JAVA_HOME = (Get-Item $ResolvedPath.Target[0]).Directory.Parent.FullName
                    } else {
                        $env:JAVA_HOME = $ResolvedPath.Directory.Parent.FullName
                    }
                    Write-Host "Auto-detected JAVA_HOME: $($env:JAVA_HOME)"
                }
            }
        }
    }

    # Run the command
    $CommandTokens = $Command -split " "
    & $MavenBin @CommandTokens
    exit $LASTEXITCODE
} else {
    Write-Error "Failed to install/find Maven locally."
    exit 1
}
