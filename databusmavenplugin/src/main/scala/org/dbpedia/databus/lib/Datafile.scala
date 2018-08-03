/*-
 * #%L
 * databus-maven-plugin
 * %%
 * Copyright (C) 2018 Sebastian Hellmann (on behalf of the DBpedia Association)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package org.dbpedia.databus.lib

import java.io._
import java.nio.file.Files
import java.security.PrivateKey
import java.util.Base64

import org.apache.commons.compress.archivers.{ArchiveInputStream, ArchiveStreamFactory}
import org.apache.commons.compress.compressors.CompressorStreamFactory
<<<<<<< HEAD
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.vocabulary.RDF
import org.dbpedia.databus.voc.{DataFileToModel, Format}
=======
import org.apache.jena.rdf.model.Model
import org.dbpedia.databus.Properties
import org.dbpedia.databus.voc.DataFileToModel
>>>>>>> 1dbd9c16cf525812f986d7c8025026a8c92922d2

import scala.io.Source

/**
  * a simple dao to collect all values for a file
  * private constructor, must be called with init to handle compression detection
  */
class Datafile private(datafile: File, props: Properties) {

  val properties = props

<<<<<<< HEAD
=======
  val mimetypeMap = Map(
    "nt" -> "application/n-triples",
    "ttl" -> "text/turtle",
    "tql" -> "application/n-quads",
    "nq" -> "application/n-quads",
    "rdf" -> "application/rdf+xml"
  )
  val compressionMimetypeMap = Map(
    "bzip2" -> "application/x-bzip2",
    "gz" -> "application/x-gzip"
  )
>>>>>>> 1dbd9c16cf525812f986d7c8025026a8c92922d2

  var mimetype: Format = _


  var md5: String = ""
  var bytes: Long = _

  // compression option
  var isArchive: Boolean = false
  var isCompressed: Boolean = false
  var compressionVariant: String = "None"

  var signatureBytes: Array[Byte] = _
  var signatureBase64: String = ""
  var verified: Boolean = false

  var preview: String = ""

  def toModel(): Model = {
    DataFileToModel.datafile2Model(this, datafile)
  }

  //todo we need to have this in dataid-mt
  def updateMimetype(): Datafile = {
    mimetype = Format.detectMimetypeByFileExtension(datafile)
    this
  }

  def updateMD5(): Datafile = {
    md5 = Hash.computeHash(datafile)
    this
  }

  def updateBytes(): Datafile = {
    bytes = datafile.length()
    this
  }

  /**
    *
    * @param lineCount gives the linecount of the preview, however it is limited to 500 chars per line, in case there is a very long line
    * @return
    */
  def updatePreview(lineCount: Int): Datafile = {

    val source = Source.fromInputStream(getInputStream())
    var x = 0
    val sb = new StringBuilder
    val it = source.getLines()
    while (it.hasNext && x <= lineCount) {
      sb.append(it.next()).append("\n")
      x += 1
    }
    source.close
    preview = sb.toString()
    //limit
    if (preview.size > (lineCount * 500)) {
      preview = preview.substring(0, lineCount * 500)
    }
    this
  }

  def updateSignature(privateKey: PrivateKey): Datafile = {
    signatureBytes = Sign.sign(privateKey, datafile);
    signatureBase64 = new String(Base64.getEncoder.encode(signatureBytes))
    //verify
    verified = Sign.verify(privateKey, datafile, signatureBytes)
    this
  }

  /**
    * Opens the file with compression, etc.
    * NOTE: if file is an archive, we assume it is only one file in it and this will be on the stream
    *
    * @return
    */
  def getInputStream(): InputStream = {
    val fi = new BufferedInputStream(new FileInputStream(datafile))
    if (isCompressed) {
      new CompressorStreamFactory()
        .createCompressorInputStream(compressionVariant, fi)
    } else if (isArchive) {
      val ais: ArchiveInputStream = new ArchiveStreamFactory()
        .createArchiveInputStream(compressionVariant, fi)
      ais.getNextEntry
      ais
    } else {
      fi
    }


  }


  override def toString

  = s"Datafile(md5=$md5\nbytes=$bytes\nisArchive=$isArchive\nisCompressed=$isCompressed\ncompressionVariant=$compressionVariant\nsignatureBytes=$signatureBytes\nsignatureBase64=$signatureBase64\nverified=$verified\n})"
}

object Datafile {
  /**
    * factory method
    * * checks wether the file exists
    * * detects compression for further reading
    *
    * @param datafile
    * @return
    */
  //todo add exception to signature
  def init(datafile: File, properties: Properties): Datafile = {

    if (!Files.exists(datafile.toPath)) {
      throw new FileNotFoundException("File not found: " + datafile)
    }
    var df: Datafile = new Datafile(datafile, properties)

    //detect mimetype
    df.updateMimetype()

    // detect compression
    var comp = Compression.detectCompression(datafile)
    var arch = Compression.detectArchive(datafile)

    comp match {
      case "None" => {
        df.isCompressed = false
        df.compressionVariant = "None"
      }
      case _ => {
        df.isCompressed = true
        df.compressionVariant = comp
      }
    }

    arch match {
      // note that if compression is also none\nvalue has already been assigned above
      case "None" => {
        df.isArchive = false
      }
      // overrides compression
      case _ => {
        df.isArchive = true
        df.compressionVariant = arch
      }
    }


    df
  }


}
