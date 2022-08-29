(ns tests.smart-contracts-test)

(def smart-contracts
  {:my-contract {:name "MyContract" :address "0xd345871874d90183DDc82639b5A2cA1bb39E41f5"} :my-contract-fwd {:name "Forwarder" :address "0x50e6B489CE4180667680F1F4aD0f644d6b1a7026" :forwards-to :my-contract}})
