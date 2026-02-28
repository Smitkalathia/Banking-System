# show_failed_diffs.ps1
# Prints expected vs actual outputs for FAILED tests (stdout + atf) in a readable way.
# Usage (from repo root):
#   powershell -NoProfile -ExecutionPolicy Bypass -File phase1\Testcases\scripts\show_failed_diffs.ps1
# Optional:
#   -Limit 80     (max lines printed per file)
#   -TestId TC-26 (only one test)

param(
  [int]$Limit = 80,
  [string]$TestId = ""
)

$ErrorActionPreference = "Stop"
$root = (Get-Location).Path

$inputDir = Join-Path $root "phase1\Testcases\input"
$expDir   = Join-Path $root "phase1\Testcases\expected"
$outDir   = Join-Path $root "phase1\Testcases\output"

if (!(Test-Path $inputDir)) { throw "Missing input dir: $inputDir" }
if (!(Test-Path $expDir))   { throw "Missing expected dir: $expDir" }
if (!(Test-Path $outDir))   { throw "Missing output dir: $outDir" }

function Read-Lines([string]$path) {
  if (!(Test-Path $path)) { return @("<MISSING FILE: $path>") }
  $lines = @(Get-Content -Path $path -ErrorAction Stop)
  if ($lines.Count -eq 0) { return @("<EMPTY FILE>") }
  return $lines
}

function Print-Section([string]$title, [string[]]$lines, [int]$limit) {
  Write-Host $title
  $n = [Math]::Min($limit, $lines.Count)
  for ($i = 0; $i -lt $n; $i++) {
    $num = "{0:D4}" -f ($i + 1)
    Write-Host "  $num  $($lines[$i])"
  }
  if ($lines.Count -gt $limit) {
    Write-Host "  ... ($($lines.Count - $limit) more lines)"
  }
}

function Get-DiffPreview([string]$expPath, [string]$actPath, [int]$take) {
  if (!(Test-Path $expPath)) { return @("Missing expected: $expPath") }
  if (!(Test-Path $actPath)) { return @("Missing actual:   $actPath") }

  $exp = @(Get-Content -Path $expPath -ErrorAction Stop)
  $act = @(Get-Content -Path $actPath -ErrorAction Stop)

  $diff = Compare-Object -ReferenceObject $exp -DifferenceObject $act
  if (!$diff) { return @() }

  return @(
    $diff |
      Select-Object -First $take |
      ForEach-Object { "{0} {1}" -f $_.SideIndicator, $_.InputObject }
  )
}

# Build list of tests to examine
$tests = Get-ChildItem -Path $inputDir -Filter "TC-*.txt" | Sort-Object Name
if ($tests.Count -eq 0) { throw "No tests found in $inputDir" }

if ($TestId -ne "") {
  $tests = $tests | Where-Object { $_.BaseName -eq $TestId }
  if ($tests.Count -eq 0) { throw "TestId not found: $TestId" }
}

$failedIds = @()

foreach ($t in $tests) {
  $id = $t.BaseName

  $expOut = Join-Path $expDir "$id.out"
  $actOut = Join-Path $outDir "$id.out"
  $expAtf = Join-Path $expDir "$id.atf"
  $actAtf = Join-Path $outDir "$id.atf"

  $outDiff = Get-DiffPreview $expOut $actOut 1
  $atfDiff = Get-DiffPreview $expAtf $actAtf 1

  if ($outDiff.Count -gt 0 -or $atfDiff.Count -gt 0) {
    $failedIds += $id
  }
}

if ($failedIds.Count -eq 0) {
  Write-Host "No failures detected (based on expected vs output)."
  exit 0
}

foreach ($id in $failedIds) {
  Write-Host "`n=============================="
  Write-Host "FAILED: $id"
  Write-Host "=============================="

  $expOut = Join-Path $expDir "$id.out"
  $actOut = Join-Path $outDir "$id.out"
  $expAtf = Join-Path $expDir "$id.atf"
  $actAtf = Join-Path $outDir "$id.atf"

  $outPreview = Get-DiffPreview $expOut $actOut 20
  $atfPreview = Get-DiffPreview $expAtf $actAtf 20

  if ($outPreview.Count -gt 0) {
    Write-Host "`n-- stdout diff preview (first 20 changes) --"
    $outPreview | ForEach-Object { Write-Host "  $_" }
  } else {
    Write-Host "`n-- stdout: no differences --"
  }

  if ($atfPreview.Count -gt 0) {
    Write-Host "`n-- atf diff preview (first 20 changes) --"
    $atfPreview | ForEach-Object { Write-Host "  $_" }
  } else {
    Write-Host "`n-- atf: no differences --"
  }

  # Print full expected/actual stdout sections for quick eyeballing
  Write-Host "`n-- EXPECTED stdout ($id.out) --"
  Print-Section "EXPECTED:" (Read-Lines $expOut) $Limit

  Write-Host "`n-- ACTUAL stdout ($id.out) --"
  Print-Section "ACTUAL:" (Read-Lines $actOut) $Limit
}