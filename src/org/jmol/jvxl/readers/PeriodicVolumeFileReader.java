package org.jmol.jvxl.readers;

public abstract class PeriodicVolumeFileReader extends VolumeFileReader {

  @Override
  protected void readSurfaceData(boolean isMapData) throws Exception {
    initializeSurfaceData();
    voxelData = new float[nPointsX][nPointsY][nPointsZ];
    readSkip();
    getPeriodicVoxels();

    // add in periodic face data

    int n;
    n = nPointsX - 1;
    for (int i = 0; i < nPointsY; ++i)
      for (int j = 0; j < nPointsZ; ++j)
        voxelData[n][i][j] = voxelData[0][i][j];
    n = nPointsY - 1;
    for (int i = 0; i < nPointsX; ++i)
      for (int j = 0; j < nPointsZ; ++j)
        voxelData[i][n][j] = voxelData[i][0][j];
    n = nPointsZ - 1;
    for (int i = 0; i < nPointsX; ++i)
      for (int j = 0; j < nPointsY; ++j)
        voxelData[i][j][n] = voxelData[i][j][0];

    // for map data, just pick out near points and get rid of voxelData

    if (isMapData && volumeData.hasPlane()) {
      volumeData.setVoxelMap();
      for (int x = 0; x < nPointsX; ++x) {
        for (int y = 0; y < nPointsY; ++y) {
          for (int z = 0; z < nPointsZ; ++z) {
            float f = volumeData.getToPlaneParameter();
            if (volumeData.isNearPlane(x, y, z, f))
              volumeData.setVoxelMapValue(x, y, z, voxelData[x][y][z]);
          }
        }
      }
      voxelData = null;
    }
    volumeData.setVoxelDataAsArray(voxelData);
    
    if (dataMin > params.cutoff)
      params.cutoff = 2 * dataMin;
  }

  protected abstract void getPeriodicVoxels() throws Exception;
  protected abstract void readSkip() throws Exception;

}
