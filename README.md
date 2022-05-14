# Exle-Dot: A P2P Lending Platform built on Ergo

![CI](https://github.com/Ergo-Lend/exle-dot/actions/workflows/scala.yml/badge.svg?branch=main)

This is the backend service that Exle (also known as Ergo-Lend) is built on. This is an application that is built on the Scala and Play framework.

## Table of contents

- [Running this app](#running-this-app)
  - [`.env`](#env)
  - [`run`](#run)
- [Developer Guide](docs/DeveloperGuide/index.md)
- [Exle Improvement Proposals](#exle-improvement-proposals)

## Running this app
You'll need to have [Docker installed](https://docs.docker.com/get-docker/).

```sh
git clone https://github.com/Ergo-Lend/exle-dot
cd exledot
```

### Copy a few example files because the real files are git ignored:

```shell
cp .env.example .env
cp docker-compose.override.yml.example docker-compose.override.yml
```

### Build everything:
*The first time you run this it's going to take a while depending on your internet connection speed and computer's hardware specs. That's because it's going to download a few Docker images and build the sbt dependencies*

```shell
docker-compose up --build
```

Now that everything is built and running, you can call the API at localhost:9000. As this is only the backend service of the app, a web frontend is not included.

To efficiently test it, we highly recommend [Postman](https://www.postman.com/)

## Exle Improvement Proposals
There's always room for improvement, and we're only at the start of where we would like to get to. If you have an idea for improvement, submit a proposal via a pull request at [ExIPs](https://github.com/Ergo-Lend/exips). 
You can also be a part of Exle by contributing to our repo and find issues that you can start off at [issues](https://github.com/Ergo-Lend/exle-dot/issues), and look for [good first issues](https://github.com/Ergo-Lend/exle-dot/labels/good%20first%20issue).

For more guides on developing and contributing, please checkout [Developers Guide](docs/DeveloperGuide/index.md).

Be chill, and let's code away. :)