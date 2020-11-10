package omar.lite.wms

import groovy.transform.CompileStatic
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.server.types.files.StreamedFile
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.reactivex.Single

@CompileStatic
@Controller( "/wms" )
class WmsController {
  WebMappingService webMappingService

  WmsController( WebMappingService webMappingService ) {
    this.webMappingService = webMappingService
  }

  @ExecuteOn( TaskExecutors.IO )
  @Get( uri = "/", produces = [ MediaType.IMAGE_JPEG, MediaType.IMAGE_GIF, MediaType.IMAGE_PNG ] )
  Single<StreamedFile> index( HttpRequest request ) {
    //StreamedFile index( HttpRequest request ) {
    GetMapRequest getMapRequest = new GetMapRequest( request.parameters )
    StreamedFile getMapResponse = webMappingService.getMap( getMapRequest )

    Single.just( getMapResponse )
    //getMapResponse
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
