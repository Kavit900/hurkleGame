package com.zenwraight.hurkle

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorSelection}
import akka.util.Timeout

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

object GameActor {
  // message to send when the game is ready to be played
  case class Ready()

  // message to send to a player actor when the guess is correct
  case class Win()

  // message to send to a player actor when the guess is wrong
  case class Loose()

  // message to send to a player actor when the guess is wrong
  case class TryAgain(hint: String)
}

class GameActor extends Actor {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(FiniteDuration(1, TimeUnit.SECONDS))

  var xAns = generatePos
  var yAns = generatePos

  def generatePos: Int = Random.nextInt(10)

  var numTurns = 5

  def getPlayerActorSelection: ActorSelection = context.actorSelection(s"/user/$PLAYER_ACTOR")

  def receive = {
    // the player has provided a guess, check if it's correct and send the appropriate response
    case PlayerActor.Guess(x: Int, y: Int) => {
      getPlayerActorSelection.resolveOne().map { actorRef =>
        //println(s"Guess is $x and $y")
        if (numTurns <= 0) {
          actorRef ! GameActor.Loose
        } else if (xAns == x && yAns == y) {
          actorRef ! GameActor.Win
        } else {
          var hint: String = ""
          if (x > xAns && y > yAns) {
            hint += "SouthWest"
          } else if (x > xAns && y == yAns) {
            hint += "West"
          } else if (x < xAns && y < yAns) {
            hint += "NorthEast"
          } else if (x == xAns && y > yAns) {
            hint += "South"
          } else if (x < xAns && y > yAns) {
            hint += "SouthEast"
          } else if (x < xAns && y == yAns) {
            hint += "East"
          } else if (x > xAns && y < yAns) {
            hint += "NorthWest"
          } else if (x == xAns && y < yAns) {
            hint += "North"
          }
          numTurns -= 1
          actorRef ! GameActor.TryAgain(hint)
        }
      }
    }
    case PlayerActor.Restart => {
      xAns = generatePos
      yAns = generatePos
      getPlayerActorSelection.resolveOne().map { actorRef =>
        actorRef ! GameActor.Ready
      }
    }
    case PlayerActor.Leave => {
      context.system.shutdown
    }
  }

  override def preStart(): Unit = {
    println("The game rules are simple, you will get 5 turns to guess the co-ordinate of Hurkle, The constraints are, the hurkle is present in 10 X 10 grid")
    getPlayerActorSelection.resolveOne().map { actorRef =>
      actorRef ! GameActor.Ready
    }
  }

}