#!/bin/bash

export LC_ALL=C

__home=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $__home/env.sh ""

moses=$moses_home/bin/moses

usage() {
	echo "Usage: -i <input_file> -f <config_file> <decoder-opt-arg>*"
}

lerr() {
	if [ $# -eq 2 ]; then
		>&2 echo $1 "ERROR: $2"
	else
		>&2 echo "ERROR: $1"
	fi
}

update_first_id_with_counter() {
	offset=$1
	perl -pe 's/^(\d+)(\s.*)$/($1+'$offset').$2/e'
}

input_file=
config_file=
show_weights=
print_id=
nbest_file=
nbest_size=
nbest_distinct=
weight_overwrite=
other_pars=

# Args parsing

while [[ $# > 0 ]]; do
	key="$1"

	case $key in
	-i|-input-file|--input)
		input_file="$2"
		shift 2
	;;

	-f|-config|--config)
		config_file="$2"
		shift 2
	;;
	-n-best-list|--n-best-list)
		nbest_file="$2"
		nbest_size="$3"
		nbest_distinct="$4"
		shift 4
	;;
	-weight-overwrite|--weight-overwrite)
		weight_overwrite="$2"
		shift 2
	;;
	-show-weights|--show-weights)
		show_weights=$key
		shift 1
	;;
	-print-id|--print-id)
		print_id=$key
		shift 1
	;;
	*)
		other_pars="${other_pars} $key"
		shift 1
	;;
	esac
done

decoder_pars="${other_pars} ${print_id}"

# Args validation

if [ -z "$config_file" ]; then
	lerr "missing parameter 'config_file' (-f, --config)"
	usage
	exit 1
fi
if [ ! -f "$config_file" ]; then
	lerr "'config_file' parameter is not a valid file: $config_file"
	printhelp
	exit 2
fi

if [ -z $show_weights ] && [ -z "$input_file" ]; then
	lerr "missing parameter 'input_file' (-i, --input)"
	usage
	exit 1
fi
if [ -z $show_weights ] && [ ! -f "$input_file" ]; then
	lerr "'input_file' parameter is not a valid file: $input_file"
	printhelp
	exit 2
fi

# Show weigth if requested

if [ ! -z $show_weights ]; then
	$moses -f $config_file ${decoder_pars} ${show_weights}
	exit 0
fi

# Temp files

tmpnbestF=$( mktemp -t mert_nbest.XXXXXXXXXX )

function cleanup_temp {
	rm -f $tmpnbestF
}

trap cleanup_temp EXIT

# Start translation

if [ ! -z $nbest_file ]; then
	nbest_pars="--n-best-list ${tmpnbestF} ${nbest_size} ${nbest_distinct}"
fi
if [ ! -z ${nbest_file} -a -e ${nbest_file} ]; then
	: > ${nbest_file}
fi 

count_lines=0

cat ${input_file} | ( while read line ; do

	sentence=${line%"$METALINE_CONTEXT_SEPARATOR"*}
	context=${line##*"$METALINE_CONTEXT_SEPARATOR"}

	cw="--context-string \"$context\""

	if [ -z "${weight_overwrite}" ]; then
		if [ -z "${print_id}" ] ; then
			echo $sentence | $moses -f ${config_file} ${decoder_pars} ${cw} ${nbest_pars}
		else
			echo $sentence | $moses -f ${config_file} ${decoder_pars} ${cw} ${nbest_pars} | update_first_id_with_counter $count_lines
		fi
	else
		if [ -z "${print_id}" ] ; then
			echo $sentence | $moses -f ${config_file} ${decoder_pars} ${cw} ${nbest_pars} -weight-overwrite "${weight_overwrite}"
		else
			echo $sentence | $moses -f ${config_file} ${decoder_pars} ${cw} ${nbest_pars} -weight-overwrite "${weight_overwrite}" | update_first_id_with_counter $count_lines
		fi
	fi

	cat ${tmpnbestF} | update_first_id_with_counter $count_lines >> ${nbest_file}

	count_lines=$(( $count_lines + 1 ))

done )



