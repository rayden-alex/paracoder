@echo off
chcp 65001 > nul

rem Set this cmd-script path as a "working dir".
rem This is not mandatory since a valid working dir set in the shortcut (.lnk) to this cmd-script path.
rem pushd %~dp0
rem echo %CD%

::for /F %a in ('echo prompt $E ^| cmd') do set "ESC=%a"
set ESC=
set Green=%ESC%[32m
set Red=%ESC%[41;93m
set ColorOff=%ESC%[0m

java -XX:AOTCache=.\build\libs\paracoder.aot -XX:+UseCompactObjectHeaders -Dspring.aot.enabled=true -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar .\build\libs\extracted\ParaCoder.jar --recurse -d --thread-count=5 --config-location=paracoder_commands_OPUS_ARDFTSRC.yml %*

if %ERRORLEVEL% NEQ 0 (
  echo %Red% ERRORLEVEL: %ERRORLEVEL% %ColorOff%
)

rem popd

pause
