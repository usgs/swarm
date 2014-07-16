@echo off
set OLDPATH=%PATH%
set PATH=%PATH%;.\lib
java -Duser.country=US -Duser.language=us -jar lib/Swarm.jar %1 %2 %3 %4 %5 %6 %7 %8 %9
set PATH=%OLDPATH%
