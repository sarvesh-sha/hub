#!/bin/bash

dir=`dirname ${0}`

if [ -e "$1" ]; then

    fgrep -q "Optio3, Inc. All Rights Reserved." $1 || {
        echo "Adding copyright to $1"
        mv $1 $1.backup
        cat ${dir}/copyright.txt $1.backup > $1
        rm $1.backup
    }

fi
