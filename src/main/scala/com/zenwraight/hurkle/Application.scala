package com.zenwraight.hurkle

import akka.actor.{ActorSystem, Props}

object Application extends App {

  val system = ActorSystem(ACTOR_SYSTEM_NAME)

  val gameActor = system.actorOf(Props(new GameActor()), name = GAME_ACTOR)
  val playerActor = system.actorOf(Props(new PlayerActor()), name = PLAYER_ACTOR)
}