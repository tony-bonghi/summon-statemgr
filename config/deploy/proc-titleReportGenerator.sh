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
  
# Title Report Generator, must run after assembly, during search update
if [ ${ACTUAL_ENV} == "production" ]; then
  logger INFO "Title Report Generator launching on ${reportGeneratorHost}"
  cmdToRun='cd ${napcurrentversion}; ./start-title-report.sh'
  cmd=`pdsh -w ${reportGeneratorHost} "${cmdToRun}" 2>&1`
  if [ $? != 0 ]; then
  logger CRITICAL "pdsh to hosts ${reportGeneratorHost} failed"
    # Don't die if it fails, because nothing in the update process actually depends on this happening
  else
    logger INFO "Title Report Generator launched on ${reportGeneratorHost}"
  fi
fi
# Title Report Generator section for "test" environment
if [ ${ACTUAL_ENV} == "test" ]; then
  logger INFO "Title Report Generator launching on ${reportGeneratorHost}"
  cmdToRun='cd ${napcurrentversion}; ./start-title-report.sh'
  cmd=`pdsh -w ${reportGeneratorHost} "${cmdToRun}" 2>&1`
  if [ $? != 0 ]; then
  logger CRITICAL "pdsh to hosts ${reportGeneratorHost} failed"
    # Don't die if it fails, because nothing in the update process actually depends on this happening
  else
    logger INFO "Title Report Generator launched on ${reportGeneratorHost}"
  fi
fi

