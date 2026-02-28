# compare_all.ps1
# Compares actual outputs against expected outputs for ALL tests.
# Checks BOTH:
#   - terminal output: expected\TC-xx.out vs output\TC-xx.out
#   - transaction file: expected\TC-xx.atf vs output\TC-xx.atf

$ErrorActionPreference = "Stop"
$root = (Get-Location).Path

$inputDir = Join-Path $root "phase1\Testcases\input"
$expDir   = Join-Path $root "phase1\Testcases\expected"
$outDir   = Join-Path $root "phase1\Testcases\output"

$tests = Get-ChildItem -Path $inputDir -Filter "TC-*.txt" | Sort-Object Name
if ($tests.Count -eq 0) { throw "No tests found in $inputDir" }

$failed = @()

function Get-LinesSafe([string]$path) {
  if (!(Test-Path $path)) { return @() }          # return empty array, not $null
  return @(Get-Content -Path $path -ErrorAction Stop)
}

foreach ($t in $tests) {
  $id = $t.BaseName

  $expOut = Join-Path $expDir "$id.out"
  $actOut = Join-Path $outDir "$id.out"

  $expAtf = Join-Path $expDir "$id.atf"
  $actAtf = Join-Path $outDir "$id.atf"

  $missing = @()
  foreach ($p in @($expOut, $actOut, $expAtf, $actAtf)) {
    if (!(Test-Path $p)) { $missing += $p }
  }

  if ($missing.Count -gt 0) {
    Write-Host "MISSING $id"
    $missing | ForEach-Object { Write-Host "  $_" }
    $failed += "$id (missing files)"
    continue
  }

$outDiff = Compare-Object -ReferenceObject @(Get-LinesSafe $expOut) -DifferenceObject @(Get-LinesSafe $actOut)
$atfDiff = Compare-Object -ReferenceObject @(Get-LinesSafe $expAtf) -DifferenceObject @(Get-LinesSafe $actAtf)
  if ($outDiff -or $atfDiff) {
    Write-Host "FAIL  $id"
    $failed += $id

    if ($outDiff) {
      Write-Host "  stdout diff (first 10 lines):"
      $outDiff | Select-Object -First 10 | ForEach-Object { Write-Host "   $($_.SideIndicator) $($_.InputObject)" }
    }

    if ($atfDiff) {
      Write-Host "  atf diff (first 10 lines):"
      $atfDiff | Select-Object -First 10 | ForEach-Object { Write-Host "   $($_.SideIndicator) $($_.InputObject)" }
    }
  }
  else {
    Write-Host "PASS  $id"
  }
}

"`nFAILED:"
$failed
if ($failed.Count -gt 0) { exit 1 } else { exit 0 }