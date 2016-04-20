@echo off
::javap -verbose \out\production\j2c2\com\lk\j2c2\MyFunction > data\MyFunction.txt
javap -verbose %1 > %2