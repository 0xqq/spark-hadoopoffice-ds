# spark-hadoopoffice-ds
 A [Spark datasource](http://spark.apache.org/docs/latest/sql-programming-guide.html#data-sources) for the HadoopOffice library. This Spark datasource assumes at least Spark 2.0. Currently this datasource supports the following formats of the HadoopOffice library:

* Excel
 * Datasource format: org.zuinnote.spark.office.Excel
 * Loading and Saving of old Excel (.xls) and new Excel (.xlsx)

This datasource will be available on Spark-packages.org and on Maven Central.

Find here the status from the Continuous Integration service: https://travis-ci.org/ZuInnoTe/spark-hadoopoffice-ds/


# Release Notes

## Version 1.0.1
Version based on hadoopoffice library 1.0.1 and the new mapreduce API via the [FileFormat API](https://github.com/apache/spark/blob/master/sql/core/src/main/scala/org/apache/spark/sql/execution/datasources/FileFormat.scala) of Spark2 datasources.

# Options
All [options from the HadoopOffice library](https://github.com/ZuInnoTe/hadoopoffice/wiki/Hadoop-File-Format) are supported. However, in the datasource you specify them without the prefix hadoopoffice. For example, instead of "hadoopoffice.read.locale.bcp47" you need to specify the option as "read.locale.bcp47".

There is one option related to Spark in case you need to write rows containing primitive types. In this case a default sheetname need to be set:
* "write.spark.defaultsheetname", any valid sheetname, e.g. Sheet1

Additionally, the following options of the standard Hadoop API are supported:
* "mapreduce.output.fileoutputformat.compress", true if output should be compressed, false if not. Note that many office formats have already a build-in compression so an additional compression may not make sense.
* "mapreduce.output.fileoutputformat.compress.codec", codec class, e.g. org.apache.hadoop.io.compress.GzipCodec



# Dependency
## Scala 2.10

groupId: com.github.zuinnote

artifactId: spark-hadoopoffice-ds_2.10

version: 1.0.1

## Scala 2.11
 
groupId: com.github.zuinnote

artifactId: spark-hadoopoffice-ds_2.11

version: 1.0.1

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
 
# Develop
## Reading
tbd
## Writing
tbd

# Language bindings
## Scala
tbd
## Java
tbd
## Python
tbd
## R
tbd
## SQL
tbd
