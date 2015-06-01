package org.jmol.adapter.readers.xtal;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.api.JmolAdapter;
import org.jmol.util.Parser;

import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

/**
 * http://cms.mpi.univie.ac.at/vasp/
 * 
 * @author Pieremanuele Canepa, Wake Forest University, Department of Physics
 *         Winston Salem, NC 27106, canepap@wfu.edu (pcanepa@mit.edu)
 * 
 * @version 1.0
 */

public class VaspPoscarReader extends AtomSetCollectionReader {

  protected Lst<String> atomLabels;
  private boolean haveAtomLabels = true;
  private boolean atomsLabeledInline;
  protected int ac;
  protected int atomPt = Integer.MIN_VALUE;
  protected String title;

  @Override
  protected void initializeReader() throws Exception {
    readStructure();
    continuing = false;
  }
  
  protected void readStructure() throws Exception {
    readJobTitle();
    readUnitCellVectors();
    readMolecularFormula();
    readCoordinates();
  }

  @Override
  protected void finalizeSubclassReader() {
    if (!haveAtomLabels && !atomsLabeledInline)     
      appendLoadNote("VASP POSCAR reader using pseudo atoms Al B C Db...");
  }

  private void readJobTitle() throws Exception {
    asc.setAtomSetName(title = rd().trim());
  }

  protected void readUnitCellVectors() throws Exception {
    // Read Unit Cell
    setSpaceGroupName("P1");
    setFractionalCoordinates(true);
    float scaleFac = parseFloatStr(rdline().trim());
    float[] unitCellData = new float[9];
    String s = rdline() + " " + rdline() + " " + rdline();
    Parser.parseStringInfestedFloatArray(s, null, unitCellData);
    if (scaleFac != 1)
      for (int i = 0; i < unitCellData.length; i++)
        unitCellData[i] *= scaleFac;
    addPrimitiveLatticeVector(0, unitCellData, 0);
    addPrimitiveLatticeVector(1, unitCellData, 3);
    addPrimitiveLatticeVector(2, unitCellData, 6);
  }

  protected String[] elementLabel;
  
  /**
   * try various ways to read the optional atom labels. There is no convention
   * here.
   * 
   * @throws Exception
   */
  protected void readMolecularFormula() throws Exception {
    //   H    C    O    Be   C    H
    if (elementLabel == null)
      elementLabel = PT.getTokens(discardLinesUntilNonBlank());
    String[] elementCounts;
    if (PT.parseInt(elementLabel[0]) == Integer.MIN_VALUE) {
      elementCounts = PT.getTokens(rdline());
      //   6    24    18     6     6    24
    } else {
      elementCounts = elementLabel;
      elementLabel = PT.split(title, " ");
      if (elementLabel.length != elementCounts.length
          || elementLabel[0].length() > 2) {
        elementLabel = PT.split(
            "Al B C Db Eu F Ga Hf I K Li Mn N O P Ru S Te U V W Xe Yb Zn", " ");
        haveAtomLabels = false;
      }
    }
    String[] labels = elementLabel;
    if (elementCounts.length == 1 && atomPt >= 0 && atomPt < 2) {
      labels = new String[] { elementLabel[atomPt] };
    }
    SB mf = new SB();
    atomLabels = new Lst<String>();
    ac = 0;
    for (int i = 0; i < elementCounts.length; i++) {
      int n = Integer.parseInt(elementCounts[i]);
      ac += n;
      String label = labels[i];
      mf.append(" ").append(label).appendI(n);
      for (int j = n; --j >= 0;)
        atomLabels.addLast(label);
    }
    String s = mf.toString();
    appendLoadNote(ac + " atoms identified for" + s);
    appendLoadNote(s);
    asc.newAtomSet();
    asc.setAtomSetName(s);
  }

  protected void readCoordinates() throws Exception {
    // If Selective is there, then skip a line 
    if (discardLinesUntilNonBlank().toLowerCase().contains("selective"))
      rd();
    if (line.toLowerCase().contains("cartesian"))
      setFractionalCoordinates(false);
    for (int i = 0; i < ac; i++) {
      String[] tokens = PT.getTokens(rdline());
      if (i == 0 && !atomsLabeledInline && tokens.length > 3 && JmolAdapter.getElementNumber(tokens[3]) > 0)
        atomsLabeledInline = true;
      String label = (atomsLabeledInline ? tokens[3] : atomLabels.get(i));
      addAtomXYZSymName(tokens, 0, null, label);
    }
  }

  protected String rdline() throws Exception  {
    rd();
    if (line != null && line.startsWith("["))
      line = line.substring(line.indexOf("]") + 1).trim();
    return line;
  }
}
