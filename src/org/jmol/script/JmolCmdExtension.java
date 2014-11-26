package org.jmol.script;

import org.jmol.java.BS;
import org.jmol.viewer.ShapeManager;

public interface JmolCmdExtension {

  public JmolCmdExtension init(Object eval);

  public String dispatch(int iShape, boolean b, T[] st) throws ScriptException;

  public Object getBitsetIdent(BS bs, String label, Object tokenValue,
                               boolean useAtomMap, int index,
                               boolean isExplicitlyAll);

  public boolean evalParallel(ScriptContext context,
                                  ShapeManager shapeManager);

}
