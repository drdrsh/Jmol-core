/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-20 07:48:25 -0500 (Fri, 20 Oct 2006) $
 * $Revision: 5991 $
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
package org.jmol.adapter.readers.cif;

import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.api.JmolAdapter;
import org.jmol.api.SymmetryInterface;
import org.jmol.java.BS;

import org.jmol.script.T;

import javajs.api.GenericCifDataParser;
import javajs.util.Lst;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.jmol.util.Logger;
import org.jmol.util.Vibration;

import javajs.util.Rdr;
import javajs.util.CifDataParser;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.V3;

/**
 * A true line-free CIF file reader for CIF files.
 * 
 * Subclasses of CIF -- mmCIF/PDBx and msCIF -- are initialized from here.
 * 
 * Added nonstandard mCIF (magnetic_ tags) 5/2/2014 note that PRELIM keys can be
 * removed at some later time
 * 
 * <p>
 * <a href='http://www.iucr.org/iucr-top/cif/'>
 * http://www.iucr.org/iucr-top/cif/ </a>
 * 
 * <a href='http://www.iucr.org/iucr-top/cif/standard/cifstd5.html'>
 * http://www.iucr.org/iucr-top/cif/standard/cifstd5.html </a>
 * 
 * @author Miguel, Egon, and Bob (hansonr@stolaf.edu)
 * 
 *         symmetry added by Bob Hanson:
 * 
 *         setSpaceGroupName() setSymmetryOperator() setUnitCellItem()
 *         setFractionalCoordinates() setAtomCoord()
 *         applySymmetryAndSetTrajectory()
 * 
 */
public class CifReader extends AtomSetCollectionReader {

  private static final String titleRecords = "__citation_title__publ_section_title__active_magnetic_irreps_details__";
  private MSCifRdr mr;

  // no need for reflection here -- the CIF reader is already
  // protected by reflection

  GenericCifDataParser parser;

  private boolean isMolecular;
  private boolean filterAssembly;
  private boolean allowRotations = true;
  private boolean checkSpecial = true;
  private boolean readIdeal = true;
  private int configurationPtr = Integer.MIN_VALUE;
  private boolean useAuthorChainID = true;

  private String thisDataSetName = "";
  private String chemicalName = "";
  private String thisStructuralFormula = "";
  private String thisFormula = "";
  private boolean iHaveDesiredModel;
  protected boolean isMMCIF;
  boolean isMagCIF;
  private String molecularType = "GEOM_BOND default";
  private char lastAltLoc = '\0';
  private boolean haveAromatic;
  private int conformationIndex;
  private int nMolecular = 0;
  private String appendedData;
  private boolean skipping;
  protected int nAtoms;
  protected int ac;

  private String auditBlockCode;
  private String lastSpaceGroupName;

  private boolean modulated;
  protected boolean isCourseGrained;
  boolean haveCellWaveVector;
  private String latticeType = null;
  private int modDim;

  protected Map<String, String> htGroup1;
  private int nAtoms0;

  @Override
  public void initializeReader() throws Exception {
    initSubclass();
    parser = new CifDataParser().set(this, null);
    allowPDBFilter = true;
    appendedData = (String) htParams.get("appendedData");
    String conf = getFilter("CONF ");
    if (conf != null)
      configurationPtr = parseIntStr(conf);
    isMolecular = checkFilterKey("MOLECUL") && !checkFilterKey("BIOMOLECULE"); // molecular; molecule
    readIdeal = !checkFilterKey("NOIDEAL");
    filterAssembly = checkFilterKey("$");
    useAuthorChainID = !checkFilterKey("NOAUTHORCHAINS");
    if (isMolecular) {
      if (!doApplySymmetry) {
        doApplySymmetry = true;
        latticeCells[0] = 1;
        latticeCells[1] = 1;
        latticeCells[2] = 1;
      }
      molecularType = "filter \"MOLECULAR\"";
    }
    checkSpecial = !checkFilterKey("NOSPECIAL");
    asc.setCheckSpecial(checkSpecial);
    allowRotations = !checkFilterKey("NOSYM");
    if (strSupercell != null && strSupercell.indexOf(",") >= 0)
      addCellType("conventional", strSupercell, true);
    readCifData();
    continuing = false;
  }

  protected void initSubclass() {
    // for MMCifReader
  }

  private void readCifData() throws Exception {

    /*
     * Modified for 10.9.64 9/23/06 by Bob Hanson to remove as much as possible
     * of line dependence. a loop could now go:
     * 
     * blah blah blah loop_ _a _b _c 0 1 2 0 3 4 0 5 6 loop_......
     * 
     * we don't actually check that any skipped loop has the proper number of
     * data points --- some multiple of the number of data keys -- but other
     * than that, we are checking here for proper CIF syntax, and Jmol will
     * report if it finds data where a key is supposed to be.
     */
    line = "";
    while ((key = parser.peekToken()) != null)
      if (!readAllData())
        break;
    if (appendedData != null) {
      parser = ((GenericCifDataParser) getInterface("javajs.util.CifDataParser")).set(null, Rdr.getBR(appendedData));
      while ((key = parser.peekToken()) != null)
        if (!readAllData())
          break;
    }
  }

  private boolean readAllData() throws Exception {
    if (key.startsWith("data_")) {
      if (iHaveDesiredModel)
        return false;
      newModel(++modelNumber);
      haveCellWaveVector = false;
      if (auditBlockCode == null)
        modulated = false;
      if (!skipping) {
        nAtoms0 = asc.ac;
        processDataParameter();
        nAtoms = asc.ac;
      }
      return true;
    }
    if (skipping && key.equals("_audit_block_code")) {
      iHaveDesiredModel = false;
      skipping = false;
    }
    if (key.startsWith("loop_")) {
      if (skipping) {
        parser.getTokenPeeked();
        parser.skipLoop();
      } else {
        processLoopBlock();
      }
      return true;
    }
    // global_ and stop_ are reserved STAR keywords
    // see http://www.iucr.org/iucr-top/lists/comcifs-l/msg00252.html
    // http://www.iucr.org/iucr-top/cif/spec/version1.1/cifsyntax.html#syntax

    // stop_ is not allowed, because nested loop_ is not allowed
    // global_ is a reserved STAR word; not allowed in CIF
    // ah, heck, let's just flag them as CIF ERRORS
    /*
     * if (key.startsWith("global_") || key.startsWith("stop_")) {
     * tokenizer.getTokenPeeked(); continue; }
     */
    if (key.indexOf("_") != 0) {
      if (key.equals("DSSR:"))
        processDSSR(this, htGroup1);
      else
        Logger.warn("CIF ERROR ? should be an underscore: " + key);
      parser.getTokenPeeked();
    } else if (!getData()) {
      return true;
    }

    if (!skipping) {
      key = parser.fixKey(key);      
      if (key.startsWith("_chemical_name") || key.equals("_chem_comp_name")) {
        processChemicalInfo("name");
      } else if (key.startsWith("_chemical_formula_structural")) {
        processChemicalInfo("structuralFormula");
      } else if (key.startsWith("_chemical_formula_sum")
          || key.equals("_chem_comp_formula")) {
        processChemicalInfo("formula");
      } else if (key.equals("_cell_modulation_dimension")) {
        modDim = parseIntStr(data);
      } else if (key.startsWith("_cell_") && key.indexOf("_commen_") < 0) {
        processCellParameter();
      } else if (key.startsWith("_atom_sites_fract_tran")) {
        processUnitCellTransformMatrix();
      } else if (key.equals("_audit_block_code")) {
        auditBlockCode = parser.fullTrim(data).toUpperCase();
        appendLoadNote(auditBlockCode);
        if (htAudit != null && auditBlockCode.contains("_MOD_")) {
          String key = PT.rep(auditBlockCode, "_MOD_", "_REFRNCE_");
          if (asc.setSymmetry((SymmetryInterface) htAudit.get(key)) != null) {
            notionalUnitCell = asc.getSymmetry().getNotionalUnitCell();
            iHaveUnitCell = true;
          }
        } else if (htAudit != null) {
          if (symops != null)
            for (int i = 0; i < symops.size(); i++)
              setSymmetryOperator(symops.get(i));
        }
        if (lastSpaceGroupName != null)
          setSpaceGroupName(lastSpaceGroupName);
      } else if (key.equals(singleAtomID)) {
        readSingleAtom();
      } else if (key.startsWith("_symmetry_space_group_name_h-m")
          || key.startsWith("_symmetry_space_group_name_hall")
          || key.startsWith("_space_group_name") 
          || key.contains("_ssg_name")
          || key.contains("_magn_name") 
          || key.contains("_bns_name") // PRELIM
      ) {
        processSymmetrySpaceGroupName();
      } else if (key.startsWith("_space_group_transform")) {
        processUnitCellTransform();
      } else if (titleRecords.contains("_" + key + "__")) {
        appendLoadNote("TITLE: " + parser.fullTrim(data) + "\n");
      } else {
        processSubclassEntry();
      }
    }
    return true;
  }

  protected void processSubclassEntry() throws Exception {
    if (modDim > 0)
      getModulationReader().processEntry();
  }

  private void processUnitCellTransform() {
    data = PT.replaceAllCharacters(data, " ", "");
    if (key.contains("_from_parent"))
      addCellType("parent", data, true);
    else if (key.contains("_to_standard"))
      addCellType("standard", data, false);
    appendLoadNote(key + ": " + data);
  }

  private Map<String, String> htCellTypes;

  private void addCellType(String type, String data, boolean isFrom) {
    if (htCellTypes == null)
      htCellTypes = new Hashtable<String, String>();
    if (data.startsWith("!")) {
      data = data.substring(1);
      isFrom = !isFrom;
    }
    String cell = (isFrom ? "!" : "") + data;
    htCellTypes.put(type, cell);
    if (type.equalsIgnoreCase(strSupercell)) {
      strSupercell = cell;
      htCellTypes.put("conventional", (isFrom ? "" : "!") + data);
    }
  }

  /**
   * No need for anything other than the atom name and symbol; coordinates will
   * be (0 0 0), and no other information is needed.
   */
  private void readSingleAtom() {
    Atom atom = new Atom();
    atom.set(0, 0, 0);
    String s = atom.atomName = parser.fullTrim(data);
    atom.elementSymbol = s.length() == 1 ? s : s.substring(0, 1)
        + s.substring(1, 2).toLowerCase();
    asc.addAtom(atom);
  }

  private MSCifRdr getModulationReader() throws Exception {
    return (mr == null ? initializeMSCIF() : mr);
  }

  private MSCifRdr initializeMSCIF() throws Exception {
    if (mr == null)
      ms = mr = (MSCifRdr) getInterface("org.jmol.adapter.readers.cif.MSCifRdr");
    modulated = (mr.initialize(this, modDim) > 0);
    return mr;
  }

  public Map<String, Integer> modelMap;

  protected void newModel(int modelNo) throws Exception {
    skipping = !doGetModel(modelNumber = modelNo, null);
    if (skipping) {
      parser.getTokenPeeked();
      return;
    }
    chemicalName = "";
    thisStructuralFormula = "";
    thisFormula = "";
    iHaveDesiredModel = isLastModel(modelNumber);
    if (isCourseGrained)
      asc.setAtomSetAuxiliaryInfo("courseGrained", Boolean.TRUE);
    if (nAtoms0 == asc.ac) {
      // we found no atoms -- must revert
      modelNumber--;
      haveModel = false;
      asc.removeCurrentAtomSet();
    } else {
      applySymmetryAndSetTrajectory();
    }
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    if (isMMCIF)
      finalizeSubclass();
    else
      applySymmetryAndSetTrajectory();
    int n = asc.atomSetCount;
    if (n > 1)
      asc.setCollectionName("<collection of " + n + " models>");
    finalizeReaderASCR();
    String header = parser.getFileHeader();
    if (header.length() > 0) {
      String s = setLoadNote();
      appendLoadNote(null);
      appendLoadNote(header);
      appendLoadNote(s);
      setLoadNote();
      asc.setInfo("fileHeader", header);
    }
    if (haveAromatic)
      addJmolScript("calculate aromatic");
  }

  protected void finalizeSubclass() throws Exception {
    // MMCifReader only    
  }

  @Override
  public void doPreSymmetry() throws Exception {
    if (magCenterings != null)
      addLatticeVectors();
    if (modDim > 0)
      getModulationReader().setModulation(false, null);
    if (isMagCIF) {
      // set fractional; note that this will be set to Cartesians later
      vibsFractional = true;
    }
  }

  @Override
  public void applySymmetryAndSetTrajectory() throws Exception {
    // This speeds up calculation, because no crosschecking
    // No special-position atoms in mmCIF files, because there will
    // be no center of symmetry, no rotation-inversions, 
    // no atom-centered rotation axes, and no mirror or glide planes.
    if (isMMCIF)
      asc.setCheckSpecial(false);
    boolean doCheckBonding = doCheckUnitCell && !isMMCIF;
    if (isMMCIF) {
      int modelIndex = asc.iSet;
      asc.setAtomSetAuxiliaryInfo(
          "PDB_CONECT_firstAtom_count_max",
          new int[] { asc.getAtomSetAtomIndex(modelIndex),
              asc.getAtomSetAtomCount(modelIndex), maxSerial });
    }
    if (htCellTypes != null) {
      for (Entry<String, String> e : htCellTypes.entrySet())
        asc.setAtomSetAuxiliaryInfo("unitcell_" + e.getKey(), e.getValue());
      htCellTypes = null;
    }
    if (!haveCellWaveVector)
      modDim = 0;
    applySymTrajASCR();
    if (doCheckBonding && (bondTypes.size() > 0 || isMolecular))
      setBondingAndMolecules();
    asc.setAtomSetAuxiliaryInfo("fileHasUnitCell", Boolean.TRUE);
    asc.xtalSymmetry = null;
  }

  @Override
  protected void finalizeSubclassSymmetry(boolean haveSymmetry) throws Exception {
    // called by applySymTrajASCR();
    SymmetryInterface sym = (haveSymmetry ? asc.getXSymmetry().getBaseSymmetry() : null);
    if (modDim > 0 && sym != null) {
      addLatticeVectors();
      asc.setTensors();
      getModulationReader().setModulation(true, sym);
      mr.finalizeModulation();
    }
    if (sym != null && vibsFractional)
      asc.getXSymmetry().scaleFractionalVibs();
    if (isMagCIF)
      asc.setNoAutoBond();
      //addJmolScript("connect none;");
    if (isMagCIF && sym != null) {
      addJmolScript("vectors on;vectors 0.15;");
      int n = asc.getXSymmetry().setSpinVectors();
      appendLoadNote(n
          + " magnetic moments - use VECTORS ON/OFF or VECTOR MAX x.x or SELECT VXYZ>0");
    }
    if (sym != null && auditBlockCode != null && auditBlockCode.contains("REFRNCE")) {
      if (htAudit == null)
        htAudit = new Hashtable<String, Object>();
      htAudit.put(auditBlockCode, sym);
    }
  }


  ////////////////////////////////////////////////////////////////
  // processing methods
  ////////////////////////////////////////////////////////////////

  private Hashtable<String, Object> htAudit;
  private Lst<String> symops;

  /**
   * initialize a new atom set
   * 
   */
  private void processDataParameter() {
    bondTypes.clear();
    parser.getTokenPeeked();
    thisDataSetName = (key.length() < 6 ? "" : key.substring(5));
    if (thisDataSetName.length() > 0)
      nextAtomSet();
    if (Logger.debugging)
      Logger.debug(key);
  }

  private void nextAtomSet() {
    asc.setAtomSetAuxiliaryInfo("isCIF", Boolean.TRUE);
    if (asc.iSet >= 0) {
      // note that there can be problems with multi-data mmCIF sets each with
      // multiple models; and we could be loading multiple files!
      asc.newAtomSet();
      if (isMMCIF)
        setModelPDB(true);
    } else {
      asc.setCollectionName(thisDataSetName);
    }
  }

  /**
   * reads some of the more interesting info into specific atomSetAuxiliaryInfo
   * elements
   * 
   * @param type
   *        "name" "formula" etc.
   * @return data
   * @throws Exception
   */
  private String processChemicalInfo(String type) throws Exception {
    if (type.equals("name")) {
      chemicalName = data = parser.fullTrim(data);
      if (!data.equals("?"))
        asc.setInfo("modelLoadNote", data);
    } else if (type.equals("structuralFormula")) {
      thisStructuralFormula = data = parser.fullTrim(data);
    } else if (type.equals("formula")) {
      thisFormula = data = parser.fullTrim(data);
    }
    if (Logger.debugging) {
      Logger.debug(type + " = " + data);
    }
    return data;
  }  
  
//  _space_group.magn_ssg_name_BNS "P2_1cn1'(0,0,g)000s"
//  _space_group.magn_ssg_number_BNS 33.1.9.5.m145.?
//  _space_group.magn_point_group "mm21'"

      
  /**
   * done by AtomSetCollectionReader
   * 
   * @throws Exception
   */
  private void processSymmetrySpaceGroupName() throws Exception {
    if (key.indexOf("_ssg_name") >= 0) {
      modulated = true;
      latticeType = data.substring(0, 1);
    } else if (modulated) {
      return;
    }
    data = parser.toUnicode(data);
    setSpaceGroupName(lastSpaceGroupName = (
          key.indexOf("h-m") > 0 ? "HM:"
        : modulated ? "SSG:" 
        : key.indexOf("bns") >= 0 ? "BNS:" 
        : key.indexOf("hall") >= 0 ? "Hall:" 
        : "")
        + data);
  }

  private void addLatticeVectors() {
    lattvecs = null;
    if (magCenterings != null) {
      // could be x+1/2,y+1/2,z,+1
      // or   x+0.5,y+0.5,z,+1
      // or   0.5+x,0.5+y,z,+1
      //
      latticeType = "Magnetic";
      lattvecs = new Lst<float[]>();
      for (int i = 0; i < magCenterings.size(); i++) {
        String s = magCenterings.get(i);
        float[] f = new float[modDim + 4];
        if (s.indexOf("x1") >= 0)
          for (int j = 1; j <= modDim + 3; j++) 
            s = PT.rep(s, "x" + j, "");
        String[] tokens = PT.split(PT.replaceAllCharacters(s, "xyz+", ""), ",");
        int n = 0;
        for (int j = 0; j < tokens.length; j++) {
          s = tokens[j].trim();
          if (s.length() == 0)
            continue;
          int pt = s.indexOf("/");
          if (pt < 0) {
            pt = s.length();
            s += "/1";
          }
          if ((f[j] = PT.parseFloat(s.substring(0, pt))
              / PT.parseFloat(s.substring(pt + 1))) != 0)
            n++;
        }
        if (n >= 2) // needs to have an x y or z as well as a +/-1;
          lattvecs.addLast(f);
      }
      magCenterings = null;
    } else if (latticeType != null && "ABCFI".indexOf(latticeType) >= 0) {
      lattvecs = new Lst<float[]>();
      try {
        ms.addLatticeVector(lattvecs, latticeType);
      } catch (Exception e) {
        // n/a
      }
    }
    if (lattvecs != null && lattvecs.size() > 0
        && asc.getSymmetry().addLatticeVectors(lattvecs))
      appendLoadNote("Note! " + lattvecs.size() + " symmetry operators added for lattice centering "
          + latticeType);
    latticeType = null;
  }

  /**
   * unit cell parameters -- two options, so we use MOD 6
   * 
   * @throws Exception
   */
  private void processCellParameter() throws Exception {
    for (int i = JmolAdapter.cellParamNames.length; --i >= 0;)
      if (key.equals(JmolAdapter.cellParamNames[i])) {
        setUnitCellItem(i, parseFloatStr(data));
        return;
      }
  }

  final private static String[] TransformFields = { "x[1][1]", "x[1][2]",
      "x[1][3]", "r[1]", "x[2][1]", "x[2][2]", "x[2][3]", "r[2]", "x[3][1]",
      "x[3][2]", "x[3][3]", "r[3]", };

  /**
   * 
   * the PDB transformation matrix cartesian --> fractional
   * 
   * @throws Exception
   */
  private void processUnitCellTransformMatrix() throws Exception {
    /*
     * PDB:
     
     SCALE1       .024414  0.000000  -.000328        0.00000
     SCALE2      0.000000   .053619  0.000000        0.00000
     SCALE3      0.000000  0.000000   .044409        0.00000

     * CIF:

     _atom_sites.fract_transf_matrix[1][1]   .024414 
     _atom_sites.fract_transf_matrix[1][2]   0.000000 
     _atom_sites.fract_transf_matrix[1][3]   -.000328 
     _atom_sites.fract_transf_matrix[2][1]   0.000000 
     _atom_sites.fract_transf_matrix[2][2]   .053619 
     _atom_sites.fract_transf_matrix[2][3]   0.000000 
     _atom_sites.fract_transf_matrix[3][1]   0.000000 
     _atom_sites.fract_transf_matrix[3][2]   0.000000 
     _atom_sites.fract_transf_matrix[3][3]   .044409 
     _atom_sites.fract_transf_vector[1]      0.00000 
     _atom_sites.fract_transf_vector[2]      0.00000 
     _atom_sites.fract_transf_vector[3]      0.00000 

     */
    float v = parseFloatStr(data);
    if (Float.isNaN(v))
      return;
    //could enable EM box: notionalUnitCell[0] = 1;
    for (int i = 0; i < TransformFields.length; i++) {
      if (key.indexOf(TransformFields[i]) >= 0) {
        setUnitCellItem(6 + i, v);
        return;
      }
    }
  }

  ////////////////////////////////////////////////////////////////
  // loop_ processing
  ////////////////////////////////////////////////////////////////

  String key;
  String data;

  /**
   * 
   * @return TRUE if data, even if ''; FALSE if '.' or '?' or eof.
   * 
   * @throws Exception
   */
  private boolean getData() throws Exception {
    key = parser.getTokenPeeked();
    data = parser.getNextToken();
    //if (Logger.debugging && data != null && data.charAt(0) != '\0')
    //Logger.debug(key  + " " + data);
    if (data == null) {
      Logger.warn("CIF ERROR ? end of file; data missing: " + key);
      return false;
    }
    return (data.length() == 0 || data.charAt(0) != '\0');
  }

  /**
   * processes loop_ blocks of interest or skips the data
   * 
   * @throws Exception
   */
  private void processLoopBlock() throws Exception {
    parser.getTokenPeeked(); //loop_
    key = parser.peekToken();
    if (key == null)
      return;
    boolean isLigand = false;
    key = parser.fixKey(key);
    if (modDim > 0)
      switch (getModulationReader().processLoopBlock()) {
      case 0:
        break;
      case -1:
        parser.skipLoop();
        //$FALL-THROUGH$
      case 1:
        return;
      }
    if (key.startsWith(FAMILY_ATOM)
        || (isLigand = key.equals("_chem_comp_atom_comp_id"))) {
      if (!processAtomSiteLoopBlock(isLigand))
        return;
      asc.setAtomSetName(thisDataSetName);
      asc.setAtomSetAuxiliaryInfo("chemicalName", chemicalName);
      asc.setAtomSetAuxiliaryInfo("structuralFormula", thisStructuralFormula);
      asc.setAtomSetAuxiliaryInfo("formula", thisFormula);  
      return;
    }
    if (key.startsWith("_symmetry_equiv_pos")
        || key.startsWith(FAMILY_SGOP)
        || key.startsWith("_symmetry_ssg_equiv")) {
      if (ignoreFileSymmetryOperators) {
        Logger.warn("ignoring file-based symmetry operators");
        parser.skipLoop();
      } else {
        processSymmetryOperationsLoopBlock();
      }
      return;
    }
    if (key.startsWith("_citation")) {
      processCitationListBlock();
      return;
    }
    if (key.startsWith("_atom_type")) {
      processAtomTypeLoopBlock();
      return;
    }
    if ((isMolecular || !doApplySymmetry) && key.startsWith("_geom_bond")) {
      processGeomBondLoopBlock();
      return;
    }
    if (processSubclassLoopBlock())
      return;
    if (key.equals("_propagation_vector_seq_id")) {// Bilbao mCIF
      addMore();
      return;
    }
    parser.skipLoop();
  }

  protected boolean processSubclassLoopBlock() throws Exception {
    return false;
  }

  private void addMore() {
    String str;
    int n = 0;
    try {
      while ((str = parser.peekToken()) != null && str.charAt(0) == '_') {
        parser.getTokenPeeked();
        n++;
      }
      int m = 0;
      String s = "";
      while ((str = parser.getNextDataToken()) != null) {
        s += str + (m % n == 0 ? "=" : " ");
        if (++m % n == 0) {
          appendUunitCellInfo(s.trim());
          s = "";
        }
      }
    } catch (Exception e) {
    }
  }

  protected int fieldProperty(int i) {
    return (i >= 0 && (field = parser.getLoopData(i)).length() > 0
        && (firstChar = field.charAt(0)) != '\0' ? propertyOf[i] : NONE);
  }

  int[] propertyOf = new int[100]; // should be enough
  int[] fieldOf = new int[100];
  String field;
  protected char firstChar = '\0';
  protected int propertyCount;

  /**
   * sets up arrays and variables for tokenizer.getData()
   * 
   * @param fields
   * @throws Exception
   */
  void parseLoopParameters(String[] fields) throws Exception {
    propertyCount = parser.parseLoopParameters(fields, fieldOf, propertyOf);
  }

  void parseLoopParametersFor(String key, String[] fields) throws Exception {
    // just once for static fields
    // first field must start with * if any do
    if (fields[0].charAt(0) == '*')
      for (int i = fields.length; --i >= 0;)
        if (fields[i].charAt(0) == '*')
          fields[i] = key + fields[i].substring(1);
    parseLoopParameters(fields);
  }

  /**
   * 
   * used for turning off fractional or nonfractional coord.
   * 
   * @param fieldIndex
   */
  private void disableField(int fieldIndex) {
    int i = fieldOf[fieldIndex];
    if (i != NONE)
      propertyOf[i] = NONE;
  }

  ////////////////////////////////////////////////////////////////
  // atom type data
  ////////////////////////////////////////////////////////////////

  private Map<String, Float> atomTypes;
  private Lst<Object[]> bondTypes = new Lst<Object[]>();

  private String disorderAssembly = ".";
  private String lastDisorderAssembly;
  private Lst<float[]> lattvecs;
  private Lst<String> magCenterings;
  private int maxSerial;

  final private static byte ATOM_TYPE_SYMBOL = 0;
  final private static byte ATOM_TYPE_OXIDATION_NUMBER = 1;

  final private static String[] atomTypeFields = { 
    "_atom_type_symbol",
    "_atom_type_oxidation_number"
  };

  /**
   * 
   * reads the oxidation number and associates it with an atom name, which can
   * then later be associated with the right atom indirectly.
   * 
   * @throws Exception
   */
  private void processAtomTypeLoopBlock() throws Exception {
    parseLoopParameters(atomTypeFields);
    for (int i = propertyCount; --i >= 0;)
      if (fieldOf[i] == NONE) {
        parser.skipLoop();
        return;
      }

    while (parser.getData()) {
      String atomTypeSymbol = null;
      float oxidationNumber = Float.NaN;
      int n = parser.getFieldCount();
      for (int i = 0; i < n; ++i) {
        switch (fieldProperty(i)) {
        case NONE:
          break;
        case ATOM_TYPE_SYMBOL:
          atomTypeSymbol = field;
          break;
        case ATOM_TYPE_OXIDATION_NUMBER:
          oxidationNumber = parseFloatStr(field);
          break;
        }
      }
      if (atomTypeSymbol == null || Float.isNaN(oxidationNumber))
        continue;
      if (atomTypes == null)
        atomTypes = new Hashtable<String, Float>();
      atomTypes.put(atomTypeSymbol, Float.valueOf(oxidationNumber));
    }
  }

  ////////////////////////////////////////////////////////////////
  // atom site data
  ////////////////////////////////////////////////////////////////

  protected final static byte NONE = -1;
  final private static byte TYPE_SYMBOL = 0;
  final private static byte LABEL = 1;
  final private static byte AUTH_ATOM = 2;
  final private static byte FRACT_X = 3;
  final private static byte FRACT_Y = 4;
  final private static byte FRACT_Z = 5;
  final private static byte CARTN_X = 6;
  final private static byte CARTN_Y = 7;
  final private static byte CARTN_Z = 8;
  final private static byte OCCUPANCY = 9;
  final private static byte B_ISO = 10;
  final private static byte COMP_ID = 11;
  final private static byte AUTH_ASYM_ID = 12;
  final private static byte AUTH_SEQ_ID = 13;
  final private static byte INS_CODE = 14;
  final private static byte ALT_ID = 15;
  final private static byte GROUP_PDB = 16;
  final private static byte MODEL_NO = 17;
  final private static byte DUMMY_ATOM = 18;

  final private static byte DISORDER_GROUP = 19;
  final private static byte ANISO_LABEL = 20;
  final private static byte ANISO_MMCIF_ID = 21;
  final private static byte ANISO_U11 = 22;
  final private static byte ANISO_U22 = 23;
  final private static byte ANISO_U33 = 24;
  final private static byte ANISO_U12 = 25;
  final private static byte ANISO_U13 = 26;
  final private static byte ANISO_U23 = 27;
  final private static byte ANISO_MMCIF_U11 = 28;
  final private static byte ANISO_MMCIF_U22 = 29;
  final private static byte ANISO_MMCIF_U33 = 30;
  final private static byte ANISO_MMCIF_U12 = 31;
  final private static byte ANISO_MMCIF_U13 = 32;
  final private static byte ANISO_MMCIF_U23 = 33;
  final private static byte U_ISO_OR_EQUIV = 34;
  final private static byte ANISO_B11 = 35;
  final private static byte ANISO_B22 = 36;
  final private static byte ANISO_B33 = 37;
  final private static byte ANISO_B12 = 38;
  final private static byte ANISO_B13 = 39;
  final private static byte ANISO_B23 = 40;
  final private static byte ANISO_BETA_11 = 41;
  final private static byte ANISO_BETA_22 = 42;
  final private static byte ANISO_BETA_33 = 43;
  final private static byte ANISO_BETA_12 = 44;
  final private static byte ANISO_BETA_13 = 45;
  final private static byte ANISO_BETA_23 = 46;
  final private static byte ADP_TYPE = 47;
  final private static byte CC_COMP_ID = 48;
  final private static byte CC_ATOM_ID = 49;
  final private static byte CC_ATOM_SYM = 50;
  final private static byte CC_ATOM_CHARGE = 51;
  final private static byte CC_ATOM_X = 52;
  final private static byte CC_ATOM_Y = 53;
  final private static byte CC_ATOM_Z = 54;
  final private static byte CC_ATOM_X_IDEAL = 55;
  final private static byte CC_ATOM_Y_IDEAL = 56;
  final private static byte CC_ATOM_Z_IDEAL = 57;
  final private static byte DISORDER_ASSEMBLY = 58;
  final private static byte ASYM_ID = 59;
  final private static byte SUBSYS_ID = 60;
  final private static byte SITE_MULT = 61;
  final private static byte THERMAL_TYPE = 62;
  final private static byte MOMENT_LABEL = 63;
  final private static byte MOMENT_PRELIM_X = 64;
  final private static byte MOMENT_PRELIM_Y = 65;
  final private static byte MOMENT_PRELIM_Z = 66;
  final private static byte MOMENT_X = 67;
  final private static byte MOMENT_Y = 68;
  final private static byte MOMENT_Z = 69;
  final private static byte ATOM_ID = 70;
  final private static byte SEQ_ID = 71; 
  final protected static String FAMILY_ATOM = "_atom_site";
  final private static String[] atomFields = { "*_type_symbol",
      "*_label", "*_auth_atom_id", "*_fract_x",
      "*_fract_y", "*_fract_z", "*_cartn_x",
      "*_cartn_y", "*_cartn_z", "*_occupancy",
      "*_b_iso_or_equiv", "*_auth_comp_id",
      "*_auth_asym_id", "*_auth_seq_id",
      "*_pdbx_pdb_ins_code", "*_label_alt_id",
      "*_group_pdb", "*_pdbx_pdb_model_num",
      "*_calc_flag", "*_disorder_group",
      "*_aniso_label", "*_anisotrop_id",
      "*_aniso_u_11", "*_aniso_u_22",
      "*_aniso_u_33", "*_aniso_u_12",
      "*_aniso_u_13", "*_aniso_u_23",
      "*_anisotrop_u[1][1]", "*_anisotrop_u[2][2]",
      "*_anisotrop_u[3][3]", "*_anisotrop_u[1][2]",
      "*_anisotrop_u[1][3]", "*_anisotrop_u[2][3]",
      "*_u_iso_or_equiv", "*_aniso_b_11",
      "*_aniso_b_22", "*_aniso_b_33",
      "*_aniso_b_12", "*_aniso_b_13",
      "*_aniso_b_23", "*_aniso_beta_11",
      "*_aniso_beta_22", "*_aniso_beta_33",
      "*_aniso_beta_12", "*_aniso_beta_13",
      "*_aniso_beta_23", "*_adp_type",
      "_chem_comp_atom_comp_id", "_chem_comp_atom_atom_id",
      "_chem_comp_atom_type_symbol", "_chem_comp_atom_charge",
      "_chem_comp_atom_model_cartn_x", "_chem_comp_atom_model_cartn_y",
      "_chem_comp_atom_model_cartn_z",
      "_chem_comp_atom_pdbx_model_cartn_x_ideal",
      "_chem_comp_atom_pdbx_model_cartn_y_ideal",
      "_chem_comp_atom_pdbx_model_cartn_z_ideal",
      "*_disorder_assembly", "*_label_asym_id",
      "*_subsystem_code", "*_symmetry_multiplicity",
      "*_thermal_displace_type", "*_moment_label",
      "*_moment_crystalaxis_mx", "*_moment_crystalaxis_my",
      "*_moment_crystalaxis_mz", "*_moment_crystalaxis_x",
      "*_moment_crystalaxis_y", "*_moment_crystalaxis_z",
      "*_id", "*_label_seq_id" };

  final private static String singleAtomID = atomFields[CC_COMP_ID];

  /* to: hansonr@stolaf.edu
   * from: Zukang Feng zfeng@rcsb.rutgers.edu
   * re: Two mmCIF issues
   * date: 4/18/2006 10:30 PM
   * "You should always use _atom_site.auth_asym_id for PDB chain IDs."
   * 
   * 
   */

  /**
   * reads atom data in any order
   * 
   * @param isLigand
   * 
   * @return TRUE if successful; FALS if EOF encountered
   * @throws Exception
   */
  boolean processAtomSiteLoopBlock(boolean isLigand) throws Exception {
    int currentModelNo = -1; // PDBX
    boolean haveCoord = true;
    parseLoopParametersFor(FAMILY_ATOM, atomFields);
    if (fieldOf[CC_ATOM_X_IDEAL] != NONE) {
      setFractionalCoordinates(false);
    } else if (fieldOf[CARTN_X] != NONE || fieldOf[CC_ATOM_X] != NONE) {
      setFractionalCoordinates(false);
      disableField(FRACT_X);
      disableField(FRACT_Y);
      disableField(FRACT_Z);
    } else if (fieldOf[FRACT_X] != NONE) {
      setFractionalCoordinates(true);
      disableField(CARTN_X);
      disableField(CARTN_Y);
      disableField(CARTN_Z);
    } else if (fieldOf[ANISO_LABEL] != NONE || fieldOf[ANISO_MMCIF_ID] != NONE
        || fieldOf[MOMENT_LABEL] != NONE) {
      haveCoord = false;
      // no coordinates, but valuable information
    } else {
      // it is a different kind of _atom_site loop block
      parser.skipLoop();
      return false;
    }
    int modelField = fieldOf[MODEL_NO];
    int siteMult = 0;
    while (parser.getData()) {
      if (isMMCIF) {
        if (modelField >= 0) {
          fieldProperty(modelField);
          int modelNo = parseIntStr(field);
          if (modelNo != currentModelNo) {
            if (iHaveDesiredModel && asc.atomSetCount > 0) {
              parser.skipLoop();
              // but only this atom loop
              skipping = false;
              continuing = true;
              break;
            }
            currentModelNo = modelNo;
            newModel(modelNo);
            if (!skipping) {
              nextAtomSet();
              if (modelMap == null || asc.ac == 0)
                modelMap = new Hashtable<String, Integer>();
              modelMap.put("" + modelNo, Integer.valueOf(Math.max(0, asc.iSet)));
              modelMap
                  .put("_" + Math.max(0, asc.iSet), Integer.valueOf(modelNo));
            }
          }
          if (skipping)
            continue;
        }
      }
      Atom atom = null;
      if (haveCoord) {
        atom = new Atom();
      } else {
        if (fieldProperty(fieldOf[ANISO_LABEL]) != NONE
            || fieldProperty(fieldOf[MOMENT_LABEL]) != NONE
            || fieldProperty(fieldOf[ANISO_MMCIF_ID]) != NONE) {
          if ((atom = asc.getAtomFromName(field)) == null)
            continue; // atom has been filtered out
        } else {
          continue;
        }
      }
      String assemblyId = null;
      String strChain = null;
      String id = null;
      int seqID = 0;
      int n = parser.getFieldCount();
      for (int i = 0; i < n; ++i) {
        int tok = fieldProperty(i);
        switch (tok) {
        case NONE:
          break;
        case SEQ_ID:
          seqID = parseIntStr(field);
          break;
        case ATOM_ID:
          id = field;
          break;
        case CC_ATOM_SYM:
        case TYPE_SYMBOL:
          String elementSymbol;
          if (field.length() < 2) {
            elementSymbol = field;
          } else {
            char ch1 = Character.toLowerCase(field.charAt(1));
            if (Atom.isValidElementSymbol2(firstChar, ch1))
              elementSymbol = "" + firstChar + ch1;
            else
              elementSymbol = "" + firstChar;
          }
          atom.elementSymbol = elementSymbol;
          if (atomTypes != null && atomTypes.containsKey(field)) {
            float charge = atomTypes.get(field).floatValue();
            atom.formalCharge = Math.round(charge);
            //because otherwise -1.6 is rounded UP to -1, and  1.6 is rounded DOWN to 1
            if (Math.abs(atom.formalCharge - charge) > 0.1)
              if (Logger.debugging) {
                Logger.debug("CIF charge on " + field + " was " + charge
                    + "; rounded to " + atom.formalCharge);
              }
          }
          break;
        case CC_ATOM_ID:
        case LABEL:
        case AUTH_ATOM:
          atom.atomName = field;
          break;
        case CC_ATOM_X_IDEAL:
          float x = parseFloatStr(field);
          if (readIdeal && !Float.isNaN(x))
            atom.x = x;
          break;
        case CC_ATOM_Y_IDEAL:
          float y = parseFloatStr(field);
          if (readIdeal && !Float.isNaN(y))
            atom.y = y;
          break;
        case CC_ATOM_Z_IDEAL:
          float z = parseFloatStr(field);
          if (readIdeal && !Float.isNaN(z))
            atom.z = z;
          break;
        case CC_ATOM_X:
        case CARTN_X:
        case FRACT_X:
          atom.x = parseFloatStr(field);
          break;
        case CC_ATOM_Y:
        case CARTN_Y:
        case FRACT_Y:
          atom.y = parseFloatStr(field);
          break;
        case CC_ATOM_Z:
        case CARTN_Z:
        case FRACT_Z:
          atom.z = parseFloatStr(field);
          break;
        case CC_ATOM_CHARGE:
          atom.formalCharge = parseIntStr(field);
          break;
        case OCCUPANCY:
          float floatOccupancy = parseFloatStr(field);
          if (!Float.isNaN(floatOccupancy))
            atom.foccupancy = floatOccupancy;
          break;
        case B_ISO:
          atom.bfactor = parseFloatStr(field) * (isMMCIF ? 1 : 100f);
          break;
        case CC_COMP_ID:
        case COMP_ID:
          atom.group3 = field;
          break;
        case ASYM_ID:
          assemblyId = field;
          if (!useAuthorChainID)
            setChainID(atom, strChain = field);            
          break;
        case AUTH_ASYM_ID:
          if (useAuthorChainID)
            setChainID(atom, strChain = field);            
          break;
        case AUTH_SEQ_ID:
          maxSerial = Math.max(maxSerial,
          atom.sequenceNumber = parseIntStr(field));
          break;
        case INS_CODE:
          atom.insertionCode = firstChar;
          break;
        case ALT_ID:
        case SUBSYS_ID:
          atom.altLoc = firstChar;
          break;
        case DISORDER_ASSEMBLY:
          disorderAssembly = field;
          break;
        case DISORDER_GROUP:
          if (firstChar == '-' && field.length() > 1) {
            atom.altLoc = field.charAt(1);
            atom.ignoreSymmetry = true;
          } else {
            atom.altLoc = firstChar;
          }
          break;
        case GROUP_PDB:
          if ("HETATM".equals(field))
            atom.isHetero = true;
          break;
        case DUMMY_ATOM:
          //see http://www.iucr.org/iucr-top/cif/cifdic_html/
          //            1/cif_core.dic/Iatom_site_calc_flag.html
          if ("dum".equals(field)) {
            atom.x = Float.NaN;
            continue; //skip 
          }
          break;
        case SITE_MULT:
          if (modulated)
            siteMult = parseIntStr(field);
          break;
        case THERMAL_TYPE:
        case ADP_TYPE:
          if (field.equalsIgnoreCase("Uiso")) {
            int j = fieldOf[U_ISO_OR_EQUIV];
            if (j != NONE)
              asc.setU(atom, 7, parseFloatStr(parser.getLoopData(j)));
          }
          break;
        case ANISO_U11:
        case ANISO_U22:
        case ANISO_U33:
        case ANISO_U12:
        case ANISO_U13:
        case ANISO_U23:
        case ANISO_MMCIF_U11:
        case ANISO_MMCIF_U22:
        case ANISO_MMCIF_U33:
        case ANISO_MMCIF_U12:
        case ANISO_MMCIF_U13:
        case ANISO_MMCIF_U23:
          // Ortep Type 8: D = 2pi^2, C = 2, a*b*
          asc.setU(atom, (propertyOf[i] - ANISO_U11) % 6, parseFloatStr(field));
          break;
        case ANISO_B11:
        case ANISO_B22:
        case ANISO_B33:
        case ANISO_B12:
        case ANISO_B13:
        case ANISO_B23:
          // Ortep Type 4: D = 1/4, C = 2, a*b*
          asc.setU(atom, 6, 4);
          asc.setU(atom, (propertyOf[i] - ANISO_B11) % 6, parseFloatStr(field));
          break;
        case ANISO_BETA_11:
        case ANISO_BETA_22:
        case ANISO_BETA_33:
        case ANISO_BETA_12:
        case ANISO_BETA_13:
        case ANISO_BETA_23:
          //Ortep Type 0: D = 1, c = 2 -- see org.jmol.symmetry/UnitCell.java
          asc.setU(atom, 6, 0);
          asc.setU(atom, (propertyOf[i] - ANISO_BETA_11) % 6,
              parseFloatStr(field));
          break;
        case MOMENT_PRELIM_X:
        case MOMENT_PRELIM_Y:
        case MOMENT_PRELIM_Z:
        case MOMENT_X:
        case MOMENT_Y:
        case MOMENT_Z:
          isMagCIF = true;
          V3 pt = atom.vib;
          if (pt == null)
            atom.vib = pt = new Vibration().setType(Vibration.TYPE_SPIN);
          float v = parseFloatStr(field);
          switch (tok) {
          case MOMENT_PRELIM_X:
          case MOMENT_X:
            pt.x = v;
            appendLoadNote("magnetic moment: " + line);
            break;
          case MOMENT_PRELIM_Y:
          case MOMENT_Y:
            pt.y = v;
            break;
          case MOMENT_PRELIM_Z:
          case MOMENT_Z:
            pt.z = v;
            break;
          }
          break;
        }
      }
      if (!haveCoord)
        continue;
      if (Float.isNaN(atom.x) || Float.isNaN(atom.y) || Float.isNaN(atom.z)) {
        Logger.warn("atom " + atom.atomName
            + " has invalid/unknown coordinates");
        continue;
      }
      if (atom.elementSymbol == null && atom.atomName != null) {
        String sym = atom.atomName;
        int pt = 0;
        while (pt < sym.length() && PT.isLetter(sym.charAt(pt)))
          pt++;
        atom.elementSymbol = (pt == 0 || pt > 2 ? "Xx" : sym.substring(0, pt));
      }
      if (!filterCIFAtom(atom, assemblyId))
        continue;
      setAtomCoord(atom);
      if (isMMCIF && !processSubclassAtom(atom, assemblyId, strChain))
        continue;
      asc.addAtomWithMappedName(atom);
      if (id != null) {
        asc.atomSymbolicMap.put(id, atom);
        if (seqID > 0) {
          V3 pt = atom.vib;
          if (pt == null)
            pt = asc.addVibrationVector(atom.index, 0, Float.NaN, T.seqid);
          pt.x = seqID;
        }
      }
      ac++;
      if (modDim > 0 && siteMult != 0)
        atom.vib = V3.new3(siteMult, 0, Float.NaN);
    }
    asc.setAtomSetAuxiliaryInfo("isCIF", Boolean.TRUE);
    if (isMMCIF)
      setModelPDB(true);
    if (isMMCIF && skipping)
      skipping = false;
    return true;
  }

//  private  Lst<Map<String, String>> vCompnds;
//  private Map<String, Map<String, String>> htModels;
  
  /**
   * @param atom  
   * @param assemblyId 
   * @param strChain
   * @return true if valid atom
   */
  protected boolean processSubclassAtom(Atom atom, String assemblyId, String strChain) {
    return false; // MMCifReader only
  }

  protected boolean filterCIFAtom(Atom atom, String assemblyId) {
    if (!filterAtom(atom, -1))
      return false;
    if (filterAssembly && filterReject(filter, "$", assemblyId))
      return false;
    if (configurationPtr > 0) {
      if (!disorderAssembly.equals(lastDisorderAssembly)) {
        lastDisorderAssembly = disorderAssembly;
        lastAltLoc = '\0';
        conformationIndex = configurationPtr;
      }
      // ignore atoms that have no designation
      if (atom.altLoc != '\0') {
        // count down until we get the desired index into the list
        if (conformationIndex >= 0 && atom.altLoc != lastAltLoc) {
          lastAltLoc = atom.altLoc;
          conformationIndex--;
        }
        if (conformationIndex != 0) {
          Logger.info("ignoring " + atom.atomName);
          return false;
        }
      }
    }
    return true;
  }

  final private static byte CITATION_ID = 0;
  final private static byte CITATION_TITLE = 1;

  final private static String[] citationFields = { 
    "_citation_id",
    "_citation_title"
  };

  private void processCitationListBlock() throws Exception {
    parseLoopParameters(citationFields);
    float[] m = new float[16];
    m[15] = 1;
    while (parser.getData()) {
      int n = parser.getFieldCount();
      for (int i = 0; i < n; ++i) {
        switch (fieldProperty(i)) {
        case CITATION_ID:
          break;
        case CITATION_TITLE:
          appendLoadNote("TITLE: " + parser.toUnicode(field));
          break;
        }
      }
    }
  }

  ////////////////////////////////////////////////////////////////
  // symmetry operations
  ////////////////////////////////////////////////////////////////

  final private static byte SYMOP_XYZ = 0;
  final private static byte SYM_EQUIV_XYZ = 1;
  final private static byte SYM_SSG_XYZ = 2;
  final private static byte SYM_SSG_OP = 3;
  final private static byte SYM_MAGN_OP = 4;
  final private static byte SYM_PRELIM_REV = 5;
  final private static byte SYM_MAGN_SSG_XYZ = 6;
  final private static byte SYM_MAGN_SSG_OP = 7;
  final private static byte SYM_MAGN_REV = 8;
  final private static byte SYM_MAGN_SSG_REV = 9;
  final private static String FAMILY_SGOP = "_space_group_symop";
  final private static String[] symmetryOperationsFields = {
      "*_operation_xyz", 
      "_symmetry_equiv_pos_as_xyz",
      "_symmetry_ssg_equiv_pos_as_xyz",
      "*_ssg_operation_algebraic",
      "*_magn_operation_xyz",
      "*_magn_ssg_centering_algebraic",
      "*_operation_timereversal", // preliminary only
      "*_magn_ssg_operation_algebraic",
      "*_magn_operation_timereversal",
      "*_magn_ssg_operation_timereversal"
  };

  /**
   * retrieves symmetry operations
   * 
   * @throws Exception
   */
  private void processSymmetryOperationsLoopBlock() throws Exception {
    if (key.endsWith("magn_centering_id") || key.endsWith("magn_ssg_centering_id")) {
      processMagCenteringLoopBlock();
      return;
    }
    parseLoopParametersFor(FAMILY_SGOP, symmetryOperationsFields);
    int nRefs = 0;
    symops = new Lst<String>();
    for (int i = propertyCount; --i >= 0;)
      if (fieldOf[i] != NONE)
        nRefs++;
    if (nRefs == 0) {
      Logger
          .warn("?que? _symmetry_equiv or _space_group_symop property not found");
      parser.skipLoop();
      return;
    }
    int n = 0;
    boolean isMag = false;
    while (parser.getData()) {
      boolean ssgop = false;
      int nn = parser.getFieldCount();
      int timeRev = (fieldProperty(fieldOf[SYM_PRELIM_REV]) == NONE
          && fieldProperty(fieldOf[SYM_MAGN_REV]) == NONE
              && fieldProperty(fieldOf[SYM_MAGN_SSG_REV]) == NONE
          ? 0 : field
          .equals("-1") ? -1 : 1);
      for (int i = 0, tok; i < nn; ++i) {
        switch (tok = fieldProperty(i)) {
        case SYM_SSG_XYZ:
          // check for non-standard record x~1~,x~2~,x~3~,x~4~  kIsfCqpM.cif
          if (field.indexOf('~') >= 0)
            field = PT.rep(field, "~", "");
          //$FALL-THROUGH$
        case SYM_SSG_OP:
        case SYM_MAGN_SSG_XYZ:
        case SYM_MAGN_SSG_OP:
          modulated = true;
          ssgop = true;
          //$FALL-THROUGH$
        case SYMOP_XYZ:
        case SYM_EQUIV_XYZ:
        case SYM_MAGN_OP:
          if (allowRotations || timeRev != 0 || ++n == 1)
            if (!modulated || ssgop) {
              if (tok == SYM_MAGN_OP || tok == SYM_MAGN_SSG_XYZ) {
                isMag = true;
                timeRev = (field.endsWith(",+1") || field.endsWith(",1")? 1
                    : field.endsWith(",-1") ? -1 : 0);
                if (timeRev != 0)
                  field = field.substring(0, field.lastIndexOf(','));
              }
              if (timeRev != 0)
                field += ",m" + (timeRev == 1 ? "+1" : "-1");
              field = field.replace(';', ' ');
              symops.addLast(field);
              setSymmetryOperator(field);
            }
          break;
        }
      }
    }
    if (ms != null && !isMag)
      addLatticeVectors();
  }

  final static byte MAGN_CENTERING = 0;
  final static byte MAGN_SSG_CENTERING = 1;

  final private static String[] magnCenteringFields = { 
      "_space_group_symop_magn_centering_xyz",
      "_space_group_symop_magn_ssg_centering_xyz"
    };

  //loop_
  //_space_group_symop.magn_centering_id
  //_space_group_symop.magn_centering_xyz
 
  
  //_space_group_symop.magn_ssg_centering_id
  //_space_group_symop.magn_ssg_centering_xyz

//
  //1 x+2/3,y+1/3,z+1/3,+1 mx,my,mz
  //2 x+1/3,y+2/3,z+2/3,+1 mx,my,mz
  //3 x,y,z,+1 mx,my,mz

  // 1 x1,x2,x3,x4,+1
  // 2 x1,x2,x3,x4+1/2,-1 

  
  private void processMagCenteringLoopBlock() throws Exception {
    parseLoopParameters(magnCenteringFields);
    magCenterings = new Lst<String>();
    while (parser.getData())
      magCenterings.addLast(fieldProperty(fieldOf[MAGN_CENTERING]) == NONE 
      && fieldProperty(fieldOf[MAGN_SSG_CENTERING]) == NONE ? null
          : field);
  }

  public int getBondOrder(String field) {
    switch (field.charAt(0)) {
    default:
      Logger.warn("unknown CIF bond order: " + field);
      //$FALL-THROUGH$
    case 'S':
      return JmolAdapter.ORDER_COVALENT_SINGLE;
    case 'D':
      return JmolAdapter.ORDER_COVALENT_DOUBLE;
    case 'T':
      return JmolAdapter.ORDER_COVALENT_TRIPLE;
    case 'A':
      haveAromatic = true;
      return JmolAdapter.ORDER_AROMATIC;
    }
  }

  final private static byte GEOM_BOND_ATOM_SITE_LABEL_1 = 0;
  final private static byte GEOM_BOND_ATOM_SITE_LABEL_2 = 1;
  final private static byte GEOM_BOND_DISTANCE = 2;
  final private static byte CCDC_GEOM_BOND_TYPE = 3;

  //final private static byte GEOM_BOND_SITE_SYMMETRY_2 = 3;

  final private static String[] geomBondFields = {
      "_geom_bond_atom_site_label_1", 
      "_geom_bond_atom_site_label_2",
      "_geom_bond_distance", 
      "_ccdc_geom_bond_type"
  //  "_geom_bond_site_symmetry_2",
  };

  /**
   * 
   * reads bond data -- N_ijk symmetry business is ignored, so we only indicate
   * bonds within the unit cell to just the original set of atoms. "connect"
   * script or "set forceAutoBond" will override these values, but see below.
   * 
   * @throws Exception
   */
  private void processGeomBondLoopBlock() throws Exception {
    parseLoopParameters(geomBondFields);
    for (int i = propertyCount; --i >= 0;)
      if (propertyOf[i] != CCDC_GEOM_BOND_TYPE && fieldOf[i] == NONE) {
        Logger.warn("?que? missing property: " + geomBondFields[i]);
        parser.skipLoop();
        return;
      }

    String name1 = null;
    String name2 = null;
    Integer order = Integer.valueOf(1);
    int bondCount = 0;
    while (parser.getData()) {
      int atomIndex1 = -1;
      int atomIndex2 = -1;
      float distance = 0;
      float dx = 0;
      //String siteSym2 = null;
      int nn = parser.getFieldCount();
      for (int i = 0; i < nn; ++i) {
        switch (fieldProperty(i)) {
        case NONE:
          break;
        case GEOM_BOND_ATOM_SITE_LABEL_1:
          atomIndex1 = asc.getAtomIndex(name1 = field);
          break;
        case GEOM_BOND_ATOM_SITE_LABEL_2:
          atomIndex2 = asc.getAtomIndex(name2 = field);
          break;
        case GEOM_BOND_DISTANCE:
          distance = parseFloatStr(field);
          int pt = field.indexOf('(');
          if (pt >= 0) {
            char[] data = field.toCharArray();
            // 3.567(12) --> 0.012
            String sdx = field.substring(pt + 1, field.length() - 1);
            int n = sdx.length();
            for (int j = pt; --j >= 0;) {
              if (data[j] == '.')
                --j;
              data[j] = (--n < 0 ? '0' : sdx.charAt(n));
            }
            dx = parseFloatStr(String.valueOf(data));
            if (Float.isNaN(dx)) {
              Logger.info("error reading uncertainty for " + line);
              dx = 0.015f;
            }
            // TODO -- this is the full +/- (dx) in x.xxx(dx) -- is that too large?
          } else {
            dx = 0.015f;
          }
          break;
        case CCDC_GEOM_BOND_TYPE:
          order = Integer.valueOf(getBondOrder(field));
          break;
        //case GEOM_BOND_SITE_SYMMETRY_2:
        //siteSym2 = field;
        //break;
        }
      }
      if (atomIndex1 < 0 || atomIndex2 < 0 || distance == 0)
        continue;
      bondCount++;
      bondTypes.addLast(new Object[] { name1, name2, Float.valueOf(distance),
          Float.valueOf(dx), order });
    }
    if (bondCount > 0) {
      Logger.info(bondCount + " bonds read");
      if (!doApplySymmetry) {
        isMolecular = true;
        doApplySymmetry = true;
        latticeCells[0] = latticeCells[1] = latticeCells[2] = 1;
      }
    }
  }

  /////////////////////////////////////
  //  bonding and molecular 
  /////////////////////////////////////

  private float[] atomRadius;
  private BS[] bsConnected;
  private BS[] bsSets;
  final private P3 ptOffset = new P3();
  private BS bsMolecule;
  private BS bsExclude;
  private int firstAtom;

  private Atom[] atoms;

  /**
   * (1) If GEOM_BOND records are present, we (a) use them to generate bonds (b)
   * add H atoms to bonds if necessary (c) turn off autoBonding ("hasBonds") (2)
   * If MOLECULAR, then we (a) use {1 1 1} if lattice is not defined (b) use
   * asc.bonds[] to construct a preliminary molecule and connect as we go (c)
   * check symmetry for connections to molecule in any one of the 27 3x3
   * adjacent cells (d) move those atoms and their connected branch set (e)
   * iterate as necessary to get all atoms desired (f) delete unselected atoms
   * (g) set all coordinates as Cartesians (h) remove all unit cell information
   */
  private void setBondingAndMolecules() {
    Logger.info("CIF creating molecule "
        + (bondTypes.size() > 0 ? " using GEOM_BOND records" : ""));
    atoms = asc.atoms;
    firstAtom = asc.getLastAtomSetAtomIndex();
    int nat = asc.getLastAtomSetAtomCount();
    ac = firstAtom + nat;

    // get list of sites based on atom names

    bsSets = new BS[nat];
    symmetry = asc.getSymmetry();
    for (int i = firstAtom; i < ac; i++) {
      int ipt = asc.getAtomFromName(atoms[i].atomName).index - firstAtom;
      if (bsSets[ipt] == null)
        bsSets[ipt] = new BS();
      bsSets[ipt].set(i - firstAtom);
    }

    // if molecular, we need atom connection lists and radii

    if (isMolecular) {
      atomRadius = new float[ac];
      for (int i = firstAtom; i < ac; i++) {
        int elemnoWithIsotope = JmolAdapter.getElementNumber(atoms[i]
            .getElementSymbol());
        atoms[i].elementNumber = (short) elemnoWithIsotope;
        int charge = (atoms[i].formalCharge == Integer.MIN_VALUE ? 0
            : atoms[i].formalCharge);
        if (elemnoWithIsotope > 0)
          atomRadius[i] = JmolAdapter.getBondingRadius(elemnoWithIsotope,
              charge);
      }
      bsConnected = new BS[ac];
      for (int i = firstAtom; i < ac; i++)
        bsConnected[i] = new BS();

      // Set up a working set of atoms in the "molecule".

      bsMolecule = new BS();

      // Set up a working set of atoms that should be excluded 
      // because they would map onto an equivalent atom's position.

      bsExclude = new BS();
    }

    boolean isFirst = true;
    while (createBonds(isFirst)) {
      isFirst = false;
      // main loop continues until no new atoms are found
    }

    if (isMolecular) {

      // Set bsAtoms to control which atoms and 
      // bonds are delivered by the iterators.

      if (asc.bsAtoms == null)
        asc.bsAtoms = new BS();
      asc.bsAtoms.clearBits(firstAtom, ac);
      asc.bsAtoms.or(bsMolecule);
      asc.bsAtoms.andNot(bsExclude);

      // Set atom positions to be Cartesians and clear out unit cell
      // so that the model displays without it.

      for (int i = firstAtom; i < ac; i++) {
        if (asc.bsAtoms.get(i))
          symmetry.toCartesian(atoms[i], true);
        else if (Logger.debugging)
          Logger.debug(molecularType + " removing " + i + " "
              + atoms[i].atomName + " " + atoms[i]);
      }
      asc.setAtomSetAuxiliaryInfo("notionalUnitcell", null);
      if (nMolecular++ == asc.iSet) {
        asc.clearGlobalBoolean(AtomSetCollection.GLOBAL_FRACTCOORD);
        asc.clearGlobalBoolean(AtomSetCollection.GLOBAL_SYMMETRY);
        asc.clearGlobalBoolean(AtomSetCollection.GLOBAL_UNITCELLS);
      }

    }

    // Set return info to enable desired defaults.

    if (bondTypes.size() > 0)
      asc.setAtomSetAuxiliaryInfo("hasBonds", Boolean.TRUE);

    // Clear temporary fields.

    bondTypes.clear();
    atomRadius = null;
    bsSets = null;
    bsConnected = null;
    bsMolecule = null;
    bsExclude = null;
  }

  /**
   * Use the site bitset to check for atoms that are within +/-dx Angstroms of
   * the specified distances in GEOM_BOND where dx is determined by the
   * uncertainty (dx) in the record. Note that this also "connects" the atoms
   * that might have been moved in a previous iteration.
   * 
   * Also connect H atoms based on a distance <= 1.1 Angstrom from a nearby
   * atom.
   * 
   * Then create molecules.
   * 
   * @param doInit
   * @return TRUE if need to continue
   */
  private boolean createBonds(boolean doInit) {

    // process GEOM_BOND records
    for (int i = bondTypes.size(); --i >= 0;) {
      Object[] o = bondTypes.get(i);
      float distance = ((Float) o[2]).floatValue();
      float dx = ((Float) o[3]).floatValue();
      int order = ((Integer) o[4]).intValue();
      int iatom1 = asc.getAtomIndex((String) o[0]);
      int iatom2 = asc.getAtomIndex((String) o[1]);
      BS bs1 = bsSets[iatom1 - firstAtom];
      BS bs2 = bsSets[iatom2 - firstAtom];
      if (bs1 == null || bs2 == null)
        continue;
      for (int j = bs1.nextSetBit(0); j >= 0; j = bs1.nextSetBit(j + 1))
        for (int k = bs2.nextSetBit(0); k >= 0; k = bs2.nextSetBit(k + 1)) {
          if ((!isMolecular || !bsConnected[j + firstAtom].get(k))
              && symmetry.checkDistance(atoms[j + firstAtom], atoms[k
                  + firstAtom], distance, dx, 0, 0, 0, ptOffset))
            addNewBond(j + firstAtom, k + firstAtom, order);
        }
    }

    // do a quick check for H-X bonds if we have GEOM_BOND

    if (bondTypes.size() > 0)
      for (int i = firstAtom; i < ac; i++)
        if (atoms[i].elementNumber == 1) {
          boolean checkAltLoc = (atoms[i].altLoc != '\0');
          for (int k = firstAtom; k < ac; k++)
            if (k != i
                && atoms[k].elementNumber != 1
                && (!checkAltLoc || atoms[k].altLoc == '\0' || atoms[k].altLoc == atoms[i].altLoc)) {
              if (!bsConnected[i].get(k)
                  && symmetry.checkDistance(atoms[i], atoms[k], 1.1f, 0, 0, 0,
                      0, ptOffset))
                addNewBond(i, k, 1);
            }
        }
    if (!isMolecular)
      return false;

    // generate the base atom set

    if (doInit)
      for (int i = firstAtom; i < ac; i++)
        if (atoms[i].atomSite + firstAtom == i && !bsMolecule.get(i))
          setBs(atoms, i, bsConnected, bsMolecule);

    // Now look through unchecked atoms for ones that
    // are within bonding distance of the "molecular" set
    // in any one of the 27 adjacent cells in 444 - 666.
    // If an atom is found, move it along with its "branch"
    // to the new location. BUT also check that we are
    // not overlaying another atom -- if that happens
    // go ahead and move it, but mark it as excluded.

    float bondTolerance = vwr.getFloat(T.bondtolerance);
    BS bsBranch = new BS();
    P3 cart1 = new P3();
    P3 cart2 = new P3();
    int nFactor = 2; // 1 was not enough. (see data/cif/triclinic_issue.cif)
    for (int i = firstAtom; i < ac; i++)
      if (!bsMolecule.get(i) && !bsExclude.get(i))
        for (int j = bsMolecule.nextSetBit(0); j >= 0; j = bsMolecule
            .nextSetBit(j + 1))
          if (symmetry.checkDistance(atoms[j], atoms[i], atomRadius[i]
              + atomRadius[j] + bondTolerance, 0, nFactor, nFactor, nFactor,
              ptOffset)) {
            setBs(atoms, i, bsConnected, bsBranch);
            for (int k = bsBranch.nextSetBit(0); k >= 0; k = bsBranch
                .nextSetBit(k + 1)) {
              atoms[k].add(ptOffset);
              cart1.setT(atoms[k]);
              symmetry.toCartesian(cart1, true);
              BS bs = bsSets[asc.getAtomIndex(atoms[k].atomName) - firstAtom];
              if (bs != null)
                for (int ii = bs.nextSetBit(0); ii >= 0; ii = bs
                    .nextSetBit(ii + 1)) {
                  if (ii + firstAtom == k)
                    continue;
                  cart2.setT(atoms[ii + firstAtom]);
                  symmetry.toCartesian(cart2, true);
                  if (cart2.distance(cart1) < 0.1f) {
                    bsExclude.set(k);
                    break;
                  }
                }
              bsMolecule.set(k);
            }
            return true;
          }
    return false;
  }

  /**
   * add the bond and mark it for molecular processing
   * 
   * @param i
   * @param j
   * @param order
   */
  private void addNewBond(int i, int j, int order) {
    asc.addNewBondWithOrder(i, j, order);
    if (!isMolecular)
      return;
    bsConnected[i].set(j);
    bsConnected[j].set(i);
  }

  /**
   * iteratively run through connected atoms, adding them to the set
   * 
   * @param atoms
   * @param iatom
   * @param bsBonds
   * @param bs
   */
  private void setBs(Atom[] atoms, int iatom, BS[] bsBonds, BS bs) {
    BS bsBond = bsBonds[iatom];
    bs.set(iatom);
    for (int i = bsBond.nextSetBit(0); i >= 0; i = bsBond.nextSetBit(i + 1)) {
      if (!bs.get(i))
        setBs(atoms, i, bsBonds, bs);
    }
  }

}
