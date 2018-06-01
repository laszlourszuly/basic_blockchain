# Basic Blockchain

This project showcases a very basic blockchain implementation. The example implementation is built and run by issuing the below command from the terminal:

    gradle clean run

And then you're free to call the HTTP endpoints with Postman, Curl or any other way you prefer. The example server runs on `localhost:5050` by default.

## The Workshop

Doing the lab yourself you'll write everything from scratch in your preferred programming language, using your favorite tools. The blockchain protocol it self doesn't require neither Java nor Gradle. Choose with your heart.

The lab will focus on the proof-of-work implementation, also known as the "mining". Regarding transaction- and block propagation, as well as validation, we'll settle with the most basic implementation possible. These areas can involve advanced mathematics in a real-world implementation, hence, falling a bit outside the scope for now.

## PART 1: The REST API

Below the blockchain public API is described. You're expected to implement a web server that can respond to the described requests.

### `/transactions [GET]`

Returns all pending transactions which haven't been included in a block yet. This endpoint is for debugging purposes.

### `/transactions [POST]`

Temporarily caches a new transaction. The blockchain implementation will include it in the next block eventually in a future mining cycle. **Your blockchain implementation is required to unconditionally propagate any received transactions to some of its peers**, regardless its internal state.

Example request body:
```json
{
    "sender": "0x123456789ABCDEF",
    "receiver": "0xFEDCBA987654321",
	"data": "Some text data for now"
}
```

| JSON field    | Type          | Description                    |
|:------------- |:------------- |:------------------------------ |
| sender        | String        | The id of the sending party.   |
| receiver      | String        | The id of the receiving party. |
| data          | String        | The data being sent.           |

### `/blocks [GET]`

Serves all blocks starting with the block with the given index. If no index is provided all blocks are served. If no block is found with a matching index (it hasn't been propagated to us yet), an empty array is served. More on the `block` data structure in part 2.

| Query param   | Type          | Description                                                  |
|:------------- |:------------- |:------------------------------------------------------------ |
| index         | Integer       | The value of the "index" field of the first block to serve   |

Example response body:
```
[
	{
		"index": 13,
		...
	},
	{
		"index": 14,
		...
	},
	{
		"index": 15,
		...
	}
]
```

### `/blocks [POST]`

When a node has mined a new block it needs to immediately propagate it to some of it's peers in the network through this endpoint. All nodes receiving a block through this endpoint **must continue to propagate the block**.

After that the node must validate the block and append it to its own version of the blockchain. If there is a gap between the last block's index and the index of the new block, the node needs to request any missing nodes from it's peers (see the **/blocks [GET]** section above). More on the block data structure in part 2.

Example request body:
```json
{
	"index": 16,
	...
}
```

### `/nodes [GET]`

Serves all peers of this node. This endpoint is intended for debugging purposes.

### `/nodes [POST]`

This is the "handshake" endpoint between nodes. Node A calls this endpoint on node B. Node B then adds node A to it's list and responds to the request with some of its own peers. Then node B calls this endpoint on node A, which will respond with some of its peers, which in turn are collected by node B.

Example request body:
```json
{
    "address": "http://192.168.1.2:5050"
}
```

Example response body:
```json
[
	"http://192.168.1.12:5050",
	"http://192.168.1.34.9000"
]
```

### `/nodes [DELETE]`

Unregisters a peer node. For the sake of completeness.


## PART 2: The Blockchain Protocol

Below the blockchain protocol is defined. Any clients must comply to it in their implementations.

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

