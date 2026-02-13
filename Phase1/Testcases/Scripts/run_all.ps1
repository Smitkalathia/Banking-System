<#
$root = (Get-Location).Path

$accounts = Join-Path $root "phase2\data\currentaccounts.txt"
if (!(Test-Path $accounts)) { throw "Missing accounts file: $accounts" }

$tests = Get-ChildItem -Path "phase1\Testcases\input" -Filter "TC-*.txt" | Sort-Object Name
if ($tests.Count -eq 0) { throw "No tests found in phase1\Testcases\input" }

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

foreach ($t in $tests) {
  $name   = $t.BaseName
  $input  = $t.FullName
  $output = Join-Path $root "phase1\Testcases\output\$name.txt"
  $trans  = Join-Path $root "phase1\Testcases\output\$name.atf"

  New-Item -ItemType Directory -Force -Path (Split-Path $output) | Out-Null

  $lines =
    Get-Content $input |
    java -cp phase2\bin AtmApp $accounts $trans

  [System.IO.File]::WriteAllLines($output, $lines, $utf8NoBom)

  Write-Host "ran $name"
}
#>