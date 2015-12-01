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

import org.jmol.script.SV;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.PT;
import javajs.util.SB;

/**
 * A service for interacting with NBOServe (experimental)
 * 
 * TODO: figure out how to manage time-consuming asynchronous requests
 * 
 * 
 */
public class NBOService {

  /// BH: observations: 
  // 
  // 1) NBOServe creates jmol_infile.txt, jmol_molfile.txt, and jmol_outfile.txt. 
  // 
  // 2) Of these three, jmol_outfile is locked for writing during NBOServe sessions 
  //    and stays locked until the program finishes. 
  //    
  //    
  // 3) When View starts up, a new jmol_outfile is created, and it can be deleted.
  // 
  // 4) The process of View...Browse... leads to the following:
  // 
  //    07/02/2015  09:04 AM                20 jmol_infile.txt
  //    07/02/2015  09:04 AM               429 jmol_outfile.txt 
  //    07/02/2015  09:05 AM                95 v_test1435845900375.txt
  //    07/02/2015  09:05 AM                11 fort.106
  //    07/02/2015  09:05 AM                 0 v_test1435845900375.txtDONE
  // 
  // v_test... is 
  //
  //  GLOBAL C_PATH C:\temp
  //  GLOBAL I_KEYWORD 6
  //  GLOBAL C_JOBSTEM ch3nh2
  //  GLOBAL I_BAS_1 6
  //  CMD LABEL
  //
  // jmol_infile.txt is:
  //
  //          1    1    2
  //  
  // jmol_outfile.txt is:
  
  //    DATA " "
  //    1    1    2
  //  NBOServe: NATURAL BOND ORBITAL PROGRAM SUITE
  //  _______________________________________________
  //  (1) NBOModel:
  //    Create & edit molecular model and input files
  //  (2) NBORun:
  //    Launch NBO analysis for chosen archive (.47) file
  //  (3) NBOView:
  //    Display NBO orbitals in 1D/2D/3D imagery
  //  (4) NBOSearch:
  //    Search NBO output interactively
  //  Your choice (1-4), (H)elp, e(X)it, or (D)ir reset?
  //  END ""
  //
  // fort.106 is 
  //
  //    END ""
  //
  // note that the actual label information came back only over sysout.
  //
  // requesting a 3D view (raytrace) produces the following files:
  //
  //  07/02/2015  09:13 AM                97 v_test1435846433040.txt
  //  07/02/2015  09:13 AM                 0 v_test1435846433040.txtDONE
  //  07/02/2015  09:14 AM                59 v_test1435846446147.txt
  //  07/02/2015  09:14 AM           480,029 ch3nh2.bmp (in c:\temp)
  //  07/02/2015  09:14 AM                 0 v_test1435846446147.txtDONE
  //  07/02/2015  09:14 AM                 2 jmol_outfile.txt
  //  07/02/2015  09:14 AM               121 jmol_molfile.txt
  //  07/02/2015  09:14 AM             5,552 raytrace.rt

  // the first v_test is: 
  //
  //  GLOBAL C_PATH C:\temp
  //  GLOBAL C_JOBSTEM ch3nh2
  //  GLOBAL I_BAS_1 6
  //  GLOBAL SIGN +1 
  //  CMD PROFILE 10  
  // 
  // and the second is:
  //
  //  GLOBAL C_PATH C:\temp
  //  GLOBAL C_JOBSTEM ch3nh2
  //  CMD VIEW 1 
  
  
  
  // modes of operation
  
  // NOTE: There was a problem in that View and Raw were both 4 here
  
  static final int MODE_ERROR = -1;
  static final int MODE_RAW = 0;// leave this 0; it is referred to that in StatusListener
  

  // these are for panel state
  static final int MODE_MODEL = 1; // don't change this number -- it is used for LOAD NBO 
  static final int MODE_RUN = 20;
  static final int MODE_VIEW = 30;
  static final int MODE_SEARCH = 40;
  
  // these are for rawCmdNew only
  static final int MODE_VIEW_LIST = 33;
  static final int MODE_SEARCH_VALUE = 45;
  static final int MODE_SEARCH_LIST = 46;
  static final int MODE_SEARCH_SELECT = 47;
  static final int MODE_IMAGE = 88;

  private static final int MODE_ERR = -1;
  private int serverMode = MODE_RAW;

  protected Viewer vwr;

  NBODialog nboDialog;

  protected Process nboServer;
  protected Thread nboListener;
  private InputStream stdout;
  protected BufferedReader nboReader;
  private PrintWriter stdinWriter;
  protected static NBOJobQueueManager manager; 

  private SB sbRet;

  private boolean inData;
  protected boolean isWorking;
  String serverPath;
  String serverDir;
  String workingPath;

  private boolean nboSync;

  private String nboModel;
  protected int dialogMode;

  /**
   * Manage communication between Jmol and NBOServer
   * 
   * @param vwr
   *        The interacting display we are reproducing (source of view angle
   *        info etc)
   */
  public NBOService(Viewer vwr) {    
    this.vwr = vwr;
    sbRet = new SB();
    java.util.Properties props = JmolPanel.historyFile.getProperties();
    setServerPath(props.getProperty("nboServerPath",
        System.getProperty("user.home") + "/NBOServe"));
    setWorkingPath(null);
    if (manager == null)
      manager = new NBOJobQueueManager();
  }

  /**
   * Set path to NBOServe.exe
   * 
   * @param path
   */
  private void setServerPath(String path) {
    serverPath = path;
    serverDir = new File(serverPath).getParent() + "/";
  }

  /**
   * Set path for all file saving
   * 
   * @param path
   *        the desired path, or null to indicate to use the current Jmol
   *        directory path + /nbo
   */
  void setWorkingPath(String path) {
    workingPath = (path == null ? vwr.getDefaultDirectory() + "/nbo" : path);
    File dir = new File(workingPath);
    if (!dir.exists())
      dir.mkdir();
    System.out.println("NBOService setting directory to " + dir);
    //    workingPath = path.substring(0, path.lastIndexOf(File.separator));

  }

  public boolean processRequest(Map<String, Object> info, int mode) {
    boolean ok = false;
    boolean nboSync = (info.get("sync") == Boolean.TRUE);
    boolean isClosed = false;
    this.dialogMode = mode;
    if (nboServer != null)
      try {
        nboServer.exitValue();
        isClosed = true;
        System.out.println("NBOServe.exe has closed unexpectedly!");
      } catch (Exception IllegalThreadStateException) {
        //
      }
    if (nboSync || this.nboSync || nboServer == null || isClosed) {
      closeProcess();
      startProcess(nboSync, dialogMode);
    }
    serverMode = ((Integer) info.get("mode")).intValue();
    if (stdinWriter == null) {
      closeProcess();
      sbRet
          .append("ERROR: Could not connect to NBOServe -- Use Tools...NBO... to set up NBOServe");
      serverMode = MODE_ERR;
    }
    String nboAction = (String) info.get("action");
    String s;
    switch (serverMode) {
    case MODE_MODEL:
      // from Jmol script, not dialog
      s = (String) info.get("value");
      if (nboAction.equals("load")) {
        s = "sh " + s;
      } else if (!nboAction.equals("run")) {
        s = null;
      }
      break;
    case MODE_IMAGE:
    case MODE_VIEW_LIST:
    case MODE_RAW:
      s = (String) info.get("value");
      if (s.startsWith("<"))
        s = "\n" + s;
      break;
    default:
      nboReport("unknown mode", MODE_ERROR);
      s = null;
      break;
    }
    if (s != null) {
      if (nboSync) {
        clearServerFile("fort.106");
        clearServerFile("jmol_molfile.txt");
      }
      sendToNBO(serverMode, s);
      if (nboSync) {
        waitFor(serverMode);
      }
      ok = true;
    }
    if (nboSync) {
      info.put("ret", sbRet.toString());
      sbRet.setLength(0);
    }
    return ok;
  }

  private boolean waitFor(int mode) {
    String fname = serverDir + (mode == MODE_MODEL ? "jmol_molfile.txt" : "fort.106");
    File f = new File(fname);
    for (int i = 0; i < 200; i++) { // 2 seconds allowed
      if (f.exists()) {
        switch (mode) {
        case MODE_MODEL:
          String dataCmd = fixNBOModel(getFileData(fname));
          Logger.info(dataCmd);
          sbRet.append(dataCmd + ";rotate best;");     
          break;
        case MODE_RAW:
          // sbRet already created?
          break;
        }
        break;
      }
      try {
        Thread.sleep(10);
        System.out.println("NBOService.waitfor()");
      } catch (InterruptedException e) {
        return false;
      }
    }
    return true;
//    try {
//      nboServer.waitFor();
//      return true;
//    } catch (InterruptedException e) {
//      return false;
//    }
  }

  private boolean clearServerFile(String fname) {
    File f = new File(serverDir + fname);
    try {
      f.delete();
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  /**
   * @param mode
   * @param s
   */
  private void sendToNBO(int mode, String s) {
    s = (mode == MODE_RAW ? s : "\r\n" + mode + "\r\n" + s + "\r\nexit\r\n"
        + (nboSync ? "x\r\n" : ""));
    sendCmd(s);
  }

  private void sendCmd(String s) {
    System.out.println("sending " + s);
    if (s.startsWith("\n<"))
      System.out.println(getFileData(serverDir + PT.trim(s, "\n<>")));
    try {
      stdinWriter.println(s);
      stdinWriter.flush();
      Thread.sleep(10);
      System.out.println("NBOService.sendCmd()");
    } catch (InterruptedException e) {
      // TODO
    }
  }

  protected void nboReport(String line, int dialogMode) {
      if (Logger.debugging)
        Logger.debug(inData + " " + nboSync + " " + sbRet.length() + " "
            + "receiving: " + line);
      if (line.startsWith("DATA \" \"")) {
        isWorking = false;
      } else if (line.startsWith("DATA ")) {
        if (line.startsWith("DATA \"model")) {
          sbRet.setLength(0);
          line = fixNBOModel(line);
        }
        inData = (line.indexOf("exit") < 0);
        if (inData)
          sbRet.append(line + "\n");
        return;
      }
      if (!inData && line.indexOf("NBO") < 0
          && dialogMode == MODE_MODEL)
        if(!line.equals(""))
        nboDialog.addLine(NBODialogConfig.DIALOG_MODEL, line);
      if (inData && sbRet != null) {
        sbRet.append(line + "\n");
        if (line.indexOf("END") >= 0) {
          inData = false;
          String m = "\"" + nboModel + "\"";
          nboModel = "\0";
          if (!nboSync && line.indexOf(m) >= 0) {
            String s = sbRet.toString();
            sbRet.setLength(0);
            if(s.contains("\\")) 
              s = s.replaceAll("\\\\", "");
            runScriptQueued(s);// + ";rotate best;select remove {*}; select on;");
            
          }
          return;
        }
      }
    //if (nboDialog != null)
      //nboDialog.appendModelOutPanel(line);
      //nboDialog.nboReport(line);
  }

  /**
   * fixes DATA line to include a title
   * 
   * @param line
   * @return line or full data block
   */
  private String fixNBOModel(String line) {
    nboModel = PT.getQuotedStringAt(line, 0);
    String s = " NBO " + nboModel;
    int pt = line.indexOf("\n");
    return (pt < 0 ? line + s : line.substring(0, pt) + s + line.substring(pt));
  }

  String startProcess(boolean sync, final int mode) {
    this.dialogMode = mode;
    try {
      System.out.println("starting NBO process sync=" + sync);
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
        @SuppressWarnings("fallthrough")
        @Override
        public void run() {
          boolean haveStart = false;
          boolean inOpener = false;
          boolean inRequest = false;
          System.out.println("nboListener " + this + " running");
          while (!Thread.currentThread().isInterrupted()) {
            String line = null;
            try {
              Thread.sleep(25); // give it a breather after startup? 
              System.out.println("NBOService.startProcess()");
              while ((line = nboReader.readLine()) != null) {
                Thread.sleep(1);
                System.out.println("NBOService.line");
                // ignore the opener business
                if (line.indexOf("DATA \" \"") >= 0) {
                  Logger.info(" [NBO opener ignored]");
                  inOpener = true;
                  continue;
                }
                if (line.indexOf("END \"\"") >= 0) {
                  inOpener = false;
                  continue;
                }
                if (inOpener)
                  continue;
                Logger.info(line);
                if (line.indexOf("*start*") >= 0) {
                  haveStart = inRequest = isWorking = true;
                  nboDialog.addLine(NBODialogConfig.DIALOG_CONFIG, null);
                  continue;
                }
                if (line.indexOf("Permission denied") >= 0||line.indexOf("PGFIO-F")>=0 || line.indexOf("Invalid command")>=0) {
                  nboDialog.alert(line
                      + "\n\nNBOServe could not access key files -- Is another version running? Perhaps NBOPro?\n");
                  isWorking = inRequest = false;
                  manager.clearQueue();
                  continue;
                }
                if (line.indexOf("missing or invalid") >= 0){
                  vwr.alert(line);
                  manager.clearQueue();
                  inRequest = isWorking = false;
                }
                if (line.indexOf("FORTRAN STOP") >= 0){
                  vwr.alert("NBOServe has stopped working");
                  restart();
                }
                if (line.indexOf("*end*") >= 0) {
                  if (haveStart)
                    isWorking = false;
                  inRequest = haveStart = false;
                  continue;
                }
                switch (dialogMode) {
                case MODE_VIEW_LIST:
                case MODE_SEARCH_VALUE:
                  if (isWorking && inRequest)
                    nboDialog.addLine(NBODialogConfig.DIALOG_VIEW,line);
                  break;
                case MODE_SEARCH_LIST:
                  if (isWorking && inRequest) {
                    nboDialog.addLine(NBODialogConfig.DIALOG_LIST, line);
                  }
                  break;
                case MODE_SEARCH_SELECT:
                  if (line.startsWith(" Select")) {
                    nboDialog.addLine(NBODialogConfig.DIALOG_SEARCH, line);
                    isWorking = inRequest = false;
                  }
                  break;
                case MODE_IMAGE: 
                  if(line.startsWith("END"))
                    isWorking = inRequest = false;
                  if(line.contains("Missing valid")){
                    isWorking = inRequest = false;
                    vwr.alert(line);
                  }
                  break;
                case MODE_MODEL:
                  if(line.indexOf("can't do that")>=0){
                    nboDialog.addLine(NBODialogConfig.DIALOG_MODEL,line);
                    isWorking = inRequest = false;
                    break;
                  }
                case MODE_RAW:
                case MODE_RUN:
                default:
                  nboReport(line, dialogMode);
                  break;
                }
                try {
                  int test = nboServer.exitValue();
                  closeProcess();
                  System.out.println("NBOService test = " + test);
                  return;
                } catch (Exception IllegalThreadStateException) {
                  // still going
                }
              }
            } catch (Throwable e1) {
              closeProcess();
              // includes thread death
              return;
            }
          }
        }
      });
      nboListener.setName("NBOServiceThread" + System.currentTimeMillis());
      nboListener.start();
      stdinWriter = new PrintWriter(nboServer.getOutputStream());
    } catch (IOException e) {
      System.out.println(e.getMessage());
      return e.getMessage();
    }
    return null;
  }

  void closeProcess() {
    //    try {
    //      stdout.close();
    //    } catch (Exception e) {
    //    }
    isWorking = false;
    stdout = null;
    try {
      stdinWriter.close();
    } catch (Exception e) {
    }
    stdinWriter = null;
    try {
      nboReader.close();
    } catch (Exception e) {
    }
    nboReader = null;
    try {
      nboListener.interrupt();      
    } catch (Exception e) {
      System.out.println("can't interrupt");
    }
    nboListener = null;
    try {
      nboServer.destroy();
    } catch (Exception e) {
    }
    nboServer = null;
  }

  String restart() {
    closeProcess();
    //return null;
    return startProcess(false, MODE_RAW);
  }


  public boolean restartIfNecessary() {
    if (nboServer == null)
      startProcess(false, MODE_RAW);
    return (nboServer != null);
  }

  void runScriptQueued(String script) {
    Logger.info("NBO->JMOL ASYNC: " + script);
    vwr.script(script);
  }
  
  private Object lock = "jmol_lock";

  synchronized String runScriptNow(String script) {
    synchronized (lock) {
      Logger.info("NBO->JMOL SYNC: " + script);
      return vwr.runScript(script);
    }
  }

  synchronized public SV evaluateJmol(String expr) {
    synchronized (lock) {
    return vwr.evaluateExpressionAsVariable(expr);
    }
  }

  synchronized public String evaluateJmolString(String expr) {
    synchronized (lock) {
      return evaluateJmol(expr).asString();
    }
  }

  synchronized public String getJmolFilename() {
    synchronized (lock) {
    return evaluateJmolString("getProperty('filename')");
    }
  }

  public boolean jobCanceled;
  
  
  /**
   * The interface for ALL communication with NBOServe from NBODialog.
   * 
   * @param cmd
   * @param data
   * @param doWait
   * @param dialogMode 
   */
  protected void rawCmdNew(String cmd, SB data, boolean doWait, int dialogMode) {
      doWait = true;
      String fname = null;
      File cmdFile = null;
      try {
        if (data == null) {
          Logger.info("issuing\n" + cmd);
        } else {
          fname = cmd + "_test.txt";// + System.currentTimeMillis() + ".txt";
          cmdFile = new File(serverDir + fname);
          Logger.info("issuing " + fname + "\n" + data);
          writeToFile(data.toString(), cmdFile);
          cmd = "<" + fname + ">";
          isWorking = doWait;
        }
        Map<String, Object> info = new Hashtable<String, Object>();
        info.put("mode", Integer.valueOf(NBOService.MODE_RAW));
        info.put("sync", Boolean.FALSE);
        info.put("action", "cmd");
        info.put("value", cmd);
        if (!processRequest(info, dialogMode)) {
          nboReport(null, dialogMode);
          nboReport("not implemented", dialogMode);
          isWorking = false;
        }
        if (doWait) {
          while (isWorking) {
            System.out.println("NBOService rawCmd");
            Thread.sleep(10);
          }
        }
        if (cmdFile != null) {
          cmdFile = new File(serverDir + fname + "DONE");
          writeToFile("", cmdFile);
        }
          
      } catch (IOException e) {
        System.out.println("Could not write to " + fname);
        isWorking = false;
      } catch (InterruptedException e) {
        isWorking = false;
      }
  }

  void writeToFile(String s, File file) throws IOException {
    PrintWriter writer = new PrintWriter(file);
    writer.print(s);
    writer.close();
    //Logger.info(s.length() + " bytes written to " + file + "\n" + s);
  }

  String getFileData(String fileName) {
    return vwr.getFileAsString4(fileName, Integer.MAX_VALUE,
        false, false, false, "nbo");
  }

  public void queueJob(String name, String statusInfo, Runnable process) {
    manager.addJob(this, name, statusInfo, process);
  }


}
