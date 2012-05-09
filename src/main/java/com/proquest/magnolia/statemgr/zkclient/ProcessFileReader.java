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
 * Created by IntelliJ IDEA.
 * User: TBonghi
 * Date: 5/8/12
 * Time: 3:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProcessFileReader {

  private List<ZKProcess> processes = new ArrayList<ZKProcess>();
  private ZKProcess currProcess = null;
  private SaxNode currNode = SaxNode.None;
  private enum SaxNode { None, Path, Args, Type, Node, DependencyNode};
  
  public ProcessFileReader() {
  }

  public List<ZKProcess> read(String processCfgFile) throws IOException, SAXException, ParserConfigurationException {
    
    // Parses the process file
    SAXParser saxParser  = SAXParserFactory.newInstance().newSAXParser();
    saxParser.parse(new File(processCfgFile), new ProcessCfgHandler());

    return processes;
  }

  /**
   * Session activity file processor.
   */
  class ProcessCfgHandler extends DefaultHandler {

    /**
     * Override to process the sleep, query, click and nextpage commands.
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

      super.startElement(uri, localName, qName, attributes);

      if ("process".equalsIgnoreCase(qName)) {
        currProcess = new ZKProcess();
        processes.add(currProcess);
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

    public void characters(char ch[], int start, int length) throws SAXException {
      
      String data = new String(ch, start, length);

      switch (currNode) {
        case Args:
          currProcess.setArgs(data);
          break;
        case Path:
          currProcess.setProcessPath(data);
          break;
        case Type:
          currProcess.setProcessType(ZKProcess.ProcessType.Java);
          break;
        case Node:
          currProcess.setNode(data);
          break;
        case DependencyNode:
          currProcess.setDependencyNode(data);
          break;
        case None:
          break;
      }

      currNode = SaxNode.None;
    }
  }
}
