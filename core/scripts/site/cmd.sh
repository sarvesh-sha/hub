function usage() {
    local cmd=$1

    if [ ! -z "$cmd" ]; then
        echo
        echo "Unknown command '$cmd'"
        echo
    fi

    echo "Usage: o3 site [options] [commands]"
    echo
    echo "  Run Site commands for the project in the current directory"
    echo
    echo "Options:"
    echo
    echo "    --noNpm   : skip the NPM part of the build"
    echo "    --arm     : build Docker image for ARMv7"
    echo "    --arm64   : build Docker image for ARM64v8"
    echo "    --noclean : don't clean repo after building Docker image"
    echo
    echo "Commands:"
    echo
    echo "    build         : creates the Docker image for the site"
    echo "    buildNoImage  : just build the product"
    echo "    run           : runs the product inside Docker"
    echo "    runBg         : runs the product inside Docker, in the background"
    echo "    runBgNoRm     : runs the product inside Docker, in the background, but don't remove on exit"
    echo "    runRaw        : runs the product in a plain JVM"
    echo "    runDirect     : runs the product in a plain JVM, passing arguments to it"
    echo "    e2ePipeline   : runs the product tests"
    echo "    push          : pushes the Docker image to Optio3 registry"
    echo "    pushNew       : pushes the Docker image to Optio3 registry, using a unique tag"
    echo "    purge         : removes product images pulled from Optio3 registry"
    echo "    analyze       : builds the Angular side, extracting stats.json"
    echo "    viewAnalysis  : uses stats.json to display Angular build output"
    echo "    serve         : builds the Angular side for HTTP/2.0 and runs it in a Dev site at http://localhost:4201"
    echo "    serve-http    : builds the Angular side for HTTP/1.1 and runs it in a Dev site at http://localhost:4201"
    echo "    serve-noaot   : builds the Angular side for HTTP/1.1 and runs it in a Dev site at http://localhost:4201"
    echo "    selfsign      : generate self-signed certificate"
    echo "    selfsign-java : generate self-signed keystore"
    echo
    exit 10
}

isHelpRequested $1 || usage

##############################

while true; do
    
    if [ "$1" == "--noNpm" ]; then
        export OPTIO3_SKIP_NPM_BUILD=1
        shift
        continue
    fi
    
    if [ "$1" == "--arm" ]; then
        targetArmV7=1
        shift
        continue
    fi

    if [ "$1" == "--autoPlatform" ]; then
		case `machine` in
			arm64e)
				targetArm64=1
				;;

			*)
				;;
		esac
			
        shift
        continue
    fi

    if [ "$1" == "--arm64" ]; then
        targetArm64=1
        shift
        continue
    fi

    if [ "$1" == "--noclean" ]; then
        skipPostClean=1
        shift
        continue
    fi

    break
done

##############################

function getImageTag() {
    getSetting         imageTag DOCKER_IMAGE_TAG
    getOptionalSetting imageSvc DOCKER_IMAGE_SERVICE
    getOptionalSetting imageCfg DOCKER_IMAGE_CONFIG_TEMPLATE
    getOptionalSetting migrationsDb DB_NAME
    getOptionalSetting migrationsLevel DB_MIGRATIONS_LEVEL
    getOptionalSetting migrationsVersion DB_MIGRATIONS_VERSION

    if [ ! -z "${targetArmV7}" ]; then
        imageTag="${imageTag}-armv7"
        dockerFile="Dockerfile.armv7"
        extraDockerParams+=" --label Optio3_Architecture=ARMv7"
    elif [ ! -z "${targetArm64}" ]; then
        imageTag="${imageTag}-arm64"
        dockerFile="Dockerfile.arm64"
        extraDockerParams+=" --label Optio3_Architecture=ARM64v8"
    else
        dockerFile="Dockerfile"
        extraDockerParams+=" --label Optio3_Architecture=X86"
    fi

    if [ "${imageSvc}" ]; then
        extraDockerParams+=" --label Optio3_TargetService=${imageSvc}"
    fi

    if [ "${imageCfg}" ]; then
        extraDockerParams+=" --label Optio3_ConfigTemplate=$(base64 -i ${imageCfg})"
    fi

    if [ "${migrationsDb}" ]; then
        extraDockerParams+=" --label Optio3_DbName=${migrationsDb}"
    fi

    if [ "${migrationsLevel}" -a "${migrationsVersion}" ]; then
        extraDockerParams+=" --label Optio3_DbSchema=rev${migrationsLevel}.${migrationsVersion}"
    fi

    extraDockerParams+=" --label Optio3_BuildId=${DOCKER_IMAGE_SERVICE}_local_$(date +"%Y%m%d_%H%M%S")"
}

function purgeTag() {
    for img in $(docker images -q --no-trunc $1); do
        docker rmi $img
    done
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
}

##############################

cd ${TARGET_DIR}

cmd=$1
shift

case "${cmd}" in
    build)
        o3 mvn --quiet clean package -Pasyncawait || {
            rc=$?
            echo
            echo "Did you forget to run 'o3 tooling mvn-plugin'?"
            echo
            exit $rc
        }
        getImageTag
        docker build ${extraDockerParams} -t ${imageTag} -f ${dockerFile} . || exit $?
        if [ -z "${skipPostClean}" ]; then
            o3 mvn --quiet clean || exit $?
        fi
        ;;

    buildNoImage)
        o3 mvn --quiet clean package -Pasyncawait || {
            rc=$?
            echo
            echo "Did you forget to run 'o3 tooling mvn-plugin'?"
            echo
            exit $rc
        }
        ;;

    buildOnlyImage)
        getImageTag
        docker build ${extraDockerParams} -t ${imageTag} -f ${dockerFile} . || exit $?
        ;;

    push)
        getImageTag
        docker tag ${imageTag} repo.dev.optio3.io:5001/${imageTag} || exit $?
        docker push repo.dev.optio3.io:5001/${imageTag} || exit $?
        docker rmi repo.dev.optio3.io:5001/${imageTag} || exit $?
        echo
        echo Use this to pull image:
        echo
        echo "    docker pull repo.dev.optio3.io:5000/${imageTag}"
        echo
        ;;

    pushNew)
        getImageTag
        uniqueTag=local_$(date +"%Y%m%d_%H%M%S")
        docker tag ${imageTag} repo.dev.optio3.io:5001/${imageTag}:${uniqueTag} || exit $?
        docker push repo.dev.optio3.io:5001/${imageTag}:${uniqueTag} || exit $?
        docker rmi repo.dev.optio3.io:5001/${imageTag}:${uniqueTag} || exit $?
        echo
        echo Use this to pull image:
        echo
        echo "    docker pull repo.dev.optio3.io:5000/${imageTag}:${uniqueTag}"
        echo
        ;;

    purge)
        purgeTag repo.dev.optio3.io:5000/optio3-builder
        purgeTag repo.dev.optio3.io:5000/optio3-builder-snapshot
        purgeTag repo.dev.optio3.io:5000/optio3-deployer
        purgeTag repo.dev.optio3.io:5000/optio3-deployer-snapshot
        purgeTag repo.dev.optio3.io:5000/optio3-hub
        purgeTag repo.dev.optio3.io:5000/optio3-hub-snapshot
        purgeTag repo.dev.optio3.io:5000/optio3-gateway
        purgeTag repo.dev.optio3.io:5000/optio3-gateway-snapshot
        ;;

    run)
        getImageTag
        getSetting containerName DOCKER_CONTAINER_NAME
        getSetting ymlFile DROPWIZARD_FILE
        getOptionalSetting extraOpts DOCKER_EXTRA_OPTS
        getOptionalSettingWithDefault cmd DROPWIZARD_CMD server
        docker run -ti --rm ${extraOpts} --name ${containerName} ${imageTag} ${cmd} ${ymlFile} || exit $?
        ;;

    runBg)
        getImageTag
        getSetting containerName DOCKER_CONTAINER_NAME
        getSetting ymlFile DROPWIZARD_FILE
        getOptionalSetting extraOpts DOCKER_EXTRA_OPTS
        getOptionalSettingWithDefault cmd DROPWIZARD_CMD server
        docker run -d --rm ${extraOpts} --name ${containerName} ${imageTag} ${cmd} ${ymlFile}
        ;;

    runBgNoRm)
        getImageTag
        getSetting containerName DOCKER_CONTAINER_NAME
        getSetting ymlFile DROPWIZARD_FILE
        getOptionalSetting extraOpts DOCKER_EXTRA_OPTS
        getOptionalSettingWithDefault cmd DROPWIZARD_CMD server
        docker run -d ${extraOpts} --name ${containerName} ${imageTag} ${cmd} ${ymlFile}
        ;;

    runRaw)
        getSetting class MAIN_CLASS
        getSetting ymlFile DROPWIZARD_FILE
        getOptionalSettingWithDefault cmd DROPWIZARD_CMD server
        java -Dfile.encoding=UTF-8 -cp 'target/*:target/internalDependency/*:target/externalDependency/*' ${class} ${cmd} ${ymlFile}
        ;;
    
    runDirect)
        shift
        getSetting class MAIN_CLASS
        getOptionalSettingWithDefault maxMem MAX_HEAP 1000
        java -Xmx${maxMem}m -Dfile.encoding=UTF-8 -cp 'target/*:target/internalDependency/*:target/externalDependency/*' ${class} $*
        ;;

    serve)
        # Make sure we have a valid certificate
        o3 site selfsign || exit $?

        getSetting nodeSrc NODE_SRC
        getSetting nodeDst NODE_DST

        ${O3_ROOT_DIR}/support/WebSiteBuilder.sh serveHttps ${nodeSrc} ${nodeDst} "" localhost-h2 ${imageTagQualifier} || exit $?
        ;;

    serve-http)
        getSetting nodeSrc NODE_SRC
        getSetting nodeDst NODE_DST

        ${O3_ROOT_DIR}/support/WebSiteBuilder.sh serve ${nodeSrc} ${nodeDst} "" localhost ${imageTagQualifier} || exit $?
        ;;

    serve-noaot)
        getSetting nodeSrc NODE_SRC
        getSetting nodeDst NODE_DST

        ${O3_ROOT_DIR}/support/WebSiteBuilder.sh serve ${nodeSrc} ${nodeDst} "" localhost-noaot ${imageTagQualifier} || exit $?
        ;;

    analyze)
        getSetting nodeSrc NODE_SRC
        getSetting nodeDst NODE_DST

        mode=$1
        if [ -z "${mode}" ]; then
            mode=test-aot
        fi

        ${O3_ROOT_DIR}/support/WebSiteBuilder.sh analyze ${nodeSrc} ${nodeDst} "" ${mode} ${imageTagQualifier} || exit $?
        ;;

    viewAnalysis)
        getSetting nodeSrc NODE_SRC
        getSetting nodeDst NODE_DST

        mode=$1
        if [ -z "${mode}" ]; then
            mode=test-aot
        fi

        ${O3_ROOT_DIR}/support/WebSiteBuilder.sh viewAnalysis ${nodeSrc} ${nodeDst} "" ${mode} ${imageTagQualifier} || exit $?
        ;;

    e2ePipeline)
        checkForDependencies

        ${O3_ROOT_DIR}/product/hub/E2EPipeline.sh optio3-tester:latest optio3-hub-snapshot:latest ${O3_ROOT_DIR}/product/hub/testOutput || exit $?
        ;;

    e2e)
        host=${2}
        if [ -z "${host}" ]; then
            LOCALIP=$(ifconfig | grep "inet " | grep -Fv 127.0.0.1 | head -n1 | awk '{print $2}')
            host=http://${LOCALIP}:8080
        fi

        if [ "${host}" == "demo" ]; then
            host=http://demo.dev.optio3.io
            noRun=1
        fi

        if [ "${host}" == "nightly" ]; then
            host=http://demo-nightly.dev.optio3.io
            noRun=1
        fi

        if [ -z "${noRun}" ]; then
            checkForDependencies

            cd ${O3_ROOT_DIR}/product/hub

            echo Building site...
            o3 site build || exit $?

            echo Starting site...
            containerId=$(o3 site runBgNoRm)

            o3 mvn --quiet clean || exit $?

            echo Waiting for site to start...
            waitForSite ${host}
        fi

        getSetting nodeSrc NODE_SRC
        ${O3_ROOT_DIR}/support/E2EBuilder.sh ${O3_ROOT_DIR}/web/tester ${host} || exit $?
        ;;

    selfsign-java)
        if [ -f selfSignedCert/keystore.jks ]; then
            rm selfSignedCert/keystore.jks
        fi
        
        keytool -genkey -keyalg RSA -alias selfsigned -keystore selfSignedCert/keystore.jks -storepass selfsigned -validity 365 -keysize 2048 -dname "CN=localhost, OU=Optio3, O=Optio3, L=Seattle, ST=WA, C=US"
        ;;

    selfsign)
        if [ -f selfSignedCert/keystore.crt -a -f selfSignedCert/keystore.p12 ]; then
            # Certificate still valid? Exit
            sudo security verify-cert -c selfSignedCert/keystore.crt >/dev/null && exit 0

            sudo security remove-trusted-cert -d selfSignedCert/keystore.crt || (
                echo "Removal of previous certificate from keychain failed with $?"
            )
            rm -rf selfSignedCert
        fi

        if [ ! -d selfSignedCert ]; then
            mkdir selfSignedCert
        fi

        openssl req -x509 -out selfSignedCert/keystore.crt -keyout selfSignedCert/keystore.key -newkey rsa:2048 -nodes -sha256 -subj '/CN=localhost' -extensions EXT -config <(cat <<EOF
[dn]
CN=localhost
[req]
distinguished_name = dn
[EXT]
subjectAltName=DNS:localhost
keyUsage=digitalSignature
extendedKeyUsage=serverAuth
EOF
        ) || (
            echo "Certificate creation failed with $?"
            exit 10
        )

        if [ -f selfSignedCert/keystore.p12 ]; then
            rm selfSignedCert/keystore.p12
        fi

        openssl pkcs12 -export -in selfSignedCert/keystore.crt -inkey selfSignedCert/keystore.key -out selfSignedCert/keystore.p12 -passout pass:selfsigned -name "Local Cert" || (
            echo "Certificate export failed with $?"
            exit 10
        )

        sudo security add-trusted-cert -d -k "/Library/Keychains/System.keychain" selfSignedCert/keystore.crt || (
            echo "Installation of certificate in keychain failed with $?"
            exit 10
        )

        echo
        echo "Self-signed certificate created!"
        echo
        echo "When using Firefox, set security.enterprise_roots.enabled = true in about:config and restart app"
        echo
        ;;

    *)
        usage ${cmd}
        exit 10
        ;;
esac
