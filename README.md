# Basic Blockchain

This project showcases a very basic blockchain implementation. The example implementation is built and run by issuing the below command from the terminal:

    gradle clean run

Alternatively you can specify a specific port for the web server (useful if you want to run two instances on the same machine, simulating two nodes in the network):

    RATPACK_PORT=9000 gradle clean run

And then you're free to call the HTTP endpoints with Postman, Curl or any other way you prefer. The example server runs on `http://localhost:5050` by default.

## The Workshop

Doing the lab yourself you'll write everything from scratch in your preferred programming language, using your favorite tools. The blockchain protocol itself doesn't require neither Java nor Gradle. Choose with your heart.

The lab will focus on a proof-of-work protocol, a bit recklessly also known as "mining". Regarding transaction and block propagation, as well as validation, we'll settle with the most basic implementations possible. In a production implementation these areas would involve some mathematics, which is a bit outside the scope of this lab.

## PART 1: The REST API

Below the blockchain public API is described. You're expected to implement a web server that can respond to the described requests.

### `/transactions [GET]`

Returns all pending transactions which haven't been included in a block yet. This endpoint is for debugging purposes.

### `/transactions [POST]`

Temporarily caches a new transaction on this node. The next "mining"-cycle will include it in the next block. **Your blockchain implementation is required to propagate any received transactions**, but be careful with resonance (imaging two nodes being isolated in a data island for some time...). More on the `transaction` data model in part 2.

Example request body:
```json
{
    "id": "A01B23C45D67E89F",
    "sender": "0x123456789ABCDEF",
    "receiver": "0xFEDCBA987654321",
    "data": "Some text data for now",
    "timestamp": 1527924094
}
```

### `/blocks [GET]`

Serves all blocks starting with the block with the given `index` query parameter. If no index is provided all blocks are served. If no block is found with a matching index (it hasn't been propagated to us yet), an empty array is served. More on the `block` data structure in part 2.

Example response body:
```json
[
    {
        "index": 13,
        "nonce": 17647389,
        "timestamp": 1527924094,
        "prevHash": "123AB456CD789EF",
        "transactions": [
            {
                "id": "A01B23C45D67E89F",
                "sender": "0x123456789ABCDEF",
                "receiver": "0xFEDCBA987654321",
                "data": "Some text data for now",
                "timestamp": 1527924094
            }
        ]
    },
    {
        "index": 14,
        "nonce": 6427135,
        "timestamp": 1527925712,
        "prevHash": "FE987CD654AB321",
        "transactions": [
            {
                "id": "F01E23D45C67B89A",
                "sender": "0x789ABCDEF123456",
                "receiver": "0xA987FEDCB654321",
                "data": "Some other text data for now",
                "timestamp": 1527928697
            }
        ]
    }
]
```

### `/blocks [POST]`

When a node has mined a new block it needs to immediately propagate it to some of it's peers in the network through this endpoint. All nodes receiving a block through this endpoint **must continue to propagate the block**, but beware of resonance (imagine two nodes being isolated in a data island for some time...). It may also be advantegous to abort any ongoing mining.

Before the node appends the new block to its own version of the blockchain, it must validate it. If there is a gap between the last block's index and the index of the new block, the node needs to request any missing nodes from it's peers (see the `/blocks [GET]` section above). If the index of the new block is less than, or equal to the index of the last block, the new block should be discarded.

### `/nodes [GET]`

Serves all peers of this node. This endpoint is intended for debugging purposes.

### `/nodes [POST]`

This is the "handshake" endpoint between nodes. Node A calls this endpoint on node B. Node B then adds node A to it's list of peers and responds to the request with some of its own peers. Then node B asks node A to return the favor by calling this endpoint on node A, which will respond with some of its peers, which node B, of course, will be very eager to collect in its peers list. Avoid duplicates in the peers list.

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

Example request body:
```json
{
    "address": "http://192.168.1.2:5050"
}
```


## PART 2: The Blockchain Protocol

Below the blockchain protocol is defined.

### The Transaction Model

A `Transaction` is the atomic building block and it describes a change of state in the blockchain. All nodes must propagate a received (new) transaction to at least one peer. No node may keep duplicates of any transaction. Any received transactions are temporarilly cached while awaiting mining.

| Form field    | Type          | Description                     |
|:------------- |:------------- |:------------------------------- |
| id            | String        | The id of the transaction. This field is calculated internally as the SHA-256 hash of "{sender}{receiver}{data}{timestamp}" |
| sender        | String        | The id of the sending party. This field is user provided. Null and empty strings are not allowed. No further verification is done |
| receiver      | String        | The id of the receiving party. This field is user provided and treated exactly as the sender field in terms of verification |
| data          | String        | The data being sent. This field is user provided and not verified at all |
| timestamp     | long          | The Unix epoch millisecond precision timestamp for when this transaction was created the first time. This field is generated internally |

### The Block Model

A `Block` is parcel of transactions, sealed for further modifications. Each block has a link to its immediate predecessor in the blockchain. This link is a one-way-hash of said block. This exakt detail is what guarantees the incorruptability in a blockchain. The hash of a block is produced by passing the block header to the SHA-256 algorithm. The block header, in turn, is produced as a concatenated string exactly like so:

    {nonce}{index}{timestamp}{prevHash}{transaction[0].id}{...}{transaction[n].id}

A block must contain at least one transaction.

Once a block is successfully composed, it must be propagated to at least one peer. Once a block is received, as a result of a peer propagation, it must be validated (see `The Validation Algorithm` section below) and if valid it must be appended to the local blockchain. All transactions included in the received block must then be removed from the pending transactions cache.

| Form field    | Type          | Description                     |
|:------------- |:------------- |:------------------------------- |
| nonce         | long          | An arbitrary number that, when hashing the block header, produces a resulting hash string that starts with "000" |
| index         | integer       | The index of the block in the blockchain on the node that mined it |
| timestamp     | long          | The Unix epoch millisecond precision timestamp for when the mining process was started |
| prevHash      | String        | The hash string of the previous block header |
| transactions  | Transaction[] | An ordered list of transactions being included in this block |

### The Peer Propagation Cycle

1. Each node should persist (long term) a list of its known peers.
1. On start-up, each node must register itself to at least one of its peers.
1. Each node must provide a list of at least one of its peer nodes in the registration response body.
1. A node may, but is not required to, add its remote peers ("peers of peers") to persistant storage.

A few tips along the road:
* Communicating with more peers doesn't necessarily give you an advantage over less peers. The more peers you interact with the more time you will be spending synchronizing and verifying. On the other hand, having too few peers makes you vulnerable for getting isolated from the network if your (few) peers go offline. "Lagom" is best.
* You may want to maintain your list of peers and favor those who are reasonably fast to respond over those who provide slower responses. This grooming may be required every now and then as network load and other circumstances may play in for shorter or longer periods of time.

### The Validation Algorithm

When receiving a propagated block verify that:

1. The `index` of the new block is exactly one more than the `index` of the last block in your blockchain.
   1. If there is a **gap** between the indices, you may be missing some blocks and must request them from a (few) peer(s). Note that these blocks will also need to be verified before appending them to your blockchain. Trust no one!
   1. If no peer can provide the missing blocks at this time, the new block is to be considered invalid. Discard it.
   1. If there is an **overlap** in the indices, it may be resonance or a fake block. The new block is to be considered invalid. Discard it.
1. The hash of the new block header (which you'll have to compose and hash locally) starts with "000".
1. The `prevHash` of the new block points to the last block in your blockchain.
1. Finally all transactions in the new block must be tested against the entire blockchain. No single transaction can exist twice in the blockchain, if it does the block is invalid.

