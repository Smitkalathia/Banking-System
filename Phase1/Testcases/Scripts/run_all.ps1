# run_all.ps1
# Runs all Phase 1 acceptance tests against the Front End (Phase 2/3).
# Produces for each test:
#   - phase1\Testcases\output\<TC-xx>.out  (terminal output)
#   - phase1\Testcases\output\<TC-xx>.atf  (daily transaction file)

$ErrorActionPreference = "Stop"
$root = (Get-Location).Path

$accounts = Join-Path $root "phase2\data\currentaccounts.txt"
$inputDir = Join-Path $root "phase1\Testcases\input"
$outDir   = Join-Path $root "phase1\Testcases\output"

if (!(Test-Path $accounts)) { throw "Missing accounts file: $accounts" }
if (!(Test-Path $inputDir)) { throw "Missing input directory: $inputDir" }

New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$tests = Get-ChildItem -Path $inputDir -Filter "TC-*.txt" | Sort-Object Name
if ($tests.Count -eq 0) { throw "No tests found in $inputDir" }

# UTF-8 without BOM (prevents strange BOM characters)
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

foreach ($t in $tests) {
  $id     = $t.BaseName
  $inFile = $t.FullName

  $outFile = Join-Path $outDir "$id.out"
  $atfFile = Join-Path $outDir "$id.atf"

  if ([string]::IsNullOrWhiteSpace($atfFile)) { throw "Internal error: atfFile path is null for $id" }

  # Clean previous outputs (avoids stale results)
  if (Test-Path $outFile) { Remove-Item $outFile -Force }
  if (Test-Path $atfFile) { Remove-Item $atfFile -Force }

  # Always create the .atf file so it exists even if the program exits early
  New-Item -ItemType File -Path $atfFile -Force | Out-Null

  Write-Host "running $id ..."

  # Run program: stdin from test file, stdout captured to $lines, .atf written by program
  $lines = Get-Content -Path $inFile | java -cp phase2\bin AtmApp $accounts $atfFile

  # Always write stdout to file (even if empty)
  [System.IO.File]::WriteAllLines($outFile, @($lines), $utf8NoBom)

  Write-Host "wrote  $outFile"
  Write-Host "wrote  $atfFile"
}