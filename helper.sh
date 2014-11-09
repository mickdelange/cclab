#!/bin/bash
 MASTER='54.172.80.73'
 MASTERID='i-c1bbb72a'
 
 BACKUP='54.172.233.253'
 BACKUPID='i-3eb27bdf'
 
 WORKER1='54.172.68.28'
 WORKER1ID='i-56afa3bd'
 
 WORKER2='54.172.110.230'
 WORKER2ID='i-1bb57cfa'
 
 WORKER3='54.85.100.215'
 WORKER3ID='i-4b5f8faa'
 
 WORKER4='54.172.131.121'
 WORKER4ID='i-485f8fa9'
 
 NODES=($MASTER $BACKUP $WORKER1 $WORKER2 $WORKER3 $WORKER4)
 NODEIDS=($MASTERID $BACKUPID $WORKER1ID $WORKER2ID $WORKER3ID $WORKER4ID)
 
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
 'processed') 
 	scp -i ~/Downloads/test1mick.pem ubuntu@${MASTER}:~/cclab/output/* output
   	;;
 *) 
 	echo "Option not valid"
   	;;
 esac

