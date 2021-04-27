pragma solidity ^0.4.18;

contract MyContract {

  address public target = 0xBEeFbeefbEefbeEFbeEfbEEfBEeFbeEfBeEfBeef;
  uint public counter;

  event onCounterIncremented(uint theCounter);
  event onSpecialEvent(uint indexed param1, uint indexed param2, uint param3, uint param4);

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

  function fireSpecialEvent(uint param1, uint param2, uint param3, uint param4) {
    onSpecialEvent(param1, param2, param3, param4);
  }
}
