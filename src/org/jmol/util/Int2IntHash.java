/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

package org.jmol.util;

public class Int2IntHash {
  int entryCount;
  Entry[] entries;
  

  public Int2IntHash(int initialCapacity) {
    entries = new Entry[initialCapacity];
  }

  public synchronized int get(int key) {
    Entry[] entries = this.entries;
    int hash = (key & 0x7FFFFFFF) % entries.length;
    for (Entry e = entries[hash]; e != null; e = e.next)
      if (e.key == key)
        return e.value;
    return Integer.MIN_VALUE;
  }

  public synchronized void put(int key, int value) {
    Entry[] entries = this.entries;
    int n = entries.length;
    int hash = (key & 0x7FFFFFFF) % n;
    for (Entry e = entries[hash]; e != null; e = e.next)
      if (e.key == key) {
        e.value = value;
        return;
      }
    if (entryCount > n) {
      int oldSize = n;
      n += n + 1;
      Entry[] newEntries = new Entry[n];
      for (int i = oldSize; --i >= 0;) {
        for (Entry e = entries[i]; e != null;) {
          Entry t = e;
          e = e.next;
          hash = (t.key & 0x7FFFFFFF) % n;
          t.next = newEntries[hash];
          newEntries[hash] = t;
        }
      }
      entries = this.entries = newEntries;
      hash = (key & 0x7FFFFFFF) % n;
    }
    entries[hash] = new Entry(key, value, entries[hash]);
    ++entryCount;
  }

  private class Entry {
    protected int key;
    protected int value;
    protected Entry next;
    
    Entry(int key, int value, Entry next) {
      this.key = key;
      this.value = value;
      this.next = next;
    }
  }
}


