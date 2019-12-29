package businesslogic

import com.google.gson.GsonBuilder
import com.mongodb.client.MongoClients
import com.mongodb.client.gridfs.GridFSBuckets
import com.mongodb.client.gridfs.model.GridFSUploadOptions
import com.mongodb.client.model.Filters.eq
import com.rabbitmq.client.ConnectionFactory
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.receiveMultipart
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.util.pipeline.PipelineContext
import kotlinx.html.HEADER
import org.bson.types.ObjectId


private const val numberWorkerQueue = "NumberWorkerMQ"
private const val sortWorkerQueue = "SortWorkerMQ"

data class NumberWorkerMessage(val FileID: String, val FileName: String)
data class SortWorkerMessage(val FileID: String, val FileName: String, val Chunk: Long)

fun processUploadedFile(): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit {
    return {
        val multipart = call.receiveMultipart()
        var fileId: ObjectId? = null
        var filename = ""
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
                        filename = part.originalFileName.orEmpty()
                        fileId = gridFSBucket.uploadFromStream(filename, it, options)
                        val chunks = myDatabase.getCollection("fs.chunks").find(eq("files_id", fileId)).count().toLong()
                        val objectId = fileId?.toHexString().orEmpty()
                        mongoClient.close()
                        val factory = ConnectionFactory()
                        factory.host = "localhost"
                        factory.newConnection().use { connection ->
                            connection.createChannel().use { channel ->
                                val gsonBuilder = GsonBuilder().create()

                                channel.queueDeclare(numberWorkerQueue, false, false, false, null)
                                val numberWorkerMessage = gsonBuilder.toJson(NumberWorkerMessage(
                                    objectId,
                                    filename
                                ))
                                channel.basicPublish("", numberWorkerQueue, null, numberWorkerMessage.toByteArray())
                                println(" [x] Sent '$numberWorkerMessage'")

                                channel.queueDeclare(sortWorkerQueue, false, false, false, null)
                                for (i in 0 until chunks) {
                                    val sortWorkerMessage = gsonBuilder.toJson(SortWorkerMessage(
                                        objectId,
                                        filename,
                                        i
                                    ))
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
        call.respond(HttpStatusCode.OK, mapOf(
            "fileId" to fileId?.toHexString(),
            "filename" to filename
        ))
    }
}

fun retrieveNumberWorkerResult(): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit {
    return {
        val fileId = call.request.queryParameters["fileId"]
        val mongoClient = MongoClients.create("mongodb://localhost")
        val myDatabase = mongoClient.getDatabase("TextDocumentsDB")
        val amount = myDatabase.getCollection("numbers").find(eq("file_id", fileId)).first()?.get("amount")
        mongoClient.close()
        call.respond(HttpStatusCode.OK, mapOf("value" to amount))
    }
}

fun retrieveSortWorkerResult(): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit {
    return {
        val fileId = call.request.queryParameters["fileId"]
        val mongoClient = MongoClients.create("mongodb://localhost")
        val myDatabase = mongoClient.getDatabase("TextDocumentsDB")
        val rawChunks = myDatabase.getCollection("fs.chunks").find(eq("files_id", fileId)).count().toLong()
        val processedChunks = myDatabase.getCollection("sorted.chunks").find(eq("file_id", fileId)).count().toLong()
        mongoClient.close()
        val status = when {
            processedChunks < rawChunks -> "$processedChunks / $rawChunks"
            else -> "merging chunks"
        }
        call.respond(HttpStatusCode.OK, mapOf("value" to status))
    }
}

fun retrieveDownloadStatus(): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit {
    return {
        val fileId = call.request.queryParameters["fileId"]
        val filename = call.request.queryParameters["filename"]
        val mongoClient = MongoClients.create("mongodb://localhost")
        val myDatabase = mongoClient.getDatabase("TextDocumentsDB")
        val gridFSBucket = GridFSBuckets.create(myDatabase)
        val available = gridFSBucket.find((eq("filename", "$filename-$fileId"))).first() != null
        mongoClient.close()
        call.respond(HttpStatusCode.OK, mapOf("value" to available))
    }
}

fun downloadFile(): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit {
    return {
        val fileId = call.request.queryParameters["fileId"]
        val filename = call.request.queryParameters["filename"]
        val mongoClient = MongoClients.create("mongodb://localhost")
        val myDatabase = mongoClient.getDatabase("TextDocumentsDB")
        val gridFSBucket = GridFSBuckets.create(myDatabase)
        val properFilename = "$filename-$fileId"
        val objectId = gridFSBucket.find((eq("filename", properFilename))).first()!!.objectId
        val downloadStream = gridFSBucket.openDownloadStream(objectId)
        val fileLength = downloadStream.gridFSFile.length.toInt()
        val fileData = ByteArray(fileLength)
        downloadStream.read(fileData)
        downloadStream.close()
        mongoClient.close()
        call.respond(HttpStatusCode.OK, fileData)
    }
}
