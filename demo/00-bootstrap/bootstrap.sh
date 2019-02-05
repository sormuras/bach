#!/usr/bin/env bash

jshell https://bit.ly/boot-bach
echo "bach.log.level=INFO"                 >> bach.properties
echo "bach.project.launch=boot/strap.Main" >> bach.properties
./bach
