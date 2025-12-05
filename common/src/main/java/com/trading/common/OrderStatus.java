package com.trading.common;

/**
 * Order status representing the lifecycle of an order through the system.
 *
 * <ul>
 *   <li>PENDING: Order received by Order Service, saved to database
 *   <li>SUBMITTED: Order acknowledged by M7 (trading engine)
 *   <li>FILLED: Order successfully executed by M7
 *   <li>REJECTED: Order rejected by M7
 * </ul>
 */
public enum OrderStatus {
  PENDING,
  SUBMITTED,
  FILLED,
  REJECTED
}
