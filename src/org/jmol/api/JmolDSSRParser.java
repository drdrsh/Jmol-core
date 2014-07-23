package org.jmol.api;

import java.util.Map;

import org.jmol.java.BS;
import org.jmol.modelset.Bond;
import org.jmol.modelset.ModelSet;
import org.jmol.script.SV;
import org.jmol.viewer.Viewer;

import javajs.api.GenericLineReader;
import javajs.util.Lst;

public interface JmolDSSRParser {

  String process(Map<String, Object> info, GenericLineReader reader, 
                 String line0, Map<String, String> htGroup1) throws Exception;

  BS getAtomBits(Viewer vwr, String key, Object dssr, Map<String, Object> cache, BS bsModel);

  void setAllDSSRParametersForModel(Viewer vwr, int modelIndex);

  String getHBonds(ModelSet ms, int modelIndex, Lst<Bond> vHBonds, boolean doReport);

  String calculateStructure(Viewer vwr, BS bsAtoms);

  String getAnnotationInfo(SV a, String match);


}
