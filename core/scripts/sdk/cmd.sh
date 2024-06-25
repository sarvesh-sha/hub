function usage() {
    local cmd=$1

    if [ ! -z "$cmd" ]; then
        echo
        echo "Unknown command '$cmd'"
        echo
    fi

    echo "Usage: o3 sdk [options] [commands]"
    echo
    echo "  Run SDK commands"
    echo
    echo "Options:"
    echo
    echo "    --noRun : don't start the target application"
    echo
    echo "Commands:"
    echo
    echo "    hub         : generates the SDK for the Hub"
    echo "    builder     : generates the SDK for the Builder"
    echo "    waypoint    : generates the SDK for the Waypoint"
    echo "    provisioner : generates the SDK for the Provisioner"
    echo "    docker      : generates the SDK for Docker"
    echo "    registry    : generates the SDK for Docker Registry"
    echo "    godaddy     : generates the SDK for GoDaddy"
    echo "    pelion      : generates the SDK for Pelion"
    echo "    reporter    : generates the SDK for Reporter"
    echo "    tester      : generates the SDK for Tester"
    echo
    exit 10
}

isHelpRequested $1 || usage

##############################

while true; do
    
    if [ "$1" == "--noRun" ]; then
        noRun=1
        shift
        continue
    fi

    break
done

##############################

function buildCodeGen() {
    cd ${O3_ROOT_DIR}/cli/sdk-cli
    echo Building Codegen cli...
    o3 mvn --quiet --noNpm package || exit $?
    echo Done building Codegen cli.
}

function runCodeGen() {
    local buildDir
    local rc
    buildDir=${O3_ROOT_DIR}/cli/sdk-cli/target
    
    java -Dfile.encoding=UTF-8 -cp ${buildDir}/'*':${buildDir}/internalDependency/'*':${buildDir}/externalDependency/'*' com.optio3.sdk.codegen.CommandLine $* || {
        exitCode=$?
        return ${exitCode}
    }

    return 0
}

function copyIfChanged() {
    local src=$1
    shift

    local dst=$1
    shift

    rsync --verbose --checksum --delete -a ${*} ${src}/ ${dst}
}

function checkForDependencies() {
    which curl &>/dev/null || {
        echo
        echo "curl not installed..."
        echo
        exit 10
    }

    which json_reformat &>/dev/null || {
        echo
        echo "Did you forget to run 'brew install yajl' ?"
        echo
        exit 10
    }
}

function waitForSite {
    local url=$1
    local timeout=60

    while [ true ]; do
        if [ "${timeout}" -lt 0 ]; then
            docker logs ${containerId}
            exit 1
        fi
        sleep 1
        curl -I ${url} 2>/dev/null >/dev/null && break
        let timeout="${timeout} - 1"
    done

    mkdir src/spec 2>/dev/null || true
    curl ${url} 2>/dev/null | json_reformat >src/spec/swagger.json
}
        
function cleanupDocker {
    if [ -d "$WORK_DIR" ]; then
        rm -rf "$WORK_DIR"
    fi

    if [ ! -z "${containerId}" ]; then
        if [ ! -z "${exitCode}" ]; then
            docker logs ${containerId}
        fi
        echo Terminating site...
        docker rm -f ${containerId} >/dev/null
    fi
}

trap cleanupDocker EXIT

# the temp directory used, within $DIR
WORK_DIR=`mktemp -d $TMPDIR/workdir.XXXXXX`

##############################

case "$1" in
    hub)
        host=${2}

        if [ -z "${host}" ]; then
            host=http://localhost:8080/api/v1/swagger.json
        fi

        if [ "${host}" == "demo" ]; then
            host=http://demo.dev.optio3.io/api/v1/swagger.json
            noRun=1
        fi

        if [ "${host}" == "nightly" ]; then
            host=http://demo-nightly.dev.optio3.io/api/v1/swagger.json
            noRun=1
        fi

        if [ -z "${noRun}" ]; then
            checkForDependencies

            cd ${O3_ROOT_DIR}/product/hub
            
            echo Building site...
            o3 site --autoPlatform --noNpm build || exit $?
            
            echo Starting site...
            containerId=$(o3 site --autoPlatform runBgNoRm)
            
            o3 mvn --quiet clean || exit $?

            echo Waiting for site to start...
            waitForSite ${host}
        fi
        
        buildCodeGen || exit $?

        runCodeGen --lang Typescript                                       --output ${WORK_DIR}/Typescript --spec ${host} || exit $?
        runCodeGen --lang Python                                           --output ${WORK_DIR}/Python     --spec ${host} || exit $?
        runCodeGen --lang Java       --package com.optio3.cloud.client.hub --output ${WORK_DIR}/Java       --spec ${host} || exit $?

        copyIfChanged ${WORK_DIR}/Typescript  ${O3_ROOT_DIR}/web/hub/src/app/services/proxy                                                                                                            || exit $?
        copyIfChanged ${WORK_DIR}/Typescript  ${O3_ROOT_DIR}/web/wdc/src/app/services/proxy                                                                                                            || exit $?
        copyIfChanged ${WORK_DIR}/Python      ${O3_ROOT_DIR}/client/python                  --exclude=utils.py --exclude=example.py                                                                    || exit $?
        copyIfChanged ${WORK_DIR}/Java        ${O3_ROOT_DIR}/client/hub                     --exclude=src/main/java/com/optio3/cloud/client/hub/util --exclude=optio3-client-hub.iml --exclude=pom.xml || exit $?
        ;;

    builder)
        host=${2}

        if [ -z "${host}" ]; then
            host=http://localhost:8180/api/v1/swagger.json
        fi

        if [ "${host}" == "builder" ]; then
            host=http://builder.dev.optio3.io/api/v1/swagger.json
            noRun=1
        fi

        if [ "${host}" == "nightly" ]; then
            host=http://builder-nightly.dev.optio3.io/api/v1/swagger.json
            noRun=1
        fi

        if [ -z "${noRun}" ]; then
            checkForDependencies

            cd ${O3_ROOT_DIR}/product/builder
            
            echo Building site...
            o3 site --autoPlatform --noNpm build || exit $?

            echo Starting site...
            containerId=$(o3 site --autoPlatform runBgNoRm)
            
            o3 mvn --quiet clean || exit $?

            echo Waiting for site to start...
            waitForSite ${host}
        fi
        
        buildCodeGen || exit $?

        runCodeGen --lang Typescript                                           --output ${WORK_DIR}/Typescript --spec ${host} || exit $?
        runCodeGen --lang Java       --package com.optio3.cloud.client.builder --output ${WORK_DIR}/Java       --spec ${host} || exit $?

        copyIfChanged ${WORK_DIR}/Typescript  ${O3_ROOT_DIR}/web/builder/src/app/services/proxy                                                       || exit $?
        copyIfChanged ${WORK_DIR}/Java        ${O3_ROOT_DIR}/client/builder                     --exclude=optio3-client-builder.iml --exclude=pom.xml || exit $?
        ;;

    waypoint)
        host=http://localhost:8280/api/v1/swagger.json

        if [ -z "${noRun}" ]; then
            checkForDependencies

            cd ${O3_ROOT_DIR}/product/waypoint
            
            echo Building site...
            o3 site --autoPlatform --noNpm build || exit $?

            echo Starting site...
            containerId=$(o3 site --autoPlatform runBgNoRm)
            
            o3 mvn --quiet clean || exit $?

            echo Waiting for site to start...
            waitForSite ${host}
        fi
        
        buildCodeGen || exit $?
        
        runCodeGen --lang Typescript                                            --output ${WORK_DIR}/Typescript --spec ${host} || exit $?
        runCodeGen --lang Java       --package com.optio3.cloud.client.waypoint --output ${WORK_DIR}/Java       --spec ${host} || exit $?

        copyIfChanged ${WORK_DIR}/Typescript  ${O3_ROOT_DIR}/web/waypoint/src/app/services/proxy                                                        || exit $?
        copyIfChanged ${WORK_DIR}/Java        ${O3_ROOT_DIR}/client/waypoint                     --exclude=optio3-client-waypoint.iml --exclude=pom.xml || exit $?
        ;;

    provisioner)
        host=http://localhost:8280/api/v1/swagger.json

        if [ -z "${noRun}" ]; then
            checkForDependencies

            cd ${O3_ROOT_DIR}/product/provisioner
            
            echo Building site...
            o3 site --autoPlatform --noNpm build || exit $?

            echo Starting site...
            containerId=$(o3 site --autoPlatform runBgNoRm)
            
            o3 mvn --quiet clean || exit $?

            echo Waiting for site to start...
            waitForSite ${host}
        fi
        
        buildCodeGen || exit $?
        
        runCodeGen --lang Typescript                                               --output ${WORK_DIR}/Typescript --spec ${host} || exit $?
        runCodeGen --lang Java       --package com.optio3.cloud.client.provisioner --output ${WORK_DIR}/Java       --spec ${host} || exit $?

        copyIfChanged ${WORK_DIR}/Typescript  ${O3_ROOT_DIR}/web/provisioner/src/app/services/proxy                                                           || exit $?
        copyIfChanged ${WORK_DIR}/Java        ${O3_ROOT_DIR}/client/provisioner                     --exclude=optio3-client-provisioner.iml --exclude=pom.xml || exit $?
        ;;

    docker)
        buildCodeGen || exit $?
        
        runCodeGen --lang Java --package com.optio3.infra.docker --deleteOldFiles --output ${O3_ROOT_DIR}/infra-automation/local --spec ${O3_ROOT_DIR}/infra-automation/local/support/docker-swagger.json || exit $?
        ;;

    registry)
        buildCodeGen || exit $?
        
        runCodeGen --lang Java --package com.optio3.infra.registry --deleteOldFiles --output ${O3_ROOT_DIR}/infra-automation/local --spec ${O3_ROOT_DIR}/infra-automation/local/support/docker-registry-swagger.json || exit $?
        ;;

    godaddy)
        buildCodeGen || exit $?

        runCodeGen --lang Java --package com.optio3.infra.godaddy --deleteOldFiles --output ${O3_ROOT_DIR}/infra-automation/cloud --spec ${O3_ROOT_DIR}/infra-automation/cloud/support/godaddy-swagger.json || exit $?
        ;;

    pelion)
        buildCodeGen || exit $?

        runCodeGen --lang Java --package com.optio3.infra.pelion.samples.analytics           --deleteOldFiles --output ${O3_ROOT_DIR}/infra-automation/cloud --spec ${O3_ROOT_DIR}/infra-automation/cloud/support/Pelion/analytics-swagger.json           || exit $?
        runCodeGen --lang Java --package com.optio3.infra.pelion.samples.apnlog              --deleteOldFiles --output ${O3_ROOT_DIR}/infra-automation/cloud --spec ${O3_ROOT_DIR}/infra-automation/cloud/support/Pelion/apn-log-swagger.json              || exit $?
#       runCodeGen --lang Java --package com.optio3.infra.pelion.monitoring          --deleteOldFiles --output ${O3_ROOT_DIR}/infra-automation/cloud --spec ${O3_ROOT_DIR}/infra-automation/cloud/support/Pelion/monitoring-alerts-swagger.json      || exit $?
        runCodeGen --lang Java --package com.optio3.infra.pelion.samples.provisioning        --deleteOldFiles --output ${O3_ROOT_DIR}/infra-automation/cloud --spec ${O3_ROOT_DIR}/infra-automation/cloud/support/Pelion/provisioning-swagger.json          || exit $?
        runCodeGen --lang Java --package com.optio3.infra.pelion.samples.stock.order         --deleteOldFiles --output ${O3_ROOT_DIR}/infra-automation/cloud --spec ${O3_ROOT_DIR}/infra-automation/cloud/support/Pelion/stock-order-swagger.json          || exit $?
        runCodeGen --lang Java --package com.optio3.infra.pelion.samples.subscriber.actions  --deleteOldFiles --output ${O3_ROOT_DIR}/infra-automation/cloud --spec ${O3_ROOT_DIR}/infra-automation/cloud/support/Pelion/subscriber-actions-swagger.json  || exit $?
        runCodeGen --lang Java --package com.optio3.infra.pelion.samples.subscriber.products --deleteOldFiles --output ${O3_ROOT_DIR}/infra-automation/cloud --spec ${O3_ROOT_DIR}/infra-automation/cloud/support/Pelion/subscriber-products-swagger.json || exit $?
        runCodeGen --lang Java --package com.optio3.infra.pelion.samples.subscribers         --deleteOldFiles --output ${O3_ROOT_DIR}/infra-automation/cloud --spec ${O3_ROOT_DIR}/infra-automation/cloud/support/Pelion/subscribers-swagger.json          || exit $?
#       runCodeGen --lang Java --package com.optio3.infra.pelion.tags                --deleteOldFiles --output ${O3_ROOT_DIR}/infra-automation/cloud --spec ${O3_ROOT_DIR}/infra-automation/cloud/support/Pelion/tags-swagger.json                  || exit $?
        ;;

    reporter)
        buildCodeGen || exit $?

        runCodeGen --lang Java --package com.optio3.client.reporter --deleteOldFiles --output ${O3_ROOT_DIR}/client/reporter --spec ${O3_ROOT_DIR}/web/reporter/src/spec/swagger.json || exit $?
        ;;

    tester)
        buildCodeGen || exit $?

        runCodeGen --lang Java --package com.optio3.client.tester --deleteOldFiles --output ${O3_ROOT_DIR}/client/tester                        --spec ${O3_ROOT_DIR}/web/tester/src/spec/swagger.json || exit $?
        runCodeGen --lang Typescript                                               --output ${O3_ROOT_DIR}/web/tester/ui/src/app/services/proxy --spec ${O3_ROOT_DIR}/web/tester/src/spec/swagger.json || exit $?

        ;;

    *)
        usage $1
        exit 10
        ;;
esac
