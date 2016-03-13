/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-26 16:57:51 -0500 (Thu, 26 Apr 2007) $
 * $Revision: 7502 $
 *
 * Copyright (C) 2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.util;

import org.jmol.java.BS;

public interface Node {
  
  // abstracts out the essential pieces for SMILES processing
  
  public int getAtomicAndIsotopeNumber();
  public String getAtomName();
  public int getAtomSite();
  public int getBondedAtomIndex(int j);
  public int getCovalentBondCount();
  public int getCovalentHydrogenCount();
  public Edge[] getEdges();
  public int getElementNumber();
  public int getFormalCharge();
  public int getIndex();
  public int getIsotopeNumber();
  public int getValence();
  public void set(float x, float y, float z);
  
  /**
   * @param property  "property_xxxx"
   * @return value or Float.NaN
   */

  public float getFloatProperty(String property);

  // abstracts out the essential pieces for SMARTS processing
  
  public BS findAtomsLike(String substring);
  public String getAtomType();
  public int getModelIndex();
  public int getImplicitHydrogenCount();
  public int getBondCount();
  public int getAtomNumber();
  public int getMissingHydrogenCount();

}
