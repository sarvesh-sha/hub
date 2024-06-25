function usage() {
    local cmd=$1

    if [ ! -z "$cmd" ]; then
        echo
        echo "Unknown command '$cmd'"
        echo
    fi

    echo "Usage: o3 import [options] [commands]"
    echo
    echo "  Run Importer commands"
    echo
    echo "Options:"
    echo
    echo "    --build          : also build the code for importers"
    echo
    echo "Commands:"
    echo
    echo "    alerton <source directory> <output file> : import from Alerton systems"
    echo "    niagara <source directory> <output file> : import from Niagara systems"
    echo
    exit 10
}

isHelpRequested $1 || usage

##############################

while true; do

    if [ "$1" == "--build" ]; then
        doBuild=1
        shift
        continue
    fi

    break
done

##############################


function build() {
    echo Building Importers...
    cd ${O3_ROOT_DIR}/cli/alerton-import
    o3 mvn --quiet --noNpm package || exit $?
    cd ${O3_ROOT_DIR}/cli/niagara-import
    o3 mvn --quiet --noNpm package || exit $?
    echo Done building Importers.
}

function runAlerton() {
    local buildDir
    buildDir=${O3_ROOT_DIR}/cli/alerton-import/target

    quotedArgs=
    while [ $# -gt 0 ]; do quotedArgs="$quotedArgs \"$1\""; shift; done
    eval java -Dfile.encoding=UTF-8 -cp ${buildDir}/'*':${buildDir}/internalDependency/'*':${buildDir}/externalDependency/'*' com.optio3.product.importers.cli.AlertonImporter $quotedArgs || {
        rc=$?
        echo
        echo "Did you forget to run 'o3 import --build'?"
        echo
        exit $rc
    }
}

function runNiagara() {
    local buildDir
    buildDir=${O3_ROOT_DIR}/cli/niagara-import/target

    quotedArgs=
    while [ $# -gt 0 ]; do quotedArgs="$quotedArgs \"$1\""; shift; done
    eval java -Dfile.encoding=UTF-8 -cp ${buildDir}/'*':${buildDir}/internalDependency/'*':${buildDir}/externalDependency/'*' com.optio3.product.importers.cli.NiagaraImporter $quotedArgs || {
        rc=$?
        echo
        echo "Did you forget to run 'o3 import --build'?"
        echo
        exit $rc
    }
}

##############################

if [ ! -z "$doBuild" ]; then
    build || exit $?
fi

cd ${TARGET_DIR}

##############################

case "$1" in
    alerton)
        argRoot=${2}
        argOutput=${3}
        if [ -z "$argRoot" ]; then
            argRoot="."
            argOutput="imported_data.json"
        fi

        runAlerton import --root ${argRoot} --output ${argOutput}
        ;;

    niagara)
        argRoot=${2}
        argOutput=${3}
        if [ -z "$argRoot" ]; then
            argRoot="."
            argOutput="imported_data.json"
        fi

        runNiagara import --root ${argRoot} --output ${argOutput}
        ;;

    *)
        usage $1
        exit 10
        ;;
esac
