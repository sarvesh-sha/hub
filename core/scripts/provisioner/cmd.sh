function usage() {
    local cmd=$1

    if [ ! -z "$cmd" ]; then
        echo
        echo "Unknown command '$cmd'"
        echo
    fi

    echo "Usage: o3 provisioner [options] [commands]"
    echo
    echo "  Run Provisioning commands"
    echo
    echo "Options:"
    echo
    echo "    --build          : also build the code for Provisioner"
    echo
    echo "Commands:"
    echo
    echo "    [provisioner arguments] : passes arguments to O3 Provisioner command"
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
    pushd ${O3_ROOT_DIR}/cli/provisioner
    echo Building Provisioner cli...
    o3 mvn --quiet --noNpm package || exit $?
    echo Done building Provisioner cli.
	popd
}

function run() {
    local buildDir
    buildDir=${O3_ROOT_DIR}/cli/provisioner/target

	if [ ! -f "${buildDir}/optio3-cli-provisioner-1.0.0-SNAPSHOT.jar" ]; then
		build
	fi

    quotedArgs=
    while [ $# -gt 0 ]; do quotedArgs="$quotedArgs \"$1\""; shift; done
	LIBUSB_FOR_M1=~/.m2/repository/io/github/dsheirer/libusb4java-darwin-aarch64/1.3.1/libusb4java-darwin-aarch64-1.3.1.jar

    echo java -Dfile.encoding=UTF-8 -cp ${LIBUSB_FOR_M1}:${buildDir}/'*':${buildDir}/internalDependency/'*':${buildDir}/externalDependency/'*' com.optio3.infra.cli.Provisioner $quotedArgs
    eval java -Dfile.encoding=UTF-8 -cp ${LIBUSB_FOR_M1}:${buildDir}/'*':${buildDir}/internalDependency/'*':${buildDir}/externalDependency/'*' com.optio3.infra.cli.Provisioner $quotedArgs || {
        rc=$?
        echo
        echo "Did you forget to run 'o3 provisioner --build'?"
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
eval run $quotedArgs 2>&1 | grep -v "WARNING:"
