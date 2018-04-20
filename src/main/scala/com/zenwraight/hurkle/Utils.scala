package com.zenwraight.hurkle

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

object Utils {

  def readResponse: Future[String] = Future {
    scala.io.StdIn.readLine()
  }

  // try read string having digits between 0 to 9 which are comma separated
  def readNumericResponse: Future[(Option[Int], Option[Int])] = {
    readResponse.map(s => {
      try {
        (Some(s.split(',').head.toInt), (Some(s.split(',').last.toInt)))
      } catch {
        case _: Throwable => (None, None)
      }
    })
  }

  // convert a yes/no response to boolean for easier use
  def readBooleanResponse: Future[Boolean] = {
    readResponse.map(s => s match {
      case "y" | "yes" | "1" | "" => true
      case _ => false
    })
  }

}