/**
* Copyright 2017 ZuInnoTe (Jörn Franke) <zuinnote@gmail.com>
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
**/

/**
*
* This test checks the data source
*
*/

package org.zuinnote.spark.office.excel


import org.apache.hadoop.hdfs.MiniDFSCluster
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FSDataInputStream
import org.apache.hadoop.fs.Path

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.Files
import java.nio.file.FileVisitResult
import java.nio.file.SimpleFileVisitor
import java.util.ArrayList
import java.util.List


import org.apache.hadoop.io.compress.CodecPool
import org.apache.hadoop.io.compress.CompressionCodec
import org.apache.hadoop.io.compress.CompressionCodecFactory
import org.apache.hadoop.io.compress.Decompressor
import org.apache.hadoop.io.compress.SplittableCompressionCodec
import org.apache.hadoop.io.compress.SplitCompressionInputStream

import org.zuinnote.hadoop.office.format.common.parser.MSExcelParser
import org.zuinnote.hadoop.office.format.common.HadoopOfficeReadConfiguration
import org.zuinnote.hadoop.office.format.common.dao.SpreadSheetCellDAO

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._


import scala.collection.mutable.ArrayBuffer
import org.scalatest.{FlatSpec, BeforeAndAfterAll, GivenWhenThen, Matchers}

class SparkScalaExcelDSSparkMasterIntegrationSpec extends FlatSpec with BeforeAndAfterAll with GivenWhenThen with Matchers {

private var sc: SparkContext = _


private val master: String = "local[2]"
private val appName: String = "example-scalasparkexcelinput-integrationtest"
private val tmpPrefix: String = "ho-integrationtest"
private var tmpPath: java.nio.file.Path = _
private val CLUSTERNAME: String ="hcl-minicluster"
private val DFS_INPUT_DIR_NAME: String = "/input"
private val DFS_OUTPUT_DIR_NAME: String = "/output"
private val DEFAULT_OUTPUT_FILENAME: String = "part-00000"
private val DEFAULT_OUTPUT_EXCEL_FILENAME: String = "part-00000.xlsx"
private val DFS_INPUT_DIR : Path = new Path(DFS_INPUT_DIR_NAME)
private val DFS_OUTPUT_DIR : Path = new Path(DFS_OUTPUT_DIR_NAME)
private val NOOFDATANODES: Int =4
private var dfsCluster: MiniDFSCluster = _
private var conf: Configuration = _
private var openDecompressors = ArrayBuffer[Decompressor]();

override def beforeAll(): Unit = {
    super.beforeAll()

		// Create temporary directory for HDFS base and shutdownhook
	// create temp directory
      tmpPath = Files.createTempDirectory(tmpPrefix)
      // create shutdown hook to remove temp files (=HDFS MiniCluster) after shutdown, may need to rethink to avoid many threads are created
	Runtime.getRuntime.addShutdownHook(new Thread("remove temporary directory") {
      	 override def run(): Unit =  {
        	try {
          		Files.walkFileTree(tmpPath, new SimpleFileVisitor[java.nio.file.Path]() {

            		override def visitFile(file: java.nio.file.Path,attrs: BasicFileAttributes): FileVisitResult = {
                		Files.delete(file)
             			return FileVisitResult.CONTINUE
        			}

        		override def postVisitDirectory(dir: java.nio.file.Path, e: IOException): FileVisitResult = {
          			if (e == null) {
            				Files.delete(dir)
            				return FileVisitResult.CONTINUE
          			}
          			throw e
        			}
        	})
      	} catch {
        case e: IOException => throw new RuntimeException("Error temporary files in following path could not be deleted "+tmpPath, e)
    }}})
	// create DFS mini cluster
	 conf = new Configuration()
	val baseDir = new File(tmpPath.toString()).getAbsoluteFile()
	conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath())
	val builder = new MiniDFSCluster.Builder(conf)
 	 dfsCluster = builder.numDataNodes(NOOFDATANODES).build()
	conf.set("fs.defaultFS", dfsCluster.getFileSystem().getUri().toString())
	// create local Spark cluster
 	val sparkConf = new SparkConf()
      .setMaster("local[2]")
      .setAppName(this.getClass.getSimpleName)
	sc = new SparkContext(sparkConf)
 }




  override def afterAll(): Unit = {
   // close Spark Context
    if (sc!=null) {
	sc.stop()
    }
    // close decompressor
	for ( currentDecompressor <- this.openDecompressors) {
		if (currentDecompressor!=null) {
			 CodecPool.returnDecompressor(currentDecompressor)
		}
 	}
    // close dfs cluster
    dfsCluster.shutdown()
    super.afterAll()
}


"The test excel file" should "be fully read with the input data source" in {
	Given("Excel 2013 test file on DFS")
	// create input directory
	dfsCluster.getFileSystem().mkdirs(DFS_INPUT_DIR)
	// copy test file
	val classLoader = getClass().getClassLoader()
    	// put testdata on DFS
    	val fileName: String="excel2013test.xlsx"
    	val fileNameFullLocal=classLoader.getResource(fileName).getFile()
    	val inputFile=new Path(fileNameFullLocal)
    	dfsCluster.getFileSystem().copyFromLocalFile(false, false, inputFile, DFS_INPUT_DIR)
	When("loaded by Excel data source")
  val sqlContext = new SQLContext(sc)
  val df = sqlContext.read.format("org.zuinnote.spark.office.excel").option("read.locale.bcp47", "de").load(dfsCluster.getFileSystem().getUri().toString()+DFS_INPUT_DIR_NAME)
	Then("all data can be read correctly")
	// check schema
    assert("rows"==df.columns(0))
    val rowsDF=df.select(explode(df("rows")).alias("rows"))
    assert("formattedValue"==rowsDF.select("rows.formattedValue").columns(0))
    assert("comment"==rowsDF.select("rows.comment").columns(0))
    assert("formula"==rowsDF.select("rows.formula").columns(0))
    assert("address"==rowsDF.select("rows.address").columns(0))
    assert("sheetName"==rowsDF.select("rows.sheetName").columns(0))

  // check data
  assert(6==df.count)
  val formattedValues = rowsDF.select("rows.formattedValue").collect
  val comments = rowsDF.select("rows.comment").collect
  val formulas = rowsDF.select("rows.formula").collect
  val addresses = rowsDF.select("rows.address").collect
  val sheetNames = rowsDF.select("rows.sheetName").collect
  // check first row
  assert("A1"==addresses(0).get(0))
  assert("test1"==formattedValues(0).get(0))
  assert("B1"==addresses(1).get(0))
  assert("test2"==formattedValues(1).get(0))
  assert("C1"==addresses(2).get(0))
  assert("test3"==formattedValues(2).get(0))
  assert("D1"==addresses(3).get(0))
  assert("test4"==formattedValues(3).get(0))
  // check second row
  assert("A2"==addresses(4).get(0))
  assert("4"==formattedValues(4).get(0))
  // check third row
  assert("A3"==addresses(5).get(0))
  assert("31/12/99"==formattedValues(5).get(0))
  assert("B3"==addresses(6).get(0))
  assert("5"==formattedValues(6).get(0))
  assert(""==addresses(7).get(0))
  assert(""==formattedValues(7).get(0))
  assert(""==addresses(8).get(0))
  assert(""==formattedValues(8).get(0))
  assert("E3"==addresses(9).get(0))
  assert("null"==formattedValues(9).get(0))
  // check forth row
  assert("A4"==addresses(10).get(0))
  assert("1"==formattedValues(10).get(0))
  // check fifth row
  assert("A5"==addresses(11).get(0))
  assert("2"==formattedValues(11).get(0))
  assert("B5"==addresses(12).get(0))
  assert("6"==formattedValues(12).get(0))
  assert("A5*A6"==formulas(12).get(0))
  assert("C5"==addresses(13).get(0))
  assert("10"==formattedValues(13).get(0))
  assert("A2+B5"==formulas(13).get(0))
  // check sixth row
  assert("A6"==addresses(14).get(0))
  assert("3"==formattedValues(14).get(0))
  assert("B6"==addresses(15).get(0))
  assert("4"==formattedValues(15).get(0))
  assert("C6"==addresses(16).get(0))
  assert("15"==formattedValues(16).get(0))
  assert("SUM(B3:B6)"==formulas(16).get(0))
}


"A new Excel file" should "be created on DFS and reread correctly" in {
	Given("In-memory data input")
  dfsCluster.getFileSystem().delete(DFS_OUTPUT_DIR,true)
  val sqlContext=new SQLContext(sc)
  import sqlContext.implicits._
  val sRdd = sc.parallelize(Seq(Seq("","","1","A1","Sheet1"),Seq("","This is a comment","2","A2","Sheet1"),Seq("","","3","A3","Sheet1"),Seq("","","A2+A3","B1","Sheet1"))).repartition(1)
	val df= sRdd.toDF()
	When("store as Excel file on DFS")
  df.write
      .format("org.zuinnote.spark.office.excel")
    .option("write.locale.bcp47", "de")
    .save(dfsCluster.getFileSystem().getUri().toString()+DFS_OUTPUT_DIR_NAME)
	Then("stored Excel file on DFS can be read correctly")
	// fetch results
  val dfIn = sqlContext.read.format("org.zuinnote.spark.office.excel").option("read.locale.bcp47", "de").load(dfsCluster.getFileSystem().getUri().toString()+DFS_OUTPUT_DIR_NAME)
  assert(3==dfIn.count)
  val rowsDF=dfIn.select(explode(dfIn("rows")).alias("rows"))
  val formattedValues = rowsDF.select("rows.formattedValue").collect
  val comments = rowsDF.select("rows.comment").collect
  val formulas = rowsDF.select("rows.formula").collect
  val addresses = rowsDF.select("rows.address").collect
  val sheetNames = rowsDF.select("rows.sheetName").collect
  // check data
  // check row 1
  assert("A1"==addresses(0).get(0))
  assert("1"==formattedValues(0).get(0))
  assert("B1"==addresses(1).get(0))
  assert("5"==formattedValues(1).get(0))
  assert("A2+A3"==formulas(1).get(0))
  // check row 2
  assert("A2"==addresses(2).get(0))
  assert("2"==formattedValues(2).get(0))
  assert("This is a comment"==comments(2).get(0))
  // check row 3
  assert("A3"==addresses(3).get(0))
  assert("3"==formattedValues(3).get(0))
}


"A new Excel file" should "be created on DFS with specified headers and reread" in {
Given("In-memory data input")
dfsCluster.getFileSystem().delete(DFS_OUTPUT_DIR,true)
val sqlContext=new SQLContext(sc)
import sqlContext.implicits._
val sRdd = sc.parallelize(Seq(Seq("","","1","A2","Sheet1"),Seq("","This is a comment","2","A3","Sheet1"),Seq("","","3","A4","Sheet1"),Seq("","","A3+A4","B1","Sheet1"))).repartition(1)
val columnNames = Seq("column1")

val df= sRdd.toDF(columnNames: _*)

When("store as Excel file on DFS")
df.write
    .format("org.zuinnote.spark.office.excel")
  .option("write.locale.bcp47", "de")
  .option("write.spark.useHeader",true)
  .save(dfsCluster.getFileSystem().getUri().toString()+DFS_OUTPUT_DIR_NAME)
Then("stored Excel file on DFS can be read correctly")
// fetch results
val dfIn = sqlContext.read.format("org.zuinnote.spark.office.excel").option("read.locale.bcp47", "de").load(dfsCluster.getFileSystem().getUri().toString()+DFS_OUTPUT_DIR_NAME)
assert(4==dfIn.count)
val rowsDF=dfIn.select(explode(dfIn("rows")).alias("rows"))
val formattedValues = rowsDF.select("rows.formattedValue").collect
val comments = rowsDF.select("rows.comment").collect
val formulas = rowsDF.select("rows.formula").collect
val addresses = rowsDF.select("rows.address").collect
val sheetNames = rowsDF.select("rows.sheetName").collect
// check data
// check row 1
assert("A1"==addresses(0).get(0))
assert("column1"==formattedValues(0).get(0))
assert("B1"==addresses(1).get(0))
assert("5"==formattedValues(1).get(0))
assert("A3+A4"==formulas(1).get(0))
// check row 2
assert("A2"==addresses(2).get(0))
assert("1"==formattedValues(2).get(0))
// check row 4
assert("A3"==addresses(3).get(0))
assert("2"==formattedValues(3).get(0))
assert("This is a comment"==comments(3).get(0))
// check row 5
assert("A4"==addresses(4).get(0))
assert("3"==formattedValues(4).get(0))
}

"A new Excel file" should "be created on DFS based on standard Spark datatypes and reread " in {
Given("In-memory data input")
dfsCluster.getFileSystem().delete(DFS_OUTPUT_DIR,true)
val sqlContext=new SQLContext(sc)
import sqlContext.implicits._
val df = Seq ((1000L, 2.1, "test"),(2000L,3.1,"test2")).toDF("column1","column2","column3")
When("store as Excel file on DFS")
df.repartition(1).write
    .format("org.zuinnote.spark.office.excel")
  .option("write.locale.bcp47", "de")
  .save(dfsCluster.getFileSystem().getUri().toString()+DFS_OUTPUT_DIR_NAME)

Then("stored Excel file on DFS can be read correctly")
// fetch results
val dfIn = sqlContext.read.format("org.zuinnote.spark.office.excel").option("read.locale.bcp47", "en").load(dfsCluster.getFileSystem().getUri().toString()+DFS_OUTPUT_DIR_NAME)
assert(2==dfIn.count)
val rowsDF=dfIn.select(explode(dfIn("rows")).alias("rows"))
val formattedValues = rowsDF.select("rows.formattedValue").collect
val comments = rowsDF.select("rows.comment").collect
val formulas = rowsDF.select("rows.formula").collect
val addresses = rowsDF.select("rows.address").collect
val sheetNames = rowsDF.select("rows.sheetName").collect
// check data
// check row 1
assert("A1"==addresses(0).get(0))
assert("1000"==formattedValues(0).get(0))
assert("B1"==addresses(1).get(0))
assert("2.1"==formattedValues(1).get(0))
assert("C1"==addresses(2).get(0))
assert("test"==formattedValues(2).get(0))
// check row 2
assert("A2"==addresses(3).get(0))
assert("2000"==formattedValues(3).get(0))
assert("B2"==addresses(4).get(0))
assert("3.1"==formattedValues(4).get(0))
assert("C2"==addresses(5).get(0))
assert("test2"==formattedValues(5).get(0))
}

"An existing Excel file" should "be read in a dataframe with simple datatypes" in {
// create input directory
dfsCluster.getFileSystem().mkdirs(DFS_INPUT_DIR)
// copy test file
val classLoader = getClass().getClassLoader()
    // put testdata on DFS
    val fileName: String="testsimple.xlsx"
    val fileNameFullLocal=classLoader.getResource(fileName).getFile()
    val inputFile=new Path(fileNameFullLocal)
    dfsCluster.getFileSystem().copyFromLocalFile(false, false, inputFile, DFS_INPUT_DIR)
When("loaded by Excel data source")
val sqlContext = new SQLContext(sc)
val df = sqlContext.read.format("org.zuinnote.spark.office.excel").option("read.locale.bcp47", "de").option("read.spark.useHeader", "true").option("read.spark.simpleMode", "true").load(dfsCluster.getFileSystem().getUri().toString()+DFS_INPUT_DIR_NAME)

Then("inferred schema is correct and data is correctly parsed")
}


}
