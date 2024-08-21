#!/bin/bash

cd real_linux_application/app

rm -rf build
mkdir build
cd build
cmake ..
make -j3

cd ../../..
echo "<current directory>"
pwd