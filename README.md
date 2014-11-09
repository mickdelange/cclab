Cloud Computing Lab Exercise
=====

This repository contains the work of Mick de Lange and Maria Voinea for the IN4392 Cloud Computing course of the TU Delft.

## Objective

The project's objective is the scheduling of image processing tasks on virtual machines in the Amazon EC2 cloud.

## Requirements

The project is meant to be compiled with JDK 1.6 and requires a `resources/AwsCredentials.properties` file with the Amazon EC2 keys for running virtual machine instances.

## Structure

Both scheduler and task processing logic can be found under the same project in the `code` folder. It can be compiled with dependencies using maven:

`mvn clean compile assembly:single`

and then run directly as a jar.

For running as a master node:

`java -jar cclab-core-3.0-jar-with-dependencies.jar "node_name" "master" ["port"]`

For running as a worker node:

`java -jar cclab-core-3.0-jar-with-dependencies.jar "node_name" "worker" ["master_ip" ["port"]]`

For running as a backup node:

`java -jar cclab-core-3.0-jar-with-dependencies.jar "node_name" "backup" "master_name" ["master_ip" ["port"]]`

The current version only supports manual booting of instances.

## Task Processing

The master node will expect to find its input image files under `input/` , in its current running directory. If not, it will create the folder and expect that files will be added there later. All output is written under `output/`.

## Command Line Interface

Both instances are capable of listening for instructions from the command line. The current options are:

**Master and Worker:**

* quit = shuts down the node process
* bcast <message_type> <message_details> = broadcasts a message through the network server and all the network clients 

**Master only:**

* sendTo <recipient_name> <message_type> <message_details> = sends a message to a specific worker
* sendTaskTo <recipient_name> <filename> = sends a NEWTASK message with the contents of the mentioned image file to the recipient for processing
* sendNextTaskTo <recipient_name> = like _sendTaskTo_, but the file is the next unprocessed file indicated by the data store

**Worker only:**

* report <message_type> <message_details> = sends a message to the master node
