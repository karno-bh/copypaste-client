# Design & Notes
## High Level Overview
Overall design points are pretty similar to the server design, i.e. Spring Boot is used as a platform. Although it is
possible to write the code within standard JDK but having a DI container is a benefit. Also, as it is with server,
logger functionality is already preconfigured and Spring Boot generates an uber jar as its output which is a result of
requirements for the task (deploy everywhere). Summarizing the said above, Spring Boot is the platform without its
server functionality.
## Design
* It will be a simple process that will connect to the server, will take the list of available files
* The process will take from the list the newest one and will download it by chunks
* It WILL NOT support re-downloading for the failed file. However, it is not a problem to extend it to behave in such a
way 
* Downloading by chunks will be divided into two threads. First thread will be downloading sequentially
chunks from the server. To be fast, it will be using a keep alive HTTP connections.
* The second thread will be responsible for verifying chunks integrity and appending to the temp file. On failed chunk
the flow will not continue
* Once the transfer is done, the file will be verified against the file checksum if it is ok, it will rename the file to
real file name
* Once the process is started it will create if it does not exist the config file under "config" directory with
appropriate configuration. As well, it will create the incoming directory if does not exist 
