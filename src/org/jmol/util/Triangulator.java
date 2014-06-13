package org.jmol.util;

import javajs.util.Lst;
import javajs.util.P4;

public interface Triangulator {

  public Lst<Object> intersectPlane(P4 plane, Lst<Object> v, int flags);
}
