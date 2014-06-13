package org.jmol.script;

import org.jmol.atomdata.RadiusData;
import org.jmol.java.BS;

public interface JmolMathExtension {

  public JmolMathExtension init(Object eval);

  public boolean evaluate(ScriptMathProcessor mp, T op, SV[] args, int tok)
      throws ScriptException;

  public Object getMinMax(Object floatOrSVArray, int intValue);

  BS setContactBitSets(BS bsA, BS bsB, boolean localOnly, float distance,
                       RadiusData rd, boolean warnMultiModel);



}
