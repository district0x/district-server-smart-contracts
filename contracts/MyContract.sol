pragma solidity ^0.4.18;

contract MyContract {

  address public target = 0xBEeFbeefbEefbeEFbeEfbEEfBEeFbeEfBeEfBeef;
  uint public counter;

  event onCounterIncremented(uint theCounter);
  event onSpecialEvent(uint someParam);

  function MyContract(uint _counter) {
    counter = _counter;
  }

  function myPlus(uint a, uint b) constant returns (uint) {
    return a + b;
  }

  function setCounter(uint i) {
    counter = i;
  }

  function incrementCounter(uint i) {
    counter += i;
    onCounterIncremented(counter);
  }

  function doubleIncrementCounter(uint i) {
    incrementCounter(i);
    incrementCounter(i);
  }

  function fireSpecialEvent(uint someParam) {
    onSpecialEvent(someParam);
  }
}
