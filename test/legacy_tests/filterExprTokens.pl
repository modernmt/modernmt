#! /usr/bin/perl

$c=0;
while ($in=<STDIN>) {
    chop($in);
    $tmp=" ".$in." ";  $tmp=~s/[ \t]/  /g;
    @expr = $tmp=~/[ ]([^ ]*[0-9][^ ]*)[ ]/g;
    
    foreach $expr (@expr) {
	printf "%d-%s ", $c,$expr;
    }
    $c++;
    printf "\n";
}
