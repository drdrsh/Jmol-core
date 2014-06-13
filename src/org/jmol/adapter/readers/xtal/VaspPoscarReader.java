package org.jmol.adapter.readers.xtal;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import javajs.util.Lst;
import javajs.util.SB;

import org.jmol.util.Logger;

/**
 * http://cms.mpi.univie.ac.at/vasp/
 * 
 * @author Pieremanuele Canepa, Wake Forest University, Department of Physics
 *         Winston Salem, NC 27106, canepap@wfu.edu
 * 
 * @version 1.0
 */

public class VaspPoscarReader extends AtomSetCollectionReader {

  private Lst<String> atomLabels = new Lst<String>();
  private int ac;

  @Override
  protected void initializeReader() throws Exception {
    readJobTitle();
    readUnitCellVectors();
    readMolecularFormula();
    readCoordinates();
    continuing = false;
  }

  private void readJobTitle() throws Exception {
    asc.setAtomSetName(rd().trim());
  }

  private void readUnitCellVectors() throws Exception {
    // Read Unit Cell
    setSpaceGroupName("P1");
    setFractionalCoordinates(true);
    float scaleFac = parseFloatStr(rd().trim());
    float[] unitCellData = new float[9];
    fillFloatArray(null, 0, unitCellData);
    if (scaleFac != 1)
      for (int i = 0; i < unitCellData.length; i++)
        unitCellData[i] *= scaleFac;
    addPrimitiveLatticeVector(0, unitCellData, 0);
    addPrimitiveLatticeVector(1, unitCellData, 3);
    addPrimitiveLatticeVector(2, unitCellData, 6);
  }

  private void readMolecularFormula() throws Exception {
    //   H    C    O    Be   C    H
    String elementLabel[] = getTokensStr(discardLinesUntilNonBlank());
    //   6    24    18     6     6    24
    String elementCounts[] = getTokensStr(rd());
    SB mf = new SB();
    for (int i = 0; i < elementCounts.length; i++) { 
      int n = Integer.parseInt(elementCounts[i]);
      ac += n;
      String label = elementLabel[i];
      mf.append(" ").append(label).appendI(n);
      for (int j = n; --j >= 0;)
        atomLabels.addLast(label);
    }
    String s = mf.toString();
    Logger.info("VaspPoscar reader: " + ac + " atoms identified for" + s);
    appendLoadNote(s);
    asc.newAtomSet();
    asc.setAtomSetName(s);
  }

  private void readCoordinates() throws Exception {
    // If Selective is there, then skip a line 
    if (discardLinesUntilNonBlank().toLowerCase().contains("selective"))
      rd();
    if (line.toLowerCase().contains("cartesian"))
      setFractionalCoordinates(false);
    for (int i = 0; i < ac; i++)
      addAtomXYZSymName(getTokensStr(rd()), 0, null, atomLabels.get(i));
  }

}
