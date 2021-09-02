#!/bin/bash

parent_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
cd "$parent_path"
source ./env.sh 

aqbanking-cli -P pinfile request --number=8 -b 54040042 --transactions -c $temp --fromdate=$fromdate --todate=$todate

    


