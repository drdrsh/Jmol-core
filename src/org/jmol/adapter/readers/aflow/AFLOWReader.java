package org.jmol.adapter.readers.aflow;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.adapter.readers.xtal.VaspPoscarReader;
import org.jmol.java.BS;

/**
 * A reader for various AFLOW file types.
 * 
 * For starters, we have output from the binaries page.
 * 
 * see http://www.aflowlib.org/binary_alloys.php
 * 
 * or, in Jmol, from:
 * 
 *  print load("http://aflowlib.mems.duke.edu/php/apool.php?POST?job=awrapper_apool&lattice=all&alloy=AgAu")
 * 
 * Unit cells are centered.
 * 
 * Selected compositions can be obtained using the filter "Ca=0.5" for example. 
 * 
 * @author Bob Hanson
 * 
 * @version 1.0
 */

public class AFLOWReader extends VaspPoscarReader {

  private String aabb;  
  private boolean readPRE;
//  private boolean readPOST;
  private float fracB = Float.NaN;
  private Map<String, float[]> compositions;
  private boolean getComposition;
  private String listKey, listKeyCase;
  private int fileModelNumber;
  


  //Looking for:
  //  
  //[NbRh] # WEB file for ALL NbRh (Nb_svRh_pv) [calcs=285] [date=2015-05-29] [aflow 31010 (C) 2003-2015 Stefano Curtarolo]
  //[NbRh] REFERENCE: G. L. W. Hart, S. Curtarolo, T.B. Massalski, and O. Levy, Comprehensive Search for New Phases and Compounds in Binary Alloy Systems Based on Platinum-Group Metals, Using a Computational First-Principles Approach, Phys. Rev. X 3, 041035 (2013). 
  //[NbRh] REFERENCE: S. Curtarolo, W. Setyawan, S. Wang, J. Xue, K. Yang, R. H. Taylor, L. J. Nelson, G. L. W. Hart, S. Sanvito, M. Buongiorno Nardelli, N. Mingo, and O. Levy, AFLOWLIB.ORG: a distributed materials properties repository from high-throughput ab initio calculations, Comp. Mat. Sci. 58, 227-235 (2012). 
  //[NbRh] REFERENCE: S. Curtarolo, W. Setyawan, G. L. W. Hart, M. Jahnatek, R. V. Chepulskii, R. H. Taylor, S. Wang, J. Xue, K. Yang, O. Levy, M. Mehl, H. T. Stokes, D. O. Demchenko, and D. Morgan, AFLOW: an automatic framework for high-throughput materials discovery, Comp. Mat. Sci. 58, 218-226 (2012). 
  //[NbRh] REFERENCE: R. H. Taylor, S. Curtarolo, and G. L. W. Hart, Predictions of the Pt8Ti phase in unexpected systems, J. Am. Chem. Soc. 132, 6851-6854 (2010). 
  //[NbRh] REFERENCE: O. Levy, R. V. Chepulskii, G. L. W. Hart, and S. Curtarolo, The new face of rhodium alloys: revealing ordered structures from first principles, J. Am. Chem. Soc. 132, 833-837 (2010). 
  //[NbRh] REFERENCE: S. Curtarolo, D. Morgan, and G. Ceder, Accuracy of ab-initio methods in predicting the crystal structures of metals: review of 80 binary alloys, Calphad 29, 163-211 (2005). 
  //[NbRh] PROTOTYPES: http://aflowlib.mems.duke.edu/AFLOW/proto.pdf
  //[NbRh/1] # **************************************************************************************************************
  //[NbRh/1] # ----------------------------------------------------------------------- 
  //[NbRh/1] NbRh/1
  //[NbRh/1] # ---- Structure PRE ---------------------------------------------------- 
  //[NbRh/1] Nb_svRh_pv/1 - (1) - FCC A1 [B] (1)  (htqc library) (swap of 2)
  //[NbRh/1] -14.173100
  //[NbRh/1]    0.50000000000000   0.00000000000000  -0.50000000000000
  //[NbRh/1]    0.50000000000000  -0.50000000000000   0.00000000000000
  //[NbRh/1]    0.00000000000000  -0.50000000000000  -0.50000000000000
  //[NbRh/1] 1 
  //[NbRh/1] Direct(1) [A1] 
  //[NbRh/1]    0.00000000000000   0.00000000000000   0.00000000000000  Rh_pv 
  //[NbRh/1] # ---- Structure POST --------------------------------------------------- 
  //[NbRh/1] Nb_svRh_pv/1 - (1) - FCC A1 [B] (1)  (ht
  //[NbRh/1] 1.244826
  //[NbRh/1]    0.00000000000000   1.54318346068944   1.54318346068944
  //[NbRh/1]    1.54318346068944   0.00000000000000   1.54318346068944
  //[NbRh/1]    1.54318346068944   1.54318346068944   0.00000000000000
  //[NbRh/1] 1 
  //[NbRh/1] Direct(1) [A1] 
  //[NbRh/1]    0.00000000000000   0.00000000000000   0.00000000000000 
  //[NbRh/1] # ---- DATA ------------------------------------------------------------- 
  //[NbRh/1]  -7.34073000000000  # H [eV] (VASP) 
  //[NbRh/1]  -7.34073000000000   # H/at [eV] (VASP) 
  //[NbRh/1]   0.00000000000000   # Hf_atom [eV] (VASP) 
  //[NbRh/1]   0.00000000000000   # Mom/at 
  //[NbRh/1]  14.17780000000000   # Volume/at 
  //[NbRh/1]   0.00000000000000   # Ca 
  //[NbRh/1]   1.00000000000000   # Cb 
  //[NbRh/1]       Fm-3m #225     # space group PRE 
  //[NbRh/1]       Fm-3m #225     # space group POST 
  //[NbRh/1] # ---- URL -------------------------------------------------------------- 
  //[NbRh/1]  http://materials.duke.edu/AFLOWDATA/LIB2_RAW/Nb_svRh_pv/1/index.php
  //[NbRh/1] # ---- aflowlib.out ----------------------------------------------- 
  //[NbRh/1]  aurl=aflowlib.duke.edu:AFLOWDATA/LIB2_RAW/Nb_svRh_pv/1 | auid=aflow:e57e1645ae54fc4b | data_api=aapi1.1 | data_source=aflowlib | loop=lock,thermodynamics | code=vasp.4.6.35 | compound=Rh1 | prototype=1 | nspecies=1 | natoms=1 | composition=1 | density=12.0526 | scintillation_attenuation_length=0.96579 | stoichiometry=1 | species=Rh | species_pp=Rh_pv | dft_type=PAW_PBE | species_pp_version=Rh_pv:PAW_PBE:06Sep2000 | species_pp_ZVAL=15 | valence_cell_iupac=6 | valence_cell_std=9 | volume_cell=14.1778 | volume_atom=14.1778 | pressure=0 | geometry=2.716697,2.716697,2.716697,60,60,60 | energy_cell=-7.34073 | energy_atom=-7.34073 | enthalpy_cell=-7.34073 | enthalpy_atom=-7.34073 | eentropy_cell=9.706e-05 | eentropy_atom=9.706e-05 | enthalpy_formation_cell=-0.000163 | enthalpy_formation_atom=-0.000163 | entropic_temperature=0 | PV_cell=0 | PV_atom=0 | spin_cell=0 | spin_atom=0 | stoich=1.0000 | calculation_time=1879.86 | calculation_memory=636.17 | calculation_cores=8 | nbondxx=1.1225 | sg=Fm-3m #225,Fm-3m #225,Fm-3m #225 | sg2=Fm-3m #225,Fm-3m #225,Fm-3m #225 | spacegroup_orig=225 | spacegroup_relax=225 | forces=0,0,0 | positions_cartesian=0,0,0 | Bravais_lattice_orig=FCC | lattice_variation_orig=FCC | lattice_system_orig=cubic | Pearson_symbol_orig=cF4 | Bravais_lattice_relax=FCC | lattice_variation_relax=FCC | lattice_system_relax=cubic | Pearson_symbol_relax=cF4 | files=CONTCAR.relax,CONTCAR.relax.abinit,CONTCAR.relax.qe,CONTCAR.relax.vasp,CONTCAR.relax1,CONTCAR.relax2,LOCK,POSCAR.orig,POSCAR.relax2,aflow.in,aflow.qmvasp.out,edata.orig.out,edata.relax.out | aflow_version=aflow30069 | aflowlib_version=31007 | aflowlib_date=20150417_04:32:31_GMT-5
  //[NbRh/1] # **************************************************************************************************************
  //[NbRh/2] # **************************************************************************************************************
  //[NbRh/2] # ----------------------------------------------------------------------- 
  //[NbRh/2] NbRh/2
  //[NbRh/2] # ---- Structure PRE ---------------------------------------------------- 
  //[NbRh/2] Nb_svRh_pv/2 - (2) - FCC A1 [A] (2)  (htqc library)
  //[NbRh/2] -18.313200
  //...
  //[NbRh] PROTOTYPES: http://aflowlib.mems.duke.edu/awrapper.html
  //[NbRh] REFERENCE: G. L. W. Hart, S. Curtarolo, T.B. Massalski, and O. Levy, Comprehensive Search for New Phases and Compounds in Binary Alloy Systems Based on Platinum-Group Metals, Using a Computational First-Principles Approach, Phys. Rev. X 3, 041035 (2013). 
  //[NbRh] REFERENCE: S. Curtarolo, W. Setyawan, S. Wang, J. Xue, K. Yang, R. H. Taylor, L. J. Nelson, G. L. W. Hart, S. Sanvito, M. Buongiorno Nardelli, N. Mingo, and O. Levy, AFLOWLIB.ORG: a distributed materials properties repository from high-throughput ab initio calculations, Comp. Mat. Sci. 58, 227-235 (2012). 
  //[NbRh] REFERENCE: S. Curtarolo, W. Setyawan, G. L. W. Hart, M. Jahnatek, R. V. Chepulskii, R. H. Taylor, S. Wang, J. Xue, K. Yang, O. Levy, M. Mehl, H. T. Stokes, D. O. Demchenko, and D. Morgan, AFLOW: an automatic framework for high-throughput materials discovery, Comp. Mat. Sci. 58, 218-226 (2012). 
  //[NbRh] REFERENCE: R. H. Taylor, S. Curtarolo, and G. L. W. Hart, Predictions of the Pt8Ti phase in unexpected systems, J. Am. Chem. Soc. 132, 6851-6854 (2010). 
  //[NbRh] REFERENCE: O. Levy, R. V. Chepulskii, G. L. W. Hart, and S. Curtarolo, The new face of rhodium alloys: revealing ordered structures from first principles, J. Am. Chem. Soc. 132, 833-837 (2010). 
  //[NbRh] REFERENCE: S. Curtarolo, D. Morgan, and G. Ceder, Accuracy of ab-initio methods in predicting the crystal structures of metals: review of 80 binary alloys, Calphad 29, 163-211 (2005). 
  //[NbRh] # WEB file for ALL NbRh (Nb_svRh_pv) [calcs=285] [date=2015-05-29] [aflow 31010 (C) 2003-2015 Stefano Curtarolo]

  @Override
  protected void initializeReader() throws Exception {
    readPRE = checkFilterKey("PRE");
//    readPOST = !checkFilterKey("NOPOST");
    forcePacked = !checkFilterKey("NOPACK");

    String s;
    
    s = getFilter("CA=");
    if (s != null)
      fracB = (1 - parseFloatStr(s.substring(1)));
    
    s = getFilter("CB=");
    if (s != null)
      fracB = parseFloatStr(s.substring(1));
    
    s = getFilter("LIST=");
    listKey = (s == null ? "HF" : s);
    listKeyCase = listKey;
    getComposition = !Float.isNaN(fracB);
    discardLinesUntilStartsWith("[");
    //asc.setAtomSetName(title = line.trim());
    aabb = line.substring(1, line.indexOf("]"));
    int pt = (PT.isUpperCase(aabb.charAt(1)) ? 1 : 2);
    defaultLabels = new String[] { aabb.substring(0, pt), aabb.substring(pt) };
    while (rd().indexOf("] REFERENCE:") >= 0)
      appendLoadNote(line);
    compositions = new Hashtable<String, float[]>();
    quiet = true;
    asc.bsAtoms = new BS();
  }

  @Override
  protected boolean checkLine() throws Exception {
    discardLinesUntilContains("Structure PRE");
    if (line == null)
      return false;
    continuing &= readPrePost();
    return continuing;
  }

  private boolean readPrePost() throws Exception {
    fileModelNumber++;
    String titleMsg = "" + (modelNumber+1)
        + (getComposition ? "," + fileModelNumber + ", Cb=" + fracB : "");
    elementLabel = null;
    if (readPRE) {
      readStructure(titleMsg);
    } else {
      readElementLabelsOnly();
      discardLinesUntilContains("Structure POST");
      readStructure(titleMsg);
    }
    if (getData()) {
      applySymmetryAndSetTrajectory();
//      XtalSymmetry sym = asc.getXSymmetry();
  //    T3 offset = sym.getOverallSpan();
    //  offset.scale(-0.5f);
      //asc.setModelInfoForSet("unitCellOffset", offset, asc.iSet);
    } else {
      asc.removeCurrentAtomSet();
    }
    return !haveModel || modelNumber != desiredModelNumber;
  }

  private boolean getData() throws Exception {
    discardLinesUntilContains("- DATA -");
    Map<String, Object> htAFLOW = new Hashtable<String, Object>();
    htAFLOW.put("fileModelNumber", Integer.valueOf(fileModelNumber));
    htAFLOW.put("modelNumber", Integer.valueOf(modelNumber));
    int pt = 0;
    SB sb = new SB();
    float listVal = Float.MAX_VALUE;
    String strcb = "?";
    while (rdline() != null && (pt = line.indexOf(" # ")) >= 0) {
      String key = line.substring(pt + 3).trim();
      String val = line.substring(0, pt).trim();
      sb.append(key).append("=").append(val).append(" | ");
      if (key.toUpperCase().startsWith(listKey)) {
        listKey = key.toUpperCase();
        listKeyCase = key;
        asc.setAtomSetName(aabb + " " + (getComposition ? fracB + " " : " ") + key + "=" + val);
        listVal = parseFloatStr(val);
      }
      if (key.equals("Ca")) {
        float ca = parseFloatStr(val);
        if (getComposition && Math.abs((1 - ca) - fracB) > 0.01f)
          return false;
      } else
      if (key.equals("Cb")) {
        float cb = parseFloatStr(strcb = val);
        if (getComposition && Math.abs(cb - fracB) > 0.01f)
          return false;
      }
    }
    float[] count_min = compositions.get(strcb);
    if (count_min == null)
      compositions.put(strcb, count_min = new float[] {0, Float.MAX_VALUE } );
    count_min[0]++;
    if (listVal < count_min[1])
      count_min[1] = listVal;
    if (!doGetModel(++modelNumber, null))
      return false;
    sb.append(discardLinesUntilContains("aurl="));
    String[] pairs = PT.split(sb.toString(), " | ");
    for (int i = pairs.length; --i >= 0;) {
      String[] kv = pairs[i].split("=");
      if (kv.length < 2)
        continue;
      float f = parseFloatStr(kv[1]);
      htAFLOW.put(kv[0], Float.isNaN(f) ? kv[1] : Float.valueOf(f));
    }
    asc.setCurrentModelInfo("aflowInfo", htAFLOW);
    return true;
  }

  @Override
  protected void finalizeSubclassReader() throws Exception {
    alignUnitCells();
    listCompositions();
    finalizeReaderASCR();
  }

  @SuppressWarnings("cast")
  private void listCompositions() {
    Lst<String> list = new Lst<String>();
    for (Entry<String, float[]> e : compositions.entrySet()) {
      float[] count_min = (float[]) e.getValue();
      list.addLast(e.getKey() + "\t" + ((int) count_min[0]) + "\t" + listKeyCase + "\t" + count_min[1]);
    }
    String[] a = new String[list.size()];
    list.toArray(a);
    Arrays.sort(a);
    for (int i = 0, n = a.length; i < n; i++)
      appendLoadNote(aabb + "\t" + a[i]);
  }

  private void alignUnitCells() {
//    for (int i = asc.atomSetCount; --i >= 0;) {
//      
////      M3 matUnitCellOrientation = (M3) modelAuxiliaryInfo.get("matUnitCellOrientation");
//
//      
//    }
  }
  
}
