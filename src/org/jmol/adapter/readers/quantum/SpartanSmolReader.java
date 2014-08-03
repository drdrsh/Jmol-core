/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-11 23:56:13 -0500 (Mon, 11 Sep 2006) $
 * $Revision: 5499 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.adapter.readers.quantum;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.util.Logger;

import javajs.util.BC;
import javajs.util.PT;
import javajs.util.SB;

/*
 * Spartan SMOL and .spartan compound document reader and .spartan06 zip files
 * 
 */

public class SpartanSmolReader extends SpartanInputReader {

  private boolean iHaveModelStatement = false;
  private boolean isCompoundDocument = false;
  private boolean inputOnly;
  private boolean espCharges;

  @Override
  protected void initializeReader() throws Exception {
    modelName = "Spartan file";
    isCompoundDocument = (rd()
        .indexOf("Compound Document File Directory") >= 0);
    inputOnly = checkFilterKey("INPUT");
    espCharges = !checkFilterKey("MULLIKEN"); // changed default in Jmol 12.1.41, 12.0.38

  }

  @Override
  protected boolean checkLine() throws Exception {
    String lcline;
    if (isCompoundDocument
        && (lcline = line.toLowerCase())
            .equals("begin directory entry molecule")
        || line.indexOf("JMOL_MODEL") >= 0 && !line.startsWith("END")) {

      // bogus type added by Jmol as a marker only

      if (modelNumber > 0)
        applySymmetryAndSetTrajectory();
      iHaveModelStatement = true;
      int modelNo = getModelNumber();
      modelNumber = (bsModels == null && modelNo != Integer.MIN_VALUE ? modelNo
          : modelNumber + 1);
      bondData = "";
      if (!doGetModel(modelNumber, null))
        return checkLastModel();
      if (modelAtomCount == 0)
        asc.newAtomSet();
      moData = new Hashtable<String, Object>();
      moData.put("isNormalized", Boolean.TRUE);
      if (modelNo == Integer.MIN_VALUE) {
        modelNo = modelNumber;
        title = "Model " + modelNo;
      } else {
        title = titles.get("Title" + modelNo);
        title = "Profile " + modelNo + (title == null ? "" : ": " + title);
      }
      Logger.info(title);
      asc.setAtomSetName(title);
      setModelPDB(false);
      asc.setCurrentAtomSetNumber(modelNo);
      if (isCompoundDocument)
        readMyTransform();
      return true;
    }
    if (iHaveModelStatement && !doProcessLines)
      return true;
    if ((line.indexOf("BEGIN") == 0)) {
      lcline = line.toLowerCase();
      if (lcline.endsWith("input")) {
        bondData = "";
        readInputRecords();
        if (asc.errorMessage != null) {
          continuing = false;
          return false;
        }
        if (title != null)
          asc.setAtomSetName(title);
        setCharges();
        if (inputOnly) {
          continuing = false;
          return false;
        }
      } else if (lcline.endsWith("_output")) {
        return true;
      } else if (lcline.endsWith("output")) {
        readOutput();
        return false;
      } else if (lcline.endsWith("molecule")
          || lcline.endsWith("molecule:asbinarystring")) {
        readMyTransform();
        return false;
      } else if (lcline.endsWith("proparc")
          || lcline.endsWith("propertyarchive")) {
        readProperties();
        return false;
      } else if (lcline.endsWith("archive")) {
        readArchive();
        return false;
      }
      return true;
    }
    if (line.indexOf("5D shell") >= 0)
      moData.put("calculationType", calculationType = line);
    return true;
  }

  @Override
  protected void finalizeReader() throws Exception {
    finalizeReaderASCR();
    // info out of order -- still a chance, at least for first model
    if (ac > 0 && spartanArchive != null
        && asc.bondCount == 0 && bondData != null)
      spartanArchive.addBonds(bondData, 0);
    if (moData != null) {
      Float n = (Float) asc
          .getAtomSetCollectionAuxiliaryInfo("HOMO_N");
      if (n != null)
        moData.put("HOMO", Integer.valueOf(n.intValue()));
    }
  }

  private void readMyTransform() throws Exception {
    float[] mat;
    String binaryCodes = rd();
    // last 16x4 bytes constitutes the 4x4 matrix, using doubles
    String[] tokens = getTokensStr(binaryCodes.trim());
    if (tokens.length < 16)
      return;
    byte[] bytes = new byte[tokens.length];
    for (int i = 0; i < tokens.length; i++)
      bytes[i] = (byte) PT.parseIntRadix(tokens[i], 16);
    mat = new float[16];
    for (int i = 16, j = bytes.length - 8; --i >= 0; j -= 8)
      mat[i] = BC.bytesToDoubleToFloat(bytes, j, false);
    setTransform(mat[0], mat[1], mat[2], mat[4], mat[5], mat[6], mat[8],
        mat[9], mat[10]);
  }

  private String endCheck = "END Directory Entry ";
  private String title;

  SpartanArchive spartanArchive;

  Map<String, String> titles;

  private void readOutput() throws Exception {
    titles = new Hashtable<String, String>();
    SB header = new SB();
    int pt;
    while (rd() != null && !line.startsWith("END ")) {
      header.append(line).append("\n");
      if ((pt = line.indexOf(")")) > 0)
        titles.put("Title" + parseIntRange(line, 0, pt), (line
            .substring(pt + 1).trim()));
    }
    asc.setInfo("fileHeader", header
        .toString());
  }

  private void readArchive() throws Exception {
    spartanArchive = new SpartanArchive(this, bondData, endCheck);
    if (readArchiveHeader()) {
      modelAtomCount = spartanArchive
          .readArchive(line, false, ac, false);
      if (ac == 0 || !isTrajectory)
        ac += modelAtomCount;
    }
  }

  private boolean haveCharges;

  private void setCharges() {
    if (haveCharges || asc.ac == 0)
      return;
    haveCharges = (espCharges
        && asc.setAtomSetCollectionPartialCharges("ESPCHARGES")
        || asc.setAtomSetCollectionPartialCharges("MULCHARGES")
        || asc.setAtomSetCollectionPartialCharges("Q1_CHARGES") || asc
        .setAtomSetCollectionPartialCharges("ESPCHARGES"));
  }

  private void readProperties() throws Exception {
    if (spartanArchive == null) {
      rd();
      return;
    }
    spartanArchive.readProperties();
    rd();
    setCharges();
  }

  private int getModelNumber() {
    try {
      int pt = line.indexOf("JMOL_MODEL ") + 11;
      return parseIntAt(line, pt);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private boolean readArchiveHeader() throws Exception {
    String modelInfo = rd();
    if (Logger.debugging)
      Logger.debug(modelInfo);
    if (modelInfo.indexOf("Error:") == 0) // no archive here
      return false;
    asc.setCollectionName(modelInfo);
    modelName = rd();
    if (Logger.debugging)
      Logger.debug(modelName);
    //    5  17  11  18   0   1  17   0 RHF      3-21G(d)           NOOPT FREQ
    rd();
    return true;
  }

}
