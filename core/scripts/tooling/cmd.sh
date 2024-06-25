function usage() {
    local cmd=$1

    if [ ! -z "$cmd" ]; then
        echo
        echo "Unknown command '$cmd'"
        echo
    fi

    echo "Usage: o3 tooling [commands]"
    echo
    echo "  Prepares some tools required by the Optio3 build"
    echo
    echo "Commands:"
    echo
    echo "    check-intellij-tools     : check if IntelliJ external tools are up-to-date"
    echo "    update-intellij-tools    : update IntelliJ external tools"
    echo "    mvn-plugin               : builds and installs the Maven plugin for async/await support"
    echo "    transform-async          : post-processes Async/Await code in the current directory (used to build from IntelliJ)"
    echo "    hibernate-enhance        : post-processes Hibernate entities in the current directory (used to build from IntelliJ)"
    echo "    hibernate-enhance-all    : post-processes Hibernate entities (used to build from IntelliJ)"
    echo "    fix-java-version         : update settings for JDK version in IntelliJ"
    echo "    jdeps [--module-summary] : compute dependencies for current project"
    echo "    data-science             : install all required data science dependencies"
    echo
    exit 10
}

isHelpRequested $1 || usage

function cleanupVersions {
    cd ${O3_ROOT_DIR}
    mvn versions:revert
}

##############################

cd ${TARGET_DIR}

case "$1" in
    check-intellij-tools)
        if [ -d ~/Library/Application\ Support/JetBrains ]; then
            cd ~/Library/Application\ Support/JetBrains
            for i in IntelliJIdea*; do
                if [ ! -d "$i" ]; then
                    continue
                fi

                if [ -f "$i/tools/External Tools.xml" ]; then
                    cmp "$i/tools/External Tools.xml" ${O3_ROOT_DIR}/support/intellij-external-tools.xml >/dev/null || {
                        echo WARNING!!!
                        echo WARNING!!! Stale IntelliJ external tools!
                        echo WARNING!!!
                        echo WARNING!!! Run \"o3 tooling update-intellij-tools\" to update.
                        echo WARNING!!!
                    }
                else
                    echo WARNING!!!
                    echo WARNING!!! Missing IntelliJ external tools!
                    echo WARNING!!!
                    echo WARNING!!! Run \"o3 tooling update-intellij-tools\" to update.
                    echo WARNING!!!
                fi
            done
        fi
        ;;

    update-intellij-tools)
        if [ -d ~/Library/Application\ Support/JetBrains ]; then
            cd ~/Library/Application\ Support/JetBrains
            for i in IntelliJIdea*; do
                if [ ! -d "$i" ]; then
                    continue
                fi

                if [ ! -d "$i/tools" ]; then
                    mkdir "$i/tools"
                fi

                cp ${O3_ROOT_DIR}/support/intellij-external-tools.xml "$i/tools/External Tools.xml"
                echo Updated \"$i/tools/External Tools.xml\"
            done
        fi
        ;;

    mvn-plugin)
        cd ${O3_ROOT_DIR}
        TOOLS_VERSION=$(mvn -q -Dexec.executable="echo" -Dexec.args='${optio3.tools.version}' --non-recursive exec:exec)
        mvn versions:set -DnewVersion=${TOOLS_VERSION} || exit $?
        trap cleanupVersions EXIT
        cd asyncawait-maven-plugin
        o3 mvn clean install || exit $?
        o3 mvn clean || exit $?
        ;;

    transform-async)
        o3 mvn --noNpm process-classes -Pasyncawait || exit $?
        ;;

    hibernate-enhance)
        #o3 mvn --noNpm hibernate-enhance:enhance || exit $?
        o3 mvn --noNpm compile || exit $?
        ;;

    hibernate-enhance-all)
        cd ${O3_ROOT_DIR}/dropwizard-shell
        o3 mvn --noNpm hibernate-enhance:enhance || exit $?
        cd ${O3_ROOT_DIR}/product/hub
        o3 mvn --noNpm hibernate-enhance:enhance || exit $?
        cd ${O3_ROOT_DIR}/product/builder
        o3 mvn --noNpm hibernate-enhance:enhance || exit $?
        ;;

    fix-java-version)
        javaVersion=$(/usr/libexec/java_home | sed 's/.*jdk1\.8\.0_\([0-9]*\).*/\1/g')
        cd ~/Library/Preferences
        files=$(grep -l -r /Library/Java/JavaVirtualMachines/jdk1.8.0_ .)
        for file in ${files}; do
            sed -i "" -e "s/1\.8\.0_\([0-9]*\)/1.8.0_${javaVersion}/g" ${file} || exit $?
        done
        ;;

    data-science)
        pip install --upgrade numpy
        pip install --upgrade pandas
        pip install --upgrade scipy
        pip install --upgrade scikit-learn
        ;;

    jdeps)
        shift
        case "$1" in
            --module-summary)
                shift
                jdeps -R --multi-release 11 -cp 'target/externalDependency/*:target/internalDependency/*' $* target/*.jar |    grep -v '^  ' | grep -v 'jar$' | grep -v 'not found' | grep -v 'JDK removed'|sed 's/.* -> //g'| sort | uniq
                ;;

            *)
                jdeps -R --multi-release 11 -cp 'target/externalDependency/*:target/internalDependency/*' $* target/*.jar
                ;;
        esac
        ;;

    *)
        usage $1
        exit 10
        ;;
esac
