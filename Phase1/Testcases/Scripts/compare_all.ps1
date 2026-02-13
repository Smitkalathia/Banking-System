<#
$root = (Get-Location).Path
$failed = @()

$tests = Get-ChildItem -Path "phase1\Testcases\input" -Filter "TC-*.txt" | Sort-Object Name

foreach ($t in $tests) {
  $name = $t.BaseName
  $exp  = Join-Path $root "phase1\Testcases\expected\$name.txt"
  $act  = Join-Path $root "phase1\Testcases\output\$name.txt"

  if (!((Test-Path $exp) -and (Test-Path $act))) {
    Write-Host "MISSING  $name"
    $failed += "$name (missing expected/actual)"
    continue
  }

  if (Compare-Object (Get-Content $exp) (Get-Content $act)) {
    Write-Host "FAIL     $name"
    $failed += $name
  } else {
    Write-Host "PASS     $name"
  }
}

"`nFAILED:"
$failed
#>