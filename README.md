Cloud Computing lab exercise
=====

This repository contains the work of Mick de Lange and Maria Voinea for the IN4392 Cloud Computing course of the TU Delft.

## Structure

Both scheduler and target application logic can be found under the same project in the `code` folder. It can be compiled with dependencies using maven:

`mvn clean compile assembly:single`

and then run directly as a jar:

`java -jar cclab-core-1.0-SNAPSHOT-jar-with-dependencies.jar`