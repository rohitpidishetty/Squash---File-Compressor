@echo off

echo.
echo.
powershell -Command ^
"Write-Host '  ######    ######    ##    ##    ###     ######  ##   ## ' -ForegroundColor Yellow; ^
 Write-Host ' ##        ##    ##   ##    ##   ## ##   ##       ##   ## ' -ForegroundColor Yellow; ^
 Write-Host '  #####    ## ## ##   ##    ##  #######   #####   ####### ' -ForegroundColor Yellow; ^
 Write-Host '      ##   ##   ##    ##    ##  ##   ##       ##  ##   ## ' -ForegroundColor Yellow; ^
 Write-Host ' ######     #### ##    ######   ##   ##  ######   ##   ## ' -ForegroundColor Yellow"

echo.
echo.
powershell -Command ^
"Write-Host '          All @copyrights reserved by NFRAC   ' -ForegroundColor Blue;"
echo.
echo.

if "%1"=="" goto :usage




if "%1"=="-squash" (
  if "%2"=="" goto :usage   
  if "%3"=="" goto :usage
  if "%4"=="" goto :usage
  java Squash -squash %2 %3 %4
  exit /b
)

if "%1"=="-desquash" (
  if "%2"=="" goto :usage
  if "%3"=="" goto :usage
  java Squash -desquash %2 %3 
  exit /b
)

:usage
echo Usage: squash -squash origin_path target_file_name target_path
echo Usage: squash -desquash target_file_name.sq target_path
exit /b
