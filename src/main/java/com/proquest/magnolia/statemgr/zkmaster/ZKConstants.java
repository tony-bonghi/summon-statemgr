package com.proquest.magnolia.statemgr.zkmaster;

/**
 * Created by IntelliJ IDEA.
 * User: TBonghi
 * Date: 5/7/12
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ZKConstants {
  public static final String MASTER_NODE = "/Master";
  public static final String STATE_START = "start";
  public static final String STATE_CONTINUE = "continue";
  public static final String STATE_STOP = "stop";
  public static final String STATE_IDLE = "IDLE";
  public static final String STATE_INPROGRESS = "IN_PROGRESS";
  public static final String STATE_COMPLETE = "COMPLETE";
  public static final String STATE_ERROR = "ERROR";
}
