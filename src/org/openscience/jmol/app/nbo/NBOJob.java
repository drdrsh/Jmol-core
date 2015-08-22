/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-12-13 22:43:17 -0600 (Sat, 13 Dec 2014) $
 * $Revision: 20162 $
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
package org.openscience.jmol.app.nbo;

import org.jmol.util.Logger;

/**
 * A job to be queued by NBOJobQueue. 
 * 
 */

class NBOJob {
  
  NBOService service;

  long startTime;
  Runnable process;
  String name;
  String statusInfo;
  String err;
  
  NBOJob(NBOService service, String name, String statusInfo, Runnable process) {
    this.service = service;
    this.name = name;
    this.statusInfo = statusInfo;
    this.process = process;
  }

  public void run() {
    startTime = System.currentTimeMillis();
    if (service.nboDialog != null)
      service.nboDialog.statusLab.setText(statusInfo);
    Logger.info("NBO job " + name + " started");      
    process.run();
    if (service.nboDialog != null)
      service.nboDialog.statusLab.setText(null);
    Logger.info("NBO job " + name + " ended ms:" + (System.currentTimeMillis() - startTime));      
  }

}
