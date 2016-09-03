package com.github.chengpohi.repository

import java.util.Date

import com.github.chengpohi.model.FileItem
import org.scalatest.{FlatSpec, ShouldMatchers}

/**
  * syncer
  * Created by chengpohi on 9/3/16.
  */
class RepositoryServiceTest extends FlatSpec with ShouldMatchers{
  it should "compare diff" in {
    val d = new Date()
    val fs = List(FileItem("run.sh", "123"))
    val sameFs = List(FileItem("run.sh", "123"))
    val result: List[FileItem] = fs.diff(sameFs)
    assert(result === List())
  }
}
