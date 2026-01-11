@echo off
chcp 65001 > nul

@echo Started: %time%
java -XX:AOTCache=paracoder.aot -XX:+UseCompactObjectHeaders -Dspring.aot.enabled=true -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar .\build\libs\extracted\ParaCoder.jar --help
@echo Completed: %time%

pause
