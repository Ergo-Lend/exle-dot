# Chain Reactor
A chain reactor is a lab where ergo transactions are modeled and run. 
Within this framework there are 3 main components
- Reactor
- Chamber
- Atomic Fusion

### Goal
1. Decouple the role of transaction handling from the system
2. Improve efficiency and performance through concurrency for transaction handling
3. A more focused system

The reactor is the shell that holds multiple chambers, which in turn holds
and runs atomic fusions.

When a transaction is drafted, it is drafted as an Atomic Fusion, the atomic
fusion is then cultivated within a chamber. The chamber manages the fusions
that happens within it. The reactor on the other hand stores and manages the
different chambers that exists within it. Other than that, the reactor also 
has the job of interacting with the outside world and notifying users on the 
state of fusions, and chambers.

## Atomic Fusion
This houses a single transaction. When a transaction has to be run, a fusion
is instantiated, and the ins and outs of a transactions is modeled in it. 
This includes but is not limited to, the InputBoxes, Modeled OutBox, Blockchain
Context, TxBuilder etc.

Role: 
1. Provide details of a transaction
2. Run and submits the transaction on chain (ctx funneled from Reactor)
3. Allows a plug and play mode for transactions modeling

## Chamber
The chamber acts as a container that houses the fusions. It doubles as a messenger
queue that runs each transaction that it contains in the most efficient manner.
This would include running it, through an Akka Actor, concurrently.

Role:
1. Manages transactions
2. Improve efficiency of transactions handling through concurrency
3. Sectionalize different types/categories of transactions
4. Assists in transaction chaining

## Reactor
The reactor acts as a middleman between the outside world (user) and the chambers.
Whenever a new transactions needs to be added. The Reactor will receive a notification
and programs the Chamber to instantiate the new fusion. 

When the outside world needs information about a fusion, it will reach out to the 
reactor, and it will provide them with the required information.

Role:
1. Interacts with the outside world
2. Retrieves transactions that needs to be run from explorer
3. Interacts with Chambers to run tx or receive statuses


 