package omar.lite.wms

import geoscript.feature.Feature
import geoscript.feature.Field
import geoscript.filter.Filter
import geoscript.geom.Bounds
import geoscript.layer.Layer
import geoscript.proj.Projection
import geoscript.workspace.PostGIS

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

import io.micronaut.context.annotation.Value
import io.micronaut.http.MediaType
import io.micronaut.http.server.types.files.StreamedFile
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import io.micronaut.scheduling.annotation.Async

import joms.oms.Init
import joms.oms.NativeChipper
import org.geotools.referencing.util.CRSUtilities
import org.geotools.util.factory.Hints
import org.ossim.oms.util.TransparentFilter

import org.apache.commons.io.output.ByteArrayOutputStream as FastByteArrayOutputStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.imageio.ImageIO
import javax.inject.Singleton
import javax.media.jai.PlanarImage
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import java.awt.color.ColorSpace
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.ComponentColorModel
import java.awt.image.DataBuffer
import java.awt.image.DataBufferByte
import java.awt.image.WritableRaster
import java.util.zip.GZIPOutputStream

@CompileStatic
@Singleton
class WebMappingService {
  Logger log = LoggerFactory.getLogger( WebMappingService )
  TransparentFilter transparentFilter = new TransparentFilter()

  static final MediaType gzipMediaType = new MediaType( "application/gzip" ) 

  @Value( '${omar.lite.wms.database.name}' )
  String database

  @Value( '${omar.lite.wms.database.username}' )
  String username

  @Value( '${omar.lite.wms.database.password}' )
  String password

  @Value( '${omar.lite.wms.database.host}' )
  String host

  @Value( '${omar.lite.wms.database.port}' )
  String port

  PostGIS workspace
  NativeChipper chipper = new NativeChipper()

  WmsStyleConfigurationProperties styleConfigurationProperties

  WebMappingService( WmsStyleConfigurationProperties styleConfigurationProperties ) {
    this.styleConfigurationProperties = styleConfigurationProperties
  }

  StreamedFile getMap( GetMapRequest request, boolean compress=false ) {
    OutputStream ostream = new FastByteArrayOutputStream()
    long chipTime, queryTime, renderTime    

    Map<String, String> opts = [
        operation: 'ortho',
        cut_height: request?.height?.toString(),
        cut_width: request?.width?.toString(),
        cut_wms_bbox: request?.bbox,
        srs: request?.srs,
        output_radiometry: 'U8',
    ]

    long queryStart = System.currentTimeMillis()
    int count = 0
    boolean contained = true

    Layer layer = workspace[ request?.layers?.split( ':' )?.last() ]
    Field geom = layer?.schema?.geom
    List<Double> coords = request?.bbox?.split( ',' )?.collect { it.toDouble() }
    Projection proj = new Projection( request?.srs )

    if ( request?.version == '1.3.0'
        && CRSUtilities.getUnit( proj?.crs?.coordinateSystem )?.toString() == '\u00b0' ) {
      coords = [ coords[ 1 ], coords[ 0 ], coords[ 3 ], coords[ 2 ] ]
    }

    Bounds bbox = new Bounds( coords[ 0 ], coords[ 1 ], coords[ 2 ], coords[ 3 ], proj )?.reproject( geom?.proj )
    Filter filter = Filter.intersects( geom?.name, bbox?.polygon )

    log.info "bbox=${bbox}"

    if ( request?.filter ) {
      filter = filter?.and( request?.filter )
    }

    Map<String, String> overrides = [ : ]

    layer?.eachFeature(
        fields: [ 'filename', 'entry_id', 'mission_id', 'ground_geom' ],
        filter: filter
    ) { Feature f ->
      // Check for style overrides based on filter.   Stop a first hit
      for ( def styleMap in styleConfigurationProperties?.styles ) {
        if ( new Filter( styleMap.value[ 'filter' ] as String ).evaluate( f ) ) {
          log.info "Using override ${styleMap.key}"
          overrides = styleMap.value[ 'params' ] as Map<String, String>
          break
        }
      }

      opts[ "image${ count }.file".toString() ] = f[ 'filename' ]?.toString()
      opts[ "image${ count }.entry".toString() ] = f[ 'entry_id' ]?.toString()
      ++count

      contained &= f?.geom?.covers( bbox?.geometry )
    }

    log.info "count=${count}, contained=${contained}"

    String styles = request?.styles?.trim()
    Map<String,String> userStyles = parseStyles( styles, overrides )

    // For Kevin,  always use what's in application.yml
    userStyles.removeAll { opts.containsKey( it.key ) }
    opts += userStyles

    long queryStop = System.currentTimeMillis()

    queryTime = queryStop - queryStart

    MediaType mediaType
    String outputFormat

    switch ( request?.format?.toLowerCase() ) {
    case 'image/jpeg':
      mediaType = MediaType.IMAGE_JPEG_TYPE
      outputFormat = 'jpg'
      break
    case 'image/png':
      mediaType = MediaType.IMAGE_PNG_TYPE
      outputFormat = 'png'
      break
    case 'image/vnd.jpeg-png':
      if ( contained ) {
        mediaType = MediaType.IMAGE_JPEG_TYPE
        outputFormat = 'jpg'
      } else {
        mediaType = MediaType.IMAGE_PNG_TYPE
        outputFormat = 'png'
      }
      break
    }

    if ( count ) {
      long chipStart = System.currentTimeMillis()
      WritableRaster raster = chipper?.run( opts )
      long chipStop = System.currentTimeMillis()

      chipTime = chipStop - chipStart

      long renderStart = System.currentTimeMillis()
      ColorModel colorModel = PlanarImage.createColorModel( raster?.sampleModel )
      BufferedImage image = new BufferedImage( colorModel, raster, colorModel?.isAlphaPremultiplied(), [ : ] as Hashtable )

      log.info colorModel as String
      log.info image as String

      if ( false ) {
        Graphics2D g2d = image.createGraphics()
        g2d.color = Color.red
        g2d.drawRect( 0, 0, 255, 255 )
        g2d.dispose()
      }

      if ( outputFormat == 'png' && request.transparent ) {
          if ( image.sampleModel.numBands == 1 ) {
            image = createTransparentGrayScale(image)
          } else {
            BufferedImage image1 = new BufferedImage( image.width, image.height, BufferedImage.TYPE_INT_ARGB )
            Graphics2D graphics2D = image1.createGraphics()

            graphics2D.drawRenderedImage( image, new AffineTransform() )
            graphics2D.dispose()
            image = TransparentFilter.fixTransparency( transparentFilter, image )
          }
      }

      ImageIO.write( image, outputFormat, new BufferedOutputStream( ostream ) )

      long renderStop = System.currentTimeMillis()

      renderTime = renderStop - renderStart
    } else {
      BufferedImage image = new BufferedImage( request?.width, request?.height, BufferedImage.TYPE_INT_ARGB )

      mediaType = MediaType.IMAGE_PNG_TYPE
      outputFormat = 'png'

      ImageIO.write( image, outputFormat,  new BufferedOutputStream( ostream ) )

      log.info( "Returning blank tile: ${ opts }" )
    }

    log.info """${ [
        query: queryTime,
        chip: chipTime,
        render: renderTime,
        contained: contained,
        outputFormat: outputFormat,
        request: request
    ] }"""

    new StreamedFile(  new BufferedInputStream( 
      ( compress ) ? createZipStream( ostream?.toInputStream() ) : ostream?.toInputStream() 
    ), mediaType )
  }

  private Map<String, String> parseStyles( String styles, Map<String, String> overrides ) {
    Map<String, String> opts = [
        bands: 'default',
        hist_op: 'auto-minmax'
    ]

    if ( styles ) {
      Map<String, Object> json = ( new JsonSlurper() ).parseText( styles ) as Map<String, Object>

      json?.each { String k, Object v ->
        switch ( k ) {
        case 'histCenterClip':
          opts[ 'hist_center_clip' ] = v?.toString()
          break
        case 'histCenterTile':
          opts[ 'hist_center' ] = v?.toString()
          break
        case 'histLinearNormClip':
          opts[ 'hist_linear_norm_clip' ] = v?.toString()
          break
        case 'histOp':
          opts[ 'hist_op' ] = v?.toString()
          break
        case 'nullPixelFlip':
          opts[ 'null_pixel_flip' ] = v?.toString()
          break

        default:
          opts[ k ] = v?.toString()
        }
      }
    }

    overrides?.each { k, v ->
      opts[ k.replace( '-', '_' ) ] = v
    }

    return opts
  }

  @EventListener
  @Async
  void onStartup( ServerStartupEvent event ) {
    log.info "OMS startup"
    Init.instance().initialize()

    Map<String, String> dbParams = [ database: database, username: username, password: password, host: host, port: port ]

    workspace = new PostGIS( database,
        user: username,
        password: password,
        host: host,
        port: port?.toInteger()
    )

    Hints.putSystemDefault(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE)

    ImageIO.useCache = false
    log.info dbParams?.toString()
    log.info "styles: ${ styleConfigurationProperties }"
  }

  InputStream createZipStream(InputStream istream) {
    FastByteArrayOutputStream ostream = new FastByteArrayOutputStream()
    GZIPOutputStream zipStream = new GZIPOutputStream(ostream)

    zipStream << istream
    zipStream.flush()
    zipStream.close()
    ostream?.toInputStream()
  }

  BufferedImage createTransparentGrayScale(BufferedImage image) {
    byte[] gray = image.data.getDataElements( 0, 0, image.width, image.height, null) as byte[]
    byte[] alpha = gray.collect { (it) == 0 ? 0x00 : 0xFF }

    def colorModel = new ComponentColorModel(
        ColorSpace.getInstance(ColorSpace.CS_GRAY),
        true, false, ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE
    )

    int[] bandOffsets = new int[] {1, 0}; // gray + alpha
    int bands = bandOffsets.length; // 2, that is

    def buffer = new DataBufferByte(image.width * image.height * bands);

    WritableRaster raster = WritableRaster.createInterleavedRaster(
        buffer, image.width, image.height, image.width * bands, bands, bandOffsets, new Point(0, 0))

    for ( def y in (0..<image.height) ) {     
        for ( def x in (0..<image.width) ) {
            def i = y * image.width + x
            raster.setPixel(x, y, [gray[i], alpha[i]] as int[])
        }        
    }
    
    def newImage = new BufferedImage(colorModel, raster, false, null)

    return newImage    
  }
}
