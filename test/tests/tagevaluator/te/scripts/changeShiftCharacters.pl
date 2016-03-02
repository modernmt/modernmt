#!/usr/bin/perl

# Chars "[", "]" and "@" are used in TER evaluation for handling
# shifts. Their presence in the original text can cause problems to
# evalTags.pl, therefore this script replaces them with (hopefully)
# unambiguous strings (HTML number)

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
	if ($chars[$i]=~/@/) {
	    $outString.=encode('UTF-8', sprintf('%s', "&#64;"));
	} elsif ($chars[$i]=~/\[/) {
	    $outString.=encode('UTF-8', sprintf('%s', "&#91;")); 
	} elsif ($chars[$i]=~/\]/) {
	    $outString.=encode('UTF-8', sprintf('%s', "&#93;")); 
	} else {
	    $outString.=encode('UTF-8', sprintf('%s', $chars[$i]));
	}
    }
    printf "%s\n", $outString;
}
