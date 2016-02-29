<?php

$domain_name = @$argv[1];
$source = @$argv[2];
$target = @$argv[3];

if (!$domain_name OR !$source OR !$target) {
 echo "\nUsage: php AB-test.php domain source_lang target_lang\n";
 echo "Run ./mmt evaluate before running this script\n";
 echo "Current limitation: Works only with single domain testing\n\n";
 exit;
}

$segs = file('../runtime/b1/master/tmp/evaluate/source.'.$source);
$ref =  file('../runtime/b1/master/tmp/evaluate/reference.'.$target);
$mmt =  file('../runtime/b1/master/tmp/evaluate/translations/MMT/'.$domain_name.'.'.$target);
$gt  =  file('../runtime/b1/master/tmp/evaluate/translations/Google_Translate/'.$domain_name.'.'.$target);

$i = 0;
foreach ($segs as $seg) {
        echo "ORIGINAL: $seg";
        echo "HUMAN   : ".$ref[$i]."";
        echo "MMT     : ".$mmt[$i]."";
        echo "GT      : ".$gt[$i]."";
        echo "\n\n";
        $i++;
}