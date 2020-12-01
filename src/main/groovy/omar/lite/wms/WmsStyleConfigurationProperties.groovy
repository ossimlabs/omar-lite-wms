package omar.lite.wms

import groovy.transform.ToString
import io.micronaut.context.annotation.ConfigurationProperties

@ToString( includeNames = true )
@ConfigurationProperties( 'omar.lite.wms' )
class WmsStyleConfigurationProperties {
  Map<String,Map<String,Object>> styles
}
