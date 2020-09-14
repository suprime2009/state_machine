/*
 * For some reason docker:publishLocal fails due to missing license
 */

package com.pavlo.db

case class DbState(id: Int, name: String)

case class DbEntity(id: Int, name: String, stateId: Int)

case class DbTransition(id: Int, fromState: Int, toState: Int)

case class DbOperation(transitionId: Int, entityId: Int, from: String, to: String)
