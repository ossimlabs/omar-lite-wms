package omar.lite.wms

import groovy.transform.CompileStatic
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.server.types.files.StreamedFile
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.reactivex.Single

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@CompileStatic
@Controller( "/wms" )
class WmsController {
  Logger log = LoggerFactory.getLogger( WebMappingService )

  WebMappingService webMappingService

  WmsController( WebMappingService webMappingService ) {
    this.webMappingService = webMappingService
  }

  @ExecuteOn( TaskExecutors.IO )
  @Get( uri = "/", produces = [ MediaType.IMAGE_JPEG, MediaType.IMAGE_GIF, MediaType.IMAGE_PNG ] )
  HttpResponse<StreamedFile> index( HttpRequest request ) {
    HttpResponse<StreamedFile>  httpResponse
    boolean compress = request.headers.getAll(HttpHeaders.ACCEPT_ENCODING).any { it.contains("gzip")}

    //compress = false
    log.info "compress: ${compress}"

    //StreamedFile index( HttpRequest request ) {
    GetMapRequest getMapRequest = new GetMapRequest( request.parameters )
    StreamedFile getMapResponse = webMappingService.getMap( getMapRequest, compress )

    httpResponse = HttpResponse.ok().body( getMapResponse )
    
    if ( compress ) {
      httpResponse = httpResponse.header('Content-Encoding', 'gzip')
    }
  
    httpResponse
  }

  @ExecuteOn( TaskExecutors.IO )
  @Get( uri = "/getMap", produces = [ MediaType.IMAGE_JPEG, MediaType.IMAGE_GIF, MediaType.IMAGE_PNG ] )
  Single<StreamedFile> getMap(
      @QueryValue(defaultValue = 'WMS') String service,
      @QueryValue(defaultValue = '1.1.1') String version,
      @QueryValue(defaultValue = 'GetMap') String request,
      @QueryValue() String layers,
      @QueryValue() String styles,
      @QueryValue() String bbox,
      @QueryValue() String srs,
      @QueryValue() Integer width,
      @QueryValue() Integer height,
      @QueryValue() String format,
      @QueryValue() Boolean transparent,
      @QueryValue() String filter
  ) {
    GetMapRequest getMapRequest = new GetMapRequest(
        service: service,
        version: version,
        request: request,
        layers: layers,
        styles: styles,
        bbox: bbox,
        srs: srs,
        width: width,
        height: height,
        format: format,
        transparent: transparent,
        filter: filter
    )

    StreamedFile getMapResponse = webMappingService.getMap( getMapRequest )

    Single.just( getMapResponse )
  }
}
