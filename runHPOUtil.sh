#!/bin/bash


HPO="/home/peter/SVN/GIT/hrmd/hrmdgui/hrmdgui_data/hp.obo"
MORBID=""
ANNOT=""
HNAME=`hostname`
if [ $HNAME = 'peter' ]; then
    HPO="/home/peter/SVN/GIT/monarch/hrmd/hrmdgui/hrmdgui_data/hp.obo"
    MORBID="/home/peter/SVN/apps/HPOapps/skelnos2OWL/morbidmap"
    ANNOT="/home/peter/SVN/HPOannot/annt/OMIM/by-disease/annotated"
fi

java -jar target/hpoutil-0.0.1-SNAPSHOT.jar --hpo $HPO --morbidmap $MORBID -A $ANNOT
