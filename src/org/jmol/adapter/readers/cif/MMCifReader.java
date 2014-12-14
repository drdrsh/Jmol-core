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

import java.util.Hashtable;
import java.util.Map;
import javajs.util.Lst;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.Rdr;
import javajs.util.SB;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Structure;
import org.jmol.api.JmolAdapter;
import org.jmol.c.STR;
import org.jmol.java.BS;
import org.jmol.util.Logger;
import org.jmol.util.SimpleUnitCell;

/**
 * @author Bob Hanson (hansonr@stolaf.edu)
 * 
 */
public class MMCifReader extends CifReader {

  private boolean isBiomolecule;
  private boolean byChain, bySymop;
  private Map<String, P3> chainAtomMap;
  private Map<String, int[]> chainAtomCounts;

  private Lst<Map<String, Object>> vBiomolecules;
  private Map<String, Object> thisBiomolecule;
  private Map<String, M4> htBiomts;
  private Map<String, Map<String, Object>> htSites;

  private Map<String, BS> assemblyIdAtoms;

  private int thisChain = -1;

  private P3 chainSum;
  private int[] chainAtomCount;
  
  private boolean isLigandBondBug; 
  // Jmol-14.3.3_2014.07.27 broke mmCIF bond reading for ligands
  // Jmol-14.3.9_2014.11.11 fixes this. 

  @Override
  protected void initSubclass() {
    setIsPDB();
    isMMCIF = true;
    byChain = checkFilterKey("BYCHAIN");
    bySymop = checkFilterKey("BYSYMOP");
    isCourseGrained = byChain || bySymop;
    if (byChain) {
      chainAtomMap = new Hashtable<String, P3>();
      chainAtomCounts = new Hashtable<String, int[]>();
    }
    if (checkFilterKey("BIOMOLECULE")) // PDB format
      filter = PT.rep(filter, "BIOMOLECULE", "ASSEMBLY");
    isBiomolecule = checkFilterKey("ASSEMBLY");
    
    // When this reader was split off from CifReader, a bug was introduced
    // into the Resolver that made it so that ligand files were read by 
    // CifReader and not MMCifReader. This caused CHEM_COMP_BOND records to be 
    // skipped and so in the case of pdbAddHydrogen no hydrogens added.
    isLigandBondBug = (stateScriptVersionInt >= 140204 && stateScriptVersionInt <= 140208
        || stateScriptVersionInt >= 140304 && stateScriptVersionInt <= 140308);

  }

  @Override
  protected void finalizeSubclass() throws Exception {
    if (byChain && !isBiomolecule)
      for (String id : chainAtomMap.keySet())
        createParticle(id);
    if (!isCourseGrained && asc.ac == nAtoms) {
      asc.removeCurrentAtomSet();
    } else {
      if ((validation != null || addedData != null) && !isCourseGrained) {
        MMCifValidationParser vs = ((MMCifValidationParser) getInterface("org.jmol.adapter.readers.cif.MMCifValidationParser")).set(this);
        String note = null; 
        if (addedData == null) {
          note = vs.finalizeValidations(modelMap);
        } else if (addedDataKey.equals("_rna3d")) {
          note = vs.finalizeRna3d(modelMap);   
        } else { 
          reader = Rdr.getBR(addedData);
          processDSSR(this, htGroup1);
        }
        if (note != null)
          appendLoadNote(note);
      }
      applySymmetryAndSetTrajectory();
    }
    
    if (htSites != null)
      addSites(htSites);
    if (vBiomolecules != null && vBiomolecules.size() == 1
        && (isCourseGrained || asc.ac > 0)) {
      asc.setAtomSetAuxiliaryInfo("biomolecules", vBiomolecules);
      Map<String, Object> ht = vBiomolecules.get(0);
      appendLoadNote("Constructing " + ht.get("name"));
      setBiomolecules(ht);
      if (thisBiomolecule != null) {
        asc.getXSymmetry().applySymmetryBio(thisBiomolecule,
            notionalUnitCell, applySymmetryToBonds, filter);
        asc.xtalSymmetry = null;
      }
    }

  }

  ////////////////////////////////////////////////////////////////
  // assembly data
  ////////////////////////////////////////////////////////////////

  final private static byte OPER_ID = 12;
  final private static byte OPER_XYZ = 13;
  final private static String FAMILY_OPER = "_pdbx_struct_oper_list";
  final private static String[] operFields = {
    "*_matrix[1][1]",
    "*_matrix[1][2]",
    "*_matrix[1][3]",
    "*_vector[1]",
    "*_matrix[2][1]",
    "*_matrix[2][2]",
    "*_matrix[2][3]",
    "*_vector[2]",
    "*_matrix[3][1]",
    "*_matrix[3][2]",
    "*_matrix[3][3]",
    "*_vector[3]", 
    "*_id",
    "*_symmetry_operation" 
  };

  final private static byte ASSEM_ID = 0;
  final private static byte ASSEM_OPERS = 1;
  final private static byte ASSEM_LIST = 2;
  final private static String FAMILY_ASSEM = "_pdbx_struct_assembly_gen";
  final private static String[] assemblyFields = {
    "*_assembly_id",
    "*_oper_expression",
    "*_asym_id_list" 
  };

  /*
  _pdbx_struct_assembly_gen.assembly_id       1 
  _pdbx_struct_assembly_gen.oper_expression   1,2,3,4 
  _pdbx_struct_assembly_gen.asym_id_list      A,B,C 
  # 
  loop_
  _pdbx_struct_oper_list.id 
  _pdbx_struct_oper_list.type 
  _pdbx_struct_oper_list.name 
  _pdbx_struct_oper_list.symmetry_operation 
  _pdbx_struct_oper_list.matrix[1][1] 
  _pdbx_struct_oper_list.matrix[1][2] 
  _pdbx_struct_oper_list.matrix[1][3] 
  _pdbx_struct_oper_list.vector[1] 
  _pdbx_struct_oper_list.matrix[2][1] 
  _pdbx_struct_oper_list.matrix[2][2] 
  _pdbx_struct_oper_list.matrix[2][3] 
  _pdbx_struct_oper_list.vector[2] 
  _pdbx_struct_oper_list.matrix[3][1] 
  _pdbx_struct_oper_list.matrix[3][2] 
  _pdbx_struct_oper_list.matrix[3][3] 
  _pdbx_struct_oper_list.vector[3] 
  1 'identity operation'         1_555  x,y,z          1.0000000000  0.0000000000  0.0000000000 0.0000000000  0.0000000000  
  1.0000000000  0.0000000000 0.0000000000  0.0000000000 0.0000000000 1.0000000000  0.0000000000  
  2 'crystal symmetry operation' 15_556 y,x,-z+1       0.0000000000  1.0000000000  0.0000000000 0.0000000000  1.0000000000  
  0.0000000000  0.0000000000 0.0000000000  0.0000000000 0.0000000000 -1.0000000000 52.5900000000 
  3 'crystal symmetry operation' 10_665 -x+1,-y+1,z    -1.0000000000 0.0000000000  0.0000000000 68.7500000000 0.0000000000  
  -1.0000000000 0.0000000000 68.7500000000 0.0000000000 0.0000000000 1.0000000000  0.0000000000  
  4 'crystal symmetry operation' 8_666  -y+1,-x+1,-z+1 0.0000000000  -1.0000000000 0.0000000000 68.7500000000 -1.0000000000 
  0.0000000000  0.0000000000 68.7500000000 0.0000000000 0.0000000000 -1.0000000000 52.5900000000 
  # 

   */

  private String[] assem = null;

  @Override
  protected void processSubclassEntry() throws Exception {
    if (key.startsWith("_pdbx_entity_nonpoly"))
      processDataNonpoly();
    else if (key.startsWith(FAMILY_ASSEM))
      processDataAssemblyGen();
    else if (key.equals("_rna3d") || key.equals("_dssr"))
      processAddedData();
  }

  private void processAddedData() {
    addedData = data;
    addedDataKey = key;
  }

  final private static byte STRUCT_REF_G3 = 0;
  final private static byte STRUCT_REF_G1 = 1;
  final private static String[] structRefFields = {
    "_struct_ref_seq_dif_mon_id", 
    "_struct_ref_seq_dif.db_mon_id" 
  };

  private boolean processSequence() throws Exception {
    parseLoopParameters(structRefFields);
    while (parser.getData()) {
      String g1 = null;
      String g3 = null;
      int n = parser.getFieldCount();
      for (int i = 0; i < n; ++i) {
        switch (fieldProperty(i)) {
        case STRUCT_REF_G3:
          g3 = field;
          break;
        case STRUCT_REF_G1:
          if (field.length() == 1)
            g1 = field.toLowerCase();
        }
      }
      if (g1 != null && g3 != null) {
        if (htGroup1 == null)
          asc.setInfo("htGroup1",
              htGroup1 = new Hashtable<String, String>());
        htGroup1.put(g3, g1);
      }
    }
    return true;
  }

  private void processDataNonpoly() throws Exception {
    if (hetatmData == null)
      hetatmData = new String[3];
    for (int i = nonpolyFields.length; --i >= 0;)
      if (key.equals(nonpolyFields[i])) {
        hetatmData[i] = data;
        break;
      }
    if (hetatmData[NONPOLY_NAME] == null || hetatmData[NONPOLY_COMP_ID] == null)
      return;
    addHetero(hetatmData[NONPOLY_COMP_ID], hetatmData[NONPOLY_NAME]);
    hetatmData = null;
  }

  private void processDataAssemblyGen() throws Exception {
    if (assem == null)
      assem = new String[3];
    if (key.indexOf("assembly_id") >= 0)
      assem[ASSEM_ID] = parser.fullTrim(data);
    else if (key.indexOf("oper_expression") >= 0)
      assem[ASSEM_OPERS] = parser.fullTrim(data);
    else if (key.indexOf("asym_id_list") >= 0)
      assem[ASSEM_LIST] = parser.fullTrim(data);
    if (assem[0] != null && assem[1] != null && assem[2] != null)
      addAssembly();
  }

  private boolean processAssemblyGenBlock() throws Exception {
    parseLoopParametersFor(FAMILY_ASSEM, assemblyFields);
    while (parser.getData()) {
      assem = new String[3];
      int count = 0;
      int p;
      int n = parser.getFieldCount();
      for (int i = 0; i < n; ++i) {
        switch (p = fieldProperty(i)) {
        case ASSEM_ID:
        case ASSEM_OPERS:
        case ASSEM_LIST:
          count++;
          assem[p] = field;
          break;
        }
      }
      if (count == 3)
        addAssembly();
    }
    assem = null;
    return true;
  }

  private void addAssembly() throws Exception {
    String id = assem[ASSEM_ID];
    int iMolecule = parseIntStr(id);
    String list = assem[ASSEM_LIST];
    appendLoadNote("found biomolecule " + id + ": " + list);
    if (!checkFilterKey("ASSEMBLY " + id + ";"))
      return;
    if (vBiomolecules == null) {
      vBiomolecules = new Lst<Map<String, Object>>();
    }
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("name", "biomolecule " + id);
    info.put("molecule",
        iMolecule == Integer.MIN_VALUE ? id : Integer.valueOf(iMolecule));
    info.put("assemblies", "$" + list.replace(',', '$'));
    info.put("operators", decodeAssemblyOperators(assem[ASSEM_OPERS]));
    info.put("biomts", new Lst<M4>());
    thisBiomolecule = info;
    Logger.info("assembly " + id + " operators " + assem[ASSEM_OPERS]
        + " ASYM_IDs " + assem[ASSEM_LIST]);
    vBiomolecules.addLast(info);
    assem = null;
  }

  private String decodeAssemblyOperators(String ops) {

    //    Identifies the operation of collection of operations 
    //    from category PDBX_STRUCT_OPER_LIST.  
    //
    //    Operation expressions may have the forms:
    //
    //     (1)        the single operation 1
    //     (1,2,5)    the operations 1, 2, 5
    //     (1-4)      the operations 1,2,3 and 4
    //     (1,2)(3,4) the combinations of operations
    //                3 and 4 followed by 1 and 2 (i.e.
    //                the cartesian product of parenthetical
    //                groups applied from right to left)
    int pt = ops.indexOf(")(");
    if (pt >= 0)
      return crossBinary(decodeAssemblyOperators(ops.substring(0, pt + 1)),
          decodeAssemblyOperators(ops.substring(pt + 1)));
    if (ops.startsWith("(")) {
      if (ops.indexOf("-") >= 0)
        ops = BS.unescape(
            "({" + ops.substring(1, ops.length() - 1).replace('-', ':') + "})")
            .toString();
      ops = PT.rep(ops, " ", "");
      ops = ops.substring(1, ops.length() - 1);
    }
    return ops;
  }

  private String crossBinary(String ops1, String ops2) {
    SB sb = new SB();
    String[] opsLeft = PT.split(ops1, ",");
    String[] opsRight = PT.split(ops2, ",");
    for (int i = 0; i < opsLeft.length; i++)
      for (int j = 0; j < opsRight.length; j++)
        sb.append(",").append(opsLeft[i]).append("|").append(opsRight[j]);
    //System.out.println((ops1 + "\n" + ops2 + "\n" + sb.toString()).length());
    return sb.toString().substring(1);
  }

  private boolean processStructOperListBlock() throws Exception {
    parseLoopParametersFor(FAMILY_OPER, operFields);
    float[] m = new float[16];
    m[15] = 1;
    while (parser.getData()) {
      int count = 0;
      String id = null;
      String xyz = null;
      int n = parser.getFieldCount();
      for (int i = 0; i < n; ++i) {
        int p = fieldProperty(i);
        switch (p) {
        case NONE:
          break;
        case OPER_ID:
          id = field;
          break;
        case OPER_XYZ:
          xyz = field;
          break;
        default:
          m[p] = parseFloatStr(field);
          ++count;
        }
      }
      if (id != null && (count == 12 || xyz != null && symmetry != null)) {
        Logger.info("assembly operator " + id + " " + xyz);
        M4 m4 = new M4();
        if (count != 12) {
          symmetry.getMatrixFromString(xyz, m, false, 0);
          m[3] *= symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_A) / 12;
          m[7] *= symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_B) / 12;
          m[11] *= symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_C) / 12;
        }
        m4.setA(m);
        if (htBiomts == null)
          htBiomts = new Hashtable<String, M4>();
        htBiomts.put(id, m4);
      }
    }
    return true;
  }

  ////////////////////////////////////////////////////////////////
  // HETATM identity
  ////////////////////////////////////////////////////////////////

  final private static byte NONPOLY_ENTITY_ID = 0;
  final private static byte NONPOLY_NAME = 1;
  final private static byte NONPOLY_COMP_ID = 2;

  final private static String[] nonpolyFields = {
      "_pdbx_entity_nonpoly_entity_id", 
      "_pdbx_entity_nonpoly_name",
      "_pdbx_entity_nonpoly_comp_id", };

  /**
   * 
   * optional nonloop format -- see 1jsa.cif
   * 
   */
  private String[] hetatmData;


  ////////////////////////////////////////////////////////////////
  // HETATM identity
  ////////////////////////////////////////////////////////////////

  final private static byte CHEM_COMP_ID = 0;
  final private static byte CHEM_COMP_NAME = 1;

  final private static String[] chemCompFields = { 
    "_chem_comp_id",
    "_chem_comp_name"
  };

  /**
   * 
   * a general name definition field. Not all hetero
   * 
   * @return true if successful; false to skip
   * 
   * @throws Exception
   */
  private boolean processChemCompLoopBlock() throws Exception {
    parseLoopParameters(chemCompFields);
    while (parser.getData()) {
      String groupName = null;
      String hetName = null;
      int n = parser.getFieldCount();
      for (int i = 0; i < n; ++i) {
        switch (fieldProperty(i)) {
        case NONE:
          break;
        case CHEM_COMP_ID:
          groupName = field;
          break;
        case CHEM_COMP_NAME:
          hetName = field;
          break;
        }
      }
      if (groupName != null && hetName != null)
        addHetero(groupName, hetName);
    }
    return true;
  }

  /**
   * 
   * a HETERO name definition field. Maybe not all hetero? nonpoly?
   * 
   * @return true if successful; false to skip
   * 
   * @throws Exception
   */
  private boolean processNonpolyLoopBlock() throws Exception {
    parseLoopParameters(nonpolyFields);
    while (parser.getData()) {
      String groupName = null;
      String hetName = null;
      int n = parser.getFieldCount();
      for (int i = 0; i < n; ++i) {
        switch (fieldProperty(i)) {
        case NONE:
        case NONPOLY_ENTITY_ID:
          break;
        case NONPOLY_COMP_ID:
          groupName = field;
          break;
        case NONPOLY_NAME:
          hetName = field;
          break;
        }
      }
      if (groupName == null || hetName == null)
        return false;
      addHetero(groupName, hetName);
    }
    return true;
  }

  private Map<String, String> htHetero;

  private void addHetero(String groupName, String hetName) {
    if (!vwr.getJBR().isHetero(groupName))
      return;
    if (htHetero == null)
      htHetero = new Hashtable<String, String>();
    htHetero.put(groupName, hetName);
    if (Logger.debugging) {
      Logger.debug("hetero: " + groupName + " = " + hetName);
    }
  }

  ////////////////////////////////////////////////////////////////
  // helix and turn structure data
  ////////////////////////////////////////////////////////////////

  final private static byte CONF_TYPE_ID = 0;
  final private static byte BEG_ASYM_ID = 1;
  final private static byte BEG_SEQ_ID = 2;
  final private static byte BEG_INS_CODE = 3;
  final private static byte END_ASYM_ID = 4;
  final private static byte END_SEQ_ID = 5;
  final private static byte END_INS_CODE = 6;
  final private static byte STRUCT_ID = 7;
  final private static byte SERIAL_NO = 8;
  final private static byte HELIX_CLASS = 9;

  final private static String[] structConfFields = {
      "*_conf_type_id", 
      "*_beg_auth_asym_id",
      "*_beg_auth_seq_id", 
      "*_pdbx_beg_pdb_ins_code",
      "*_end_auth_asym_id", 
      "*_end_auth_seq_id",
      "*_pdbx_end_pdb_ins_code", 
      "*_id",
      "*_pdbx_pdb_helix_id", 
      "*_pdbx_pdb_helix_class" };

  final private static String FAMILY_STRUCTCONF = "_struct_conf";
  /**
   * identifies ranges for HELIX and TURN
   * 
   * @return true if successful; false to skip
   * @throws Exception
   */
  private boolean processStructConfLoopBlock() throws Exception {
    parseLoopParametersFor(FAMILY_STRUCTCONF, structConfFields);
    for (int i = propertyCount; --i >= 0;)
      if (fieldOf[i] == NONE) {
        Logger.warn("?que? missing property: " + structConfFields[i]);
        return false;
      }
    while (parser.getData()) {
      Structure structure = new Structure(-1, STR.HELIX, STR.HELIX, null, 0, 0);
      int n = parser.getFieldCount();
      for (int i = 0; i < n; ++i) {
        switch (fieldProperty(i)) {
        case NONE:
          break;
        case CONF_TYPE_ID:
          if (field.startsWith("TURN"))
            structure.structureType = structure.substructureType = STR.TURN;
          else if (!field.startsWith("HELX"))
            structure.structureType = structure.substructureType = STR.NONE;
          break;
        case BEG_ASYM_ID:
          structure.startChainStr = field;
          structure.startChainID = vwr.getChainID(field, true);
          break;
        case BEG_SEQ_ID:
          structure.startSequenceNumber = parseIntStr(field);
          break;
        case BEG_INS_CODE:
          structure.startInsertionCode = firstChar;
          break;
        case END_ASYM_ID:
          structure.endChainStr = field;
          structure.endChainID = vwr.getChainID(field, true);
          break;
        case END_SEQ_ID:
          structure.endSequenceNumber = parseIntStr(field);
          break;
        case HELIX_CLASS:
          structure.substructureType = Structure.getHelixType(parseIntStr(field));
          break;
        case END_INS_CODE:
          structure.endInsertionCode = firstChar;
          break;
        case STRUCT_ID:
          structure.structureID = field;
          break;
        case SERIAL_NO:
          structure.serialID = parseIntStr(field);
          break;
        }
      }
      asc.addStructure(structure);
    }
    return true;
  }

  ////////////////////////////////////////////////////////////////
  // sheet structure data
  ////////////////////////////////////////////////////////////////

  final private static byte SHEET_ID = 0;
  final private static byte STRAND_ID = 7;

  final private static String FAMILY_SHEET = "_struct_sheet_range";
  final private static String[] structSheetRangeFields = {
      "*_sheet_id", //unused placeholder
      "*_beg_auth_asym_id",
      "*_beg_auth_seq_id",
      "*_pdbx_beg_pdb_ins_code",
      "*_end_auth_asym_id",
      "*_end_auth_seq_id",
      "*_pdbx_end_pdb_ins_code", 
      "*_id"
  };

  /**
   * 
   * identifies sheet ranges
   * 
   * @return true if successful; false to skip
   * 
   * @throws Exception
   */
  private boolean processStructSheetRangeLoopBlock() throws Exception {
    parseLoopParametersFor(FAMILY_SHEET, structSheetRangeFields);
    for (int i = propertyCount; --i >= 0;)
      if (fieldOf[i] == NONE) {
        Logger.warn("?que? missing property:" + structSheetRangeFields[i]);
        return false;
      }
    while (parser.getData()) {
      Structure structure = new Structure(-1, STR.SHEET, STR.SHEET, null, 0, 0);
      int n = parser.getFieldCount();
      for (int i = 0; i < n; ++i) {
        switch (fieldProperty(i)) {
        case BEG_ASYM_ID:
          structure.startChainID = vwr.getChainID(field, true);
          break;
        case BEG_SEQ_ID:
          structure.startSequenceNumber = parseIntStr(field);
          break;
        case BEG_INS_CODE:
          structure.startInsertionCode = firstChar;
          break;
        case END_ASYM_ID:
          structure.endChainID = vwr.getChainID(field, true);
          break;
        case END_SEQ_ID:
          structure.endSequenceNumber = parseIntStr(field);
          break;
        case END_INS_CODE:
          structure.endInsertionCode = firstChar;
          break;
        case SHEET_ID:
          structure.strandCount = 1;
          structure.structureID = field;
          break;
        case STRAND_ID:
          structure.serialID = parseIntStr(field);
          break;
        }
      }
      asc.addStructure(structure);
    }
    return true;
  }

  final private static byte SITE_ID = 0;
  final private static byte SITE_COMP_ID = 1;
  final private static byte SITE_ASYM_ID = 2;
  final private static byte SITE_SEQ_ID = 3;
  final private static byte SITE_INS_CODE = 4; //???

  final private static String FAMILY_STRUCSITE = "_struct_site_gen";
  final private static String[] structSiteFields = {
      "*_site_id", 
      "*_auth_comp_id",
      "*_auth_asym_id", 
      "*_auth_seq_id",
      "*_label_alt_id", //should be an insertion code, not an alt ID? 
  };

  //  loop_
  //  _struct_site_gen.id 
  //  _struct_site_gen.site_id 
  //  _struct_site_gen.pdbx_num_res 
  //  _struct_site_gen.label_comp_id 
  //  _struct_site_gen.label_asym_id 
  //  _struct_site_gen.label_seq_id 
  //  _struct_site_gen.auth_comp_id 
  //  _struct_site_gen.auth_asym_id 
  //  _struct_site_gen.auth_seq_id 
  //  _struct_site_gen.label_atom_id 
  //  _struct_site_gen.label_alt_id 
  //  _struct_site_gen.symmetry 
  //  _struct_site_gen.details 
  //  1 CAT 5 GLN A 92  GLN A 92  . . ? ? 
  //  2 CAT 5 GLU A 58  GLU A 58  . . ? ? 
  //  3 CAT 5 HIS A 40  HIS A 40  . . ? ? 
  //  4 CAT 5 TYR A 38  TYR A 38  . . ? ? 
  //  5 CAT 5 PHE A 100 PHE A 100 . . ? ? 
  //  # 

  /**
   * 
   * identifies structure sites
   * 
   * @return true if successful; false to skip
   * 
   * @throws Exception
   */
  private boolean processStructSiteBlock() throws Exception {
    parseLoopParametersFor(FAMILY_STRUCSITE, structSiteFields);
    for (int i = 3; --i >= 0;)
      if (fieldOf[i] == NONE) {
        Logger.warn("?que? missing property: " + structSiteFields[i]);
        return false;
      }
    String siteID = "";
    String seqNum = "";
    String insCode = "";
    String chainID = "";
    String resID = "";
    String group = "";
    Map<String, Object> htSite = null;
    htSites = new Hashtable<String, Map<String, Object>>();
    while (parser.getData()) {
      int n = parser.getFieldCount();
      for (int i = 0; i < n; ++i) {
        switch (fieldProperty(i)) {
        case SITE_ID:
          if (group != "") {
            String groups = (String) htSite.get("groups");
            groups += (groups.length() == 0 ? "" : ",") + group;
            group = "";
            htSite.put("groups", groups);
          }
          siteID = field;
          htSite = htSites.get(siteID);
          if (htSite == null) {
            htSite = new Hashtable<String, Object>();
            //htSite.put("seqNum", "site_" + (++siteNum));
            htSite.put("groups", "");
            htSites.put(siteID, htSite);
          }
          seqNum = "";
          insCode = "";
          chainID = "";
          resID = "";
          break;
        case SITE_COMP_ID:
          resID = field;
          break;
        case SITE_ASYM_ID:
          chainID = field;
          break;
        case SITE_SEQ_ID:
          seqNum = field;
          break;
        case SITE_INS_CODE: //optional
          insCode = field;
          break;
        }
        if (seqNum != "" && resID != "")
          group = "[" + resID + "]" + seqNum
              + (insCode.length() > 0 ? "^" + insCode : "")
              + (chainID.length() > 0 ? ":" + chainID : "");
      }
    }
    if (group != "") {
      String groups = (String) htSite.get("groups");
      groups += (groups.length() == 0 ? "" : ",") + group;
      group = "";
      htSite.put("groups", groups);
    }
    return true;
  }

  private void setBiomolecules(Map<String, Object> biomolecule) {
    if (!isBiomolecule || assemblyIdAtoms == null && chainAtomCounts == null)
      return;
    M4 mident = M4.newM4(null);
    String[] ops = PT.split((String) biomolecule.get("operators"), ",");
    String assemblies = (String) biomolecule.get("assemblies");
    Lst<M4> biomts = new Lst<M4>();
    biomolecule.put("biomts", biomts);
    biomts.addLast(mident);
    for (int j = 0; j < ops.length; j++) {
      M4 m = getOpMatrix(ops[j]);
      if (m != null && !m.equals(mident))
        biomts.addLast(m);
    }
    BS bsAll = new BS();
    P3 sum = new P3();
    int count = 0;
    int nAtoms = 0;
    String[] ids = PT.split(assemblies, "$");
    for (int j = 1; j < ids.length; j++) {
      String id = ids[j];
      if (assemblyIdAtoms != null) {
        BS bs = assemblyIdAtoms.get(id);
        if (bs != null) {
          //System.out.println(id + " " + bs.cardinality());
          bsAll.or(bs);
        }
      } else if (isCourseGrained) {
        P3 asum = chainAtomMap.get(id);
        int c = chainAtomCounts.get(id)[0];
        if (asum != null) {
          if (bySymop) {
            sum.add(asum);
            count += c;
          } else {
            createParticle(id);
            nAtoms++;
          }
        }
      }
    }
    if (isCourseGrained) {
      if (bySymop) {
        nAtoms = 1;
        Atom a1 = new Atom();
        a1.setT(sum);
        a1.scale(1f / count);
        a1.radius = 16;
      }
    } else {
      nAtoms = bsAll.cardinality();
      if (nAtoms < asc.ac)
        asc.bsAtoms = bsAll;
    }
    biomolecule.put("atomCount", Integer.valueOf(nAtoms * ops.length));
  }

  private void createParticle(String id) {
    P3 asum = chainAtomMap.get(id);
    int c = chainAtomCounts.get(id)[0];
    Atom a = new Atom();
    a.setT(asum);
    a.scale(1f / c);
    a.elementSymbol = "Pt";
    setChainID(a, id); 
    a.radius = 16;
    asc.addAtom(a);
  }

  private M4 getOpMatrix(String ops) {
    if (htBiomts == null)
      return M4.newM4(null);
    int pt = ops.indexOf("|");
    if (pt >= 0) {
      M4 m = M4.newM4(htBiomts.get(ops.substring(0, pt)));
      m.mul(htBiomts.get(ops.substring(pt + 1)));
      return m;
    }
    return htBiomts.get(ops);
  }

  ////////////////////////////////////////////////////////////////
  // bond data
  ////////////////////////////////////////////////////////////////

  final private static byte CHEM_COMP_BOND_ATOM_ID_1 = 0;
  final private static byte CHEM_COMP_BOND_ATOM_ID_2 = 1;
  final private static byte CHEM_COMP_BOND_VALUE_ORDER = 2;
  final private static byte CHEM_COMP_BOND_AROMATIC_FLAG = 3;
  
  final private static String FAMILY_COMPBOND = "_chem_comp_bond";
  final private static String[] chemCompBondFields = {
    "*_atom_id_1", 
    "*_atom_id_2",
    "*_value_order", 
    "*_pdbx_aromatic_flag"
  };
  private boolean processLigandBondLoopBlock() throws Exception {
    parseLoopParametersFor(FAMILY_COMPBOND, chemCompBondFields);
    // alas -- saved states must not read ligand bonding
    // the problem was that these files were not recognized as mmCIF 
    // files by the resolver when this MMCifReader was created.
    
    if (isLigandBondBug)
      return false;
    for (int i = propertyCount; --i >= 0;)
      if (fieldOf[i] == NONE) {
        Logger.warn("?que? missing property: " + chemCompBondFields[i]);
        return false;
      }
    int order = 0;
    boolean isAromatic = false;
    while (parser.getData()) {
      Atom atom1 = null;
      Atom atom2 = null;
      order = 0;
      isAromatic = false;
      int n = parser.getFieldCount();
      for (int i = 0; i < n; ++i) {
        switch (fieldProperty(i)) {
        case CHEM_COMP_BOND_ATOM_ID_1:
          atom1 = asc.getAtomFromName(field);
          break;
        case CHEM_COMP_BOND_ATOM_ID_2:
          atom2 = asc.getAtomFromName(field);
          break;
        case CHEM_COMP_BOND_AROMATIC_FLAG:
          isAromatic = (field.charAt(0) == 'Y');
          break;
        case CHEM_COMP_BOND_VALUE_ORDER:
          order = getBondOrder(field);
          break;
        }
      }
      if (isAromatic)
        switch (order) {
        case JmolAdapter.ORDER_COVALENT_SINGLE:
          order = JmolAdapter.ORDER_AROMATIC_SINGLE;
          break;
        case JmolAdapter.ORDER_COVALENT_DOUBLE:
          order = JmolAdapter.ORDER_AROMATIC_DOUBLE;
          break;
        }
      asc.addNewBondWithOrderA(atom1, atom2, order);
    }
    return true;
  }

  @Override
  public boolean processSubclassAtom(Atom atom, String assemblyId, String strChain) {    
    if (byChain && !isBiomolecule) {
      if (thisChain != atom.chainID) {
        thisChain = atom.chainID;
        String id = "" + atom.chainID;
        chainSum = chainAtomMap.get(id);
        if (chainSum == null) {
          chainAtomMap.put(id, chainSum = new P3());
          chainAtomCounts.put(id, chainAtomCount = new int[1]);
        }
      }
      chainSum.add(atom);
      chainAtomCount[0]++;
      return false;
    }
    if (isBiomolecule && isCourseGrained) {
      P3 sum = chainAtomMap.get(assemblyId);
      if (sum == null) {
        chainAtomMap.put(assemblyId, sum = new P3());
        chainAtomCounts.put(assemblyId, new int[1]);
      }
      chainAtomCounts.get(assemblyId)[0]++;
      sum.add(atom);
      return false;
    }
    if (assemblyId != null) {
      if (assemblyIdAtoms == null)
        assemblyIdAtoms = new Hashtable<String, BS>();
      BS bs = assemblyIdAtoms.get(assemblyId);
      if (bs == null)
        assemblyIdAtoms.put(assemblyId, bs = new BS());
      bs.set(ac);
    }
    if (atom.isHetero && htHetero != null) {
      asc.setAtomSetAuxiliaryInfo("hetNames", htHetero);
      asc.setInfo("hetNames", htHetero);
      htHetero = null;
    }
//    if (assemblyId != null && strChain != null) {
//      addAssemblyId(assemblyId, strChain);
//    }
    return true;
  }

//  /**
//   * An idea that was not followed up.
//   * 
//   * @param assemblyId  
//   * @param strChain 
//   */
//  private void addAssemblyId(String assemblyId, String strChain) {
//    if (vCompnds == null) {
//      vCompnds = new Lst<Map<String, String>>();
//      htModels = new Hashtable<String, Map<String, String>>();
//    }
//    Map<String, String> ht = htModels.get(assemblyId);
//    if (ht == null) {
//      htModels.put("compoundSource", assemblyId, (ht = new Hashtable<String, String>());
//    }
//  }

  @Override
  protected boolean processSubclassLoopBlock() throws Exception {
    if (key.startsWith(FAMILY_OPER))
      return processStructOperListBlock();
    if (key.startsWith(FAMILY_ASSEM))
      return processAssemblyGenBlock();
    if (key.startsWith("_struct_ref_seq_dif"))
      return processSequence();

    if (isCourseGrained)
      return false;

    if (key.startsWith(FAMILY_STRUCSITE))
      return processStructSiteBlock();
    if (key.startsWith(FAMILY_COMPBOND))
      return processLigandBondLoopBlock();
    if (key.startsWith("_chem_comp"))
      return processChemCompLoopBlock();
    if (key.startsWith("_pdbx_entity_nonpoly"))
      return processNonpolyLoopBlock();
    if (key.startsWith(FAMILY_STRUCTCONF) && !key.startsWith("_struct_conf_type"))
      return processStructConfLoopBlock();
    if (key.startsWith(FAMILY_SHEET))
      return processStructSheetRangeLoopBlock();
    return false;
  }

}
