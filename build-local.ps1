param([switch]$SkipChecks)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$taskJavaCandidates = @(
    $env:JAVA_HOME,
    'C:\Program Files\Android\Android Studio\jbr',
    'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
)
$taskJavaHome = $taskJavaCandidates | Where-Object { $_ -and (Test-Path -LiteralPath (Join-Path $_ 'bin\java.exe')) } | Select-Object -First 1
if (-not $taskJavaHome) { throw 'Chưa tìm thấy JDK. Chọn Gradle JDK 21 trong Android Studio hoặc đặt JAVA_HOME đúng.' }
$env:JAVA_HOME = $taskJavaHome
if ($SkipChecks) { & .\gradlew.bat assembleDebug }
else { & .\gradlew.bat assembleDebug testDebugUnitTest lintDebug }
exit $LASTEXITCODE
