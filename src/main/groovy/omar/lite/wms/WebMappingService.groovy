package omar.lite.wms

import geoscript.feature.Feature
import geoscript.feature.Field
import geoscript.filter.Filter
import geoscript.geom.Bounds
import geoscript.layer.Layer
import geoscript.workspace.PostGIS
import geoscript.workspace.Workspace
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Value
import io.micronaut.http.MediaType
import io.micronaut.http.server.types.files.StreamedFile
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import io.micronaut.scheduling.annotation.Async
import joms.oms.Chipper
import joms.oms.ImageUtil
import joms.oms.Init
import joms.oms.ossimImageDataRefPtr
import joms.oms.ossimInterleaveType
import org.apache.commons.io.output.ByteArrayOutputStream as FastByteArrayOutputStream
import org.ossim.oms.util.TransparentFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.imageio.ImageIO
import javax.inject.Singleton
import java.awt.Color
import java.awt.Graphics2D
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.ComponentColorModel
import java.awt.image.DataBuffer
import java.awt.image.PixelInterleavedSampleModel
import java.awt.image.Raster
import java.awt.image.SampleModel

@CompileStatic
@Singleton
class WebMappingService {
  Logger log = LoggerFactory.getLogger( WebMappingService )
  TransparentFilter transparentFilter = new TransparentFilter()

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

  StreamedFile getMap( GetMapRequest request ) {
    OutputStream ostream = new FastByteArrayOutputStream()
    long chipTime, queryTime, renderTime

    Map<String, String> opts = [
        operation        : 'ortho',
        cut_height       : request?.height?.toString(),
        cut_width        : request?.width?.toString(),
        cut_wms_bbox     : request?.bbox,
        srs              : request?.srs,
        output_radiometry: 'U8'
    ]

    String styles = request?.styles?.trim()

    if ( styles ) {
      Map<String, Object> json = ( new JsonSlurper() ).parseText( styles ) as Map<String, Object>

      json?.each { String k, Object v ->
        switch ( k ) {
        case 'histCenterClip':
          opts[ 'hist_center_clip' ] = v?.toString()
          break
        case 'histCenterTile':
          opts[ 'hist_center_tile' ] = v?.toString()
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
    } else {
      opts[ 'bands' ] = 'default'
      opts[ 'hist_op' ] = 'auto-minmax'
    }

    long queryStart = System.currentTimeMillis()

//    Workspace omardb = new PostGIS( database,
//        user: username,
//        password: password,
//        host: host,
//        port: port?.toInteger()
//    )

    int count = 0
    boolean contained = true

//    Workspace.withWorkspace( omardb ) { Workspace workspace ->
    Layer layer = workspace[ request?.layers?.split( ':' )?.last() ]
    Field geom = layer?.schema?.geom
    List<Double> coords = request?.bbox?.split( ',' )?.collect { it.toDouble() }
    Bounds bbox = new Bounds( coords[ 0 ], coords[ 1 ], coords[ 2 ], coords[ 3 ], request?.srs )?.reproject( geom?.proj )
    Filter filter = Filter.intersects( geom?.name, bbox?.polygon )

    if ( request?.filter ) {
      filter = filter?.and( request?.filter )
    }

    layer?.eachFeature(
        fields: [ 'filename', 'entry_id', 'ground_geom' ],
        filter: filter
    ) { Feature f ->
      opts[ "image${ count }.file".toString() ] = f[ 'filename' ]?.toString()
      opts[ "image${ count }.entry".toString() ] = f[ 'entry_id' ]?.toString()
      ++count

      contained &= f?.geom?.covers( bbox?.geometry )
//        null
    }
//    }

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
      Chipper chipper = new Chipper()

      //log.info opts?.toString()

      if ( chipper.initialize( opts ) ) {
        //println  'initialized'
        ossimImageDataRefPtr chip = chipper.getChip( opts )

        if ( chip && chip.valid() ) {
          long renderStart = System.currentTimeMillis()
          DataBuffer dataBuffer = new ImageDataBuffer( chip, ossimInterleaveType.OSSIM_BIP )?.dataBuffer
          int width = chip?.width?.toInteger()
          int height = chip?.height?.toInteger()
          int pixelStride = chip?.numberOfBands.toInteger()
          int scanlineStride = pixelStride * width

          SampleModel sampleModel = new PixelInterleavedSampleModel( dataBuffer.dataType,
              width, height, pixelStride, scanlineStride,
              ( 0..<chip.numberOfBands ) as int[] )

          Raster raster = Raster.createWritableRaster( sampleModel, dataBuffer, null )
          ColorSpace colorSpace = ColorSpace.getInstance( ( raster?.getNumBands() == 1 ) ? ColorSpace.CS_GRAY : ColorSpace.CS_sRGB )
          ColorModel colorModel = new ComponentColorModel( colorSpace, false, false, ComponentColorModel.OPAQUE, dataBuffer.dataType )
          BufferedImage image = new BufferedImage( colorModel, raster, colorModel.isAlphaPremultiplied(), null )

          if ( false ) {
            Graphics2D g2d = image.createGraphics()
            g2d.color = Color.red
            g2d.drawRect( 0, 0, 255, 255 )
            g2d.dispose()
          }

          if ( outputFormat == 'png' && request.transparent ) {
//            log.info "transparency fix"
            image = TransparentFilter.fixTransparency( transparentFilter, image )
          }

          ImageIO.write( image, outputFormat, new BufferedOutputStream( ostream ) )

          long renderStop = System.currentTimeMillis()

          renderTime = renderStop - renderStart
          chip?.delete()
          chip = null
        } else {
          log.error 'Error: invalid chip'
        }
      } else {
        log.error "Error: initializing: ${ opts }"
      }

      chipper.delete()
      chipper = null

      long chipStop = System.currentTimeMillis()

      chipTime = chipStop - chipStart
    } else {
      BufferedImage image = new BufferedImage( request?.width, request?.height, BufferedImage.TYPE_INT_ARGB )

      mediaType = MediaType.IMAGE_PNG_TYPE
      outputFormat = 'png'
      ImageIO.write( image, outputFormat, new BufferedOutputStream( ostream ) )
      log.info( "Error: returning blank tile: ${ opts }" )
    }

    log.info "${ [ query: queryTime, chip: chipTime, render: renderTime, contained: contained, outputFormat: outputFormat /*, request: request */ ] }"

    new StreamedFile( new BufferedInputStream( ostream?.toInputStream() ), mediaType )
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


    log.info dbParams?.toString()
  }
}