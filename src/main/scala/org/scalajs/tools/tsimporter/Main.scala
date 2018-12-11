/* TypeScript importer for Scala.js
 * Copyright 2013-2014 LAMP/EPFL
 * @author  Sébastien Doeraene
 */

package org.scalajs.tools.tsimporter

import java.io.{ Console => _, Reader => _, _ }

import Trees._

import scala.util.parsing.input._
import parser.TSDefParser

/** Entry point for the TypeScript importer of Scala.js */
object Main {
  def main(args: Array[String]) {
    for (config <- Config.parser.parse(args, Config())) {
      importTsFile(config) match {
        case Right(()) =>
          ()
        case Left(message) =>
          Console.err.println(message)
          System.exit(2)
      }
    }
  }

  def importTsFile(config: Config): Either[String, Unit] = {
    val javaReader = new BufferedReader(new FileReader(config.inputFileName))
    try {
      val reader = new PagedSeqReader(PagedSeq.fromReader(javaReader))
      parseDefinitions(reader).map { definitions =>
        val output = new PrintWriter(new BufferedWriter(new FileWriter(config.outputFileName)))
        try {
          process(definitions, output, config.packageName)
          Right(())
        } finally {
          output.close()
        }
      }
    } finally {
      javaReader.close()
    }
  }

  private def process(definitions: List[DeclTree], output: PrintWriter,
      outputPackage: String, abstractField: Boolean) {
    new Importer(output)(definitions, outputPackage, abstractField)
  }

  private def parseDefinitions(reader: Reader[Char]): Either[String, List[DeclTree]] = {
    val parser = new TSDefParser
    parser.parseDefinitions(reader) match {
      case parser.Success(rawCode, _) =>
        Right(rawCode)

      case parser.NoSuccess(msg, next) =>
        Left(
            "Parse error at %s\n".format(next.pos.toString) +
            msg + "\n" +
            next.pos.longString)
    }
  }
}
