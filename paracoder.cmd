@echo off
chcp 65001 > nul

rem Set this cmd-script path as a "working dir".
rem This is not mandatory since a valid working dir set in the shortcut (.lnk) to this cmd-script path.
rem pushd %~dp0
rem echo %CD%

java --enable-preview -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar d:\java\prj\paracoder\build\libs\ParaCoder-1.0.3.jar --recurse --thread-count=5 -d %*

echo ERRORLEVEL:%ERRORLEVEL%

rem popd

pause
