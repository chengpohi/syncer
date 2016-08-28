package com.github.chengpohi.model

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */

case class Record(name: String, md5: String, status: Option[String] = Some("exist"))
