package org.jmol.adapter.readers.cif;

import java.util.Map;

import org.jmol.adapter.smarter.AtomSetCollectionReader;

public interface CifValidationParser {

  CifValidationParser set(AtomSetCollectionReader cr);

  String finalizeValidations(Map<String, Integer> modelMap);
  
}
