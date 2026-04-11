@echo off
chcp 65001 > nul

java -XX:AOTCache=.\build\libs\paracoder.aot -XX:+UseCompactObjectHeaders -Dspring.aot.enabled=true -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar .\build\libs\extracted\ParaCoder.jar -pf=false --thread-count=5 --config-location=paracoder_commands_PDF.yml %*

echo ERRORLEVEL:%ERRORLEVEL%
pause
