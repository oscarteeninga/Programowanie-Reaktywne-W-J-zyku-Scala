#!/bin/bash

#killing the process running on $1 port
to_be_killed=$(lsof -i ":$1" | tail -n 1)
echo $to_be_killed 
echo $to_be_killed | tr -s ' ' | cut -f2 -d' ' | xargs kill -9
