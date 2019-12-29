echo "Running containers..."

docker run --name mymongo -p 27017:27017 -d mongo:latest
docker start mymongo

docker run -d -p 5672:5672 --hostname myrabbit --name myrabbit rabbitmq:3-management
docker start myrabbit

cd numberworker

docker build -t numberworker .
docker run --link mymongo:mymongo --link myrabbit:myrabbit numberworker

cd ..
cd sortworker

docker build -t sortworker .
docker run --link mymongo:mymongo --link myrabbit:myrabbit sortworker

cd ..
cd reduceworker

docker build -t reduceworker .
docker run --link mymongo:mymongo --link myrabbit:myrabbit reduceworker

cd ..
cd frontend

docker build -t frontend .
docker run --link mymongo:mymongo --link myrabbit:myrabbit -p 8080:8080 frontend