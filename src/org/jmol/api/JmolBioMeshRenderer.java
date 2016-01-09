package org.jmol.api;

import org.jmol.modelset.ModelSet;
import org.jmol.render.ShapeRenderer;
import org.jmol.shape.Shape;
import org.jmol.shapebio.BioShape;
import org.jmol.viewer.Viewer;

public interface JmolBioMeshRenderer {

  boolean setColix(short colix);

  void setup(JmolRendererInterface g3d, ModelSet ms, Shape shape);

  void setViewerG3dShapeID(Viewer vwr, int shapeID);

  void setFancyRibbon(int i);

  void setFancyArrowHead(int i);

  void setFancyConic(int i, int tension);

  void renderMeshes();

  void initBS();

  boolean check(boolean doCap0, boolean doCap1);

  void initialize(ShapeRenderer bsr, BioShape bioShape, int monomerCount);
}
