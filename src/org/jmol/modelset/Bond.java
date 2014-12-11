/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.modelset;

import org.jmol.util.C;
import org.jmol.util.Edge;
import org.jmol.util.Node;
import org.jmol.viewer.JC;

public class Bond extends Edge {

  public Atom atom1;
  public Atom atom2;

  public short mad;
  public short colix;

  /**
   * @j2sIgnoreSuperConstructor
   * @param atom1
   * @param atom2
   * @param order
   * @param mad
   * @param colix
   */
  public Bond(Atom atom1, Atom atom2, int order,
              short mad, short colix) {
    this.atom1 = atom1;
    this.atom2 = atom2;
    this.colix = colix;
    setOrder(order);
    setMad(mad);
  }

  public void setMad(short mad) {
    this.mad = mad;
    setShapeVisibility(mad != 0);
  }

  public int shapeVisibilityFlags;
  
  void setShapeVisibility(boolean isVisible) {
    boolean wasVisible = ((shapeVisibilityFlags & myVisibilityFlag) != 0);
    if (wasVisible == isVisible)
      return;
    atom1.addDisplayedBond(myVisibilityFlag, isVisible);
    atom2.addDisplayedBond(myVisibilityFlag, isVisible);
    if (isVisible)
      shapeVisibilityFlags |= myVisibilityFlag;
    else
      shapeVisibilityFlags &= ~myVisibilityFlag;
  }
            
  
  public final static int myVisibilityFlag = JC.getShapeVisibilityFlag(JC.SHAPE_STICKS);

  public String getIdentity() {
    return (index + 1) + " "+ Edge.getBondOrderNumberFromOrder(order) + " " + atom1.getInfo() + " -- "
        + atom2.getInfo() + " " + atom1.distance(atom2);
  }

  @Override
  public boolean isCovalent() {
    return (order & BOND_COVALENT_MASK) != 0;
  }

  @Override
  public boolean isHydrogen() {
    return isOrderH(order);
  }

  public static boolean isOrderH(int order) {
    return (order & BOND_HYDROGEN_MASK) != 0;
  }

  boolean isStereo() {
    return (order & BOND_STEREO_MASK) != 0;
  }

  boolean isPartial() {
    return (order & BOND_PARTIAL_MASK) != 0;
  }

  boolean isAromatic() {
    return (order & BOND_AROMATIC_MASK) != 0;
  }

  boolean isPymolStyle() {
    return (order & BOND_PYMOL_MULT) == BOND_PYMOL_MULT;
  }

  public float getEnergy() {
    // hbonds only
    return 0;
  }
  
  public int getValence() {
    return (!isCovalent() ? 0
        : isPartial() || is(BOND_AROMATIC) ? 1
        : order & 7);
  }

  void deleteAtomReferences() {
    if (atom1 != null)
      atom1.deleteBond(this);
    if (atom2 != null)
      atom2.deleteBond(this);
    atom1 = atom2 = null;
  }

  public void setTranslucent(boolean isTranslucent, float translucentLevel) {
    colix = C.getColixTranslucent3(colix, isTranslucent, translucentLevel);
  }
  
  public void setOrder(int order) {
    if (atom1.getElementNumber() == 16 && atom2.getElementNumber() == 16)
      order |= BOND_SULFUR_MASK;
    if (order == BOND_AROMATIC_MASK)
      order = BOND_AROMATIC;
    this.order = order | (this.order & BOND_NEW);
  }

  @Override
  public int getAtomIndex1() {
    // required for Smiles parser
    return atom1.i;
  }
  
  @Override
  public int getAtomIndex2() {
    // required for Smiles parser
    return atom2.i;
  }
  
  @Override
  public int getCovalentOrder() {
    return Edge.getCovalentBondOrder(order);
  }

  public Atom getOtherAtom(Atom thisAtom) {
    return (atom1 == thisAtom ? atom2 : atom2 == thisAtom ? atom1 : null);
  }
  
  ////////////////////////////////////////////////////////////////
  
  public boolean is(int bondType) {
    return (order & ~BOND_NEW) == bondType;
  }

  @Override
  public Node getOtherAtomNode(Node thisAtom) {
    return (atom1 == thisAtom ? atom2 : atom2 == thisAtom ? atom1 : null);
  }
  
  @Override
  public String toString() {
    return atom1 + " - " + atom2;
  }

}
