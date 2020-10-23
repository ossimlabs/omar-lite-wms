package omar.lite.wms

import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import groovy.transform.ToString
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpParameters

@ToString( includeNames = true )
@CompileStatic
@Introspected
@MapConstructor
class GetMapRequest {
  String service
  String version
  String request
  String srs
  String bbox
  Integer width
  Integer height
  Boolean transparent
  String format
  String layers
  String styles
  String filter

  GetMapRequest( HttpParameters parameters ) {
    parameters.forEach ( String key, List<String> values ) -> {
      switch ( key?.toUpperCase() ) {
      case 'FILTER':
        filter = values?.first()
        break
      case 'FORMAT':
        format = values?.first()
        break
      case 'STYLES':
        styles = values?.first()
        break
      case 'WIDTH':
        width = values?.first()?.toInteger()
        break
      case 'HEIGHT':
        height = values?.first()?.toInteger()
        break
      case 'LAYERS':
        layers = values?.first()
        break
      case 'REQUEST':
        request = values?.first()
        break
      case 'CRS':
      case 'SRS':
        srs = values?.first()
        break
      case 'BBOX':
        bbox = values?.first()
        break
      case 'VERSION':
        version = values?.first()
        break
      case 'SERVICE':
        service = values?.first()
        break
      case 'TRANSPARENT':
        transparent = values?.first()?.toBoolean()
        break
      }
    }
  }
}
