#!/usr/bin/perl

use strict;
use warnings;


while (<>) {
    $_=~s/</ </g; 
    $_=~s/>/> /g; 
    print $_;
}
