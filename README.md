# cco-assignment2

Install MongoDB and spinn up a Docker container
> docker run --name mymongo -p 27017:27017 -d mongo:latest

Run the MongoDB client shell from the container
> docker run -it --link mymongo --rm mongo mongo --host mymongo test

Use mongofiles to manipulate files stored in the MongoDB instance in GridFS objects from the command line

> docker run -it --link mymongo --rm mongo bash
> root@dae2ccd08dc3:/# mongofiles --host mymongo list
> 2019-12-04T21:40:11.041+0000 connected to: mongodb://mymongo/
> myfile.txt 938848
> root@dae2ccd08dc3:/# mongofiles --host mymongo --help

Install RabbitMQ and spin up a Docker container
> docker run -d -p 5672:5672 --hostname myrabbit --name myrabbit rabbitmq:3