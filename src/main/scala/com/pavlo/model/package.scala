/*
 * For some reason docker:publishLocal fails due to missing license
 */

package com.pavlo

package object model {
  type ErrorOr[V] = Either[ElementError, V]
}
