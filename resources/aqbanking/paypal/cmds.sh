#!/bin/bash

parent_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
cd "$parent_path"
source ./env.sh 

aqbanking-cli request --number=8 --aid=3 --transactions -c $temp --fromdate=$fromdate --todate=$todate


    


