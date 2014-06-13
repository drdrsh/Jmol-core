package org.jmol.api;


import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.thread.JmolThread;



public interface MinimizerInterface {

  public abstract boolean minimize(int steps, double crit, BS bsSelected, 
                                   BS bsFixed, boolean haveFixed, 
                                   boolean isSilent, String ff) throws Exception;
  public abstract MinimizerInterface setProperty(String propertyName, Object propertyValue);
  public abstract Object getProperty(String propertyName, int param);
  public abstract void calculatePartialCharges(Bond[] bonds, int bondCount, Atom[] atoms, BS bsAtoms);
  public abstract boolean startMinimization();
  public abstract boolean stepMinimization();
  public abstract void endMinimization();
  public abstract boolean minimizationOn();
  public abstract JmolThread getThread();
}
