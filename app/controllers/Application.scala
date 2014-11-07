package controllers

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, Props}
import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.concurrent.Akka
import play.api.Play.current

object Application extends Controller {

  val user_key: String = "user"
  val opponent_key: String = "opponent"

  def index = Action { request =>
    Ok(views.html.template(views.html.main()))
  }

  def chooseOpponent = Action {request =>
    val user = request.getQueryString(user_key).get
    Ok(views.html.template(views.html.chooseOpponent())).withSession(user_key -> user)
  }

  def playGame = Action { request =>
    val user = request.getQueryString(user_key).get
    val opponent = request.getQueryString(opponent_key).get
    Ok(views.html.template(views.html.playGame(user,opponent))).withSession(user_key -> user,opponent_key -> opponent)
  }
}
