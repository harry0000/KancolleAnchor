package com.harry0000.kancolle.ac

import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, StandardOpenOption}

object Main {

  def main(args: Array[String]): Unit = {
    val path = Config.distPath
    val areas = PlaceCrawler.crawl()

    writeCSV(path, "place.csv", areas)
    writeJSON(path, "place.json", areas)
  }

  val headers = Seq("area", "prefecture_code", "prefecture_name", "place_name", "place_address")

  def writeCSV(path: String, name: String, areas: Seq[Area]): Unit = {
    import com.github.tototoshi.csv.CSVWriter

    val writer = CSVWriter.open(new File(path, name))
    try {
      writer.writeRow(headers)

      for {
        area  <- areas
        pref  <- area.prefectures
        place <- pref.places
      } {
        writer.writeRow(Seq(area.name, pref.code, pref.name, place.name, place.address))
      }
    } finally {
      writer.close()
    }
  }

  def writeJSON(path: String, name: String, areas: Seq[Area]): Unit = {
    import spray.json._
    import PlaceCrawler._

    val writer = Files.newBufferedWriter(new File(path, name).toPath, UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    try {
      writer.write(areas.toJson.prettyPrint)
    } finally {
      writer.close()
    }
  }
}
