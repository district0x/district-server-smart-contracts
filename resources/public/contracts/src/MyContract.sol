pragma solidity ^0.4.18;

contract MyContract {

  uint public counter;

  event onCounterIncremented(uint counter);

  function MyContract(uint _counter) {
    counter = _counter;
  }

  function myPlus(uint a, uint b) constant returns (uint) {
    return a + b;
  }

  function incrementCounter(uint i) {
    counter += i;
    onCounterIncremented(counter);
  }
}
