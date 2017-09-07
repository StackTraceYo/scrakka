package com.stacktrace.yo.scrapeline.engine

import akka.actor.{ActorSystem, Props}
import com.stacktrace.yo.scrapeline.engine.core.EngineProtocol.Begin
import com.stacktrace.yo.scrapeline.engine.core.ScrapeLineEngine
import com.stacktrace.yo.scrapeline.engine.scrape.ScrapeProtocol.{ScrapeUrl, ScrapedContentCallBack}

import scala.concurrent.ExecutionContextExecutor

/**
  * Created by Stacktraceyo on 9/6/17.
  */
abstract class BaseScrapeLine extends ScrapelineDefinition {

  implicit val as: ActorSystem = ActorSystem("Scrapeline")
  implicit val ec: ExecutionContextExecutor = as.dispatcher

  private val scrapelineEngine = as.actorOf(Props(new ScrapeLineEngine(this)))

  override def begin(): Unit = {
    scrapelineEngine ! Begin()
  }

  override def andThenScrape(url: String, pipe: ScrapedContentCallBack): Unit = {
    scrapelineEngine ! ScrapeUrl(url, pipe)
  }
}