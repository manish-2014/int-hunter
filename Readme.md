# Introduction

This dinky little project can scan war/jar/ear/tar files for Java code and identitfies any anti-pattern in code

In my case, I wanted to find all java methods in my code base that were setting "int" values to database.
We were running out of int in tables and wanted to find which all columns and associtated code would have to chage to long type.
This project can be used to find such code and then you can change the code accordingly.

Design.md is the design document for the project. It explains how the code works, how to run it, and how to extend it.

## note on using ai
well Claude code is not very good at understanding bytecode and how to parse it and using butecode distances. Howeever , it did generat the project stucture and the serviceloader etc.
I coded the Extractors.

## How to run the project

1. Clone the repository
2. mvn clean compile test install
3. runing in terminal
   java -jar int-hunter-1.0.0.jar --archiveFile <path to ear>/<test.ear> --stagingDir ./staging --out report.csv
