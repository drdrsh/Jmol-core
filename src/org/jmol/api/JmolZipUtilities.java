package org.jmol.api;

import java.io.BufferedReader;
import java.io.InputStream;

import java.util.Map;

import javajs.api.GenericZipTools;

import org.jmol.io.JmolBinary;
import org.jmol.viewer.Viewer;

public interface JmolZipUtilities {

  public boolean cachePngjFile(JmolBinary jmb, String[] data);

  public String determineSurfaceFileType(BufferedReader br);

  public Object getAtomSetCollectionOrBufferedReaderFromZip(Viewer vwr, JmolAdapter adapter,
                                                            InputStream is,
                                                            String fileName,
                                                            String[] zipDirectory,
                                                            Map<String, Object> htParams,
                                                            int i, boolean asBufferedReader);

  public byte[] getCachedPngjBytes(JmolBinary jmb, String pathName);

  public String[] spartanFileList(GenericZipTools zpt, String name, String zipDirectory);

  public Object getImage(Viewer vwr, Object fullPathNameOrBytes, String echoName);

}
