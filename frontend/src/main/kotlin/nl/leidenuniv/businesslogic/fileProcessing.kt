package nl.leidenuniv.businesslogic

import com.google.gson.GsonBuilder
import com.mongodb.client.MongoClients
import com.mongodb.client.gridfs.GridFSBuckets
import com.mongodb.client.gridfs.model.GridFSUploadOptions
import com.mongodb.client.model.Filters.eq
import com.rabbitmq.client.ConnectionFactory
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.receiveMultipart
import io.ktor.response.*
import io.ktor.util.pipeline.PipelineContext
import org.bson.types.ObjectId


private const val numberWorkerQueue = "NumberWorkerMQ"
private const val sortWorkerQueue = "SortWorkerMQ"
//private const val mongoDBHost = "localhost"
//private const val rabbitMQHost = "localhost"
private const val mongoDBHost = "mymongo"
private const val rabbitMQHost = "myrabbit"

private val mongoClient = MongoClients.create("mongodb://$mongoDBHost")
private val myDatabase = mongoClient.getDatabase("TextDocumentsDB")
private val gridFSBucket = GridFSBuckets.create(myDatabase)

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
                        val options = GridFSUploadOptions().chunkSizeBytes(255 * 1024)
                        filename = part.originalFileName.orEmpty()
                        fileId = gridFSBucket.uploadFromStream(filename, it, options)
                        val chunks = myDatabase.getCollection("fs.chunks").find(eq("files_id", fileId)).count().toLong()
                        val objectId = fileId?.toHexString().orEmpty()
                        val factory = ConnectionFactory()
                        factory.host = rabbitMQHost
                        factory.newConnection().use { connection ->
                            connection.createChannel().use { channel ->
                                val gsonBuilder = GsonBuilder().create()

                                channel.queueDeclare(numberWorkerQueue, false, false, false, null)
                                val numberWorkerMessage = gsonBuilder.toJson(
                                    NumberWorkerMessage(
                                        objectId,
                                        filename
                                    )
                                )
                                channel.basicPublish("",
                                    numberWorkerQueue, null, numberWorkerMessage.toByteArray())
                                println(" [x] Sent '$numberWorkerMessage'")

                                channel.queueDeclare(sortWorkerQueue, false, false, false, null)
                                for (i in 0 until chunks) {
                                    val sortWorkerMessage = gsonBuilder.toJson(
                                        SortWorkerMessage(
                                            objectId,
                                            filename,
                                            i
                                        )
                                    )
                                    channel.basicPublish("",
                                        sortWorkerQueue, null, sortWorkerMessage.toByteArray())
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
        val amount = myDatabase.getCollection("numbers").find(eq("file_id", fileId)).first()?.get("amount")
        call.respond(HttpStatusCode.OK, mapOf("value" to amount))
    }
}

fun retrieveSortWorkerResult(): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit {
    return {
        val fileId = call.request.queryParameters["fileId"]
        val rawChunks = myDatabase.getCollection("fs.chunks").find(eq("files_id", ObjectId(fileId))).count().toLong()
        val processedChunks = myDatabase.getCollection("sorted.chunks").find(eq("file_id", fileId)).count().toLong()
        val status = when {
            processedChunks < rawChunks -> "sorting $processedChunks / $rawChunks"
            else -> "merging chunks"
        }
        call.respond(HttpStatusCode.OK, mapOf("value" to status))
    }
}

fun retrieveDownloadStatus(): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit {
    return {
        val fileId = call.request.queryParameters["fileId"]
        val filename = call.request.queryParameters["filename"]
        val available = gridFSBucket.find((eq("filename", "$filename-$fileId"))).first() != null
        call.respond(HttpStatusCode.OK, mapOf("value" to available))
    }
}

fun downloadFile(): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit {
    return {
        val fileId = call.request.queryParameters["fileId"]
        val filename = call.request.queryParameters["filename"]
        val properFilename = "$filename-$fileId"
        val objectId = gridFSBucket.find((eq("filename", properFilename))).first()!!.objectId
        val downloadStream = gridFSBucket.openDownloadStream(objectId)
        val fileLength = downloadStream.gridFSFile.length.toInt()
        val fileData = ByteArray(fileLength)
        downloadStream.read(fileData)
        downloadStream.close()
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(
                ContentDisposition.Parameters.FileName,
                properFilename
            ).toString()
        )
        call.respondBytes(fileData)
    }
}
