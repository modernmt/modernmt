#! /bin/bash

function usage()
{
    cmnd=$(basename $0);
    cat<<EOF

$cmnd - adds sentence start/end symbols and trims words longer
       than 80 characters

USAGE:
       $cmnd [options]

OPTIONS:
       -h        Show this message

EOF
}

# Parse options
while getopts h OPT; do
    case "$OPT" in
        h)
            usage >&2;
            exit 0;
            ;;
    esac
done

#adds sentence start/end symbols to standard input and 
#trims words longer than 80 characters

(sed 's/^/<s> /' | sed 's/$/ <\/s>/';) |\
sed 's/\([^ ]\{80\}\)\([^ ]\{1,\}\)/\1/g'

