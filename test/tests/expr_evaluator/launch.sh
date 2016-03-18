#!/bin/bash
#Launch your test here

wdir=$(cd $(dirname $0) ; pwd)

tmpLog=${wdir}/ee/LOG.out.$$
if [ $# -eq 0 ]
then
    ${wdir}/ee/evalExpr.pl ${wdir}/ee/hyp.it ${wdir}/ee/ref.it &> ${tmpLog}
    status=$?
else
    ${wdir}/ee/evalExpr.pl "$@" &> ${tmpLog}
    status=$?
fi

if [ "$status" -eq 0 ]
then
    errorRate=$(grep ExprErrorRate < ${tmpLog} | sed -e 's|^\sExprErrorRate\s=\s||')
    echo '{"results": {"expr_error_rate": "'${errorRate}'"}, "passed": true}'
else
    reason=$(cat < ${tmpLog} | tr '\012' ' ')
    echo '{"results": {"error": "'${reason}'"}, "passed": false}'
fi

\rm ${tmpLog}
