#!/usr/bin/env bash

set -e
set -x

BUILD_VERSION="v0.2"

# Print the extracted version
echo "Application Version: $BUILD_VERSION"

export DOCKER_DEFAULT_PLATFORM=linux/amd64
IMAGE_NAME="suvera/keycloak-scim2-storage"

docker build -t $IMAGE_NAME:$BUILD_VERSION .

docker image push $IMAGE_NAME:$BUILD_VERSION

echo "Successfully built and pushed the Docker image."
