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

# now that indexing has finished, we can promote the shards
# production has multiple datacenters and we do things in a certain order for the cluster coordination
if [ ${ACTUAL_ENV} == "production" ]; then
  alertLogger "Search Update Cycle: Entering stage updateProductionSearch" "Search update cycle has entered stage updateProductionSearch at `date` on ${hostname} for environment: ${ACTUAL_ENV}"
  updateProductionSearch
  if [ $? != 0 ]; then
    logger CRITICAL "production search update failed!"
    die
  fi
# other environments dont
else
  logger INFO "promoting shards"
  alertLogger "Search Update Cycle: Entering stage shardUpdate" "Search update cycle has entered stage shardUpdate at `date` on ${hostname} for environment: ${ACTUAL_ENV}"
  shardUpdate
  if [ $? != 0 ]; then
    logger CRITICAL "shardUpdate failed"
    die
  fi
fi

alertLogger "Search Update Cycle Finished" "Search update cycle has finished at `date` on ${hostname} for environment: ${ACTUAL_ENV}"
echo "Search update cycle has finished on `date` on ${hostname} for environment: ${ACTUAL_ENV}" | mail -s "Search Update Cycle Finished" summon-contentingestion@lists.summon.serialssolutions.com

exit 0;