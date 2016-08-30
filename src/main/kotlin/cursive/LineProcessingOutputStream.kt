/*
 * Copyright 2016 Colin Fleming
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cursive

import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CoderResult

abstract class LineProcessingOutputStream : OutputStream() {
  val line = StringBuilder()
  val bytes: ByteBuffer = ByteBuffer.allocate(8192)
  val chars: CharBuffer = CharBuffer.allocate(8192)
  val decoder: CharsetDecoder = Charset.forName("UTF8").newDecoder()

  override fun write(b: Int) {
    bytes.put(b.toByte())
    process(false)
  }

  override fun write(b: ByteArray) {
    bytes.put(b)
    process(false)
  }

  override fun write(b: ByteArray, off: Int, len: Int) {
    bytes.put(b, off, len)
    process(false)
  }

  fun process(endOfInput: Boolean) {
    do {
      bytes.flip()
      val result = decoder.decode(bytes, chars, endOfInput)
      bytes.compact()

      chars.flip()
      while (chars.remaining() > 0) {
        val ch = chars.get()
        line.append(ch)
        if (ch == '\n') {
          processLine(line.toString())
          line.setLength(0)
        }
      }
      chars.compact()
    } while (result == CoderResult.OVERFLOW)
  }

  override fun close() {
    process(true)
    if (line.length > 0) {
      processLine(line.toString() + "\n")
    }
  }

  abstract fun processLine(line: String)
}
