package com.proquest.magnolia.statemgr.zkclient;

/**
 * Represents a process that can be launched by the ZKClient.
 */
public class ZKProcess {

  private String processPath = "";
  private String args = "";
  private ProcessType processType = ProcessType.Java;
  private String dependencyNode = "";
  private String node = "";

  enum ProcessType {
    Java, Shell, EXE, Gradle
  }

  public String getProcessPath() {
    return processPath;
  }

  public void setProcessPath(String processPath) {
    this.processPath = processPath;
  }

  public String getArgs() {
    return args;
  }

  public void setArgs(String args) {
    this.args = args;
  }

  public ProcessType getProcessType() {
    return processType;
  }

  public void setProcessType(ProcessType processType) {
    this.processType = processType;
  }

  public String getDependencyNode() {
    return dependencyNode;
  }

  public void setDependencyNode(String dependencyNode) {
    this.dependencyNode = dependencyNode;
  }

  public String getNode() {
    return node;
  }

  public void setNode(String node) {
    this.node = node;
  }
}
