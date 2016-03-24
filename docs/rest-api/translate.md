# Translate API

## Description
The ```Translate``` API returns the translation of the provided text. The text is XML encoded and **can contain XML Tags**.
Because each MMT Engine supports only one language pair in one direction, you don't need to specify the source and target languages.

## Input / Output definition

**HTTP method:**

``` GET ```

**Input:**

* ```q```: the text to translate in XML format (XML tags accepted, text must be XML-encoded)
* ```context```: the context text to be used to translate the sentence (if *null*, no context will be used)
* ```context_array```: a JSON Array containing the exact context to be used (format is the same of the output of the ```Context API```)
* ```nbest```: the number of top best translations (default is 0)

**Output:**

A JSON object containing the following fields:

```translation``` the translation of the provided sentence

```took``` the decoding time in millisecods

```context``` the context scores used to translate the sentence

```nbest``` the list of the top N best translations with their scores

### Example

```GET translate?q=%26quot%3BThis%20is%20an%20%3Cb%3Eexample%3C%2Fb%3E%26quot%3B&context=Mr+President```

```json
{
    "context": [
        {
            "id": "europarl",
            "score": 0.13375875
        },
        {
            "id": "ibm",
            "score": 0.008800022
        }
    ],
    "took": 45,
    "translation": "&quot;Questo \u00e8 un <b>esempio</b>&quot;"
}
```
