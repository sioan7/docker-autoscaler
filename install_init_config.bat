echo Start initilization!

docker run --name mymongo -p 27017:27017 -d mongo:latest

echo Not sure about the ports though...
docker run -d -p 5672:5672 --hostname NumberWorkerMQ --name NumberWorkerMQ rabbitmq:3
docker run -d -p 5673:5673 --hostname SortWorkerMQ --name SortWorkerMQ rabbitmq:3
docker run -d -p 5674:5674 --hostname ReduceWorkerMQ --name ReduceWorkerMQ rabbitmq:3

echo End initilization!
PAUSE