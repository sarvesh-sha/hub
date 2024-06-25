#!/bin/bash

IMAGE_TAG=optio3-node--hub

rm -rf output

oldVersion=`docker image inspect -f "{{ .Id }}" ${IMAGE_TAG} 2>/dev/null`

docker build -t ${IMAGE_TAG} .

newVersion=`docker image inspect -f "{{ .Id }}" ${IMAGE_TAG} 2>/dev/null`

if [ "$oldVersion" != "$newVersion" -a "$oldVersion" != "" ]; then
	docker rmi $oldVersion
fi

docker run --rm -v $(PWD)/output:/usr/src/app/output ${IMAGE_TAG} npm run build:dev

