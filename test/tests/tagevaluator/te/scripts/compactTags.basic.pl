#!/usr/bin/perl

use strict;
use warnings;


while (<>) {
    my $openTagsN=0;
    my $outString="";

    chop; my $seg=$_; $seg=~s/[ \t]+/ /g; $seg=~s/^ | $//g;
    my @tok=split(/ /, $seg);
    $openTagsN=0;
    for (my $i=0; $i<=$#tok; $i++) {
	if ($openTagsN>0) {
	    $outString.="_#_$tok[$i]";
	} else {
	    $outString.=" $tok[$i]";
	}
	$openTagsN++ if ($tok[$i]=~/^</);
	$openTagsN-- if ($tok[$i]=~/>$/);
    }
    printf STDOUT "%s\n", $outString;
}
