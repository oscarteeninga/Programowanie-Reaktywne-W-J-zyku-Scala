#!/usr/bin/env bash

sbt "runMain EShop.lab6.WorkHttpApp" &

sbt "runMain EShop.lab6.ClusterNodeApp seed-node1" &
sbt "runMain EShop.lab6.ClusterNodeApp seed-node2" &
sbt "runMain EShop.lab6.ClusterNodeApp" &

sbt "runMain EShop.lab6.WorkHttpClusterApp 9001" &
sbt "runMain EShop.lab6.WorkHttpClusterApp 9002" &
sbt "runMain EShop.lab6.WorkHttpClusterApp 9003" &

sbt gatling-it:test
sbt gatling-it:lastReport