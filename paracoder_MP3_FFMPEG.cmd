@echo off
chcp 65001 > nul

java -XX:AOTCache=paracoder.aot -XX:+UseCompactObjectHeaders -Dspring.aot.enabled=true -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar d:\java\prj\paracoder\build\libs\extracted\ParaCoder.jar --recurse --thread-count=5 --config-location=paracoder_commands_MP3_FFMPEG.yml %*

echo ERRORLEVEL:%ERRORLEVEL%
pause
