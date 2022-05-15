# Exle-Dot: A P2P Lending Platform built on Ergo

![CI](https://github.com/Ergo-Lend/exle-dot/actions/workflows/scala.yml/badge.svg?branch=main)

This is the backend service that Exle (also known as Ergo-Lend) is built on. This is an application that is built on the Scala and Play framework.

## Table of contents

- [Running this app](#running-this-app)
  - [`.env`](#env)
  - [`run`](#run)
- [Developer Guide](docs/DeveloperGuide/index.md)
  - [How to Contribute](docs/DeveloperGuide/index.md/#how-to-contribute)
- [Exle Improvement Proposals](#exle-improvement-proposals)

## Running this app
Build exle-dot:
```sh
git clone https://github.com/Ergo-Lend/exle-dot
cd exledot
sbt assembly
```

Update the environment variables for your db by running by creating a .env file with the values filled up for:
```shell
JDBC_DB_URL="jdbc:postgresql://localhost:{port}/{tablename}"
POSTGRES_PASSWORD={database password}
POSTGRES_USER={database user}
SECRET_KEY={Play app secret}
```

Run the debug environment by running:
```shell
sbt -jvm-debug 9000 run
```

## Docker quick start
You'll need to have [Docker installed](https://docs.docker.com/get-docker/).

### Copy a few example files because the real files are git ignored:

```shell
cp .env.example .env
cp docker-compose.override.yml.example docker-compose.override.yml
```

If you have a database set up and would like to use your own db. Change POSTGRES_PASSWORD in [docker-compose.yml](docker-compose.yml) and POSTGRES_USER in your new .env file.

### Build everything:
*The first time you run this it's going to take a while depending on your internet connection speed and computer's hardware specs. That's because it's going to download a few Docker images and build the sbt dependencies*

```shell
docker compose up --build
```

Now that everything is built and running, you can call the API at localhost:9000. As this is only the backend service of the app, a web frontend is not included.

To efficiently test it, we highly recommend [Postman](https://www.postman.com/)

And when you're done, use this command to close out everything.
```shell
docker compose down
```

Note: This build uses a volume on your machine. And when you first run it, it will initialize all the db required in the postgres container and save to that volume. If it's running db errors, please check logs via:
```shell
docker compose logs -f # -f to follow the logs
```

## Exle Improvement Proposals
There's always room for improvement, and we're only at the start of where we would like to get to. If you have an idea for improvement, submit a proposal via a pull request at [ExIPs](https://github.com/Ergo-Lend/exips). 
You can also be a part of Exle by contributing to our repo and find issues that you can start off at [issues](https://github.com/Ergo-Lend/exle-dot/issues), and look for [good first issues](https://github.com/Ergo-Lend/exle-dot/labels/good%20first%20issue).

For more guides on developing and contributing, please checkout [Developers Guide](docs/DeveloperGuide/index.md).

Be chill, and let's code away. :)