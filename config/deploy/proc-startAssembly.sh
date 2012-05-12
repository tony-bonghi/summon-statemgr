#!/bin/bash
#set -x
# NAP assembly Automation

PRG_DIR=`dirname "${0}"`
BASE_DIR=`cd "${PRG_DIR}" ; pwd`
BASE_NAME=`basename ${0}`

. ${BASE_DIR}/util-functions.sh
. ${BASE_DIR}/magnolia-functions.sh
. ${BASE_DIR}/nap-update-functions.sh

setenvironment "$1" "$2"
setNapEnvironment
if [ ${ACTUAL_ENV} == "production" ]; then
  . ${BASE_DIR}/loadbalancer-functions.sh
fi

# now that indexers are clean and ready, lets fire up assembly
logger INFO "starting assembly"
alertLogger "Search Update Cycle: Entering stage assembly" "Search update cycle has entered stage assembly at `date` on ${hostname} for environment: ${ACTUAL_ENV}"
ensureDirExists ${titleReportDir}
${BASE_DIR}/start-assembly-ALL.sh
if [ $? != 0 ]; then
  logger CRITICAL "assembly did not start clean"
  die;
else
  logger INFO "assembly started"
  assemblyStartTime=`date +'%s'`
fi

exit 0;