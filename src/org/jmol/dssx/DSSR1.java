/* $RCSfile$
 * $Author: nicove $
 * $Date: 2007-03-25 06:44:28 -0500 (Sun, 25 Mar 2007) $
 * $Revision: 7224 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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
package org.jmol.dssx;

import java.util.Map;

import javajs.util.Lst;
import javajs.util.PT;

import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.HBond;
import org.jmol.modelset.ModelSet;
import org.jmol.modelsetbio.BasePair;
import org.jmol.modelsetbio.BioModel;
import org.jmol.modelsetbio.BioPolymer;
import org.jmol.modelsetbio.NucleicMonomer;
import org.jmol.modelsetbio.NucleicPolymer;
import org.jmol.script.T;
import org.jmol.util.C;
import org.jmol.util.Edge;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

/**
 * 
 * A parser for output from 3DNA web service.
 * 
 * load =1d66/dssr
 * 
 * also other annotations now,
 * 
 * load *1cbs/dom
 * 
 * calls EBI for the mmCIF file and also retrieves the domains mapping JSON
 * report.
 * 
 * 
 * load *1cbs/val
 * 
 * calls EBI for the mmCIF file and also retrieves the validation outliers JSON
 * report.
 * 
 * Bob Hanson July 2014
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */
public class DSSR1 extends AnnotationParser {

  /**
   * The paths to the unit id data within the structure.
   * 
   * This is one long string; all lowercase, each surrounded by double periods.
   * 
   */
  
  private final static String DSSR_PATHS = 
      "..bulges.nts_long" +
      "..coaxstacks.stems.pairs.nt*" +
      "..hairpins.nts_long" +
      "..hbonds.atom1_id;atom2_id" +
      "..helices.pairs.nt*" +
      "..iloops.nts_long" +
      "..isocanonpairs.nt*" +
      "..junctions.nts_long" +
      "..kissingloops.hairpins.nts_long" +
      "..multiplets.nts_long" +
      "..nonstack.nts_long" +
      "..nts.nt_id" +
      "..pairs.nt*" +
      "..sssegments.nts_long" +
      "..stacks.nts_long" +
      "..stems.pairs.nt*" +
      "..";

  public DSSR1() {
    // for reflection
  }

  @Override
  public String calculateDSSRStructure(Viewer vwr, BS bsAtoms) {
    BS bs = vwr.ms.getModelBS(bsAtoms == null ? vwr.bsA() : bsAtoms, true);
    String s = "";
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
      s += getDSSRForModel(vwr, i) + "\n";
    return s;
  }

  @SuppressWarnings("unchecked")
  private String getDSSRForModel(Viewer vwr, int modelIndex) {
    Map<String, Object> info = null;
    String out = null;
    while (true) {
      if (!vwr.ms.am[modelIndex].isBioModel)
        break;
      info = vwr.ms.getModelAuxiliaryInfo(modelIndex);
      if (info.containsKey("dssr"))
        break;
      BS bs = vwr.getModelUndeletedAtomsBitSet(modelIndex);
      bs.and(vwr.ms.getAtoms(T.nucleic, null));
      if (bs.nextClearBit(0) < 0) {
        info = null;
        break;
      }
      try {
        String name = (String) vwr.setLoadFormat("=dssrModel/", '=', false);
        name = PT.rep(name, "%20", " ");
        Logger.info("fetching " + name + "[pdb data]");
        String data = vwr.getPdbAtomData(bs, null, false, false);
        data = vwr.getFileAsString3(name + data, false, null);
        Map<String, Object> x = vwr.parseJSON(data);
        if (x != null) {
          info.put("dssr", x);
          setGroup1(vwr.ms, modelIndex);
          fixDSSRJSONMap(x);
          setBioPolymers((BioModel) vwr.ms.am[modelIndex], false);
        }
      } catch (Exception e) {
        info = null;
        out = "" + e;
      }
      break;
    }
    return (info != null ? PT.rep(Escape.escapeMap((Map<String, Object>) ((Map<String, Object>) info.get("dssr"))
        .get("counts")),",",",\n") : out == null ? "model has no nucleotides" : out);
  }

  /**
   * kissingLoops and coaxStacks use index arrays instead of duplication;
   * 
   * @param map
   * @return msg
   */
  @Override
  public String fixDSSRJSONMap(Map<String, Object> map) {
    String s = "";
    
    try {

      fixIndices(map, "kissingLoops", "hairpin");
      fixIndices(map, "coaxStacks", "stem");
      
//      lst = (Lst<Object>) map.get("hbonds");
//      if (lst != null) {
//        for (int i = lst.size(); --i >= 0;) {
//          Map<String, Object> smap = (Map<String, Object>) lst.get(i);
//          smap.put("res_long", removeUnitAtom((String) smap.get("atom1_id"))
//              + "," + removeUnitAtom((String) smap.get("atom2_id")));
//        }
//      }  

      if (map.containsKey("counts"))
        s += "_M.dssr.counts = " + map.get("counts").toString() + "\n";
      if (map.containsKey("dbn"))
        s += "_M.dssr.dbn = " + map.get("dbn").toString();
    } catch (Exception e) {
      // ignore??
    }

    return s;
  }

  /**
   * create a key/value pair root+"s" for all indices of root+"_indices"
   * @param map
   * @param key
   * @param root
   */
  @SuppressWarnings("unchecked")
  private void fixIndices(Map<String, Object> map, String key,
                          String root) {
    String indices = root + "_indices";
    String original = root + "s";
    Lst<Object>lst = (Lst<Object>) map.get(key);
    if (lst != null) {
      Lst<Object>  hpins = (Lst<Object>) map.get(original);
      for (int i = lst.size(); --i >= 0;) {
        Map<String, Object> kmap = (Map<String, Object>) lst.get(i);
        Lst<Object> khlist = (Lst<Object>) kmap.get(indices);
        int n = khlist.size();
        if (n > 0) {
          Lst<Object> khpins = new Lst<Object>();
          kmap.put(original, khpins);
          for (int j = n; --j >= 0;)
            khpins.addLast(hpins.get(((Integer) khlist.get(j)).intValue() - 1));
        }
      }
    }
  }

//  private static String removeUnitAtom(String unitID) {
//    int pt1 = 0;
//    int pt2 = unitID.length();
//    for (int i = 0, pt = -1; i < 7 && (pt = unitID.indexOf("|", pt + 1)) >= 0;i++) {
//      switch (i) {
//      case 4:
//        pt1 = pt + 1;
//        break;
//      case 6:
//        pt2 = pt;
//        break;
//      }
//    }
//    unitID = unitID.substring(0, pt1) + "|" + unitID.substring(pt2);
//    return unitID;
//  }

   @SuppressWarnings("unchecked")
  @Override
  public void getBasePairs(Viewer vwr, int modelIndex) {
    ModelSet ms = vwr.ms;
    Map<String, Object> info = (Map<String, Object>) ms.getInfo(modelIndex,
        "dssr");
    Lst<Map<String, Object>> pairs = (info == null ? null
        : (Lst<Map<String, Object>>) info.get("pairs"));
    Lst<Map<String, Object>> singles = (info == null ? null
        : (Lst<Map<String, Object>>) info.get("ssSegments"));
    if (pairs == null && singles == null) {
      setBioPolymers((BioModel) vwr.ms.am[modelIndex], true);
      return;
    }
    BS bsAtoms = ms.am[modelIndex].bsAtoms;
    try {
      BS bs = new BS();
      Atom[] atoms = ms.at;
      if (pairs != null)
        for (int i = pairs.size(); --i >= 0;) {
          Map<String, Object> map = pairs.get(i);
          String unit1 = (String) map.get("nt1");
          String unit2 = (String) map.get("nt2");
          int a1 = ms.getSequenceBits(unit1, bsAtoms, bs).nextSetBit(0);
          bs.clearAll();
          int a2 = ms.getSequenceBits(unit2, bsAtoms, bs).nextSetBit(0);
          bs.clearAll();
          BasePair.add(map, setRes(atoms[a1]), setRes(atoms[a2]));
        }
      if (singles != null)
        for (int i = singles.size(); --i >= 0;) {
          Map<String, Object> map = singles.get(i);
          String units = (String) map.get("nts_long");
          ms.getSequenceBits(units, bsAtoms, bs);
          for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1))
            setRes(atoms[j]);
        }
    } catch (Exception e) {
      Logger.error("Exception " + e + " in DSSRParser.getBasePairs");
    }

  }

  private void setBioPolymers(BioModel m, boolean b) {
    int n = m.getBioPolymerCount();
    for (int i = n; --i >= 0;) {
      BioPolymer bp = m.bioPolymers[i];
      if (bp.isNucleic())
        ((NucleicPolymer) bp).isDssrSet = b;
    }
  }

  private NucleicMonomer setRes(Atom atom) {
    NucleicMonomer m = (NucleicMonomer) atom.group;
    ((NucleicPolymer) m.bioPolymer).isDssrSet = true;
    return m;
  }


  @Override
  /**
   * 
   * Retrieve a set of atoms using vwr.extractProperty with 
   * and for other annotations
   * 
   */
  public BS getAtomBits(Viewer vwr, String key, Object dbObj,
                        Map<String, Object> annotationCache, int type,
                        int modelIndex, BS bsModel) {
    if (dbObj == null)
      return new BS();
    //boolean isStruc = (type == T.rna3d);
    //boolean isDomains = (type == T.domains);
    //boolean isValidation = (type == T.validation);
    boolean doCache = !key.contains("NOCACHE");
    if (!doCache) {
      key = PT.rep(key, "NOCACHE", "").trim();
    }
//    key = fixKeyDSSR(key);
    BS bs = (doCache ? (BS) annotationCache.get(key) : null);
    if (bs != null)
      return bs;
    bs = new BS();
    if (doCache)
      annotationCache.put(key, bs);
    try {
      // drilling
      key = key.toLowerCase();
      int pt = DSSR_PATHS.indexOf(".." + key) + 2;
      int len = key.length();
      while (pt >= 2 && len > 0) {
        dbObj = vwr.extractProperty(dbObj, key, -1);
        pt += len + 1;
        int pt1 = DSSR_PATHS.indexOf(".", pt);
        key = DSSR_PATHS.substring(pt, pt1);
        len = key.length();
        if (key.indexOf(";") >= 0)
          key = "[select " + key + "]";
      }
      bs.or(vwr.ms.getAtoms(T.sequence, dbObj.toString()));
      bs.and(bsModel);
    } catch (Exception e) {
      System.out.println(e.toString() + " in AnnotationParser");
      bs.clearAll();
    }
    return bs;
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public String getHBonds(ModelSet ms, int modelIndex, Lst<Bond> vHBonds,
                          boolean doReport) {
    Map<String, Object> info = (Map<String, Object>) ms.getInfo(modelIndex, "dssr");
    Lst<Object> list;
    if (info == null || (list = (Lst<Object>) info.get("hbonds")) == null)
      return "no DSSR hydrogen-bond data";
    BS bsAtoms = ms.am[modelIndex].bsAtoms; 
    String unit1 = null, unit2 = null;
    int a1 = 0, a2 = 0;
    try {
      BS bs = new BS();
      for (int i = list.size(); --i >= 0;) {
        Map<String, Object> map = (Map<String, Object>) list.get(i); 
        unit1 = (String) map.get("atom1_id");    
        a1 = ms.getSequenceBits(unit1, bsAtoms, bs).nextSetBit(0);
        if (a1 < 0) {
          Logger.error("Atom " + unit1 + " was not found");
          continue;
        }
        unit2 = (String) map.get("atom2_id");
        bs.clearAll();
        a2 = ms.getSequenceBits(unit2, bsAtoms, bs).nextSetBit(0);
        if (a2 < 0) {
          Logger.error("Atom " + unit2 + " was not found");
          continue;
        }
        bs.clearAll();
        float energy = 0;
        vHBonds.addLast(new HBond(ms.at[a1], ms.at[a2], Edge.BOND_H_REGULAR,
            (short) 1, C.INHERIT_ALL, energy));
      }
    } catch (Exception e) {
    }
    return "DSSR reports " + list.size() + " hydrogen bonds";
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public void setGroup1(ModelSet ms, int modelIndex) {
    Map<String, Object> info = (Map<String, Object>) ms.getInfo(modelIndex,
        "dssr");
    Lst<Map<String, Object>> list;
    if (info == null
        || (list = (Lst<Map<String, Object>>) info.get("nts")) == null)
      return;
    BioModel m = (BioModel) ms.am[modelIndex];
    BS bsAtoms = m.bsAtoms;
    Atom[] atoms = ms.at;
    BS bs = new BS();
    for (int i = list.size(); --i >= 0;) {
      Map<String, Object> map = list.get(i);
      char ch = ((String) map.get("nt_code")).charAt(0);
      if (!Character.isLowerCase(ch))
        continue;
      String unit1 = (String) map.get("nt_id");
      m.getAllSequenceBits(unit1, bsAtoms, bs);
      Logger.info("" + ch + " " + unit1 + " " + bs);
      atoms[bsAtoms.nextSetBit(0)].group.group1 = ch;
      bs.clearAll();
    }
  }
}
