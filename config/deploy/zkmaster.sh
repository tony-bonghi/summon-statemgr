#!/bin/bash

java -cp ./summon_statemgr.jar com.proquest.magnolia.statemgr.zkmaster.ZKMaster 127.0.0.1:2181 ./mastercfg.properties $1 