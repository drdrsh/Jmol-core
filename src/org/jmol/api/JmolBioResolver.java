package org.jmol.api;

import java.util.Map;
import java.util.Properties;

import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Chain;
import org.jmol.modelset.Group;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelLoader;
import org.jmol.modelsetbio.Resolver;
import org.jmol.viewer.Viewer;

public interface JmolBioResolver {

  public Group distinguishAndPropagateGroup(Chain chain, String group3, int seqcode,
                                                  int firstAtomIndex, int maxAtomIndex, 
                                                  int modelIndex, int[] specialAtomIndexes,
                                                  Atom[] atoms);
  
  public void initializeHydrogenAddition();

  public void finalizeHydrogens();

  public void setHaveHsAlready(boolean b);

  public void addImplicitHydrogenAtoms(JmolAdapter adapter, int i, int nH);

  public JmolBioResolver setLoader(ModelLoader modelLoader);

  public Object fixPropertyValue(BS bsAtoms, Object data, boolean toHydrogens);

  public Model getBioModel(int modelIndex,
                        int trajectoryBaseIndex, String jmolData,
                        Properties modelProperties,
                        Map<String, Object> modelAuxiliaryInfo);

  public void iterateOverAllNewStructures(JmolAdapter adapter,
                                          Object atomSetCollection);

  public void setGroupLists(int ipt);

  public boolean isKnownPDBGroup(String g3, int maxID);

  public boolean isHetero(String g3);

  public boolean getAminoAcidValenceAndCharge(String g3, String atomName,
                                              int[] aaRet);

  public byte lookupSpecialAtomID(String name);

  public int[] getArgbs(int tok);

  Resolver setViewer(Viewer vwr);

  public short getGroupID(String g3);

  public String toStdAmino3(String sValue);
  
  }

