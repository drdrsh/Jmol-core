/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-12-13 22:43:17 -0600 (Sat, 13 Dec 2014) $
 * $Revision: 20162 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.openscience.jmol.app.nbo;

import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.jmolpanel.JmolPanel;
import org.openscience.jmol.app.jmolpanel.NBODialog;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Scanner;

import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

/**
 * A service for interacting with NBOServe (experimental)
 * 
 * TODO: figure out how to manage time-consuming asynchronous requests
 * 
 */
public class NBOService {

  public static final int MODEL = 1; 

  public static final int RUN = 2;

  public static final int VIEW = 3;


  private transient Viewer vwr;
  
  public NBODialog nboDialog;
    
  private Process nboServer;
  private Thread nboListener;
  private InputStream stdout;
  public Scanner nboScanner;
  public BufferedReader nboReader;
  private int nboMode;
  private PrintWriter stdinWriter;

  private SB sbRet;
  private String nboAction;

  private boolean inData;

  public String serverPath;
  public String workingPath;

  private boolean nboSync;

  private String nboModel;
  

  /**
   * Manage communication between Jmol and NBOServer
   * 
   * @param vwr The interacting display we are reproducing (source of view angle info etc)
   */
  public NBOService(Viewer vwr) {
    this.vwr = vwr;
    sbRet = new SB();
    java.util.Properties props = JmolPanel.historyFile.getProperties();
    setServerPath(props.getProperty("nboServerPath",
        System.getProperty("user.home") + "/NBOServe"));
  }

  private void setServerPath(String path) {
    serverPath = path;
    workingPath = path.substring(0, path.lastIndexOf(File.separator));
  }

  public boolean processRequest(Map<String, Object> info) {
    boolean ok = false;
    boolean nboSync = (info.get("sync") == Boolean.TRUE);
    boolean isClosed = false;
    if (nboServer != null)
      try {
        nboServer.exitValue();
        isClosed = true;
      } catch (Exception e) {
        //
      }
    if (nboSync || this.nboSync || nboServer == null || isClosed) {
      closeProcess();
      startProcess(nboSync);
    }
    nboMode = ((Integer) info.get("mode")).intValue();
    if (stdinWriter == null) {
      closeProcess();
      sbRet
          .append("ERROR: Could not connect to NBOServe -- Use Tools...NBO... to set up NBOServe");
      nboMode = 0;
    }
    nboAction = (String) info.get("action");
    String s;
    switch (nboMode) {
    case MODEL:
      s = (String) info.get("value");
      if (nboAction.equals("load")) {
        s = "sh " + s;
      } else if (nboAction.equals("run")) {
        // ok as is
      } else {
        s = null;
      }
      break;
    case VIEW:
      s = (String) info.get("value");
      break;
    default:
      nboReport("unknown mode");
      s = null;
      break;
    }
    if (s != null) {
      sendToNBO(nboMode, s);
      if (nboSync) {
        try {
          nboServer.waitFor();
        } catch (InterruptedException e) {
          return false;
        }
      }
      ok = true;
    }
    if (nboSync) {
      info.put("ret", sbRet.toString());
      sbRet.setLength(0);
    }
    return ok;
  }

  /**
   * @param mode
   * @param s
   */
  private void sendToNBO(int mode, String s) {
    s = mode + "\n" + s + "\nexit" + (nboSync ? "\nexit" : "");
    sendCmd(s);
  }
  
  private void sendCmd(String s) {    
    System.out.println("sending: " + s + "\n");
    stdinWriter.println(s);
    stdinWriter.flush();
  }

  public void nboReport(String line) {
    System.out.println("receiving: " + line);
    if (nboDialog != null)
      nboDialog.nboReport(line);
    if (line.startsWith("DATA ")) {
      if (line.startsWith("DATA \"model")) {
        nboModel = PT.getQuotedStringAt(line, 0);
        line += " NBO " + nboModel;
      }
      inData = (line.indexOf("exit") < 0);
    }
    if (inData) {
      sbRet.append(line + "\n");
    }
    if (inData && line.indexOf("END") >= 0) {
      inData = false;
      String s = sbRet.toString();
      sbRet.setLength(0);
      String m = "\"" + nboModel + "\"";
      nboModel = "\0";
      if (!nboSync && line.indexOf(m) >= 0)
        vwr.script(s);
    }
  }


  public String startProcess(boolean sync) {
    try {
      nboSync = sync;
      File pathToExecutable = new File(serverPath);
      ProcessBuilder builder = new ProcessBuilder(serverPath);
      builder.directory(new File(pathToExecutable.getParent())); // this is where you set the root folder for the executable to run with
      builder.redirectErrorStream(true);
      nboServer = builder.start();
      stdout = nboServer.getInputStream();
      nboReader = new BufferedReader(new InputStreamReader(stdout));
      nboListener = null;
      nboListener = new Thread(new Runnable() {
        @Override
        public void run() {
          while (true) {
            String line;
            try {
              while ((line = nboReader.readLine()) != null)
                nboReport(line);
            } catch (Exception e1) {
            }
            break;
          }
        }
      });
      nboListener.start();
      stdinWriter = new PrintWriter(nboServer.getOutputStream());
    } catch (IOException e) {
      System.out.println(e.getMessage());
      return e.getMessage();
    }
    return null;
  }
  
  public void closeProcess() {
//    try {
//      stdout.close();
//    } catch (Exception e) {
//    }
    stdout = null;
    try {
      stdinWriter.close();
    } catch (Exception e) {
    }
    stdinWriter = null;
    try {
      nboScanner.close();
    } catch (Exception e) {
    }
    nboScanner = null;
    try {
      nboReader.close();
    } catch (Exception e) {
    }
    nboReader = null;
    try {
      nboListener.interrupt();
    } catch (Exception e) {
    }
    nboListener = null;
    try {
      nboServer.destroy();
    } catch (Exception e) {
    }
    nboServer = null;
  }


//  private String getNBOFile(String fname) {
//    return vwr.getFileAsString3(workingPath.replace('\\', '/') + "/" + fname, true, "nbodialog");
//  }

  /**
   * Just saves the path settings from this session.
   */
  public void saveHistory() {
    java.util.Properties props = new java.util.Properties();
    props.setProperty("nboServerPath", serverPath);
    //props.setProperty("nboWorkingPath", workingPath);
    JmolPanel.historyFile.addProperties(props);
  }
  
}
