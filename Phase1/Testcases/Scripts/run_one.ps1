<#
param(
  [Parameter(Mandatory=$true)]
  [string]$TestId
)

$root = (Get-Location).Path

$input    = Join-Path $root "phase1\Testcases\input\$TestId.txt"
$expected = Join-Path $root "phase1\Testcases\expected\$TestId.txt"  # optional
$output   = Join-Path $root "phase1\Testcases\output\$TestId.txt"
$trans    = Join-Path $root "phase1\Testcases\output\$TestId.atf"
$accounts = Join-Path $root "phase2\data\currentaccounts.txt"

if (!(Test-Path $input))    { throw "Missing input file: $input" }
if (!(Test-Path $accounts)) { throw "Missing accounts file: $accounts" }

New-Item -ItemType Directory -Force -Path (Split-Path $output) | Out-Null

# UTF-8 without BOM (prevents ∩╗┐)
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

$lines =
  Get-Content $input |
  java -cp phase2\bin AtmApp $accounts $trans

[System.IO.File]::WriteAllLines($output, $lines, $utf8NoBom)

Write-Host "ran $TestId"
#>