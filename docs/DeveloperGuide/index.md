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
- **Single Lender**
: Single Lender feature is all in this module
- **Exle Common**
: A common module where generics are stored and are shared between many modules.
- **Exle Chain**
: A system that runs transactions against the blockchain in a concurrent and chained manner.
- **Exle Pay**
: Ergo Pay transaction Handling
- **Tx Handler**
: Handles transactions that needs to be processed

## Implementing a new Loan Feature
Implementing a new loan feature would require a few components.
1. System Design (ExIP)
2. Boxes (yml files)
3. Contracts (Template: exle-common/src/main/resources/ExleContracts/Templates)
4. Boxes Case Classes 
4. Txs
5. Tx Handlers


## Before Submitting PR
run:
1. formatting, refer to [scalafmt](https://scalameta.org/scalafmt/docs/installation.html#task-keys)
```shell
sbt scalafmtAll
sbt scalafmtSbt
sbt single-lender/scalafmt
sbt single-lender/Test/scalafmt
sbt exle-common/scalafmt
sbt exle-common/Test/scalafmt
...
```
or run ./exlefmt.sh (update sh file if adding modules)
2. tests
```shell
sbt "project {project name (f.e exle-chain)}" test
```
for test coverage 
```shell
sbt "project {project name (f.e exle-chain)}" coverage test
```