setlocal
echo OFF

set IMAGE_TAG=optio3-node--waypoint

rd /s/q output 2>nul

docker build -t %IMAGE_TAG% .
docker image prune -f

docker run --rm -v .\output:/usr/src/app/output %IMAGE_TAG% npm run build:dev
