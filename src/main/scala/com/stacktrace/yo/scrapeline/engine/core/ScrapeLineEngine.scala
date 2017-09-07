package com.stacktrace.yo.scrapeline.engine.core

import akka.actor.{ActorRef, Props}
import com.stacktrace.yo.scrapeline.engine.ScrapelineDefinition
import com.stacktrace.yo.scrapeline.engine.core.EngineProtocol.{Begin, Read, Scrape}
import com.stacktrace.yo.scrapeline.engine.scrape.ScrapeProtocol.{ScrapeUrl, ScrapedContentCallBack}
import com.stacktrace.yo.scrapeline.engine.scrape.{ScrapeEngine, ScrapeSupervisor}

/**
  * Created by Stacktraceyo on 9/6/17.
  */
class ScrapeLineEngine(scrapeline: ScrapelineDefinition) extends Engine with ScrapeEngine {


  //  override val fileSourceSupervisor: ActorRef = context.actorOf(Props(new FileSourceSupervisor(this)))
  override val scrapeSupervisor: ActorRef = context.actorOf(Props(new ScrapeSupervisor(this)))

  override def receive: PartialFunction[Any, Unit] = {

    case Begin() =>
      scrapeline.start.foreach {
        case Scrape(url) =>
          log.info("Scraping Url: {}", url)
          self ! ScrapeUrl(url, scrapeline.beginScrape)
        case Read(url) =>
          log.info("Reading Url: {}", url)
      }
    case msg@ScrapeUrl(url: String, callback: ScrapedContentCallBack) =>
      scrapeSupervisor ! msg

  }


}
