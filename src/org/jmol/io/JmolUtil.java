/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javajs.api.GenericBinaryDocument;
import javajs.api.GenericPlatform;
import javajs.api.GenericZipTools;
import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.OC;
import javajs.util.PT;
import javajs.util.Rdr;
import javajs.util.SB;

import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolZipUtilities;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

public class JmolUtil implements JmolZipUtilities {

  public JmolUtil() {
    // for reflection
  }

  /**
   * called by SmarterJmolAdapter to see if we have a Spartan directory and, if
   * so, open it and get all the data into the correct order.
   * @param zpt 
   * 
   * @param is
   * @param zipDirectory
   * @return String data for processing
   */
  private static SB checkSpecialData(GenericZipTools zpt, InputStream is, String[] zipDirectory) {
    boolean isSpartan = false;
    // 0 entry is not used here
    for (int i = 1; i < zipDirectory.length; i++) {
      if (zipDirectory[i].endsWith(".spardir/")
          || zipDirectory[i].indexOf("_spartandir") >= 0) {
        isSpartan = true;
        break;
      }
    }
    if (!isSpartan)
      return null;
    SB data = new SB();
    data.append("Zip File Directory: ").append("\n")
        .append(Escape.eAS(zipDirectory, true)).append("\n");
    Map<String, String> fileData = new Hashtable<String, String>();
    zpt.getAllZipData(is, new String[] {}, "", "Molecule", fileData);
    String prefix = "|";
    String outputData = fileData.get(prefix + "output");
    if (outputData == null)
      outputData = fileData.get((prefix = "|" + zipDirectory[1]) + "output");
    data.append(outputData);
    String[] files = getSpartanFileList(prefix, getSpartanDirs(outputData));
    for (int i = 2; i < files.length; i++) {
      String name = files[i];
      if (fileData.containsKey(name))
        data.append(fileData.get(name));
      else
        data.append(name + "\n");
    }
    return data;
  }

  /**
   * called by SmarterJmolAdapter to see if we can automatically assign a file
   * from the zip file. If so, return a subfile list for this file. The first
   * element of the list is left empty -- it would be the zipfile name.
   * 
   * Assignment can be made if (1) there is only one file in the collection or
   * (2) if the first file is xxxx.spardir/
   * 
   * Note that __MACOS? files are ignored by the ZIP file reader.
   * 
   * @param zipDirectory
   * @return subFileList
   */
  static String[] checkSpecialInZip(String[] zipDirectory) {
    String name;
    return (zipDirectory.length < 2 ? null : (name = zipDirectory[1])
        .endsWith(".spardir/") || zipDirectory.length == 2 ? new String[] { "",
        (name.endsWith("/") ? name.substring(0, name.length() - 1) : name) }
        : null);
  }

  /**
   * read the output file from the Spartan directory and decide from that what
   * files need to be read and in what order - usually M0001 or a set of
   * Profiles. But Spartan saves the Profiles in alphabetical order, not
   * numerical. So we fix that here.
   * 
   * @param outputFileData
   * @return String[] list of files to read
   */
  private static String[] getSpartanDirs(String outputFileData) {
    if (outputFileData == null)
      return new String[] {};
    Lst<String> v = new Lst<String>();
    String token;
    String lasttoken = "";
    if (!outputFileData.startsWith("java.io.FileNotFoundException")
        && !outputFileData.startsWith("FILE NOT FOUND")
        && outputFileData.indexOf("<html") < 0)
      try {
        StringTokenizer tokens = new StringTokenizer(outputFileData, " \t\r\n");
        while (tokens.hasMoreTokens()) {
          // profile file name is just before each right-paren:
          /*
           * MacSPARTAN '08 ENERGY PROFILE: x86/Darwin 130
           * 
           * Dihedral Move : C3 - C2 - C1 - O1 [ 4] -180.000000 .. 180.000000
           * Dihedral Move : C2 - C1 - O1 - H3 [ 4] -180.000000 .. 180.000000
           * 
           * 1 ) -180.00 -180.00 -504208.11982719 2 ) -90.00 -180.00
           * -504200.18593376
           * 
           * ...
           * 
           * 24 ) 90.00 180.00 -504200.18564495 25 ) 180.00 180.00
           * -504208.12129747
           * 
           * Found a local maxima E = -504178.25455465 [ 3 3 ]
           * 
           * 
           * Reason for exit: Successful completion Mechanics CPU Time : 1:51.42
           * Mechanics Wall Time: 12:31.54
           */
          if ((token = tokens.nextToken()).equals(")"))
            v.addLast(lasttoken);
          else if (token.equals("Start-")
              && tokens.nextToken().equals("Molecule"))
            v.addLast(PT.split(tokens.nextToken(), "\"")[1]);
          lasttoken = token;
        }
      } catch (Exception e) {
        //
      }
    return (v.size() == 0 ? new String[] { "M0001" } : v.toArray(new String[v
        .size()]));
  }

  /**
   * returns the list of files to read for every Spartan spardir. Simple numbers
   * are assumed to be Profiles; others are models.
   * 
   * @param name
   * @param dirNums
   * @return String[] list of files to read given a list of directory names
   * 
   */
  private static String[] getSpartanFileList(String name, String[] dirNums) {
    String[] files = new String[2 + dirNums.length * 5];
    files[0] = "SpartanSmol";
    files[1] = "Directory Entry ";
    int pt = 2;
    name = name.replace('\\', '/');
    if (name.endsWith("/"))
      name = name.substring(0, name.length() - 1);
    String sep = (name.endsWith(".zip") ? "|" : "/");
    for (int i = 0; i < dirNums.length; i++) {
      String path = name + sep;
      path += (PT.isDigit(dirNums[i].charAt(0)) ? "Profile." + dirNums[i]
              : dirNums[i]) + "/";
      files[pt++] = path + "#JMOL_MODEL " + dirNums[i];
      files[pt++] = path + "input";
      files[pt++] = path + "archive";
      files[pt++] = path + "Molecule:asBinaryString";
      files[pt++] = path + "proparc";
    }
    return files;
  }

  private static String shortSceneFilename(String pathName) {
    int pt = pathName.indexOf("_scene_") + 7;
    if (pt < 7)
      return pathName;
    String s = "";
    if (pathName.endsWith("|state.spt")) {
      int pt1 = pathName.indexOf('.', pt);
      if (pt1 < 0)
        return pathName;
      s = pathName.substring(pt, pt1);
    }
    int pt2 = pathName.lastIndexOf("|");
    return pathName.substring(0, pt) + s
        + (pt2 > 0 ? pathName.substring(pt2) : "");
  }

  @Override
  public Object getAtomSetCollectionOrBufferedReaderFromZip(Viewer vwr,
                                                            JmolAdapter adapter,
                                                            InputStream is,
                                                            String fileName,
                                                            String[] zipDirectory,
                                                            Map<String, Object> htParams,
                                                            int subFilePtr, boolean asBufferedReader) {

    // we're here because user is using | in a load file name
    // or we are opening a zip file.

    boolean doCombine = (subFilePtr == 1);
    htParams.put("zipSet", fileName);
    String[] subFileList = (String[]) htParams.get("subFileList");
    if (subFileList == null)
      subFileList = checkSpecialInZip(zipDirectory);
    String subFileName = (subFileList == null
        || subFilePtr >= subFileList.length ? null : subFileList[subFilePtr]);
    if (subFileName != null
        && (subFileName.startsWith("/") || subFileName.startsWith("\\")))
      subFileName = subFileName.substring(1);
    int selectedFile = 0;
    if (subFileName == null && htParams.containsKey("modelNumber")) {
      selectedFile = ((Integer) htParams.get("modelNumber")).intValue();
      if (selectedFile > 0 && doCombine)
        htParams.remove("modelNumber");
    }

    // zipDirectory[0] is the manifest if present
    String manifest = (String) htParams.get("manifest");
    boolean useFileManifest = (manifest == null);
    if (useFileManifest)
      manifest = (zipDirectory.length > 0 ? zipDirectory[0] : "");
    boolean haveManifest = (manifest.length() > 0);
    if (haveManifest) {
      if (Logger.debugging)
        Logger.debug("manifest for  " + fileName + ":\n" + manifest);
    }
    boolean ignoreErrors = (manifest.indexOf("IGNORE_ERRORS") >= 0);
    boolean selectAll = (manifest.indexOf("IGNORE_MANIFEST") >= 0);
    boolean exceptFiles = (manifest.indexOf("EXCEPT_FILES") >= 0);
    if (selectAll || subFileName != null)
      haveManifest = false;
    if (useFileManifest && haveManifest) {
      String path = FileManager.getManifestScriptPath(manifest);
      if (path != null)
        return JC.NOTE_SCRIPT_FILE + fileName + path + "\n";
    }
    Lst<Object> vCollections = new Lst<Object>();
    Map<String, Object> htCollections = (haveManifest ? new Hashtable<String, Object>()
        : null);
    int nFiles = 0;
    // 0 entry is manifest

    // check for a Spartan directory. This is not entirely satisfying,
    // because we aren't reading the file in the proper sequence.
    // this code is a hack that should be replaced with the sort of code
    // running in FileManager now.

    GenericZipTools zpt = vwr.getJzt();
    Object ret = checkSpecialData(zpt, is, zipDirectory);
    if (ret instanceof String)
      return ret;
    SB data = (SB) ret;
    try {
      if (data != null) {
        BufferedReader reader = Rdr.getBR(data.toString());
        if (asBufferedReader)
          return reader;
        ret = adapter
            .getAtomSetCollectionFromReader(fileName, reader, htParams);
        if (ret instanceof String)
          return ret;
        if (ret instanceof AtomSetCollection) {
          AtomSetCollection atomSetCollection = (AtomSetCollection) ret;
          if (atomSetCollection.errorMessage != null) {
            if (ignoreErrors)
              return null;
            return atomSetCollection.errorMessage;
          }
          return atomSetCollection;
        }
        if (ignoreErrors)
          return null;
        return "unknown reader error";
      }
      if (is instanceof BufferedInputStream)
        is = Rdr.getPngZipStream((BufferedInputStream) is, true);
      ZipInputStream zis = (ZipInputStream) zpt.newZipInputStream(is);
      ZipEntry ze;
      if (haveManifest)
        manifest = '|' + manifest.replace('\r', '|').replace('\n', '|') + '|';
      while ((ze = zis.getNextEntry()) != null
          && (selectedFile <= 0 || vCollections.size() < selectedFile)) {
        if (ze.isDirectory())
          continue;
        String thisEntry = ze.getName();
        if (subFileName != null && !thisEntry.equals(subFileName))
          continue;
        if (subFileName != null)
          htParams.put("subFileName", subFileName);
        if (thisEntry.startsWith("JmolManifest") || haveManifest
            && exceptFiles == manifest.indexOf("|" + thisEntry + "|") >= 0)
          continue;
        byte[] bytes = Rdr.getLimitedStreamBytes(zis, ze.getSize());
        //        String s = new String(bytes);
        //        System.out.println("ziputil " + s.substring(0, 100));
        if (Rdr.isGzipB(bytes))
          bytes = Rdr.getLimitedStreamBytes(
              zpt.getUnGzippedInputStream(bytes), -1);
        if (Rdr.isZipB(bytes) || Rdr.isPngZipB(bytes)) {
          BufferedInputStream bis = Rdr.getBIS(bytes);
          String[] zipDir2 = zpt.getZipDirectoryAndClose(bis, "JmolManifest");
          bis = Rdr.getBIS(bytes);
          Object atomSetCollections = getAtomSetCollectionOrBufferedReaderFromZip(
              vwr, adapter, bis, fileName + "|" + thisEntry, zipDir2,
              htParams, ++subFilePtr, asBufferedReader);
          if (atomSetCollections instanceof String) {
            if (ignoreErrors)
              continue;
            return atomSetCollections;
          } else if (atomSetCollections instanceof AtomSetCollection
              || atomSetCollections instanceof Lst<?>) {
            if (haveManifest && !exceptFiles)
              htCollections.put(thisEntry, atomSetCollections);
            else
              vCollections.addLast(atomSetCollections);
          } else if (atomSetCollections instanceof BufferedReader) {
            if (doCombine)
              zis.close();
            return atomSetCollections; // FileReader has requested a zip file
            // BufferedReader
          } else {
            if (ignoreErrors)
              continue;
            zis.close();
            return "unknown zip reader error";
          }
        } else if (Rdr.isPickleB(bytes)) {
          BufferedInputStream bis = Rdr.getBIS(bytes);
          if (doCombine)
            zis.close();
          return bis;
        } else {
          String sData;
          if (Rdr.isCompoundDocumentB(bytes)) {
            GenericBinaryDocument jd = (GenericBinaryDocument) Interface
                .getInterface("javajs.util.CompoundDocument", vwr, "file");
            jd.setStream(zpt, Rdr.getBIS(bytes), true);
            sData = jd.getAllDataFiles("Molecule", "Input").toString();
          } else {
            // could be a PNGJ file with an internal pdb.gz entry, for instance
            sData = Rdr.fixUTF(bytes);
          }
          BufferedReader reader = Rdr.getBR(sData);
          if (asBufferedReader) {
            if (doCombine)
              zis.close();
            return reader;
          }
          String fname = fileName + "|" + ze.getName();

          ret = adapter.getAtomSetCollectionFromReader(fname, reader, htParams);

          if (!(ret instanceof AtomSetCollection)) {
            if (ignoreErrors)
              continue;
            zis.close();
            return "" + ret;
          }
          if (haveManifest && !exceptFiles)
            htCollections.put(thisEntry, ret);
          else
            vCollections.addLast(ret);
          AtomSetCollection a = (AtomSetCollection) ret;
          if (a.errorMessage != null) {
            if (ignoreErrors)
              continue;
            zis.close();
            return a.errorMessage;
          }
        }
      }

      if (doCombine)
        zis.close();

      // if a manifest exists, it sets the files and file order

      if (haveManifest && !exceptFiles) {
        String[] list = PT.split(manifest, "|");
        for (int i = 0; i < list.length; i++) {
          String file = list[i];
          if (file.length() == 0 || file.indexOf("#") == 0)
            continue;
          if (htCollections.containsKey(file))
            vCollections.addLast(htCollections.get(file));
          else if (Logger.debugging)
            Logger.debug("manifested file " + file + " was not found in "
                + fileName);
        }
      }
      if (!doCombine)
        return vCollections;
      
      AtomSetCollection result = (vCollections.size() == 1 && vCollections.get(0) instanceof AtomSetCollection 
            ? (AtomSetCollection) vCollections.get(0) : new AtomSetCollection("Array", null, null,
          vCollections));
      if (result.errorMessage != null) {
        if (ignoreErrors)
          return null;
        return result.errorMessage;
      }
      if (nFiles == 1)
        selectedFile = 1;
      if (selectedFile > 0 && selectedFile <= vCollections.size())
        return vCollections.get(selectedFile - 1);
      return result;

    } catch (Exception e) {
      if (ignoreErrors)
        return null;
      Logger.error("" + e);
      return "" + e;
    } catch (Error er) {
      Logger.errorEx(null, er);
      return "" + er;
    }
  }

  public boolean clearAndCachePngjFile(JmolBinary jmb, String[] data) {
    jmb.fm.pngjCache = new Hashtable<String, Object>();
    if (data == null || data[0] == null)
      return false;
    data[0] = Rdr.getZipRoot(data[0]);
    String shortName = shortSceneFilename(data[0]);
    Map<String, Object> cache = jmb.fm.pngjCache;
    try {
      data[1] = jmb.fm.vwr.getJzt().cacheZipContents( 
          Rdr.getPngZipStream((BufferedInputStream) jmb.fm
              .getBufferedInputStreamOrErrorMessageFromName(data[0], null,
                  false, false, null, false, true), true), shortName, cache, false);
    } catch (Exception e) {
      return false;
    }
    if (data[1] == null)
      return false;
    byte[] bytes = data[1].getBytes();
    System.out.println("jmolutil caching " + bytes.length + " bytes as " + jmb.fm.getCanonicalName(data[0]));
    cache.put(jmb.fm.getCanonicalName(data[0]), bytes); // marker in case the .all. file is changed
    if (shortName.indexOf("_scene_") >= 0) {
      cache.put(shortSceneFilename(data[0]), bytes); // good for all .min. files of this scene set
      bytes = (byte[]) cache.remove(shortName + "|state.spt");
      if (bytes != null)
        cache.put(shortSceneFilename(data[0] + "|state.spt"), bytes);
    }
    for (String key : cache.keySet())
      System.out.println(key);
    return true;
}

  @Override
  public byte[] getCachedPngjBytes(JmolBinary jmb, String pathName) {
    if (pathName.startsWith("file:///"))
      pathName = "file:" +pathName.substring(7);
    Logger.info("JmolUtil checking PNGJ cache for " + pathName);
    String shortName = shortSceneFilename(pathName);
    if (jmb.fm.pngjCache == null
        && !clearAndCachePngjFile(jmb, new String[] { pathName, null }))
      return null;
    Map<String, Object> cache = jmb.fm.pngjCache;
    boolean isMin = (pathName.indexOf(".min.") >= 0);
    if (!isMin) {
      String cName = jmb.fm.getCanonicalName(Rdr.getZipRoot(pathName));
      if (!cache.containsKey(cName)
          && !clearAndCachePngjFile(jmb, new String[] { pathName, null }))
        return null;
      if (pathName.indexOf("|") < 0)
        shortName = cName;
    }
    if (cache.containsKey(shortName)) {
      Logger.info("FileManager using memory cache " + shortName);
      return (byte[]) jmb.fm.pngjCache.get(shortName);
    }
    //    for (String key : pngjCache.keySet())
    //    System.out.println(" key=" + key);
    //System.out.println("FileManager memory cache size=" + pngjCache.size()
    //  + " did not find " + pathName + " as " + shortName);
    if (!isMin || !clearAndCachePngjFile(jmb, new String[] { pathName, null }))
      return null;
    Logger.info("FileManager using memory cache " + shortName);
    return (byte[]) cache.get(shortName);
  }

  /**
   * 
   * Special loading for file directories. This method is called from the
   * FileManager via SmarterJmolAdapter. It's here because Resolver is the place
   * where all distinctions are made.
   * 
   * In the case of spt files, no need to load them; here we are just checking
   * for type.
   * 
   * In the case of .spardir directories, we need to provide a list of the
   * critical files that need loading and concatenation for the
   * SpartanSmolReader.
   * 
   * we return an array for which:
   * 
   * [0] file type (class prefix) or null for SPT file [1] header to add for
   * each BEGIN/END block (ignored) [2...] files to load and concatenate
   * 
   * @param name
   * @param type
   * @return array detailing action for this set of files
   */
  @Override
  public String[] spartanFileList(GenericZipTools zpt, String name, String type) {
    // make list of required files
    String[] dirNums = getSpartanDirs(type);
    if (dirNums.length == 0 && name.endsWith(".spardir.zip")
        && type.indexOf(".zip|output") >= 0) {
      // try again, with the idea that 
      String sname = name.replace('\\', '/');
      int pt = name.lastIndexOf(".spardir");
      pt = sname.lastIndexOf("/");
      // mac directory zipped up?
      sname = name + "|" + name.substring(pt + 1, name.length() - 4);
      return new String[] { "SpartanSmol", sname, sname + "/output" };
    }
    return getSpartanFileList(name, dirNums);
  }

  @Override
  public Object getImage(Viewer vwr, Object fullPathNameOrBytes,
                         String echoName, boolean forceSync) {
    Object image = null;
    Object info = null;
    GenericPlatform apiPlatform = vwr.apiPlatform;
    boolean createImage = false;
    String fullPathName = "" + fullPathNameOrBytes;
    if (fullPathNameOrBytes instanceof String) {
      boolean isBMP = fullPathName.toUpperCase().endsWith("BMP");
      // previously we were loading images 
      if (forceSync || fullPathName.indexOf("|") > 0 || isBMP) {
        Object ret = vwr.fm.getFileAsBytes(fullPathName, null);
        if (!AU.isAB(ret))
          return "" + ret;
        // converting bytes to an image in JavaScript is a synchronous process
        if (vwr.isJS)
          info = new Object[] { echoName, fullPathNameOrBytes, ret };
        else
          image = apiPlatform.createImage(ret);
      } else if (OC.urlTypeIndex(fullPathName) >= 0) {
        // if JavaScript returns an image, than it must have been cached, and 
        // the call was not asynchronous after all.
        if (vwr.isJS)
          info = new Object[] { echoName, fullPathNameOrBytes, null };
        else
          try {
            image = apiPlatform.createImage(new URL((URL) null, fullPathName,
                null));
          } catch (Exception e) {
            return "bad URL: " + fullPathName;
          }
      } else {
        createImage = true;
      }
    } else if (vwr.isJS) {
      // not sure that this can work. 
      // ensure that apiPlatform.createImage is called
      //
      info = new Object[] { echoName,
          Rdr.guessMimeTypeForBytes((byte[]) fullPathNameOrBytes),
          fullPathNameOrBytes };
    } else {
      createImage = true;
    }
    if (createImage)
      image = apiPlatform
          .createImage("\1close".equals(fullPathNameOrBytes) ? "\1close"
              + echoName : fullPathNameOrBytes);
    else if (info != null) // JavaScript only
      image = apiPlatform.createImage(info);

    /**
     * 
     * @j2sNative
     * 
     *            return image;
     * 
     */
    {
      if (image == null)
        return null;
      try {
        if (!apiPlatform.waitForDisplay(info, image))
          return null;
        return (apiPlatform.getImageWidth(image) < 1 ? "invalid or missing image "
            + fullPathName
            : image);
      } catch (Exception e) {
        return e.toString() + " opening " + fullPathName;
      }
    }
  }
  
}
