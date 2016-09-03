package com.github.chengpohi.file

import java.io.File

import org.scalatest.{FlatSpec, ShouldMatchers}

/**
  * syncer
  * Created by chengpohi on 9/3/16.
  */
class PathReaderTest extends FlatSpec with ShouldMatchers {
  it should "ls all files with filter function" in {
    val path: String = getClass.getResource("/test/").getPath
    val files = PathReader.apply(path).ls((file: File) => file.getName.endsWith(".txt"))
    files.foreach(f => assert(f.getName.endsWith(".txt")))
  }
}
