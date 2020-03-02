#!/usr/bin/env bash
set -e

command=$(basename $0)

function print_help {
    echo "Usage: $command -m MMT_VERSION -v VERSION"
    echo "Build the ModernMT docker image and tags it with the specified version".
    echo
    echo "Mandatory arguments:"
    echo "  -m, --mmt           repository commit hash or branch"
    echo "  -v, --version       the release version number (i.e. \"1.0\")"
    echo
    echo "Optional arguments:"
    echo "      --no-cache      build Dockerfile without cache"
}

while [[ $# -gt 0 ]]; do
    arg_key="$1"

    case ${arg_key} in
        -m|--mmt)
        MMT_VERSION="$2"
        shift # past argument
        shift # past value
        ;;
        -v|--version)
        VERSION="$2"
        shift # past argument
        shift # past value
        ;;
        --no-cache)
        DOCKER_OPS="--no-cache"
        shift # past argument
        ;;
        -h|--help)
        print_help
        exit 0
        ;;
        *)    # unknown option
        echo "$command: invalid option '$arg_key'"
        echo "Try '$command --help' for more information"
        exit 1
        ;;
    esac
done

if [[ -z "$MMT_VERSION" || -z "$VERSION" ]]; then
    echo "$command: missing operand"
    echo "Try '$command --help' for more information."
    exit 1
fi

# -- Script run -----------------------------------------------------------------
docker build ${DOCKER_OPS} \
    --build-arg MMT_VERSION=${MMT_VERSION} \
    . -t modernmt/master:v${VERSION} -t modernmt/master:latest
