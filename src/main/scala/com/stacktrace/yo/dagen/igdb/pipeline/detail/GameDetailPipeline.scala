package com.stacktrace.yo.dagen.igdb.pipeline.detail

import java.io.File

import akka.actor.{ActorRef, PoisonPill, Props}
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.stacktrace.yo.dagen.igdb.pipeline.detail.actor.GameDetailSupervisor
import com.stacktrace.yo.dagen.old.core.IGDBAPIClient
import com.stacktrace.yo.dagen.old.core.Protocol._
import com.stacktrace.yo.dagen.old.core.pipeline.PipelineActor
import org.stacktrace.yo.igdb.client.IGDBClient
import org.stacktrace.yo.igdb.model.Game

import scala.io.Source

/**
  * Created by Ahmad on 8/23/2017.
  */
class GameDetailPipeline extends PipelineActor {

  def client: IGDBClient = IGDBAPIClient.getClient

  var controller: ActorRef = _
  var details: Vector[String] = _


  override def receive: PartialFunction[Any, Unit] = {
    case StartPhase() =>
      controller = sender
      if (canSkip) {
        self ! PipelineFinished()
      }
      else {
        val supervisor = context.actorOf(Props(new GameDetailSupervisor(details)))
        supervisor ! StartDelegate(self)
      }
    case PipelineFinished() =>
      controller ! PhaseFinished("GameDetailPipeline")
      self ! PoisonPill
    case Working() =>
      controller ! StartDownstream("GameDetailPipeline")
  }


  override def canSkip: Boolean = {
    val file = new File("games/")
    val files = recursiveListFiles(file)
    var i = 0
    val ids = scala.collection.mutable.HashSet[Long]()
    files.foreach(
      file => {
        val reader = new ObjectMapper()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .registerModule(DefaultScalaModule)
          .readerFor(classOf[Array[Game]])
        val gameList: Array[Game] = reader.readValue[Array[Game]](file)
        gameList.toList.foreach(game => {
          ids += game.getId
          i += 1
        }
        )
      }
    )
    val missing = scala.collection.mutable.ListBuffer[String]()
    if (i != start().size) {
      details.foreach(id => {
        if (!ids.contains(id.toLong)) {
          missing += id
        }
      })
      details = missing.toVector
      false
    } else {
      true
    }
  }

  def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles
    these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }

  private def start(): Vector[String] = {

    val idList = Source.fromFile("gamecombined.txt")
      .getLines()
      .toVector
      .map(each => {
        each.split(",")(0)
      })

    if (new File("games").exists()) {
      log.info("Games Directory Exists..")
    }
    else if (new File("games").mkdir()) {
      log.info("Directory was created successfully")
    }
    else {
      log.info("Failed trying to create the directory")
    }
    details = idList
    idList
  }
}
