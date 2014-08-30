/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-22 14:12:46 -0500 (Sun, 22 Oct 2006) $
 * $Revision: 5999 $
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

package org.jmol.adapter.readers.molxyz;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;


import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;

/**
 * A reader for MDLI mol and sdf files.
 * <p>
 * <a href='http://www.mdli.com/downloads/public/ctfile/ctfile.jsp'>
 * http://www.mdli.com/downloads/public/ctfile/ctfile.jsp </a>
 * <p>
 * 
 * also: http://www.mdl.com/downloads/public/ctfile/ctfile.pdf
 * 
 * simple symmetry extension via load command: 9/2006 hansonr@stolaf.edu
 * 
 * setAtomCoord(atom, x, y, z) applySymmetryAndSetTrajectory()
 * 
 * simple 2D-->3D conversion using
 * 
 * load "xxx.mol" FILTER "2D"
 * 
 */
public class MolReader extends AtomSetCollectionReader {

  /*
   * from ctfile.pdf:
   * 
   * $MDL REV 1 date/time
   * $MOL
   * $HDR
   * [Molfile Header Block (see Chapter 4) = name, pgm info, comment]
   * $END HDR
   * $CTAB
   * [Ctab Block (see Chapter 2) = count + atoms + bonds + lists + props]
   * $END CTAB
   * $RGP
   * rrr [where rrr = Rgroup number]
   * $CTAB
   * [Ctab Block]
   * $END CTAB
   * $END RGP
   * $END MOL
   */

  private boolean is2D;
  private boolean haveAtomSerials;
  protected String dimension;
  protected boolean allow2D = true;
  private int iatom0;
  private V3000Rdr vr;

  @Override
  public void initializeReader() throws Exception {
    is2D = checkFilterKey("2D");
  }

  @Override
  protected boolean checkLine() throws Exception {
    boolean isMDL = (line.startsWith("$MDL"));
    if (isMDL) {
      discardLinesUntilStartsWith("$HDR");
      rd();
      if (line == null) {
        Logger.warn("$HDR not found in MDL RG file");
        continuing = false;
        return false;
      }
    } else if (line.equals("M  END"))
      return true;
    if (doGetModel(++modelNumber, null)) {
      iatom0 = asc.ac;
      processMolSdHeader();
      processCtab(isMDL);
      vr = null;
      if (isLastModel(modelNumber)) {
        continuing = false;
        return false;
      }
    }
    if (line != null && line.indexOf("$$$$") < 0)
      discardLinesUntilStartsWith("$$$$");
    return true;
  }

  @Override
  public void finalizeSubclassReader() throws Exception {
    finalizeReaderMR();
  }

  protected void finalizeReaderMR() throws Exception {
    if (is2D)
      set2D();
    isTrajectory = false;
    finalizeReaderASCR();
  }

  private void processMolSdHeader() throws Exception {
    /* 
     * obviously we aren't being this strict, but for the record:
     *  
     * from ctfile.pdf (October 2003):
     * 
     * Line 1: Molecule name. This line is unformatted, but like all 
     * other lines in a molfile may not extend beyond column 80. 
     * If no name is available, a blank line must be present.
     * Caution: This line must not contain any of the reserved 
     * tags that identify any of the other CTAB file types 
     * such as $MDL (RGfile), $$$$ (SDfile record separator), 
     * $RXN (rxnfile), or $RDFILE (RDfile headers). 
     * 
     * Line 2: This line has the format:
     * IIPPPPPPPPMMDDYYHHmmddSSssssssssssEEEEEEEEEEEERRRRRR
     * (FORTRAN: A2<--A8--><---A10-->A2I2<--F10.5-><---F12.5--><-I6-> )
     * User's first and last initials (l), program name (P), 
     * date/time (M/D/Y,H:m), dimensional codes (d), scaling factors (S, s), 
     * energy (E) if modeling program input, internal 
     * registry number (R) if input through MDL form. A blank line can be 
     * substituted for line 2. If the internal registry number is more than 
     * 6 digits long, it is stored in an M REG line (described in Chapter 3). 
     * 
     * Line 3: A line for comments. If no comment is entered, a blank line 
     * must be present.
     */

    String header = "";
    String thisDataSetName = line;
    header += line + "\n";
    asc.setCollectionName(line);
    rd();
    if (line == null)
      return;
    header += line + "\n";
    dimension = (line.length() < 22 ? "3D" : line.substring(20, 22));
    if (!allow2D && dimension.equals("2D"))
      throw new Exception("File is 2D, not 3D");
    asc.setInfo("dimension", dimension);
    //line 3: comment
    rd();
    if (line == null)
      return;
    header += line + "\n";
    Logger.info(header);
    checkCurrentLineForScript();
    asc.setInfo("fileHeader", header);
    newAtomSet(thisDataSetName);
  }

  private void processCtab(boolean isMDL) throws Exception {
    if (isMDL)
      discardLinesUntilStartsWith("$CTAB");
    if (rd() == null)
      return;
    if (line.indexOf("V3000") >= 0) {
      is2D = (dimension.equals("2D"));
      vr = ((V3000Rdr) Interface.getInterface("org.jmol.adapter.readers.molxyz.V3000Rdr", vwr, "file")).set(this);
      discardLinesUntilContains("COUNTS");
      vr.readAtomsAndBonds(getTokens());
    } else {
      readAtomsAndBonds(parseIntRange(line, 0, 3), parseIntRange(line, 3, 6));
    }
    applySymmetryAndSetTrajectory();
  }

  // 0         1         2         3         4         5         6         7
  // 01234567890123456789012345678901234567890123456789012345678901234567890
  // xxxxx.xxxxyyyyy.yyyyzzzzz.zzzz aaaddcccssshhhbbbvvvHHHrrriiimmmnnneee

  private void readAtomsAndBonds(int ac, int bc) throws Exception {
    for (int i = 0; i < ac; ++i) {
      rd();
      int len = line.length();
      String elementSymbol;
      float x, y, z;
      int charge = 0;
      int isotope = 0;
      int iAtom = Integer.MIN_VALUE;
      x = parseFloatRange(line, 0, 10);
      y = parseFloatRange(line, 10, 20);
      z = parseFloatRange(line, 20, 30);
      if (len < 34) {
        // deal with older Mol format where nothing after the symbol is used
        elementSymbol = line.substring(31).trim();
      } else {
        elementSymbol = line.substring(31, 34).trim();
        if (len >= 39) {
          int code = parseIntRange(line, 36, 39);
          if (code >= 1 && code <= 7)
            charge = 4 - code;
          code = parseIntRange(line, 34, 36);
          if (code != 0 && code >= -3 && code <= 4) {
            isotope = JmolAdapter.getNaturalIsotope(JmolAdapter
                .getElementNumber(elementSymbol));
            switch (isotope) {
            case 0:
              break;
            case 1:
              isotope = -code;
              break;
            default:
              isotope += code;
            }
          }
          //if (len >= 63) {  this field is not really an atom number. It's for atom-atom mapping in reaction files
          //  iAtom = parseIntRange(line, 60, 63);
          //  if (iAtom == 0)
          //    iAtom = Integer.MIN_VALUE;
          //}
          // previous model in series may have atom numbers indicated
          if (iAtom == Integer.MIN_VALUE && haveAtomSerials)
            iAtom = i + 1;
        }
      }
      addMolAtom(iAtom, isotope, elementSymbol, charge, x, y, z);
    }
    
    // read bonds
    
    for (int i = 0; i < bc; ++i) {
      rd();
      String iAtom1, iAtom2;
      int stereo = 0;
      iAtom1 = line.substring(0, 3).trim();
      iAtom2 = line.substring(3, 6).trim();
      int order = parseIntRange(line, 6, 9);
      if (is2D && order == 1 && line.length() >= 12)
        stereo = parseIntRange(line, 9, 12);
      order = fixOrder(order, stereo);
      if (haveAtomSerials)
        asc.addNewBondFromNames(iAtom1, iAtom2, order);
      else
        asc.addNewBondWithOrder(iatom0 + parseIntStr(iAtom1) - 1, iatom0
            + parseIntStr(iAtom2) - 1, order);
    }
    
    // read V2000 user data
    
    while (rd() != null && line.indexOf("$$$$") != 0) {
      if (line.toUpperCase().contains("_PARTIAL_CHARGES")) {
        try {
          Atom[] atoms = asc.atoms;
          for (int i = parseIntStr(rd()); --i >= 0;) {
            String[] tokens = getTokensStr(rd());
            int atomIndex = parseIntStr(tokens[0]) + iatom0 - 1;
            float partialCharge = parseFloatStr(tokens[1]);
            if (!Float.isNaN(partialCharge))
              atoms[atomIndex].partialCharge = partialCharge;
          }
        } catch (Exception e) {
          return;
        }
      }
    }
  }

  public void addMolAtom(int iAtom, int isotope, String elementSymbol,
                         int charge, float x, float y, float z) {
    switch (isotope) {
    case 0:
      break;
    case -1:
      elementSymbol = "D";
      break;
    case -2:
      elementSymbol = "T";
      break;
    default:
      elementSymbol = isotope + elementSymbol;
    }
    if (is2D && z != 0)
      is2D = false;
    Atom atom = new Atom();
    atom.elementSymbol = elementSymbol;
    atom.formalCharge = charge;
    setAtomCoordXYZ(atom, x, y, z);
    if (iAtom == Integer.MIN_VALUE) {
      asc.addAtom(atom);
    } else {
      haveAtomSerials = true;
      atom.atomSerial = iAtom;
      asc.addAtomWithMappedSerialNumber(atom);
    }
  }

  public int fixOrder(int order, int stereo) {
    switch (order) {
    default:
    case 0:
    case -10:
      return 1; // smiles parser error 
    case 1:
      switch (stereo) {
      case 1: // UP
        return JmolAdapter.ORDER_STEREO_NEAR;
      case 3: // DOWN, V3000
      case 6: // DOWN
        return JmolAdapter.ORDER_STEREO_FAR;
      }
      break;
    case 2:
    case 3:
      break;
    case 4:
      return JmolAdapter.ORDER_AROMATIC;
    case 5:
      return JmolAdapter.ORDER_PARTIAL12;
    case 6:
      return JmolAdapter.ORDER_AROMATIC_SINGLE;
    case 7:
      return JmolAdapter.ORDER_AROMATIC_DOUBLE;
    case 8:
    case 9: // haptic
      return JmolAdapter.ORDER_PARTIAL01;
    }
    return order;
  }

  public void addMolBond(String iAtom1, String iAtom2, int order, int stereo) {
    order = fixOrder(order, stereo);
    if (haveAtomSerials)
     asc.addNewBondFromNames(iAtom1, iAtom2, order);
    else
      asc.addNewBondWithOrder(iatom0 + parseIntStr(iAtom1) - 1, iatom0
          + parseIntStr(iAtom2) - 1, order);
  }

}
