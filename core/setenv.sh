#!/bin/bash

export O3_ROOT_DIR=${PWD}
#`git rev-parse --show-toplevel`

function addToPath() {
    local target
    target=${1}

    for i in ${PATH//:/$'\n'}; do {
        if [ "$i" == "${target}" ]; then
            return
        fi
    }; done
    export PATH="${PATH}:${target}"
}

function makeHomeRelative() {
    local target noprefix
    target=${1}

    noprefix=${target#${HOME}}
    if [ "${noprefix}" != "${target}" ]; then
        echo "~${noprefix}"
    else
        echo "${target}"
    fi
}

O3_ROOT_DIR=$(makeHomeRelative $O3_ROOT_DIR)

addToPath ${O3_ROOT_DIR}/scripts

o3 tooling check-intellij-tools
