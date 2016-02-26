# Test infrastructure

## Contribute with a new test
All the tests are contained under the directory 'ModernMT/test'.

The easiest way to create a new test is to launch the script 'create_new_test.py':
```bash
cd test
python create_new_test.py My_new_test
```

A json file named 'test.json' contains useful information about all the available tests and it is located inside the root folder of each test.

Inside 'test.json' exists a json map for each runnable tests containing the name, the description and the path of the test it-self:
```json
  {
  "enabled": true,
  "name": "ContextTest",
  "description": "It creates an instance of the context analyzer and queries it to compute some metrics to evaluate the efficiency of the IR system."
  "full_description": "..."
}
```

## Launch a test

First of all compile the source code with:

```bash
cd ModernMT/src
mvn clean install
```

Now to launch a test go under the directory 'ModernMT/src/Test/target/' with

```bash
cd Test/target/
```

and launch the class with the test to run with

```bash
java -cp mmt-test-0.11.1.jar {package_of_the_class_to_run}.{class_to_run}
```

For example to launch the precision test for the context analyzer run the following commands specifying the paths of the training and test directory:

```bash
java -cp mmt-test-0.11.1.jar eu.modernmt.test.contextanalyzer.context.PrecisionTest -lang en -lines 30 -train <path_to_training_directory> -test <path_to_test_directory>
```

Another example is the launch of the tagevaluator test:

```bash
java -Dmmt.home=/path/to/MMT/home -cp mmt-test-0.11.1.jar eu.modernmt.test.tagevaluator.TagEvaluator -t separate -r reference_file -y hypothesis_file
```


## Output

Each test prints on the standard ouput a json containg both a fixed structure and a map depending on the nature of the test.

The structure of the json is the following:
```
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
```json
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
```

Another example is the output of the tagevaluator test on the provided toy files:
```json
{
    "description": "It evaluates the quality of the tag management by calculating the tag-error-rate of an hypothesis file with respect to a reference file.",
    "name": "Tag Evaluator",
    "passed": true,
    "results": {
        "tagErrorRate": 0.15152
    }
}
```
