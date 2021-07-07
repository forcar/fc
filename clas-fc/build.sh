#!/bin/csh -f
#=================================================================
# BUILDING SCRIPT for CLAS-FC PROJECT (first maven build)
# then the documentatoin is build from the sources and commited
# to the documents page
#=================================================================
# Maven Build

if(`filetest -e lib` == '0') then
    mkdir lib
endif

# clas12ana
echo "Building clas-fc..."
    mvn clean install
    cp target/clas-fc-3.0-jar-with-dependencies.jar lib/
    cd ..


# Finishing touches
echo ""
echo "--> Done building....."
echo ""
echo "    Usage : build.sh"
echo ""
