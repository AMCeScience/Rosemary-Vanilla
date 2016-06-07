# Copyright (C) 2016  Academic Medical Center of the University of Amsterdam (AMC)
#  
# This program is semi-free software: you can redistribute it and/or modify it
# under the terms of the Rosemary license. You may obtain a copy of this
# license at:
# 
# https://github.com/AMCeScience/Rosemary-Vanilla/blob/master/LICENSE.md
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the License for the specific language governing permissions and
# limitations under the License.
#  
# You should have received a copy of the Rosemary license
# along with this program. If not, 
# see https://github.com/AMCeScience/Rosemary-Vanilla/blob/master/LICENSE.md.
#  
#        Project: https://github.com/AMCeScience/Rosemary-Vanilla
#        AMC eScience Website: http://www.ebioscience.amc.nl/

module = angular.module 'Rosemary'
module.factory 'Tools', () ->

  new class Tools

    ###########################
    # Instance variables      #
    ###########################

    ###########################
    # Constructor & init      #
    ###########################

    ###########################
    # Methods                 #
    ###########################

    tokenizer: (str) -> str
    name_tokenizer: (obj) -> obj.name

    bloodhound: (data, dt = @name_tokenizer, qt = @tokenizer) ->
      bloodhound = new Bloodhound
        datumTokenizer: dt
        queryTokenizer: qt
        local: data

      bloodhound.promise = bloodhound.initialize()
      bloodhound

    plural: (name, amount) ->
      if _.endsWith name, 's'
        name = name.slice 0, -1

      switch amount
        when 0 then 'no ' + name + 's'
        when 1 then 'one ' + name
        else amount + ' ' + name + 's'