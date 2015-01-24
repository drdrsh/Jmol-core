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

import javajs.util.SB;

/**
 * A service for interacting with NBOServe (experimental)
 * 
 * TODO: figure out how to manage time-consuming asynchronous requests
 * 
 */
public class NBOService {

  public static final int MODEL = 1; // do not change - referred to as "1" in org.jmol.script.ScriptEval.cmdLoad

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

  /**
   * Manage communication between Jmol and NBOServer
   * 
   * @param vwr The interacting display we are reproducing (source of view angle info etc)
   */
  public NBOService(Viewer vwr) {
    this.vwr = vwr;
    java.util.Properties props = JmolPanel.historyFile.getProperties();
    serverPath = props.getProperty("nboServerPath",
        System.getProperty("user.home"));
    workingPath = props.getProperty("nboWorkingPath",
        System.getProperty("user.home"));
  }

  public boolean processRequest(Map<String, Object> info) {
    boolean ok = false;
    boolean sync = (info.get("sync") == Boolean.TRUE);
    sbRet = (sync ? new SB() : null);
    closeProcess();
    startProcess(sync);
    nboMode = ((Integer) info.get("mode")).intValue();
    if (stdinWriter == null) {
      closeProcess();
      sbRet.append("ERROR: Could not connect to NBOServe -- Use Tools...NBO... to set up NBOServe");
      nboMode = 0;
    }
    nboAction = (String) info.get("action");
    switch (nboMode) {
    case MODEL:
      String s = (String) info.get("value");
      if (nboAction.equals("load")) {
        s = "sh " + s;
      } else if (nboAction.equals("run")) {
        // ok as is
      } else {
        s = null;
      }
      if (s != null) {
        sendToNBO(MODEL, s, sync);
        if (sync) {
          try {
            nboServer.waitFor();
          } catch (InterruptedException e) {
            return false;  
          }
          sbRet.append(getModel());
        }
        ok = true;
      }
      break;
    default:
      if (sync)
        sbRet.append("unknown mode");
      break;
    }
    if (sync) {
      info.put("ret", sbRet.toString());
    }    
    return ok;
  }

  /**
   * temporary kludge to force exit
   * 
   * @param mode
   * @param s
   * @param sync
   */
  private void sendToNBO(int mode, String s, boolean sync) {
    stdinWriter.println(mode + "\n" + s + "\n" + (true || sync ? "exit\nexit\n" : ""));
    stdinWriter.flush();
  }
  
  public void nboReport(String nextLine) {
    System.out.println(nextLine);
    if (sbRet != null)
      sbRet.append(nextLine + "\n");
    try {
      if (nboDialog != null)
        nboDialog.nboReport(nextLine);
    } catch (Throwable t) {
      // ignore
    }
  }

  public String serverPath;
  public String workingPath;
  
  public String startProcess(boolean sync) {
    try {
      File pathToExecutable = new File(serverPath);
      ProcessBuilder builder = new ProcessBuilder(serverPath);
      builder.directory(new File(pathToExecutable.getParent())); // this is where you set the root folder for the executable to run with
      builder.redirectErrorStream(true);
      nboServer = builder.start();
      stdout = nboServer.getInputStream();
      nboReader = new BufferedReader(new InputStreamReader(stdout));
      nboListener = null;
      if (!sync) {
        nboListener = new Thread(new Runnable() {
          @Override
          public void run() {
            while (true) {
              String line;
              try {
                while ((line = nboReader.readLine()) != null) {
                  nboReport(line);
                }
                asyncCallback();
              } catch (Exception e1) {
              }
              break;
            }
          }
        });
        nboListener.start();
      }
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


  /**
   * process report from NBO -- asynchronous only
   */
  public void asyncCallback() {
    switch (nboMode) {
    case MODEL:
      if (nboAction.equals("load")) {
        String s = getModel();
        nboDialog.setModel(s);
        vwr.loadInline(s);
      } else {
        nboDialog.nboReport(null);
        nboDialog.nboReport(getOutput());        
      }
    }
  }

  /**
   * temporary only
   * 
   * @return model data
   */
  private String getModel() {
    return getNBOFile("jmol_molfile.txt");
  }

  /**
   * temporary only
   * 
   * @return output data
   */
  private String getOutput() {
    return getNBOFile("jmol_outfile.txt");
  }

  private String getNBOFile(String fname) {
    return vwr.getFileAsString3(workingPath.replace('\\', '/') + "/" + fname, true, "nbodialog");
  }

  /**
   * Just saves the path settings from this session.
   */
  public void saveHistory() {
    java.util.Properties props = new java.util.Properties();
    props.setProperty("nboServerPath", serverPath);
    props.setProperty("nboWorkingPath", workingPath);
    JmolPanel.historyFile.addProperties(props);
  }
  
}
