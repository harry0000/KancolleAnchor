package com.harry0000.kancolle.ac

import com.typesafe.config.ConfigFactory

object Config {
  private lazy val config = ConfigFactory.load()

  def distPath = config.getString("dist.path")
}
