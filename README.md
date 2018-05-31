# Basic Blockchain

This project showcases a very basic blockchain implementation. You build and run the web server with gradle by issuing:

    gradle clean run

and then you're free to call the HTTP endpoints with Postman, Curl or any other way you prefer. The server runs on `localhost:5050` by default.

## The REST API

Below the blockchain public API is described. Not all of the endpoints are required by the blockchain protocol, but to be able to easily follow what's going on, some events, like triggering a mining process, or synchronizing with any peer nodes, have been isolated to manual HTTP requests issued by the user.

### `POST /transactions`

Records a new transaction which will be included in the next block once it's mined.

| Form field    | Type          | Description                    |
|:------------- |:------------- |:------------------------------ |
| sender        | String        | The id of the sending party.   |
| receiver      | String        | The id of the receiving party. |
| data          | String        | The data being sent.           |

### `GET /transactions`

Returns all pending transactions which haven't been included in a block yet. This endpoint is primarilly for debugging purpouses.

### `GET /block`

Starts mining the next block from a snapshot of the currently pending transactions and either times out after 40 seconds or delivers the newly mined block (which internally has already been added to the local chain). This endpoint is primarilly for debugging purpouses. A new mining process is ideally started as soon as the former is finished or aborted by a peer node propagating a new block it has mined.

### `GET /blockchain`

Returns the current state of the local blockchain as a list of blocks.

### `POST /nodes`

Registers a peer node. On success, the response body MUST include a JSON list of a sub set of at least [1..n] registered peer. 

| JSON attributes      | Description                             |
|:-------------------- |:--------------------------------------- |
| address              | The peer URL to to register.            |

### `DELETE /nodes`

Unregisters a peer node. For the sake of completeness.

| JSON attributes      | Description                             |
|:-------------------- |:--------------------------------------- |
| address              | The peer URL to unregister.          |


## The Blockchain Protocol

Below the blockchain protocol is defined. Any clients must comply to it in their implementations. This version of the protocol doesn't offer any compliance verification of peer nodes, nore does it describe how disobedience is handled. It only describes the blockchain integrity tests.

### The Transaction Model

All nodes must propagate a received transaction to at least one peer. No node may keep duplicates of any transaction.

| Form field    | Type          | Description                     |
|:------------- |:------------- |:------------------------------- |
| id            | String        | The id of the transaction. This field is calculated internally as the SHA-256 hash of "{sender}{receiver}{data}{timestamp}" |
| sender        | String        | The id of the sending party. This field is user provided. Null and empty strings are not allowed. No further verification is done |
| receiver      | String        | The id of the receiving party. This field is user provided and treated exactly as the sender field in terms of verification |
| data          | String        | The data being sent. This field is user provided and not verified on any point |
| timestamp     | long          | The Unix epoch millisecond precision timestamp for when this transaction was created on this node. This field is generated internally |

### The Block Model

The hash of a block is produced by passing the block header to the SHA-256 algorithm. The block header, in turn, is produced as a concatenated string exactly like so:

    {index}{nonce}{timestamp}{prevHash}{transaction[0].id}{...}{transaction[n].id}

Once a block is successfully mined (a nonce is found that produces enough leading zero's) the new block must be propagated to at least [1..n] peers. Once a block is received, as a result of a peer propagation, it must be validated. If valid; any ongoing mining process must be abandoned and the new block must be appended to the local chain. All transactions included in the received block must be removed from the internal cache.

| Form field    | Type          | Description                     |
|:------------- |:------------- |:------------------------------- |
| nonce         | long          | An arbitrary number that, when hashing the block header, produces a resulting hash string that starts with "00" |
| difficulty    | int           | The number of required leading zeros int the produced hash string to consider the block to be solved |
| timestamp     | long          | The Unix epoch millisecond precision timestamp for when the mining process was started |
| prevHash      | String        | The hash string of the previous block header |
| transactions  | Transaction[] | An ordered list of transactions being included in this block |

### The Peer Propagation Algorithm

1. Each node should persist (long term) a list of its known peers.
1. On start-up, each node must register itself to at least one of its peers.
1. Each node must provide a list of at least one of its peer nodes in the registration response body.
1. A node may, but is not required to, add its remote peers ("peers of peers") to persistant storage.

Tips for implementation:
* More peers doesn't necessarily give you an advantage over less peers. The more peers you have the more time you will be spending synchronizing and verifying. On the other hand, having too few peers makes you vulnerable to getting isolated from the network if your (few) peers go offline. "Lagom" is best.
* You may want to maintain your list of peers and favor those who are reasonably fast to respond over those who provide slower responses. This grooming may be required every now and then as network load and other circumstances may play in for shorter or longer periods of time.

### The Verification Algorithm

For each peer node:

1. Favor the longest chain, or your own if equally long.
1. For each block in the chain requested from a peer, verify that:
   1. The nonce produces a hash string that starts with correct amount of zeros.
   1. The produced hash is the same as the "prevHash" field in the next block.

If any single one of the described tests fail, the tested chain is discarded.


## Topics to Discuss

There are a couple of topics rased in the "Issues" section. Feel free to add more as you see fit.

