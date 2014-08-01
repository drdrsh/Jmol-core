package org.jmol.adapter.readers.cif;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.Lst;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.script.SV;

public class MMCifValidationParser {

  private boolean asResidues;
  private AtomSetCollectionReader reader;

  private Map<String, int[]> resMap;
  private Map<String, Integer> atomMap;

  public MMCifValidationParser() {
    //for reflection
  }

  public MMCifValidationParser set(AtomSetCollectionReader reader) {
    this.reader = reader;
    asResidues = reader.checkFilterKey("ASRES");
    return this;
  }

  /**
   * Create property_xxxx for each validation category.
   * @param modelMap 
   * @return loading note
   * 
   */
  public String finalizeValidations(Map<String, Integer> modelMap) {

    mapAtomResIDs(modelMap);

    // Returns a Lst<Object> of property data in the form 
    // name(String), data(float[]), modelIndex (Integer), isGroup (Boolean);

    SV svMap = (SV) reader.validation;
    Lst<Object> retProps = reader.vwr.getAnnotationParser().catalogValidations(
        reader.vwr, svMap, getModelAtomIndices(), resMap,
        (asResidues ? null : atomMap), modelMap);

    String note = (retProps == null || retProps.size() == 0 ? null
        : setProperties(retProps));
    svMap.getMap().put("_note", SV.newS(note));
    return note;
  }

  /**
   * Map all atom and residue unit ids to atom indexes
   * @param modelMap 
   * 
   */
  private void mapAtomResIDs(Map<String, Integer> modelMap) {

    // model_chainCode_resno_inscode
    // model_chainCode_resno_inscode_ATOMNAME_altcode
    //   

    Atom[] atoms = reader.asc.atoms;
    resMap = new Hashtable<String, int[]>();
    atomMap = new Hashtable<String, Integer>();
    int iresLast = -1;
    int[] resLast = null;
    String smodel = "" + modelMap.get("_0");
    for (int i = 0, model = 1, i0 = 0, n = reader.asc.getAtomSetAtomCount(0); i < n; i++) {
      Atom a = atoms[i];
      int ires = a.sequenceNumber;
      String res = smodel + "_" + a.chainID + "_" + ires + "_"
          + (a.insertionCode == '\0' ? "" : "" + a.insertionCode);
      String atom = res + "_" + a.atomName.toUpperCase() + "_"
          + (a.altLoc == '\0' ? "" : "" + Character.toLowerCase(a.altLoc));
      Integer ia = Integer.valueOf(i - i0);
      if (ires != iresLast) {
        iresLast = ires;
        if (resLast != null)
          resLast[1] = i - i0;
        resMap.put(res, resLast = new int[] { i - i0, n });
      }
      atomMap.put(atom, ia);
      if (i == n - 1) {
        i0 += n;
        n = reader.asc.getAtomSetAtomCount(model++);
      }
    }
  }

  /**
   * prepare a list of starting atom indices for each
   * model, adding one additional one to indicate 1 + last atom index
   * 
   * @return array
   */
  private int[] getModelAtomIndices() {
    int[] indices = new int[reader.asc.atomSetCount + 1];
    for (int m = indices.length - 1; --m >= 0;)
      indices[m] = reader.baseAtomIndex + reader.asc.getAtomSetAtomIndex(m);
    indices[indices.length - 1] = reader.asc.ac;
    return indices;
  }

  /**
   * Set property_xxx for atoms from list of name, data, modelIndex series.
   * 
   * @param propList
   * @return note string
   */
  private String setProperties(Lst<Object> propList) {
    String note = "Validations loaded:";
    for (int i = 0, n = propList.size(); i < n;) {
      String key = (String) propList.get(i++);
      float[] f = (float[]) propList.get(i++);
      int model = ((Integer) propList.get(i++)).intValue();
      boolean isGroup = ((Boolean) propList.get(i++)).booleanValue();
      int count = 0;
      float max = 0;
      int reslast = -1;
      int i0 = reader.asc.getAtomSetAtomIndex(model);
      for (int j = f.length; --j >= 0;)
        if (f[j] != 0) {
          if (isGroup) {
            int res = reader.asc.atoms[i0+j].sequenceNumber;
            if (res != reslast) {
              reslast = res;
              count++;
            }            
          } else {
            count++;
          }
          max = Math.max(f[j], max);
        }
      note += "\n  property_" + key + " (" + (isGroup ? "residues: ": "atoms: ") + count 
          + (max == 1 ? "" : ", max: " + ((int) (max * 100)) / 100f) + ")";
      reader.asc.setAtomProperties(key, f, model, isGroup);
    }
    return note;
  }


}
