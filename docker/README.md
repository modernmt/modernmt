# Running MMT in a Docker Container
## Maintainer: Ulrich Germann (ugermann@inf.ed.ac.uk)

* Build
```bash
docker build -t <name of your image> dev/Ubuntu-16.04
```

* Run
```bash
docker run -it --name <name of your container> <name of your image> bash
```

This places you into the root directory of MMT.

