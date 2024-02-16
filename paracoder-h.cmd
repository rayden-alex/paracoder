@echo off
chcp 65001 > nul

java --enable-preview -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar .\build\libs\ParaCoder-1.0.3.jar --help

pause
