#!/bin/bash

img=repo.dev.optio3.io:5000/optio3-reporter:reporter_master_20210311_0930

docker run --rm -it -p 3000:3000 -v ${PWD}/src:/home/pptruser/app/src --tmpfs /optio3-scratch ${img} npm start
