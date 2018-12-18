# district-server-smart-contracts

[![Build Status](https://travis-ci.org/district0x/district-server-smart-contracts.svg?branch=master)](https://travis-ci.org/district0x/district-server-smart-contracts)

Clojurescript-node.js [mount](https://github.com/tolitius/mount) module for a district server, that takes care of smart-contracts loading, deployment, function calling and event handling.

## Installation
Latest released version of this library: <br>
[![Clojars Project](https://img.shields.io/clojars/v/district0x/district-server-smart-contracts.svg)](https://clojars.org/district0x/district-server-smart-contracts)

Include `[district.server.smart-contracts]` in your CLJS file, where you use `mount/start`

## API Overview

**Warning:** district0x modules are still in early stages, therefore API can change in a future.

- [district-server-smart-contracts](#districtserversmart-contracts)
  - [contract-address](#contract-address)
  - [contract-name](#contract-name)
  - [contract-abi](#contract-abi)
  - [contract-bin](#contract-bin)
  - [instance](#instance)
  - [contract-call](#contract-call)
  - [deploy-smart-contract!](#deploy-smart-contract!)
  - [write-smart-contracts!](#write-smart-contracts!)
  - [wait-for-tx-receipt](#wait-for-tx-receipt)
  - [contract-event-in-tx](#contract-event-in-tx)
  - [contract-events-in-tx](#contract-events-in-tx)
  - [replay-past-events](#replay-past-events)

## Real-world example
To see how district server modules play together in real-world app, you can take a look at [NameBazaar server folder](https://github.com/district0x/name-bazaar/tree/master/src/name_bazaar/server), 
where this is deployed in production.

## Usage
You can pass following args to smart-contracts module: 
* `:contracts-build-path` Path to your compiled smart contracts, where you have .bin and .abi files. (default: `"<cwd>/resources/public/contracts/build/"`)
* `:contracts-var` Var of smart-contracts map in namespace where you want to store addresses of deployed smart-contracts
* `:print-gas-usage?` If true, will print gas usage after each contract deployment or state function call. Useful for development. 

Since every time we deploy a smart-contract, it has different address, we need way to store it in a file, so both server and UI can access it, even after restart. For this purpose, we create a namespace containing only smart-contract names and addresses, that will be modified automatically by this module. For example: 
```clojure
(ns my-district.smart-contracts)

(def smart-contracts
  {:my-contract
   {:name "MyContract", ;; By this name .abi and .bin files are loaded
    :address "0x0000000000000000000000000000000000000000"}})
```

That's all that's needed there. Let's see how can snippet using it look like:

```clojure
(ns my-district
  (:require [mount.core :as mount]
            [cljs-web3.eth :as web3-eth]
            [my-district.smart-contracts]
            [district.server.smart-contracts :as contracts]))

(-> (mount/with-args
      {:web3 {:port 8545}
       :smart-contracts {:contracts-var #'my-district.smart-contracts/smart-contracts
                         :print-gas-usage? true}})
    (mount/start))

(contracts/contract-address :my-contract)
;; => "0x0000000000000000000000000000000000000000"

(contracts/deploy-smart-contract! :my-contract)
;; (prints) :my-contract 0x575262e80edf7d4b39d95422f86195eb4c21bb52 1,234,435

(contracts/contract-address :my-contract)
;; => "0x575262e80edf7d4b39d95422f86195eb4c21bb52"

(contracts/contract-call :my-contract :my-plus-function [2 3])
;; => 5

;; The module uses just cljs-web3 under the hood, so this is equivalent to the line above
(web3-eth/contract-call (contracts/instance :my-contract) :my-plus-function 2 3)
;; => 5

;; Persist newly deplyed contract addresses into my-district.smart-contracts namespace
(contracts/write-smart-contracts!)
;; (Writes into my-district.smart-contracts, figwheel reloads the file)
```
Next time you'd start the program, `:my-contract` contract would be loaded with newly deployed address.

## module dependencies

### [district-server-config](https://github.com/district0x/district-server-config)
`district-server-smart-contracts` gets initial args from config provided by `district-server-config/config` under the key `:smart-contracts`. These args are then merged together with ones passed to `mount/with-args`.

### [district-server-web3](https://github.com/district0x/district-server-web3)
`district-server-smart-contracts` relies on getting [web3](https://github.com/ethereum/web3.js) instance from `district-server-web3/web3`. That's why, in example, you need to set up `:web3` in `mount/with-args` as well.

If you wish to use custom modules instead of dependencies above while still using `district-server-smart-contracts`, you can easily do so by [mount's states swapping](https://github.com/tolitius/mount#swapping-states-with-states).

## district.server.smart-contracts
Namespace contains following functions for working with smart-contrats:
#### <a name="contract-address">`contract-address [contract-key]`
Returns contract's address

#### <a name="contract-name">`contract-name [contract-key]`
Returns contract's name. E.g "MyContract"

#### <a name="contract-abi">`contract-abi [contract-key]`
Returns contract's [ABI](https://github.com/ethereum/wiki/wiki/Ethereum-Contract-ABI)

#### <a name="contract-bin">`contract-bin [contract-key]`
Returns contract's bin

#### <a name="instance">`instance [contract-key & [contract-address]]`
Returns contract's instance. If provided address, it will create instance related to given address

#### <a name="contract-call">`contract-call [contract-key method args opts]`
Convenient wrapper around [cljs-web3](https://github.com/district0x/cljs-web3) contract-call function. <br>
<br>
* `contract-key` can be one of: 
  * keyword (e.g `:my-contract`)
  * tuple: keyword + address, for contract at specific address (e.g `[:my-contract "0x575262e80edf7d4b39d95422f86195eb4c21bb52"]`)
  * tuple: keyword + keyword, to use ABI from first contract and address from second contract   (e.g `[:my-contract :my-other-contract]`)
* `method` : keyword for the method name e.g. `:my-method` 
* `args` : a vector of arguments for `method` (optional)
* `opts:` map of options passed as message data (optional), possible keys include:
  * `:gas` Gas limit, default 4M
  * `:from` From address, defaults to first address from your accounts

Returns a Promise which resolves to the transaction-hash in the case of state-altering transactions or response in case of retrieve transactions.
 
#### <a name="deploy-smart-contract!">`deploy-smart-contract! [contract-key args opts]`
Deploys contract to the blockchain. Returns contract object and also stores new address in internal state.   
* `opts:`
  * `:arguments` Arguments passed to a contract constructor
  * `:gas` Gas limit, default 4M
  * `:from` From address, defaults to first address from your accounts
  * `:placeholder-replacements` a map containing replacements for [library placeholders](http://solidity.readthedocs.io/en/develop/contracts.html#libraries) in contract's binary
```clojure
(def replacements
  ;; Key can be pretty much any string
  ;; Value can be contract-key or address
  {"beefbeefbeefbeefbeefbeefbeefbeefbeefbeef" :my-other-contract
   "__Set________Set________Set________Set__" "0x575262e80edf7d4b39d95422f86195eb4c21bb52"})
```

Returns a Promise which resolves to the contracts address.

#### <a name="write-smart-contracts!">`write-smart-contracts! []`
Writes smart-contracts that are currently in module's state into file that was passed to `:contracts-var`.    

#### <a name="wait-for-tx-receipt">`wait-for-tx-receipt [tx-hash]`
Function blocks until transaction is transmitted to the network, returns a Promise which resolves to the receipt. 

#### <a name="contract-event-in-tx">`contract-event-in-tx [tx-hash contract-key event-name & args]`
Will return first contract event with name `event-name` that occured during execution of transaction with hash `tx-hash`. This is useful, when you look for data in specific event after doing some transaction. For example in tests, or mock data generating. Advantage is that this function is synchronous, compared to setting up event filter with web3. 
```clojure
(let [opts {:gas 200000 :from some-address}
      tx-hash (contracts/contract-call :my-contract :fn-that-fires-event)]
  (contracts/contract-event-in-tx tx-hash :my-contract :TheEvent))
  ;; {:block-number 12 :args {:a 1 :b 2} :event "TheEvent" ... }
```

#### <a name="contract-events-in-tx">`contract-events-in-tx [tx-hash contract-key event-name & args]`
The same as `contract-event-in-tx` but instead of first event, returns collection of all events with name `event-name`

#### <a name="replay-past-events">`replay-past-events [event-filter callback opts]`
Reruns all past events and calls callback for each one. This is similiar to what you do with normal web3 event filter, but with this one you can slow down rate at which callbacks are fired. 
Helps in case you have large number of events with slow callbacks, to prevent unresponsive app. 
```clojure
(-> (contracts/contract-call :my-contract :on-some-event {:from-block 0})
  (replay-past-events on-some-event {:delay 10})) ;; in ms
```
## Development
```bash
# To start REPL and run tests
lein deps
lein repl
(start-tests!)

# In other terminal
node tests-compiled/run-tests.js

# To run tests without REPL
lein doo node "tests" once
```


