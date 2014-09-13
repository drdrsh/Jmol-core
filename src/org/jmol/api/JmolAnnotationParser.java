package org.jmol.api;

import java.util.Map;

import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.ModelSet;
import org.jmol.script.SV;
import org.jmol.viewer.Viewer;

import javajs.api.GenericLineReader;
import javajs.util.Lst;

public interface JmolAnnotationParser {

  String processDSSR(Map<String, Object> info, GenericLineReader reader, 
                 String line0, Map<String, String> htGroup1) throws Exception;

  BS getAtomBits(Viewer vwr, String key, Object dssr, Map<String, Object> cache, int type, int modelIndex, BS bsModel);

  void setAllDSSRParametersForModel(Viewer vwr, int modelIndex);

  String getHBonds(ModelSet ms, int modelIndex, Lst<Bond> vHBonds, boolean doReport);

  String calculateDSSRStructure(Viewer vwr, BS bsAtoms);

  String getAnnotationInfo(Viewer vwr, SV a, String match, int type, int modelIndex);

  Lst<Object> catalogValidations(Viewer vwr, SV validation, int[] modelAtomIndices,
                            Map<String, int[]> valResMap,
                            Map<String, Integer> map, Map<String, Integer> modelMap);

  Lst<SV> initializeAnnotation(SV objAnn, int type, int modelIndex);

  Lst<Float> getAtomValidation(Viewer vwr, String type, Atom atom);

  void fixAtoms(int modelIndex, SV v, BS bsAddedMask, int type, int margin);

  String catalogStructureUnits(Viewer vwr, SV svMap, int[] modelAtomIndices,
                               Map<String, int[]> resMap, Object object,
                               Map<String, Integer> modelMap);
  
}
