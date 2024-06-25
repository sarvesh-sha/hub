function usage() {
    local cmd=$1

    if [ ! -z "$cmd" ]; then
        echo
        echo "Unknown command '$cmd'"
        echo
    fi

    echo "Usage: o3 ldap [options] [commands]"
    echo
    echo "  Run LDAP commands"
    echo
    echo "Options:"
    echo
    echo "    --build          : also build the code for LDAP"
    echo
    echo "Commands:"
    echo
    echo "    [ldap arguments] : passes arguments to O3 LDAP command"
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
    cd ${O3_ROOT_DIR}/cli/ldap
    echo Building LDAP cli...
    o3 mvn --quiet --noNpm package || exit $?
    echo Done building LDAP cli.
}

function run() {
    local buildDir
    buildDir=${O3_ROOT_DIR}/cli/ldap/target

    quotedArgs=
    while [ $# -gt 0 ]; do quotedArgs="$quotedArgs \"$1\""; shift; done
    eval java -Dfile.encoding=UTF-8 -cp ${buildDir}/'*':${buildDir}/internalDependency/'*':${buildDir}/externalDependency/'*' com.optio3.infra.cli.Ldap $quotedArgs || {
        rc=$?
        echo
        echo "Did you forget to run 'o3 ldap --build'?"
        echo
        exit $rc
    }
}

##############################

if [ ! -z "$doBuild" ]; then
    build || exit $?
fi

quotedArgs=
while [ $# -gt 0 ]; do quotedArgs="$quotedArgs \"$1\""; shift; done
eval run $quotedArgs
