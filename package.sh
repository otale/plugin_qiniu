#!/bin/bash

mvn clean assembly:assembly

mv target/plugin_qiniu-jar-with-dependencies.jar plugin_qiniu.jar
