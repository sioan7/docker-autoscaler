import businesslogic.*
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.html.respondHtml
import io.ktor.http.content.*
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import pages.index
import java.io.*

fun main() {
    val server = embeddedServer(
        Netty,
        port = 8080,
        module = Application::primaryModule
    )
    server.start(wait = true)
}

fun Application.primaryModule() {
    install(DefaultHeaders)
    install(ContentNegotiation) {
        gson()
    }
    routing {
        routePages()
        routeStaticResources()
        routeAPI()
    }
}

fun Route.routePages() {
    get("/", body = {
        val s: String? = call.request.queryParameters["fileId"]
        call.respondHtml(block = index)
    })
}

fun Route.routeStaticResources() {
    static("static") {
        staticRootFolder = File("src/main/resources/static")
        files("css")
        files("js")
        file("favicon.ico")
    }
}

fun Route.routeAPI() {
    post("/file", processUploadedFile())
    get("/numbers", retrieveNumberWorkerResult())
    get("/sortStatus", retrieveSortWorkerResult())
    get("/downloadStatus", retrieveDownloadStatus())
    get("/download", downloadFile())
}
