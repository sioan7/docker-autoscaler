# cco-assignment2

## TODO
- [ ] IOAN -> Front end
- [ ] JAN -> Number worker
- [ ] JAN -> Sort worker
- [ ] JAN -> Reduce worker
- [ ] IOAN -> Containerize each component with Docker
- [ ] IOAN -> Deploy to Kubernetes 

Each component has to be its own application so it is containerizable.

###
Things we agreed on:
- Name of the Database (MongoDB) --> TextDocumentsDB
- Names of the MQs --> NumberWorkerMQ, SortWorkerMQ, ReduceWorkerMQ

### Front end
User perspective
- upload file
- displays current status
- displays result of computation
- download result file

Implementation
- create sort tasks and number tasks
- upload file to GridFS
- determine how many chunks the file consists of (simply query the file object from MongoDB after file upload, it contains the file length and chunk size that was used). Then create a message (task) for each chunk

### General worker
- pull message from own message queue
- remove message from the message queue only when work is completed ? how to ensure two workers are not working on the same message

### Number worker
- retrieve data (chunk) from DB
- process chunk -> output will be the number of numbers
- write output to the DB

### Sort worker
- retrieve data (chunk) from DB
- process chunk -> output will be the sorted array of numbers
- write output to the DB
- **if** last chunk **then** send message to reducer worker

### Reduce worker
- retrieve data (chunk) from DB
- process chunk -> output will be the number of numbers
- write output to the DB

## Commands

Install MongoDB and spinn up a Docker container

```
docker run --name mymongo -p 27017:27017 -d mongo:latest
```

Run the MongoDB client shell from the container

```
docker run -it --link mymongo --rm mongo mongo --host mymongo test
```

Use mongofiles to manipulate files stored in the MongoDB instance in GridFS objects from the command line

```
docker run -it --link mymongo --rm mongo bash
root@dae2ccd08dc3:/# mongofiles --host mymongo list
2019-12-04T21:40:11.041+0000 connected to: mongodb://mymongo/
myfile.txt 938848
root@dae2ccd08dc3:/# mongofiles --host mymongo --help
```

Install RabbitMQ and spin up a Docker container

```
docker run -d -p 5672:5672 --hostname myrabbit --name myrabbit rabbitmq:3
```
