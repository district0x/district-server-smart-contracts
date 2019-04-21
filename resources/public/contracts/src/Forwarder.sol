pragma solidity ^0.4.18;

import "DelegateProxy.sol";

/**
 * @title Forwarder proxy contract with editable target
 */

contract Forwarder is DelegateProxy {

  address public target = 0xBEeFbeefbEefbeEFbeEfbEEfBEeFbeEfBeEfBeef; // checksumed to silence warning

  /**
   * @dev Replaces targer forwarder contract is pointing to
   * Only authenticated user can replace target

   * @param _target New target to proxy into
  */
  function setTarget(address _target) public {
    target = _target;
  }

  function() payable {
    delegatedFwd(target, msg.data);
  }

}