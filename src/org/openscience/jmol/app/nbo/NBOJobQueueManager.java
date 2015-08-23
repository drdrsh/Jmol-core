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

import javajs.util.Lst;

import org.jmol.util.Logger;

/**
 * A queue for running NBOJobs sequentially .
 * 
 */
class NBOJobQueueManager {

  private static final int QUEUE_MAX = 10;

  protected Lst<NBOJob> list;

  /*
   * Jobs are executed in order, FIFO. The manager thread will only persist as long as there
   * are more than one job in the queue, then it will delete itself;
   * 
   */

  private Object lock = "NBOQueueManagerLock";

  boolean running;
  boolean busy;
  protected NBOJob thisJob;
  protected NBOQueueThread queueThread;

  NBOJobQueueManager() {
    list = new Lst<NBOJob>();
  }

  void addJob(NBOService nboService, String name, String statusInfo,
              Runnable process) {
    synchronized (lock) {
      if (list.size() > QUEUE_MAX) {
        if (thisJob != null) {
          Logger
              .info("NBOJobQueneManager: max queue reached -- canceling jobs and clearing the queue");
          cancelJob();
          clearQueue();
        }
      }
      if (name.equals("clear")) {
        cancelJob();
      } else {
        System.out.println("adding job " + list.size() + ": " + name + "; "
            + statusInfo);
        list.addLast(new NBOJob(nboService, name, statusInfo, process));
        dumpList();
      }
    }
    if (queueThread == null || !running) {
      queueThread = new NBOQueueThread();
      running = true;
      queueThread.start();
    }
  }

  private void dumpList() {
    try {
      for (int i = 0; i < list.size(); i++) {
        System.out.println("QUEUE " + i + " " + list.get(i).name + " "
            + list.get(i).statusInfo);
      }
    } catch (Exception e) {
      return;
    }
  }

  private void cancelJob() {
    busy = false;
    if (thisJob != null) {
      Logger.info("Canceling job " + thisJob);
      thisJob.service.isWorking = false;
      thisJob.service.jobCanceled = true;
    }
  }

  void clearQueue() {
    list.clear();
  }

  class NBOQueueThread extends Thread {
    @Override
    public void run() {
      try {
        while (running && list.size() > 0) {
          while (busy)
            Thread.sleep(20);
          if (!running || interrupted())
            break;
          busy = true;
          thisJob = list.remove(0);
          thisJob.service.jobCanceled = false;
          thisJob.run();
          busy = false;
        }
      } catch (InterruptedException e) {
      }
      running = false;
      list.clear();
      queueThread = null; // remove thyself
    }
  }

}
