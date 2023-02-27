package com.target.data_validator

import io.circe.Json
import io.circe.yaml.parser
import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.types._

object TestHelpers {

  def parseYaml(yaml: String): Json = {
    parser.parse(yaml).right.getOrElse(Json.Null)
  }

  def mkDict(args: (String, String)*): VarSubstitution = {
    val ret = new VarSubstitution
    args.foreach(kv => ret.addString(kv._1, kv._2))
    ret
  }

  def mkDictJson(elems: (String, String)*): VarSubstitution = {
    val ret = new VarSubstitution
    elems.foreach(e => ret.add(e._1, JsonUtils.string2Json(e._2)))
    ret
  }

  def mkConfig(tables: List[ValidatorTable]): ValidatorConfig =
    ValidatorConfig(2, 10, None, detailedErrors = false, None, None, tables) // scalastyle:ignore

  def mkDataFrame(data: List[Row], schema: StructType)(implicit spark: SparkSession, sc: SparkContext): DataFrame =
    spark.createDataFrame(sc.parallelize(data), schema)

  def guessType(v: Any): DataType = v.getClass.getCanonicalName match {
    case "java.lang.Short" => ShortType
    case "java.lang.String" => StringType
    case "java.lang.Integer" => IntegerType
    case "java.lang.Double" => DoubleType
    case "java.lang.Boolean" => BooleanType
    case "java.lang.Long" => LongType
    case "java.lang.Byte" => ByteType
    case _ => throw new IllegalArgumentException(s"Unknown type '${v.getClass.getCanonicalName}'")
  }

  def mkSchema(args: (String, List[Any])*): StructType = {
    StructType(args.map(x => StructField(x._1, guessType(x._2.head))))
  }

  def mkRows(args: (String, List[Any])*): List[Row] = {
    val len = args.head._2.length
    require(args.forall(_._2.length == len))
    (0 until len).map(i => Row(args.map(_._2.apply(i)): _*)).toList
  }

  /** creates dataFrame from array of (label, List[Any])
    * @param spark
    * @param args
    *   \- is array of tuple(String, List[Any]) supported types are String, Double, Int, Long
    * @return
    *   DataFrame
    *
    * ie mkDf(("item" -> List("Eggs", "Milk", "Bread", "Cheese")), ("price" -> List( 5.49, 3.89, 4.50, 6.00),
    * ("quantity" -> List( 12, 5, 2, 10)))
    *
    * will return a dataframe
    */
  def mkDf(spark: SparkSession, args: (String, List[Any])*): DataFrame = {
    require(args.forall(_._2.length == args.head._2.length))
    val schema = mkSchema(args: _*)
    val data = mkRows(args: _*)
    spark.createDataFrame(spark.sparkContext.parallelize(data), schema)
  }
}
