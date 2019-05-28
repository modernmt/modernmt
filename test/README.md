# Test suite for ModernMT

Under this directory tree there is the ModernMT test suite. In order to run the tests, the best option is to use the latest Docker image:

```bash
sudo docker run --runtime=nvidia -v $MMT_HOME/test:/opt/mmt/test -it --rm \
                --entrypoint "python3" modernmt/master:latest test
```
