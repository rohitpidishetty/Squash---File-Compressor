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

if "%1"=="-compress" (
  if "%2"=="" goto :usage
  if "%3"=="" goto :usage
  java Squash -compress %2 %3
  exit /b
)

if "%1"=="-decompress" (
  if "%2"=="" goto :usage
  java Squash -decompress %2
  exit /b
)

:usage
echo Usage: squash -compress origin_path target_file_name
echo Usage: squash -decompress target_file_name.tar.sq
exit /b
