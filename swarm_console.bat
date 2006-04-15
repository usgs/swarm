@echo off
java -Xmx256M -cp .;lib/usgs.jar;lib/colt.jar;lib/seed-pdcc.jar;lib/looks-2.0.1.jar gov.usgs.swarm.Swarm %1 %2 %3 %4 %5 %6 %7 %8 %9