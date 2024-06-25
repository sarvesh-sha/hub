function sigHUP()
{
    echo Got sigHUP
    kill -HUP %1
    recheck=true
}

function sigINT()
{
    echo Got sigINT
    kill -TERM %1
    recheck=true
}

function sigQUIT()
{
    echo Got sigQUIT
    kill -QUIT %1
    recheck=true
}

function sigTERM()
{
    echo Got sigTERM
    kill -TERM %1
    recheck=true
}

function waitForTermination()
{
    while [ true ]; do
        wait %1
        exitCode=$?
        if [ ! -z $recheck ]; then
            unset recheck
            continue;
        fi
        
        exit $exitCode
    done
}

trap sigHUP  HUP
trap sigINT  INT
trap sigQUIT QUIT
trap sigTERM TERM

######

JAVA_OPTS=""

if [ ! -z "${OPTIO3_MAX_MEM}" ]; then
   JAVA_OPTS="${JAVA_OPTS} -Xmx${OPTIO3_MAX_MEM}m"
fi

java ${JAVA_OPTS} -XX:-HeapDumpOnOutOfMemoryError -cp "*" com.optio3.cloud.waypoint.WaypointApplication ${*} &

waitForTermination
