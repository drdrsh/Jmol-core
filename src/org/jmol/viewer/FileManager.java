/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development Team
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
package org.jmol.viewer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.Map;

import javajs.api.BytePoster;
import javajs.api.GenericBinaryDocument;
import javajs.api.GenericFileInterface;
import javajs.util.AU;
import javajs.util.BArray;
import javajs.util.Base64;
import javajs.util.DataReader;
import javajs.util.Lst;
import javajs.util.OC;
import javajs.util.PT;
import javajs.util.Rdr;
import javajs.util.SB;

import org.jmol.api.Interface;
import org.jmol.api.JmolDomReaderInterface;
import org.jmol.api.JmolFilesReaderInterface;
import org.jmol.io.FileReader;
import org.jmol.io.JmolBinary;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer.ACCESS;


public class FileManager implements BytePoster {

  public static String SIMULATION_PROTOCOL = "http://SIMULATION/";

  public Viewer vwr;

  public JmolBinary jmb;

  FileManager(Viewer vwr) {
    this.vwr = vwr;
    jmb = new JmolBinary(this);
    clear();
  }

  void clear() {
    // from zap
    setFileInfo(new String[] { vwr.getZapName() });
    jmb.spardirCache = null;
   
  }

  public void clearPngjCache(String fileName) {
    jmb.clearPngjCache(fileName == null ? null : getCanonicalName(Rdr.getZipRoot(fileName)));
  }

  private void setLoadState(Map<String, Object> htParams) {
    if (vwr.getPreserveState()) {
      htParams.put("loadState", vwr.g.getLoadState(htParams));
    }
  }

  private String pathForAllFiles = ""; // leave private because of setPathForAllFiles
  
  public String getPathForAllFiles() {
    return pathForAllFiles;
  }
  
  String setPathForAllFiles(String value) {
    if (value.length() > 0 && !value.endsWith("/") && !value.endsWith("|"))
        value += "/";
    return pathForAllFiles = value;
  }

  private String nameAsGiven = JC.ZAP_TITLE, fullPathName, lastFullPathName, lastNameAsGiven = JC.ZAP_TITLE, fileName;

  /**
   * Set fullPathName, fileName, and nameAsGiven
   * 
   * @param fileInfo if null, replace fullPathName and nameAsGiven with last version of such
   * 
   * 
   */
  public void setFileInfo(String[] fileInfo) {
    if (fileInfo == null) {
      fullPathName = lastFullPathName;
      nameAsGiven = lastNameAsGiven;
      return;
    }
    // used by ScriptEvaluator dataFrame and load methods to temporarily save the state here
    fullPathName = fileInfo[0];
    fileName = fileInfo[Math.min(1,  fileInfo.length - 1)];
    nameAsGiven = fileInfo[Math.min(2, fileInfo.length - 1)];
    if (!nameAsGiven.equals(JC.ZAP_TITLE)) {
      lastNameAsGiven = nameAsGiven;
      lastFullPathName = fullPathName;
    }
  }

  public String[] getFileInfo() {
    // used by ScriptEvaluator dataFrame method
    return new String[] { fullPathName, fileName, nameAsGiven };
  }

  public String getFullPathName(boolean orPrevious) {
    String f =(fullPathName != null ? fullPathName : nameAsGiven);
    return (!orPrevious || !f.equals(JC.ZAP_TITLE) ? f : lastFullPathName != null ? lastFullPathName : lastNameAsGiven);
  }

  public String getFileName() {
    return fileName != null ? fileName : nameAsGiven;
  }

  // for applet proxy
  private URL appletDocumentBaseURL = null;
  private String appletProxy;

  String getAppletDocumentBase() {
    return (appletDocumentBaseURL == null ? "" : appletDocumentBaseURL.toString());
  }

  void setAppletContext(String documentBase) {
    try {
      System.out.println("setting document base to \"" + documentBase + "\"");      
      appletDocumentBaseURL = (documentBase.length() == 0 ? null : new URL((URL) null, documentBase, null));
    } catch (MalformedURLException e) {
      System.out.println("error setting document base to " + documentBase);
    }
  }

  void setAppletProxy(String appletProxy) {
    this.appletProxy = (appletProxy == null || appletProxy.length() == 0 ? null
        : appletProxy);
  }


  /////////////// createAtomSetCollectionFromXXX methods /////////////////

  // where XXX = File, Files, String, Strings, ArrayData, DOM, Reader

  /*
   * note -- createAtomSetCollectionFromXXX methods
   * were "openXXX" before refactoring 11/29/2008 -- BH
   * 
   * The problem was that while they did open the file, they
   * (mostly) also closed them, and this was confusing.
   * 
   * The term "clientFile" was replaced by "atomSetCollection"
   * here because that's what it is --- an AtomSetCollection,
   * not a file. The file is closed at this point. What is
   * returned is the atomSetCollection object.
   * 
   * One could say this is just semantics, but there were
   * subtle bugs here, where readers were not always being 
   * closed explicitly. In the process of identifying Out of
   * Memory Errors, I felt it was necessary to clarify all this.
   * 
   * Apologies to those who feel the original clientFile notation
   * was more generalizable or understandable. 
   * 
   */
  Object createAtomSetCollectionFromFile(String name,
                                         Map<String, Object> htParams,
                                         boolean isAppend) {
    if (htParams.get("atomDataOnly") == null)
      setLoadState(htParams);
    String name0 = name;
    name = vwr.resolveDatabaseFormat(name);
    if (!name0.equals(name) && name0.indexOf("/") < 0 
        && (name0.startsWith("$") || name0.startsWith(":") || name0.startsWith("==")))
      htParams.put("dbName", name0);
    int pt = name.indexOf("::");
    String nameAsGiven = (pt >= 0 ? name.substring(pt + 2) : name);
    String fileType = (pt >= 0 ? name.substring(0, pt) : null);
    Logger.info("\nFileManager.getAtomSetCollectionFromFile(" + nameAsGiven
        + ")" + (name.equals(nameAsGiven) ? "" : " //" + name));
    String[] names = getClassifiedName(nameAsGiven, true);
    if (names.length == 1)
      return names[0];
    String fullPathName = names[0];
    String fileName = names[1];
    htParams.put("fullPathName", (fileType == null ? "" : fileType + "::")
        + fullPathName.replace('\\', '/'));
    if (vwr.getBoolean(T.messagestylechime) && vwr.getBoolean(T.debugscript))
      vwr.getChimeMessenger().update(fullPathName);
    FileReader fileReader = new FileReader(this, vwr, fileName, fullPathName, nameAsGiven,
        fileType, null, htParams, isAppend);
    fileReader.run();
    return fileReader.getAtomSetCollection();
  }

  Object createAtomSetCollectionFromFiles(String[] fileNames,
                                          Map<String, Object> htParams,
                                          boolean isAppend) {
    setLoadState(htParams);
    String[] fullPathNames = new String[fileNames.length];
    String[] namesAsGiven = new String[fileNames.length];
    String[] fileTypes = new String[fileNames.length];
    for (int i = 0; i < fileNames.length; i++) {
      int pt = fileNames[i].indexOf("::");
      String nameAsGiven = (pt >= 0 ? fileNames[i].substring(pt + 2)
          : fileNames[i]);
      String fileType = (pt >= 0 ? fileNames[i].substring(0, pt) : null);
      String[] names = getClassifiedName(nameAsGiven, true);
      if (names.length == 1)
        return names[0];
      fullPathNames[i] = names[0];
      fileNames[i] = names[0].replace('\\', '/');
      fileTypes[i] = fileType;
      namesAsGiven[i] = nameAsGiven;
    }
    htParams.put("fullPathNames", fullPathNames);
    htParams.put("fileTypes", fileTypes);
    JmolFilesReaderInterface filesReader = newFilesReader(fullPathNames, namesAsGiven,
        fileTypes, null, htParams, isAppend);
    filesReader.run();
    return filesReader.getAtomSetCollection();
  }

  Object createAtomSetCollectionFromString(String strModel,
                                           Map<String, Object> htParams,
                                           boolean isAppend) {
    setLoadState(htParams);
    boolean isAddH = (strModel.indexOf(JC.ADD_HYDROGEN_TITLE) >= 0);
    String[] fnames = (isAddH ? getFileInfo() : null);
    FileReader fileReader = new FileReader(this, vwr, "string", "string", "string", null,
        Rdr.getBR(strModel), htParams, isAppend);
    fileReader.run();
    if (fnames != null)
      setFileInfo(fnames);
    if (!isAppend && !(fileReader.getAtomSetCollection() instanceof String)) {
// zap is unnecessary  - it was done already in FileReader, and it 
// inappropriately clears the PDB chain name map
//      vwr.zap(false, true, false);
      setFileInfo(new String[] { strModel == JC.MODELKIT_ZAP_STRING ? JC.MODELKIT_ZAP_TITLE
          : "string"});
    }
    return fileReader.getAtomSetCollection();
  }

  Object createAtomSeCollectionFromStrings(String[] arrayModels,
                                           SB loadScript,
                                           Map<String, Object> htParams,
                                           boolean isAppend) {
    if (!htParams.containsKey("isData")) {
      String oldSep = "\"" + vwr.getDataSeparator() + "\"";
      String tag = "\"" + (isAppend ? "append" : "model") + " inline\"";
      SB sb = new SB();
      sb.append("set dataSeparator \"~~~next file~~~\";\ndata ").append(tag);
      for (int i = 0; i < arrayModels.length; i++) {
        if (i > 0)
          sb.append("~~~next file~~~");
        sb.append(arrayModels[i]);
      }
      sb.append("end ").append(tag).append(";set dataSeparator ")
          .append(oldSep);
      loadScript.appendSB(sb);
    }
    setLoadState(htParams);
    Logger.info("FileManager.getAtomSetCollectionFromStrings(string[])");
    String[] fullPathNames = new String[arrayModels.length];
    DataReader[] readers = new DataReader[arrayModels.length];
    for (int i = 0; i < arrayModels.length; i++) {
      fullPathNames[i] = "string[" + i + "]";
      readers[i] = newDataReader(vwr, arrayModels[i]);
    }
    JmolFilesReaderInterface filesReader = newFilesReader(fullPathNames, fullPathNames,
        null, readers, htParams, isAppend);
    filesReader.run();
    return filesReader.getAtomSetCollection();
  }

  Object createAtomSeCollectionFromArrayData(Lst<Object> arrayData,
                                             Map<String, Object> htParams,
                                             boolean isAppend) {
    // NO STATE SCRIPT -- HERE WE ARE TRYING TO CONSERVE SPACE
    Logger.info("FileManager.getAtomSetCollectionFromArrayData(Vector)");
    int nModels = arrayData.size();
    String[] fullPathNames = new String[nModels];
    DataReader[] readers = new DataReader[nModels];
    for (int i = 0; i < nModels; i++) {
      fullPathNames[i] = "String[" + i + "]";
      readers[i] = newDataReader(vwr, arrayData.get(i));
    }
    JmolFilesReaderInterface filesReader = newFilesReader(fullPathNames, fullPathNames,
        null, readers, htParams, isAppend);
    filesReader.run();
    return filesReader.getAtomSetCollection();
  }

  static DataReader newDataReader(Viewer vwr, Object data) {
    String reader = (data instanceof String ? "String"
        : AU.isAS(data) ? "Array" 
        : data instanceof Lst<?> ? "List" : null);
    if (reader == null)
      return null;
    DataReader dr = (DataReader) Interface.getInterface("javajs.util." + reader + "DataReader", vwr, "file");
    return dr.setData(data);
  }

  private JmolFilesReaderInterface newFilesReader(String[] fullPathNames,
                                                  String[] namesAsGiven,
                                                  String[] fileTypes,
                                                  DataReader[] readers,
                                                  Map<String, Object> htParams,
                                                  boolean isAppend) {
    JmolFilesReaderInterface fr = (JmolFilesReaderInterface) Interface
        .getOption("io.FilesReader", vwr, "file");
    fr.set(this, vwr, fullPathNames, namesAsGiven, fileTypes, readers, htParams,
        isAppend);
    return fr;
  }

  Object createAtomSetCollectionFromDOM(Object DOMNode,
                                        Map<String, Object> htParams) {
    JmolDomReaderInterface aDOMReader = (JmolDomReaderInterface) Interface.getOption("io.DOMReadaer", vwr, "file");
    aDOMReader.set(this, vwr, DOMNode, htParams);
    aDOMReader.run();
    return aDOMReader.getAtomSetCollection();
  }

  /**
   * not used in Jmol project -- will close reader
   * 
   * @param fullPathName
   * @param name
   * @param reader
   * @param htParams 
   * @return fileData
   */
  Object createAtomSetCollectionFromReader(String fullPathName, String name,
                                           Object reader,
                                           Map<String, Object> htParams) {
    FileReader fileReader = new FileReader(this, vwr, name, fullPathName, name, null,
        reader, htParams, false);
    fileReader.run();
    return fileReader.getAtomSetCollection();
  }

  /////////////// generally useful file I/O methods /////////////////

  // mostly internal to FileManager and its enclosed classes

  BufferedInputStream getBufferedInputStream(String fullPathName) {
    Object ret = getBufferedReaderOrErrorMessageFromName(fullPathName,
        new String[2], true, true);
    return (ret instanceof BufferedInputStream ? (BufferedInputStream) ret
        : null);
  }

  public Object getBufferedInputStreamOrErrorMessageFromName(String name,
                                                             String fullName,
                                                             boolean showMsg,
                                                             boolean checkOnly,
                                                             byte[] outputBytes,
                                                             boolean allowReader,
                                                             boolean allowCached) {
    byte[] cacheBytes = null;
    if (allowCached && outputBytes == null) {
      cacheBytes = (fullName == null || jmb.pngjCache == null ? null
          : jmb.getCachedPngjBytes(fullName));
      if (cacheBytes == null)
        cacheBytes = (byte[]) cacheGet(name, true);
    }
    BufferedInputStream bis = null;
    Object ret = null;
    String errorMessage = null;
    try {
      if (cacheBytes == null) {
        boolean isPngjBinaryPost = (name.indexOf("?POST?_PNGJBIN_") >= 0);
        boolean isPngjPost = (isPngjBinaryPost || name.indexOf("?POST?_PNGJ_") >= 0);
        if (name.indexOf("?POST?_PNG_") > 0 || isPngjPost) {
          String[] errMsg = new String[1];
          byte[] bytes = vwr.getImageAsBytes(isPngjPost ? "PNGJ" : "PNG", 0, 0,
              -1, errMsg);
          if (errMsg[0] != null)
            return errMsg[0];
          if (isPngjBinaryPost) {
            outputBytes = bytes;
            name = PT.rep(name, "?_", "=_");
          } else {
            name = new SB().append(name).append("=")
                .appendSB(Base64.getBase64(bytes)).toString();
          }
        }
        int iurl = OC.urlTypeIndex(name);
        boolean isURL = (iurl >= 0);
        String post = null;
        if (isURL && (iurl = name.indexOf("?POST?")) >= 0) {
          post = name.substring(iurl + 6);
          name = name.substring(0, iurl);
        }
        boolean isApplet = (appletDocumentBaseURL != null);
        if (allowCached && name.indexOf(".png") >= 0 && jmb.pngjCache == null
            && vwr.cachePngFiles())
          jmb.clearAndCachePngjFile(null);
        if (isApplet || isURL) {
          if (isApplet && isURL && appletProxy != null)
            name = appletProxy + "?url=" + urlEncode(name);
          URL url = (isApplet ? new URL(appletDocumentBaseURL, name, null)
              : new URL((URL) null, name, null));
          if (checkOnly)
            return null;
          name = url.toString();
          if (showMsg && name.toLowerCase().indexOf("password") < 0)
            Logger.info("FileManager opening 1 " + name);
          // note that in the case of JS, this is a javajs.util.SB.
          ret = vwr.apiPlatform.getURLContents(url, outputBytes, post, false);
//          if ((ret instanceof SB && ((SB) ret).length() < 3
//                || ret instanceof String && ((String) ret).startsWith("java."))
//              && name.startsWith("http://ves-hx-89.ebi.ac.uk")) {
//            // temporary bypass for EBI firewalled development server
//            // defaulting to current directory and JSON file
//            name = "http://chemapps.stolaf.edu/jmol/jsmol/data/" 
//            + name.substring(name.lastIndexOf("/") + 1) 
//            + (name.indexOf("/val") >= 0 ? ".val" : ".ann") + ".json";
//            ret = getBufferedInputStreamOrErrorMessageFromName(name, fullName,
//                showMsg, checkOnly, outputBytes, allowReader, allowCached);
//          }

          byte[] bytes = null;
          if (ret instanceof SB) {
            SB sb = (SB) ret;
            if (allowReader && !Rdr.isBase64(sb))
              return Rdr.getBR(sb.toString());
            bytes = Rdr.getBytesFromSB(sb);
          } else if (AU.isAB(ret)) {
            bytes = (byte[]) ret;
          }
          if (bytes != null)
            ret = Rdr.getBIS(bytes);
        } else if (!allowCached
            || (cacheBytes = (byte[]) cacheGet(name, true)) == null) {
          if (showMsg)
            Logger.info("FileManager opening 2 " + name);
          ret = vwr.apiPlatform.getBufferedFileInputStream(name);
        }
        if (ret instanceof String)
          return ret;
      }
      bis = (cacheBytes == null ? (BufferedInputStream) ret : Rdr
          .getBIS(cacheBytes));
      if (checkOnly) {
        bis.close();
        bis = null;
      }
      return bis;
    } catch (Exception e) {
      try {
        if (bis != null)
          bis.close();
      } catch (IOException e1) {
      }
      errorMessage = "" + e;
    }
    return errorMessage;
  }
  
  private String urlEncode(String name) {
    try {
      return URLEncoder.encode(name, "utf-8");
    } catch (UnsupportedEncodingException e) {
      return name;
    }
  }

  public String getEmbeddedFileState(String fileName, boolean allowCached) {
    String[] dir = null;
    dir = getZipDirectory(fileName, false, allowCached);
    if (dir.length == 0) {
      String state = vwr.getFileAsString4(fileName, -1, false, true, false, "file");
      return (state.indexOf(JC.EMBEDDED_SCRIPT_TAG) < 0 ? ""
          : JmolBinary.getEmbeddedScript(state));
    }
    for (int i = 0; i < dir.length; i++)
      if (dir[i].indexOf(".spt") >= 0) {
        String[] data = new String[] { fileName + "|" + dir[i], null };
        getFileDataAsString(data, -1, false, false, false);
        return data[1];
      }
    return "";
  }

  /**
   * just check for a file as being readable. Do not go into a zip file
   * 
   * @param filename
   * @param getStream 
   * @param ret 
   * @return String[2] where [0] is fullpathname and [1] is error message or null
   */
  Object getFullPathNameOrError(String filename, boolean getStream, String[] ret) {
    String[] names = getClassifiedName(filename, true);
    if (names == null || names[0] == null || names.length < 2)
      return new String[] { null, "cannot read file name: " + filename };
    String name = names[0];
    String fullPath = names[0].replace('\\', '/');
    name = Rdr.getZipRoot(name);
    Object errMsg = getBufferedInputStreamOrErrorMessageFromName(name, fullPath, false, !getStream, null, false, !getStream);
    ret[0] = fullPath;
    if (errMsg instanceof String)
      ret[1] = (String) errMsg;
    return errMsg;
  }

  public Object getBufferedReaderOrErrorMessageFromName(String name,
                                                 String[] fullPathNameReturn,
                                                 boolean isBinary,
                                                 boolean doSpecialLoad) {
    Object data = cacheGet(name, false);
    boolean isBytes = AU.isAB(data);
    byte[] bytes = (isBytes ? (byte[]) data : null);
    if (name.startsWith("cache://")) {
      if (data == null)
        return "cannot read " + name;
      if (isBytes) {
        bytes = (byte[]) data;
      } else {
        return Rdr.getBR((String) data);
      }
    }
    String[] names = getClassifiedName(name, true);
    if (names == null)
      return "cannot read file name: " + name;
    if (fullPathNameReturn != null)
      fullPathNameReturn[0] = names[0].replace('\\', '/');
    return getUnzippedReaderOrStreamFromName(names[0], bytes,
        false, isBinary, false, doSpecialLoad, null);
  }

  public Object getUnzippedReaderOrStreamFromName(String name, byte[] bytes,
                                                  boolean allowZipStream,
                                                  boolean forceInputStream,
                                                  boolean isTypeCheckOnly,
                                                  boolean doSpecialLoad,
                                                  Map<String, Object> htParams) {
    String[] subFileList = null;
    String[] info = (bytes == null && doSpecialLoad ? getSpartanFileList(name)
        : null);
    String name00 = name;
    if (info != null) {
      if (isTypeCheckOnly)
        return info;
      if (info[2] != null) {
        String header = info[1];
        Map<String, String> fileData = new Hashtable<String, String>();
        if (info.length == 3) {
          // we need information from the output file, info[2]
          String name0 = getObjectAsSections(info[2], header, fileData);
          fileData.put("OUTPUT", name0);
          info = jmb.spartanFileList(name, fileData.get(name0));
          if (info.length == 3) {
            // might have a second option
            name0 = getObjectAsSections(info[2], header, fileData);
            fileData.put("OUTPUT", name0);
            info = jmb.spartanFileList(info[1], fileData.get(name0));
          }
        }
        // load each file individually, but return files IN ORDER
        SB sb = new SB();
        if (fileData.get("OUTPUT") != null)
          sb.append(fileData.get(fileData.get("OUTPUT")));
        String s;
        for (int i = 2; i < info.length; i++) {
          name = info[i];
          name = getObjectAsSections(name, header, fileData);
          Logger.info("reading " + name);
          s = fileData.get(name);
          sb.append(s);
        }
        s = sb.toString();
        jmb.spardirPut(name00.replace('\\', '/'), s.getBytes());
        return Rdr.getBR(s);
      }
      // continuing...
      // here, for example, for an SPT file load that is not just a type check
      // (type check is only for application file opening and drag-drop to
      // determine if
      // script or load command should be used)
    }

    if (bytes == null && jmb.pngjCache != null) {
      bytes = jmb.getCachedPngjBytes(name);
      if (bytes != null && htParams != null)
        htParams.put("sourcePNGJ", Boolean.TRUE);
    }
    String fullName = name;
    if (name.indexOf("|") >= 0) {
      subFileList = PT.split(name.replace('\\', '/'), "|");
      if (bytes == null)
        Logger.info("FileManager opening 3 " + name);
      name = subFileList[0];
    }
    Object t = (bytes == null ? getBufferedInputStreamOrErrorMessageFromName(
        name, fullName, true, false, null, !forceInputStream, true)
        : Rdr.getBIS(bytes));
    try {
      if (t instanceof String)
        return t;
      if (t instanceof BufferedReader)
        return t;
      BufferedInputStream bis = (BufferedInputStream) t;
      if (Rdr.isGzipS(bis))
        bis = Rdr.getUnzippedInputStream(vwr.getJzt(), bis);
      if (Rdr.isCompoundDocumentS(bis)) {
        GenericBinaryDocument doc = (GenericBinaryDocument) Interface
            .getInterface("javajs.util.CompoundDocument", vwr, "file");
        doc.setStream(vwr.getJzt(), bis, true);
        return Rdr.getBR(doc.getAllDataFiles(
            "Molecule", "Input").toString());
      }
      if (Rdr.isPickleS(bis))
        return bis;
      bis = Rdr.getPngZipStream(bis, true);
      if (Rdr.isZipS(bis)) {
        if (allowZipStream)
          return Rdr.newZipInputStream(vwr.getJzt(), bis);
        Object o = Rdr.getZipFileDirectory(vwr.getJzt(), bis, subFileList, 1, forceInputStream);
        return (o instanceof String ? Rdr.getBR((String) o) : o);
      }
      return (forceInputStream ? bis : Rdr.getBufferedReader(bis, null));
    } catch (Exception ioe) {
      return ioe.toString();
    }
  }

  private String[] getSpartanFileList(String name) {
      // check for .spt file type -- Jmol script
      if (name.endsWith(".spt"))
        return new String[] { null, null, null }; // DO NOT actually load any file
      // check for zipped up spardir -- we'll automatically take first file there
      if (name.endsWith(".spardir.zip"))
        return new String[] { "SpartanSmol", "Directory Entry ", name + "|output"};
      name = name.replace('\\', '/');
      if (!name.endsWith(".spardir") && name.indexOf(".spardir/") < 0)
        return null; 
      // look for .spardir or .spardir/...
      int pt = name.lastIndexOf(".spardir");
      if (pt < 0)
        return null;
      if (name.lastIndexOf("/") > pt) {
        // a single file in the spardir directory is requested
        return new String[] { "SpartanSmol", "Directory Entry ",
            name + "/input", name + "/archive",
            name + "/Molecule:asBinaryString", name + "/proparc" };      
      }
      return new String[] { "SpartanSmol", "Directory Entry ", name + "/output" };
  }

  /**
   * delivers file contents and directory listing for a ZIP/JAR file into sb
   * 
   * @param name
   * @param header
   * @param fileData
   * @return name of entry
   */
  private String getObjectAsSections(String name, String header,
                                     Map<String, String> fileData) {
    if (name == null)
      return null;
    String[] subFileList = null;
    boolean asBinaryString = false;
    String name0 = name.replace('\\', '/');
    if (name.indexOf(":asBinaryString") >= 0) {
      asBinaryString = true;
      name = name.substring(0, name.indexOf(":asBinaryString"));
    }
    SB sb = null;
    if (fileData.containsKey(name0))
      return name0;
    if (name.indexOf("#JMOL_MODEL ") >= 0) {
      fileData.put(name0, name0 + "\n");
      return name0;
    }
    String fullName = name;
    if (name.indexOf("|") >= 0) {
      subFileList = PT.split(name, "|");
      name = subFileList[0];
    }
    BufferedInputStream bis = null;
    try {
      Object t = getBufferedInputStreamOrErrorMessageFromName(name, fullName,
          false, false, null, false, true);
      if (t instanceof String) {
        fileData.put(name0, (String) t + "\n");
        return name0;
      }
      bis = (BufferedInputStream) t;
      if (Rdr.isCompoundDocumentS(bis)) {
        GenericBinaryDocument doc = (GenericBinaryDocument) Interface
            .getInterface("javajs.util.CompoundDocument", vwr, "file");
        doc.setStream(vwr.getJzt(), bis, true);
        doc.getAllDataMapped(name.replace('\\', '/'), "Molecule", fileData);
      } else if (Rdr.isZipS(bis)) {
        Rdr.getAllZipData(vwr.getJzt(), bis, subFileList, name.replace('\\', '/'), "Molecule",
            fileData);
      } else if (asBinaryString) {
        // used for Spartan binary file reading
        GenericBinaryDocument bd = (GenericBinaryDocument) Interface
            .getInterface("javajs.util.BinaryDocument", vwr, "file");
        bd.setStream(vwr.getJzt(), bis, false);
        sb = new SB();
        //note -- these headers must match those in ZipUtil.getAllData and CompoundDocument.getAllData
        if (header != null)
          sb.append("BEGIN Directory Entry " + name0 + "\n");
        try {
          while (true)
            sb.append(Integer.toHexString(bd.readByte() & 0xFF)).appendC(' ');
        } catch (Exception e1) {
          sb.appendC('\n');
        }
        if (header != null)
          sb.append("\nEND Directory Entry " + name0 + "\n");
        fileData.put(name0, sb.toString());
      } else {
        BufferedReader br = Rdr.getBufferedReader(
            Rdr.isGzipS(bis) ? new BufferedInputStream(Rdr.newGZIPInputStream(vwr.getJzt(), bis)) : bis, null);
        String line;
        sb = new SB();
        if (header != null)
          sb.append("BEGIN Directory Entry " + name0 + "\n");
        while ((line = br.readLine()) != null) {
          sb.append(line);
          sb.appendC('\n');
        }
        br.close();
        if (header != null)
          sb.append("\nEND Directory Entry " + name0 + "\n");
        fileData.put(name0, sb.toString());
      }
    } catch (Exception ioe) {
      fileData.put(name0, ioe.toString());
    }
    if (bis != null)
      try {
        bis.close();
      } catch (Exception e) {
        //
      }
    if (!fileData.containsKey(name0))
      fileData.put(name0, "FILE NOT FOUND: " + name0 + "\n");
    return name0;
  }

  /**
   * 
   * @param fileName
   * @param addManifest
   * @param allowCached 
   * @return [] if not a zip file;
   */
  public String[] getZipDirectory(String fileName, boolean addManifest, boolean allowCached) {
    Object t = getBufferedInputStreamOrErrorMessageFromName(fileName, fileName,
        false, false, null, false, allowCached);
    return Rdr.getZipDirectoryAndClose(vwr.getJzt(), (BufferedInputStream) t, addManifest ? "JmolManifest" : null);
  }

  public Object getFileAsBytes(String name, OC out) {
    // ?? used by eval of "WRITE FILE"
    // will be full path name
    if (name == null)
      return null;
    String fullName = name;
    String[] subFileList = null;
    if (name.indexOf("|") >= 0) {
      subFileList = PT.split(name, "|");
      name = subFileList[0];
    }
    Object t = getBufferedInputStreamOrErrorMessageFromName(name, fullName,
        false, false, null, false, true);
    if (t instanceof String)
      return "Error:" + t;
    try {
      BufferedInputStream bis = (BufferedInputStream) t;
      Object bytes = (out != null 
          || subFileList == null
          || subFileList.length <= 1 
          || !Rdr.isZipS(bis) && !Rdr.isPngZipStream(bis) 
              ? Rdr.getStreamAsBytes(bis,out) 
          : Rdr.getZipFileContentsAsBytes(vwr.getJzt(), bis, subFileList, 1));
      bis.close();
      return bytes;
    } catch (Exception ioe) {
      return ioe.toString();
    }
  }


  public Map<String, Object> getFileAsMap(String name) {
    Map<String, Object> bdata = new Hashtable<String, Object>();
    Object t;
    if (name == null) {
      String[] errMsg = new String[1];
      byte[] bytes = vwr.getImageAsBytes("PNGJ", -1, -1, -1, errMsg);
      if (errMsg[0] != null) {
        bdata.put("_ERROR_", errMsg[0]);
        return bdata;
      }
      t = Rdr.getBIS(bytes);
    } else {
      String[] data = new String[2];
      t = getFullPathNameOrError(name, true, data);
      if (t instanceof String) {
        bdata.put("_ERROR_", t);
        return bdata;
      }
      if (!checkSecurity(data[0])) {
        bdata.put("_ERROR_", "java.io. Security exception: cannot read file "
            + data[0]);
        return bdata;
      }
    }
    try {
      Rdr.readFileAsMap(vwr.getJzt(), (BufferedInputStream) t, bdata, name);
      
    } catch (Exception e) {
      bdata.clear();
      bdata.put("_ERROR_", "" + e);
    }
    return bdata;
  }

  /**
   * 
   * @param data
   *        [0] initially path name, but returned as full path name; [1]file
   *        contents (directory listing for a ZIP/JAR file) or error string
   * @param nBytesMax
   *        or -1
   * @param doSpecialLoad
   * @param allowBinary
   * @param checkProtected
   *        TODO
   * @return true if successful; false on error
   */

  public boolean getFileDataAsString(String[] data, int nBytesMax,
                                     boolean doSpecialLoad,
                                     boolean allowBinary, boolean checkProtected) {
    data[1] = "";
    String name = data[0];
    if (name == null)
      return false;
    Object t = getBufferedReaderOrErrorMessageFromName(name, data, false,
        doSpecialLoad);
    if (t instanceof String) {
      data[1] = (String) t;
      return false;
    }
    if (checkProtected && !checkSecurity(data[0])) {
      data[1] = "java.io. Security exception: cannot read file " + data[0];
      return false;
    }
    try {
      return Rdr.readAllAsString((BufferedReader) t, nBytesMax, allowBinary,
          data, 1);
    } catch (Exception e) {
      return false;
    }
  }

  private boolean checkSecurity(String f) {
    // when load() function and local file: 
    if (!f.startsWith("file:"))
       return true;
    int pt = f.lastIndexOf('/');
    // root directory C:/foo or file:///c:/foo or "/foo"
    // no hidden files 
    // no files without extension
    if (f.lastIndexOf(":/") == pt - 1 
      || f.indexOf("/.") >= 0 
      || f.lastIndexOf('.') < f.lastIndexOf('/'))
      return false;
    return true;
  }

  @SuppressWarnings("unchecked")
  public void loadImage(Object nameOrBytes, String echoName) {
    Object image = null;
    String nameOrError = null;
    byte[] bytes = null;
    boolean isShowImage = (echoName != null && echoName.startsWith("\1"));
    if (isShowImage) {
      if (echoName.equals("\1closeall\1null")) {
        vwr.loadImageData(Boolean.TRUE, "\1closeall", "\1closeall", null);
        return;
      }
      if ("\1close".equals(nameOrBytes)) {
        vwr.loadImageData(Boolean.FALSE, "\1close", echoName, null);
        return;
      }
    }
    if (nameOrBytes instanceof Map) {
      nameOrBytes = (((Map<String, Object>) nameOrBytes).containsKey("_DATA_") ? ((Map<String, Object>) nameOrBytes)
          .get("_DATA_") : ((Map<String, Object>) nameOrBytes).get("_IMAGE_"));
    }
    if (nameOrBytes instanceof SV)
      nameOrBytes = ((SV) nameOrBytes).value;
    String name = (nameOrBytes instanceof String ? (String) nameOrBytes : null);
    if (name != null && name.startsWith(";base64,")) {
      bytes = Base64.decodeBase64(name);
    } else if (nameOrBytes instanceof BArray) {
      bytes = ((BArray) nameOrBytes).data;
    } else if (echoName == null || nameOrBytes instanceof String) {
      String[] names = getClassifiedName((String) nameOrBytes, true);
      nameOrError = (names == null ? "cannot read file name: " + nameOrBytes
          : names[0].replace('\\', '/'));
      if (names != null)
        image = jmb.getImage(vwr, nameOrError, echoName);
    } else {
      image = nameOrBytes;
    }
    if (bytes != null)
      image = jmb.getImage(vwr, bytes, echoName);
    if (image instanceof String) {
      nameOrError = (String) image;
      image = null;
    }
    if (!vwr.isJS && image != null && bytes != null)
      nameOrError = ";base64," + Base64.getBase64(bytes).toString();
    if (!vwr.isJS || isShowImage && nameOrError == null)
      vwr.loadImageData(image, nameOrError, echoName, null);
    // JSmol will call that from awtjs2d.Platform.java asynchronously
  }

  /**
   * [0] and [2] may return same as [1] in the 
   * case of a local unsigned applet.
   * 
   * @param name
   * @param isFullLoad
   *        false only when just checking path
   * @return [0] full path name, [1] file name without path, [2] full URL
   */
  private String[] getClassifiedName(String name, boolean isFullLoad) {
    if (name == null)
      return new String[] { null };
    boolean doSetPathForAllFiles = (pathForAllFiles.length() > 0);
    if (name.startsWith("?") || name.startsWith("http://?")) {
      if (!vwr.isJS && (name = vwr.dialogAsk("Load", name, null)) == null)
        return new String[] { isFullLoad ? "#CANCELED#" : null };
      doSetPathForAllFiles = false;
    }
    GenericFileInterface file = null;
    URL url = null;
    String[] names = null;
    if (name.startsWith("cache://")) {
      names = new String[3];
      names[0] = names[2] = name;
      names[1] = stripPath(names[0]);
      return names;
    }
    name = vwr.resolveDatabaseFormat(name);
    if (name.indexOf(":") < 0 && name.indexOf("/") != 0)
      name = addDirectory(vwr.getDefaultDirectory(), name);
    if (appletDocumentBaseURL == null) {
      // This code is for the app or signed local applet 
      // -- no local file reading for headless
      if (OC.urlTypeIndex(name) >= 0 || vwr.haveAccess(ACCESS.NONE)
          || vwr.haveAccess(ACCESS.READSPT) && !name.endsWith(".spt")
          && !name.endsWith("/")) {
        try {
          url = new URL((URL) null, name, null);
        } catch (MalformedURLException e) {
          return new String[] { isFullLoad ? e.toString() : null };
        }
      } else {
        file = vwr.apiPlatform.newFile(name);
        String s = file.getFullPath();
        // local unsigned applet may have access control issue here and get a null return
        String fname = file.getName();
        names = new String[] { (s == null ? fname : s), fname,
            (s == null ? fname : "file:/" + s.replace('\\', '/')) };
      }
    } else {
      // This code is only for the non-local applet
      try {
        if (name.indexOf(":\\") == 1 || name.indexOf(":/") == 1)
          name = "file:/" + name;
        //        else if (name.indexOf("/") == 0 && vwr.isSignedApplet())
        //        name = "file:" + name;
        url = new URL(appletDocumentBaseURL, name, null);
      } catch (MalformedURLException e) {
        return new String[] { isFullLoad ? e.toString() : null };
      }
    }
    if (url != null) {
      names = new String[3];
      names[0] = names[2] = url.toString();
      names[1] = stripPath(names[0]);
    }
    if (doSetPathForAllFiles) {
      String name0 = names[0];
      names[0] = pathForAllFiles + names[1];
      Logger.info("FileManager substituting " + name0 + " --> " + names[0]);
    }
    if (isFullLoad && (file != null || OC.urlTypeIndex(names[0]) == OC.URL_LOCAL)) {
      String path = (file == null ? PT.trim(names[0].substring(5), "/")
          : names[0]);
      int pt = path.length() - names[1].length() - 1;
      if (pt > 0) {
        path = path.substring(0, pt);
        setLocalPath(vwr, path, true);
      }
    }
    return names;
  }

  private static String addDirectory(String defaultDirectory, String name) {
    if (defaultDirectory.length() == 0)
      return name;
    char ch = (name.length() > 0 ? name.charAt(0) : ' ');
    String s = defaultDirectory.toLowerCase();
    if ((s.endsWith(".zip") || s.endsWith(".tar")) && ch != '|' && ch != '/')
      defaultDirectory += "|";
    return defaultDirectory
        + (ch == '/'
            || ch == '/'
            || (ch = defaultDirectory.charAt(defaultDirectory.length() - 1)) == '|'
            || ch == '/' ? "" : "/") + name;
  }

  String getDefaultDirectory(String name) {
    String[] names = getClassifiedName(name, true);
    if (names == null)
      return "";
    name = fixPath(names[0]);
    return (name == null ? "" : name.substring(0, name.lastIndexOf("/")));
  }

  private static String fixPath(String path) {
    path = path.replace('\\', '/');
    path = PT.rep(path, "/./", "/");
    int pt = path.lastIndexOf("//") + 1;
    if (pt < 1)
      pt = path.indexOf(":/") + 1;
    if (pt < 1)
      pt = path.indexOf("/");
    if (pt < 0)
      return null;
    String protocol = path.substring(0, pt);
    path = path.substring(pt);

    while ((pt = path.lastIndexOf("/../")) >= 0) {
      int pt0 = path.substring(0, pt).lastIndexOf("/");
      if (pt0 < 0)
        return PT.rep(protocol + path, "/../", "/");
      path = path.substring(0, pt0) + path.substring(pt + 3);
    }
    if (path.length() == 0)
      path = "/";
    return protocol + path;
  }

  public String getFilePath(String name, boolean addUrlPrefix,
                            boolean asShortName) {
    String[] names = getClassifiedName(name, false);
    return (names == null || names.length == 1 ? "" : asShortName ? names[1]
        : addUrlPrefix ? names[2] 
        : names[0] == null ? ""
        : names[0].replace('\\', '/'));
  }

  public static GenericFileInterface getLocalDirectory(Viewer vwr, boolean forDialog) {
    String localDir = (String) vwr
        .getP(forDialog ? "currentLocalPath" : "defaultDirectoryLocal");
    if (forDialog && localDir.length() == 0)
      localDir = (String) vwr.getP("defaultDirectoryLocal");
    if (localDir.length() == 0)
      return (vwr.isApplet ? null : vwr.apiPlatform.newFile(System
          .getProperty("user.dir", ".")));
    if (vwr.isApplet && localDir.indexOf("file:/") == 0)
      localDir = localDir.substring(6);
    GenericFileInterface f = vwr.apiPlatform.newFile(localDir);
    try {
      return f.isDirectory() ? f : f.getParentAsFile();
    } catch (Exception e) {
      return  null;
    }
  }

  /**
   * called by getImageFileNameFromDialog 
   * called by getOpenFileNameFromDialog
   * called by getSaveFileNameFromDialog
   * 
   * called by classifyName for any full file load
   * called from the CD command
   * 
   * currentLocalPath is set in all cases
   *   and is used specifically for dialogs as a first try
   * defaultDirectoryLocal is set only when not from a dialog
   *   and is used only in getLocalPathForWritingFile or
   *   from an open/save dialog.
   * In this way, saving a file from a dialog doesn't change
   *   the "CD" directory. 
   * Neither of these is saved in the state, but 
   * 
   * 
   * @param vwr
   * @param path
   * @param forDialog
   */
  public static void setLocalPath(Viewer vwr, String path,
                                  boolean forDialog) {
    while (path.endsWith("/") || path.endsWith("\\"))
      path = path.substring(0, path.length() - 1);
    vwr.setStringProperty("currentLocalPath", path);
    if (!forDialog)
      vwr.setStringProperty("defaultDirectoryLocal", path);
  }

  public static String getLocalPathForWritingFile(Viewer vwr, String file) {
    if (file.startsWith("http://"))
      return file;
    file = PT.rep(file, "?", "");
    if (file.indexOf("file:/") == 0)
      return file.substring(6);
    if (file.indexOf("/") == 0 || file.indexOf(":") >= 0)
      return file;
    GenericFileInterface dir = null;
    try {
      dir = getLocalDirectory(vwr, false);
    } catch (Exception e) {
      // access control for unsigned applet
    }
    return (dir == null ? file : fixPath(dir.toString() + "/" + file));
  }

  public static String setScriptFileReferences(String script, String localPath,
                                               String remotePath,
                                               String scriptPath) {
    if (localPath != null)
      script = setScriptFileRefs(script, localPath, true);
    if (remotePath != null)
      script = setScriptFileRefs(script, remotePath, false);
    script = PT.rep(script, "\1\"", "\"");
    if (scriptPath != null) {
      while (scriptPath.endsWith("/"))
        scriptPath = scriptPath.substring(0, scriptPath.length() - 1);
      for (int ipt = 0; ipt < scriptFilePrefixes.length; ipt++) {
        String tag = scriptFilePrefixes[ipt];
        script = PT.rep(script, tag + ".", tag + scriptPath);
      }
    }
    return script;
  }

  /**
   * Sets all local file references in a script file to point to files within
   * dataPath. If a file reference contains dataPath, then the file reference is
   * left with that RELATIVE path. Otherwise, it is changed to a relative file
   * name within that dataPath. 
   * 
   * Only file references starting with "file://" are changed.
   * 
   * @param script
   * @param dataPath
   * @param isLocal 
   * @return revised script
   */
  private static String setScriptFileRefs(String script, String dataPath,
                                                boolean isLocal) {
    if (dataPath == null)
      return script;
    boolean noPath = (dataPath.length() == 0);
    Lst<String> fileNames = new  Lst<String>();
    JmolBinary.getFileReferences(script, fileNames);
    Lst<String> oldFileNames = new  Lst<String>();
    Lst<String> newFileNames = new  Lst<String>();
    int nFiles = fileNames.size();
    for (int iFile = 0; iFile < nFiles; iFile++) {
      String name0 = fileNames.get(iFile);
      String name = name0;
      if (isLocal == OC.isLocal(name)) {
        int pt = (noPath ? -1 : name.indexOf("/" + dataPath + "/"));
        if (pt >= 0) {
          name = name.substring(pt + 1);
        } else {
          pt = name.lastIndexOf("/");
          if (pt < 0 && !noPath)
            name = "/" + name;
          if (pt < 0 || noPath)
            pt++;
          name = dataPath + name.substring(pt);
        }
      }
      Logger.info("FileManager substituting " + name0 + " --> " + name);
      oldFileNames.addLast("\"" + name0 + "\"");
      newFileNames.addLast("\1\"" + name + "\"");
    }
    return PT.replaceStrings(script, oldFileNames, newFileNames);
  }

  public static String[] scriptFilePrefixes = new String[] { "/*file*/\"",
      "FILE0=\"", "FILE1=\"" };

  public static String stripPath(String name) {
    int pt = Math.max(name.lastIndexOf("|"), name.lastIndexOf("/"));
    return name.substring(pt + 1);
  }

  public static String fixFileNameVariables(String format, String fname) {
    String str = PT.rep(format, "%FILE", fname);
    if (str.indexOf("%LC") < 0)
      return str;
    fname = fname.toLowerCase();
    str = PT.rep(str, "%LCFILE", fname);
    if (fname.length() == 4)
      str = PT.rep(str, "%LC13", fname.substring(1, 3));
    return str;
  }

  private Map<String, Object> cache = new Hashtable<String, Object>();

  void cachePut(String key, Object data) {
    key = key.replace('\\', '/');
    if (Logger.debugging)
      Logger.debug("cachePut " + key);
    if (data == null || "".equals(data)) { // J2S error -- cannot implement Int32Array.equals 
      cache.remove(key);
      return;
    }
    cache.put(key, data);
    jmb.getCachedPngjBytes(key);
  }
  
  public Object cacheGet(String key, boolean bytesOnly) {
    key = key.replace('\\', '/');
    // in the case of JavaScript local file reader, 
    // this will be a cached file, and the filename will not be known.
    int pt = key.indexOf("|");
    if (pt >= 0)
      key = key.substring(0, pt);
    key = getFilePath(key, true, false);
    Object data = null;
    /**
     * @j2sNative
     * 
     * (data = Jmol.Cache.get(key)) || (data = this.cache.get(key));
     * 
     */
    {
    //if (Logger.debugging)
      //Logger.debug
      Logger.info("cacheGet " + key + " " + cache.containsKey(key));
       data = cache.get(key);
    }    
    return (bytesOnly && (data instanceof String) ? null : data);
  }

  void cacheClear() {
    Logger.info("cache cleared");
    cache.clear();
    clearPngjCache(null);
  }

  public int cacheFileByNameAdd(String fileName, boolean isAdd) {
    if (fileName == null || !isAdd && fileName.equalsIgnoreCase("")) {
      cacheClear();
      return -1;
    }
    Object data;
    if (isAdd) {
      fileName = vwr.resolveDatabaseFormat(fileName);
      data = getFileAsBytes(fileName, null);
      if (data instanceof String)
        return 0;
      cachePut(fileName, data);
    } else {
      if (fileName.endsWith("*"))
        return AU.removeMapKeys(cache, fileName.substring(0, fileName.length() - 1));
      data = cache.remove(fileName.replace('\\', '/'));
    }
    return (data == null ? 0 : data instanceof String ? ((String) data).length()
        : ((byte[]) data).length);
  }

  public Map<String, Integer> cacheList() {
    Map<String, Integer> map = new Hashtable<String, Integer>();
    for (Map.Entry<String, Object> entry : cache.entrySet())
      map.put(entry.getKey(), Integer
          .valueOf(AU.isAB(entry.getValue()) ? ((byte[]) entry
              .getValue()).length : entry.getValue().toString().length()));
    return map;
  }

  public String getCanonicalName(String pathName) {
    String[] names = getClassifiedName(pathName, true);
    return (names == null ? pathName : names[2]);
  }

  @Override
  public String postByteArray(String fileName, byte[] bytes) {
    // in principle, could have sftp or ftp here
    // but sftp is not implemented
    Object ret = getBufferedInputStreamOrErrorMessageFromName(fileName, null, false,
            false, bytes, false, true);
    if (ret instanceof String)
      return (String) ret;
    try {
      ret = Rdr.getStreamAsBytes((BufferedInputStream) ret, null);
    } catch (IOException e) {
      try {
        ((BufferedInputStream) ret).close();
      } catch (IOException e1) {
        // ignore
      }
    }
    return (ret == null ? "" : Rdr.fixUTF((byte[]) ret));
  }

}
