package businesslogic

import com.mongodb.client.MongoClients
import com.mongodb.client.gridfs.GridFSBuckets
import com.mongodb.client.gridfs.model.GridFSUploadOptions
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.util.pipeline.PipelineContext
import org.bson.types.ObjectId


fun processUploadedFile(): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit {
    return {
        val multipart = call.receiveMultipart()
        var fileId: ObjectId? = null
        multipart.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                }
                is PartData.FileItem -> {
                    part.streamProvider().use {
                        val mongoClient = MongoClients.create("mongodb://localhost")
                        val myDatabase = mongoClient.getDatabase("TextDocumentsDB")
                        val gridFSBucket = GridFSBuckets.create(myDatabase)
                        println(gridFSBucket.bucketName)
                        val options = GridFSUploadOptions().chunkSizeBytes(262144)
                        for (gridFSFile in gridFSBucket.find()) {
                            println(gridFSFile.filename)
                        }
                        fileId = gridFSBucket.uploadFromStream(part.originalFileName, it, options)
                        mongoClient.close()
                    }

                }
            }
            part.dispose()
        }
        call.respond(HttpStatusCode.OK, mapOf("value" to fileId?.toHexString()))
    }
}
