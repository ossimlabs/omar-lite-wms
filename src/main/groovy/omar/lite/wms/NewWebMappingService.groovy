package omar.lite.wms

import groovy.transform.CompileStatic
import io.micronaut.http.MediaType
import io.micronaut.http.server.types.files.StreamedFile
import org.apache.commons.io.output.ByteArrayOutputStream as FastByteArrayOutputStream

import javax.imageio.ImageIO
import javax.inject.Singleton
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage

@Singleton
@CompileStatic
class NewWebMappingService {
  StreamedFile getMap( GetMapRequest request ) {
    BufferedImage image = new BufferedImage( request.width, request.height, BufferedImage.TYPE_INT_ARGB )

    FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream()
    String outputFormat = 'png'
    Graphics2D g2d = image.createGraphics()

    g2d.color = Color.red
    g2d.drawRect( 0, 0, request.width - 1, request.height - 1 )
    g2d.dispose()

    ImageIO.write( image, outputFormat, new BufferedOutputStream( outputStream ) )

    return new StreamedFile( new BufferedInputStream( outputStream?.toInputStream() ), MediaType.IMAGE_PNG_TYPE )
  }
}