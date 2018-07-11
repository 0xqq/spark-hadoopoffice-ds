# spark-hadoopoffice-ds
[![Build Status](https://travis-ci.org/ZuInnoTe/spark-hadoopoffice-ds.svg?branch=master)](https://travis-ci.org/ZuInnoTe/spark-hadoopoffice-ds)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/ebd5a75819fb4636ad176f30078fd776)](https://www.codacy.com/app/jornfranke/spark-hadoopoffice-ds?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ZuInnoTe/spark-hadoopoffice-ds&amp;utm_campaign=Badge_Grade)

A [Spark datasource](http://spark.apache.org/docs/latest/sql-programming-guide.html#data-sources) for the [HadoopOffice library](https://github.com/ZuInnoTe/hadoopoffice). This Spark datasource assumes at least Spark 2.0.1. However, the HadoopOffice library can also be used directly from Spark 1.x (see [how to](https://github.com/ZuInnoTe/hadoopoffice/wiki) section). Currently this datasource supports the following formats of the HadoopOffice library:

* Excel
  * Datasource format: org.zuinnote.spark.office.Excel
  * Loading and Saving of old Excel (.xls) and new Excel (.xlsx)

This datasource is available on [Spark-packages.org](https://spark-packages.org/package/ZuInnoTe/spark-hadoopoffice-ds) and on [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Chadoopoffice).

Find here the status from the Continuous Integration service: https://travis-ci.org/ZuInnoTe/spark-hadoopoffice-ds/


# Release Notes

Find the latest release information [here](https://github.com/ZuInnoTe/spark-hadoopoffice-ds/releases)

# Options
All [options from the HadoopOffice library](https://github.com/ZuInnoTe/hadoopoffice/wiki/Hadoop-File-Format), such as metadata, encryption/decryption or low footprint mode, are supported. However, in the datasource you specify them without the prefix hadoopoffice. For example, instead of "hadoopoffice.read.locale.bcp47" you need to specify the option as "read.locale.bcp47".


Additionally the following options exist:
* "read.spark.useHeader" interpret the first row of the Excel as column names of the data frame, True if headers should be read, False if not. Default: False. Check also out [further options](https://github.com/ZuInnoTe/hadoopoffice/wiki/Hadoop-File-Format#header) to deal with headers and skipping lines in other sheets by configuring the HadoopOffice library.
* "read.spark.simpleMode" infers the schema of the DataFrame from the data in the Excel or use a custom schema. This schema consists of primitive DataTypes of Spark SQL (String, Byte, Short, Integer, Long, Decimal, Date, Boolean). If the schema is inferred it is done only based on one file in the directory. Additionally, the conversion of Decimals is based on the locale that you define (see hadoopoffice options from above). True if schema should be inferred, False if not. Default: False
* "read.spark.simpleMode.maxInferRows" (as of 1.1.0). This defines the maximum rows to read for inferring the schema. This is useful if you know already that the schema can be determined from a given number of rows. Alternatively, if you want to provide a custom schema set this to 0. Default: all rows ("-1")
* "read.spark.simpleMode.dateLocale" (as of 1.1.0). This is an experimental options and defines the date locale (in [BCP47](https://tools.ietf.org/html/bcp47) format) for parsing dates in simple mode. Due to a POI speciality in most of the cases (even if you use Excel in a different language) it needs to be set to US. Defaults to "US". 
* "read.spark.simpleMode.datePattern" (as of 1.2.0). This is an experimental option and defines the date pattern for parsing dates in simple mode (overrides "read.spark.simpleMode.dateLocale", if defined). You can define here any date format using the patterns defined in [SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html). Default: ""
* "read.spark.simpleMode.dateTimePattern" (as of 1.2.0). This is an experimental option and defines the date pattern for parsing timestamps in simple mode. You can define here any date time format using the patterns defined in [SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html). Default: "" (means that it defaults to the [java.sql.Timestamp](https://docs.oracle.com/javase/8/docs/api/java/sql/Timestamp.html) pattern).

There are the following options related to Spark in case you need to write rows containing primitive types. In this case a default sheetname need to be set:
* "write.spark.defaultsheetname", any valid sheetname, e.g. Sheet1
* "write.spark.datelocale" (changed in 1.1.1), date formatting used for writing, due to a particularity of Apache POI and Excel most of the time US dateformat is the correct one (Excel converts it automatically to the right dateformat in the language of the user). Default: MM/dd/yyyy
* "write.spark.useHeader" use the column names of the dataframe as headers in Excel (=first row in Excel), True if headers should be written, False if not. Default: False

Additionally, the following options of the standard Hadoop API are supported:
* "mapreduce.output.fileoutputformat.compress", true if output should be compressed, false if not. Note that many office formats have already a build-in compression so an additional compression may not make sense.
* "mapreduce.output.fileoutputformat.compress.codec", codec class, e.g. org.apache.hadoop.io.compress.GzipCodec



# Dependency
## Scala 2.10

groupId: com.github.zuinnote

artifactId: spark-hadoopoffice-ds_2.10

version: 1.1.1

## Scala 2.11
 
groupId: com.github.zuinnote

artifactId: spark-hadoopoffice-ds_2.11

version: 1.1.1

# Schema
## Excel File
An Excel file loaded into a DataFrame  has the following schema. Basically each row contains an Array with all Excel cells in this row. For each cell the following information are available:
* formattedValue: This is what you see when you open Excel
* comment: A comment for this cell
* formula: A formula for this cell (Note: without the =, e.g. "A1+A2")
* address: The address of the cell in A1 format (e.g. "B2")
* sheetName: The name of the sheet of this cell

 ```
root                                                                                                                                                                                   
 |-- rows: array (nullable = true)                                                                                                                                                     
 |    |-- element: struct (containsNull = true)                                                                                                                                        
 |    |    |-- formattedValue: string (nullable = true)                                                                                                                                
 |    |    |-- comment: string (nullable = true)                                                                                                                                       
 |    |    |-- formula: string (nullable = true)                                                                                                                                       
 |    |    |-- address: string (nullable = true)                                                                                                                                       
 |    |    |-- sheetName: string (nullable = true)                                                                                                                          
 ```
 
 If you use the option "read.spark.simpleMode" then the schema consists of primitve Spark SQL DataTypes. For example, for [this Excel file](https://github.com/ZuInnoTe/spark-hadoopoffice-ds/blob/master/src/it/resources/testsimple.xlsx?raw=true) the following schema is automatically inferred (note also the option "read.spark.useHeader" is applied):
 ```
 root
 |-- decimalsc1: decimal(2,1) (nullable = true)
 |-- booleancolumn: boolean (nullable = true)
 |-- datecolumn: date (nullable = true)
 |-- stringcolumn: string (nullable = true)
 |-- decimalp8sc3: decimal(8,3) (nullable = true)
 |-- bytecolumn: byte (nullable = true)
 |-- shortcolumn: short (nullable = true)
 |-- intcolumn: integer (nullable = true)
 |-- longcolumn: long (nullable = true)


 ```
 
# Develop
## Reading
As you can see in the schema, the datasource reads each Excel row in an array. Each element of the array is a structure describing an Excel cell. This structure describes the formatted value (based on the locale), the comment, the formula, the address of the cell in A1 format and the name of the sheet to which the cell belongs. In Scala you can easily read Excel files using the following snippet (assuming US locale for the Excel file):

 ```
val sqlContext = sparkSession.sqlContext
val df = sqlContext.read
    .format("org.zuinnote.spark.office.excel")
    .option("read.locale.bcp47", "us")  
.load(args(0))
```
Find a full example [here](https://github.com/ZuInnoTe/hadoopoffice/wiki/Read-an-Excel-document-using-the-Spark2-datasource-API). 

Another option is to infer the schema of primitive Spark SQL DataTypes automatically:

 ```
val sqlContext = sparkSession.sqlContext
val df = sqlContext.read
    .format("org.zuinnote.spark.office.excel")
    .option("read.locale.bcp47", "us").option("spark.read.simpleMode",true)  
.load(args(0))

 ```

This option can be combined with spark.read.useHeader to interpret the first row in the Excel as column names of the DataFrame.

## Writing
You can have two options for writing data to Excel files:
* You can have a dataframe with columns of simple datatypes (no map, no list, no struct) that should be written in rows of an Excel sheet. You can define the sheetname by using the option "write.spark.defaultsheetname" (default is "Sheet1"). In this way, you can only write values, but no formulas, comments etc. Additionally you can define the option "write.spark.useHeader" to write the column names of the DataFrame as the first row of the Excel.
* You can have a dataframe with arrays where each element corresponds to the schema defined above. In this case you have full control where the data ends, you can use formulas, comments etc.

The second option is illustrated in this snippet (Assuming US locale for the Excel). It creates a simple Excel document with 4 cells. They are stored in sheet "Sheet1". The following Cells exist (A1 with value 1), (A2 with value 2 and comment), (A3 with value 3), (B1 with formula A2+A3). The resulting Excel file is stored in the directory /home/user/office/output
 ```
val sRdd = sparkSession.sparkContext.parallelize(Seq(Seq("","","1","A1","Sheet1"),Seq("","This is a comment","2","A2","Sheet1"),Seq("","","3","A3","Sheet1"),Seq("","","A2+A3","B1","Sheet1"))).repartition(1)
	val df= sRdd.toDF()
	df.write
      .format("org.zuinnote.spark.office.excel")
    .option("write.locale.bcp47", "us") 
.save("/home/user/office/output")
```
Find a full example [here](https://github.com/ZuInnoTe/hadoopoffice/wiki/Write-an-Excel-document-using-the-Spark2-datasource-API).
# Language bindings
## Scala
 This example loads Excel documents from the folder "/home/user/office/input" using the Excel representation (format) and shows the total number of rows, the schema and the first 20 rows. The locale for formatting cell values is set to "us". Find a full example [here](https://github.com/ZuInnoTe/hadoopoffice/wiki/Read-an-Excel-document-using-the-Spark2-datasource-API). 


 ```
val sqlContext = sparkSession.sqlContext
val df = sqlContext.read
    .format("org.zuinnote.spark.office.excel")
    .option("read.locale.bcp47", "us")  // example to set the locale to us
    .load("/home/user/office/input")
	val totalCount = df.count
	// print to screen
	println("Total number of rows in Excel: "+totalCount)	
	df.printSchema
	// print formattedValues
df.show 
```
## Java
  This example loads Excel documents from the folder "/home/user/office/input" using the Excel representation (format) and shows the total number of rows, the schema and the first 20 rows. The locale for formatting cell values is set to "us".
 ```
 SQLContext sqlContext = sparkSession.sqlContext;
 Dataframe df = sqlContext.read
 .format("org.zuinnote.spark.office.excel")
    .option("read.locale.bcp47", "us")  // example to set the locale to us
    .load("/home/user/office/input");
 	long totalCount = df.count;
	// print to screen
	System.out.println("Total number of rows in Excel: "+totalCount);
	df.printSchema();
	// print formattedValues
df.show();

```
## R
   This example loads Excel documents from the folder "/home/user/office/input" using the Excel representation (format). The locale for formatting cell values is set to "us".
```
library(SparkR)

Sys.setenv('SPARKR_SUBMIT_ARGS'='"--packages" "com.github.zuinnote:spark-hadoopoffice-ds_2.11:1.1.0" "sparkr-shell"')
sqlContext <- sparkRSQL.init(sc)

df <- read.df(sqlContext, "/home/user/office/input", source = "org.zuinnote.spark.office.excel", "read.locale.bcp47" = "us")
 ```
## Python
This example loads Excel documents from the folder "/home/user/office/input" using the Excel representation (format).The locale for formatting cell values is set to "us".
```
from pyspark.sql import SQLContext
sqlContext = SQLContext(sc)

df = sqlContext.read.format('org.zuinnote.spark.office.excel').options('read.locale.bcp47'='us').load('/home/user/office/input')
```
## SQL
The following statement creates a table that contains Excel data in the folder //home/user/office/input. The locale for formatting cell values is set to "us".
```
CREATE TABLE ExcelData
USING  org.zuinnote.spark.office.excel
OPTIONS (path "/home/user/office/input", read.locale.bcp47 "us")
```

