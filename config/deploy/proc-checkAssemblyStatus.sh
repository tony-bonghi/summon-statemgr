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
  
# now that assembly has started, we have to wait for it to finish.
# once assembly has entered its ASSEMBLER stage it will produce txnlogs which the indexers will actively consume
# we will know when assembly has finished when checkAssemblyStatus returns 0
while true; do
  checkAssemblyStatus
  status=$?
  if [ $status == 0 ]; then
    logger INFO "assembly has completed"
    assemblyStopTime=`date +'%s'`
    let assemblyRuntime=${assemblyStopTime}-${assemblyStartTime}
    let assemblyRuntimeMinutes=${assemblyRuntime}/60
    logger INFO "assembly runtime: ${assemblyRuntimeMinutes} minutes"
    alertLogger "Search Update Cycle: assembly has completed in ${assemblyRuntimeMinutes} minutes" "Search update cycle has completed assembly in ${assemblyRuntimeMinutes} minutes at `date` on ${hostname} for environment: ${ACTUAL_ENV}"
    break
  elif [ $status == 1 ]; then
    logger INFO "assembly is incomplete"
  elif [ $status == 2 ]; then
    logger CRITICAL "cassandra is DOWN on ${cassandraFirstHost}!"
    die
  elif [ $status == 3 ]; then
    logger CRITICAL "synchronization is empty! this should not happen at this state"
    die
  fi
  
  # check every 5 minutes
  sleep 5m
done

exit 0;