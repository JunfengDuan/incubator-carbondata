/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.carbondata.examples.util

import java.io.File

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{CarbonContext, SaveMode}

import org.apache.carbondata.core.util.CarbonProperties

// scalastyle:off println

object ExampleUtils {

  def currentPath: String = new File(this.getClass.getResource("/").getPath + "/../../")
    .getCanonicalPath
  val storeLocation = currentPath + "/target/store"
  val kettleHome = new File(currentPath + "/../processing/carbonplugins").getCanonicalPath

  def createCarbonContext(appName: String): CarbonContext = {
    val sc = new SparkContext(new SparkConf()
          .setAppName(appName)
          .setMaster("local[2]"))
    sc.setLogLevel("ERROR")

    println(s"Starting $appName using spark version ${sc.version}")

    val cc = new CarbonContext(sc, storeLocation, currentPath + "/target/carbonmetastore")
    cc.setConf("carbon.kettle.home", kettleHome)

    // whether use table split partition
    // true -> use table split partition, support multiple partition loading
    // false -> use node split partition, support data load by host partition
    CarbonProperties.getInstance().addProperty("carbon.table.split.partition.enable", "false")
    cc
  }

  /**
   * This func will write a sample CarbonData file containing following schema:
   * c1: String, c2: String, c3: Double
   */
  def writeSampleCarbonFile(cc: CarbonContext, tableName: String, numRows: Int = 1000): Unit = {
    cc.sql(s"DROP TABLE IF EXISTS $tableName")
    writeDataframe(cc, tableName, numRows, SaveMode.Overwrite)
  }

  /**
   * This func will append data to the CarbonData file
   */
  def appendSampleCarbonFile(cc: CarbonContext, tableName: String, numRows: Int = 1000): Unit = {
    writeDataframe(cc, tableName, numRows, SaveMode.Append)
  }

  /**
   * create a new dataframe and write to CarbonData file, based on save mode
   */
  private def writeDataframe(
      cc: CarbonContext, tableName: String, numRows: Int, mode: SaveMode): Unit = {
    // use CarbonContext to write CarbonData files
    import cc.implicits._
    val sc = cc.sparkContext
    val df = sc.parallelize(1 to numRows, 2)
        .map(x => ("a", "b", x))
        .toDF("c1", "c2", "c3")

    // save dataframe to carbon file
    df.write
        .format("carbondata")
        .option("tableName", tableName)
        .option("compress", "true")
        .option("useKettle", "false")
        .mode(mode)
        .save()
  }
}
// scalastyle:on println

