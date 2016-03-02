#!/usr/bin/perl

use strict;
use warnings;
use Encode qw(decode encode);


my @chars;

while (my $in=<STDIN>) {
    my $outString="";
    $in = decode('UTF-8', $in);
    chop $in;
    @chars=split(//, $in);
    for (my $i=0; $i<=$#chars; $i++) {
	if ($chars[$i]=~/</) {
	    $outString.=encode('UTF-8', 
			       sprintf(' %s{%d}', 
				       $chars[$i],
				       &countBackwardSpaces($i)));
	} elsif ($chars[$i]=~/>/) {
	    $outString.=encode('UTF-8', 
			       sprintf('{%d}%s ', 
				       &countForwardSpaces($i), 
				       $chars[$i]));
	} else {
	    $outString.=encode('UTF-8', sprintf('%s', $chars[$i]));
	}
    }
    printf "%s\n", $outString;
}

sub countBackwardSpaces () {
    my $idx=$_[0];
    my $i; my $n;
    $n=0;
    for ($i=$idx-1; $i>=0; $i--) {
	if ($chars[$i]=~/[ \t]/) {
	    $n++;
	} else {
	    last;
        }
    }
    return $n;
}

sub countForwardSpaces () {
    my $idx=$_[0];
    my $i; my $n;
    $n=0;
    for ($i=$idx+1; $i<=$#chars; $i++) {
	if ($chars[$i]=~/[ \t]/) {
	    $n++;
	} else {
	    last;
	}
    }
    return $n;
}
