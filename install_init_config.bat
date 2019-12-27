echo Start initilization!

docker run --name mymongo -p 27017:27017 -d mongo:latest

docker run -d --name="RabbitMQ" --hostname="RabbitMQ" --publish="4369:4369" --publish="5671:5671" --publish="5672:5672" --publish="15671:15671" --publish="15672:15672" --publish="25672:25672" rabbitmq:3-management

echo End initilization!
PAUSE