# Tags projection API

## Description
The ```Tags projection``` API takes as input a source sentence and its translation and gives as output the translation with the tags projected from the source sentence.

### Example

**Input:**

Source sentence:
```<br>hello, <b id="1">first<b id="2"> test<br>.```

Translation:
```ciao, primo test.```

**Output:**

Translation with tags:  ```<br>ciao, <b id="1">primo<b id="2"> test<br>.```

## Input / Output definition

**HTTP method:**

``` GET ```

**Input:**

```s*```: the XML encoded source sentence.

```t*```: the XML encoded translation.

```sl```: the source language (RFC-3066).

```tl```: the target language (RFC-3066).

```d```: if equals to 1 then the source and target tokens and their alignments will be added to the json response.

```symmetrization```: 0 = Intersection, 1 = Union, 2 = GrowDiagFinalAnd (default option), 3 = GrowDiag.

```*```: mandatory fields

**Output:**

A JSON object with a key ```translation``` whose value is a XML encoded String representing the translation with the tags projected from the source sentence.

### API call example

```GET http://localhost:8045/tags-projection?s=<br>hello%2C%20this%20is%20the%20%3Cb%3Efirst%3C%2Fb%3E%20test%26t%3Dciao%2C%20primo%20test.```


```json
    {
        "translation": "<br>ciao, <b id=\"1\">primo<b id=\"2\"> test<br>."
    }
```
