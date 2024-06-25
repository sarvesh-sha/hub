if [ ! -d testOutput ]; then
  mkdir testOutput
fi
docker run --rm -it -p 3100:3100 -v ${PWD}/src:/home/pptruser/app/src -v ${PWD}/testOutput:/optio3-test-output optio3-tester npm start
