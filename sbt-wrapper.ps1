param(
  [Parameter(ValueFromRemainingArguments = $true)]
  [string[]]$PassedArgs
)

$launcher = "D:\Development\sbt\bin\sbtn-x86_64-pc-win32.exe"
$joinedArgs = $PassedArgs -join " "
$isPrintInvocation = $joinedArgs -match "(^|\s)print(\s|$)"

$output = & $launcher @PassedArgs 2>&1
$exitCode = $LASTEXITCODE

if (-not $isPrintInvocation) {
  $output
  exit $exitCode
}

$cleanOutput = $output | ForEach-Object {
  [regex]::Replace($_.ToString(), "\x1b\[[0-9;]*[A-Za-z]", "")
}

if ($exitCode -ne 0) {
  $cleanOutput
  exit $exitCode
}

$path = $cleanOutput |
  ForEach-Object { $_.Trim() } |
  Where-Object { $_ -match "^[A-Za-z]:\\" } |
  Select-Object -Last 1

if ($null -eq $path) {
  $cleanOutput
  exit 1
}

Write-Output $path
