# district-server-smart-contracts

[![CircleCI](https://circleci.com/gh/district0x/district-server-smart-contracts.svg?style=svg)](https://circleci.com/gh/district0x/district-server-smart-contracts)

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
  - [create-event-filter](#create-event-filter)
  - [deploy-smart-contract!](#deploy-smart-contract!)
  - [write-smart-contracts!](#write-smart-contracts!)
  - [wait-for-block](#wait-for-block)
  - [contract-event-in-tx](#contract-event-in-tx)
  - [contract-events-in-tx](#contract-events-in-tx)
  - [replay-past-events](#replay-past-events)
  - [replay-past-events-in-order](#replay-past-events-in-order)

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
    :address "0x0000000000000000000000000000000000000000"}
   :my-contract-fwd ;; If you're using forwarder smart contract, define :forwards-to with key of a contract forwarded to
   {:name "Forwarder"
    :address "0x0000000000000000000000000000000000000000"
    :forwards-to :my-contract}})
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

#### <a name="contract-call">`contract-call [contract method args opts]`
Convenient wrapper around [cljs-web3](https://github.com/district0x/cljs-web3) contract-call function. <br>
*Note* : This function needs an unlocked account for signing the transaction! <br>
* `contract` can be one of:
  * keyword (e.g `:my-contract`)
  * keyword of forwarder (e.g `my-contract-fwd`): If you defined `:forwards-to` in your smart-contracts definition, you can just use the key of
  forwarder and it'll know, that it should use ABI of contract in `:forwards-to`.
  * tuple: keyword + address, for contract at specific address (e.g `[:my-contract "0x575262e80edf7d4b39d95422f86195eb4c21bb52"]`)
  * tuple: keyword + keyword, to use ABI from first contract and address from second contract   (e.g `[:my-contract :my-other-contract]`)
* `method` : keyword for the method name e.g. `:my-method`
* `args` : a vector of arguments for `method` (optional)
* `opts:` map of options passed as message data (optional), possible keys include:
  * `:gas` Gas limit, default 4M
  * `:from` From address, defaults to first address from your accounts
  * `:ignore-forward?` Will ignore if contract has property `:forwards-to` and will use ABI of a forwarder

Returns a Promise which resolves to the transaction-hash in the case of state-altering transactions or response in case of retrieve transactions.

#### <a name="create-event-filter">`create-event-filter [contract event filter-opts opts on-event]`
Installs an event filter. <br>
<br>
* `contract` can be one of:
  * keyword (e.g `:my-contract`)
  * tuple: keyword + address, for contract at specific address (e.g `[:my-contract "0x575262e80edf7d4b39d95422f86195eb4c21bb52"]`)
  * tuple: keyword + keyword, to use ABI from first contract and address from second contract   (e.g `[:my-contract :my-other-contract]`)
* `event` : :camel_case keyword for the event name e.g. `:my-event`
* `filter-opts` : map of indexed return values you want to filter the logs by (see [web3 documentation](https://github.com/ethereum/wiki/wiki/JavaScript-API#contract-events) for additional details").
* `opts` : specifies additional filter options, can be one of:
   * "latest" to specify that only new observed events should be processed.
   * map `{:from-block 0 :to-block 100}` specifying earliest and latest block, on which the event handler should fire.
* `on-event` : event handler function. <br>
Returns event filter.

#### <a name="deploy-smart-contract!">`deploy-smart-contract! [contract-key args opts]`
Deploys contract to the blockchain. Returns contract object and also stores new address in internal state. <br>
*Note* : This function needs an unlocked account for signing the transaction! <br>
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

#### <a name="wait-for-block">`wait-for-block [block-number callback]`
Function blocks until block with the specified number is mined. Takes a nodejs-style callback as a second parameter.

#### <a name="contract-event-in-tx">`contract-event-in-tx [tx-hash contract-key event-name & args]`
Will return first contract event with name `event-name` that occured during execution of transaction with hash `tx-hash`. This is useful, when you look for data in specific event after doing some transaction. For example in tests, or mock data generating. Advantage is that this function is synchronous, compared to setting up event filter with web3.
```clojure
(let [opts {:gas 200000 :from some-address}
      tx-hash (contracts/contract-call :my-contract :fn-that-fires-event)]
  (contracts/contract-event-in-tx tx-hash :my-contract :TheEvent))
  ;; {:block-number 12 :args {:a 1 :b 2} :event "TheEvent" ... }
```

#### <a name="contract-events-in-tx">`contract-events-in-tx [tx-hash contract-key event-name & args]`
The same as `contract-event-in-tx` but instead of first event, returns collection of all events with name `event-name`.

#### <a name="replay-past-events">`replay-past-events [event-filter callback opts]`
Reruns all past events and calls callback for each one. This is similiar to what you do with normal web3 event filter, but with this one you can slow down rate at which callbacks are fired.
Helps in case you have large number of events with slow callbacks, to prevent unresponsive app.
Opts you can pass:
* `:delay` - To put delay in between callbacks in ms
* `:transform-fn` - Function to transform collection of events
* `:on-finish` - Will be called after calling callback for all events


```clojure
(-> (contracts/create-event-filter :my-contract :on-some-event {} {:from-block 0})
  (replay-past-events on-some-event {:delay 10})) ;; in ms
```

#### <a name="replay-past-events-in-order">`replay-past-events-in-order [event-filters callback opts]`
Given a collection of filters get all past events from the filters, sorts them by :block-number :transaction-index :log-index and calls callback for each one in order.
Event passed into callback contains `:contract` and `:event` keys, to easily identify the event.
If callback function returns a JS/Promise it will block until executed.
**NOTE** there is no built-in error handling, so the callback needs to handle promise rejections on it's own.

Opts you can pass:
* `:transform-fn` - Function to transform collection of sorted events
* `:on-finish` - Will be called after calling callback for all events
* `:from-block` - Only download and replay past events starting from this block
* `:to-block` - Only download and replay past to this block
* `:block-step` - Blocks numbered `:from-block` until `:to-block` will be requested in equal chunks of size block-step to avoid sending too big of a request to the node.
* `:skip-log-indexes` - A set of tuples like [tx log-index]. Logs in `:from-block` block with this [tx log-index] will be skipped


```clojure
(contracts/replay-past-events-in-order
   [(contracts/create-event-filter :my-contract :on-special-event {} {:from-block 0 :to-block "latest"})
    (contracts/create-event-filter :my-contract :on-counter-incremented {} {:from-block 0 :to-block "latest"})]
   (fn [err evt]
    (println "Contract: " (:name (:contract evt)) ", event: " (:event evt) ", args: " (:args evt)))
   {:on-finish (fn []
                 (println "Finished calling callbacks"))})
```

## Development
```bash
# Setup
lein deps
docker run --name=ganache -p 8545:8545 trufflesuite/ganache-cli:v6.12.1 -p 8545

# To run tests
truffle migrate --network ganache --reset
lein doo node "nodejs-tests" once
```
