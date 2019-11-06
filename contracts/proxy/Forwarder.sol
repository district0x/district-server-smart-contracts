pragma solidity ^0.4.18;

import "../proxy/DelegateProxy.sol";

contract Forwarder is DelegateProxy {

  // After compiling contract, `beefbeef...` is replaced in the bytecode by the real target address
  address public target = 0xBEeFbeefbEefbeEFbeEfbEEfBEeFbeEfBeEfBeef; // checksumed to silence warning

  /**
   * @dev Replaces targer forwarder contract is pointing to
   * Only authenticated user can replace target
   * @param _target New target to proxy into
   */
  function setTarget(address _target) public {
    target = _target;
  }

  /*
   * @dev Forwards all calls to target
   */
  function() payable {
    delegatedFwd(target, msg.data);
  }
}
