/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-26 01:48:23 -0500 (Tue, 26 Sep 2006) $
 * $Revision: 5729 $
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.adapter.readers.pymol;

import javajs.util.Lst;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.readers.pdb.PdbReader;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.Structure;
import org.jmol.api.JmolAdapter;
import org.jmol.api.PymolAtomReader;
import org.jmol.c.STR;
import org.jmol.java.BS;
import org.jmol.script.T;
import org.jmol.util.BoxInfo;
import org.jmol.util.BSUtil;
import org.jmol.util.Logger;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.V3;

/**
 * PyMOL PSE (binary Python session) file reader.
 * development started Feb 2013 Jmol 13.1.13
 * reasonably full implementation May 2013 Jmol 13.1.16
 * 
 * PyMOL state --> Jmol model 
 * PyMOL object --> Jmol named atom set, isosurface, CGO, or measurement 
 * PyMOL group --> Jmol named atom set (TODO: add isosurfaces and measures to these?) 
 * PyMOL movie: an initial view and a set of N "frames" 
 * PyMOL frame: references (a) a state, (b) a script, and (c) a view
 * PyMOL scene --> Jmol scene, including view, frame, visibilities, colors
 *
 * using set LOGFILE, we can dump this to a readable form.
 * 
 * trajectories are not supported yet.
 * 
 * Basic idea is as follows: 
 * 
 * 1) Pickle file is read into a Hashtable.
 * 2) Atoms, bonds, and structures are created, as per other readers, from MOLECULE objects
 * 3) Rendering of atoms and bonds is interpreted as JmolObject objects via PyMOLScene 
 * 3) Other objects such as electron density maps, compiled graphical objects, and 
 *    measures are interpreted, creating more JmolObjects
 * 3) JmolObjects are finalized after file reading takes place by a call from ModelLoader
 *    back here to finalizeModelSet(), which runs PyMOLScene.setObjects, which runs JmolObject.finalizeObject.
 * 
 *  TODO: Handle discrete objects, DiscreteAtmToIdx? 
 *     
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 * 
 */

public class PyMOLReader extends PdbReader implements PymolAtomReader {

  private static final int MIN_RESNO = -1000; // minimum allowed residue number

  private static String nucleic = " A C G T U ADE THY CYT GUA URI DA DC DG DT DU ";

  private boolean allowSurface = true;
  private boolean doResize;
  private boolean doCache;
  private boolean isStateScript;
  private boolean sourcePNGJ;

  private int ac0;
  private int ac;
  private int stateCount;
  private int structureCount;

  private boolean isHidden;

  private BS bsStructureDefined = new BS();
  private BS bsBytesExcluded;

  private int[] atomMap;
  private Map<String, BS> ssMapSeq;

  private PyMOLScene pymolScene;

  private P3 xyzMin = P3.new3(1e6f, 1e6f, 1e6f);
  private P3 xyzMax = P3.new3(-1e6f, -1e6f, -1e6f);

  private int nModels;
  private boolean logging;

  private BS[] reps = new BS[PyMOL.REP_JMOL_MAX];

  private boolean isMovie;

  private int pymolFrame;
  private boolean allStates;
  private int totalAtomCount;
  private int pymolVersion;
  private P3[] trajectoryStep;
  private int trajectoryPtr;

  private String objectName;

  //private List<List<Object>> selections;
  private Map<String, Lst<Object>> volumeData;
  private Lst<Lst<Object>> mapObjects;
  private boolean haveMeasurements;
  private int[] frames;
  private Hashtable<Integer, Lst<Object>> uniqueSettings;
  private Atom[] atoms;
  private boolean haveScenes;
  private int baseAtomIndex; // preliminary only; may be revised later if load FILES
  private int baseModelIndex; // preliminary only; may be revised later if load FILES

  private Lst<Object> sceneOrder;

  private int bondCount;
  @Override
  protected void setup(String fullPath, Map<String, Object> htParams, Object reader) {
    isBinary = mustFinalizeModelSet = true;
    setupASCR(fullPath, htParams, reader);
  }

  @Override
  protected void initializeReader() throws Exception {
    baseAtomIndex = ((Integer) htParams.get("baseAtomIndex")).intValue();
    baseModelIndex = ((Integer) htParams.get("baseModelIndex")).intValue();
    asc.setInfo("noAutoBond",
        Boolean.TRUE);
    asc.setAtomSetAuxiliaryInfo("pdbNoHydrogens", Boolean.TRUE);
    asc
        .setInfo("isPyMOL", Boolean.TRUE);
    if (isTrajectory)
      trajectorySteps = new Lst<P3[]>();

    isStateScript = htParams.containsKey("isStateScript");
    sourcePNGJ = htParams.containsKey("sourcePNGJ");
    doResize = checkFilterKey("DORESIZE");
    allowSurface = !checkFilterKey("NOSURFACE");
    doCache = checkFilterKey("DOCACHE");
    
    // logic is as follows:
    //
    // isStateScript --> some of this is already done for us. For example, everything is
    //                   already colored and scaled, and there is no need to set the perspective. 
    //
    // doCache && sourcePNGJ   --> reading from a PNGJ that was created with DOCACHE filter
    //                         --> no need for caching.
    //
    // !doCache && sourcePNGJ  --> "standard" PNGJ created without caching
    //                         --> ignore the fact that this is from a PNGJ file
    //
    // doCache && !sourcePNGJ  --> we need to cache surfaces
    //
    // !doCache && !sourcePNGJ --> standard PSE loading

    if (doCache && sourcePNGJ)
      doCache = false;
    else if (sourcePNGJ && !doCache)
      sourcePNGJ = false;
    if (doCache)
      bsBytesExcluded = new BS();
    logging = false;
    super.initializeReader();
  }

  @Override
  public void processBinaryDocument() throws Exception {
    PickleReader reader = new PickleReader(binaryDoc, vwr);
    Map<String, Object> map = reader.getMap(logging);
    reader = null;
    process(map);
  }

  @Override
  protected void setAdditionalAtomParameters(Atom atom) {
    // override PDBReader settings
  }

  @Override
  protected void finalizeReader() throws Exception {
    finalizeReaderPDB();
    asc.setTensors();
  }
  /**
   * At the end of the day, we need to finalize all the JmolObjects, set the
   * trajectories, and, if filtered with DOCACHE, cache a streamlined binary
   * file for inclusion in the PNGJ file.
   * 
   */
  @Override
  public void finalizeModelSet() {

    pymolScene.setReaderObjects();
    
    if (haveMeasurements) {
      appendLoadNote(vwr.getMeasurementInfoAsString());
      setLoadNote();
    }
    
    if (haveScenes) {
      String[] scenes = new String[sceneOrder.size()];
      for (int i = scenes.length; --i >= 0;)
        scenes[i] = (String) sceneOrder.get(i);
      Map<String, Object> info = vwr.getModelSetAuxiliaryInfo();
      info.put("scenes", scenes);
    }
    
    vwr.ms.setTrajectoryBs(BSUtil.newBitSet2(baseModelIndex,
        vwr.ms.mc));
    if (!isStateScript)
      pymolScene.setFrameObject(0, null);

    // exclude unnecessary named objects

    if (bsBytesExcluded != null) {
      int nExcluded = bsBytesExcluded.cardinality();
      byte[] bytes0 = (byte[]) vwr.getFileAsBytes(filePath, null);
      byte[] bytes = new byte[bytes0.length - nExcluded];
      for (int i = bsBytesExcluded.nextClearBit(0), n = bytes0.length, pt = 0; i < n; i = bsBytesExcluded
          .nextClearBit(i + 1))
        bytes[pt++] = bytes0[i];
      bytes0 = null;
      String fileName = filePath;
      vwr.cachePut(fileName, bytes);
    }
  }

  /**
   * The main processor.
   * 
   * @param map
   */
  @SuppressWarnings("unchecked")
  private void process(Map<String, Object> map) {

    pymolVersion = ((Integer) map.get("version")).intValue();
    appendLoadNote("PyMOL version: " + pymolVersion);

    // create settings and uniqueSettings lists
    Lst<Object> settings = fixSettings(getMapList(map, "settings"));
    sceneOrder = getMapList(map, "scene_order");
    haveScenes = getFrameScenes(map);
    Lst<Object> file = listAt(settings, PyMOL.session_file);
    if (file != null)
      Logger.info("PyMOL session file: " + file.get(2));
    //asc.setAtomSetCollectionAuxiliaryInfo("settings", settings);
    setUniqueSettings(getMapList(map, "unique_settings"));
    pymolScene = new PyMOLScene(this, vwr, settings, uniqueSettings, 
        pymolVersion, haveScenes, baseAtomIndex, baseModelIndex, doCache, filePath);

    // just log and display some information here

    logging = (vwr.getLogFileName().length() > 0);
    Lst<Object> names = getMapList(map, "names");
    for (Map.Entry<String, Object> e : map.entrySet()) {
      String name = e.getKey();
      Logger.info(name);
      if (name.equals("names")) {
        for (int i = 1; i < names.size(); i++) {
          Lst<Object> obj = listAt(names, i);
          Logger.info("  " + stringAt(obj, 0));
        }
      }
    }
    if (logging) {
      if (logging)
        vwr.log("$CLEAR$");
      //String s = map.toString();//.replace('=', '\n');
      for (Map.Entry<String, Object> e : map.entrySet()) {
        String name = e.getKey();
        if (!"names".equals(name)) {
          vwr.log("\n===" + name + "===");
          vwr.log(PT.rep(e.getValue().toString(), "[",
              "\n["));
        }
      }
      vwr.log("\n===names===");
      for (int i = 1; i < names.size(); i++) {
        vwr.log("");
        Lst<Object> list = (Lst<Object>) names.get(i);
        vwr.log(" =" + list.get(0).toString() + "=");
        try {
          vwr.log(PT.rep(list.toString(), "[", "\n["));
        } catch (Throwable e) {
          //
        }
      }
    }

    // set up additional colors
    // not 100% sure what color clamping is, but this seems to work.
    addColors(getMapList(map, "colors"), pymolScene
        .globalSetting(PyMOL.clamp_colors) != 0);

    // set a few global flags
    allStates = (pymolScene.globalSetting(PyMOL.all_states) != 0);
    pymolFrame = (int) pymolScene.globalSetting(PyMOL.frame);
    // discover totalAtomCount and stateCount:
    getAtomAndStateCount(names);
    pymolScene.setStateCount(stateCount);

    int pymolState = (int) pymolScene.globalSetting(PyMOL.state);
    if (!isMovie)
      pymolScene.setFrameObject(T.frame, (allStates ? Integer.valueOf(-1)
          : Integer.valueOf(pymolState - 1)));
    appendLoadNote("frame=" + pymolFrame + " state=" + pymolState
        + " all_states=" + allStates);

    // resize frame
    if (!isStateScript && doResize) {
      int width = 0, height = 0;
      try {
        width = intAt(getMapList(map, "main"), 0);
        height = intAt(getMapList(map, "main"), 1);
      } catch (Exception e) {
        // ignore
      }
      String note;
      if (width > 0 && height > 0) {
        note = "PyMOL dimensions width=" + width + " height=" + height;
        asc.setInfo(
            "perferredWidthHeight", new int[] { width, height });
        //Dimension d = 
        vwr.resizeInnerPanel(width, height);
      } else {
        note = "PyMOL dimensions?";
      }
      appendLoadNote(note);
    }
    // PyMOL setting all_states disables movies
    Lst<Object> mov;
    if (!isStateScript && !allStates
        && (mov = getMapList(map, "movie")) != null) {
      int frameCount = intAt(mov, 0);
      if (frameCount > 0)
        processMovie(mov, frameCount);
    }
    if (totalAtomCount == 0)
      asc.newAtomSet();
    //selections = new List<List<Object>>();

    if (allStates && desiredModelNumber == Integer.MIN_VALUE) {
      // if all_states and no model number indicated, display all states
    } else if (isMovie) {
      // otherwise, if a movie, load all states
      switch (desiredModelNumber) {
      case Integer.MIN_VALUE:
        break;
      default:
        desiredModelNumber = frames[(desiredModelNumber > 0
            && desiredModelNumber <= frames.length ? desiredModelNumber
            : pymolFrame) - 1];
        pymolScene.setFrameObject(T.frame, Integer
            .valueOf(desiredModelNumber - 1));
        break;
      }
    } else if (desiredModelNumber == 0) {
      // otherwise if you specify model "0", only load the current PyMOL state
      desiredModelNumber = pymolState;
    } else {
      // load only the state you request, or all states, if you don't specify
    }
    int n = names.size();
    
    for (int j = 0; j < stateCount; j++) {
      if (!doGetModel(++nModels, null))
        continue;
      model(nModels);
      pymolScene.currentAtomSetIndex = asc.iSet;
      if (isTrajectory) {
        trajectoryStep = new P3[totalAtomCount];
        trajectorySteps.addLast(trajectoryStep);
        trajectoryPtr = 0;
      }
      for (int i = 1; i < n; i++)
        processObject(listAt(names, i), true, j);
    }
    for (int i = 1; i < n; i++)
      processObject(listAt(names, i), false, 0);
    pymolScene.setReaderObjectInfo(null, 0, null, false, null, null, null);

    // not currently generating selections
    //processSelections();

    // meshes are special objects that depend upon grid map data
    if (mapObjects != null && allowSurface)
      processMeshes(); 

    // trajectories are not supported yet
    if (isTrajectory) {
      appendLoadNote("PyMOL trajectories read: " + trajectorySteps.size());
      asc.finalizeTrajectoryAs(trajectorySteps, null);
    }

    processDefinitions();
    processSelectionsAndScenes(map);
    // no need to render if this is a state script
    pymolScene.finalizeVisibility();
    if (!isStateScript) {
      // same idea as for a Jmol state -- session reinitializes
      vwr.initialize(true);
      addJmolScript(pymolScene.getViewScript(getMapList(map, "view"))
          .toString());
    }
    if (ac == 0)
      asc.setInfo("dataOnly",
          Boolean.TRUE);
    pymolScene.offsetObjects();
  }

  /**
   * Recent PyMOL files may not have all settings. 
   * For now, we just add null values;
   * @param settings
   * @return settings
   */
  private Lst<Object> fixSettings(Lst<Object> settings) {
    int n = settings.size();
    for (int i = 0; i < n; i++) {
      @SuppressWarnings("unchecked")
      int i2 = intAt((Lst<Object>) settings.get(i), 0);
      while (i < i2) {
        Logger.info("PyMOL reader adding null settings #" + i);
        settings.add(i++, new Lst<Object>());
        n++;
      }
      
    }
    return settings;
  }

  /**
   * remove all scenes that do not define a frame.
   * @param map
   * @return  true if there are scenes that define a frame
   */
  @SuppressWarnings("unchecked")
  private boolean getFrameScenes(Map<String, Object> map) {
    if (sceneOrder == null)
      return false;
    Map<String, Object> scenes = (Map<String, Object>) map.get("scene_dict");
    for (int i = 0; i < sceneOrder.size(); i++) {
      String name = stringAt(sceneOrder, i);
      Lst<Object> thisScene = getMapList(scenes, name);
      if (thisScene == null || thisScene.get(2) == null)
        sceneOrder.remove(i--);
    }
    return (sceneOrder != null && sceneOrder.size() != 0);
  }

  /**
   * Create uniqueSettings from the "unique_settings" map item.
   * This will be used later in processing molecule objects.
   * 
   * @param list
   * @return max id
   */
  @SuppressWarnings("unchecked")
  private int setUniqueSettings(Lst<Object> list) {
    uniqueSettings = new Hashtable<Integer, Lst<Object>>();
    int max = 0;
    if (list != null && list.size() != 0) {
      for (int i = list.size(); --i >= 0;) {
        Lst<Object> atomSettings = (Lst<Object>) list.get(i);
        int id = intAt(atomSettings, 0);
        if (id > max)
          max = id;
        Lst<Object> mySettings = (Lst<Object>) atomSettings.get(1);
        for (int j = mySettings.size(); --j >= 0;) {
          Lst<Object> setting = (Lst<Object>) mySettings.get(j);
          int uid = (id << 10) + intAt(setting, 0);
          uniqueSettings.put(Integer.valueOf(uid), setting);
          Logger.info("PyMOL unique setting " + id + " " + setting);
        }
      }
    }
    return max;
  }

  /**
   * Add new colors from the main "colors" map object.
   * Not 100% clear how color clamping works.
   * 
   * @param colors
   * @param isClamped
   */
  private void addColors(Lst<Object> colors, boolean isClamped) {
    if (colors == null || colors.size() == 0)
      return;
    // note, we are ignoring lookup-table colors
    for (int i = colors.size(); --i >= 0;) {
      Lst<Object> c = listAt(colors, i);
      PyMOL.addColor((Integer) c.get(1), isClamped ? PyMOLScene.colorSettingClamped(c)
          : PyMOLScene.colorSetting(c));
    }
  }

  /**
   * Look through all named objects for molecules, counting
   * atoms and also states; see if trajectories are compatible (experimental).
   *  
   * @param names
   */
  private void getAtomAndStateCount(Lst<Object> names) {
    int n = 0;
    for (int i = 1; i < names.size(); i++) {
      Lst<Object> execObject = listAt(names, i);
      int type = intAt(execObject, 4);
      if (!checkObject(execObject))
        continue;
      if (type == PyMOL.OBJECT_MOLECULE) {
        Lst<Object> pymolObject = listAt(execObject, 5);
        Lst<Object> states = listAt(pymolObject, 4);
        int ns = states.size();
        if (ns > stateCount)
          stateCount = ns;
        int nAtoms = listAt(pymolObject, 7).size();
        for (int j = 0; j < ns; j++) {
          Lst<Object> state = listAt(states, j);
          Lst<Object> idxToAtm = listAt(state, 3);
          if (idxToAtm == null) {
            isTrajectory = false;
          } else {
            int m = idxToAtm.size();
            n += m;
            if (isTrajectory && m != nAtoms)
              isTrajectory = false;
          }
        }
      }
    }
    totalAtomCount = n;
    Logger.info("PyMOL total atom count = " + totalAtomCount);
    Logger.info("PyMOL state count = " + stateCount);
  }

  private boolean checkObject(Lst<Object> execObject) {
    objectName = stringAt(execObject, 0);
    isHidden = (intAt(execObject, 2) != 1);
    return (objectName.indexOf("_") != 0);
  }

  /**
   * Create a JmolObject that will represent the movie.
   * For now, only process unscripted movies without views.
   * 
   * @param mov
   * @param frameCount
   */
  private void processMovie(Lst<Object> mov, int frameCount) {
    Map<String, Object> movie = new Hashtable<String, Object>();
    movie.put("frameCount", Integer.valueOf(frameCount));
    movie.put("currentFrame", Integer.valueOf(pymolFrame - 1));
    boolean haveCommands = false, haveViews = false, haveFrames = false;
    Lst<Object> list = listAt(mov, 4);
    for (int i = list.size(); --i >= 0;)
      if (intAt(list, i) != 0) {
        frames = new int[list.size()];
        for (int j = frames.length; --j >= 0;)
          frames[j] = intAt(list, j) + 1;
        movie.put("frames", frames);
        haveFrames = true;
        break;
      }
    Lst<Object> cmds = listAt(mov, 5);
    String cmd;
    for (int i = cmds.size(); --i >= 0;)
      if ((cmd = stringAt(cmds, i)) != null && cmd.length() > 1) {
        cmds = fixMovieCommands(cmds);
        if (cmds != null) {
          movie.put("commands", cmds);
          haveCommands = true;
          break;
        }
      }
    Lst<Object> views = listAt(mov, 6);
    Lst<Object> view;
    for (int i = views.size(); --i >= 0;)
      if ((view = listAt(views, i)) != null && view.size() >= 12
          && view.get(1) != null) {
        haveViews = true;
        views = fixMovieViews(views);
        if (views != null) {
          movie.put("views", views);
          break;
        }
      }
    appendLoadNote("PyMOL movie frameCount = " + frameCount);
    if (haveFrames && !haveCommands && !haveViews) {
      // simple animation
      isMovie = true;
      pymolScene.setReaderObjectInfo(null, 0, null, false, null, null, null);
      pymolScene.setFrameObject(T.movie, movie);
    } else {
      //isMovie = true;  for now, no scripted movies
      //pymol.put("movie", movie);
    }
  }

  /**
   * Could implement something here that creates a Jmol view.
   * 
   * @param views
   * @return new views
   */
  private static Lst<Object> fixMovieViews(Lst<Object> views) {
    // TODO -- PyMOL to Jmol views
    return views;
  }

  /**
   * Could possibly implement something here that interprets PyMOL script commands.
   * 
   * @param cmds
   * @return new cmds
   */
  private static Lst<Object> fixMovieCommands(Lst<Object> cmds) {
    // TODO -- PyMOL to Jmol commands
    return cmds;
  }
  
  /**
   * The main object processor. Not implemented: ALIGNMENT, CALLBACK, SLICE,
   * SURFACE
   * 
   * @param execObject
   * @param moleculeOnly
   * @param iState
   */
  @SuppressWarnings("unchecked")
  private void processObject(Lst<Object> execObject, boolean moleculeOnly,
                             int iState) {
    if (execObject == null)
      return;
    int type = intAt(execObject, 4);
    Lst<Object> startLen = (Lst<Object>) execObject
        .get(execObject.size() - 1);
    if ((type == PyMOL.OBJECT_MOLECULE) != moleculeOnly || !checkObject(execObject))
      return;
    Lst<Object> pymolObject = listAt(execObject, 5);
    Lst<Object> stateSettings = null;
    if (type == PyMOL.OBJECT_MOLECULE) {
      Lst<Object> states = listAt(pymolObject, 4);
      Lst<Object> state = listAt(states, iState);
      Lst<Object> idxToAtm = listAt(state, 3);
      if (iState > 0 && (idxToAtm == null || idxToAtm.size() == 0))
        return;
      stateSettings = listAt(state, 7);
    } else   if (iState > 0) {
        return;
    }
    
    Logger.info("PyMOL model " + (nModels) + " Object " + objectName
        + (isHidden ? " (hidden)" : " (visible)"));
    Lst<Object> objectHeader = listAt(pymolObject, 0);
    String parentGroupName = (execObject.size() < 8 ? null : stringAt(
        execObject, 6));
    if (" ".equals(parentGroupName))
      parentGroupName = null;
    pymolScene.setReaderObjectInfo(objectName, type, parentGroupName, isHidden, listAt(objectHeader, 8), stateSettings, (moleculeOnly ? "_" + (iState + 1) : ""));
    BS bsAtoms = null;
    boolean doExclude = (bsBytesExcluded != null);
    String msg = null;
    switch (type) {
    default:
      msg = "" + type;
      break;
    case PyMOL.OBJECT_SELECTION:
      // not treating these properly yet
      //selections.addLast(execObject);
      break;
    case PyMOL.OBJECT_MOLECULE:
      doExclude = false;
      bsAtoms = processMolecule(pymolObject, iState);
      break;
    case PyMOL.OBJECT_MEASURE:
      doExclude = false;
      processMeasure(pymolObject);
      break;
    case PyMOL.OBJECT_MAPMESH:
    case PyMOL.OBJECT_MAPDATA:
      processMap(pymolObject, type == PyMOL.OBJECT_MAPMESH, false);
      break;
    case PyMOL.OBJECT_GADGET:
      processGadget(pymolObject);
      break;
    case PyMOL.OBJECT_GROUP:
        if (parentGroupName == null)
          parentGroupName = ""; // force creation
      break;
    case PyMOL.OBJECT_CGO:
      msg = "CGO";
      processCGO(pymolObject);
      break;

      // unimplemented:
      
    case PyMOL.OBJECT_ALIGNMENT:
      msg = "ALIGNEMENT";
      break;
    case PyMOL.OBJECT_CALCULATOR:
      msg = "CALCULATOR";
      break;
    case PyMOL.OBJECT_CALLBACK:
      msg = "CALLBACK";
      break;
    case PyMOL.OBJECT_SLICE:
      msg = "SLICE";
      break;
    case PyMOL.OBJECT_SURFACE:
      msg = "SURFACE";
      break;
    }
    if (parentGroupName != null || bsAtoms != null) {
      PyMOLGroup group = pymolScene.addGroup(execObject, parentGroupName, type);
      if (bsAtoms != null)
        bsAtoms = group.addGroupAtoms(bsAtoms);
    }
    if (doExclude) {
      int i0 = intAt(startLen, 0);
      int len = intAt(startLen, 1);
      bsBytesExcluded.setBits(i0, i0 + len);
      Logger.info("cached PSE file excludes PyMOL object type " + type
          + " name=" + objectName + " len=" + len);
    }
    if (msg != null)
      Logger.error("Unprocessed object type " + msg + " " + objectName);
  }

  /**
   * Create a CGO JmolObject, just passing on key information. 
   * 
   * @param pymolObject
   */
  private void processCGO(Lst<Object> pymolObject) {
    if (isStateScript)
      return;
    if (isHidden)
      return;
    Lst<Object> data = listAt(listAt(pymolObject, 2), 0);
    int color = PyMOL.getRGB(intAt(listAt(pymolObject, 0), 2));
    String name = pymolScene.addCGO(data, color);
    if (name != null)
      appendLoadNote("CGO " + name);
  }

  /**
   * Only process _e_pot objects -- which we need for color settings 
   * @param pymolObject
   */
  private void processGadget(Lst<Object> pymolObject) {
    if (objectName.endsWith("_e_pot"))
      processMap(pymolObject, true, true);
  }

  /**
   * Create mapObjects and volumeData; create an ISOSURFACE JmolObject.
   * 
   * @param pymolObject
   * @param isObject
   * @param isGadget 
   */
  private void processMap(Lst<Object> pymolObject, boolean isObject, boolean isGadget) {
    if (isObject) {
      if (sourcePNGJ)
        return;
      if (isHidden && !isGadget)
        return; // for now
      if (mapObjects == null)
        mapObjects = new Lst<Lst<Object>>();
      mapObjects.addLast(pymolObject);
    } else {
      if (volumeData == null)
        volumeData = new Hashtable<String, Lst<Object>>();
      volumeData.put(objectName, pymolObject);
      if (!isHidden && !isStateScript)
        pymolScene.addIsosurface(objectName);
    }
    pymolObject.addLast(objectName);
  }

//  private void processSelections() {
//    for (int i = selections.size(); --i >= 0;) {
//      List<Object> execObject = selections.get(i);
//      checkObject(execObject);
//      processObjectSelection(listAt(execObject, 5));
//    }
//  }
////  [Ca, 1, 0, 
////   [0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], -1, 
////   [
////   [H115W, 
////   [2529, 2707], 
////   [1, 1]]], ]
//  private void processObjectSelection(List<Object> pymolObject) {
//    BS bs = new BS();
//    for (int j = pymolObject.size(); --j >= 0;) {
//      List<Object> data = listAt(pymolObject, j);
//      String parent = stringAt(data, 0);
//      atomMap = htAtomMap.get(parent);
//      if (atomMap == null)
//        continue;
//      List<Object> atoms = listAt(data, 1);
//      for (int i = atoms.size(); --i >= 0;) {
//        int ia = atomMap[intAt(atoms, i)];
//        if (ia >= 0)
//          bs.set(ia);
//      }
//    }
//    if (!bs.isEmpty())
//      htNames.put(objectName, bs);
//  }


  /**
   * Create a MEASURE JmolObject.
   * 
   * @param pymolObject
   */
  private void processMeasure(Lst<Object> pymolObject) {
    if (isStateScript)
      return;
    if (isHidden)
      return; // will have to reconsider this if there is a movie, though
    Logger.info("PyMOL measure " + objectName);
    Lst<Object> measure = listAt(listAt(pymolObject, 2), 0);
    int pt;
    int nCoord = (measure.get(pt = 1) instanceof Lst<?> ? 2 : measure
        .get(pt = 4) instanceof Lst<?> ? 3
        : measure.get(pt = 6) instanceof Lst<?> ? 4 : 0);
    if (nCoord == 0)
      return;
    Lst<Object> setting = listAt(pymolObject, 0);
    BS bsReps = getBsReps(listAt(setting, 3));
    Lst<Object> list = listAt(measure, pt);
    Lst<Object> offsets = listAt(measure, 8);
    boolean haveLabels = (measure.size() > 8);
    int color = intAt(setting, 2);
    if (pymolScene.addMeasurements(null, nCoord, list, bsReps, color, offsets, haveLabels))
      haveMeasurements = true;    
  }

  /**
   * Create everything necessary to generate a molecule in Jmol. 
   * 
   * @param pymolObject
   * @param iState
   * @return atom set only if this is a trajectory.
   */
  private BS processMolecule(Lst<Object> pymolObject, int iState) {
    Lst<Object> states = listAt(pymolObject, 4);
    Lst<Object> state = listAt(states, iState);
    Lst<Object> idxToAtm = listAt(state, 3);
    int n = (idxToAtm == null ? 0 : idxToAtm.size());
    if (n == 0)
      return null;

    ac = ac0 = asc.ac;
    int nAtoms = intAt(pymolObject, 3);
    if (nAtoms == 0)
      return null;
    ssMapSeq = new Hashtable<String, BS>();
    if (iState == 0)
      processMolCryst(listAt(pymolObject, 10));
    Lst<Bond> bonds = getBondList(listAt(pymolObject, 6));

    
    Lst<Object> pymolAtoms = listAt(pymolObject, 7);
    atomMap = new int[nAtoms];
    BS bsAtoms = pymolScene.setAtomMap(atomMap, ac0);
    for (int i = 0; i < PyMOL.REP_JMOL_MAX; i++)
      reps[i] = BS.newN(1000);

    // TODO: Implement trajectory business here.


    Lst<Object> coords = listAt(state, 2);
    Lst<Object> labelPositions = listAt(state, 8);
    if (iState == 0 || !isTrajectory)
      for (int idx = 0; idx < n; idx++) {
        P3 a = addAtom(pymolAtoms, intAt(idxToAtm, idx), idx, coords, 
            labelPositions, bsAtoms, iState);
        if (a != null)
          trajectoryStep[trajectoryPtr++] = a;
      }
    addBonds(bonds);
    addMolStructures();
    atoms = asc.atoms;
    if (!isStateScript)
      createShapeObjects();
    ssMapSeq = null;

    Logger.info("reading " + (ac - ac0) + " atoms");
    Logger.info("----------");
    return bsAtoms;
  }

  /**
   * Pick up the crystal data.
   * 
   * @param cryst
   */
  private void processMolCryst(Lst<Object> cryst) {
    if (cryst == null || cryst.size() == 0)
      return;
    Lst<Object> l = listAt(listAt(cryst, 0), 0);
    Lst<Object> a = listAt(listAt(cryst, 0), 1);
    setUnitCell(floatAt(l, 0), floatAt(l, 1), floatAt(l, 2), floatAt(a, 0),
        floatAt(a, 1), floatAt(a, 2));
    setSpaceGroupName(stringAt(cryst, 1));
  }

  /**
   * Create the bond set.
   * 
   * @param bonds
   * @return list of bonds
   */
  private Lst<Bond> getBondList(Lst<Object> bonds) {
    Lst<Bond> bondList = new Lst<Bond>();
    int asSingle = (pymolScene.booleanSetting(PyMOL.valence) ? 0 : JmolAdapter.ORDER_PYMOL_SINGLE);
    int n = bonds.size();
    for (int i = 0; i < n; i++) {
      Lst<Object> b = listAt(bonds, i);
      int order = intAt(b, 2);
      if (order < 1 || order > 3)
        order = 1;
      int ia = intAt(b, 0);
      int ib = intAt(b, 1);
      if (order > 1 && asSingle == 0)
        order |= JmolAdapter.ORDER_PYMOL_MULT;
      else
        order |= asSingle;
      Bond bond = new Bond(ia, ib, order);
      bond.uniqueID = (b.size() > 6 && intAt(b, 6) != 0 ? intAt(b, 5) : -1);
      bondList.addLast(bond);
    }
    return bondList;
  }

  // [0] Int        resv
  // [1] String     chain
  // [2] String     alt
  // [3] String     resi
  // [4] String     segi
  // [5] String     resn
  // [6] String     name
  // [7] String     elem
  // [8] String     textType
  // [9] String     label
  // [10] String    ssType
  // [11] Int       hydrogen
  // [12] Int       customType
  // [13] Int       priority
  // [14] Float     b-factor
  // [15] Float     occupancy
  // [16] Float     vdw
  // [17] Float     partialCharge
  // [18] Int       formalCharge
  // [19] Int       hetatm
  // [20] List      reps
  // [21] Int       color pointer
  // [22] Int       id
  // [23] Int       cartoon type
  // [24] Int       flags
  // [25] Int       bonded
  // [26] Int       chemFlag
  // [27] Int       geom
  // [28] Int       valence
  // [29] Int       masked
  // [30] Int       protekted
  // [31] Int       protons
  // [32] Int       unique_id
  // [33] Int       stereo
  // [34] Int       discrete_state
  // [35] Float     elec_radius
  // [36] Int       rank
  // [37] Int       hb_donor
  // [38] Int       hb_acceptor
  // [39] Int       atomic_color
  // [40] Int       has_setting
  // [41] Float     U11
  // [42] Float     U22
  // [43] Float     U33
  // [44] Float     U12
  // [45] Float     U13
  // [46] Float     U23

  /**
   * @param pymolAtoms
   *        list of atom details
   * @param apt
   *        array pointer into pymolAtoms
   * @param icoord
   *        array pointer into coords (/3)
   * @param coords
   *        coordinates array
   * @param labelPositions
   * @param bsState
   *        this state -- Jmol atomIndex
   * @param iState
   * @return true if successful
   * 
   */
  private P3 addAtom(Lst<Object> pymolAtoms, int apt, int icoord,
                     Lst<Object> coords, Lst<Object> labelPositions,
                     BS bsState, int iState) {
    atomMap[apt] = -1;
    Lst<Object> a = listAt(pymolAtoms, apt);
    int seqNo = intAt(a, 0); // may be negative
    String chainID = stringAt(a, 1); // may be more than one char.
    String altLoc = stringAt(a, 2);
    String insCode = " "; //?    
    String name = stringAt(a, 6);
    String group3 = stringAt(a, 5);
    if (group3.length() > 3)
      group3 = group3.substring(0, 3);
    if (group3.equals(" "))
      group3 = "UNK";
    String sym = stringAt(a, 7);
    if (sym.equals("A"))
      sym = "C";
    boolean isHetero = (intAt(a, 19) != 0);
    int ichain = vwr.getChainID(chainID);
    Atom atom = processAtom(new Atom(), name, altLoc.charAt(0), group3, ichain, seqNo, insCode.charAt(0), isHetero, sym);
    if (!filterPDBAtom(atom, fileAtomIndex++))
      return null;
    icoord *= 3;
    float x = floatAt(coords, icoord);
    float y = floatAt(coords, ++icoord);
    float z = floatAt(coords, ++icoord);
    BoxInfo.addPointXYZ(x, y, z, xyzMin, xyzMax, 0);
    if (isTrajectory && iState > 0)
      return null;
    boolean isNucleic = (nucleic.indexOf(group3) >= 0);
    if (bsState != null)
      bsState.set(ac);

    String label = stringAt(a, 9);
    String ssType = stringAt(a, 10);
    if (seqNo >= MIN_RESNO
        && (!ssType.equals(" ") || name.equals("CA") || isNucleic)) {
      BS bs = ssMapSeq.get(ssType);
      if (bs == null)
        ssMapSeq.put(ssType, bs = new BS());
      bs.set(seqNo - MIN_RESNO);
      ssType += ichain;
      bs = ssMapSeq.get(ssType);
      if (bs == null)
        ssMapSeq.put(ssType, bs = new BS());
      bs.set(seqNo - MIN_RESNO);
    }
    atom.bfactor = floatAt(a, 14);
    atom.foccupancy = floatAt(a, 15);
    atom.radius = floatAt(a, 16);
    if (atom.radius == 0)
      atom.radius = 1;
    atom.partialCharge = floatAt(a, 17);
    int formalCharge = intAt(a, 18);

    BS bsReps = getBsReps(listAt(a, 20));
    int atomColor = intAt(a, 21);
    int serNo = intAt(a, 22);
    int cartoonType = intAt(a, 23);
    int flags = intAt(a, 24);
    boolean bonded = (intAt(a, 25) != 0);
    
    // repurposing vib; leaving Z = Float.NaN to disable actual vibrations
    
    int uniqueID = (a.size() > 40 && intAt(a, 40) == 1 ? intAt(a, 32) : -1);
    atom.vib = V3.new3(uniqueID, cartoonType, Float.NaN);
    if (a.size() > 46) {
      float[] data = PyMOLScene.floatsAt(a, 41, new float[8], 6);
      asc.setAnisoBorU(atom, data, 12);
    }
    //if (uniqueID > 0)
      //pymolScene.setUnique(uniqueID, atom);
    pymolScene.setAtomColor(atomColor);
    processAtom2(atom, serNo, x, y, z, formalCharge);

    // set pymolScene bit sets and create labels

    if (!bonded)
      pymolScene.bsNonbonded.set(ac);
    if (!label.equals(" ")) {
      pymolScene.bsLabeled.set(ac);
      Lst<Object> labelOffset = listAt(labelPositions, apt);
      pymolScene.addLabel(ac, uniqueID, atomColor, labelOffset, label);
    }
    if (isHidden)
      pymolScene.bsHidden.set(ac);
    if (isNucleic)
      pymolScene.bsNucleic.set(ac);
    for (int i = 0; i < PyMOL.REP_MAX; i++)
      if (bsReps.get(i))
        reps[i].set(ac);
    if (atom.elementSymbol.equals("H"))
      pymolScene.bsHydrogen.set(ac);
    if ((flags & PyMOL.FLAG_NOSURFACE) != 0)
      pymolScene.bsNoSurface.set(ac);
    atomMap[apt] = ac++;
    return null;
  }

  private void addBonds(Lst<Bond> bonds) {
    int n = bonds.size();
    for (int i = 0; i < n; i++) {
      Bond bond = bonds.get(i);
      bond.atomIndex1 = atomMap[bond.atomIndex1];
      bond.atomIndex2 = atomMap[bond.atomIndex2];
      if (bond.atomIndex1 < 0 || bond.atomIndex2 < 0)
        continue;
      pymolScene.setUniqueBond(bondCount++, bond.uniqueID);
      asc.addBond(bond);
    }
  }

  private void addMolStructures() {
    addMolSS("H", STR.HELIX);
    addMolSS("S", STR.SHEET);
    addMolSS("L", STR.TURN);
    addMolSS(" ", STR.NONE);
  }

  /**
   * Secondary structure definition.
   * 
   * @param ssType
   * @param type
   */
  private void addMolSS(String ssType, STR type) {
    if (ssMapSeq.get(ssType) == null)
      return;
    int istart = -1;
    int iend = -1;
    int ichain = 0;
    Atom[] atoms = asc.atoms;
    BS bsSeq = null;
    BS bsAtom = pymolScene.getSSMapAtom(ssType);
    int n = ac + 1;
    int seqNo = -1;
    int thischain = 0;
    int imodel = -1;
    int thisModel = -1;
    for (int i = ac0; i < n; i++) {
      if (i == ac) {
        thischain = 0;
      } else {
        seqNo = atoms[i].sequenceNumber;
        thischain = atoms[i].chainID;
        thisModel = atoms[i].atomSetIndex;
      }
      if (thischain != ichain || thisModel != imodel) {
        ichain = thischain;
        imodel = thisModel;
        bsSeq = ssMapSeq.get(ssType + thischain);
        --i; // replay this one
        if (istart < 0)
          continue;
      } else if (bsSeq != null && seqNo >= MIN_RESNO
          && bsSeq.get(seqNo - MIN_RESNO)) {
        iend = i;
        if (istart < 0)
          istart = i;
        continue;
      } else if (istart < 0) {
        continue;
      }
      if (type != STR.NONE) {
        int pt = bsStructureDefined.nextSetBit(istart);
        if (pt >= 0 && pt <= iend)
          continue;
        bsStructureDefined.setBits(istart, iend + 1);
        Structure structure = new Structure(imodel, type, type,
            type.toString(), ++structureCount, type == STR.SHEET ? 1
                : 0);
        Atom a = atoms[istart];
        Atom b = atoms[iend];
        int i0 = asc.getAtomSetAtomIndex(thisModel);
	        //System.out.println("addstruc " + i0 + " " + istart + " " + iend + " " + a.atomName + " " + b.atomName + " " + a.atomSerial + " " + b.atomSerial);
        structure.set(a.chainID, a.sequenceNumber, a.insertionCode, b.chainID,
            b.sequenceNumber, b.insertionCode, istart - i0, iend - i0);
        asc.addStructure(structure);
      }
      bsAtom.setBits(istart, iend + 1);
      istart = -1;
    }
  }

  /**
   * Create JmolObjects for all the molecular shapes; not executed for a state
   * script.
   * 
   */
  private void createShapeObjects() {
    pymolScene.createShapeObjects(reps, allowSurface && !isHidden, ac0, ac);
  }

  ////// end of molecule-specific JmolObjects //////

  ///// final processing /////

  /**
   * Create mesh or mep JmolObjects. Caching the volumeData, because it will be
   * needed by org.jmol.jvxl.readers.PyMOLMeshReader
   * 
   */
  private void processMeshes() {
    vwr.cachePut(pymolScene.surfaceInfoName, volumeData);
    for (int i = mapObjects.size(); --i >= 0;) {
      Lst<Object> obj = mapObjects.get(i);
      String objName = obj.get(obj.size() - 1).toString();
      boolean isMep = objName.endsWith("_e_pot");
      String mapName;
      int tok;
      if (isMep) {
        // a hack? for electrostatics2.pse
        // _e_chg (surface), _e_map (volume data), _e_pot (gadget)
        tok = T.mep;
        String root = objName.substring(0, objName.length() - 3);
        mapName = root + "map";
        String isosurfaceName = pymolScene.getObjectID(root + "chg");
        if (isosurfaceName == null)
          continue;
        obj.addLast(isosurfaceName);
        pymolScene.mepList += ";" + isosurfaceName + ";";
      } else {
        tok = T.mesh;
        mapName = stringAt(listAt(listAt(obj, 2), 0), 1);
      }
      Lst<Object> surface = volumeData.get(mapName);
      if (surface == null)
        continue;
      obj.addLast(mapName);
      volumeData.put(objName, obj);
      volumeData.put("__pymolSurfaceData__", obj);
      if (!isStateScript)
        pymolScene.addMesh(tok, obj, objName, isMep);
      appendLoadNote("PyMOL object " + objName + " references map " + mapName);
    }
  }


  /**
   * Create a JmolObject that will define atom sets based on PyMOL objects
   * 
   */
  private void processDefinitions() {
    String s = vwr.getAtomDefs(pymolScene.setAtomDefs());
    if (s.length() > 2)
      s = s.substring(0, s.length() - 2);
    appendLoadNote(s);
  }

  /**
   * A PyMOL scene consists of one or more of: view frame visibilities, by
   * object colors, by color reps, by type currently just extracts viewpoint
   * 
   * @param map
   */
  @SuppressWarnings("unchecked")
  private void processSelectionsAndScenes(Map<String, Object> map) {
    if (!pymolScene.needSelections())
      return;
    Map<String, Lst<Object>> htObjNames = PyMOLScene.listToMap(getMapList(
        map, "names"));
    if (haveScenes) {
      Map<String, Object> scenes = (Map<String, Object>) map.get("scene_dict");
      finalizeSceneData();
      Map<String, Lst<Object>> htSecrets = PyMOLScene
          .listToMap(getMapList(map, "selector_secrets"));
      for (int i = 0; i < sceneOrder.size(); i++) {
        String name = stringAt(sceneOrder, i);
        Lst<Object> thisScene = getMapList(scenes, name);
        if (thisScene == null)
          continue;
        pymolScene.buildScene(name, thisScene, htObjNames, htSecrets);
        appendLoadNote("scene: " + name);
      }
    }
    pymolScene.setCarveSets(htObjNames);
  }

  ////////////////// set the rendering ////////////////

  /**
   * Make sure atom uniqueID (vectorX) and cartoonType (vectorY) are made
   * permanent
   */
  private void finalizeSceneData() {
    int[] cartoonTypes = new int[ac];
    int[] uniqueIDs = new int[ac];
    int[] sequenceNumbers = new int[ac];
    boolean[] newChain = new boolean[ac];
    float[] radii = new float[ac];
    int lastAtomChain = Integer.MIN_VALUE;
    int lastAtomSet = Integer.MIN_VALUE;
    for (int i = 0; i < ac; i++) {
      cartoonTypes[i] = getCartoonType(i);
      uniqueIDs[i] = getUniqueID(i);
      sequenceNumbers[i] = getSequenceNumber(i);
      radii[i] = getVDW(i);
      if (lastAtomChain != atoms[i].chainID
          || lastAtomSet != atoms[i].atomSetIndex) {
        newChain[i] = true;
        lastAtomChain = atoms[i].chainID;
        lastAtomSet = atoms[i].atomSetIndex;
      }
    }
    pymolScene.setAtomInfo(uniqueIDs, cartoonTypes, sequenceNumbers,
        newChain, radii);
  }

  // generally useful static methods

  private static int intAt(Lst<Object> list, int i) {
    return ((Number) list.get(i)).intValue();
  }

  private static String stringAt(Lst<Object> list, int i) {
    String s = list.get(i).toString();
    return (s.length() == 0 ? " " : s);
  }

  @SuppressWarnings("unchecked")
  private static Lst<Object> getMapList(Map<String, Object> map, String key) {
    return (Lst<Object>) map.get(key);
  }

  private static BS getBsReps(Lst<Object> list) {
    BS bsReps = new BS();
    int n = Math.min(list.size(), PyMOL.REP_MAX);
    for (int i = 0; i < n; i++) {
      if (intAt(list, i) == 1)
        bsReps.set(i);
    }
    return bsReps;
  }

  private float floatAt(Lst<Object> a, int i) {
    return PyMOLScene.floatAt(a, i);
  }

  private Lst<Object> listAt(Lst<Object> list, int i) {
    return PyMOLScene.listAt(list, i);
  }

  /// PymolAtomReader interface
  
  @Override
  public int getUniqueID(int iAtom) {
    return (int) atoms[iAtom].vib.x;
  }

  @Override
  public int getCartoonType(int iAtom) {
    return (int) atoms[iAtom].vib.y;
  }

  @Override
  public float getVDW(int iAtom) {
    return atoms[iAtom].radius;
  }

  @Override
  public int getSequenceNumber(int iAtom) {
    return atoms[iAtom].sequenceNumber;
  }

  @Override
  public boolean compareAtoms(int iPrev, int i) {
    return atoms[iPrev].chainID != atoms[i].chainID;
  }



}
