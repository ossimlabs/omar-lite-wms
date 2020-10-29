package omar.lite.wms

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.HttpResponse
import io.micronaut.http.server.HttpServerConfiguration

@Controller( "/" )
class HomeController {
    HttpServerConfiguration httpServerConfiguration

    HomeController( HttpServerConfiguration httpServerConfiguration ) {
        this.httpServerConfiguration = httpServerConfiguration
    }

    @Get( "/" )
    HttpResponse index() {
        return HttpResponse.redirect( "${ httpServerConfiguration.contextPath ?: '' }/swagger-ui".toURI() )
    }
}