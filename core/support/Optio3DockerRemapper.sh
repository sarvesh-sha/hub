#!/bin/bash

dir=$1
subdir=

debug=${OPTIO3_DOCKER_BIND_DEBUG}

fallbackValue=$dir

if [ ! -z "${debug}" ]; then
    env 1>&2
    echo Input: $dir 1>&2
fi

function concat() {
    if [ -z "$2" ]; then
        echo $1
    else
        echo $1/$2
    fi
}

while [ ! -z "${dir}" ]; do

    #
    # The shell doesn't like slashes in the name of environment variables.
    # We have to escape a couple of characters:
    #
    #      _   =>   _u_
    #      .   =>   _d_
    #      /   =>   _s_
    #
    dirEscaped=${dir//_/_u_}
    dirEscaped=${dirEscaped//\./_d_}
    dirEscaped=${dirEscaped//\//_s_}

    eval __env=\${OPTIO3_DOCKER_BIND_$dirEscaped}

    if [ ! -z "${__env}" ]; then
        if [ ! -z "${debug}" ]; then
            echo Found: "${__env}" "${subdir}" 1>&2
        fi

        echo $(concat "${__env}" "${subdir}")
        exit 0
    fi

    subdir=$(concat "`basename ${dir}`" "${subdir}")

    dir=`dirname ${dir}`

    if [ "${dir}" == "/" -o "${dir}" == "." ]; then
        break
    fi
done

if [ ! -z "${debug}" ]; then
    echo Fallback: $fallbackValue 1>&2
fi

echo $fallbackValue
