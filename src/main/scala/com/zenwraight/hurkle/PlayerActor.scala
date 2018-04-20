package com.zenwraight.hurkle

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorSelection}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

object PlayerActor {
  // the message to send when providing a guess for position
  case class Guess(x: Int, y: Int)

  // the message to send when user wants to play another round
  case class Restart()

  // the message to send when player wants to leave a game
  case class Leave()
}

class PlayerActor extends Actor {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(FiniteDuration(1, TimeUnit.SECONDS))
  def getGameActorSelection: ActorSelection = context.actorSelection(s"/user/$GAME_ACTOR")

  def askAndThen[T](ask: Future[T])(then: T => Any) = {
    context.become(idle)

    ask onComplete {
      case Success(t) => then(t)
      case Failure(t: Throwable) => stopWithError(t)
    }
  }

  // ask for player's guess and act accordingly
  def askForGuess = {
    println(s"Provide x and y co-ordinate separated by comma")

    askAndThen(Utils.readNumericResponse) {
      case (Some(x: Int), Some(y: Int)) => {
        getGameActorSelection.resolveOne().map { actorRef =>
          actorRef ! PlayerActor.Guess(x,y)
          context.become(waitingForRoundResult)
        }
      }
      case (None, None) => stop
      case (None, Some(_)) => stop
      case (Some(_), None) => stop
    }
  }

  // ask the player if they'd like to take another try
  def askForRetry(hint: String) = {
    println("That's not correct, Try again? (Y/n)")
    println(s"Hint: Go $hint")

    askAndThen(Utils.readBooleanResponse) {
      case true => askForGuess
      case false => stop
    }
  }

  // ask for player if they would like to restart the game for another round
  def askForRestartOnWin = {
    println("You Win! Play another game? (y/n)")

    askAndThen(Utils.readBooleanResponse) {
      case true => {
        getGameActorSelection.resolveOne().map { actorRef =>
          actorRef ! PlayerActor.Restart
          context.become(initializing)
        }
      }
      case false => stop
    }
  }

  // ask for player if they would like to restart the game for another round
  def askForRestartOnLoose = {
    println("You Loose! Play another game? (y/n)")

    askAndThen(Utils.readBooleanResponse) {
      case true => {
        getGameActorSelection.resolveOne().map { actorRef =>
          actorRef ! PlayerActor.Restart
          context.become(initializing)
        }
      }
      case false => stop
    }
  }

  def stop: Unit = {
    println(s"Goodbye!")
    getGameActorSelection.resolveOne().map { actorRef =>
      actorRef ! PlayerActor.Leave
      context.stop(self)
    }
  }

  def stopWithError(t: Throwable) = {
    System.err.println(s"An error has occurred while reading the user's input $t")
    getGameActorSelection.resolveOne().map { actorRef =>
      actorRef ! PlayerActor.Leave
      context.stop(self)
    }
  }

  def receive = initializing

  def idle = Actor.emptyBehavior

  def initializing: Receive = {
    case GameActor.Ready => askForGuess
  }

  def waitingForRoundResult: Receive = {
    case GameActor.Win => askForRestartOnWin
    case GameActor.Loose => askForRestartOnLoose
    case GameActor.TryAgain(hint: String) => askForRetry(hint)
  }
}