function usage() {
    local cmd=$1

    if [ ! -z "$cmd" ]; then
        echo
        echo "Unknown command '$cmd'"
        echo
    fi

    echo "Usage: o3 db [commands]"
    echo
    echo "  Run DB commands for the project in the current directory"
    echo
    echo "Options:"
    echo
    echo "    --port <port>    : port number to use for server (default 13306)"
    echo "    --server-id <id> : DB server id (default 1)"
    echo
    echo "Commands:"
    echo
    echo "    schema                  : updates the Liquibase DB schema"
    echo "    metamodel               : refreshes the Hibernate MetaModels"
    echo "    loadBackup <backupFile> : launch MariaDB from a backup archive"
    echo "    shell                   : open shell in MariaDB container"
    echo
    exit 10
}

isHelpRequested $1 || usage

override_port=
server_id=1
db_cmd=run

while true; do
    
    if [ "$1" == "--port" ]; then
        shift
        override_port=${1}
        shift
        continue
    fi

    if [ "$1" == "--server-id" ]; then
        shift
        server_id=${1}
        shift
        continue
    fi

    if [ "$1" == "--primary" ]; then
        shift
        db_cmd="run_replica ${1}"
        shift
        continue
    fi

    if [ "$1" == "--" ]; then
        shift
        continue
    fi

    break
done

db_container="restore-mariadb${override_port}"
db_volume_data="restore-data${override_port}"
db_volume_config="restore-config${override_port}"

##############################

newTempFile TMP_FILE

function runRefresh() {
    args=$*
    getSetting class MAIN_CLASS
    getSetting ymlFile DROPWIZARD_FILE
    java -Dfile.encoding=UTF-8 -cp 'target/*:target/internalDependency/*:target/externalDependency/*' ${class} refreshDb $args ${ymlFile} 1>"${TMP_FILE}" 2>&1 || {
        rc=$?
        cat "${TMP_FILE}"
        echo
        echo Command refreshDb $args failed with exit code $rc
        exit $rc
    }
}

function cleanupBackup {
    echo "Shutting down MariaDB Backup..."
    docker rm -f -v     ${db_container}     >/dev/null
    docker volume rm -f ${db_volume_data}   >/dev/null
    docker volume rm -f ${db_volume_config} >/dev/null
    echo "Done"
}

##############################

cd ${TARGET_DIR}

case "$1" in
    schema)
        o3 mvn --quiet --noNpm clean package -Pasyncawait || {
            rc=$?
            echo
            echo "Did you forget to run 'o3 tooling mvn-plugin'?"
            echo
            exit $rc
        }
        getSetting migrationsId DB_MIGRATIONS_ID
        getSetting migrationsLevel DB_MIGRATIONS_LEVEL
        getSetting migrationsVersion DB_MIGRATIONS_VERSION

        migrations="--migrationsId ${migrationsId} --migrationsRevisionLevel ${migrationsLevel} --migrationsVersionNumber ${migrationsVersion}"

        echo Refreshing schema for h2_v${migrationsVersion}.xml
        runRefresh --db h2    ${migrations} --targetRoot src/main/resources || exit $?
        echo Refreshing schema for mysql_v${migrationsVersion}.xml
        runRefresh --db mysql ${migrations} --targetRoot src/main/resources || exit $?

        o3 mvn --quiet clean || exit $?
        ;;

    metamodel)
        getSetting profile METAMODEL_PROFILE

        rm -rf target/classes target/generated-sources

        src=target/generated-sources/annotations
        dst=src/main/java

        oldFiles=$(cd ${dst}; find . -name \*_.java)
        for i in ${oldFiles}; do
            rm ${dst}/$i
        done

        o3 mvn --noNpm -- --batch-mode -P${profile} compile 1>/dev/null 2>&1 # The build will fail, just ignore any errors

        newFiles=$(cd ${src}; find . -name \*_.java)
        for i in ${newFiles}; do

            cat <<EOF >${dst}/$i
/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
EOF

            cat ${src}/$i >>${dst}/$i
            rm ${src}/$i

        done

        rm -rf target/classes target/generated-sources
        ;;

    loadBackup)
        backupFile=`normalizePath ${2}`

        if [ -z "${backupFile}" ]; then
            echo "No backup specified!"
            exit 10
        fi

        if [ ! -f "${backupFile}" ]; then
            echo "Backup ${backupFile} not found!"
            exit 10
        fi

        trap cleanupBackup EXIT

        docker volume rm ${db_volume_data}    >/dev/null 2>/dev/null
        docker volume rm ${db_volume_config} >/dev/null 2>/dev/null

        docker volume create ${db_volume_data}   >/dev/null || exit $?
        docker volume create ${db_volume_config} >/dev/null || exit $?

        docker run --rm -i -v ${db_volume_config}:/optio3-config busybox sh -c 'cat >/optio3-config/setup.sh' <<EOF
MYSQL_ROOT_PASSWORD=test
MYSQL_REPL_PASSWORD=replicate
MYSQL_DATABASE=hub_db
OPTIO3_DB_HOST=dbserver-${server_id}
OPTIO3_SERVER_ID=${server_id}
EOF

        echo Reloading backup...
        startTime=`date +%s`
        docker run --rm -i -v ${db_volume_data}:/optio3-data -v ${backupFile}:/optio3-backup.tar.gz busybox sh -c "mkdir -p /optio3-data/mysql && cd /optio3-data/mysql && tar xfz /optio3-backup.tar.gz" || exit $?
        endTime=`date +%s`
        diffTime=$(( ${endTime} - ${startTime} ))
        echo Reloaded backup in ${diffTime} seconds at `date`

        echo Starting database...
        docker network create restore-db 2>/dev/null || true

        docker run -d --name ${db_container} -v ${db_volume_config}:/optio3-config -v ${db_volume_data}:/optio3-data -p ${override_port:-13306}:3306 --network restore-db --network-alias dbserver-${server_id} repo.dev.optio3.io:5000/optio3-mariadb:10.3.27_revH $db_cmd >/dev/null || exit $?
        echo "Started MariaDB on port ${override_port:-13306}"
        read -p "Press any key to stop..."
        echo
        ;;

    shell)
        docker exec -ti ${db_container} bash -i -l -c "cd /optio3-data/mysql; bash -i"
        ;;

    *)
        usage $1
        exit 10
        ;;
esac
