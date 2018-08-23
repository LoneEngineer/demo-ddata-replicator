#!/bin/bash

victim=$1
node1=$2
node2=$3

victimIP=`kubectl get pod $victim -o json | jq .status.podIP | cut -d \" -f 2`
node1IP=`kubectl get pod $node1 -o json | jq .status.podIP | cut -d \" -f 2`
node2IP=`kubectl get pod $node2 -o json | jq .status.podIP | cut -d \" -f 2`

kubectl exec $victim -- route add -net $node1IP netmask 255.255.255.255 reject && \
 kubectl exec $victim -- route add -net $node2IP netmask 255.255.255.255 reject && \
 kubectl exec $node1 -- route add -net $victimIP netmask 255.255.255.255 reject && \
 kubectl exec $node2 -- route add -net $victimIP netmask 255.255.255.255 reject