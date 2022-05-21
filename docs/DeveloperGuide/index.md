# Exle's Developer's Guide

You're new to Exle, and you want to contribute? What should you do?

First off. Setup and ensure that the app is working on your machine. via [Setup and Running the app](../../README.md/#running-this-app)

Next, pick an issue from [Issues](https://github.com/Ergo-Lend/exle-dot/issues) to work on. Remember to notify the maintainers that you'll be working on it. 

## How to Contribute
1. Implement feature
2. Make a PR request
3. Send for review
4. Get Approved

Congrats, you've made your first improvement.

## How to navigate the code base?
The code base is a play app that depends on multiple modules that are built in-house. Below are the lists of modules and what it does.

### Modules
- **Exle Core**
: The core module that implements the main features, and inherits most of the other modules.
- **Exle Common**
: A common module where generics are stored and are shared between many modules.
- **Exle Chain**
: A system that runs transactions against the blockchain in a concurrent and chained manner.

## Before Submitting PR
run:
1. formatting
```shell
sbt scalafmtAll
```
2. tests
```shell
sbt "project {project name (f.e exle-chain)}" test
```
for test coverage 
```shell
sbt "project {project name (f.e exle-chain)}" coverage test
```