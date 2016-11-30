# Running MMT in a Docker Container
## Maintainer: Ulrich Germann (ugermann@inf.ed.ac.uk)

### Running the ready-to-use image

MMT has a Docker image ready-to-use on Docker Hub: **modernmt/master**.

Please see [Install](../INSTALL.md#option-1---using-docker)

### Building your own Images

* Build
```bash
docker build -t <name of your image> dev/Ubuntu-16.04
```

* Run
```bash
docker run -it --name <name of your container> <name of your image> bash
```

This places you into the root directory of MMT.

Use ***--ulimit*** if you plan to have many thoussands of different domains

