#!/usr/bin/env bash

#
#  Copyright (C) 2018 Hortonworks Inc.
#
#  Licenced under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

if [[ "x${DTEST_HOME}" == "x" ]]
then
    echo Please set DTEST_HOME
    exit 1
fi

NUM_CONTAINERS=${DTEST_NUM_CONTAINERS:-10}

build_branch=$1
build_label=$2
build_repository=$3
profile=$4
shift; shift; shift; shift;


$DTEST_HOME/bin/dtest -b $build_branch -c $NUM_CONTAINERS -d $WORKSPACE -l $build_label -r $build_repository -p $profile $@
exit $?
