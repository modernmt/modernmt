# Tags projection API

## Description
The ```Tags projection``` API takes as input a source sentence and its translation and gives as output the translation with the tags projected from the source sentence.

**Note:** Tag Projection is disabled by default because it slow down significantly the engine startup. To enable it set the property `enable_tag_projection` to `True` in configuration file `engines/<engine-name>/engine.ini`.

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

```s```: the XML encoded source sentence.

```t```: the XML encoded translation.

```sl```: the source language (RFC-3066).

```tl```: the target language (RFC-3066).

```d```: (optional) if equals to 1 then the source and target tokens and their alignments will be added to the json response.

```symmetrization```: (optional) possible values are `Intersection`, `Union`, `GrowDiagonalFinalAnd` (default option) and `GrowDiagonal`.

**Output:**

A JSON object with a key ```translation``` whose value is a XML encoded String representing the translation with the tags projected from the source sentence.

### API call example

```GET tags-projection?sl=en&tl=it&s=%3Cbr%3Ehello%2C%20%3Cb%20id%3D%221%22%3Efirst%3Cb%20id%3D%222%22%3E%20test%3Cbr%3E.&t=ciao%2C%20primo%20test.```


```json
{
    "data": {
        "translation": "<br>ciao, <b id=\"1\">primo<b id=\"2\"> test<br>."
    },
    "status": 200
}
```
