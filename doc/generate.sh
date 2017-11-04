#!/bin/bash

DOC_BRANCH=gh-pages
DOC_TARGET=target/doc

if [[ ! -d $DOC_TARGET ]]; then
    GIT_REMOTE=$(git remote -v | head -1 | awk '{ print $2; }')
    echo "Cloning $DOC_BRANCH branch from $GIT_REMOTE into $DOC_TARGET ..."
    mkdir -p $(dirname $DOC_TARGET) || exit 2
    git clone $GIT_REMOTE $DOC_TARGET || exit 2
    pushd $DOC_TARGET
    if [[ $1 == "init" ]]; then
        git checkout --orphan $DOC_BRANCH
        git symbolic-ref HEAD refs/heads/$DOC_BRANCH
        rm .git/index
        git clean -fdx
    else
        git checkout $DOC_BRANCH || exit 3
    fi
    popd
fi

echo "Generating documentation in $DOC_TARGET ..."
lein with-profile +doc do codox, marg --dir $DOC_TARGET/marginalia
