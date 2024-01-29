@echo off
chcp 65001 > nul

java --enable-preview -jar .\build\libs\ParaCoder-1.0.2.jar --recurse --thread-count=5 -d=false %*

echo ERRORLEVEL:%ERRORLEVEL%

pause
