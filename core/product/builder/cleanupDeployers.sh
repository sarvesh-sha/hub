#!/bin/bash

for i in $(docker ps --filter "label=Optio3_Deployment_Purpose" -q); do
    docker rm -f $i || exit $?
done

for i in $(docker volume ls --filter "label=Optio3_Deployment_Purpose" -q); do
    docker volume rm $i || exit $?
done
