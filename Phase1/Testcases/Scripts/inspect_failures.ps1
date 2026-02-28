param(
  [string]$TestId = "",
  [int]$DiffLines = 30,
  [int]$ShowLines = 120
)

$ErrorActionPreference = "Stop"
$root = (Get-Location).Path

$inputDir = Join-Path $root "phase1\Testcases\input"
$expDir   = Join-Path $root "phase1\Testcases\expected"
$outDir   = Join-Path $root "phase1\Testcases\output"

if (!(Test-Path $inputDir)) { throw "Missing input dir: $inputDir" }
if (!(Test-Path $expDir))   { throw "Missing expected dir: $expDir" }
if (!(Test-Path $outDir))   { throw "Missing output dir: $outDir" }

function ReadLines([string]$path) {
  if (!(Test-Path $path)) { return @("<MISSING: $path>") }
  $lines = @(Get-Content -Path $path -ErrorAction Stop)
  if ($lines.Count -eq 0) { return @("<EMPTY FILE>") }
  return $lines
}

function PrintBlock([string]$title, [string[]]$lines, [int]$maxLines) {
  Write-Host $title
  $n = [Math]::Min($maxLines, $lines.Count)
  for ($i=0; $i -lt $n; $i++) {
    $num = "{0:D3}" -f ($i+1)
    Write-Host "  $num  $($lines[$i])"
  }
  if ($lines.Count -gt $maxLines) {
    Write-Host "  ... ($($lines.Count - $maxLines) more lines)"
  }
}

function DiffPreview([string]$expPath, [string]$actPath, [int]$take) {
  if (!(Test-Path $expPath)) { return @("Missing expected: $expPath") }
  if (!(Test-Path $actPath)) { return @("Missing actual:   $actPath") }
  $exp = @(Get-Content -Path $expPath -ErrorAction Stop)
  $act = @(Get-Content -Path $actPath -ErrorAction Stop)
  $diff = Compare-Object -ReferenceObject $exp -DifferenceObject $act
  if (!$diff) { return @() }
  return @(
    $diff | Select-Object -First $take | ForEach-Object {
      "{0} {1}" -f $_.SideIndicator, $_.InputObject
    }
  )
}

$tests = Get-ChildItem -Path $inputDir -Filter "TC-*.txt" | Sort-Object Name
if ($tests.Count -eq 0) { throw "No tests found in $inputDir" }

if ($TestId -ne "") {
  $tests = $tests | Where-Object { $_.BaseName -eq $TestId }
  if ($tests.Count -eq 0) { throw "TestId not found: $TestId" }
}

$failed = New-Object System.Collections.Generic.List[string]

# Identify failures (stdout + atf)
foreach ($t in $tests) {
  $id = $t.BaseName
  $expOut = Join-Path $expDir "$id.out"
  $actOut = Join-Path $outDir "$id.out"
  $expAtf = Join-Path $expDir "$id.atf"
  $actAtf = Join-Path $outDir "$id.atf"

  $outDiff = DiffPreview $expOut $actOut 1
  $atfDiff = DiffPreview $expAtf $actAtf 1

  if ($outDiff.Count -gt 0 -or $atfDiff.Count -gt 0) { $failed.Add($id) }
}

if ($failed.Count -eq 0) {
  Write-Host "No failing tests detected."
  exit 0
}

foreach ($id in $failed) {
  $inPath  = Join-Path $inputDir "$id.txt"
  $expOut  = Join-Path $expDir "$id.out"
  $actOut  = Join-Path $outDir "$id.out"
  $expAtf  = Join-Path $expDir "$id.atf"
  $actAtf  = Join-Path $outDir "$id.atf"

  Write-Host "`n====================================================="
  Write-Host "FAILED: $id"
  Write-Host "====================================================="

  PrintBlock "-- INPUT ($id.txt) --" (ReadLines $inPath) 60

  $stdoutDiff = DiffPreview $expOut $actOut $DiffLines
  if ($stdoutDiff.Count -gt 0) {
    Write-Host "`n-- STDOUT DIFF PREVIEW (first $DiffLines changes) --"
    $stdoutDiff | ForEach-Object { Write-Host "  $_" }
  } else {
    Write-Host "`n-- STDOUT: no differences --"
  }

  PrintBlock "`n-- EXPECTED STDOUT ($id.out) --" (ReadLines $expOut) $ShowLines
  PrintBlock "`n-- ACTUAL STDOUT ($id.out) --"   (ReadLines $actOut) $ShowLines

  $atfDiff = DiffPreview $expAtf $actAtf $DiffLines
  if ($atfDiff.Count -gt 0) {
    Write-Host "`n-- ATF DIFF PREVIEW (first $DiffLines changes) --"
    $atfDiff | ForEach-Object { Write-Host "  $_" }
  } else {
    Write-Host "`n-- ATF: no differences --"
  }
}