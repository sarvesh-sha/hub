#!/bin/bash

dir=`dirname ${0}`

#find . -name '*.java' -exec sh ${dir}/updateCopyrightForFile.sh {} \;
git ls-files '*.java' | xargs -n 1 sh ${dir}/updateCopyrightForFile.sh
