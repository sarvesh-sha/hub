#!/bin/bash

scriptDir=`dirname $0`

if [ "${OPTIO3_SKIP_NPM_BUILD}" != "" ]; then
    echo Skipping NPM build for ${PWD}
    exit 0
fi

########################################

function getFilePart() {
    echo ${1##*/}
}

function getDirPart() {
    local dir
    local dirLen
    local filePart
    local filePartLen
    local dirPart
    dir=${1}

    dirLen=${#dir}
    filePart=${dir##*/}
    filePartLen=${#filePart}

    if [ "${dir}" == "${filePart}" ]; then
        # No slashes.
        dirPart=${PWD}
    else
        dirPart=${dir:0:${dirLen} - ${filePartLen} - 1}
        if [ -z "${dirPart}" ]; then
            dirPart=/
        else
            dirPart=$(normalizePath ${dirPart})
        fi
    fi

    echo ${dirPart}
}

function joinDirAndFileParts() {
    local dirPart
    local filePart
    dirPart=${1}
    filePart=${2}

    if [ "${filePart}" == "" ]; then
        echo "${dirPart}"
    elif [ "${dirPart}" == "/" ]; then
        echo "/${filePart}"
    else
        echo "${dirPart}/${filePart}"
    fi
}

function normalizePath() {
    local dir
    local dirPart
    local filePart
    dir=${1}

    filePart=$(getFilePart ${dir})
    dirPart=$(getDirPart ${dir})
    
    if [ "${filePart}" == ".." ]; then
        echo $(getDirPart ${dirPart})
        return
    fi

    joinDirAndFileParts ${dirPart} ${filePart}
}

function shouldRebuild() {
    local root=${1}
    
    if [ ! -e "${root}/index.html" ]; then
        return 2
    fi

    for srcFile in `find ${src} -type f -not \( -path "${src}/.idea/*" -or -path "${src}/node_modules/*" -or -path "${src}/testoutput/*" \)`; do
        if [ "${srcFile}" -nt "${root}/index.html" ]; then
            echo "${srcFile} is newer than ${root}/index.html, rebuilding..."
            return 1
        fi
    done

    return 0
}

########################################

source ${PWD}/o3.config || exit $?

mode=$1
src=$(normalizePath $2)
dst_root=$(normalizePath $3)
dst_rel=$4
flavor=$5
imageTagQualifier=$6
sslDir=${PWD}/selfSignedCert

#
# If this is a production build, upgrade from test-* to prod-*
#
case "${flavor}" in
    test-aot)
        if [ ! -z "${OPTIO3_BUILD_PROD}" ]; then
            flavor=prod-aot
        fi
        ;;

    test)
        if [ ! -z "${OPTIO3_BUILD_PROD}" ]; then
            flavor=prod
        fi
        ;;
esac


dst=${dst_root}${dst_rel}

IMAGE_TAG=${NODE_IMAGE_TAG}${imageTagQualifier}-${flavor}

DIST_ROOTS="${dst}/dist ${dst}/dist/en-US ${dst}/dist/it"

####

if [ "${mode}" == "build-cdn" ]; then
    if [ ! -z "${OPTIO3_BUILD_DEPLOYURL}" ]; then
        deployURL=${OPTIO3_BUILD_DEPLOYURL}${dst_rel}/
    fi

    mode=build
fi

if [ "${mode}" == "build" ]; then
    for root in ${DIST_ROOTS}; do
        shouldRebuild ${root} && {
            echo No need to rebuild web site assets...
            exit 0
        }
    done

    rm -rf ${dst}
fi

####

cd ${src}

grep -r 'from "app/' src/framework && {
    echo
    echo "Failed due to references from Framework to App!!"
    echo
    exit 10
}

################################################################################

remapped_dst=$(${scriptDir}/Optio3DockerRemapper.sh ${dst})
bindVolumes="-v ${remapped_dst}:/usr/src/app/output"

function mapSourceCode() {
    bindVolumes="${bindVolumes} -v ${src}/src:/usr/src/app/src-shadow"
}

function mapSSL() {
    bindVolumes="${bindVolumes} -v ${sslDir}:/usr/src/app/ssl"
}

################################################################################

dockerOpt=
timestamp=$(date "+%Y%m%d_%H%M%S")
targetCommand="${mode}:${flavor}"

case "${mode}" in
    build)
        if [ ! -z "${deployURL}" ]; then
            extraNpmOpt="--deploy-url ${deployURL}"
        fi
        ;;

    analyze)
        analysisDir=`mktemp -d $TMPDIR/workdir.XXXXXX`
        bindVolumes="-v ${analysisDir}:/usr/src/app/output"
        extraNpmOpt="--stats-json"
        targetCommand="build:${flavor}"
        ;;

    viewAnalysis)
        cd ${src}
        npm run analyze
        exit 0
        ;;

    serve)
        bindVolumes=""
        mapSourceCode
        dockerOpt="-ti -p 4201:4201"
        extraNpmOpt="--host 0.0.0.0"
        ;;

    serveHttps)
        bindVolumes=""
        mapSourceCode
        mapSSL
        dockerOpt="-ti -p 4201:4201"
        targetCommand="serve:${flavor}"
        extraNpmOpt="--host 0.0.0.0 --ssl --sslCert=/usr/src/app/ssl/keystore.crt --sslKey=/usr/src/app/ssl/keystore.key"
        ;;
esac


# deletes the temp files
function cleanup {
    local file=${1}

    if [ ! -z "${file}" ]; then
        if [ -f "${file}" ]; then
            rm "${file}"
        fi
    fi
}

# deletes the temp directories
function cleanupDir {
    local dir=${1}
    
    if [ ! -z "${dir}" ]; then
        if [ -d "${dir}" ]; then
            rm -rf "${dir}"
        fi
    fi
}

function cleanupAll {
    cleanup ${index_dst}
    cleanup ${manifest_dst}

    cleanupDir ${analysisDir}
}

# register the cleanup function to be called on the EXIT signal

##########################################################
#
# Change %%BUILD_VERSION%% with a timestamp of the build.
#
if [ -z "${OPTIO3_BUILD_COMMIT}" ]; then
    commit=$(git rev-parse HEAD)
else
    commit=${OPTIO3_BUILD_COMMIT}
fi

function replaceParameters() {
    if [ -e "${1}" ]; then
        sed -e s/%%BUILD_TIMESTAMP%%/${timestamp}/g \
            -e s/%%BUILD_COMMIT%%/${commit}/g \
            -e s^%%BUILD_DEPLOYURL%%^${deployURL}^g \
            <"${1}" >"${2}" || exit $?
    fi
}

index_src="${src}/src/index.template.html"
index_dst="${src}/src/index.html"

manifest_src="${src}/src/manifest.template.webmanifest"
manifest_dst="${src}/src/manifest.webmanifest"

replaceParameters ${index_src} ${index_dst}
replaceParameters ${manifest_src} ${manifest_dst}

trap cleanupAll EXIT

#
##########################################################

# Build first time with output, to capture errors.
docker build --force-rm -t ${IMAGE_TAG} . || exit $?
# Rebuild, which is a no-op on success, to capture image id.
IMAGE_ID=$(docker build -q .)

# Remove unreferenced images
docker image prune -f || exit $?

#####

mkdir -p ${dst}
docker run ${dockerOpt} --rm ${bindVolumes} ${IMAGE_ID} npm run ${targetCommand} -- ${extraNpmOpt} || exit $?

case "${mode}" in
    build)
        foundOutput=0
        for root in ${DIST_ROOTS}; do

            # Delete old font formats
            rm ${root}/*.eot ${root}/*.ttf ${root}/*.svg ${root}/*.woff
            
            if [ -e "${root}/index.html" ]; then
                if [ -e "${root}/ngsw.json" ]; then
                    rm "${root}/ngsw-worker.js"
                    cp "${src}/ngsw-worker.js" "${root}/ngsw-worker.js"

                    if [ ! -z "${deployURL}" ]; then
                        cp "${root}/ngsw.json" "${root}/ngsw.json.orig"
                        sed -E '/(index.html|manifest.webmanifest)/! s*"/*"'"${deployURL}"'*' "${root}/ngsw.json.orig" > "${root}/ngsw.json" || exit $?
                    fi
                fi
                foundOutput=1
                break
            fi
        done

        if [ -z "$foundOutput" ]; then
            echo Building ${IMAGE_TAG} [${IMAGE_ID}] for ${dst} failed, no index.html got generated!
            exit 10
        fi
        ;;

    analyze)
        cp ${analysisDir}/dist/stats.json ${src}
        ;;
    
esac
