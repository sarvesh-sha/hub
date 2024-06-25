#!/bin/bash

scriptDir=`dirname $0`

TESTER_IMAGE=$1
HUB_IMAGE=$2
OUTPUT=$3

function usage() {
    if [ -z $TESTER_IMAGE ] || [ -z $HUB_IMAGE ] || [ -z $OUTPUT ]; then
        echo
        echo "Usage: o3 site e2ePipeline [testerImage] [hubImage] [outputDirectory]"
        echo
        echo "  Run the E2E test pipeline. Creates tester container, hub container, and outputs test results to the given directory"
        echo
        exit 10
    fi
}

function checkImageExists {
    local image=$1

    if [ -z $(docker images -q $image) ]; then
        echo "Image $image does not exist"
        exit 1
    fi
}

function checkForDependencies() {
    which jq &>/dev/null || {
        echo
        echo "Did you forget to run 'brew install jq' ?"
        echo
        exit 10
    }
}

function dockerCompose {
    env TESTER_IMAGE=$TESTER_IMAGE HUB_IMAGE=$HUB_IMAGE OUTPUT=$OUTPUT docker compose -f $scriptDir/docker-compose-test.yml $*
}

function cleanup {
    dockerCompose down --remove-orphans
}

function waitForSite {
    local url=$1
    local timeout=60

    while [ true ]; do
        if [ "${timeout}" -lt 0 ]; then
            echo "Service did not start within timeout"
            dockerCompose logs
            exit 1
        fi
        sleep 1
        curl -I ${url} 2>/dev/null >/dev/null && break
        let timeout="${timeout} - 1"
    done
}

function getTestCases {
    tests=`curl -s -X 'POST' \
      'http://localhost:3100/api/v1/tests/' \
      -H 'accept: application/json' \
      -H 'Content-Type: application/json' \
      -d '{
      "url": "http://hub:8080"
    }'`

    echo $tests | jq -c '. | length'
}

function startTests {
    curl -X 'POST' \
      'http://localhost:3100/api/v1/tests/run-all' \
      -H 'accept: application/json' \
      -H 'Content-Type: application/json' \
      -d '{
      "url": "http://hub:8080"
    }' 2>/dev/null >/dev/null
}

function waitForData {
    local url=$1
    local timeout=180

    while [ true ]; do
        if [ "${timeout}" -lt 0 ]; then
            echo "Tests did not start within timeout"
            dockerCompose logs
            exit 1
        fi

        sleep 1
        testResults=$(curl -s -X 'GET' 'http://localhost:3100/api/v1/tests/progress' -H 'accept: application/json')
        length=$(echo $testResults | jq '. | length')
        if [ "${length}" -gt 0 ]; then
            break
        fi

        let timeout="${timeout} - 1"
    done
}

function waitForTests {
    local url=$1
    local timeout=60000
    local currentId=''


    while [ true ]; do
        if [ "${timeout}" -lt 0 ]; then
            echo "Tests did not finish within timeout"
            dockerCompose logs
            exit 1
        fi
        sleep 1
        testResults=$(curl -s -X 'GET' 'http://localhost:3100/api/v1/tests/progress' -H 'accept: application/json' | jq -c .)
        numDone=$(echo $testResults | jq -c -r 'map(select(.testEnd)) | length')
        lastTest=$(getTest "${testResults}" -1)
        lastId=$(getTestId "$lastTest")

        if [ -z $currentId ]; then
            currentId=$lastId
        fi

        if [ $lastId != $currentId ]; then
            currentId=$lastId
            previousTest=$(getTest "${testResults}" -2)
            logTest "$previousTest"
        fi

        if [ $numDone -eq $numTests ]; then
            logTest "$lastTest"
            break
        fi

        let timeout="${timeout} - 1"
    done
}

function getTest {
    local testResults="$1"
    local index=$2

    echo $testResults | jq -c --argjson index $index '.[$index]'
}

function getTestId {
    local test="$1"
    echo $test | jq -c '.id'
}

function logTest {
    local testCase="$1"

    name=$(echo $testCase | jq -r '.name, if .testEnd then (if .failure == "" then "-- Passed" else "-- Failed " + .failure end) else "-- Running" end')
    echo $name
}

function deleteLastLine {
    echo -e '\r'
}

usage

trap cleanup EXIT

checkImageExists $HUB_IMAGE
checkImageExists $TESTER_IMAGE
checkForDependencies

cleanup

echo
echo "E2E Pipeline Starting"
echo
echo "Output will be at $OUTPUT"
echo

dockerCompose up -d || exit $?

echo
echo "Waiting for services to start."
waitForSite http://localhost:8080

echo "Service started successfully"

numTests=$(getTestCases)
echo
echo "$numTests tests detected"
echo

echo "Starting tests"
startTests

echo "Generating Data"
waitForData
echo "Data generated successfully"

echo "Test Run Starting"
waitForTests
echo "Test Run Complete"