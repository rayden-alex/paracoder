@echo off
chcp 65001 > nul

@echo Started: %time%
java -XX:AOTCache=paracoder.aot --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar .\build\libs\ParaCoder.jar --help
@echo Completed: %time%

pause
