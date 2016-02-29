# Test infrastructure

## Contribute with a new test
All the tests are contained under the directory 'ModernMT/test/tests'.

The easiest way to create a new test is to launch the script 'create_new_test.py' and to pass as argument the name of the test that you would like to create.

In order to do that, from the root directory of the ModernMT project launch the following commands:
```bash
cd test
python create_new_test.py My_new_test
```

The 'create_new_test.py' script will generate a directory under the direcotry 'ModernMT/test/tests' with the same name that you specified as argument.

Under this new directory it also creates two files:

  * launch.sh
  * test.json

#### launch.sh

'launch.sh' is the script that will be launch automatically from 'ModernMT/test/run_all_tests.py' script.

Edit 'launch.sh' in order to launch your test. 

Note: use the string "$@" (double quotes included) to forward the arguments of the bash script to your test.

The following commands show how to launch your test and redirect the arguemnts to it:
```bash
java eu.modermt.MyNewTest "$@"
python MyNewTest.py "$@"
./MyNewTest.sh "$@"
```

#### test.json

The json file named 'test.json' contains useful information about the test it-self and it is located inside the root folder of each test.

'test.json' must contain at least the following keys:
```
  {
  "enabled": true|false,
  "name": "<TEST_NAME>",
  "description": "A description of the test",
  "full_description": "A complete and exhaustive description of the test"
}
```

## Output

Each test prints on the standard ouput a json containg both a fixed structure and a map depending on the nature of the test.

The structure of the json is the following:
```
{  
   "passed": true|false,
   "results": {  
      <USEFUL_INFORMATION_ABOUT_THE_RESULTS_OF_THE_TEST>
   }
}
```

An example of the output of a test could be the following:
```json
{  
   "passed":true,
   "results":{  
      "recall": 0.7,
      "minAllowedRecall": 0.5,
      "precision": 0.8,
      "minAllowedPrecision": 0.6,
      "avgQueryTime": 200,
      "minAllowedQueryTime": 100
   }
}
```

## Launch a test

Move into the test directory from the root directory of the ModerMT project with:

```bash
cd test
```

Now you an list all the available test with:

```bash
python run_tests.py -list
```

It outputs useful information about which tests are enabled, which are not and which have a malformed test.json:

```
## Enabled tests ## 
#Test_name Enabled Description
My_New_Test_1 true  "It tests the precision of ..."
My_New_Test_4 true  "It tests the throughput of ..."

## Disabled tests ## 
#Test_name Enabled Description
My_New_Test_3 false  "It tests the recall of ..."
My_New_Test_5 false  "It tests the alignment of ..."

## Malformed tests ## 
#Test_name Enabled Description
My_New_Test_2
My_New_Test_6

```

To execute a specific test execute run_tests.py and pass the name of the test as argument:

```bash
python run_tests.py -name My_New_Test_1
```

Or to execute all the enabled tests run run_tests.py without any arguments:

```bash
python run_tests.py
```
