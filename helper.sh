#!/bin/bash
 MASTER='54.165.141.240'
 MASTERID='i-c1bbb72a'
 
 BACKUP='54.173.17.110'
 BACKUPID='i-3eb27bdf'
 
 WORKER1='54.165.194.46'
 WORKER1ID='i-56afa3bd'
 
 WORKER2='54.172.241.40'
 WORKER2ID='i-1bb57cfa'
 
 WORKER3='54.84.150.16'
 WORKER3ID='i-4b5f8faa'
 
 WORKER4='54.165.112.16'
 WORKER4ID='i-485f8fa9'
 
 NODES=($MASTER $BACKUP)
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

