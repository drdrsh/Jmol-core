/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-02-25 17:19:14 -0600 (Sat, 25 Feb 2006) $
 * $Revision: 4529 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

package org.jmol.shapecgo;

import org.jmol.java.BS;
import org.jmol.script.T;
import org.jmol.shape.Mesh;
import org.jmol.shape.MeshCollection;
import org.jmol.shapespecial.Draw;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

public class CGO extends Draw {
  
  CGOMesh[] cmeshes = new CGOMesh[4];
  private CGOMesh cgoMesh;
  private boolean useColix;
  
  public CGO() {
    // for reflection
    super();
  }
  
  private void initCGO() {
    // TODO
    
  }

  @Override
  public void allocMesh(String thisID, Mesh m) {
    int index = meshCount++;
    meshes = cmeshes = (CGOMesh[]) AU.ensureLength(cmeshes,
        meshCount * 2);
    currentMesh = thisMesh = cgoMesh = cmeshes[index] = (m == null ? new CGOMesh(vwr, thisID,
        colix, index) : (CGOMesh) m);
    currentMesh.color = color;
    currentMesh.index = index;
    currentMesh.useColix = useColix;
    if (thisID != null && thisID != MeshCollection.PREVIOUS_MESH_ID
        && htObjects != null)
      htObjects.put(thisID.toUpperCase(), currentMesh);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void setProperty(String propertyName, Object value, BS bs) {

    if ("init" == propertyName) {
      initCGO();
      setPropertySuper("init", value, bs);
      return;
    }
    
    if ("setCGO" == propertyName) {
      Lst<Object> list = (Lst<Object>) value;
      setProperty("init", null, null);
      int n = list.size() - 1;
      setProperty("thisID", list.get(n), null);
      propertyName = "set";
      setProperty("set", value, null);
      return;
    }
    
    if ("set" == propertyName) {
      if (cgoMesh == null) {
        allocMesh(null, null);
        cgoMesh.colix = colix;
        cgoMesh.color = color;
        cgoMesh.useColix = useColix;
      }
      cgoMesh.isValid = setCGO((Lst<Object>) value);
      if (cgoMesh.isValid) {
        scale(cgoMesh, newScale);
        cgoMesh.initialize(T.fullylit, null, null);
        cgoMesh.title = title;
        cgoMesh.visible = true;
      }
      clean();
      return;
    }
    
    setPropertySuper(propertyName, value, bs);
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public boolean getPropertyData(String property, Object[] data) {
    if (property == "keys") {
      Lst<String> keys = (data[1] instanceof Lst<?> ? (Lst<String>) data[1] : new Lst<String>());
      data[1] = keys;
      keys.addLast("data");
      // will continue on to getPropertyIndex
    }
    if (property == "data")
      return CGOMesh.getData(data);
    return getPropDataMC(property, data);
  }

  @Override
  protected void deleteMeshElement(int i) {
    if (meshes[i] == currentMesh)
      currentMesh = cgoMesh = null;
    meshes = cmeshes = (CGOMesh[]) AU
        .deleteElements(meshes, i, 1);
  }

  @Override
  protected void setPropertySuper(String propertyName, Object value, BS bs) {
    currentMesh = cgoMesh;
    setPropMC(propertyName, value, bs);
    cgoMesh = (CGOMesh)currentMesh;  
  }

  @Override
  protected void clean() {
    for (int i = meshCount; --i >= 0;)
      if (meshes[i] == null || cmeshes[i].cmds == null || cmeshes[i].cmds.size() == 0)
        deleteMeshI(i);
  }

  private boolean setCGO(Lst<Object> data) {
    if (cgoMesh == null)
      allocMesh(null, null);
    cgoMesh.clear("cgo");
    return cgoMesh.set(data);
  }

  @Override
  protected void scale(Mesh mesh, float newScale) {
    // TODO
    
  }
  
  @Override
  public String getShapeState() {
    SB s = new SB();
    s.append("\n");
    appendCmd(s, myType + " delete");
    for (int i = 0; i < meshCount; i++) {
      CGOMesh mesh = cmeshes[i];
      s.append(getCommand2(mesh, mesh.modelIndex));
      if (!mesh.visible)
        s.append(" " + myType + " ID " + PT.esc(mesh.thisID) + " off;\n");
    }
    return s.toString();
  }

  @Override
  protected String getCommand2(Mesh mesh, int iModel) {
    CGOMesh cmesh = (CGOMesh) mesh;
    SB str = new SB();
    int modelCount = vwr.ms.mc;
    if (iModel >= 0 && modelCount > 1)
      appendCmd(str, "frame " + vwr.getModelNumberDotted(iModel));
    str.append("  CGO ID ").append(PT.esc(mesh.thisID));
    if (iModel < 0)
      iModel = 0;
    str.append(" [");
    int n = cmesh.cmds.size();
    for (int i = 0; i < n; i++)
      str.append(" " + cmesh.cmds.get(i));
    str.append(" ];\n");
    appendCmd(str, cmesh.getState("cgo"));
    if (cmesh.useColix)
      appendCmd(str, getColorCommandUnk("cgo", cmesh.colix, translucentAllowed));
    return str.toString();
  }
  
}
