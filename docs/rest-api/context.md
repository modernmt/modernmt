# Context API

## Description
The ```Context``` API returns an array of the top domains matching the provided content.
For each domain you obtain the consine similarity between the given context, and that domain.

## Input / Output definition

**HTTP method:**

``` GET ```

**Input:**

* ```limit```: the max number of returned elements (default is 10)
* ```text```: the context text to be analyzed
* ```local_file```: the absolute path to a local file to be used as context string; if specified, ```text``` is ignored. 

**Output:**

A JSON array of objects with ```id``` and ```score````:

```id``` is the name of the domain

```score``` is the cosine similarity between the domain and the given text

### Example

```GET context?text=Mr+President```

```json
[
    {
        "id": "europarl",
        "score": 0.13375875
    },
    {
        "id": "ibm",
        "score": 0.008800022
    }
]
```
