#!/bin/sh
SCRIPT=$(find . -type f -name scapig-developer-hub)
rm -f scapig-developer-hub*/RUNNING_PID
exec $SCRIPT -Dhttp.port=9017 -J-Xms16M -J-Xmx64m
