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
  
alertLogger "Search Update Cycle Started" "Search update cycle has started at `date` on ${hostname} for environment: ${ACTUAL_ENV}"
echo "Search update cycle has started at `date` on ${hostname} for environment: ${ACTUAL_ENV}" | mail -s "Search Update Cycle Started" summon-contentingestion@lists.summon.serialssolutions.com 

# prepare indexers for an assembly run
# production has multiple datacenters
if [ ${ACTUAL_ENV} == "production" ]; then
  alertLogger "Search Update Cycle: Entering stage prepareIndexers iad1" "Search update cycle has entered stage prepareIndexers iad1 at `date` on ${hostname} for environment: ${ACTUAL_ENV}"
  logger INFO "preparing indexers at iad1"
  prepareIndexers iad1
  if [ $? != 0 ]; then
    logger WARN "prepareIndexers at iad1 failed, will try again!"
    unset FAILED
    prepareIndexers iad1
    if [ $? != 0 ]; then
      logger CRITICAL "prepareIndexers at iad1 failed!"
      die;
    fi
  fi
  logger INFO "preparing indexers at dfw1"
  alertLogger "Search Update Cycle: Entering stage prepareIndexers dfw1" "Search update cycle has entered stage prepareIndexers dfw1 at `date` on ${hostname} for environment: ${ACTUAL_ENV}"
  prepareIndexers dfw1
  if [ $? != 0 ]; then
    logger WARN "prepareIndexers at dfw1 failed, will try again!"
    unset FAILED
    prepareIndexers dfw1
    if [ $? != 0 ]; then
      logger WARN "prepareIndexers at dfw1 failed!"    
      die
    fi
  fi
# other environments dont
else
  logger INFO "preparing indexers"
  alertLogger "Search Update Cycle: Entering stage prepareIndexers" "Search update cycle has entered stage prepareIndexers at `date` on ${hostname} for environment: ${ACTUAL_ENV}"
  prepareIndexers
  if [ $? != 0 ]; then
    logger WARN "prepareIndexers failed, will try again!"
    prepareIndexers
    if [ $? != 0 ]; then
      logger CRITICAL "prepareIndexers failed!"
      die
    fi
  fi
fi
exit 0;
