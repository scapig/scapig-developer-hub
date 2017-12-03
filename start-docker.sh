#!/bin/sh
SCRIPT=$(find . -type f -name tapi-developer-hub)
rm -f tapi-developer-hub*/RUNNING_PID
exec $SCRIPT -Dhttp.port=8010
