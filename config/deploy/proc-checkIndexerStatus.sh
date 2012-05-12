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
  
# assembly has finished but there is a good chance that indexers are still consuming txnlogs and they must finish.
# if an index check fails, we don't break and die. wait for someone to fix the indexer as it is the most time consuming stage
iad1_DONE="false"
dfw1_DONE="false"
while true; do
  # production has multiple datacenters
  if [ ${ACTUAL_ENV} == "production" ]; then
    # only check iad1 if its not finished
    if [ ${iad1_DONE} != "true" ]; then
      logger INFO "checking indexers at iad1"
      checkIndexerStatus iad1
      status=$?
      if [ $status == 0 ]; then
        logger INFO "indexing has completed at iad1"
        iad1_DONE="true"
      elif [ $status == 1 ]; then
        logger INFO "waiting for all indexers at iad1 to finish indexing"
      elif [ $status == 2 ]; then
        logger CRITICAL "indexing has failed at iad1"
        die
      fi
    fi

    # only check dfw1 if its not finished
    if [ ${dfw1_DONE} != "true" ]; then    
      logger INFO "checking indexers at dfw1"
      checkIndexerStatus dfw1
      status=$?
      if [ $status == 0 ]; then
        logger INFO "indexing has completed at dfw1"
        dfw1_DONE="true"
      elif [ $status == 1 ]; then
        logger INFO "waiting for all indexers at dfw1 to finish indexing"
      elif [ $status == 2 ]; then
        logger CRITICAL "indexing has failed at dfw1"
        die
      fi
    fi
    
    # if both iad1 and dfw1 are done indexing we can finally break
    if [[ ${iad1_DONE} == "true" ]] && [[ ${dfw1_DONE} == "true" ]]; then
      logger INFO "both iad1 and dfw1 finished indexing"
      alertLogger "Search Update Cycle: Indexing completed" "Search update cycle has completed indexing at `date` on ${hostname} for environment: ${ACTUAL_ENV}"
      break
    fi
    
  # other environments dont have multiple datacenters to worry about
  else
    logger INFO "checking indexers"
    checkIndexerStatus
    status=$?
    if [ $status == 0 ]; then
      logger INFO "indexing has completed"
      alertLogger "Search Update Cycle: Indexing completed" "Search update cycle has completed indexing at `date` on ${hostname} for environment: ${ACTUAL_ENV}"
      break
    elif [ $status == 1 ]; then
      logger INFO "indexing is still running"
    elif [ $status == 2 ]; then
      logger CRITICAL "indexing has failed"
      die
    fi
  fi

  # check every 5 minutes
  sleep 5m
done

exit 0;