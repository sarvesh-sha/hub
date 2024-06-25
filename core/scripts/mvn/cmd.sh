function usage() {
    local cmd=$1

    if [ ! -z "$cmd" ]; then
        echo "Unknown command '$cmd'"
        echo
    fi

    echo "Usage: o3 mvn [options] [commands]"
    echo
    echo "  Run Maven commands for the project in the current directory and its dependencies"
    echo
    echo "Options:"
    echo
    echo "    --test          : run tests (default false)"
    echo "    --all           : build all the projects (default false)"
    echo "    --quiet         : only show the output when something goes wrong"
    echo "    --noNpm         : skip the NPM part of the build"
    echo "    --depTree       : compute the JAR dependency tree for this project"
    echo "    --analyzeDep    : compute type-level dependencies"
    echo "    --analyzeAll    : include internal projects in the dependency analysis"
    echo "    --analyzeVerbose: report detailed per-class dependency analysis"
    echo "    --              : end of command options, passing the remaining ones to Maven"
    echo
    echo "Commands:"
    echo
    echo "    [mvn arguments] : passes arguments to Maven"
    echo
    exit 10
}

isHelpRequested $1 || usage

##############################

project=pom.xml

testOverride=-Dmaven.test.skip=true
quiet=
buildAll=

while true; do
    
    if [ "$1" == "--test" ]; then
        testOverride=
        shift
        continue
    fi

    if [ "$1" == "--quiet" ]; then
        quiet=1
        shift
        continue
    fi

    if [ "$1" == "--all" ]; then
        buildAll=1
        shift
        continue
    fi

    if [ "$1" == "--noNpm" ]; then
        export OPTIO3_SKIP_NPM_BUILD=1
        shift
        continue
    fi

    if [ "$1" == "--depTree" ]; then
        quiet=2
        newTempFile DEP_TREE
        shift
        continue
    fi
    
    if [ "$1" == "--analyzeDep" ]; then
        DEP_ANALISYS=true
        shift
        continue
    fi
    
    if [ "$1" == "--analyzeAll" ]; then
        DEP_ANALISYS_ALL=true
        shift
        continue
    fi


    if [ "$1" == "--analyzeVerbose" ]; then
        DEP_ANALISYS_VERBOSE=true
        shift
        continue
    fi

    if [ "$1" == "--" ]; then
        shift
        continue
    fi

    break
done

if [ ! -z "$DEP_TREE" ]; then
    buildAll=
    mavenArguments="dependency:tree -DoutputType=text -DoutputFile=${DEP_TREE} -Dscope=compile"
elif [ ! -z "$DEP_ANALISYS" ]; then
    buildAll=
    mavenArguments="process-classes -Panalyze-dependencies"

    if [ -z "$DEP_ANALISYS_ALL" ]; then
        mavenArguments="${mavenArguments} -Doptio3.analyze.onlyjars=true"
    fi

    if [ ! -z "$DEP_ANALISYS_VERBOSE" ]; then
        mavenArguments="${mavenArguments} -Doptio3.analyze.verbose=true"
    fi

else
    mavenArguments=$*
fi

if [ -z "$mavenArguments" ]; then
    usage
fi

dir=${TARGET_DIR}

while true; do
    parentdir=`dirname ${dir}`

    if [ ! -f "${parentdir}/pom.xml" ]; then
        break
    fi

    project=`basename ${dir}`/${project}
    dir=${parentdir}

done

if [ -z "$buildAll" ]; then

    cd ${dir}

    mavenArguments="--also-make --projects ${project} ${mavenArguments}"

else

    cd ${TARGET_DIR}
    
fi

if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=`/usr/libexec/java_home`
fi

if [ ! -z "$quiet" ]; then
    newTempFile TMP_FILE

    commandLine="mvn ${testOverride} ${mavenArguments}"
    if [ "$quiet" == "1" ]; then
        echo Executing ${commandLine} ...
    fi

    ${commandLine} 1>"${TMP_FILE}" 2>&1 || {
        rc=$?
        cat "${TMP_FILE}"
        echo
        echo Build failed with exit code $rc
        exit $rc
    }

    if [ "$quiet" == "1" ]; then
        echo Done
    fi

    if [ ! -z "$DEP_TREE" ]; then
        cat ${DEP_TREE}
    fi

else

    mvn ${testOverride} ${mavenArguments} || exit $?

fi
