JOB_CLASS_NAME := "com.ververica.troubleshooting.RocksDBMemLeakNthLast"
JOB_JAR_FILE   := "../build/libs/lab-rocksdb-memleak-0.1-SNAPSHOT-all.jar"
JM_CONTAINER    = $(shell docker ps --filter name=jobmanager --format={{.ID}})

all: image run

run:
	chmod ugo+rwx chkpts
	docker-compose up -d

submit:
	docker cp $(JOB_JAR_FILE) "$(JM_CONTAINER)":/job.jar
	docker exec -t -i "$(JM_CONTAINER)" flink run -d -p 2 -c $(JOB_CLASS_NAME) /job.jar

stop:
	docker-compose kill

image: .imageid

.imageid:
	docker build --iidfile .imageid . && sleep 5s
	docker tag $(shell cat .imageid) flink:1.11.1-scala_2.11-dev
