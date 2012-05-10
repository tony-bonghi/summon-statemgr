package com.proquest.magnolia.statemgr.common;

/**
 * Constants for the ZK client applications.
 */
public interface ZKConstants {
  public static final String MASTER_NODE            = "/Master/State";
  public static final String MASTER_STATE_START     = "start";
  public static final String MASTER_STATE_CONTINUE  = "continue";
  public static final String MASTER_STATE_STOP      = "stop";
  public static final String STATE_IDLE             = "IDLE";
  public static final String STATE_INPROGRESS       = "IN_PROGRESS";
  public static final String STATE_SUCCESS          = "SUCCESS";
  public static final String STATE_ERROR            = "ERROR";
}
