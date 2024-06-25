#!/bin/bash

if [ -d /usr/src/app/src-shadow ]; then
	echo Starting background file copy...
	inotify-hookable -w /usr/src/app/src-shadow -c 'rsync --verbose --checksum --delete -a /usr/src/app/src-shadow/ /usr/src/app/src' >&/var/log/file-monitor.log &
fi
