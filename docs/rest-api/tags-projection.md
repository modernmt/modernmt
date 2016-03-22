# Tags projection API

## Description
The ```Tags projection``` API takes as input a source sentence and its translation and give as output the translation with the tags projected from the source sentence.

### Example

**Input:**

Source sentence:
```<br>hello, <b id="1">&apos;first&apos;<b id="2"> test<br>.```

Translation:
```ciao, &apos;primo&apos; test.```

**Output:**

Translation with tags:  ```<br>ciao, <b id="1">&apos;primo&apos;<b id="2"> test<br>.```

## Input / Output definition

**HTTP method:**

``` GET ```

**Input:**

```s```: the XML encoded source sentence.

```t```: the XML encoded translation.

**Output:**

A JSON object with a key ```translation``` whose value is a XML encoded String representing the translation with the tags projected from the source sentence.

### Example

```GET tags-projection?s=<br>hello%2C%20<b%20id%3D"1">%60first%60<b%20id%3D"2">%20test<br>.&t=ciao%2C%20%26apos%3Bprimo%26apos%3B%20test.```

```json
    {
        "translation": "<br>ciao, <b id=\"1\">&apos;primo&apos; <b id=\"2\">test<br>."
    }
```
