# run_one.ps1
param(
  [Parameter(Mandatory=$true)]
  [string]$TestId
)

$ErrorActionPreference = "Stop"
$root = (Get-Location).Path

$accounts = Join-Path $root "phase2\data\currentaccounts.txt"
$inputDir = Join-Path $root "phase1\Testcases\input"
$outDir   = Join-Path $root "phase1\Testcases\output"

if (!(Test-Path $accounts)) { throw "Missing accounts file: $accounts" }
if (!(Test-Path $inputDir)) { throw "Missing input directory: $inputDir" }

# Allow passing "TC-12" or "TC-12.txt"
$base = [System.IO.Path]::GetFileNameWithoutExtension($TestId)
if ([string]::IsNullOrWhiteSpace($base)) { throw "Invalid TestId: '$TestId'" }

$inFile  = Join-Path $inputDir "$base.txt"
$outFile = Join-Path $outDir   "$base.out"
$atfFile = Join-Path $outDir   "$base.atf"

if (!(Test-Path $inFile)) { throw "Missing input file: $inFile" }

New-Item -ItemType Directory -Force -Path $outDir | Out-Null

# UTF-8 without BOM
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

# Clean previous outputs (avoids stale results)
if (Test-Path $outFile) { Remove-Item $outFile -Force }
if (Test-Path $atfFile) { Remove-Item $atfFile -Force }

# Always create the .atf file so it exists even if the program exits early
New-Item -ItemType File -Path $atfFile -Force | Out-Null

Write-Host "running $base ..."

# Run program: stdin from test file, stdout captured to $lines, .atf written by program
$lines = Get-Content -Path $inFile | java -cp phase2\bin AtmApp $accounts $atfFile

# Always write stdout to file (even if empty)
[System.IO.File]::WriteAllLines($outFile, @($lines), $utf8NoBom)

Write-Host "wrote  $outFile"
Write-Host "wrote  $atfFile"