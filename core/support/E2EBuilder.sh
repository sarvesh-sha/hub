
source ${PWD}/o3.config || exit $?

src=$1
SITE_URL=$2

cd ${src}

# Build first time with output, to capture errors.
docker build -f ./Dockerfile . || exit $?
# Rebuild, which is a no-op on success, to capture image id.
IMAGE_ID=$(docker build -q -f ./Dockerfile .)

################################################################################

dockerOpt="-ti -p 3100:3100 -e SITE_URL=${SITE_URL} -e HEADLESS=true"
bindVolumes="-v $(pwd)/testoutput:/optio3-test-output"

docker run ${dockerOpt} ${bindVolumes} --rm ${IMAGE_ID} npm start || exit $?
