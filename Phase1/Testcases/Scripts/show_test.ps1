param([Parameter(Mandatory)] [string]$Id)

$e = "phase1\Testcases\expected\$Id.out"
$o = "phase1\Testcases\output\$Id.out"

Write-Host "`n--- EXPECTED ---"
Get-Content $e

Write-Host "`n--- ACTUAL ---"
Get-Content $o

Write-Host "`n--- DIFF ---"
Compare-Object (Get-Content $e) (Get-Content $o)