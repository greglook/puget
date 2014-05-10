#!/bin/bash

COVERALLS_URL='https://coveralls.io/api/v1/jobs'
CLOVERAGE_VERSION='1.0.4' lein2 cloverage --coveralls
curl -F 'json_file=@target/coverage/coveralls.json' "$COVERALLS_URL"
