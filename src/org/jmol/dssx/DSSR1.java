/* $RCSfile$
 * $Author: nicove $
 * $Date: 2007-03-25 06:44:28 -0500 (Sun, 25 Mar 2007) $
 * $Revision: 7224 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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
package org.jmol.dssx;

import java.util.Hashtable;
import java.util.Map;

import javajs.api.GenericLineReader;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.Rdr;
import javajs.util.SB;

import org.jmol.java.BS;
import org.jmol.modelset.Bond;
import org.jmol.modelset.HBond;
import org.jmol.modelset.ModelSet;
import org.jmol.modelsetbio.BasePair;
import org.jmol.modelsetbio.BioModel;
import org.jmol.modelsetbio.BioPolymer;
import org.jmol.modelsetbio.NucleicMonomer;
import org.jmol.modelsetbio.NucleicPolymer;
import org.jmol.script.T;
import org.jmol.util.C;
import org.jmol.util.Edge;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

/**
 * 
 * A parser for output from 3DNA web service.
 * 
 * load =1d66/dssr
 * 
 * also other annotations now,
 * 
 * load *1cbs/dom
 * 
 * calls EBI for the mmCIF file and also retrieves the domains mapping JSON
 * report.
 * 
 * 
 * load *1cbs/val
 * 
 * calls EBI for the mmCIF file and also retrieves the validation outliers JSON
 * report.
 * 
 * Bob Hanson July 2014
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */
public class DSSR1 extends AnnotationParser {

  public DSSR1() {
    // for reflection
  }

// TODO
  
}
