#!/bin/bash
javac -cp ./target/go_data_crawler.jar -processorpath ./target/go_data_crawler.jar -processor dbg.n.tst.LocalVarProcessor -d ./target/app ./src/main/java/jbuild/main_jbuild.java
