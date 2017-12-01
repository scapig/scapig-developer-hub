#!/bin/sh
SCRIPT=$(find . -type f -name tapi-developer-hub)
exec $SCRIPT -Dhttp.port=8010
