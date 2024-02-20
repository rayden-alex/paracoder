@echo off
chcp 65001 > nul

java --enable-preview -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar .\build\libs\ParaCoder.jar --help

pause
