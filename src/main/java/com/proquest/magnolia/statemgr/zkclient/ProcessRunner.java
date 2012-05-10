package com.proquest.magnolia.statemgr.zkclient;

import com.proquest.magnolia.statemgr.common.ZKClientBase;
import com.proquest.magnolia.statemgr.common.ZKConstants;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Responsible for running the actual process on the machine.
 */
public class ProcessRunner implements Runnable {

  private static final Logger logger = Logger.getLogger(ProcessRunner.class.getName());
  
  private ZKClientBase zk = null;
  private ZKProcess zkProc = null;
  private Process child = null;

  /**
   * Constructor.
   * @param zk        ZKClient Reference
   * @param zkProcess Process to run
   */
  public ProcessRunner(ZKClientBase zk, ZKProcess zkProcess) {
    this.zk = zk;
    this.zkProc = zkProcess;
  }

  /**
   * Starts the process associated with this object on a new thread.
   */
  public void startProcess() {
    logger.info(String.format("Starting the thread used to start the process {process=[%s]}", zkProc.getProcessPath()));
    Thread t = new Thread(this);
    t.setDaemon(true);
    t.setName("subproc-" + zkProc.getNode());
    t.start();
  }

  /**
   * Runnable...
   */
  @Override
  public void run() {
    try {
      int retVal = 0;
      
      logger.info(String.format("Starting child process {process=[%s]}", zkProc.getProcessPath()));
      
      zk.setData(zkProc.getNode() + "/State", ZKConstants.STATE_INPROGRESS);
      child = Runtime.getRuntime().exec(zkProc.getProcessPath() + " " + zkProc.getArgs());
      StreamWriter stdOut = new StreamWriter(child.getInputStream());
      StreamWriter stdErr = new StreamWriter(child.getErrorStream());
      try {
        retVal = child.waitFor();
      } catch (InterruptedException e) {
        logger.warn(String.format("Process interrupted  {process=[%s]", zkProc.getProcessPath()));
        retVal = 1;
      }
      child = null;
      
      if (retVal == 0) {
        logger.info(String.format("Process succeeded { {process=[%s]}", zkProc.getProcessPath()));
        zk.setData(zkProc.getNode() + "/State", ZKConstants.STATE_SUCCESS);
      } else {
        logger.error(String.format("Process failed { {process=[%s]}", zkProc.getProcessPath()));
        zk.setData(zkProc.getNode() + "/State", ZKConstants.STATE_ERROR);
      }

      if (stdErr.getOutput().length() > 0) {
        zk.setData(zkProc.getNode() + "/StateInfo", stdErr.getOutput());
      }

      if (stdOut.getOutput().length() > 0) {
        zk.setData(zkProc.getNode() + "/StateInfo", stdOut.getOutput());
      }

    } catch (Exception e) {
      logger.error("", e);
    }
  }

  /**
   * Stops the client process synchronously if one exists.
   * @param logMessage  A log message
   */
  public void stopProcess(String logMessage) {
    if (child != null) {
      logger.info(logMessage);
      child.destroy();
      try {
        child.waitFor();
      } catch (InterruptedException e) {
      }
      child = null;
    }
  }

  /**
   * Buffers the log information output by the specified process.
   * This information is written out to the stateInfo node.
   */
  static class StreamWriter extends Thread {
    InputStream is = null;
    StringBuilder buffer = new StringBuilder();

    StreamWriter(InputStream is) {
      this.is = is;
      start();
    }

    public String getOutput() {
      return buffer.toString();
    }
    
    public void run() {
      byte b[] = new byte[80];
      int rc;
      try {
        while ((rc = is.read(b)) > 0) {
          buffer.append(new String(b));
        }
      } catch (IOException e) {
      }
    }
  }
}
