package org.jmol.adapter.readers.cif;

import org.jmol.adapter.smarter.AtomSetCollectionReader;

public interface CifValidationParser {

  CifValidationParser set(AtomSetCollectionReader cr);

  String finalizeValidations();
  
}
