package com.proquest.magnolia.statemgr.zkclient;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads in the client process file to allow access to a list of ZKProcess objects.
 */
public class ProcessFileReader {

  private List<ZKProcess> zkProcesses = new ArrayList<ZKProcess>();
  private ZKProcess currProc = null;
  private SaxNode currNode = SaxNode.None;
  private enum SaxNode { None, Path, Args, Type, Node, DependencyNode};
  
  public ProcessFileReader() {
  }

  /**
   * Reads the client process input file containing the list of processes to be triggered
   * when state changes occur.
   *
   * @param processCfgFile  The process definition file.
   *
   * @return  A list of ZKProcess objects read from the input XML file.
   *
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public List<ZKProcess> read(String processCfgFile) throws IOException, SAXException, ParserConfigurationException {
    
    // Parses the process file
    SAXParser saxParser  = SAXParserFactory.newInstance().newSAXParser();
    saxParser.parse(new File(processCfgFile), new ProcessCfgHandler());

    return zkProcesses;
  }

  /**
   * ZKProcess client process configuration file parser handler.
   */
  class ProcessCfgHandler extends DefaultHandler {

    /**
     * Override to process the sleep, query, click and nextpage commands.
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

      super.startElement(uri, localName, qName, attributes);

      if ("process".equalsIgnoreCase(qName)) {
        currProc = new ZKProcess();
        zkProcesses.add(currProc);
      } else if ("path".equalsIgnoreCase(qName)) {
        currNode = SaxNode.Path;        
      } else if ("args".equalsIgnoreCase(qName)) {
        currNode = SaxNode.Args;
      } else if ("type".equalsIgnoreCase(qName)) {
        currNode = SaxNode.Type;
      } else if ("node".equalsIgnoreCase(qName)) {
        currNode = SaxNode.Node;
      } else if ("dependency".equalsIgnoreCase(qName)) {
        currNode = SaxNode.DependencyNode;
      }
    }

    /**
     * Updates the current ZKProcess object with the data from the
     * currently active Sax node.
     *
     * @param ch      The data
     * @param start   Start offset of the data
     * @param length  Data length
     * @throws SAXException
     */
    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
      
      String data = new String(ch, start, length);

      switch (currNode) {
        case Args:
          currProc.setArgs(data);
          break;
        case Path:
          currProc.setProcessPath(data);
          break;
        case Type:
          currProc.setProcessType(ZKProcess.ProcessType.Java);
          break;
        case Node:
          currProc.setNode(data);
          break;
        case DependencyNode:
        {
          String[] dns = data.split(",");
          for (String dn : dns) {
            currProc.addDependencyNode(dn);
          }
          break;
        }
        case None:
          break;
      }
      currNode = SaxNode.None;
    }
  }
}
