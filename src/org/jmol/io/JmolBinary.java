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
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;

import javajs.api.GenericBinaryDocument;
import javajs.util.PT;
import javajs.util.Rdr;
import javajs.util.SB;

import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolZipUtilities;
import org.jmol.util.Logger;
import org.jmol.viewer.FileManager;


public class JmolBinary {

  public FileManager fm;

  public JmolBinary() {
    // for reflection
  }
  
  public JmolBinary set(FileManager fm) {
    this.fm = fm;
    return this;
  }
 
  JmolZipUtilities jzu;
  
  private JmolZipUtilities getJzu() {
    return (jzu == null ? jzu = (JmolZipUtilities) Interface.getOption("io.JmolUtil", fm.vwr, "file") : jzu);
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
  public Object getAtomSetCollectionOrBufferedReaderFromZip(JmolAdapter adapter, InputStream is, String fileName, String[] zipDirectory,
                             Map<String, Object> htParams, boolean asBufferedReader) {
    return getJzu().getAtomSetCollectionOrBufferedReaderFromZip(fm.vwr, adapter, is, fileName, zipDirectory, htParams, 1, asBufferedReader);
  }

  public Object getImage(Object fullPathNameOrBytes, String echoName, boolean forceSync) {
    return getJzu().getImage(fm.vwr, fullPathNameOrBytes, echoName, forceSync);
  }

  public BufferedReader spartanFileGetRdr(String name, String[] info) {
    String name00 = name;
    String header = info[1];
    Map<String, String> fileData = new Hashtable<String, String>();
    if (info.length == 3) {
      // we need information from the output file, info[2]
      String name0 = spartanGetObjectAsSections(info[2], header, fileData);
      fileData.put("OUTPUT", name0);
      info = spartanFileList(name, fileData.get(name0));
      if (info.length == 3) {
        // might have a second option
        name0 = spartanGetObjectAsSections(info[2], header, fileData);
        fileData.put("OUTPUT", name0);
        info = spartanFileList(info[1], fileData.get(name0));
      }
    }
    // load each file individually, but return files IN ORDER
    SB sb = new SB();
    if (fileData.get("OUTPUT") != null)
      sb.append(fileData.get(fileData.get("OUTPUT")));
    String s;
    for (int i = 2; i < info.length; i++) {
      name = info[i];
      name = spartanGetObjectAsSections(name, header, fileData);
      Logger.info("reading " + name);
      s = fileData.get(name);
      sb.append(s);
    }
    s = sb.toString();
    fm.spardirPut(name00.replace('\\', '/'), s.getBytes());
    return Rdr.getBR(s);
  }

  private String[] spartanFileList(String name, String zipDirectory) {
    return getJzu().spartanFileList(fm.vwr.getJzt(), name, zipDirectory);
  }

  /**
   * delivers file contents and directory listing for a ZIP/JAR file into sb
   * 
   * @param name
   * @param header
   * @param fileData
   * @return name of entry
   */
  private String spartanGetObjectAsSections(String name, String header,
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
      Object t = fm.getBufferedInputStreamOrErrorMessageFromName(name, fullName,
          false, false, null, false, true);
      if (t instanceof String) {
        fileData.put(name0, (String) t + "\n");
        return name0;
      }
      bis = (BufferedInputStream) t;
      if (Rdr.isCompoundDocumentS(bis)) {
        // very specialized reader; assuming we have a Spartan document here
        GenericBinaryDocument doc = (GenericBinaryDocument) Interface
            .getInterface("javajs.util.CompoundDocument", fm.vwr, "file");
        doc.setStream(fm.vwr.getJzt(), bis, true);
        doc.getAllDataMapped(name.replace('\\', '/'), "Molecule", fileData);
      } else if (Rdr.isZipS(bis)) {
        fm.vwr.getJzt().getAllZipData(bis, subFileList, name.replace('\\', '/'), "Molecule",
            fileData);
      } else if (asBinaryString) {
        // used for Spartan binary file reading
        GenericBinaryDocument bd = (GenericBinaryDocument) Interface
            .getInterface("javajs.util.BinaryDocument", fm.vwr, "file");
        bd.setStream(fm.vwr.getJzt(), bis, false);
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
            Rdr.isGzipS(bis) ? new BufferedInputStream(fm.vwr.getJzt().newGZIPInputStream(bis)) : bis, null);
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

  public byte[] getCachedPngjBytes(String pathName) {
    return getJzu().getCachedPngjBytes(this, pathName);
  }

}

