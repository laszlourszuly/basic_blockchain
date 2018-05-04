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

Returns all pending transactions which haven't been included in a block yet.

### `GET /block`

Starts mining the next block from a snapshot of the currently pending transactions and either times out after 40 seconds or delivers the newly mined block (which then already is added internally to the local chain).

### `GET /blockchain`

Returns the current state of the local blockchain as a list of blocks.

### `POST /nodes`

Registers a peer node which we can synchronize against, once requested to do so.

| Form field    | Type          | Description                    |
|:------------- |:------------- |:------------------------------ |
| address       | String        | The peer URL to to register.   |

### `DELETE /nodes`

Unregisters a peer node. We will no longer be able to synchronize against this peer.

| Form field    | Type          | Description                    |
|:------------- |:------------- |:------------------------------ |
| address       | String        | The peer URL to to unregister. |

### `GET /nodes`

Returns the URL's of all currently registered peer nodes.


## The Blockchain Protocol

Below the blockchain protocol is defined. Any clients must comply to it in their implementations. This version of the protocol doesn't offer any compliance verification on new nodes (nore does it describe how disoedience should be handled). It only describes the blockchain integrity tests.

### The Transaction Model

| Form field    | Type          | Description                     |
|:------------- |:------------- |:------------------------------- |
| id            | String        | The id of the transaction. This field is calculated internally as the SHA-256 hash of "{sender}{receiver}{data}{timestamp}" |
| sender        | String        | The id of the sending party. This field is user provided. Null and empty strings are not allowed. No further verification is done |
| receiver      | String        | The id of the receiving party. This field is user provided and treated exactly as the sender field in terms of verification |
| data          | String        | The data being sent. This field is user provided and not verified on any point |
| timestamp     | long          | The Unix epoch millisecond precision timestamp for when this transaction was created on this node. This field is generated internally |

### The Block Model

The hash of a block is produced by passing the block header to the SHA-256 algorithm. The block header, in turn, is produced as a concatenated string exactly like so:

    {index}{nonce}{timestamp}{prevHash}{transaction[0].id}{transaction[n-1].id}{transaction[n].id}

| Form field    | Type          | Description                     |
|:------------- |:------------- |:------------------------------- |
| index         | int           | The index of the block within the blockchain |
| nonce         | long          | An arbitrary number that, when hashing the block header, produces a resulting hash string that starts with "00" |
| timestamp     | long          | The Unix epoch millisecond precision timestamp for when the mining process was started |
| prevHash      | String        | The hash string of the previous block header |
| transactions  | Transaction[] | An ordered list of transactions being included in this block |

### The Synchronization Algorithm

For each peer node:

1. Favor the longest chain, or your own if equally long.
1. Verify for each block in the chain that
    1. The nonce produces a hash string that starts with "00"
    1. The produced hash it the same as the "prevHash" field in the next block


## Topics to Discuss

There are a couple of topics rased in the "Issues" section. Feel free to add more as you see fit.

