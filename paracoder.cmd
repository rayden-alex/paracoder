@echo off
chcp 65001 > nul

java --enable-preview -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar d:\java\prj\paracoder\build\libs\ParaCoder-1.0.2.jar --recurse --thread-count=5 -d %*

echo ERRORLEVEL:%ERRORLEVEL%

pause
