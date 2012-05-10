package com.proquest.magnolia.statemgr.common;

/**
 * Constants for the ZK client applications.
 */
public interface ZKConstants {
  public static final String NODE_MASTER            = "/Master/State";
  public static final String MASTER_STATE_START     = "start";
  public static final String MASTER_STATE_CONTINUE  = "continue";
  public static final String MASTER_STATE_STOP      = "stop";

  public static final String NODE_STATE             = "/State";
  public static final String NODE_STATE_INFO        = "/StateInfo";
  public static final String NODE_DESCRIPTION       = "/Description";
  public static final String NODE_TIME_START        = "/Timestamp/Start";
  public static final String NODE_TIME_END          = "/Timestamp/End";

  public static final String STATE_IDLE             = "IDLE";
  public static final String STATE_INPROGRESS       = "IN_PROGRESS";
  public static final String STATE_SUCCESS          = "SUCCESS";
  public static final String STATE_ERROR            = "ERROR";
}
