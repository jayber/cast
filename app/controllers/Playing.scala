package controllers

import play.api.libs.json.{JsString, Writes, JsValue, Json}

object Playing {
  val TICK_ENERGY = Tweakables.TICK_ENERGY
  val END_SCORE = Tweakables.END_SCORE

  def apply(player1: Player, player2: Player): Playing = {
    val playingPlayers = Map(player1.name -> PlayingPlayer(player1, Seq(None, None, None, None), 0),
      player2.name -> PlayingPlayer(player2, Seq(None, None, None, None), 0))
    Playing(playingPlayers)
  }

  implicit val optionWrites = new Writes[Option[String]] {
    override def writes(o: Option[String]): JsValue = JsString(o.getOrElse(""))
  }

  implicit val playingWrites = new Writes[Playing] {
    override def writes(playing: Playing): JsValue = Json.obj("playing" -> playing.playingPlayers.mapValues {
      player => Json.obj("score" -> player.score, "spells" -> player.spellBuffer)
    })
  }
}

case class Playing(playingPlayers: Map[String, PlayingPlayer]) extends State {

  def castSpell(spell: String, name: String): State = {
    val updatedPlayer = playingPlayers(name).cast(spell)

    if (playingPlayers(name) != updatedPlayer) {
      val playing = Playing(playingPlayers + (updatedPlayer.name -> updatedPlayer))
      playing.tellPlayers()
      playing
    } else this
  }

  def tick(): State = {
    val newPlayingPlayersMap = scoreHits(purgeCounterSpells(playingPlayers))
    val finalPlayingPlayersMap = newPlayingPlayersMap.mapValues(_.advanceBuffer).mapValues(_.advanceScore)
    
    if (finalPlayingPlayersMap != playingPlayers) {
      val playing = Playing(finalPlayingPlayersMap)
      playing.tellPlayers()
      checkGameOver(playing)
    } else {
      this
    }
  }

  def purgeCounterSpells(playingPlayers: Map[String, PlayingPlayer]): Map[String, PlayingPlayer] = {
    val values = playingPlayers.values
    val firstPlayer = values.head
    val secondPlayer = values.last

    def removeFirstMatches(bufferOne: Seq[Option[String]], bufferTwo: Seq[Option[String]]): (Seq[Option[String]], Seq[Option[String]]) = {
      bufferOne(0) match {
        case None => (bufferOne, bufferTwo)
        case value => if (bufferTwo.contains(value)) {
          (bufferOne.updated(0, None), bufferTwo.updated(bufferTwo.indexOf(value), None))
        } else (bufferOne, bufferTwo)
      }
    }
    val buffers = removeFirstMatches(firstPlayer.spellBuffer, secondPlayer.spellBuffer)
    val (bufferTwo, bufferOne) = removeFirstMatches(buffers._2, buffers._1)

    Map(firstPlayer.name -> firstPlayer.copy(spellBuffer = bufferOne), secondPlayer.name -> secondPlayer.copy(spellBuffer = bufferTwo))
  }

  def scoreHits(playingPlayers: Map[String, PlayingPlayer]) = {
    def scorePlayer(player1Buffer: Seq[Option[String]], player2Score: Int, name: String): Int = {
      player1Buffer.last match {
        case Some(spell) =>
          GameActor.tellPlayers(Json.obj("strike"->name), playingPlayers.values.head.player, playingPlayers.values.last.player)
          player2Score + scoreSpell(spell)
        case _ => player2Score
      }
    }
    val values = playingPlayers.values
    val firstPlayer = values.head
    val secondPlayer = values.last
    //remember: player1's spells affect player2 and vice versa
    Map(firstPlayer.name -> firstPlayer.copy(score = scorePlayer(secondPlayer.spellBuffer, firstPlayer.score, firstPlayer.name)),
      secondPlayer.name -> secondPlayer.copy(score = scorePlayer(firstPlayer.spellBuffer, secondPlayer.score, secondPlayer.name)))
  }

  def scoreSpell(spell: String): Int = {
    spell match {
      case "BB" => 3
      case _ => 1
    }
  }

  def checkGameOver(playing: Playing) = {
    val sorted = playing.playingPlayers.values.toList.sortBy { entry =>
      entry.score
    }

    if (sorted.last.score >= Playing.END_SCORE) {
      val result = GameOver(sorted.head, sorted.last)
      result.tellPlayers()
      result
    } else playing
  }


  def tellPlayers() {
    GameActor.tellPlayers(Json.toJson(this), playingPlayers.values.head.player, playingPlayers.values.last.player)
  }
}