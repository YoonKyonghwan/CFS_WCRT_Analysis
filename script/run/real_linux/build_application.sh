#!/bin/bash

cd real_linux_application/app

if [ ! -d build ]; then
    mkdir build
fi
cd build
cmake ..
make -j3

cd ../../..
echo "<current directory>"
pwd