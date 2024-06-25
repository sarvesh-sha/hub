function usage() {
    local cmd=$1

    if [ ! -z "$cmd" ]; then
        echo
        echo "Unknown command '$cmd'"
        echo
    fi

    echo "Usage: o3 decoder [options] [commands]"
    echo
    echo "  Run Decoder commands"
    echo
    echo "Options:"
    echo
    echo "    --forceBuild : force rebuilding Decoder application"
    echo
    echo "Commands:"
    echo
    echo "    metadata      : decodes record metadata"
    echo "    stagedResults : decodes staged gateway results"
    echo "    timeSeries    : decodes time series"
    echo
    exit 10
}

isHelpRequested $1 || usage

##############################

while true; do

    if [ "$1" == "--forceBuild" ]; then
        forceBuild=1
        shift
        continue
    fi

    break
done

##############################

function buildDecoder() {
    pushd ${O3_ROOT_DIR}/cli/decoder-cli
    o3 mvn --quiet --noNpm package || exit $?
    popd
}

function runDecoder() {
    local buildDir
    local rc
    buildDir=${O3_ROOT_DIR}/cli/decoder-cli/target
    
    java -Dfile.encoding=UTF-8 -cp ${buildDir}/'*':${buildDir}/internalDependency/'*':${buildDir}/externalDependency/'*' com.optio3.decoder.CommandLine $* || {
        exitCode=$?
        return ${exitCode}
    }

    return 0
}

##############################

if [ -z "${forceBuild}" ]; then

    if [ -f "${O3_ROOT_DIR}/cli/decoder-cli/optio3-decoder-cli-1.0.0-SNAPSHOT.jar" ]; then
        forceBuild=1
    fi

fi

if [ ! -z "${forceBuild}" ]; then
    buildDecoder || exit $?
fi

case "$1" in
    metadata)
        file=$(normalizePath ${2})

        runDecoder --asMetadata --input ${file} || exit $?
        ;;

    stagedResults)
        file=$(normalizePath ${2})

        runDecoder --asStagedResult --input ${file} || exit $?
        ;;

    timeSeries)
        file=$(normalizePath ${2})

        runDecoder --asTimeSeries --input ${file} || exit $?
        ;;

    *)
        usage $1
        exit 10
        ;;
esac
