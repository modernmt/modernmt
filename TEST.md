# Test infrastructure

## Contribute with a new test
All the tests are contained in the module 'Test' under the directory 'ModernMT/src'.

In order to easily maintain all the tests, the package structure of each test is built as follow:

```
eu.modernmt.test.{name_of_module_to_test}.{package_of_the_class_to_test}
```

A json file named 'test.json' contains useful information about all the available tests and it is located under the package 'eu.modernmt.test'.

Inside 'test.json' exists a json map for each runnable tests containing the name, the description and the path of the test it-self:
```json
[
  {
    "script": "contextanalyzer/context/PrecisionTest.java",
    "name": "ContextTest",
    "description": "It creates an instance of the context analyzer and queries it to compute some metrics to evaluate the efficiency of the IR system."
  }
]
```

## Launch a test

To launch a test go under the directory 'ModernMT/src/Test/target/' with

```bash
cd ModernMT/src/Test/target/
```

and launch the class with the test to run with

```bash
java -cp mmt-test-0.11.1.jar {package_of_the_class_to_run}.{class_to_run}
```

For example to launch the precision test for the context analyzer run the following commands specifying the paths of the training and test directory:

```bash
cd ModernMT/src/Test/target/
java -cp mmt-test-0.11.1.jar eu.modernmt.test.contextanalyzer.context.PrecisionTest -lang en -lines 30 -train <path_to_training_directory> -test <path_to_test_directory>
```

## Output

Each test print on the standard ouput a json containg both a fixed structure and a maps depending on the nature of the test.

The structure of the json is the following:
```json
{  
   "name": "<NAME_OF_THE_TEST>",
   "description": "<USEFUL_DESCRIPTION_OF_THE_TEST>",
   "passed": <true|false>,
   "results": {  
      <USEFUL_INFORMATION_ABOUT_THE_RESULTS_OF_THE_TEST>
   }
}
```

An example of the output of a test is the one generate from the PrecisionTest of the context analyzer:

{  
   "name":"Context Analyzer Precision Test",
   "description":"It creates an instance of the context analyzer and queries it to compute some metrics to evaluate the efficiency of the IR system.",
   "passed":true,
   "results":{  
      "avgScoreGap":0.29505555480718615,
      "AvgQueryTime":675.05,
      "Recall":1.0,
      "avgPosition":1.0,
      "numberOfMatches":1.0
   }
}
