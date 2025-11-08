$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method POST -ContentType "application/json" -Body '{}'
Write-Host "Register Response:"
$response | ConvertTo-Json

# Test a second registration to see different random nickname
$response2 = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method POST -ContentType "application/json" -Body '{}'
Write-Host "Second Register Response:"
$response2 | ConvertTo-Json