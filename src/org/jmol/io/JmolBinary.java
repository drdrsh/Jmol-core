/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-05 09:07:28 -0500 (Thu, 05 Apr 2007) $
 * $Revision: 7326 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
package org.jmol.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolZipUtilities;

import javajs.util.Rdr;
import javajs.util.Lst;
import javajs.util.PT;

import org.jmol.util.Logger;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;


public class JmolBinary {

  public FileManager fm;

  public Map<String, Object> pngjCache;
  public Map<String, byte[]> spardirCache;

  public JmolBinary(FileManager fm) {
    this.fm = fm;
  }
 
  public static final String JPEG_CONTINUE_STRING = " #Jmol...\0";
  
  public final static String PMESH_BINARY_MAGIC_NUMBER = "PM\1\0";

  public static String determineSurfaceTypeIs(InputStream is) {
    // drag-drop only
    BufferedReader br;
    try {
      br = Rdr.getBufferedReader(new BufferedInputStream(is), "ISO-8859-1");
    } catch (IOException e) {
      return null;
    }
    return determineSurfaceFileType(br);
  }
  
  public static String getEmbeddedScript(String script) {
    if (script == null)
      return script;
    int pt = script.indexOf(JC.EMBEDDED_SCRIPT_TAG);
    if (pt < 0)
      return script;
    int pt1 = script.lastIndexOf("/*", pt);
    int pt2 = script.indexOf((script.charAt(pt1 + 2) == '*' ? "*" : "") + "*/",
        pt);
    if (pt1 >= 0 && pt2 >= pt)
      script = script.substring(
          pt + JC.EMBEDDED_SCRIPT_TAG.length(), pt2)
          + "\n";
    while ((pt1 = script.indexOf(JPEG_CONTINUE_STRING)) >= 0)
      script = script.substring(0, pt1)
          + script.substring(pt1 + JPEG_CONTINUE_STRING.length() + 4);
    if (Logger.debugging)
      Logger.debug(script);
    return script;
  }

  static JmolZipUtilities jzu;
  
  static JmolZipUtilities getJzu() {
    return (jzu == null ? jzu = (JmolZipUtilities) Interface.getOption("io.JmolUtil") : jzu);
  }

  public byte[] getCachedPngjBytes(String pathName) {
    return (pathName.indexOf(".png") < 0 ? null : getJzu().getCachedPngjBytes(this, pathName));
  }

  public boolean clearAndCachePngjFile(String[] data) {
      pngjCache = new Hashtable<String, Object>();
      return (data == null || data[0] == null ? false : getJzu().cachePngjFile(this, data));
  }
  
  public void spardirPut(String name, byte[] bytes) {
    if (spardirCache == null)
      spardirCache = new Hashtable<String, byte[]>();
    spardirCache.put(name, bytes);
  }
  
  public void clearPngjCache(String fileName) {
    if (pngjCache == null)
      return;
    if (fileName != null && !pngjCache.containsKey(fileName))
      return;
    pngjCache = null;
    Logger.info("PNGJ cache cleared");
  }

  /**
   * A rather complicated means of reading a ZIP file, which could be a 
   * single file, or it could be a manifest-organized file, or it could be
   * a Spartan directory.
   * 
   * @param adapter 
   * 
   * @param is 
   * @param fileName 
   * @param zipDirectory 
   * @param htParams 
   * @param asBufferedReader 
   * @return a single atomSetCollection
   * 
   */
  public static Object getAtomSetCollectionOrBufferedReaderFromZip(JmolAdapter adapter, InputStream is, String fileName, String[] zipDirectory,
                             Map<String, Object> htParams, boolean asBufferedReader) {
    return getJzu().getAtomSetCollectionOrBufferedReaderFromZip(Rdr.getJzt(), adapter, is, fileName, zipDirectory, htParams, 1, asBufferedReader);
  }

  public static String[] spartanFileList(String name, String zipDirectory) {
    return getJzu().spartanFileList(Rdr.getJzt(), name, zipDirectory);
  }

  public static String determineSurfaceFileType(BufferedReader br) {
    return getJzu().determineSurfaceFileType(br);
  }

  public Object getImage(Viewer vwr, Object fullPathNameOrBytes, String echoName) {
    return getJzu().getImage(vwr, fullPathNameOrBytes, echoName);
  }

  public static void getFileReferences(String script, Lst<String> fileList) {
    for (int ipt = 0; ipt < FileManager.scriptFilePrefixes.length; ipt++) {
      String tag = FileManager.scriptFilePrefixes[ipt];
      int i = -1;
      while ((i = script.indexOf(tag, i + 1)) >= 0) {
        String s = PT.getQuotedStringAt(script, i);
        if (s.indexOf("::") >= 0)
          s = PT.split(s, "::")[1];
        fileList.addLast(s);
      }
    }
  }

  /**
   * check a JmolManifest for a reference to a script file (.spt)
   * 
   * @param manifest
   * @return null, "", or a directory entry in the ZIP file
   */

  public static String getManifestScriptPath(String manifest) {
    if (manifest.indexOf("$SCRIPT_PATH$") >= 0)
      return "";
    String ch = (manifest.indexOf('\n') >= 0 ? "\n" : "\r");
    if (manifest.indexOf(".spt") >= 0) {
      String[] s = PT.split(manifest, ch);
      for (int i = s.length; --i >= 0;)
        if (s[i].indexOf(".spt") >= 0)
          return "|" + PT.trim(s[i], "\r\n \t");
    }
    return null;
  }

  public static BufferedReader getBufferedReaderForResource(Viewer vwr, 
                                                            Object resourceClass,
                                              String classPath,
                                              String resourceName)
       throws IOException {

     /**
      * @j2sNative
      * 
      *            resourceName = vwr.vwrOptions.get("codePath") +
      *            classPath + resourceName;
      * 
      */
     {
       URL url = resourceClass.getClass().getResource(resourceName);
       boolean ret = true;
       if (url == null) {
         System.err.println("Couldn't find file: " + classPath + resourceName);
         throw new IOException();
       }
       if (ret) // avoids dead code message
         return Rdr.getBufferedReader(new BufferedInputStream(
             (InputStream) url.getContent()), null);
     }
     // JavaScript only; here and not in JavaDoc to preserve Eclipse search reference
     return (BufferedReader) vwr.getBufferedReaderOrErrorMessageFromName(
         resourceName, new String[] { null, null }, false);
   }


}

