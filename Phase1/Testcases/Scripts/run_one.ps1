param(
  [Parameter(Mandatory=$true)]
  [string]$TestId
)

$ErrorActionPreference = "Stop"
$root = (Get-Location).Path

$input  = Join-Path $root "phase1\Testcases\input\$TestId.txt"
$output = Join-Path $root "phase1\Testcases\output\$TestId.out"
$trans  = Join-Path $root "phase1\Testcases\output\$TestId.atf"

$origAccounts = Join-Path $root "phase2\data\currentaccounts.txt"
$workAccounts = Join-Path $root "phase1\Testcases\output\working_currentaccounts.txt"

if (!(Test-Path $input))        { throw "Missing input file: $input" }
if (!(Test-Path $origAccounts)) { throw "Missing accounts file: $origAccounts" }

New-Item -ItemType Directory -Force -Path (Split-Path $output) | Out-Null

# Create working accounts file if it does not exist yet
if (!(Test-Path $workAccounts)) {
  Copy-Item -Force $origAccounts $workAccounts
}

# UTF-8 without BOM
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

$lines =
  Get-Content $input |
  java -cp phase2\bin AtmApp $workAccounts $trans

[System.IO.File]::WriteAllLines($output, $lines, $utf8NoBom)

Write-Host "ran $TestId (accounts=$workAccounts)"