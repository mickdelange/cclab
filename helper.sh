#!/bin/bash
 MASTER='54.173.192.88'
 MASTERID='i-c1bbb72a'
 
 BACKUP='54.173.84.22'
 BACKUPID='i-3eb27bdf'
 
 WORKER1='54.173.184.220'
 WORKER1ID='i-56afa3bd'
 
 WORKER2='54.173.150.98'
 WORKER2ID='i-1bb57cfa'
 
 NODES=($MASTER $WORKER1 $WORKER2)
 NODEIDS=($MASTERID $BACKUPID WORKER1ID $WORKER2ID)
 
 # get number of elements in the array
 NUMBER=${#NODES[@]}
 
 case "$1" in

 'newjar') 	
 	for (( i=0;i<$NUMBER;i++)); do
    	scp -i ~/Downloads/test1mick.pem code/CCLabCore/target/*.jar ubuntu@${NODES[${i}]}:~/cclab/bin
	done 
	;;
 'connect') 
 	NAME=$2
		ssh -i ~/Downloads/test1mick.pem ubuntu@${!NAME}
    ;;
 'input')  	
 	scp -i ~/Downloads/test1mick.pem input/* ubuntu@${MASTER}:~/cclab/input
    ;;
 'logs') 
	NAME=$2	
 	scp -i ~/Downloads/test1mick.pem ubuntu@${!NAME}:~/cclab/logs/* logs
   	;;
 *) 
 	echo "Option not valid"
   	;;
 esac

