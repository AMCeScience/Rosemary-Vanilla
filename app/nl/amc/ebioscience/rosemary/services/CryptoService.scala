/*
 * Copyright (C) 2016  Academic Medical Center of the University of Amsterdam (AMC)
 * 
 * This program is semi-free software: you can redistribute it and/or modify it
 * under the terms of the Rosemary license. You may obtain a copy of this
 * license at:
 * 
 * https://github.com/AMCeScience/Rosemary-Vanilla/blob/master/LICENSE.md
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * You should have received a copy of the Rosemary license
 * along with this program. If not, 
 * see https://github.com/AMCeScience/Rosemary-Vanilla/blob/master/LICENSE.md.
 * 
 *        Project: https://github.com/AMCeScience/Rosemary-Vanilla
 *        AMC eScience Website: http://www.ebioscience.amc.nl/
 */
package nl.amc.ebioscience.rosemary.services

import javax.inject._
import org.keyczar.Crypter

trait CryptoService {
  def encrypt(plaintext: String): String
  def decrypt(cipher: String): String
}

@Singleton
class RosemaryCryptoService @Inject() (configService: ConfigService) extends CryptoService {
  private val crypter = new Crypter(configService.getStringConfig("crypto.key"))

  override def encrypt(plaintext: String) = crypter.encrypt(plaintext)
  override def decrypt(cipher: String) = crypter.decrypt(cipher)
}
