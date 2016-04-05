#! /usr/bin/perl

exit print "usage: \n\n evalExpr.pl hyp ref [-n (normalization)] [-v (verbosity)]\n\n" if $#ARGV != 1 && $#ARGV != 2 && $#ARGV != 3;

# Read optional arguments
$norm=0; $verbose=0;
foreach my $a (2..$#ARGV) {

    if ($ARGV[$a] eq "-n") {
        $norm=1;
    } elsif ($ARGV[$a] eq "-v") {
        $verbose=1;
    } else {
        printf "Wrong parameter (%s)\n", $ARGV[$a];
	print "usage: \n\n evalExpr.pl hyp ref [-n (normalization)] [-v (verbosity)]\n\n";
        exit(0);
    }
}
print "n=$norm v=$verbose\n";

$err_msg = "\tError in opening file\n";
$|=1;

open (FH, "<$ARGV[0]") || die $err_msg, "$ARGV[0]";
chop(@hyp=<FH>);
close(FH);

open (FH, "<$ARGV[1]") || die $err_msg, "$ARGV[1]";
chop(@ref=<FH>);
close(FH);

if ($#hyp!=$#ref) {
    printf STDERR "ERROR: different number of lines in input files (%d vs. %d)\n", $#hyp+1, $#ref+1;
    exit(0);
}


$totCorrN=0; $totSubN=0; $totDelN=0; $totInsN=0;
for ($i=0; $i<=$#ref; $i++) {
    printf "REF: %s\nHYP: %s\n", $ref[$i], $hyp[$i] if ($verbose>0);
    $tmp=" ".$ref[$i]." ";  $tmp=~s/[ \t]/  /g;
    if ($norm==1) {
	$tmp=~s/</&lt;/g; $tmp=~s/>/&gt;/g;
     	$tmp=~s/\"/&quot;/g; $tmp=~s/\'/&apos;/g;
     	$tmp=~s/\&/&amp;/g;
    }
    @refExpr = $tmp=~/[ ]([^ ]*[0-9][^ ]*)[ ]/g;
    $tmp=" ".$hyp[$i]." ";   $tmp=~s/[ \t]/  /g;
    if ($norm==1) {
    	$tmp=~s/</&lt;/g; $tmp=~s/>/&gt;/g;
    	$tmp=~s/\"/&quot;/g; $tmp=~s/\'/&apos;/g;
    	$tmp=~s/\&/&amp;/g;
    }
    @hypExpr = $tmp=~/[ ]([^ ]*[0-9][^ ]*)[ ]/g;
    ($corrN,$subN,$delN,$insN) = &computeMatches();
    $totCorrN+=$corrN; $totSubN+=$subN; $totDelN+=$delN; $totInsN+=$insN;
    printf " C=%d S=%d D=%d I=%d\n\n", $corrN, $subN, $delN, $insN if ($verbose>0);
}

printf "\n\tExprErrorRate = %.2f (C=%d S=%d D=%d I=%d)\n\n", 
    100-100*$totCorrN/($totCorrN+$totSubN+$totDelN+$totInsN), 
    $totCorrN, 
    $totSubN, 
    $totDelN, 
    $totInsN;

exit(0);

sub computeMatches () {
    my (%refExpr, %hypExpr);
    my ($mN,$sN,$dN,$iN)=(0,0,0,0);
    %refExpr=();
    %hypExpr=();
    foreach $refExpr (@refExpr) {
	$refExpr{"$refExpr"}++;
    }
    foreach $hypExpr (@hypExpr) {
	$hypExpr{"$hypExpr"}++;
    }
    foreach my $refExpr (keys %refExpr) {
	if (defined $hypExpr{"$refExpr"}) { # this expr occurrs both in ref and in hyp => MATCH
	    if ($refExpr{"$refExpr"}<$hypExpr{"$refExpr"}) {
		$mN+=$refExpr{"$refExpr"};
		printf "\tMATCH (%d times) %s\n", $refExpr{"$refExpr"},$refExpr if ($verbose>0);
		printf "\tINS   (%d times) %s\n", $hypExpr{"$refExpr"}-$refExpr{"$refExpr"}, $refExpr if ($verbose>0);
		$iN+=$hypExpr{"$refExpr"}-$refExpr{"$refExpr"};
	    } elsif ($refExpr{"$refExpr"}>$hypExpr{"$refExpr"}) {
		$mN+=$hypExpr{"$refExpr"};
		printf "\tMATCH (%d times) %s\n", $hypExpr{"$refExpr"}, $refExpr if ($verbose>0);
		printf "\tDEL   (%d times) %s\n", $refExpr{"$refExpr"}-$hypExpr{"$refExpr"}, $refExpr if ($verbose>0);
		$dN+=$refExpr{"$refExpr"}-$hypExpr{"$refExpr"};
	    } else { # $refExpr{"$refExpr"} == $hypExpr{"$refExpr"}
		$mN+=$hypExpr{"$refExpr"};
		printf "\tMATCH (%d times) %s\n", $hypExpr{"$refExpr"}, $refExpr if ($verbose>0);
	    }
	    delete $hypExpr{"$refExpr"};
	} else { # this expr occurrs only in ref => DELETION
	    printf "\tDEL   (%d times) %s\n", $refExpr{"$refExpr"}, $refExpr if ($verbose>0);
	    $dN+=$refExpr{"$refExpr"};
	}
    }

    $flag4print=1;
    foreach $hypExpr (keys %hypExpr) { # this expr was not matched => INSERTION
	printf " Checking remaining INS:\n" if ($verbose>0 && $flag4print);
	$flag4print=0;
	printf "\tINS   (%d times) %s\n", $hypExpr{"$hypExpr"}, $hypExpr if ($verbose>0);
	$iN+=$hypExpr{"$hypExpr"};
    }
    printf "\n" if ($verbose>0);
    $sN=($dN>$iN)?$iN:$dN;
    $dN-=$sN;
    $iN-=$sN;
    return ($mN,$sN,$dN,$iN);
}
