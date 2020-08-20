package omar.lite.wms

import groovy.transform.CompileStatic
import groovy.transform.Immutable

import java.awt.Rectangle
import java.awt.image.ColorModel
import java.awt.image.Raster
import java.awt.image.RenderedImage
import java.awt.image.SampleModel
import java.awt.image.WritableRaster

//@Immutable
@CompileStatic
class OmsRenderedImage implements RenderedImage {
  Vector<RenderedImage> sources
  String[] propertyNames
  ColorModel colorModel
  SampleModel sampleModel
  int width
  int height
  int minX
  int minY
  int numXTiles
  int numYTiles
  int minTileX
  int minTileY
  int tileWidth
  int tileHeight
  int tileGridXOffset
  int tileGridYOffset

  @Override
  Raster getTile( int tileX, int tileY ) {
    return null
  }

  @Override
  Raster getData() {
    return null
  }

  @Override
  Raster getData( Rectangle rect ) {
    return null
  }

  @Override
  WritableRaster copyData( WritableRaster raster ) {
    return null
  }
}
