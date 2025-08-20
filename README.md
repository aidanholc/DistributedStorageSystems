## Running the project

<ol>
<li> Install the most recent JDK </li>
<li> Compile the project with `javac *.java` </li>


### Running Server for lab 2

After compiling the project, launch the server with "java ServerRunner".

Once the server is up and running you can run tests or benchmarking with "java ClientRunner" or "java Benchmark".


### Running Project 3

This project uses gradle as a build tool as we have external dependencies for hashing. 

I have the client hardcoded to start reading from port 5660 - so I'd recommend starting from there if you want the client jar to work as well.

to set up a server, run java -jar /GradleProject/app/build/libs/server.jar <portNumber>

to set up a client, run java -jar /GradleProject/app/build/libs/client.jar <NumServers> <ReplicationSize> <ReadConsensus>

Currently the client only reads, to change that you would need to go into MultiClientRunner.java and change line 48 from sendRead to sendWrite, and add the value you are writing. 

To rebuild the project, navigate to the GradleProject directory and run ./gradlew shadowJar clientShadowJar

