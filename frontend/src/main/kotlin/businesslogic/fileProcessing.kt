package businesslogic

import com.google.gson.GsonBuilder
import com.mongodb.client.MongoClients
import com.mongodb.client.gridfs.GridFSBuckets
import com.mongodb.client.gridfs.model.GridFSUploadOptions
import com.mongodb.client.model.Filters.eq
import com.rabbitmq.client.ConnectionFactory
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

private const val numberWorkerQueue = "NumberWorkerMQ"
private const val sortWorkerQueue = "SortWorkerMQ"

data class NumberWorkerMessage(val FileID: String, val FileName: String)
data class SortWorkerMessage(val objectId: String, val chunk: Long)

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
                        val options = GridFSUploadOptions().chunkSizeBytes(255 * 1024)
                        fileId = gridFSBucket.uploadFromStream(part.originalFileName, it, options)
                        val chunks = myDatabase.getCollection("fs.chunks").find(eq("files_id", fileId)).count().toLong()
                        val objectId = fileId.toString()
                        mongoClient.close()
                        val factory = ConnectionFactory()
                        factory.host = "localhost"
                        factory.newConnection().use { connection ->
                            connection.createChannel().use { channel ->
                                val gsonBuilder = GsonBuilder().create()

                                channel.queueDeclare(numberWorkerQueue, false, false, false, null)
                                val numberWorkerMessage = gsonBuilder.toJson(NumberWorkerMessage(
                                    objectId,
                                    part.originalFileName.orEmpty()
                                ))
                                channel.basicPublish("", numberWorkerQueue, null, numberWorkerMessage.toByteArray())
                                println(" [x] Sent '$numberWorkerMessage'")

                                channel.queueDeclare(sortWorkerQueue, false, false, false, null)
                                for (i in 0 until chunks) {
                                    val sortWorkerMessage = gsonBuilder.toJson(SortWorkerMessage(objectId, i))
                                    channel.basicPublish("", sortWorkerQueue, null, sortWorkerMessage.toByteArray())
                                    println(" [x] Sent '$sortWorkerMessage'")
                                }
                            }
                        }
                    }
                }
            }
            part.dispose()
        }
        call.respond(HttpStatusCode.OK, mapOf("value" to fileId?.toHexString()))
    }
}
