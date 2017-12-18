# district-server-smart-contracts

Clojurescript-node.js [mount](https://github.com/tolitius/mount) component for a district server, that takes care of smart-contracts loading, deployment, function calling and event handling.

## Installation
Add `[district0x/district-server-smart-contracts "1.0.0"]` into your project.clj  
Include `[district.server.smart-contracts]` in your CLJS file, where you use `mount/start`

**Warning:** district0x components are still in early stages, therefore API can change in a future.

## Real-world example
To see how district server components play together in real-world app, you can take a look at [NameBazaar server folder](https://github.com/district0x/name-bazaar/tree/master/src/name_bazaar/server), 
where this is deployed in production.

## Usage
You can pass following args to smart-contracts component: 
* `:contracts-build-path` Path to your compiled smart contracts, where you have .bin and .abi files. (default: `"<cwd>/resources/public/contracts/build/"`)
* `:contracts-var` Var of smart-contracts map in namespace where you want to store addresses of deployed smart-contracts
* `:print-gas-usage?` If true, will print gas usage after each contract deployment or state function call. Useful for development. 
* `:auto-mining?` Pass true if you're using [ganache](https://github.com/trufflesuite/ganache) in auto-mining mode. This is for internal optimisations, may not be needed in future. 

Since every time we deploy a smart-contract, it has different address, we need way to store it in a file, so both server and UI can access it, even after restart. For this purpose, we create a namespace containing only smart-contract names and addresses, that will be modified automatically by this component. For example: 
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
                         :print-gas-usage? true
                         :auto-mining? true}})
  (mount/start))

(contracts/contract-address :my-contract)
;; => "0x0000000000000000000000000000000000000000"

(contracts/deploy-smart-contract! :my-contract)
;; (prints) :my-contract 0x575262e80edf7d4b39d95422f86195eb4c21bb52 1,234,435

(contracts/contract-address :my-contract)
;; => "0x575262e80edf7d4b39d95422f86195eb4c21bb52"

(contracts/contract-call :my-contract :my-plus-function 2 3)
;; => 5

;; The component uses just cljs-web3 under the hood, so this is equivalent to the line above
(web3-eth/contract-call (contracts/instance :my-contract) :my-plus-function 2 3)
;; => 5

;; Persist newly deplyed contract addresses into my-district.smart-contracts namespace
(contracts/write-smart-contracts!)
;; (Writes into my-district.smart-contracts, figwheel reloads the file)
```
Next time you'd start the program, `:my-contract` contract would be loaded with newly deployed address.

## Component dependencies

### [district-server-config](https://github.com/district0x/district-server-config)
`district-server-smart-contracts` gets initial args from config provided by `district-server-config/config` under the key `:smart-contracts`. These args are then merged together with ones passed to `mount/with-args`.

### [district-server-web3](https://github.com/district0x/district-server-web3)
`district-server-smart-contracts` relies on getting [web3](https://github.com/ethereum/web3.js) instance from `district-server-web3/web3`. That's why, in example, you need to set up `:web3` in `mount/with-args` as well.

If you wish to use custom components instead of dependencies above while still using `district-server-smart-contracts`, you can easily do so by [mount's states swapping](https://github.com/tolitius/mount#swapping-states-with-states).

## district-server-smart-contracts
Namespace contains following functions for working with smart-contrats:
#### `contract-address [contract-key]`
Returns contract's address

#### `contract-name [contract-key]`
Returns contract's name. E.g "MyContract"

#### `contract-abi [contract-key]`
Returns contract's [ABI](https://github.com/ethereum/wiki/wiki/Ethereum-Contract-ABI)

#### `contract-bin [contract-key]`
Returns contract's bin

#### `instance [contract-key & [contract-address]]`
Returns contract's instance. If provided address, it will create instance related to given address

#### `contract-call [contract-key method & args]`
Same as you call [cljs-web3](https://github.com/district0x/cljs-web3) contract-call function, except for contract-key it's enough to pass just keyword (e.g `:my-contract`) or tuple, if you want to do it at specific address   
(e.g `[:my-contract "0x575262e80edf7d4b39d95422f86195eb4c21bb52"]`)

#### `deploy-smart-contract! [contract-key opts]`
Deploys contract to the blockchain. Returns contract object and also stores new address in internal state.   
`opts:`
* `:arguments` Arguments passed to a contract constructor
* `:gas` Gas limit, default 4M
* `:from` From address, default first address from your accounts
* `:placeholder-replacements` a map containing replacements for [library placeholders](http://solidity.readthedocs.io/en/develop/contracts.html#libraries) in contract's binary
```clojure
(def replacements
  ;; Key can be pretty much any string
  ;; Value can be contract-key or address
  {"beefbeefbeefbeefbeefbeefbeefbeefbeefbeef" :my-other-contract
   "__Set________Set________Set________Set__" "0x575262e80edf7d4b39d95422f86195eb4c21bb52"})
```

#### `contract-event-in-tx [tx-hash contract-key event-name & args]`
Will return first contract event with name `event-name` that occured during execution of transaction with hash `tx-hash`. This is useful, when you look for data in specific event after doing some transaction. For example in tests, or mock data generating. Advantage is that this function is synchronous, compared to setting up event filter with web3. 
```clojure
(let [opts {:gas 200000 :from some-address}
      tx-hash (contracts/contract-call :my-contract :fn-that-fires-event)]
  (contracts/contract-event-in-tx tx-hash :my-contract :TheEvent))
  ;; {:block-number 12 :args {:a 1 :b 2} :event "TheEvent" ... }
```

#### `replay-past-events [event-filter callback opts]`
Reruns all past events and calls callback for each one. This is similiar to what you do with normal web3 event filter, but with this one you can slow down rate at which callbacks are fired. 
Helps in case you have large number of events with slow callbacks, to prevent unresponsive app. 
```clojure
(-> (contracts/contract-call :my-contract :on-some-event {:from-block 0})
  (replay-past-events on-some-event {:delay 10})) ;; in ms
```


